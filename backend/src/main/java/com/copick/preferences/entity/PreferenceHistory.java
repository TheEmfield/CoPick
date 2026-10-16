package com.copick.preferences.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "preference_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @Column(name = "profile_id", nullable = false)
    private Integer profileId;

    @Column(name = "genre_id", nullable = false)
    private Integer genreId;

    @Column(name = "old_weight", precision = 3, scale = 2)
    private BigDecimal oldWeight;

    @Column(name = "new_weight", precision = 3, scale = 2)
    private BigDecimal newWeight;

    @Column(name = "changed_at")
    private OffsetDateTime changedAt;
}