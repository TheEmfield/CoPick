package com.copick.preferences.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.copick.preferences.dto.PreferenceResponseDto;
import com.copick.preferences.dto.PreferenceUpdateDto;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.entity.PreferenceHistory;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import com.copick.preferences.exception.GenreNotFoundException;
import com.copick.preferences.exception.InvalidWeightException;
import com.copick.preferences.repository.GenreRepository;
import com.copick.preferences.repository.PreferenceHistoryRepository;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreferenceService: управление жанровыми предпочтениями")
class PreferenceServiceTest {

    private static final Integer PROFILE_ID = 7;
    private static final Integer GENRE_ID = 3;

    @Mock
    private ProfilePreferenceRepository preferenceRepository;

    @Mock
    private PreferenceHistoryRepository historyRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private GenreRepository genreRepository;

    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService =
                new PreferenceService(preferenceRepository, historyRepository, profileRepository, genreRepository);
    }

    private static Genre genre(int id, String name) {
        Genre genre = new Genre();
        genre.setGenreId(id);
        genre.setGenreName(name);
        return genre;
    }

    private static ProfilePreference preference(Integer profileId, Integer genreId, Double weight) {
        ProfilePreference pref = new ProfilePreference();
        pref.setId(new ProfilePreferenceId(profileId, genreId));
        pref.setProfile(new Profile());
        pref.getProfile().setProfileId(profileId);
        pref.setGenre(genre(genreId, "genre-" + genreId));
        pref.setWeight(weight);
        return pref;
    }

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {

        @Test
        @DisplayName("несуществующий профиль -> PROFILE_NOT_FOUND")
        void unknownProfileThrows() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(false);

            assertThatThrownBy(() -> preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 0.5))))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        @DisplayName("пустой список обновлений возвращает текущие предпочтения")
        void emptyUpdatesReturnCurrentPreferences() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(preferenceRepository.findByProfileIdWithGenre(PROFILE_ID))
                    .thenReturn(List.of(preference(PROFILE_ID, GENRE_ID, 0.5)));

            List<PreferenceResponseDto> result =
                    preferenceService.updatePreferences(PROFILE_ID, List.of());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).genreId()).isEqualTo(GENRE_ID);
            verify(preferenceRepository, never()).save(any(ProfilePreference.class));
        }

        @Test
        @DisplayName("вес вне [-1, 1] или null -> INVALID_WEIGHT")
        void rejectsInvalidWeights() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);

            assertThatThrownBy(() -> preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 1.5))))
                    .isInstanceOf(InvalidWeightException.class);
            assertThatThrownBy(() -> preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, -1.01))))
                    .isInstanceOf(InvalidWeightException.class);
            assertThatThrownBy(() -> preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, null))))
                    .isInstanceOf(InvalidWeightException.class);
        }

        @Test
        @DisplayName("неизвестный жанр -> GENRE_NOT_FOUND")
        void unknownGenreThrows() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(genreRepository.findAllById(anyCollection())).thenReturn(List.of());

            assertThatThrownBy(() -> preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 0.5))))
                    .isInstanceOf(GenreNotFoundException.class);

            verify(preferenceRepository, never()).save(any(ProfilePreference.class));
        }

        @Test
        @DisplayName("новое предпочтение создаётся и попадает в историю (oldWeight = null)")
        void createsNewPreferenceAndWritesHistory() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(genreRepository.findAllById(anyCollection())).thenReturn(List.of(genre(GENRE_ID, "action")));
            when(preferenceRepository.findById(new ProfilePreferenceId(PROFILE_ID, GENRE_ID)))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any(ProfilePreference.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(preferenceRepository.findByProfileIdWithGenre(PROFILE_ID)).thenReturn(List.of());

            preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 0.8)));

            ArgumentCaptor<ProfilePreference> prefCaptor = ArgumentCaptor.forClass(ProfilePreference.class);
            verify(preferenceRepository).save(prefCaptor.capture());
            assertThat(prefCaptor.getValue().getWeight()).isEqualTo(0.8);
            assertThat(prefCaptor.getValue().getId())
                    .isEqualTo(new ProfilePreferenceId(PROFILE_ID, GENRE_ID));

            ArgumentCaptor<PreferenceHistory> historyCaptor = ArgumentCaptor.forClass(PreferenceHistory.class);
            verify(historyRepository).save(historyCaptor.capture());
            PreferenceHistory history = historyCaptor.getValue();
            assertThat(history.getOldWeight()).isNull();
            assertThat(history.getNewWeight()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("изменение существующего предпочтения пишется в историю")
        void updatesExistingPreferenceAndWritesHistory() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(genreRepository.findAllById(anyCollection())).thenReturn(List.of(genre(GENRE_ID, "action")));
            ProfilePreference existing = preference(PROFILE_ID, GENRE_ID, 0.5);
            when(preferenceRepository.findById(new ProfilePreferenceId(PROFILE_ID, GENRE_ID)))
                    .thenReturn(Optional.of(existing));
            when(preferenceRepository.save(any(ProfilePreference.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(preferenceRepository.findByProfileIdWithGenre(PROFILE_ID)).thenReturn(List.of());

            preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 0.9)));

            assertThat(existing.getWeight()).isEqualTo(0.9);

            ArgumentCaptor<PreferenceHistory> historyCaptor = ArgumentCaptor.forClass(PreferenceHistory.class);
            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getOldWeight()).isEqualTo(0.5);
            assertThat(historyCaptor.getValue().getNewWeight()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("тот же вес не вызывает ни сохранения, ни истории")
        void sameWeightSkipsSaveAndHistory() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(genreRepository.findAllById(anyCollection())).thenReturn(List.of(genre(GENRE_ID, "action")));
            when(preferenceRepository.findById(new ProfilePreferenceId(PROFILE_ID, GENRE_ID)))
                    .thenReturn(Optional.of(preference(PROFILE_ID, GENRE_ID, 0.5)));
            when(preferenceRepository.findByProfileIdWithGenre(PROFILE_ID)).thenReturn(List.of());

            preferenceService.updatePreferences(
                    PROFILE_ID, List.of(new PreferenceUpdateDto(GENRE_ID, 0.5)));

            verify(preferenceRepository, never()).save(any(ProfilePreference.class));
            verify(historyRepository, never()).save(any(PreferenceHistory.class));
        }
    }

    @Nested
    @DisplayName("Чтение и удаление предпочтений")
    class ReadAndDelete {

        @Test
        @DisplayName("getPreferences мапит сущности в DTO с именами жанров")
        void getPreferencesMapsToDto() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);
            when(preferenceRepository.findByProfileIdWithGenre(PROFILE_ID))
                    .thenReturn(List.of(preference(PROFILE_ID, GENRE_ID, 0.7)));

            List<PreferenceResponseDto> result = preferenceService.getPreferences(PROFILE_ID);

            assertThat(result).hasSize(1);
            PreferenceResponseDto dto = result.get(0);
            assertThat(dto.genreId()).isEqualTo(GENRE_ID);
            assertThat(dto.genreName()).isEqualTo("genre-" + GENRE_ID);
            assertThat(dto.weight()).isEqualTo(0.7);
        }

        @Test
        @DisplayName("getPreferences несуществующего профиля -> PROFILE_NOT_FOUND")
        void getPreferencesForUnknownProfileThrows() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(false);

            assertThatThrownBy(() -> preferenceService.getPreferences(PROFILE_ID))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        @DisplayName("deletePreference проверяет профиль и удаляет запись")
        void deletePreferenceChecksAndDeletes() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);

            preferenceService.deletePreference(PROFILE_ID, GENRE_ID);

            verify(preferenceRepository).deleteByProfileIdAndGenreId(PROFILE_ID, GENRE_ID);
        }

        @Test
        @DisplayName("deleteAllPreferences удаляет всё и проверяет профиль")
        void deleteAllPreferencesChecksAndDeletes() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(true);

            preferenceService.deleteAllPreferences(PROFILE_ID);

            verify(preferenceRepository).deleteAllByProfileId(PROFILE_ID);
        }

        @Test
        @DisplayName("delete-операции несуществующего профиля -> PROFILE_NOT_FOUND")
        void deleteForUnknownProfileThrows() {
            when(profileRepository.existsById(PROFILE_ID)).thenReturn(false);

            assertThatThrownBy(() -> preferenceService.deletePreference(PROFILE_ID, GENRE_ID))
                    .isInstanceOf(ProfileNotFoundException.class);
            assertThatThrownBy(() -> preferenceService.deleteAllPreferences(PROFILE_ID))
                    .isInstanceOf(ProfileNotFoundException.class);

            verify(preferenceRepository, never()).deleteByProfileIdAndGenreId(any(), any());
            verify(preferenceRepository, never()).deleteAllByProfileId(any());
        }
    }
}
