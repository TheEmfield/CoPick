package com.copick.saved.repository;

import com.copick.saved.entity.SavedMovie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedMovieRepository extends JpaRepository<SavedMovie, Integer> {

  @EntityGraph(attributePaths = {"addedBy", "genres"})
  @Query("SELECT DISTINCT sm FROM SavedMovie sm WHERE sm.group.groupId = :groupId ORDER BY sm.addedAt DESC")
  List<SavedMovie> findByGroupIdWithDetails(@Param("groupId") Integer groupId);

  Optional<SavedMovie> findByGroupGroupIdAndExternalIdAndContentType(
      Integer groupId, String externalId, String contentType);

  @Query("SELECT sm FROM SavedMovie sm WHERE sm.savedId = :savedId AND sm.group.groupId = :groupId")
  Optional<SavedMovie> findByIdAndGroupId(@Param("savedId") Integer savedId, @Param("groupId") Integer groupId);
}