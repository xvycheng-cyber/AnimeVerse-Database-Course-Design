package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.dto.RoleSaveRequest;
import com.animeverse.entity.SysRole;
import com.animeverse.mapper.SysRoleMapper;
import com.animeverse.mapper.SysUserMapper;
import com.animeverse.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream()
                .map(role -> RoleVO.from(role, userMapper.countUsersByRole(role.getId())))
                .toList();
    }

    @Transactional
    public RoleVO create(RoleSaveRequest request) {
        if (roleMapper.findByRoleCode(request.roleCode()) != null) {
            throw BusinessException.conflict("角色编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        SysRole role = SysRole.builder()
                .roleCode(request.roleCode())
                .roleName(request.roleName())
                .description(request.description())
                .createdAt(now)
                .build();
        roleMapper.insert(role);
        return RoleVO.from(role, 0);
    }

    @Transactional
    public RoleVO update(Long roleId, RoleSaveRequest request) {
        SysRole role = requireRole(roleId);
        if (SetHolder.PROTECTED_CODES.contains(role.getRoleCode())
                && !role.getRoleCode().equals(request.roleCode())) {
            throw BusinessException.conflict("内置角色编码不允许修改");
        }
        SysRole sameCode = roleMapper.findByRoleCode(request.roleCode());
        if (sameCode != null && !sameCode.getId().equals(roleId)) {
            throw BusinessException.conflict("角色编码已存在");
        }
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        roleMapper.updateById(role);
        return RoleVO.from(role, userMapper.countUsersByRole(roleId));
    }

    @Transactional
    public void delete(Long roleId) {
        SysRole role = requireRole(roleId);
        if (SetHolder.PROTECTED_CODES.contains(role.getRoleCode())) {
            throw BusinessException.conflict("内置角色不允许删除");
        }
        if (userMapper.countUsersByRole(roleId) > 0) {
            throw BusinessException.conflict("角色仍分配给用户，不能删除");
        }
        roleMapper.deleteById(roleId);
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        return role;
    }

    private static final class SetHolder {
        private static final java.util.Set<String> PROTECTED_CODES = java.util.Set.of("ADMIN", "USER");
    }
}
