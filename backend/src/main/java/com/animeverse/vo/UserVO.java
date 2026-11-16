package com.animeverse.vo;

import com.animeverse.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String avatarUrl,
        Integer status,
        boolean enabled,
        List<String> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserVO from(SysUser user, List<String> roles) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getStatus() != null && user.getStatus() == 1,
                roles,
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
