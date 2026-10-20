package com.copick.algorithm.dto;

import java.util.Set;

public record MovieCandidateDto(
        int movieId,
        String title,
        Set<Integer> genreIds,
        double rating,
        int voteCount
) {}