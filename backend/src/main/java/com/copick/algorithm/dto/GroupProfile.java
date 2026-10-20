package com.copick.algorithm.dto;

import java.util.Map;
import java.util.Set;

public record GroupProfile(
        Map<Integer, Double> averageWeights,
        Set<Integer> vetoedGenreIds,
        int participantCount,
        Map<Integer, Double> genreInfluenceCoefficients
) {}