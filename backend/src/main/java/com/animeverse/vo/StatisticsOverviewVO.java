package com.animeverse.vo;

public record StatisticsOverviewVO(
        long userCount,
        long animeCount,
        long episodeCount,
        long studioCount,
        long reviewCount,
        long favoriteCount,
        long watchRecordCount
) {
}
