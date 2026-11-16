package com.copick.saved.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.copick.groups.entity.Group;
import com.copick.preferences.entity.Genre;
import com.copick.profiles.entity.Profile;
import com.copick.saved.entity.SavedMovie;

public record SavedMovieResponseDto(
    Integer savedId,
    Integer groupId,
    String externalId,
    String contentType,
    String title,
    String originalTitle,
    String posterUrl,
    String backdropUrl,
    Double rating,
    Integer releaseYear,
    String overview,
    String notes,
    Integer addedByProfileId,
    String addedByNickname,
    OffsetDateTime addedAt,
    List<String> genres
) {
    public static SavedMovieResponseDto fromEntity(SavedMovie movie) {
        List<String> genreNames = movie.getGenres() != null
            ? movie.getGenres().stream().map(Genre::getGenreName).toList()
            : List.of();

        Group group = movie.getGroup();
        Integer groupId = group != null ? movie.getGroup().getGroupId() : null;

        Profile addedBy = movie.getAddedBy();
        Integer addedById = addedBy != null ? movie.getAddedBy().getProfileId() : null;
        String addedByNick = addedBy != null ? movie.getAddedBy().getNickname() : null;

        return new SavedMovieResponseDto(
            movie.getSavedId(),
            groupId,
            movie.getExternalId(),
            movie.getContentType(),
            movie.getTitle(),
            movie.getOriginalTitle(),
            movie.getPosterUrl(),
            movie.getBackdropUrl(),
            movie.getRating(),
            movie.getReleaseYear(),
            movie.getOverview(),
            movie.getNotes(),
            addedById,
            addedByNick,
            movie.getAddedAt(),
            genreNames
        );
    }
}