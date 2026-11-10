package com.copick.saved.controller;

import static com.copick.testsupport.ControllerTestSupport.authenticateAs;
import static com.copick.testsupport.ControllerTestSupport.clearAuthentication;
import static com.copick.testsupport.ControllerTestSupport.standaloneWithPrincipal;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.exception.GlobalExceptionHandler;
import com.copick.saved.dto.SaveMovieRequest;
import com.copick.saved.dto.SavedMovieResponseDto;
import com.copick.saved.exception.MovieAlreadySavedException;
import com.copick.saved.service.SavedMovieService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedMovieController: REST /api/v1/groups/{groupId}/saved")
class SavedMovieControllerTest {

    private static final Integer GROUP_ID = 1;
    private static final Integer PROFILE_ID = 7;

    @Mock
    private SavedMovieService savedMovieService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneWithPrincipal(
                new SavedMovieController(savedMovieService), new GlobalExceptionHandler());
        authenticateAs(PROFILE_ID);
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    private static SaveMovieRequest request() {
        return new SaveMovieRequest("tmdb-42", "movie", "Интерстеллар", "Interstellar",
                "https://poster", "https://backdrop", 8.6, 2014, "Эпический sci-fi", null,
                List.of("action"));
    }

    private static SavedMovieResponseDto response() {
        return new SavedMovieResponseDto(100, GROUP_ID, "tmdb-42", "movie", "Интерстеллар",
                "Interstellar", "https://poster", "https://backdrop", 8.6, 2014,
                "Эпический sci-fi", null, PROFILE_ID, "movie_lover", null, List.of("action"));
    }

    @Test
    @DisplayName("GET -> список сохранённых фильмов группы")
    void getSavedMoviesReturnsList() throws Exception {
        when(savedMovieService.getSavedMovies(GROUP_ID, PROFILE_ID))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/groups/{groupId}/saved", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].savedId").value(100))
                .andExpect(jsonPath("$[0].title").value("Интерстеллар"))
                .andExpect(jsonPath("$[0].genres[0]").value("action"));
    }

    @Test
    @DisplayName("POST -> 201 и созданная запись")
    void addToSavedReturns201() throws Exception {
        SaveMovieRequest request = request();
        when(savedMovieService.addToSaved(eq(GROUP_ID), eq(PROFILE_ID), eq(request)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/groups/{groupId}/saved", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId": "tmdb-42",
                                  "contentType": "movie",
                                  "title": "Интерстеллар",
                                  "originalTitle": "Interstellar",
                                  "posterUrl": "https://poster",
                                  "backdropUrl": "https://backdrop",
                                  "rating": 8.6,
                                  "releaseYear": 2014,
                                  "overview": "Эпический sci-fi",
                                  "notes": null,
                                  "genres": ["action"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedId").value(100))
                .andExpect(jsonPath("$.externalId").value("tmdb-42"));

        verify(savedMovieService).addToSaved(eq(GROUP_ID), eq(PROFILE_ID), eq(request));
    }

    @Test
    @DisplayName("POST дубля -> 409 MOVIE_ALREADY_SAVED")
    void addDuplicateReturns409() throws Exception {
        when(savedMovieService.addToSaved(eq(GROUP_ID), eq(PROFILE_ID), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new MovieAlreadySavedException("tmdb-42", GROUP_ID));

        mockMvc.perform(post("/api/v1/groups/{groupId}/saved", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId": "tmdb-42", "contentType": "movie", "title": "Интерстеллар"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MOVIE_ALREADY_SAVED"));
    }

    @Test
    @DisplayName("DELETE /{savedId} -> 204")
    void removeFromSavedReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{groupId}/saved/{savedId}", GROUP_ID, 100))
                .andExpect(status().isNoContent());

        verify(savedMovieService).removeFromSaved(GROUP_ID, 100, PROFILE_ID);
    }
}
