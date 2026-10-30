package com.copick.preferences.service;

import com.copick.preferences.dto.GenreResponseDto;
import com.copick.preferences.repository.GenreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Transactional(readOnly = true)
    public List<GenreResponseDto> getAllGenres() {
        return genreRepository.findAllWithContentType().stream()
            .map(GenreResponseDto::fromEntity)
            .toList();
    }
}
