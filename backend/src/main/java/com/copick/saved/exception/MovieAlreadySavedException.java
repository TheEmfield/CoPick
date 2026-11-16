package com.copick.saved.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MovieAlreadySavedException extends DomainException {
    public MovieAlreadySavedException(String externalId, Integer groupId) {
        super(
            "Контент с externalId '" + externalId + "' уже сохранён в группе с ID " + groupId,
            HttpStatus.CONFLICT,
            "MOVIE_ALREADY_SAVED"
        );
    }
}