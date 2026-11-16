package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AnimeListVO {

    private Long id;
    private String title;
    private String titleJp;
    private String titleEn;
    private String originalTitle;
    private Long studioId;
    private String studioName;
    private String coverUrl;
    private String synopsis;
    private Integer releaseYear;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String status;
    private Integer episodeCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private BigDecimal rating;
    private Long favoriteCount;
    private Long reviewCount;
    private Long viewCount;
    private Boolean favorited;
    private LocalDateTime createdAt;
}
