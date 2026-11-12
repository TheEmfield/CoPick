package com.copick.preferences.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.preferences.dto.GenreResponseDto;
import com.copick.preferences.service.GenreService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenreController: REST /api/v1/genres")
class GenreControllerTest {

    @Mock
    private GenreService genreService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GenreController(genreService)).build();
    }

    @Test
    @DisplayName("GET -> список жанров")
    void getGenresReturnsList() throws Exception {
        when(genreService.getAllGenres()).thenReturn(List.of(
                new GenreResponseDto(3, "action", 1),
                new GenreResponseDto(4, "comedy", 1)));

        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].genreId").value(3))
                .andExpect(jsonPath("$[0].genreName").value("action"))
                .andExpect(jsonPath("$[0].contentType").value(1));
    }
}
