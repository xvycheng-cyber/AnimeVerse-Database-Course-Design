package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.PageResult;
import com.animeverse.service.StudioService;
import com.animeverse.vo.StudioVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioController {

    private final StudioService studioService;

    @GetMapping
    public ApiResponse<PageResult<StudioVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(studioService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudioVO> detail(@PathVariable Long id) {
        return ApiResponse.success(studioService.get(id));
    }
}
