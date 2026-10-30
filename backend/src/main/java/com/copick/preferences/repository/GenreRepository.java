package com.copick.preferences.repository;

import com.copick.preferences.entity.Genre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    Optional<Genre> findByGenreName(String genreName);

    @Query("SELECT g FROM Genre g JOIN FETCH g.ContentType ORDER BY g.genreId ASC")
    List<Genre> findAllWithContentType();
}
