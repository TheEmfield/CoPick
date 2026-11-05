package com.copick.recommendations.entity;

import com.copick.groups.entity.Group;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "recommendation_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Integer snapshotId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", unique = true, nullable = false)
    private Group group;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}