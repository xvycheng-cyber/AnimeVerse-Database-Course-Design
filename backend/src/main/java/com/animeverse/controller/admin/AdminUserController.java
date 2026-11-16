package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.UserRolesRequest;
import com.animeverse.dto.UserStatusRequest;
import com.animeverse.service.UserService;
import com.animeverse.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(userService.page(page, size, keyword));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "USER", action = "SET_STATUS")
    public ApiResponse<UserVO> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ApiResponse.success(userService.setEnabled(id, request.enabled()));
    }

    @PutMapping("/{id}/roles")
    @OperationLog(module = "USER", action = "SET_ROLES")
    public ApiResponse<UserVO> setRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRolesRequest request
    ) {
        return ApiResponse.success(userService.setRoles(id, request.roleIds()));
    }
}
