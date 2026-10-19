package com.copick.profiles.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidNicknameException extends DomainException {
    public InvalidNicknameException(String reason) {
        super(reason, HttpStatus.BAD_REQUEST, "INVALID_NICKNAME");
    }
}
