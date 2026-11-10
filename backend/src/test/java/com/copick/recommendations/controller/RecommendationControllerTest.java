package com.copick.recommendations.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.algorithm.dto.ScoredMovie;
import com.copick.recommendations.dto.GenreStat;
import com.copick.recommendations.dto.GroupStatsResponse;
import com.copick.recommendations.dto.SnapshotMovieResponse;
import com.copick.recommendations.service.RecommendationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationController: REST /api/v1/groups/{id}/recommendations")
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecommendationController(recommendationService)).build();
    }

    @Test
    @DisplayName("GET /{id}/recommendations передаёт параметры и возвращает подборку")
    void getRecommendationsPassesParameters() throws Exception {
        when(recommendationService.getRecommendations(eq(1), eq(List.of(2, 3)), eq(List.of(4)), eq(5)))
                .thenReturn(List.of(new ScoredMovie(101, 1.25, Set.of(3), 0.3)));

        mockMvc.perform(get("/api/v1/groups/1/recommendations")
                        .param("profileIds", "2", "3")
                        .param("genreIds", "4")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value(101))
                .andExpect(jsonPath("$[0].finalScore").value(1.25));

        verify(recommendationService).getRecommendations(1, List.of(2, 3), List.of(4), 5);
    }

    @Test
    @DisplayName("GET /{id}/recommendations без параметров использует limit по умолчанию")
    void getRecommendationsUsesDefaultLimit() throws Exception {
        when(recommendationService.getRecommendations(eq(1), eq(null), eq(null), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/groups/1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(recommendationService).getRecommendations(1, null, null, 10);
    }

    @Test
    @DisplayName("GET /{id}/recommendations/stats -> статистика группы")
    void getGroupStatsReturnsStats() throws Exception {
        when(recommendationService.getGroupStats(1)).thenReturn(new GroupStatsResponse(
                2,
                List.of(new GenreStat("action", 0.8, 0.6)),
                List.of("horror")));

        mockMvc.perform(get("/api/v1/groups/1/recommendations/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.topGenres[0].genreName").value("action"))
                .andExpect(jsonPath("$.topGenres[0].avgWeight").value(0.8))
                .andExpect(jsonPath("$.topGenres[0].minWeight").value(0.6))
                .andExpect(jsonPath("$.bannedGenres[0]").value("horror"));
    }

    @Test
    @DisplayName("GET /{id}/recommendations/last -> последний снимок")
    void getLastSnapshotReturnsMovies() throws Exception {
        when(recommendationService.getLastSnapshot(1)).thenReturn(List.of(new SnapshotMovieResponse(
                "Movie A", "Original A", "https://poster", "https://backdrop",
                8.0, 2024, "overview", List.of("action"))));

        mockMvc.perform(get("/api/v1/groups/1/recommendations/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Movie A"))
                .andExpect(jsonPath("$[0].genres[0]").value("action"));
    }
}
