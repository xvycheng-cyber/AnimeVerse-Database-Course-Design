package com.animeverse.vo;

public record TriggerFavoriteVO(
        Long animeId,
        boolean enabled,
        boolean changed,
        long beforeCount,
        long afterCount
) {
}
