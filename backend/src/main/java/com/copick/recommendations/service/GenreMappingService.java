package com.copick.recommendations.service;

import com.copick.preferences.entity.Genre;
import com.copick.preferences.repository.GenreRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GenreMappingService {

    private final Map<String, Integer> nameToIdMap;
    private final Map<Integer, String> idToNameMap;

    public GenreMappingService(GenreRepository genreRepository) {
        // Загружаем один раз при старте приложения
        Map<String, Integer> tempNameToId = loadGenres(genreRepository);
        this.nameToIdMap = Map.copyOf(tempNameToId);

        this.idToNameMap = tempNameToId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    @Cacheable(value = "genres_mapping", key = "'all'")
    private Map<String, Integer> loadGenres(GenreRepository genreRepository) {
        return genreRepository.findAll().stream()
                .collect(Collectors.toMap(
                        g -> g.getGenreName().toLowerCase().trim(),
                        Genre::getGenreId
                ));
    }

    public Integer getGenreIdByName(String genreName) {
        if (genreName == null) return null;
        return nameToIdMap.get(genreName.toLowerCase().trim());
    }

    public String getGenreNameById(Integer genreId) {
        if (genreId == null) return null;
        return idToNameMap.get(genreId);
    }
}