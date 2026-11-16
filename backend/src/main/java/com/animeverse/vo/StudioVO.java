package com.animeverse.vo;

import com.animeverse.entity.AnimeStudio;

import java.time.LocalDateTime;

public record StudioVO(
        Long id,
        String name,
        String studioName,
        String studioNameEn,
        String originalName,
        String country,
        Integer foundedYear,
        String description,
        String logoUrl,
        String website,
        String websiteUrl,
        LocalDateTime createdAt
) {
    public static StudioVO from(AnimeStudio studio) {
        return new StudioVO(
                studio.getId(),
                studio.getStudioName(),
                studio.getStudioName(),
                studio.getStudioNameEn(),
                studio.getStudioNameEn(),
                studio.getCountry(),
                studio.getFoundedYear(),
                studio.getDescription(),
                studio.getLogoUrl(),
                studio.getWebsiteUrl(),
                studio.getWebsiteUrl(),
                studio.getCreatedAt()
        );
    }
}
