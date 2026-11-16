package com.copick.saved.entity;

import com.copick.groups.entity.Group;
import com.copick.preferences.entity.Genre;
import com.copick.profiles.entity.Profile;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_id")
    private Integer savedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "content_type", nullable = false, length = 16)
    private String contentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "poster_url", length = 512)
    private String posterUrl;

    @Column(name = "backdrop_url", length = 512)
    private String backdropUrl;

    @Column(name = "rating", columnDefinition = "NUMERIC")
    private Double rating;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private Profile addedBy;

    @Column(name = "added_at")
    private OffsetDateTime addedAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "saved_movie_genres",
        joinColumns = @JoinColumn(name = "saved_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();
}