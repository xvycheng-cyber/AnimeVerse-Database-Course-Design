package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class RankingVO {

    private Long animeId;
    private String title;
    private String coverUrl;
    private LocalDate releaseDate;
    private String status;
    private String studioName;
    private BigDecimal averageRating;
    private Long favoriteCount;
    private Long reviewCount;
    private Long viewCount;
    private BigDecimal weightedScore;
    private Integer ranking;

    public BigDecimal getRating() {
        return averageRating;
    }

    public BigDecimal getScore() {
        return averageRating;
    }

    public Integer getReleaseYear() {
        return releaseDate == null ? null : releaseDate.getYear();
    }

    public Long getId() {
        return animeId;
    }

    public Long getRankNo() {
        return ranking == null ? null : ranking.longValue();
    }
}
