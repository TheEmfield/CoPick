package com.copick.recommendations.service;

import com.copick.algorithm.dto.*;
import com.copick.algorithm.service.CoPickAlgorithm;
import com.copick.connector.client.ConnectorClient;
import com.copick.connector.dto.*;
import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.recommendations.dto.*;
import com.copick.recommendations.entity.*;
import com.copick.recommendations.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final CoPickAlgorithm algorithm;
    private final ConnectorClient connectorClient;
    private final GenreMappingService genreMappingService;
    private final GroupMemberRepository groupMemberRepository;
    private final ProfilePreferenceRepository preferenceRepository;
    private final RecommendationSnapshotRepository snapshotRepository;
    private final SnapshotMovieRepository snapshotMovieRepository;
    private final SnapshotMovieGenreRepository snapshotMovieGenreRepository;

    public RecommendationService(
            CoPickAlgorithm algorithm,
            ConnectorClient connectorClient,
            GenreMappingService genreMappingService,
            GroupMemberRepository groupMemberRepository,
            ProfilePreferenceRepository preferenceRepository,
            RecommendationSnapshotRepository snapshotRepository,
            SnapshotMovieRepository snapshotMovieRepository,
            SnapshotMovieGenreRepository snapshotMovieGenreRepository
    ) {
        this.algorithm = algorithm;
        this.connectorClient = connectorClient;
        this.genreMappingService = genreMappingService;
        this.groupMemberRepository = groupMemberRepository;
        this.preferenceRepository = preferenceRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotMovieRepository = snapshotMovieRepository;
        this.snapshotMovieGenreRepository = snapshotMovieGenreRepository;
    }

    @Transactional
    public List<ScoredMovie> getRecommendations(
            int groupId,
            List<Integer> requestedProfileIds,
            List<Integer> contextGenreIds,
            int limit
    ) {
        List<GroupMember> members;
        if (requestedProfileIds != null && !requestedProfileIds.isEmpty()) {
            members = groupMemberRepository.findByIdGroupIdAndIdProfileIdIn(groupId, requestedProfileIds);
        } else {
            members = groupMemberRepository.findByIdGroupId(groupId);
        }

        if (members.isEmpty()) {
            return List.of();
        }

        List<Integer> profileIds = members.stream()
                .map(m -> m.getId().getProfileId())
                .toList();

        List<ProfilePreference> prefs = preferenceRepository.findByIdProfileIdIn(profileIds);

        Map<Integer, Map<Integer, Double>> profileToGenres = new HashMap<>();
        for (ProfilePreference p : prefs) {
            profileToGenres
                    .computeIfAbsent(p.getId().getProfileId(), k -> new HashMap<>())
                    .put(p.getId().getGenreId(), p.getWeight());
        }

        List<ParticipantPreference> participants = profileToGenres.entrySet().stream()
                .map(entry -> new ParticipantPreference(entry.getKey(), entry.getValue()))
                .toList();

        GroupProfile groupProfile = algorithm.calculateGroupProfile(participants, contextGenreIds);

        int targetTotal = Math.max(limit, 20);

        List<GenrePreference> preferences = groupProfile.genreInfluenceCoefficients().entrySet().stream()
                .map(entry -> {
                    String genreName = genreMappingService.getGenreNameById(entry.getKey());
                    int count = (int) Math.round(entry.getValue() * targetTotal);
                    return new GenrePreference(genreName, count);
                })
                .filter(p -> p.genre() != null && p.count() > 0)
                .toList();

        List<String> excludedGenres = groupProfile.vetoedGenreIds().stream()
                .map(genreMappingService::getGenreNameById)
                .filter(Objects::nonNull)
                .toList();

        DiscoverRequest request = new DiscoverRequest(preferences, excludedGenres);

        List<ConnectorMovie> connectorMovies = connectorClient.fetchMovies(request);

        List<MovieCandidate> candidates = connectorMovies.stream()
                .map(cm -> {
                    Set<Integer> internalGenreIds = cm.genres().stream()
                            .map(genreMappingService::getGenreIdByName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    return new MovieCandidate(
                            cm.id(),
                            cm.title(),
                            internalGenreIds,
                            cm.rating()
                    );
                })
                .toList();

        List<ScoredMovie> result = algorithm.generateRecommendations(participants, contextGenreIds, candidates, true);

        Set<Integer> scoredMovieIds = result.stream()
                .map(ScoredMovie::movieId)
                .collect(Collectors.toSet());

        List<ConnectorMovie> moviesToSave = connectorMovies.stream()
                .filter(cm -> scoredMovieIds.contains(cm.id()))
                .toList();

        saveSnapshot(groupId, moviesToSave, profileIds);

        return result;
    }

    @Transactional(readOnly = true)
    public GroupStatsResponse getGroupStats(int groupId) {
        List<GroupMember> members = groupMemberRepository.findByIdGroupId(groupId);
        if (members.isEmpty()) {
            return new GroupStatsResponse(0, List.of(), List.of());
        }

        List<Integer> profileIds = members.stream()
                .map(m -> m.getId().getProfileId())
                .toList();

        List<ProfilePreference> prefs = preferenceRepository.findByIdProfileIdIn(profileIds);

        Map<Integer, Map<Integer, Double>> profileToGenres = new HashMap<>();
        for (ProfilePreference p : prefs) {
            profileToGenres
                    .computeIfAbsent(p.getId().getProfileId(), k -> new HashMap<>())
                    .put(p.getId().getGenreId(), p.getWeight());
        }

        List<ParticipantPreference> participants = profileToGenres.entrySet().stream()
                .map(entry -> new ParticipantPreference(entry.getKey(), entry.getValue()))
                .toList();

        GroupProfile groupProfile = algorithm.calculateGroupProfile(participants, List.of());

        Map<Integer, Double> minWeights = new HashMap<>();
        for (ProfilePreference p : prefs) {
            minWeights.merge(p.getId().getGenreId(), p.getWeight(), Math::min);
        }

        List<GenreStat> topGenres = groupProfile.averageWeights().entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new GenreStat(
                        genreMappingService.getGenreNameById(entry.getKey()),
                        entry.getValue(),
                        minWeights.getOrDefault(entry.getKey(), 0.0)
                ))
                .toList();

        List<String> bannedGenres = groupProfile.vetoedGenreIds().stream()
                .map(genreMappingService::getGenreNameById)
                .filter(Objects::nonNull)
                .toList();

        return new GroupStatsResponse(members.size(), topGenres, bannedGenres);
    }

    @Transactional
    public void saveSnapshot(int groupId, List<ConnectorMovie> movies, List<Integer> profileIds) {
        snapshotRepository.deleteByGroupGroupId(groupId);

        RecommendationSnapshot snapshot = RecommendationSnapshot.builder()
                .group(createGroupReference(groupId))
                .createdAt(OffsetDateTime.now())
                .build();
        snapshot = snapshotRepository.save(snapshot);

        Integer snapshotId = snapshot.getSnapshotId();

        for (int i = 0; i < movies.size(); i++) {
            ConnectorMovie movie = movies.get(i);

            SnapshotMovie snapshotMovie = SnapshotMovie.builder()
                    .id(new SnapshotMovieId(snapshotId, i))
                    .title(movie.title())
                    .originalTitle(movie.original_title())
                    .posterUrl(movie.poster_url())
                    .backdropUrl(movie.backdrop_url())
                    .rating(movie.rating())
                    .releaseYear(movie.release_year())
                    .overview(movie.overview())
                    .build();
            snapshotMovieRepository.save(snapshotMovie);

            for (String genreName : movie.genres()) {
                Integer genreId = genreMappingService.getGenreIdByName(genreName);
                if (genreId != null) {
                    SnapshotMovieGenre genre = SnapshotMovieGenre.builder()
                            .id(new SnapshotMovieGenreId(snapshotId, i, genreId))
                            .build();
                    snapshotMovieGenreRepository.save(genre);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SnapshotMovieResponse> getLastSnapshot(int groupId) {
        Optional<RecommendationSnapshot> snapshotOpt = snapshotRepository.findByGroupGroupId(groupId);
        if (snapshotOpt.isEmpty()) {
            return List.of();
        }

        RecommendationSnapshot snapshot = snapshotOpt.get();
        Integer snapshotId = snapshot.getSnapshotId();

        List<SnapshotMovie> movies = snapshotMovieRepository
                .findByIdSnapshotIdOrderByMoviePositionAsc(snapshotId);

        Map<Integer, List<SnapshotMovieGenre>> genresByPosition = snapshotMovieGenreRepository
                .findByIdSnapshotId(snapshotId)
                .stream()
                .collect(Collectors.groupingBy(g -> g.getId().getMoviePosition()));

        return movies.stream()
                .map(movie -> toSnapshotMovieResponse(movie, genresByPosition))
                .toList();
    }

    private SnapshotMovieResponse toSnapshotMovieResponse(
            SnapshotMovie movie,
            Map<Integer, List<SnapshotMovieGenre>> genresByPosition
    ) {
        List<String> genreNames = genresByPosition
                .getOrDefault(movie.getId().getMoviePosition(), List.of())
                .stream()
                .map(g -> genreMappingService.getGenreNameById(g.getId().getGenreId()))
                .filter(Objects::nonNull)
                .toList();

        return new SnapshotMovieResponse(
                movie.getTitle(),
                movie.getOriginalTitle(),
                movie.getPosterUrl(),
                movie.getBackdropUrl(),
                movie.getRating(),
                movie.getReleaseYear(),
                movie.getOverview(),
                genreNames
        );
    }

    private Group createGroupReference(Integer groupId) {
        Group group = new Group();
        group.setGroupId(groupId);
        return group;
    }
}