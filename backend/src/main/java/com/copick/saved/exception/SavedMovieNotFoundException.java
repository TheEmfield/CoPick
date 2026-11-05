package com.copick.saved.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class SavedMovieNotFoundException extends DomainException {
    public SavedMovieNotFoundException(Integer savedId, Integer groupId) {
        super(
            "Сохранённый элемент с ID " + savedId + " не найден или не принадлежит группе " + groupId,
            HttpStatus.NOT_FOUND,
            "SAVED_MOVIE_NOT_FOUND");
    }
}