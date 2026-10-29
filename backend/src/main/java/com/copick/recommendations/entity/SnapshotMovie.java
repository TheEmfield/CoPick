package com.copick.recommendations.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snapshot_movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotMovie {
    @EmbeddedId
    private SnapshotMovieId id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "poster_url", length = 512)
    private String posterUrl;

    @Column(name = "backdrop_url", length = 512)
    private String backdropUrl;

    @Column(name = "rating", precision = 3, scale = 1)
    private Double rating;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;
}