package com.copick.algorithm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.GroupProfile;
import com.copick.algorithm.dto.MovieCandidate;
import com.copick.algorithm.dto.ParticipantPreference;
import com.copick.algorithm.dto.ScoredMovie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CoPickAlgorithm: конвейер подбора фильмов для группы")
class CoPickAlgorithmTest {

    private CoPickAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new CoPickAlgorithm(new AlgorithmConfig());
    }

    private static ParticipantPreference participant(int profileId, Map<Integer, Double> weights) {
        return new ParticipantPreference(profileId, weights);
    }

    private static MovieCandidate movie(int id, Set<Integer> genres, double rating) {
        return new MovieCandidate(id, "movie-" + id, genres, rating);
    }

    @Test
    @DisplayName("пустой список кандидатов -> пустой результат")
    void generateRecommendationsReturnsEmptyForNoCandidates() {
        List<ScoredMovie> result = algorithm.generateRecommendations(
                List.of(participant(1, Map.of(1, 1.0))), List.of(), List.of(), true);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("жесткое вето: фильм с вето жанром получает -1 и уходит в конец")
    void hardVetoPushesMovieToBottom() {
        List<ParticipantPreference> participants =
                List.of(participant(1, Map.of(1, -1.0, 2, 1.0)));

        List<MovieCandidate> candidates = List.of(
                movie(10, Set.of(1, 2), 5.0), // содержит вето-жанр
                movie(11, Set.of(2), 5.0)     // только любимый жанр
        );

        List<ScoredMovie> result = algorithm.generateRecommendations(participants, List.of(), candidates, false);

        assertThat(result).extracting(ScoredMovie::movieId).containsExactly(11, 10);
        assertThat(result.get(1).finalScore()).isEqualTo(-1.0, within(1e-9));
    }

    @Test
    @DisplayName("мягкое вето: вето-жанр в контексте «сегодня» заменяется на -kappa")
    void softVetoAppliesKappaWeightForContextGenre() {
        // Жанр 1 вето, но он входит в контекст: вес = -kappa = -0.5.
        List<ParticipantPreference> participants = List.of(participant(1, Map.of(1, -1.0)));

        List<ScoredMovie> result = algorithm.generateRecommendations(
                participants, List.of(1), List.of(movie(10, Set.of(1), 5.0)), false);

        // personal = mean(-0.5) + lambda * min(0, -0.5) = -0.5 - 0.25 = -0.75
        // group   = -0.75 (один участник)
        // final   = -0.75 + beta * 1 (контекст) + rho * q (rating 5 -> q = 0) = -0.35
        assertThat(result).hasSize(1);
        assertThat(result.get(0).finalScore()).isEqualTo(-0.35, within(1e-9));
    }

    @Test
    @DisplayName("групповая оценка балансирует среднее и оценку наименее довольных (alpha)")
    void groupScoreBalancesMeanAndLeastSatisfied() {
        // Два участника: оценки 1.0 и 0.0.
        List<ParticipantPreference> participants = List.of(
                participant(1, Map.of(1, 1.0)),
                participant(2, Map.of(1, 0.0))
        );

        List<ScoredMovie> result = algorithm.generateRecommendations(
                participants, List.of(), List.of(movie(10, Set.of(1), 5.0)), false);

        // mean = 0.5; k = max(1, 2/4) = 1; lowmeank = 0.0
        // group = alpha * 0.5 + (1 - alpha) * 0.0 = 0.3
        assertThat(result.get(0).finalScore()).isEqualTo(0.3, within(1e-9));
    }

    @Test
    @DisplayName("при 8 участниках lowmeank усредняет двух самых недовольных (k = n/4 = 2)")
    void lowMeanKTakesQuarterOfLeastSatisfied() {
        List<ParticipantPreference> participants = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            participants.add(participant(i, Map.of(1, 1.0)));
        }
        for (int i = 5; i <= 8; i++) {
            participants.add(participant(i, Map.of(1, 0.5)));
        }

        List<ScoredMovie> result = algorithm.generateRecommendations(
                participants, List.of(), List.of(movie(10, Set.of(1), 5.0)), false);

        // mean = (4*1.0 + 4*0.5) / 8 = 0.75; k = 2; lowmeank = (0.5 + 0.5) / 2 = 0.5
        // group = 0.6 * 0.75 + 0.4 * 0.5 = 0.65
        assertThat(result.get(0).finalScore()).isEqualTo(0.65, within(1e-9));
    }

    @Test
    @DisplayName("null-контекст обрабатывается как пустой (без NPE и без бонуса)")
    void nullContextTreatedAsEmpty() {
        List<ScoredMovie> result = algorithm.generateRecommendations(
                List.of(participant(1, Map.of(1, 1.0))),
                null,
                List.of(movie(10, Set.of(1), 5.0)),
                false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).finalScore()).isEqualTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("без MMR при равных оценках порядок определяется по убыванию movieId")
    void withoutMmrTiesBrokenByHigherMovieId() {
        List<ParticipantPreference> participants = List.of(participant(1, Map.of(1, 1.0)));

        List<MovieCandidate> candidates = List.of(
                movie(1, Set.of(1), 5.0),
                movie(2, Set.of(1), 5.0)
        );

        List<ScoredMovie> result = algorithm.generateRecommendations(participants, List.of(), candidates, false);

        assertThat(result).extracting(ScoredMovie::movieId).containsExactly(2, 1);
    }

    @Nested
    @DisplayName("MMR")
    class MmrDiversification {

        private List<ParticipantPreference> participants;
        private List<MovieCandidate> candidates;

        @BeforeEach
        void setUpData() {
            // Участнику нравятся оба жанра одинаково — все фильмы получают final = 1.0.
            participants = List.of(participant(1, Map.of(1, 1.0, 2, 1.0)));
            candidates = List.of(
                    movie(4, Set.of(1), 5.0),      // только жанр 1
                    movie(3, Set.of(1), 5.0),      // только жанр 1
                    movie(2, Set.of(2), 5.0),      // только жанр 2
                    movie(1, Set.of(1, 2), 5.0)    // оба жанра
            );
        }

        @Test
        @DisplayName("MMR меняет порядок: похожие фильмы разносятся, разнообразие продвигается")
        void mmrReordersSimilarMovies() {
            List<ScoredMovie> withoutMmr =
                    algorithm.generateRecommendations(participants, List.of(), candidates, false);
            List<ScoredMovie> withMmr =
                    algorithm.generateRecommendations(participants, List.of(), candidates, true);

            // Без MMR — просто сортировка (равные оценки -> по убыванию id)
            assertThat(withoutMmr).extracting(ScoredMovie::movieId).containsExactly(4, 3, 2, 1);

            // С MMR: после выбора фильма 4 ({1}) фильм 3 ({1}, sim=1) штрафуется сильнее,
            // первым выбирается непохожий фильм 2 ({2}), затем 1 ({1,2}), в конце 3.
            assertThat(withMmr).extracting(ScoredMovie::movieId).containsExactly(4, 2, 1, 3);
        }

        @Test
        @DisplayName("пул MMR ограничен параметром M: 60 кандидатов -> 50 результатов")
        void mmrPoolLimitedByM() {
            List<MovieCandidate> many = IntStream.rangeClosed(1, 60)
                    .mapToObj(id -> movie(id, Set.of(1), 5.0))
                    .toList();

            List<ScoredMovie> withoutMmr =
                    algorithm.generateRecommendations(participants, List.of(), many, false);
            List<ScoredMovie> withMmr =
                    algorithm.generateRecommendations(participants, List.of(), many, true);

            assertThat(withoutMmr).hasSize(60);
            assertThat(withMmr).hasSize(50);
        }
    }

    @Nested
    @DisplayName("calculateGroupProfile: агрегация предпочтений группы")
    class GroupProfileTest {

        @Test
        @DisplayName("null и пустой список участников -> пустой профиль")
        void nullOrEmptyParticipantsGiveEmptyProfile() {
            GroupProfile nullProfile = algorithm.calculateGroupProfile(null, List.of());
            GroupProfile emptyProfile = algorithm.calculateGroupProfile(List.of(), List.of());

            for (GroupProfile profile : List.of(nullProfile, emptyProfile)) {
                assertThat(profile.averageWeights()).isEmpty();
                assertThat(profile.vetoedGenreIds()).isEmpty();
                assertThat(profile.participantCount()).isZero();
                assertThat(profile.genreInfluenceCoefficients()).isEmpty();
            }
        }

        @Test
        @DisplayName("считает средние веса, вето и коэффициенты влияния")
        void computesAveragesVetoAndInfluence() {
            List<ParticipantPreference> participants = List.of(
                    participant(1, Map.of(1, 1.0, 2, -1.0)),
                    participant(2, Map.of(1, 0.5, 3, -1.0))
            );

            GroupProfile profile = algorithm.calculateGroupProfile(participants, List.of());

            assertThat(profile.participantCount()).isEqualTo(2);
            assertThat(profile.averageWeights())
                    .containsEntry(1, 0.75)
                    .containsEntry(2, -0.5)
                    .containsEntry(3, -0.5)
                    .hasSize(3);
            assertThat(profile.vetoedGenreIds()).containsExactlyInAnyOrder(2, 3);
            // единственный положительный жанр получает весь вес
            assertThat(profile.genreInfluenceCoefficients()).containsEntry(1, 1.0);
        }

        @Test
        @DisplayName("вето-жанр из контекста «сегодня» не считается вето")
        void vetoOnContextGenreIsIgnored() {
            List<ParticipantPreference> participants =
                    List.of(participant(1, Map.of(2, -1.0)));

            GroupProfile profile = algorithm.calculateGroupProfile(participants, List.of(2));

            assertThat(profile.vetoedGenreIds()).isEmpty();
            assertThat(profile.averageWeights()).containsEntry(2, -1.0);
            // отрицательный жанр не попадает в коэффициенты влияния
            assertThat(profile.genreInfluenceCoefficients()).isEmpty();
        }

        @Test
        @DisplayName("вето одного участника убирает жанр из коэффициентов даже при положительном среднем")
        void vetoKillsInfluenceDespitePositiveAverage() {
            List<ParticipantPreference> participants = List.of(
                    participant(1, Map.of(1, -1.0)),
                    participant(2, Map.of(1, 1.0)),
                    participant(3, Map.of(1, 1.0))
            );

            GroupProfile profile = algorithm.calculateGroupProfile(participants, List.of());

            assertThat(profile.averageWeights()).containsEntry(1, 1.0 / 3.0);
            assertThat(profile.vetoedGenreIds()).containsExactly(1);
            assertThat(profile.genreInfluenceCoefficients()).isEmpty();
        }

        @Test
        @DisplayName("нет положительных весов -> коэффициенты влияния пусты")
        void noPositiveWeightsGiveEmptyInfluence() {
            List<ParticipantPreference> participants =
                    List.of(participant(1, Map.of(1, -0.5, 2, 0.0)));

            GroupProfile profile = algorithm.calculateGroupProfile(participants, List.of());

            assertThat(profile.genreInfluenceCoefficients()).isEmpty();
        }
    }
}
