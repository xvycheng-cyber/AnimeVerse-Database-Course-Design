package com.animeverse.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AnimeDetailVO(
        Long id,
        String title,
        String titleJp,
        String titleEn,
        String originalTitle,
        Long studioId,
        String studioName,
        String coverUrl,
        String synopsis,
        Integer releaseYear,
        LocalDate releaseDate,
        LocalDate endDate,
        String status,
        Integer episodeCount,
        BigDecimal averageRating,
        Integer ratingCount,
        BigDecimal rating,
        Long favoriteCount,
        Long reviewCount,
        Long viewCount,
        List<GenreVO> genres,
        List<StaffCreditVO> staff,
        List<PlatformLinkVO> platforms,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean favorited
) {
}
