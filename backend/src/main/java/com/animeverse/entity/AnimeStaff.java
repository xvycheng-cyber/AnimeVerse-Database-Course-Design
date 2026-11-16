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
@TableName("anime_staff")
public class AnimeStaff {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("staff_name")
    private String staffName;
    @TableField("staff_name_jp")
    private String staffNameJp;
    @TableField("birth_date")
    private LocalDate birthDate;
    private String country;
    @TableField("avatar_url")
    private String avatarUrl;
    private String bio;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
