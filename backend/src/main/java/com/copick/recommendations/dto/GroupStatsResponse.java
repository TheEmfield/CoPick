package com.copick.recommendations.dto;

import java.util.List;

public record GroupStatsResponse(
        int memberCount,
        List<GenreStat> topGenres,
        List<String> bannedGenres
) {}