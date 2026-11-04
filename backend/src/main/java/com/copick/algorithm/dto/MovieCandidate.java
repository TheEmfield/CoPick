package com.copick.algorithm.dto;

import java.util.Set;

public record MovieCandidate(
        int movieId,
        String title,
        Set<Integer> genreIds,
        double rating,
        int voteCount
) {}