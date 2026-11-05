package com.copick.groups.dto;

public record GroupCreateRequest(
        String name,
        Short ageRestriction
) {}