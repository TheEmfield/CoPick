package com.copick.preferences.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для обновления предпочтения пользователя. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceUpdateDto {
    private Integer genreId;
    private BigDecimal weight;
}
