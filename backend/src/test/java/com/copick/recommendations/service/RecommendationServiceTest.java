package com.copick.recommendations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.copick.algorithm.dto.GroupProfile;
import com.copick.algorithm.dto.ParticipantPreference;
import com.copick.algorithm.dto.ScoredMovie;
import com.copick.algorithm.service.CoPickAlgorithm;
import com.copick.connector.client.ConnectorClient;
import com.copick.connector.dto.ConnectorMovie;
import com.copick.connector.dto.DiscoverRequest;
import com.copick.connector.dto.GenrePreference;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.recommendations.dto.GroupStatsResponse;
import com.copick.recommendations.dto.SnapshotMovieResponse;
import com.copick.recommendations.entity.RecommendationSnapshot;
import com.copick.recommendations.entity.SnapshotMovie;
import com.copick.recommendations.entity.SnapshotMovieGenre;
import com.copick.recommendations.entity.SnapshotMovieGenreId;
import com.copick.recommendations.entity.SnapshotMovieId;
import com.copick.recommendations.repository.RecommendationSnapshotRepository;
import com.copick.recommendations.repository.SnapshotMovieGenreRepository;
import com.copick.recommendations.repository.SnapshotMovieRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService: оркестрация рекомендаций и снимков")
class RecommendationServiceTest {

    private static final int GROUP_ID = 1;

    @Mock
    private CoPickAlgorithm algorithm;

    @Mock
    private ConnectorClient connectorClient;

    @Mock
    private GenreMappingService genreMappingService;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ProfilePreferenceRepository preferenceRepository;

    @Mock
    private RecommendationSnapshotRepository snapshotRepository;

    @Mock
    private SnapshotMovieRepository snapshotMovieRepository;

    @Mock
    private SnapshotMovieGenreRepository snapshotMovieGenreRepository;

    private RecommendationService recommendationService;
    
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
	meterRegistry = new SimpleMeterRegistry();
        recommendationService = new RecommendationService(
                algorithm, connectorClient, genreMappingService, groupMemberRepository,
                preferenceRepository, snapshotRepository, snapshotMovieRepository, snapshotMovieGenreRepository, meterRegistry);
        ReflectionTestUtils.setField(recommendationService, "candidatePoolSize", 100);
    }

    private static GroupMember member(int profileId) {
        GroupMember member = new GroupMember();
        member.setId(new GroupMemberId(GROUP_ID, profileId));
        member.setRole(GroupRole.MEMBER);
        return member;
    }

    private static ProfilePreference pref(int profileId, int genreId, double weight) {
        ProfilePreference preference = new ProfilePreference();
        preference.setId(new ProfilePreferenceId(profileId, genreId));
        preference.setWeight(weight);
        return preference;
    }

    private static ConnectorMovie connectorMovie(int id, String title, List<String> genres, double rating) {
        return new ConnectorMovie(id, title, "Original " + title,
                "https://poster/" + id, "https://backdrop/" + id, rating,
                2024, genres, "overview " + id, genres.isEmpty() ? null : genres.get(0));
    }

    private static ScoredMovie scored(int id, double score) {
        return new ScoredMovie(id, score, Set.of(3), 0.0);
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendations {

        @Test
        @DisplayName("нет участников -> пустой результат без похода в коннектор")
        void noMembersReturnsEmptyResult() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of());

            List<ScoredMovie> result =
                    recommendationService.getRecommendations(GROUP_ID, null, null, 10);

            assertThat(result).isEmpty();
            verifyNoInteractions(connectorClient, algorithm, snapshotRepository);
        }

        @Test
        @DisplayName("запрошенные profileIds ограничивают выборку участников")
        void requestedProfileIdsNarrowMembers() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileIdIn(GROUP_ID, List.of(1, 2)))
                    .thenReturn(List.of());

            recommendationService.getRecommendations(GROUP_ID, List.of(1, 2), null, 10);

            verify(groupMemberRepository).findByIdGroupIdAndIdProfileIdIn(GROUP_ID, List.of(1, 2));
            verify(groupMemberRepository, never()).findByIdGroupId(GROUP_ID);
        }

        @Test
        @DisplayName("полный сценарий: профиль группы -> запрос к коннектору -> оценки -> снимок")
        void fullPipelineBuildsRequestScoresAndSavesSnapshot() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID))
                    .thenReturn(List.of(member(1), member(2)));
            when(preferenceRepository.findByIdProfileIdIn(List.of(1, 2)))
                    .thenReturn(List.of(pref(1, 3, 1.0), pref(2, 3, 0.5), pref(2, 9, -1.0)));

            // профиль группы: жанр 3 влияет (коэф. 1.0), жанр 9 под вето
            GroupProfile groupProfile = new GroupProfile(
                    Map.of(3, 0.75), Set.of(9), 2, Map.of(3, 1.0));
            when(algorithm.calculateGroupProfile(anyList(), anyList())).thenReturn(groupProfile);
            when(genreMappingService.getGenreNameById(3)).thenReturn("action");
            when(genreMappingService.getGenreNameById(9)).thenReturn("horror");

            // коннектор возвращает два фильма
            when(connectorClient.fetchMovies(any(DiscoverRequest.class)))
                    .thenReturn(List.of(
                            connectorMovie(101, "Movie A", List.of("action"), 8.0),
                            connectorMovie(102, "Movie B", List.of("mystery"), 7.0)));

            // маппинг жанров: action -> 3, mystery неизвестен коннектору
            when(genreMappingService.getGenreIdByName("action")).thenReturn(3);
            when(genreMappingService.getGenreIdByName("mystery")).thenReturn(null);

            // алгоритм возвращает оценки
            when(algorithm.generateRecommendations(anyList(), anyList(), anyList(), eq(true)))
                    .thenReturn(List.of(scored(101, 1.2), scored(102, 0.4)));

            // снимок сохраняется
            when(snapshotRepository.save(any(RecommendationSnapshot.class))).thenAnswer(invocation -> {
                RecommendationSnapshot snapshot = invocation.getArgument(0);
                snapshot.setSnapshotId(77);
                return snapshot;
            });

            List<ScoredMovie> result =
                    recommendationService.getRecommendations(GROUP_ID, null, List.of(9), 10);

            assertThat(result).extracting(ScoredMovie::movieId).containsExactly(101, 102);

            // запрос к коннектору собран корректно
            ArgumentCaptor<DiscoverRequest> requestCaptor = ArgumentCaptor.forClass(DiscoverRequest.class);
            verify(connectorClient).fetchMovies(requestCaptor.capture());
            DiscoverRequest request = requestCaptor.getValue();
            // 1.0 * 100 = 100 фильмов жанра action
            assertThat(request.preferences()).containsExactly(new GenrePreference("action", 100));
            assertThat(request.excluded_genres()).containsExactly("horror");

            // кандидаты переданы алгоритму с замапленными внутренними id жанров
            ArgumentCaptor<List<com.copick.algorithm.dto.MovieCandidate>> candidatesCaptor =
                    ArgumentCaptor.forClass((Class<List<com.copick.algorithm.dto.MovieCandidate>>) (Class<?>) List.class);
            verify(algorithm).generateRecommendations(anyList(), anyList(), candidatesCaptor.capture(), eq(true));
            assertThat(candidatesCaptor.getValue())
                    .extracting(com.copick.algorithm.dto.MovieCandidate::movieId)
                    .containsExactly(101, 102);
            // неизвестный жанр "mystery" отфильтрован
            assertThat(candidatesCaptor.getValue().get(1).genreIds()).isEmpty();

            // старый снимок удалён, новый сохранён вместе с фильмами и жанрами
            verify(snapshotRepository).deleteByGroupGroupId(GROUP_ID);
            verify(snapshotRepository).flush();
            verify(snapshotMovieRepository, org.mockito.Mockito.times(2)).save(any(SnapshotMovie.class));
            // жанр сохранён только для "action"
            verify(snapshotMovieGenreRepository, org.mockito.Mockito.times(1))
                    .save(any(SnapshotMovieGenre.class));
        }

        @Test
        @DisplayName("limit обрезает результат, в снимок попадают только фильмы из выдачи")
        void limitTruncatesResultAndSnapshot() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of(member(1)));
            when(preferenceRepository.findByIdProfileIdIn(List.of(1))).thenReturn(List.of());

            when(algorithm.calculateGroupProfile(anyList(), anyList()))
                    .thenReturn(new GroupProfile(Map.of(), Set.of(), 1, Map.of()));
            when(connectorClient.fetchMovies(any(DiscoverRequest.class)))
                    .thenReturn(List.of(
                            connectorMovie(101, "Movie A", List.of(), 8.0),
                            connectorMovie(102, "Movie B", List.of(), 7.0)));
            when(algorithm.generateRecommendations(anyList(), anyList(), anyList(), eq(true)))
                    .thenReturn(List.of(scored(101, 1.2), scored(102, 0.4)));
            when(snapshotRepository.save(any(RecommendationSnapshot.class))).thenAnswer(invocation -> {
                RecommendationSnapshot snapshot = invocation.getArgument(0);
                snapshot.setSnapshotId(77);
                return snapshot;
            });

            List<ScoredMovie> result =
                    recommendationService.getRecommendations(GROUP_ID, null, null, 1);

            assertThat(result).extracting(ScoredMovie::movieId).containsExactly(101);

            // в снимок попал только первый фильм
            ArgumentCaptor<SnapshotMovie> movieCaptor = ArgumentCaptor.forClass(SnapshotMovie.class);
            verify(snapshotMovieRepository).save(movieCaptor.capture());
            assertThat(movieCaptor.getValue().getTitle()).isEqualTo("Movie A");
        }

        @Test
        @DisplayName("жанры с нулевым count и неизвестными именами не попадают в запрос")
        void zeroCountsAndUnknownNamesFilteredFromRequest() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of(member(1)));
            when(preferenceRepository.findByIdProfileIdIn(List.of(1))).thenReturn(List.of());

            // коэффициент 0.002 * 100 = 0.2 -> округляется в 0 и отфильтровывается
            when(algorithm.calculateGroupProfile(anyList(), anyList()))
                    .thenReturn(new GroupProfile(Map.of(3, 0.002), Set.of(), 1, Map.of(3, 0.002)));
            when(genreMappingService.getGenreNameById(3)).thenReturn(null); // имя неизвестно

            when(connectorClient.fetchMovies(any(DiscoverRequest.class))).thenReturn(List.of());
            when(algorithm.generateRecommendations(anyList(), anyList(), anyList(), eq(true)))
                    .thenReturn(List.of());
            when(snapshotRepository.save(any(RecommendationSnapshot.class))).thenAnswer(invocation -> {
                RecommendationSnapshot snapshot = invocation.getArgument(0);
                snapshot.setSnapshotId(77);
                return snapshot;
            });

            recommendationService.getRecommendations(GROUP_ID, null, null, 10);

            ArgumentCaptor<DiscoverRequest> requestCaptor = ArgumentCaptor.forClass(DiscoverRequest.class);
            verify(connectorClient).fetchMovies(requestCaptor.capture());
            assertThat(requestCaptor.getValue().preferences()).isEmpty();
            assertThat(requestCaptor.getValue().excluded_genres()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getGroupStats")
    class GetGroupStats {

        @Test
        @DisplayName("нет участников -> нулевая статистика")
        void noMembersReturnsZeroStats() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of());

            GroupStatsResponse stats = recommendationService.getGroupStats(GROUP_ID);

            assertThat(stats.memberCount()).isZero();
            assertThat(stats.topGenres()).isEmpty();
            assertThat(stats.bannedGenres()).isEmpty();
        }

        @Test
        @DisplayName("топ-5 жанров по среднему весу + минимальный вес + вето")
        void computesTopGenresAndBanned() {
            when(groupMemberRepository.findByIdGroupId(GROUP_ID))
                    .thenReturn(List.of(member(1), member(2)));
            when(preferenceRepository.findByIdProfileIdIn(List.of(1, 2)))
                    .thenReturn(List.of(
                            pref(1, 3, 1.0), pref(2, 3, 0.6),   // жанр 3: avg 0.8, min 0.6
                            pref(1, 4, 0.4)));                     // жанр 4: avg 0.4, min 0.4
            // жанр 5 есть только в профиле группы (вето), но не в prefs — минимальный вес 0.0 по умолчанию

            when(algorithm.calculateGroupProfile(anyList(), anyList())).thenReturn(new GroupProfile(
                    Map.of(3, 0.8, 4, 0.4, 5, -1.0), Set.of(5), 2, Map.of(3, 0.67, 4, 0.33)));
            when(genreMappingService.getGenreNameById(3)).thenReturn("action");
            when(genreMappingService.getGenreNameById(4)).thenReturn("comedy");
            when(genreMappingService.getGenreNameById(5)).thenReturn("horror");

            GroupStatsResponse stats = recommendationService.getGroupStats(GROUP_ID);

            assertThat(stats.memberCount()).isEqualTo(2);

            assertThat(stats.topGenres()).hasSize(3);
            assertThat(stats.topGenres().get(0).genreName()).isEqualTo("action");
            assertThat(stats.topGenres().get(0).avgWeight()).isEqualTo(0.8);
            assertThat(stats.topGenres().get(0).minWeight()).isEqualTo(0.6);
            assertThat(stats.topGenres().get(1).genreName()).isEqualTo("comedy");
            assertThat(stats.topGenres().get(2).genreName()).isEqualTo("horror");
            assertThat(stats.topGenres().get(2).minWeight()).isEqualTo(0.0); // дефолт при отсутствии

            assertThat(stats.bannedGenres()).containsExactly("horror");
        }
    }

    @Nested
    @DisplayName("getLastSnapshot")
    class GetLastSnapshot {

        @Test
        @DisplayName("нет снимка -> пустой список")
        void noSnapshotReturnsEmptyList() {
            when(snapshotRepository.findByGroupGroupId(GROUP_ID)).thenReturn(Optional.empty());

            assertThat(recommendationService.getLastSnapshot(GROUP_ID)).isEmpty();
        }

        @Test
        @DisplayName("снимок собирается из фильмов и жанров по позициям")
        void returnsSnapshotMoviesWithGenreNames() {
            RecommendationSnapshot snapshot = new RecommendationSnapshot();
            snapshot.setSnapshotId(55);
            snapshot.setCreatedAt(OffsetDateTime.parse("2026-02-01T10:00:00Z"));
            when(snapshotRepository.findByGroupGroupId(GROUP_ID)).thenReturn(Optional.of(snapshot));

            SnapshotMovie first = SnapshotMovie.builder()
                    .id(new SnapshotMovieId(55, 0))
                    .title("Movie A").originalTitle("Original A")
                    .posterUrl("https://poster/a").backdropUrl("https://backdrop/a")
                    .rating(8.0).releaseYear(2024).overview("First").build();
            SnapshotMovie second = SnapshotMovie.builder()
                    .id(new SnapshotMovieId(55, 1))
                    .title("Movie B").originalTitle("Original B")
                    .posterUrl("https://poster/b").backdropUrl("https://backdrop/b")
                    .rating(7.0).releaseYear(2023).overview("Second").build();
            when(snapshotMovieRepository.findBySnapshotIdOrderByPosition(55))
                    .thenReturn(List.of(first, second));

            when(snapshotMovieGenreRepository.findByIdSnapshotId(55)).thenReturn(List.of(
                    new SnapshotMovieGenre(new SnapshotMovieGenreId(55, 0, 3)),
                    new SnapshotMovieGenre(new SnapshotMovieGenreId(55, 0, 4)),
                    new SnapshotMovieGenre(new SnapshotMovieGenreId(55, 1, 3)),
                    new SnapshotMovieGenre(new SnapshotMovieGenreId(55, 0, 999)) // неизвестный жанр
            ));
            when(genreMappingService.getGenreNameById(3)).thenReturn("action");
            when(genreMappingService.getGenreNameById(4)).thenReturn("comedy");
            when(genreMappingService.getGenreNameById(999)).thenReturn(null);

            List<SnapshotMovieResponse> result = recommendationService.getLastSnapshot(GROUP_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).title()).isEqualTo("Movie A");
            assertThat(result.get(0).genres()).containsExactly("action", "comedy");
            assertThat(result.get(1).title()).isEqualTo("Movie B");
            assertThat(result.get(1).genres()).containsExactly("action");
        }
    }
}
