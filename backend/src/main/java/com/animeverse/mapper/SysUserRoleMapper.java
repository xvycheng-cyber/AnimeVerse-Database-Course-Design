package com.animeverse.mapper;

import com.animeverse.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface SysUserRoleMapper {

    @Insert("""
            INSERT INTO sys_user_role (user_id, role_id, assigned_at, assigned_by)
            VALUES (#{userId}, #{roleId}, #{assignedAt}, #{assignedBy})
            """)
    int insertAssignment(SysUserRole assignment);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
