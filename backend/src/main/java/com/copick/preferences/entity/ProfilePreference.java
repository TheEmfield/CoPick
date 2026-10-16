package com.copick.preferences.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "profile_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProfilePreferenceId.class)
public class ProfilePreference {
    @Id
    @Column(name = "profile_id")
    private Integer profileId;

    @Id
    @Column(name = "genre_id")
    private Integer genreId;

    @Column(name = "weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal weight; // numeric(3,2) от -1 до 1

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}