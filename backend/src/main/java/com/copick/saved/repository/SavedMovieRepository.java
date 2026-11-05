package com.copick.saved.repository;

import com.copick.saved.entity.SavedMovie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedMovieRepository extends JpaRepository<SavedMovie, Integer> {

    /**
     * Возвращает все сохранённые фильмы группы ВМЕСТЕ с их жанрами и автором.
     */
    @EntityGraph(attributePaths = "addedBy")
    @Query("SELECT DISTINCT sm FROM SavedMovie sm " +
            "LEFT JOIN FETCH sm.genres smg " +
            "LEFT JOIN FETCH smg.genre " +
            "WHERE sm.group.id = :groupId " +
            "ORDER BY sm.addedAt DESC")
    List<SavedMovie> findByGroupIdWithGenres(@Param("groupId") Integer groupId);

    /**
     * Проверяет, не сохранён ли уже этот контент в данной группе (защита от дубликатов).
     */
    @Query("SELECT sm FROM SavedMovie sm WHERE sm.group.groupId = :groupId AND sm.externalId = :externalId AND sm.contentType = :contentType")
    Optional<SavedMovie> findByGroupIdAndExternalIdAndContentType(
            @Param("groupId") Integer groupId,
            @Param("externalId") String externalId,
            @Param("contentType") String contentType
    );

    /**
     * Безопасное получение записи: находит фильм только если он принадлежит указанной группе.
     */
    @Query("SELECT sm FROM SavedMovie sm WHERE sm.savedId = :savedId AND sm.group.id = :groupId")
    Optional<SavedMovie> findByIdAndGroupId(@Param("savedId") Integer savedId,
                                            @Param("groupId") Integer groupId);
}