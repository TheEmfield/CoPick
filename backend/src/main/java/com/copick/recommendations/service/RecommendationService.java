package com.copick.recommendations.service;

import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.GroupProfile;
import com.copick.algorithm.dto.MovieCandidate;
import com.copick.algorithm.dto.ParticipantPreference;
import com.copick.algorithm.dto.ScoredMovie;
import com.copick.algorithm.service.CoPickAlgorithm;
import com.copick.connector.client.ConnectorClient;
import com.copick.connector.dto.ConnectorMovie;
import com.copick.connector.dto.DiscoverRequest;
import com.copick.connector.dto.GenrePreference;
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
    public List<ScoredMovie> getRecommendations(int groupId, List<Integer> requestedProfileIds, int limit) {

        // 1. Получаем участников группы (эффективный запрос к БД)
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

        // 2. Загружаем предпочтения всех участников одним запросом
        List<ProfilePreference> prefs = preferenceRepository.findByIdProfileIdIn(profileIds);

        // 3. Группируем предпочтения по профилям: Map<profileId, Map<genreId, weight>>
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

        // 4. Считаем профиль группы (контекстные жанры пока пустые)
        GroupProfile groupProfile = algorithm.calculateGroupProfile(participants, List.of());

        // 5. Формируем запрос к Go-коннектору
        int targetTotal = Math.max(limit, 20); // Минимум 20 фильмов, чтобы алгоритму было из чего выбирать (M=50 по дефолту)

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

        // 6. Получаем фильмы от Go-коннектора
        List<ConnectorMovie> connectorMovies = connectorClient.fetchMovies(request);

        // 7. Маппинг: ConnectorMovie (String genres) -> MovieCandidate (Integer genreIds)
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

        // 8. Запускаем чистый алгоритм и возвращаем результат
        return algorithm.generateRecommendations(participants, List.of(), candidates, true);
    }
}