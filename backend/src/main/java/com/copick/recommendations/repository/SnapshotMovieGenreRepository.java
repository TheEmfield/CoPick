package com.copick.recommendations.repository;

import com.copick.recommendations.entity.SnapshotMovieGenre;
import com.copick.recommendations.entity.SnapshotMovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotMovieGenreRepository extends JpaRepository<SnapshotMovieGenre, SnapshotMovieGenreId> {

    @Query("SELECT smg FROM SnapshotMovieGenre smg WHERE smg.id.snapshotId = :snapshotId")
    List<SnapshotMovieGenre> findBySnapshotId(@Param("snapshotId") Integer snapshotId);

    List<SnapshotMovieGenre> findByIdSnapshotId(Integer snapshotId);
}