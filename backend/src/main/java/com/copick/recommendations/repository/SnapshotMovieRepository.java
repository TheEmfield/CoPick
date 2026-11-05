package com.copick.recommendations.repository;

import com.copick.recommendations.entity.SnapshotMovie;
import com.copick.recommendations.entity.SnapshotMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotMovieRepository extends JpaRepository<SnapshotMovie, SnapshotMovieId> {
    List<SnapshotMovie> findByIdSnapshotIdOrderByMoviePositionAsc(Integer snapshotId);
}