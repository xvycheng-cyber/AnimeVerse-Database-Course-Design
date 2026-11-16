package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.config.JwtProperties;
import com.animeverse.dto.LoginRequest;
import com.animeverse.dto.RegisterRequest;
import com.animeverse.entity.SysRole;
import com.animeverse.entity.SysUser;
import com.animeverse.entity.SysUserRole;
import com.animeverse.mapper.SysRoleMapper;
import com.animeverse.mapper.SysUserMapper;
import com.animeverse.mapper.SysUserRoleMapper;
import com.animeverse.security.JwtService;
import com.animeverse.vo.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String FIXTURE_PASSWORD = "Fixture_BCrypt#42";

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        JwtService jwtService = new JwtService(
                new JwtProperties("test-secret-that-is-longer-than-thirty-two-bytes", 60)
        );
        authService = new AuthService(
                userMapper,
                roleMapper,
                userRoleMapper,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
    }

    @Test
    void shouldRegisterWithBcryptHashAndDefaultUserRole() {
        when(roleMapper.findByRoleCode("USER")).thenReturn(
                SysRole.builder().id(2L).roleCode("USER").roleName("普通用户").build()
        );
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        AuthResponse response = authService.register(
                new RegisterRequest("sakura01", FIXTURE_PASSWORD, "Sakura", "sakura@example.com")
        );

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        SysUser inserted = userCaptor.getValue();
        assertThat(inserted.getPasswordHash()).isNotEqualTo(FIXTURE_PASSWORD);
        assertThat(passwordEncoder.matches(FIXTURE_PASSWORD, inserted.getPasswordHash())).isTrue();
        assertThat(inserted.getStatus()).isEqualTo(1);

        ArgumentCaptor<SysUserRole> roleCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insertAssignment(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(roleCaptor.getValue().getRoleId()).isEqualTo(2L);
        assertThat(response.token()).isNotBlank();
        assertThat(response.user().roles()).containsExactly("USER");
    }

    @Test
    void shouldRejectDuplicateUsernameBeforeInsert() {
        when(userMapper.findByUsername("sakura01")).thenReturn(SysUser.builder().id(1L).build());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("sakura01", FIXTURE_PASSWORD, "Sakura", "sakura@example.com")
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void disabledAccountLoginReturnsForbiddenInsteadOfGenericUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("account disabled"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("disabled-user", FIXTURE_PASSWORD)
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("账户已停用");
        });
    }

    @Test
    void lockedAccountLoginRetainsForbiddenSemantics() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("account locked"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("locked-user", FIXTURE_PASSWORD)
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("账户已锁定");
        });
    }
}
