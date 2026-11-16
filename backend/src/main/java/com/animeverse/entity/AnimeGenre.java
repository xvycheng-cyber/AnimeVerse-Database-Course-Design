package com.animeverse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("anime_genre")
public class AnimeGenre {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("genre_name")
    private String genreName;
    @TableField("genre_slug")
    private String genreSlug;
    private String description;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
