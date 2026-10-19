package com.copick.profiles.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Неверный nickname или пароль", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }
}
