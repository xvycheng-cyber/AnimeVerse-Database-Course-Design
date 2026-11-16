package com.animeverse.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("anime_platform_relation")
public class AnimePlatformRelation {

    @TableField("anime_id")
    private Long animeId;
    @TableField("platform_id")
    private Long platformId;
    @TableField("external_url")
    private String externalUrl;
    @TableField("available_from")
    private LocalDate availableFrom;
    @TableField("available_until")
    private LocalDate availableUntil;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
