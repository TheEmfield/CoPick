package com.copick.algorithm.dto;

import java.util.Set;

public record ScoredMovie(
        int movieId,
        double finalScore,
        Set<Integer> genreIds,
        double qualityScore
) {}
