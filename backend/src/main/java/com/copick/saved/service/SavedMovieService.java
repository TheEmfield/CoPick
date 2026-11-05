package com.copick.saved.service;

import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMemberId;
import com.copick.groups.exception.GroupNotFoundException;
import com.copick.groups.exception.MemberNotFoundException;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.groups.repository.GroupRepository;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.repository.GenreRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import com.copick.saved.dto.SaveMovieRequest;
import com.copick.saved.dto.SavedMovieResponseDto;
import com.copick.saved.entity.SavedMovie;
import com.copick.saved.exception.MovieAlreadySavedException;
import com.copick.saved.exception.SavedMovieNotFoundException;
import com.copick.saved.repository.SavedMovieRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedMovieService {

    private final SavedMovieRepository savedMovieRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ProfileRepository profileRepository;
    private final GenreRepository genreRepository;

    @Transactional
    public SavedMovieResponseDto addToSaved(Integer groupId, Integer profileId, SaveMovieRequest request) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));

        checkGroupMembership(groupId, profileId);

        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));

        savedMovieRepository.findByGroupGroupIdAndExternalIdAndContentType(
            groupId, request.externalId(), request.contentType()
        ).ifPresent(existing -> {
            throw new MovieAlreadySavedException(request.externalId(), groupId);
        });

        SavedMovie savedMovie = new SavedMovie();
        savedMovie.setGroup(group);
        savedMovie.setAddedBy(profile);
        savedMovie.setExternalId(request.externalId());
        savedMovie.setContentType(request.contentType());
        savedMovie.setTitle(request.title());
        savedMovie.setOriginalTitle(request.originalTitle());
        savedMovie.setPosterUrl(request.posterUrl());
        savedMovie.setBackdropUrl(request.backdropUrl());
        savedMovie.setRating(request.rating());
        savedMovie.setReleaseYear(request.releaseYear());
        savedMovie.setOverview(request.overview());
        savedMovie.setNotes(request.notes());
        savedMovie.setAddedAt(OffsetDateTime.now());

        if (request.genres() != null && !request.genres().isEmpty()) {
            Set<Genre> resolvedGenres = new HashSet<>();
            for (String genreName : request.genres()) {
                genreRepository.findByGenreName(genreName).ifPresent(resolvedGenres::add);
            }
            savedMovie.setGenres(resolvedGenres);
        }

        SavedMovie saved = savedMovieRepository.save(savedMovie);
        log.info("Фильм '{}' (savedId={}) сохранен в группу {} пользователем {}",
            saved.getTitle(), saved.getSavedId(), groupId, profileId);

        return SavedMovieResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedMovieResponseDto> getSavedMovies(Integer groupId, Integer profileId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GroupNotFoundException(groupId);
        }
        checkGroupMembership(groupId, profileId);

        return savedMovieRepository.findByGroupIdWithDetails(groupId).stream()
            .map(SavedMovieResponseDto::fromEntity)
            .toList();
    }

    @Transactional
    public void removeFromSaved(Integer groupId, Integer savedId, Integer profileId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GroupNotFoundException(groupId);
        }
        checkGroupMembership(groupId, profileId);

        SavedMovie savedMovie = savedMovieRepository.findByIdAndGroupId(savedId, groupId)
            .orElseThrow(() -> new SavedMovieNotFoundException(savedId, groupId));

        savedMovieRepository.delete(savedMovie);
        log.info("Элемент с savedId={} удалён из группы {} пользователем {}", savedId, groupId, profileId);
    }

    private void checkGroupMembership(Integer groupId, Integer profileId) {
        if (!groupMemberRepository.existsById(new GroupMemberId(groupId, profileId))) {
            throw new MemberNotFoundException(groupId, profileId);
        }
    }
}