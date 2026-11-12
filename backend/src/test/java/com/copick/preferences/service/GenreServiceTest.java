package com.copick.preferences.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.copick.preferences.entity.ContentType;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.repository.GenreRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenreService: справочник жанров")
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    private GenreService genreService;

    @BeforeEach
    void setUp() {
        genreService = new GenreService(genreRepository);
    }

    @Test
    @DisplayName("getAllGenres возвращает DTO с id жанра и типом контента")
    void getAllGenresMapsEntitiesToDto() {
        ContentType movieType = new ContentType(1, "movie");

        Genre action = new Genre(3, "action", movieType);
        Genre comedy = new Genre(4, "comedy", movieType);

        org.mockito.Mockito.when(genreRepository.findAllWithContentType())
                .thenReturn(List.of(action, comedy));

        var result = genreService.getAllGenres();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).genreId()).isEqualTo(3);
        assertThat(result.get(0).genreName()).isEqualTo("action");
        assertThat(result.get(0).contentType()).isEqualTo(1);
        assertThat(result.get(1).genreName()).isEqualTo("comedy");
    }
}
