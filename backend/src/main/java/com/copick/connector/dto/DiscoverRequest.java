package com.copick.connector.dto;

import java.util.List;

public record DiscoverRequest(
        List<GenrePreference> preferences,
        List<String> excluded_genres
) {}