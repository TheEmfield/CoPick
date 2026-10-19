package com.copick.profiles.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class NicknameAlreadyExistsException extends DomainException {
    public NicknameAlreadyExistsException(String nickname) {
        super("Никнейм '" + nickname + "' уже занят", HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS");
    }
}
