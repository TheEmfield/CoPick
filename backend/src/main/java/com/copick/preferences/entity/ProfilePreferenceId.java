package com.copick.preferences.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePreferenceId implements Serializable {
    private Integer profileId;
    private Integer genreId;
}