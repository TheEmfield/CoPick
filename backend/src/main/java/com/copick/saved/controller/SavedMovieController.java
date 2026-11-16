package com.copick.saved.controller;

import com.copick.saved.dto.SaveMovieRequest;
import com.copick.saved.dto.SavedMovieResponseDto;
import com.copick.saved.service.SavedMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/saved")
@RequiredArgsConstructor
public class SavedMovieController {

    private final SavedMovieService savedMovieService;

    @GetMapping
    public ResponseEntity<List<SavedMovieResponseDto>> getSavedMovies(
        @PathVariable Integer groupId,
        @AuthenticationPrincipal Integer profileId
    ) {
        List<SavedMovieResponseDto> response = savedMovieService.getSavedMovies(groupId, profileId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SavedMovieResponseDto> addToSaved(
        @PathVariable Integer groupId,
        @AuthenticationPrincipal Integer profileId,
        @RequestBody SaveMovieRequest request
    ) {
        SavedMovieResponseDto response = savedMovieService.addToSaved(groupId, profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{savedId}")
    public ResponseEntity<Void> removeFromSaved(
        @PathVariable Integer groupId,
        @PathVariable Integer savedId,
        @AuthenticationPrincipal Integer profileId
    ) {
        savedMovieService.removeFromSaved(groupId, savedId, profileId);
        return ResponseEntity.noContent().build();
    }
}