package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InviteCodeGenerationException extends DomainException {
    public InviteCodeGenerationException(int attempts) {
        super(
            "Не удалось сгенерировать уникальный invite-код за " + attempts + " попыток",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INVITE_CODE_GENERATION_FAILED");
    }
}
