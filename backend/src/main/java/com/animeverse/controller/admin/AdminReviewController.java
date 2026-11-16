package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.ReviewVisibilityRequest;
import com.animeverse.service.ReviewService;
import com.animeverse.vo.ReviewVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(reviewService.adminPage(page, size, keyword, status));
    }

    @PutMapping({"/{id}/visibility", "/{id}/hidden"})
    @OperationLog(module = "REVIEW", action = "SET_VISIBILITY")
    public ApiResponse<ReviewVO> setVisibility(
            @PathVariable Long id,
            @Valid @RequestBody ReviewVisibilityRequest request
    ) {
        return ApiResponse.success(reviewService.setHidden(id, request.hidden()));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "REVIEW", action = "ADMIN_DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reviewService.adminDelete(id);
        return ApiResponse.success();
    }
}
