package com.animeverse.mapper;

import com.animeverse.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT id, username, password_hash, nickname, email, avatar_url, status,
                   last_login_at, created_at, updated_at
            FROM sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    SysUser findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password_hash, nickname, email, avatar_url, status,
                   last_login_at, created_at, updated_at
            FROM sys_user
            WHERE email = #{email}
            LIMIT 1
            """)
    SysUser findByEmail(@Param("email") String email);

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countUsersByRole(@Param("roleId") Long roleId);
}
