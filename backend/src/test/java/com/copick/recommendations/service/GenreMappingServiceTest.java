package com.copick.recommendations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
@DisplayName("GenreMappingService: соответствие имён жанров и внутренних id")
class GenreMappingServiceTest {

    @Mock
    private GenreRepository genreRepository;

    private GenreMappingService genreMappingService;

    @BeforeEach
    void setUp() {
        Genre action = new Genre(3, " action ", new ContentType(1, "movie")); // с пробелами
        Genre comedy = new Genre(4, "comedy", new ContentType(1, "movie"));
        when(genreRepository.findAll()).thenReturn(List.of(action, comedy));

        genreMappingService = new GenreMappingService(genreRepository);
    }

    @Test
    @DisplayName("имя жанра (в т.ч. с пробелами) -> внутренний id")
    void mapsNameToId() {
        assertThat(genreMappingService.getGenreIdByName("action")).isEqualTo(3);
        assertThat(genreMappingService.getGenreIdByName("  comedy  ")).isEqualTo(4);
    }

    @Test
    @DisplayName("id жанра -> имя")
    void mapsIdToName() {
        assertThat(genreMappingService.getGenreNameById(3)).isEqualTo("action");
        assertThat(genreMappingService.getGenreNameById(4)).isEqualTo("comedy");
    }

    @Test
    @DisplayName("неизвестное имя или id -> null")
    void unknownValuesReturnNull() {
        assertThat(genreMappingService.getGenreIdByName("horror")).isNull();
        assertThat(genreMappingService.getGenreIdByName("")).isNull();
        assertThat(genreMappingService.getGenreNameById(999)).isNull();
    }

    @Test
    @DisplayName("null-аргументы обрабатываются без NPE")
    void nullArgumentsReturnNull() {
        assertThat(genreMappingService.getGenreIdByName(null)).isNull();
        assertThat(genreMappingService.getGenreNameById(null)).isNull();
    }
}
