package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.service.StatisticsService;
import com.animeverse.vo.StatisticsOverviewVO;
import com.animeverse.vo.TrendPointVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewVO> overview() {
        return ApiResponse.success(statisticsService.overview());
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> trend(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        return ApiResponse.success(statisticsService.trend(days));
    }
}
