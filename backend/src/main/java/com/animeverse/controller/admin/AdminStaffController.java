package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.StaffSaveRequest;
import com.animeverse.service.StaffService;
import com.animeverse.vo.StaffVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping
    public ApiResponse<PageResult<StaffVO>> page(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) long page,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1)
            @jakarta.validation.constraints.Max(100) long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(staffService.page(page, size, keyword));
    }

    @PostMapping
    @OperationLog(module = "STAFF", action = "CREATE")
    public ResponseEntity<ApiResponse<StaffVO>> create(@Valid @RequestBody StaffSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "STAFF", action = "UPDATE")
    public ApiResponse<StaffVO> update(
            @PathVariable Long id,
            @Valid @RequestBody StaffSaveRequest request
    ) {
        return ApiResponse.success(staffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "STAFF", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ApiResponse.success();
    }
}
