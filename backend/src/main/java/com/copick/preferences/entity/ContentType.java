package com.copick.preferences.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "content_name", nullable = false, unique = true, length = 16)
    private String contentName;
}