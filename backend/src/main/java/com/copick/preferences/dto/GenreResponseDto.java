package com.copick.preferences.dto;

import com.copick.preferences.entity.Genre;

public record GenreResponseDto(Integer genreId, String genreName, Integer contentType) {
    public static GenreResponseDto fromEntity(Genre genre) {
        return new GenreResponseDto(
            genre.getGenreId(), genre.getGenreName(), genre.getContentType().getTypeId());
    }
}
