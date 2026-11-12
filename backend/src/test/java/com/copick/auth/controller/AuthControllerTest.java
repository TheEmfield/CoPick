package com.copick.auth.controller;

import static com.copick.testsupport.ControllerTestSupport.authenticateAs;
import static com.copick.testsupport.ControllerTestSupport.clearAuthentication;
import static com.copick.testsupport.ControllerTestSupport.standaloneWithPrincipal;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.auth.service.JwtService;
import com.copick.exception.GlobalExceptionHandler;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.NicknameAlreadyExistsException;
import com.copick.profiles.service.ProfileService;
import java.time.OffsetDateTime;
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
@DisplayName("AuthController: REST /api/v1/auth (MockMvc standalone)")
class AuthControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneWithPrincipal(new AuthController(profileService, jwtService), new GlobalExceptionHandler());
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    private static Profile profile(int id, String nickname) {
        Profile profile = new Profile();
        profile.setProfileId(id);
        profile.setNickname(nickname);
        profile.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        return profile;
    }

    @Test
    @DisplayName("POST /register -> 201 + токен и профиль")
    void registerReturns201WithTokenAndProfile() throws Exception {
        when(profileService.register("newbie", "secret123")).thenReturn(profile(7, "newbie"));
        when(jwtService.generateToken(7)).thenReturn("jwt-token-123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "newbie", "password": "secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.profile.profileId").value(7))
                .andExpect(jsonPath("$.profile.nickname").value("newbie"))
                .andExpect(jsonPath("$.profile.createdAt").exists());
    }

    @Test
    @DisplayName("POST /register при занятом никнейме -> 409 с кодом NICKNAME_ALREADY_EXISTS")
    void registerWithTakenNicknameReturns409() throws Exception {
        when(profileService.register("taken", "secret123"))
                .thenThrow(new NicknameAlreadyExistsException("taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "taken", "password": "secret123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NICKNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /login -> 200 + токен и профиль")
    void loginReturns200WithTokenAndProfile() throws Exception {
        when(profileService.login("newbie", "secret123")).thenReturn(profile(7, "newbie"));
        when(jwtService.generateToken(7)).thenReturn("jwt-token-123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "newbie", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.profile.nickname").value("newbie"));
    }

    @Test
    @DisplayName("PATCH /me обновляет профиль текущего пользователя -> 200")
    void updateMeReturnsUpdatedProfile() throws Exception {
        authenticateAs(7);
        when(profileService.updateProfile(eq(7), eq("renamed"), eq("oldPass"), eq("newPass123")))
                .thenReturn(profile(7, "renamed"));

        mockMvc.perform(patch("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "renamed", "oldPassword": "oldPass", "newPassword": "newPass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(7))
                .andExpect(jsonPath("$.nickname").value("renamed"));

        verify(profileService).updateProfile(7, "renamed", "oldPass", "newPass123");
    }
}
