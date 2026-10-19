package com.copick.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Базовый класс для всех доменных исключений. Каждое конкретное исключение фиксирует свой
 * HTTP-статус и error-код, чтобы GlobalExceptionHandler мог обработать их единообразно.
 */
@Getter
public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected DomainException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
