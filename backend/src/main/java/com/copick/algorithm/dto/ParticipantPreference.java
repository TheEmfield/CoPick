package com.copick.algorithm.dto;

import java.util.Map;

public record ParticipantPreference(
        int profileId,
        Map<Integer, Double> genreWeights
) {}