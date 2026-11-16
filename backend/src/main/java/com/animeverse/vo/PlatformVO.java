package com.animeverse.vo;

import com.animeverse.entity.StreamingPlatform;

import java.time.LocalDateTime;

public record PlatformVO(
        Long id,
        String name,
        String platformName,
        String platformCode,
        String websiteUrl,
        String country,
        Boolean active,
        String logoUrl,
        String description,
        LocalDateTime createdAt
) {
    public static PlatformVO from(StreamingPlatform platform) {
        return new PlatformVO(
                platform.getId(),
                platform.getPlatformName(),
                platform.getPlatformName(),
                platform.getPlatformCode(),
                platform.getWebsiteUrl(),
                platform.getCountry(),
                platform.getIsActive() != null && platform.getIsActive() == 1,
                null,
                null,
                platform.getCreatedAt()
        );
    }
}
