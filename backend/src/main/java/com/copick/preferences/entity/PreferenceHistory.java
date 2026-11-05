package com.copick.preferences.entity;

import com.copick.profiles.entity.Profile;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "preference_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(name = "old_weight", columnDefinition = "NUMERIC")
    private Double oldWeight;

    @Column(name = "new_weight", columnDefinition = "NUMERIC")
    private Double newWeight;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private OffsetDateTime changedAt;
}