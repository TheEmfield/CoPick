package com.copick.profiles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.InvalidCredentialsException;
import com.copick.profiles.exception.InvalidNicknameException;
import com.copick.profiles.exception.InvalidPasswordException;
import com.copick.profiles.exception.NicknameAlreadyExistsException;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService: регистрация, вход и управление профилем")
class ProfileServiceTest {

    private static final Integer PROFILE_ID = 7;
    private static final String NICKNAME = "movie_lover";
    private static final String RAW_PASSWORD = "secret123";
    private static final String HASH = "$2a$10$hashhashhashhashhashhashhashhashhashhash";

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository, passwordEncoder);
    }

    private static Profile existingProfile() {
        Profile profile = new Profile();
        profile.setProfileId(PROFILE_ID);
        profile.setNickname(NICKNAME);
        profile.setPasswordHash(HASH);
        return profile;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("успешная регистрация: хэш пароля сохраняется")
        void registerSavesProfileWithHashedPassword() {
            when(profileRepository.existsByNickname(NICKNAME)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASH);
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
                Profile saved = invocation.getArgument(0);
                saved.setProfileId(PROFILE_ID);
                return saved;
            });

            Profile registered = profileService.register(NICKNAME, RAW_PASSWORD);

            assertThat(registered.getProfileId()).isEqualTo(PROFILE_ID);
            assertThat(registered.getNickname()).isEqualTo(NICKNAME);
            assertThat(registered.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("никнейм должен быть от 3 до 32 символов латиницей/цифрами/_")
        void rejectsInvalidNicknames() {
            assertInvalidNickname(null);
            assertInvalidNickname("  ");
            assertInvalidNickname("ab");                      // короче 3
            assertInvalidNickname("a".repeat(33));            // длиннее 32
            assertInvalidNickname("кириллица");               // не латиница
            assertInvalidNickname("user@mail");               // недопустимый символ

            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("пароль должен быть от 6 до 72 символов")
        void rejectsInvalidPasswords() {
            assertThatThrownBy(() -> profileService.register(NICKNAME, null))
                    .isInstanceOf(InvalidPasswordException.class);
            assertThatThrownBy(() -> profileService.register(NICKNAME, ""))
                    .isInstanceOf(InvalidPasswordException.class);
            assertThatThrownBy(() -> profileService.register(NICKNAME, "12345"))
                    .isInstanceOf(InvalidPasswordException.class);
            assertThatThrownBy(() -> profileService.register(NICKNAME, "x".repeat(73)))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("занятый никнейм -> NICKNAME_ALREADY_EXISTS")
        void rejectsTakenNickname() {
            when(profileRepository.existsByNickname(NICKNAME)).thenReturn(true);

            assertThatThrownBy(() -> profileService.register(NICKNAME, RAW_PASSWORD))
                    .isInstanceOf(NicknameAlreadyExistsException.class);

            verify(profileRepository, never()).save(any(Profile.class));
        }

        private void assertInvalidNickname(String nickname) {
            assertThatThrownBy(() -> profileService.register(nickname, RAW_PASSWORD))
                    .isInstanceOf(InvalidNicknameException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("успешный вход при совпадении пароля")
        void loginSucceedsWithCorrectPassword() {
            when(profileRepository.findByNickname(NICKNAME)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);

            Profile loggedIn = profileService.login(NICKNAME, RAW_PASSWORD);

            assertThat(loggedIn.getProfileId()).isEqualTo(PROFILE_ID);
        }

        @Test
        @DisplayName("неизвестный никнейм -> INVALID_CREDENTIALS")
        void loginFailsForUnknownNickname() {
            when(profileRepository.findByNickname("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.login("ghost", RAW_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("неверный пароль -> INVALID_CREDENTIALS")
        void loginFailsWithWrongPassword() {
            when(profileRepository.findByNickname(NICKNAME)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);

            assertThatThrownBy(() -> profileService.login(NICKNAME, "wrong"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("null-хэш в БД трактуется как неверный пароль")
        void loginFailsWithNullHash() {
            Profile noHash = existingProfile();
            noHash.setPasswordHash(null);
            when(profileRepository.findByNickname(NICKNAME)).thenReturn(Optional.of(noHash));

            assertThatThrownBy(() -> profileService.login(NICKNAME, RAW_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Чтение профилей")
    class Read {

        @Test
        @DisplayName("getProfile возвращает профиль или PROFILE_NOT_FOUND")
        void getProfileFindsOrThrows() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            assertThat(profileService.getProfile(PROFILE_ID).getNickname()).isEqualTo(NICKNAME);

            when(profileRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> profileService.getProfile(999))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateNickname / updatePassword / updateProfile")
    class Updates {

        @Test
        @DisplayName("смена никнейма на тот же — no-op без обращения к БД")
        void sameNicknameIsNoOp() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));

            Profile result = profileService.updateNickname(PROFILE_ID, NICKNAME);

            assertThat(result.getNickname()).isEqualTo(NICKNAME);
            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("смена никнейма сохраняет новый ник")
        void changesNickname() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(profileRepository.existsByNickname("new_nick")).thenReturn(false);
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Profile result = profileService.updateNickname(PROFILE_ID, "new_nick");

            assertThat(result.getNickname()).isEqualTo("new_nick");
        }

        @Test
        @DisplayName("смена на занятый никнейм -> NICKNAME_ALREADY_EXISTS")
        void changeToTakenNicknameThrows() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(profileRepository.existsByNickname("taken_nick")).thenReturn(true);

            assertThatThrownBy(() -> profileService.updateNickname(PROFILE_ID, "taken_nick"))
                    .isInstanceOf(NicknameAlreadyExistsException.class);
        }

        @Test
        @DisplayName("updatePassword: старый пароль обязателен и проверяется")
        void updatePasswordChecksOldPassword() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);

            assertThatThrownBy(() -> profileService.updatePassword(PROFILE_ID, "wrong", "newpass1"))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("updatePassword: успешная смена сохраняет новый хэш")
        void updatePasswordSavesNewHash() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);
            when(passwordEncoder.encode("newpass1")).thenReturn("new-hash");

            profileService.updatePassword(PROFILE_ID, RAW_PASSWORD, "newpass1");

            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("updateProfile: меняет и никнейм, и пароль за один вызов")
        void updateProfileChangesBothFields() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(profileRepository.existsByNickname("new_nick")).thenReturn(false);
            when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);
            when(passwordEncoder.encode("newpass1")).thenReturn("new-hash");
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Profile result = profileService.updateProfile(PROFILE_ID, "new_nick", RAW_PASSWORD, "newpass1");

            assertThat(result.getNickname()).isEqualTo("new_nick");
            assertThat(result.getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("updateProfile: null/blank поля не меняются")
        void updateProfileIgnoresBlankFields() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Profile result = profileService.updateProfile(PROFILE_ID, "  ", null, null);

            assertThat(result.getNickname()).isEqualTo(NICKNAME);
            assertThat(result.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("updateProfile: неверный старый пароль -> INVALID_CREDENTIALS")
        void updateProfileWithWrongOldPasswordThrows() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);

            assertThatThrownBy(() -> profileService.updateProfile(PROFILE_ID, null, "wrong", "newpass1"))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("updateProfile: короткий новый пароль -> INVALID_PASSWORD")
        void updateProfileWithWeakNewPasswordThrows() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);

            assertThatThrownBy(() -> profileService.updateProfile(PROFILE_ID, null, RAW_PASSWORD, "123"))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("updateProfile: занятый никнейм -> NICKNAME_ALREADY_EXISTS")
        void updateProfileWithTakenNicknameThrows() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(profileRepository.existsByNickname("taken_nick")).thenReturn(true);

            assertThatThrownBy(() -> profileService.updateProfile(PROFILE_ID, "taken_nick", null, null))
                    .isInstanceOf(NicknameAlreadyExistsException.class);
        }
    }

    @Test
    @DisplayName("deleteProfile удаляет существующий профиль")
    void deleteProfileRemovesIt() {
        Profile profile = existingProfile();
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        profileService.deleteProfile(PROFILE_ID);

        verify(profileRepository).delete(profile);
    }

    @Test
    @DisplayName("deleteProfile несуществующего -> PROFILE_NOT_FOUND")
    void deleteMissingProfileThrows() {
        when(profileRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteProfile(999))
                .isInstanceOf(ProfileNotFoundException.class);
    }
}
