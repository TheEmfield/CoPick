package com.copick.groups.dto;

public record GroupListItemDto(
        Integer groupId,
        String name,
        String inviteCode,
        String role,
        Integer memberCount
) {}