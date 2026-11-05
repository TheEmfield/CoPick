package com.copick.saved.dto;

import java.util.List;

public record SaveMovieRequest(
    String externalId,
    String contentType,
    String title,
    String originalTitle,
    String posterUrl,
    String backdropUrl,
    Double rating,
    Integer releaseYear,
    String overview,
    String notes,
    List<String> genres
) {}
