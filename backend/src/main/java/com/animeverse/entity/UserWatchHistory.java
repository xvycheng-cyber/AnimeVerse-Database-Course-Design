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
@TableName("user_watch_history")
public class UserWatchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("episode_id")
    private Long episodeId;
    @TableField("progress_seconds")
    private Integer progressSeconds;
    private Integer completed;
    @TableField("last_watched_at")
    private LocalDateTime lastWatchedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
