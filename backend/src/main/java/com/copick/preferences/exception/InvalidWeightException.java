package com.copick.preferences.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InvalidWeightException extends DomainException {

    public InvalidWeightException(BigDecimal weight) {
        super(
                "Вес предпочтения должен быть в диапазоне [-1, 1], получено: " + weight,
                HttpStatus.BAD_REQUEST,
                "INVALID_WEIGHT"
        );
    }

    public InvalidWeightException() {
        super("Вес предпочтения не может быть null", HttpStatus.BAD_REQUEST, "INVALID_WEIGHT");
    }
}
