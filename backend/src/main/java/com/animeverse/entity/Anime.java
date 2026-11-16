package com.animeverse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("anime")
public class Anime {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    @TableField("title_jp")
    private String titleJp;
    @TableField("title_en")
    private String titleEn;
    @TableField("studio_id")
    private Long studioId;
    @TableField("cover_url")
    private String coverUrl;
    private String synopsis;
    @TableField("release_date")
    private LocalDate releaseDate;
    @TableField("end_date")
    private LocalDate endDate;
    private String status;
    @TableField("episode_count")
    private Integer episodeCount;
    @TableField("average_rating")
    private BigDecimal averageRating;
    @TableField("rating_count")
    private Integer ratingCount;
    @TableField("favorite_count")
    private Long favoriteCount;
    @TableField("review_count")
    private Long reviewCount;
    @TableField("view_count")
    private Long viewCount;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
