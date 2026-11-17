package com.copick.saved.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.copick.groups.entity.Group;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedMovieService: сохранённые фильмы группы")
class SavedMovieServiceTest {

    private static final Integer GROUP_ID = 1;
    private static final Integer PROFILE_ID = 7;
    private static final Integer SAVED_ID = 100;

    @Mock
    private SavedMovieRepository savedMovieRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private GenreRepository genreRepository;

    private SavedMovieService savedMovieService;

    @BeforeEach
    void setUp() {
        savedMovieService = new SavedMovieService(
                savedMovieRepository, groupRepository, groupMemberRepository, profileRepository, genreRepository);
    }

    private static Group group() {
        Group group = new Group();
        group.setGroupId(GROUP_ID);
        group.setName("Кино по пятницам");
        return group;
    }

    private static Profile profile() {
        Profile profile = new Profile();
        profile.setProfileId(PROFILE_ID);
        profile.setNickname("movie_lover");
        return profile;
    }

    private static SaveMovieRequest request(List<String> genres) {
        return new SaveMovieRequest(
                "tmdb-42", "movie", "Интерстеллар", "Interstellar",
                "https://poster", "https://backdrop", 8.6, 2014,
                "Эпический sci-fi", "пересмотреть", genres);
    }

    private void stubGroupAndMembership() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(groupMemberRepository.existsById(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("addToSaved")
    class AddToSaved {

        @Test
        @DisplayName("несуществующая группа -> GROUP_NOT_FOUND")
        void unknownGroupThrows() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedMovieService.addToSaved(GROUP_ID, PROFILE_ID, request(null)))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("не-участник группы не может сохранять фильмы")
        void nonMemberCannotSave() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
            when(groupMemberRepository.existsById(any())).thenReturn(false);

            assertThatThrownBy(() -> savedMovieService.addToSaved(GROUP_ID, PROFILE_ID, request(null)))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("несуществующий профиль -> PROFILE_NOT_FOUND")
        void unknownProfileThrows() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
            when(groupMemberRepository.existsById(any())).thenReturn(true);
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedMovieService.addToSaved(GROUP_ID, PROFILE_ID, request(null)))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        @DisplayName("дубль по externalId в группе -> MOVIE_ALREADY_SAVED")
        void duplicateMovieThrows() {
            stubGroupAndMembership();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile()));
            when(savedMovieRepository.findByGroupGroupIdAndExternalIdAndContentType(GROUP_ID, "tmdb-42", "movie"))
                    .thenReturn(Optional.of(new SavedMovie()));

            assertThatThrownBy(() -> savedMovieService.addToSaved(GROUP_ID, PROFILE_ID, request(null)))
                    .isInstanceOf(MovieAlreadySavedException.class);

            verify(savedMovieRepository, never()).save(any(SavedMovie.class));
        }

        @Test
        @DisplayName("успешное сохранение: поля переносятся, жанры резолвятся по имени")
        void savesMovieWithResolvedGenres() {
            Profile profile = profile();
            stubGroupAndMembership();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(savedMovieRepository.findByGroupGroupIdAndExternalIdAndContentType(GROUP_ID, "tmdb-42", "movie"))
                    .thenReturn(Optional.empty());
            Genre action = new Genre();
            action.setGenreId(3);
            action.setGenreName("action");
            Genre drama = new Genre();
            drama.setGenreId(5);
            drama.setGenreName("drama");
            when(genreRepository.findByGenreName("action")).thenReturn(Optional.of(action));
            when(genreRepository.findByGenreName("drama")).thenReturn(Optional.of(drama));
            when(genreRepository.findByGenreName("unknown")).thenReturn(Optional.empty());
            when(savedMovieRepository.save(any(SavedMovie.class))).thenAnswer(invocation -> {
                SavedMovie saved = invocation.getArgument(0);
                saved.setSavedId(SAVED_ID);
                return saved;
            });

            SavedMovieResponseDto dto = savedMovieService.addToSaved(
                    GROUP_ID, PROFILE_ID, request(List.of("action", "unknown", "drama")));

            ArgumentCaptor<SavedMovie> captor = ArgumentCaptor.forClass(SavedMovie.class);
            verify(savedMovieRepository).save(captor.capture());
            SavedMovie saved = captor.getValue();

            assertThat(saved.getExternalId()).isEqualTo("tmdb-42");
            assertThat(saved.getContentType()).isEqualTo("movie");
            assertThat(saved.getTitle()).isEqualTo("Интерстеллар");
            assertThat(saved.getReleaseYear()).isEqualTo(2014);
            assertThat(saved.getAddedBy()).isSameAs(profile);
            assertThat(saved.getAddedAt()).isNotNull();
            assertThat(saved.getGenres()).containsExactlyInAnyOrder(action, drama);

            assertThat(dto.savedId()).isEqualTo(SAVED_ID);
            assertThat(dto.groupId()).isEqualTo(GROUP_ID);
            assertThat(dto.addedByNickname()).isEqualTo("movie_lover");
            assertThat(dto.genres()).containsExactlyInAnyOrder("action", "drama");
        }

        @Test
        @DisplayName("сохранение без списка жанров оставляет genres пустым")
        void savesMovieWithoutGenres() {
            stubGroupAndMembership();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile()));
            when(savedMovieRepository.findByGroupGroupIdAndExternalIdAndContentType(GROUP_ID, "tmdb-42", "movie"))
                    .thenReturn(Optional.empty());
            when(savedMovieRepository.save(any(SavedMovie.class))).thenAnswer(invocation -> {
                SavedMovie saved = invocation.getArgument(0);
                saved.setSavedId(SAVED_ID);
                return saved;
            });

            SavedMovieResponseDto dto = savedMovieService.addToSaved(GROUP_ID, PROFILE_ID, request(List.of()));

            assertThat(dto.genres()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSavedMovies / removeFromSaved")
    class ReadAndRemove {

        @Test
        @DisplayName("список сохранённых фильмов группы")
        void returnsSavedMoviesForMember() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
            when(groupMemberRepository.existsById(any())).thenReturn(true);

            SavedMovie saved = new SavedMovie();
            saved.setSavedId(SAVED_ID);
            saved.setGroup(group());
            saved.setExternalId("tmdb-42");
            saved.setContentType("movie");
            saved.setTitle("Интерстеллар");
            saved.setAddedBy(profile());
            saved.setGenres(Set.of());
            when(savedMovieRepository.findByGroupIdWithDetails(GROUP_ID)).thenReturn(List.of(saved));

            List<SavedMovieResponseDto> result = savedMovieService.getSavedMovies(GROUP_ID, PROFILE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Интерстеллар");
        }

        @Test
        @DisplayName("getSavedMovies для несуществующей группы -> GROUP_NOT_FOUND")
        void getSavedMoviesForUnknownGroupThrows() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(false);

            assertThatThrownBy(() -> savedMovieService.getSavedMovies(GROUP_ID, PROFILE_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("getSavedMovies не-участником -> MEMBER_NOT_FOUND")
        void getSavedMoviesByNonMemberThrows() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
            when(groupMemberRepository.existsById(any())).thenReturn(false);

            assertThatThrownBy(() -> savedMovieService.getSavedMovies(GROUP_ID, PROFILE_ID))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("removeFromSaved удаляет фильм группы")
        void removesSavedMovie() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
            when(groupMemberRepository.existsById(any())).thenReturn(true);
            SavedMovie saved = new SavedMovie();
            when(savedMovieRepository.findByIdAndGroupId(SAVED_ID, GROUP_ID)).thenReturn(Optional.of(saved));

            savedMovieService.removeFromSaved(GROUP_ID, SAVED_ID, PROFILE_ID);

            verify(savedMovieRepository).delete(saved);
        }

        @Test
        @DisplayName("removeFromSaved чужого файла -> SAVED_MOVIE_NOT_FOUND")
        void removeUnknownSavedMovieThrows() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
            when(groupMemberRepository.existsById(any())).thenReturn(true);
            when(savedMovieRepository.findByIdAndGroupId(SAVED_ID, GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedMovieService.removeFromSaved(GROUP_ID, SAVED_ID, PROFILE_ID))
                    .isInstanceOf(SavedMovieNotFoundException.class);

            verify(savedMovieRepository, never()).delete(any(SavedMovie.class));
        }

        @Test
        @DisplayName("removeFromSaved не-участником -> MEMBER_NOT_FOUND")
        void removeByNonMemberThrows() {
            when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
            when(groupMemberRepository.existsById(any())).thenReturn(false);

            assertThatThrownBy(() -> savedMovieService.removeFromSaved(GROUP_ID, SAVED_ID, PROFILE_ID))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}
