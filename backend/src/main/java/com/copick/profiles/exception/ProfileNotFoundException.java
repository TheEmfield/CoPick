package com.copick.profiles.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends DomainException {
    public ProfileNotFoundException(Integer profileId) {
        super("Профиль с id=" + profileId + " не найден", HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND");
    }

    public ProfileNotFoundException(String nickname) {
        super(
            "Профиль с nickname='" + nickname + "' не найден",
            HttpStatus.NOT_FOUND,
            "PROFILE_NOT_FOUND");
    }
}
