package com.animeverse.vo;

import com.animeverse.entity.SysRole;

import java.time.LocalDateTime;

public record RoleVO(
        Long id,
        String name,
        String code,
        String roleCode,
        String roleName,
        String description,
        Long userCount,
        LocalDateTime createdAt
) {
    public static RoleVO from(SysRole role, long userCount) {
        return new RoleVO(
                role.getId(),
                role.getRoleName(),
                role.getRoleCode(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                userCount,
                role.getCreatedAt()
        );
    }
}
