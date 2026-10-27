package com.copick.algorithm.service;

import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.*;

import java.util.*;
import java.util.stream.Collectors;

public class CoPickAlgorithm {

    private final AlgorithmConfig config;

    public CoPickAlgorithm(AlgorithmConfig config) {
        this.config = config;
    }

    public CoPickAlgorithm() {
        this(new AlgorithmConfig());
    }

    public List<ScoredMovie> generateRecommendations(
            List<ParticipantPreference> participants,
            List<Integer> contextGenreIds,
            List<MovieCandidate> candidates,
            boolean useMmr
    ) {
        GroupProfile groupProfile = calculateGroupProfile(participants, contextGenreIds);

        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<Integer> contextSet = new HashSet<>(contextGenreIds != null ? contextGenreIds : List.of());

        Map<Integer, Double> groupScores = new HashMap<>();
        for (MovieCandidate movie : candidates) {
            List<Double> personalScores = new ArrayList<>();
            for (ParticipantPreference user : participants) {
                personalScores.add(calculatePersonalScore(movie, user, contextSet));
            }
            groupScores.put(movie.movieId(), calculateGroupScore(personalScores));
        }

        List<ScoredMovie> afterFinal = calculateFinalScore(candidates, groupScores, contextSet);

        if (useMmr) {
            return diversify(afterFinal);
        } else {
            return afterFinal.stream().limit(config.K()).collect(Collectors.toList());
        }
    }

    public GroupProfile calculateGroupProfile(
            List<ParticipantPreference> participants,
            List<Integer> contextGenreIds
    ) {
        if (participants == null || participants.isEmpty()) {
            return new GroupProfile(Map.of(), Set.of(), 0, Map.of());
        }

        Map<Integer, Double> sumWeights = new HashMap<>();
        Set<Integer> vetoedGenres = new HashSet<>();
        int n = participants.size();

        for (ParticipantPreference p : participants) {
            for (Map.Entry<Integer, Double> entry : p.genreWeights().entrySet()) {
                int genreId = entry.getKey();
                double weight = entry.getValue();

                sumWeights.merge(genreId, weight, Double::sum);

                if (weight == -1.0 && !contextGenreIds.contains(genreId)) {
                    vetoedGenres.add(genreId);
                }
            }
        }

        Map<Integer, Double> averageWeights = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : sumWeights.entrySet()) {
            averageWeights.put(entry.getKey(), entry.getValue() / n);
        }

        Map<Integer, Double> influenceCoefficients = getIntegerDoubleMap(averageWeights, vetoedGenres);

        return new GroupProfile(averageWeights, vetoedGenres, n, influenceCoefficients);
    }

    private double calculatePersonalScore(
            MovieCandidate movie,
            ParticipantPreference user,
            Set<Integer> contextGenreIds
    ) {
        int count = movie.genreIds().size();
        if (count == 0) return 0.0;

        double sum = 0.0;
        double minWeight = 0.0;

        for (int genreId : movie.genreIds()) {
            double weight = user.genreWeights().getOrDefault(genreId, 0.0);

            if (weight == -1.0) {
                if (!contextGenreIds.contains(genreId)) {
                    return -1.0;
                }
                weight = -config.kappa();
            }

            sum += weight;
            if (weight < minWeight) {
                minWeight = weight;
            }
        }

        double mean = sum / count;
        double penalty = config.lambda() * Math.min(0, minWeight);
        return Math.max(-1.0, mean + penalty);

    }

    private double calculateGroupScore(List<Double> personalScores) {
        if (personalScores == null || personalScores.isEmpty()) {
            return 0.0;
        }

        int n = personalScores.size();

        List<Double> sorted = new ArrayList<>(personalScores);
        Collections.sort(sorted);

        double mean = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        int k = Math.max(1, n / 4);

        double lowmeank = sorted.stream()
                .limit(k)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return config.alpha() * mean + (1.0 - config.alpha()) * lowmeank;
    }

    private List<ScoredMovie> calculateFinalScore(
            List<MovieCandidate> candidates,
            Map<Integer, Double> groupScores,
            Set<Integer> contextGenreIds
    ) {
        Set<Integer> contextSet = new HashSet<>(contextGenreIds != null ? contextGenreIds : List.of());

        List<ScoredMovie> scoredMovies = new ArrayList<>();

        for (MovieCandidate movie : candidates) {
            double g = groupScores.getOrDefault(movie.movieId(), 0.0);

            boolean hasContext = !Collections.disjoint(movie.genreIds(), contextSet);
            double ctx = hasContext ? 1.0 : 0.0;

            double q = 0.0;
            if (movie.voteCount() >= 100) {
                q = (movie.rating() - 5.0) / 10.0;
            }

            double finalScore = g + config.beta() * ctx + config.rho() * q;

            scoredMovies.add(new ScoredMovie(movie.movieId(), finalScore, movie.genreIds(), q));
        }

        scoredMovies.sort(Comparator
                .comparing(ScoredMovie::finalScore)
                .thenComparing(ScoredMovie::qualityScore)
                .thenComparingInt(ScoredMovie::movieId).reversed()
        );

        return scoredMovies;
    }

    private List<ScoredMovie> diversify(List<ScoredMovie> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<ScoredMovie> pool = candidates.stream().limit(config.M()).toList();

        double maxFinal = pool.stream().mapToDouble(ScoredMovie::finalScore).max().orElse(0.0);
        double minFinal = pool.stream().mapToDouble(ScoredMovie::finalScore).min().orElse(0.0);
        double range = maxFinal - minFinal;

        Map<Integer, Double> normalizedScores = new HashMap<>();
        for (ScoredMovie movie : pool) {
            double fHat = range > 0 ? (movie.finalScore() - minFinal) / range : 0.0;
            normalizedScores.put(movie.movieId(), fHat);
        }

        List<ScoredMovie> selected = new ArrayList<>();
        Set<Integer> selectedIds = new HashSet<>();
        Map<Integer, Double> ms = new HashMap<>();

        for (ScoredMovie movie : pool) {
            ms.put(movie.movieId(), 0.0);
        }

        while (selected.size() < config.K() && selected.size() < pool.size()) {
            ScoredMovie bestMovie = null;
            double bestMmr = Double.NEGATIVE_INFINITY;

            for (ScoredMovie movie : pool) {
                if (selectedIds.contains(movie.movieId())) continue;

                double fHat = normalizedScores.get(movie.movieId());
                double currentMs = ms.get(movie.movieId());

                double mmr = config.mu() * fHat - (1.0 - config.mu()) * currentMs;

                if (mmr > bestMmr) {
                    bestMmr = mmr;
                    bestMovie = movie;
                }
            }

            if (bestMovie == null) break;

            selected.add(bestMovie);
            selectedIds.add(bestMovie.movieId());

            for (ScoredMovie remaining : pool) {
                if (selectedIds.contains(remaining.movieId())) continue;

                double sim = jaccardSimilarity(bestMovie.genreIds(), remaining.genreIds());
                double currentMs = ms.get(remaining.movieId());
                ms.put(remaining.movieId(), Math.max(currentMs, sim));
            }
        }

        return selected;
    }

    private static Map<Integer, Double> getIntegerDoubleMap(Map<Integer, Double> averageWeights, Set<Integer> vetoedGenres) {
        Map<Integer, Double> influenceCoefficients = new HashMap<>();
        double positiveSum = 0.0;

        for (Map.Entry<Integer, Double> entry : averageWeights.entrySet()) {
            if (entry.getValue() > 0 && !vetoedGenres.contains(entry.getKey())) {
                positiveSum += entry.getValue();
            }
        }

        if (positiveSum > 0) {
            for (Map.Entry<Integer, Double> entry : averageWeights.entrySet()) {
                int genreId = entry.getKey();
                double weight = entry.getValue();

                if (weight > 0 && !vetoedGenres.contains(genreId)) {
                    influenceCoefficients.put(genreId, weight / positiveSum);
                }
            }
        }
        return influenceCoefficients;
    }

    private double jaccardSimilarity(Set<Integer> setA, Set<Integer> setB) {
        if (setA.isEmpty() && setB.isEmpty()) return 0.0;

        Set<Integer> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<Integer> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }
}