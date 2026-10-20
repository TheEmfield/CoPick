package com.copick.algorithm.dto;

import java.Util.Map;

public record ParticipantPreferenceDto(
        int profileId,
        Map<Integer, Double> genreWeights
) {}