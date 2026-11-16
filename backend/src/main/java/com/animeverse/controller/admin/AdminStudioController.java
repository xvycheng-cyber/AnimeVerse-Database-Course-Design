package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.StudioSaveRequest;
import com.animeverse.service.StudioService;
import com.animeverse.vo.StudioVO;
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
@RequestMapping("/api/admin/studios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminStudioController {

    private final StudioService studioService;

    @GetMapping
    public ApiResponse<PageResult<StudioVO>> page(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) long page,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1)
            @jakarta.validation.constraints.Max(100) long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(studioService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudioVO> detail(@PathVariable Long id) {
        return ApiResponse.success(studioService.get(id));
    }

    @PostMapping
    @OperationLog(module = "STUDIO", action = "CREATE")
    public ResponseEntity<ApiResponse<StudioVO>> create(@Valid @RequestBody StudioSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(studioService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "STUDIO", action = "UPDATE")
    public ApiResponse<StudioVO> update(
            @PathVariable Long id,
            @Valid @RequestBody StudioSaveRequest request
    ) {
        return ApiResponse.success(studioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "STUDIO", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studioService.delete(id);
        return ApiResponse.success();
    }
}
