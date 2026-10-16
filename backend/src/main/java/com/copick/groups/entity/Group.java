package com.copick.groups.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "invite_code", unique = true, length = 16)
    private String inviteCode;

    @Column(name = "age_restriction")
    private Short ageRestriction;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}