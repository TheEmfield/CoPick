package com.copick.recommendations.repository;

import com.copick.recommendations.entity.SnapshotMovieGenre;
import com.copick.recommendations.entity.SnapshotMovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotMovieGenreRepository extends JpaRepository<SnapshotMovieGenre, SnapshotMovieGenreId> {

    List<SnapshotMovieGenre> findByIdSnapshotIdAndIdMoviePosition(Integer snapshotId, Integer moviePosition);

    List<SnapshotMovieGenre> findByIdSnapshotId(Integer snapshotId);
}