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
@TableName("anime_studio")
public class AnimeStudio {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("studio_name")
    private String studioName;
    @TableField("studio_name_en")
    private String studioNameEn;
    private String country;
    @TableField("founded_year")
    private Integer foundedYear;
    private String description;
    @TableField("logo_url")
    private String logoUrl;
    @TableField("website_url")
    private String websiteUrl;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
