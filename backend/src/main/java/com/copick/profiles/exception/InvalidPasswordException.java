package com.copick.profiles.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends DomainException {
    public InvalidPasswordException(String reason) {
        super(reason, HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
    }
}
