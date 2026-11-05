package com.copick.connector.dto;

import java.util.List;

public record ConnectorMovie(
        int id,
        String title,
        String original_title,
        String poster_url,
        String backdrop_url,
        double rating,
        int release_year,
        List<String> genres,
        String overview,
        String requested_genre
) {}