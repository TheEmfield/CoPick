package com.copick.auth.dto;

import java.time.OffsetDateTime;
import com.copick.profiles.entity.Profile;

public record ProfileDto(Integer profileId, String nickname, OffsetDateTime createdAt) {
    public static ProfileDto fromEntity(Profile profile) {
        return new ProfileDto(profile.getProfileId(), profile.getNickname(), profile.getCreatedAt());
    }
}
