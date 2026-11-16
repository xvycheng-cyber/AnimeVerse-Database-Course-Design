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
@TableName("operation_log")
public class OperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("operator_user_id")
    private Long operatorUserId;
    @TableField("action_type")
    private String actionType;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Long targetId;
    private String description;
    @TableField("ip_address")
    private String ipAddress;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
