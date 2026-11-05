package com.copick.preferences.dto;

import java.util.List;

public record UpdatePreferenceRequest(List<PreferenceUpdateDto> preferences) {}
