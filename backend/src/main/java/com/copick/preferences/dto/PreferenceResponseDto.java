package com.copick.preferences.dto;

import com.copick.preferences.entity.ProfilePreference;
import java.time.OffsetDateTime;

public record PreferenceResponseDto(
    Integer genreId, String genreName, Double weight, OffsetDateTime updatedAt) {
    public static PreferenceResponseDto fromEntity(ProfilePreference pref) {
        return new PreferenceResponseDto(
            pref.getGenre().getGenreId(),
            pref.getGenre().getGenreName(),
            pref.getWeight(),
            pref.getUpdatedAt());
    }
}
