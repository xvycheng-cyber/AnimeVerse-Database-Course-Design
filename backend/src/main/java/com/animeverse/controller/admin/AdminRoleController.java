package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.dto.RoleSaveRequest;
import com.animeverse.service.RoleService;
import com.animeverse.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleVO>> list() {
        return ApiResponse.success(roleService.list());
    }

    @PostMapping
    @OperationLog(module = "ROLE", action = "CREATE")
    public ResponseEntity<ApiResponse<RoleVO>> create(@Valid @RequestBody RoleSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(roleService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "ROLE", action = "UPDATE")
    public ApiResponse<RoleVO> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveRequest request
    ) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "ROLE", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }
}
