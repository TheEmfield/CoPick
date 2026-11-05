package com.copick.recommendations.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snapshot_movie_genres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotMovieGenre {
    @EmbeddedId
    private SnapshotMovieGenreId id;
}