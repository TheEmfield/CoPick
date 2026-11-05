package com.copick.recommendations.dto;

public record GenreStat(
        String genreName,
        double avgWeight,
        double minWeight
) {}