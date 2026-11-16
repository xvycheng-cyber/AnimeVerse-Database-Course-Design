package com.animeverse.vo;

import java.time.LocalDate;

public record PlatformLinkVO(
        Long platformId,
        String name,
        String platformName,
        String platformCode,
        String logoUrl,
        String watchUrl,
        String externalUrl,
        LocalDate availableFrom,
        LocalDate availableUntil
) {
}
