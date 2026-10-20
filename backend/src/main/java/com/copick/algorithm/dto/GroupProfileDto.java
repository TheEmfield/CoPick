package com.copick.algorithm.dto;

import java.util.Map;
import java.util.Set;

public record GroupProfileDto(
        Map<Integer, Double> averageWeights,
        Set<Integer> vetoedGenreIds,
        int participantCount
) {}