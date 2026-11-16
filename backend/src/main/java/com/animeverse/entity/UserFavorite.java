package com.animeverse.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("user_favorite")
public class UserFavorite {

    @TableField("user_id")
    private Long userId;
    @TableField("anime_id")
    private Long animeId;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
