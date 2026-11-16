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
@TableName("streaming_platform")
public class StreamingPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("platform_name")
    private String platformName;
    @TableField("platform_code")
    private String platformCode;
    @TableField("website_url")
    private String websiteUrl;
    private String country;
    @TableField("is_active")
    private Integer isActive;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
