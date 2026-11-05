package com.copick.preferences.controller;

import com.copick.preferences.dto.GenreResponseDto;
import com.copick.preferences.service.GenreService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<List<GenreResponseDto>> getGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

}
