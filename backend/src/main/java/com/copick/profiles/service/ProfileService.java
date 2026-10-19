package com.copick.profiles.service;

import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.InvalidCredentialsException;
import com.copick.profiles.exception.InvalidNicknameException;
import com.copick.profiles.exception.InvalidPasswordException;
import com.copick.profiles.exception.NicknameAlreadyExistsException;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    // Nickname: 3-32 символа, латиница/цифры/подчёркивание.
    private static final int NICKNAME_MIN_LENGTH = 3;
    private static final int NICKNAME_MAX_LENGTH = 32;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private static final String ROLE_USER = "ROLE_USER";

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    /** Регистрация нового профиля. */
    @Transactional
    public Profile register(String nickname, String rawPassword) {
        validateNickname(nickname);
        validatePassword(rawPassword);

        if (profileRepository.existsByNickname(nickname)) {
            throw new NicknameAlreadyExistsException(nickname);
        }

        Profile profile = new Profile();
        profile.setNickname(nickname);
        profile.setPasswordHash(passwordEncoder.encode(rawPassword));

        profile = profileRepository.save(profile);
        log.info("Зарегистрирован новый профиль: id={}, nickname={}", profile.getProfileId(), nickname);
        return profile;
    }

    /**
    * Аутентификация по nickname + паролю.
    * @return профиль аутентифицированного пользователя
    * @throws InvalidCredentialsException если nickname не найден ИЛИ пароль неверный
    */
    @Transactional(readOnly = true)
    public Profile login(String nickname, String rawPassword) {
        Profile profile =
            profileRepository.findByNickname(nickname).orElseThrow(InvalidCredentialsException::new);

        if (profile.getPasswordHash() == null
            || !passwordEncoder.matches(rawPassword, profile.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        setAuthentication(profile);
        log.info("Успешный вход: profileId={}, nickname={}", profile.getProfileId(), nickname);
        return profile;
    }

    /**
    * Получить профиль по ID. Используется, например, для отображения чужого профиля участникам
    * группы.
    */
    @Transactional(readOnly = true)
    public Profile getProfile(Integer profileId) {
        return profileRepository
            .findById(profileId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));
    }

    /** Найти профиль по nickname. */
    @Transactional(readOnly = true)
    public Optional<Profile> findByNickname(String nickname) {
        return profileRepository.findByNickname(nickname);
    }

    /** Проверка существования nickname. */
    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return profileRepository.existsByNickname(nickname);
    }

    /** Смена никнейма. */
    @Transactional
    public Profile updateNickname(Integer profileId, String newNickname) {
        Profile profile = getProfile(profileId);

        if (profile.getNickname().equals(newNickname)) {
          return profile;
        }

        validateNickname(newNickname);

        if (profileRepository.existsByNickname(newNickname)) {
            throw new NicknameAlreadyExistsException(newNickname);
        }

        String oldNickname = profile.getNickname();
        profile.setNickname(newNickname);
        profile = profileRepository.save(profile);
        log.info(
            "Никнейм изменён: profileId={}, old='{}', new='{}'", profileId, oldNickname, newNickname);
        return profile;
    }

    /** Смена пароля. */
    @Transactional
    public void updatePassword(Integer profileId, String oldPassword, String newPassword) {
        Profile profile = getProfile(profileId);

        if (profile.getPasswordHash() == null
            || !passwordEncoder.matches(oldPassword, profile.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        validatePassword(newPassword);

        profile.setPasswordHash(passwordEncoder.encode(newPassword));
        profileRepository.save(profile);
        log.info("Пароль изменён: profileId={}", profileId);
    }

    private void setAuthentication(Profile profile) {
        Authentication auth =
            new UsernamePasswordAuthenticationToken(
                profile.getProfileId(), null, List.of(new SimpleGrantedAuthority(ROLE_USER)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new InvalidNicknameException("Никнейм не может быть пустым");
        }
        if (nickname.length() < NICKNAME_MIN_LENGTH || nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new InvalidNicknameException(
                "Никнейм должен быть от "
                    + NICKNAME_MIN_LENGTH
                    + " до "
                    + NICKNAME_MAX_LENGTH
                    + " символов");
        }
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new InvalidNicknameException(
                "Никнейм может содержать только латинские буквы, цифры и знак подчёркивания");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new InvalidPasswordException("Пароль не может быть пустым");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new InvalidPasswordException(
                "Пароль должен быть не менее " + PASSWORD_MIN_LENGTH + " символов");
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            throw new InvalidPasswordException(
                "Пароль не может быть длиннее " + PASSWORD_MAX_LENGTH + " символов (ограничение BCrypt)");
        }
    }
}