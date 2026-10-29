package com.copick.recommendations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotMovieGenreId implements Serializable {
    @Column(name = "snapshot_id")
    private Integer snapshotId;

    @Column(name = "movie_position")
    private Integer moviePosition;

    @Column(name = "genre_id")
    private Integer genreId;
}