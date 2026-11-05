package com.copick.preferences.controller;

import com.copick.preferences.dto.PreferenceResponseDto;
import com.copick.preferences.dto.UpdatePreferenceRequest;
import com.copick.preferences.service.PreferenceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles/me/preferences")
public class ProfilePreferenceController {

    private final PreferenceService preferenceService;

    public ProfilePreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<List<PreferenceResponseDto>> getProfilePreferences(
        @AuthenticationPrincipal Integer currentProfileId) {
        return ResponseEntity.ok(preferenceService.getPreferences(currentProfileId));
    }

    @PutMapping
    public ResponseEntity<List<PreferenceResponseDto>> putProfilePreferences(
        @AuthenticationPrincipal Integer currentProfileId,
        @RequestBody UpdatePreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(currentProfileId, request.preferences()));
    }

    @DeleteMapping("/{genreId}")
    public ResponseEntity<Void> deleteProfilePreference(
        @AuthenticationPrincipal Integer currentProfileId, @PathVariable Integer genreId) {
        preferenceService.deletePreference(currentProfileId, genreId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfilePreferences(
        @AuthenticationPrincipal Integer currentProfileId) {
        preferenceService.deleteAllPreferences(currentProfileId);
        return ResponseEntity.noContent().build();
    }

}
