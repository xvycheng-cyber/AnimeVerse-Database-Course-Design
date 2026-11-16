package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.dto.PlatformSaveRequest;
import com.animeverse.service.PlatformService;
import com.animeverse.vo.PlatformVO;
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
@RequestMapping("/api/admin/platforms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlatformController {

    private final PlatformService platformService;

    @GetMapping
    public ApiResponse<List<PlatformVO>> list() {
        return ApiResponse.success(platformService.list());
    }

    @PostMapping
    @OperationLog(module = "PLATFORM", action = "CREATE")
    public ResponseEntity<ApiResponse<PlatformVO>> create(@Valid @RequestBody PlatformSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(platformService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "PLATFORM", action = "UPDATE")
    public ApiResponse<PlatformVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PlatformSaveRequest request
    ) {
        return ApiResponse.success(platformService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "PLATFORM", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        platformService.delete(id);
        return ApiResponse.success();
    }
}
