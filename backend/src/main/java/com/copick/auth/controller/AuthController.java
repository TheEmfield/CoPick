package com.copick.auth.controller;

import com.copick.auth.dto.AuthResponse;
import com.copick.auth.dto.LoginRequest;
import com.copick.auth.dto.ProfileDto;
import com.copick.auth.dto.RegisterRequest;
import com.copick.auth.dto.UpdateMeRequest;
import com.copick.auth.service.JwtService;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        Profile profile = profileService.register(request.nickname(), request.password());
        String token = jwtService.generateToken(profile.getProfileId());

        AuthResponse response = new AuthResponse(token, ProfileDto.fromEntity(profile));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Profile profile = profileService.login(request.nickname(), request.password());
        String token = jwtService.generateToken(profile.getProfileId());

        AuthResponse response = new AuthResponse(token, ProfileDto.fromEntity(profile));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileDto> updateMe(
        @AuthenticationPrincipal Integer currentProfileId, @RequestBody UpdateMeRequest request) {
        Profile updatedProfile =
            profileService.updateProfile(
                currentProfileId,
                request.nickname(),
                request.oldPassword(),
                request.newPassword());

        return ResponseEntity.ok(ProfileDto.fromEntity(updatedProfile));
    }
}
