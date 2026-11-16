package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.ProfileUpdateRequest;
import com.animeverse.entity.SysRole;
import com.animeverse.entity.SysUser;
import com.animeverse.entity.SysUserRole;
import com.animeverse.mapper.SysRoleMapper;
import com.animeverse.mapper.SysUserMapper;
import com.animeverse.mapper.SysUserRoleMapper;
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public UserVO currentProfile() {
        return getUser(SecurityUtils.currentUserId());
    }

    @Transactional
    public UserVO updateCurrentProfile(ProfileUpdateRequest request) {
        Long userId = SecurityUtils.currentUserId();
        SysUser user = requireUser(userId);
        SysUser sameEmail = userMapper.findByEmail(request.email());
        if (sameEmail != null && !sameEmail.getId().equals(userId)) {
            throw BusinessException.conflict("邮箱已被其他账户使用");
        }
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setAvatarUrl(request.avatarUrl());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return getUser(userId);
    }

    public PageResult<UserVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword)
                        .or()
                        .like(SysUser::getEmail, keyword))
                .orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> result = userMapper.selectPage(Page.of(page, size), wrapper);
        List<UserVO> records = result.getRecords().stream()
                .map(user -> getUser(user.getId()))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public UserVO setEnabled(Long userId, boolean enabled) {
        SysUser user = requireUser(userId);
        if (user.getId().equals(SecurityUtils.currentUserId()) && !enabled) {
            throw BusinessException.badRequest("不能停用当前登录账户");
        }
        user.setStatus(enabled ? 1 : 0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return getUser(userId);
    }

    @Transactional
    public UserVO setRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        Set<Long> uniqueRoleIds = new HashSet<>(roleIds);
        if (uniqueRoleIds.size() != roleIds.size()) {
            throw BusinessException.badRequest("角色列表不能包含重复项");
        }
        List<SysRole> roles = roleMapper.selectBatchIds(uniqueRoleIds);
        if (roles.size() != uniqueRoleIds.size()) {
            throw BusinessException.badRequest("包含不存在的角色");
        }
        SysRole adminRole = roleMapper.findByRoleCode("ADMIN");
        if (adminRole != null
                && userMapper.findRoleCodes(userId).contains("ADMIN")
                && !uniqueRoleIds.contains(adminRole.getId())
                && userMapper.countUsersByRole(adminRole.getId()) <= 1) {
            throw BusinessException.conflict("不能移除系统中最后一个管理员");
        }
        userRoleMapper.deleteByUserId(userId);
        LocalDateTime assignedAt = LocalDateTime.now();
        for (Long roleId : uniqueRoleIds) {
            userRoleMapper.insertAssignment(SysUserRole.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .assignedAt(assignedAt)
                    .assignedBy(SecurityUtils.currentUserId())
                    .build());
        }
        return getUser(userId);
    }

    public UserVO getUser(Long userId) {
        SysUser user = requireUser(userId);
        return UserVO.from(user, userMapper.findRoleCodes(userId));
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }
}
