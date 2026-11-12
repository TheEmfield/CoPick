package com.copick.preferences.controller;

import static com.copick.testsupport.ControllerTestSupport.authenticateAs;
import static com.copick.testsupport.ControllerTestSupport.clearAuthentication;
import static com.copick.testsupport.ControllerTestSupport.standaloneWithPrincipal;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.exception.GlobalExceptionHandler;
import com.copick.preferences.dto.PreferenceResponseDto;
import com.copick.preferences.dto.PreferenceUpdateDto;
import com.copick.preferences.exception.InvalidWeightException;
import com.copick.preferences.service.PreferenceService;
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
@DisplayName("ProfilePreferenceController: REST /api/v1/profiles/me/preferences")
class ProfilePreferenceControllerTest {

    private static final Integer PROFILE_ID = 7;

    @Mock
    private PreferenceService preferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneWithPrincipal(
                new ProfilePreferenceController(preferenceService), new GlobalExceptionHandler());
        authenticateAs(PROFILE_ID);
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    @DisplayName("GET -> предпочтения текущего профиля")
    void getPreferencesReturnsList() throws Exception {
        when(preferenceService.getPreferences(PROFILE_ID)).thenReturn(List.of(
                new PreferenceResponseDto(3, "action", 0.8, null),
                new PreferenceResponseDto(4, "comedy", -0.5, null)));

        mockMvc.perform(get("/api/v1/profiles/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].genreName").value("action"))
                .andExpect(jsonPath("$[0].weight").value(0.8))
                .andExpect(jsonPath("$[1].weight").value(-0.5));
    }

    @Test
    @DisplayName("PUT -> обновление предпочтений, возвращает итоговый список")
    void putPreferencesUpdatesAndReturnsList() throws Exception {
        List<PreferenceUpdateDto> updates =
                List.of(new PreferenceUpdateDto(3, 0.9), new PreferenceUpdateDto(4, -1.0));
        when(preferenceService.updatePreferences(eq(PROFILE_ID), eq(updates)))
                .thenReturn(List.of(new PreferenceResponseDto(3, "action", 0.9, null)));

        mockMvc.perform(put("/api/v1/profiles/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preferences": [{"genreId": 3, "weight": 0.9}, {"genreId": 4, "weight": -1.0}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genreId").value(3))
                .andExpect(jsonPath("$[0].weight").value(0.9));

        verify(preferenceService).updatePreferences(PROFILE_ID, updates);
    }

    @Test
    @DisplayName("PUT с невалидным весом -> 400 INVALID_WEIGHT")
    void putPreferencesWithInvalidWeightReturns400() throws Exception {
        when(preferenceService.updatePreferences(eq(PROFILE_ID), org.mockito.ArgumentMatchers.anyList()))
                .thenThrow(new InvalidWeightException(5.0));

        mockMvc.perform(put("/api/v1/profiles/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preferences": [{"genreId": 3, "weight": 5.0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_WEIGHT"));
    }

    @Test
    @DisplayName("DELETE /{genreId} -> 204")
    void deleteSinglePreferenceReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/profiles/me/preferences/3"))
                .andExpect(status().isNoContent());

        verify(preferenceService).deletePreference(PROFILE_ID, 3);
    }

    @Test
    @DisplayName("DELETE -> 204 (удаление всех предпочтений)")
    void deleteAllPreferencesReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/profiles/me/preferences"))
                .andExpect(status().isNoContent());

        verify(preferenceService).deleteAllPreferences(PROFILE_ID);
    }
}
