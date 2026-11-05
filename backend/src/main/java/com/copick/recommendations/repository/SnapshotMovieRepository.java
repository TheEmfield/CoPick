package com.copick.recommendations.repository;

import com.copick.recommendations.entity.SnapshotMovie;
import com.copick.recommendations.entity.SnapshotMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotMovieRepository extends JpaRepository<SnapshotMovie, SnapshotMovieId> {
    @Query("SELECT sm FROM SnapshotMovie sm WHERE sm.id.snapshotId = :snapshotId ORDER BY sm.id.moviePosition ASC")
    List<SnapshotMovie> findBySnapshotIdOrderByPosition(@Param("snapshotId") Integer snapshotId);
}