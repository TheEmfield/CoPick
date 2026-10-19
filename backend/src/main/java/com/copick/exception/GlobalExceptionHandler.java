package com.copick.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/** Централизованная обработка исключений. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
    * Все доменные исключения — маппим в HTTP-ответ с указанным статусом. Логируем на warn: это
    * ожидаемые ошибки бизнес-логики.
    */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(
        DomainException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiError error = buildError(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /** Catch-all для всего остального. Не раскрываем внутренности клиенту. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ApiError error =
            buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Внутренняя ошибка сервера",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiError buildError(
        HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return ApiError.builder()
            .timestamp(OffsetDateTime.now())
            .status(status.value())
            .error(errorCode)
            .message(message)
            .path(request.getRequestURI())
            .build();
    }
}