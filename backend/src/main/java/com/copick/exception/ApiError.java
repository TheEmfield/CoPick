package com.copick.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Единый формат HTTP-ответа при ошибке. JsonInclude.NON_NULL — чтобы не тащить null-поля в ответ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private OffsetDateTime timestamp;
    private int status;
    private String error; // машинный код ошибки, напр. "GROUP_NOT_FOUND"
    private String message; // человекочитаемое сообщение
    private String path; // URI запроса
}