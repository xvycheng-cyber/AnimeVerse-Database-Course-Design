package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrendPointVO {

    private String date;
    private Long userCount;
    private Long reviewCount;
    private Long favoriteCount;
    private Long count;
}
