package com.copick.recommendations.service;

import  com.copick.recommendations.dto.*;
import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.*;
import com.copick.algorithm.service.CoPickAlgorithm;
import com.copick.connector.client.ConnectorClient;
import com.copick.connector.dto.*;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final CoPickAlgorithm algorithm;
    private final ConnectorClient connectorClient;
    private final GenreMappingService genreMappingService;
    private final GroupMemberRepository groupMemberRepository;
    private final ProfilePreferenceRepository preferenceRepository;

    public RecommendationService(
            CoPickAlgorithm algorithm,
            ConnectorClient connectorClient,
            GenreMappingService genreMappingService,
            GroupMemberRepository groupMemberRepository,
            ProfilePreferenceRepository preferenceRepository
    ) {
        this.algorithm = algorithm;
        this.connectorClient = connectorClient;
        this.genreMappingService = genreMappingService;
        this.groupMemberRepository = groupMemberRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public List<ScoredMovie> getRecommendations(int groupId, List<Integer> requestedProfileIds, List<Integer> contextGenreIds, int limit) {

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

        Map<Integer, Map<Integer, Double>> profileToGenres = prefs.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getId().getProfileId(),
                        Collectors.toMap(
                                p -> p.getId().getGenreId(),
                                ProfilePreference::getWeight
                        )
                ));

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

        return algorithm.generateRecommendations(participants, contextGenreIds, candidates, true);
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

        Map<Integer, Map<Integer, Double>> profileToGenres = prefs.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getId().getProfileId(),
                        Collectors.toMap(
                                p -> p.getId().getGenreId(),
                                p -> p.getWeight()
                        )
                ));

        List<ParticipantPreference> participants = profileToGenres.entrySet().stream()
                .map(entry -> new ParticipantPreference(entry.getKey(), entry.getValue()))
                .toList();

        GroupProfile groupProfile = algorithm.calculateGroupProfile(participants, List.of());

        Map<Integer, Double> minWeights = prefs.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getId().getGenreId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(ProfilePreference::getWeight, Collectors.toList()),
                                list -> list.stream().mapToDouble(Double::doubleValue).min().orElse(0.0)
                        )
                ));

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

        return new GroupStatsResponse(
                members.size(),
                topGenres,
                bannedGenres
        );
    }
}