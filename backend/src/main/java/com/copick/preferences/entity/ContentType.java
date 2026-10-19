package com.copick.preferences.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "content_types")
@Getter
@Setter
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