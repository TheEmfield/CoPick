package com.copick.algorithm.service;

import com.copick.algorithm.config.AlgorithmConfig;
import com.copick.algorithm.dto.*;

import java.util.*;

public class CoPickAlgorithm {

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
}