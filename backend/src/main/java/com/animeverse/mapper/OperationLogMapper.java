package com.animeverse.mapper;

import com.animeverse.entity.OperationLogEntity;
import com.animeverse.vo.OperationLogVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

    @Select("""
            <script>
            SELECT l.id, l.operator_user_id, u.username, l.action_type, l.target_type,
                   l.target_id, l.description, l.ip_address, l.created_at
            FROM operation_log l
            LEFT JOIN sys_user u ON u.id = l.operator_user_id
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR l.action_type LIKE CONCAT('%', #{keyword}, '%')
                OR l.description LIKE CONCAT('%', #{keyword}, '%')
                OR l.ip_address LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="targetType != null and targetType != ''">
              AND l.target_type = #{targetType}
            </if>
            ORDER BY l.created_at DESC, l.id DESC
            </script>
            """)
    IPage<OperationLogVO> selectLogPage(
            Page<OperationLogVO> page,
            @Param("keyword") String keyword,
            @Param("targetType") String targetType
    );
}
