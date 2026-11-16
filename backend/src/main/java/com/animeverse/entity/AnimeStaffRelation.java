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
@TableName("anime_staff_relation")
public class AnimeStaffRelation {

    @TableField("anime_id")
    private Long animeId;
    @TableField("staff_id")
    private Long staffId;
    @TableField("role_type")
    private String roleType;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
