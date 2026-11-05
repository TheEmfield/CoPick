package com.copick.preferences.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class GenreNotFoundException extends DomainException {
    public GenreNotFoundException(Integer genreId) {
        super("Жанр с id=" + genreId + " не найден", HttpStatus.NOT_FOUND, "GENRE_NOT_FOUND");
    }
}
