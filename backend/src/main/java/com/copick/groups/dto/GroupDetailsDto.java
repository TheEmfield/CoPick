package com.copick.groups.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record GroupDetailsDto(
        Integer groupId,
        String name,
        String inviteCode,
        Short ageRestriction,
        Integer ownerId,
        OffsetDateTime createdAt,
        List<MemberDto> members
) {
    public record MemberDto(
            Integer profileId,
            String nickname,
            String role
    ) {}
}