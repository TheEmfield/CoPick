package com.copick.groups.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(GroupMemberId.class)
public class GroupMember {
    @Id
    @Column(name = "group_id")
    private Integer groupId;

    @Id
    @Column(name = "profile_id")
    private Integer profileId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;
}