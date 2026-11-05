package com.copick.recommendations.dto;

import java.util.List;

public record SnapshotMovieResponse(
        String title,
        String originalTitle,
        String posterUrl,
        String backdropUrl,
        Double rating,
        Integer releaseYear,
        String overview,
        List<String> genres
) {}