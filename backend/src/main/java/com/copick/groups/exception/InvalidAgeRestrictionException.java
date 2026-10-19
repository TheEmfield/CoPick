package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidAgeRestrictionException extends DomainException {
    public InvalidAgeRestrictionException(Short age) {
        super(
            "Возрастное ограничение должно быть в диапазоне [0, 21], получено: " + age,
            HttpStatus.BAD_REQUEST,
            "INVALID_AGE_RESTRICTION");
    }
}
