package com.copick.preferences.repository;

import com.copick.preferences.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    // Минимальный — нужен только для getReferenceById в PreferenceService.
    // Методы для управления жанрами появятся, когда потребуется UI листинга.
}
