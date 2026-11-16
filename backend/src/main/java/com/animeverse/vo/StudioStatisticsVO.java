package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class StudioStatisticsVO {

    private Long studioId;
    private String studioName;
    private Long animeCount;
    private BigDecimal averageRating;
    private Long favoriteTotal;
    private Long reviewTotal;
}
