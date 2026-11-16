package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.dto.LoginRequest;
import com.animeverse.dto.RegisterRequest;
import com.animeverse.entity.SysRole;
import com.animeverse.entity.SysUser;
import com.animeverse.entity.SysUserRole;
import com.animeverse.mapper.SysRoleMapper;
import com.animeverse.mapper.SysUserMapper;
import com.animeverse.mapper.SysUserRoleMapper;
import com.animeverse.security.AuthenticatedUser;
import com.animeverse.security.JwtService;
import com.animeverse.vo.AuthResponse;
import com.animeverse.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw BusinessException.conflict("用户名已存在");
        }
        if (userMapper.findByEmail(request.email()) != null) {
            throw BusinessException.conflict("邮箱已被注册");
        }
        SysRole userRole = roleMapper.findByRoleCode("USER");
        if (userRole == null) {
            throw BusinessException.conflict("系统缺少 USER 角色，请先初始化角色数据");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = SysUser.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .email(request.email())
                .status(1)
                .lastLoginAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        userRoleMapper.insertAssignment(SysUserRole.builder()
                .userId(user.getId())
                .roleId(userRole.getId())
                .assignedAt(now)
                .assignedBy(user.getId())
                .build());

        Set<String> roles = new LinkedHashSet<>(List.of(userRole.getRoleCode()));
        AuthenticatedUser principal = toPrincipal(user, roles);
        return new AuthResponse(jwtService.generateAccessToken(principal), toUserVO(user, roles));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
            SysUser user = requireUser(principal.id());
            LocalDateTime now = LocalDateTime.now();
            user.setLastLoginAt(now);
            user.setUpdatedAt(now);
            userMapper.updateById(user);
            return new AuthResponse(jwtService.generateAccessToken(principal), toUserVO(user, principal.roles()));
        } catch (BadCredentialsException exception) {
            throw BusinessException.unauthorized("用户名或密码错误");
        } catch (DisabledException exception) {
            throw BusinessException.forbidden("账户已停用");
        } catch (LockedException exception) {
            throw BusinessException.forbidden("账户已锁定");
        }
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在");
        }
        return user;
    }

    private AuthenticatedUser toPrincipal(SysUser user, Set<String> roles) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getStatus() != null && user.getStatus() == 1,
                roles
        );
    }

    private UserVO toUserVO(SysUser user, Set<String> roles) {
        return UserVO.from(user, List.copyOf(roles));
    }
}
