package com.animeverse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("anime_episode")
public class AnimeEpisode {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("anime_id")
    private Long animeId;
    @TableField("episode_number")
    private Integer episodeNumber;
    private String title;
    private String synopsis;
    @TableField("duration_seconds")
    private Integer durationSeconds;
    @TableField("air_date")
    private LocalDate airDate;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
