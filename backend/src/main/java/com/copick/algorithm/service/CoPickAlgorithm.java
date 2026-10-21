package com.copick.algorithm.service;

import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.*;

import java.util.*;
import java.util.stream.Collectors;

public class CoPickAlgorithm {

    public List<ScoredMovie> generateRecommendations(
            List<ParticipantPreference> participants,
            List<Integer> contextGenreIds,
            List<MovieCandidate> apiCandidates,
            AlgorithmConfig config,
            boolean useMmr
    ) {
        GroupProfile groupProfile = calculateGroupProfile(participants);

        List<MovieCandidate> afterVeto = applyVeto(apiCandidates, groupProfile.vetoedGenreIds());
        if (afterVeto.isEmpty()) {
            return List.of();
        }

        Map<Integer, Double> groupScores = new HashMap<>();
        for (MovieCandidate movie : afterVeto) {
            List<Double> personalScores = new ArrayList<>();
            for (ParticipantPreference user : participants) {
                personalScores.add(calculatePersonalScore(movie, user, config.lambda()));
            }
            groupScores.put(movie.movieId(), calculateGroupScore(personalScores, config.alpha()));
        }

        List<ScoredMovie> afterFinal = calculateFinalScore(
                afterVeto, groupScores, contextGenreIds, config.beta(), config.rho()
        );

        if (useMmr) {
            return diversify(afterFinal, config.M(), config.K(), config.mu());
        } else {
            return afterFinal.stream().limit(config.K()).collect(Collectors.toList());
        }
    }

    public GroupProfile calculateGroupProfile(List<ParticipantPreference> participants) {
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

                if (weight == -1.0) {
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

    public List<MovieCandidate> applyVeto(List<MovieCandidate> candidates, Set<Integer> vetoedGenreIds) {
        if (candidates == null || vetoedGenreIds == null || vetoedGenreIds.isEmpty()) {
            return candidates;
        }

        return candidates.stream()
                .filter(movie -> Collections.disjoint(movie.genreIds(), vetoedGenreIds))
                .toList();
    }

    public double calculatePersonalScore(MovieCandidate movie, ParticipantPreference user, double lambda) {
        for (int genreId : movie.genreIds()) {
            if (user.genreWeights().getOrDefault(genreId, 0.0) == -1.0) {
                return -1.0;
            }
        }

        double sum = 0.0;
        double minWeight = 0.0;
        int count = movie.genreIds().size();
        if (count == 0) return 0.0;

        for (int genreId : movie.genreIds()) {
            double weight = user.genreWeights().getOrDefault(genreId, 0.0);
            sum += weight;
            if (weight < minWeight) {
                minWeight = weight;
            }
        }

        double mean = sum / count;
        double penalty = lambda * Math.min(0, minWeight);
        double rawScore = mean + penalty;

        return Math.max(-1.0, Math.min(1.0, rawScore));
    }

    public double calculateGroupScore(List<Double> personalScores, double alpha) {
        if (personalScores == null || personalScores.isEmpty()) {
            return 0.0;
        }

        int n = personalScores.size();

        List<Double> sorted = new ArrayList<>(personalScores);
        Collections.sort(sorted);

        double mean = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        int k = Math.max(1, n / 4);

        double lowmeanK = sorted.stream()
                .limit(k)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return alpha * mean + (1.0 - alpha) * lowmeanK;
    }

    public List<ScoredMovie> calculateFinalScore(
            List<MovieCandidate> candidates,
            Map<Integer, Double> groupScores,
            List<Integer> contextGenreIds,
            double beta,
            double rho
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

            double finalScore = g + beta * ctx + rho * q;

            scoredMovies.add(new ScoredMovie(movie.movieId(), finalScore, movie.genreIds(), q));
        }

        scoredMovies.sort(Comparator
                .comparing(ScoredMovie::finalScore).reversed()
                .thenComparing(ScoredMovie::qualityScore).reversed()
                .thenComparingInt(ScoredMovie::movieId)
        );

        return scoredMovies;
    }

    public List<ScoredMovie> diversify(
            List<ScoredMovie> candidates,
            int M,
            int K,
            double mu
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<ScoredMovie> pool = candidates.stream().limit(M).collect(Collectors.toList());

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

        while (selected.size() < K && selected.size() < pool.size()) {
            ScoredMovie bestMovie = null;
            double bestMmr = Double.NEGATIVE_INFINITY;

            for (ScoredMovie movie : pool) {
                if (selectedIds.contains(movie.movieId())) continue;

                double fHat = normalizedScores.get(movie.movieId());
                double currentMs = ms.get(movie.movieId());

                double mmr = mu * fHat - (1.0 - mu) * currentMs;

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