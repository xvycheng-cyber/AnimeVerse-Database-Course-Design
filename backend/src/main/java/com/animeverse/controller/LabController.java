package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.dto.AddAnimeProcedureRequest;
import com.animeverse.dto.FavoriteTriggerRequest;
import com.animeverse.dto.TransactionDemoRequest;
import com.animeverse.service.LabService;
import com.animeverse.vo.RankingVO;
import com.animeverse.vo.StudioStatisticsVO;
import com.animeverse.vo.TransactionDemoVO;
import com.animeverse.vo.TriggerFavoriteVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LabController {

    private final LabService labService;

    @GetMapping("/views/{viewName}")
    public ApiResponse<List<Map<String, Object>>> view(@PathVariable String viewName) {
        return ApiResponse.success(labService.queryView(viewName));
    }

    @PostMapping("/trigger/favorite")
    @OperationLog(module = "LAB", action = "TRIGGER_FAVORITE")
    public ApiResponse<TriggerFavoriteVO> favoriteTrigger(
            @Valid @RequestBody FavoriteTriggerRequest request
    ) {
        return ApiResponse.success(labService.triggerFavorite(request));
    }

    @PostMapping("/procedure/add-anime")
    @OperationLog(module = "LAB", action = "PROCEDURE_ADD_ANIME")
    public ApiResponse<Map<String, Object>> addAnime(
            @Valid @RequestBody AddAnimeProcedureRequest request
    ) {
        return ApiResponse.success(labService.addAnime(request));
    }

    @DeleteMapping("/procedure/anime/{animeId}")
    @OperationLog(module = "LAB", action = "PROCEDURE_DELETE_ANIME")
    public ApiResponse<Map<String, Object>> deleteAnime(
            @PathVariable @Min(1) Long animeId
    ) {
        return ApiResponse.success(labService.deleteAnime(animeId));
    }

    @GetMapping("/procedure/studio-statistics/{studioId}")
    public ApiResponse<StudioStatisticsVO> studioStatistics(@PathVariable Long studioId) {
        return ApiResponse.success(labService.studioStatistics(studioId));
    }

    @GetMapping("/procedure/anime-ranking")
    public ApiResponse<List<RankingVO>> ranking(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.success(labService.animeRanking(limit));
    }

    @GetMapping("/cursor/studio-report")
    public ApiResponse<List<StudioStatisticsVO>> studioReport() {
        return ApiResponse.success(labService.studioReport());
    }

    @PostMapping("/transaction/demo")
    public ApiResponse<TransactionDemoVO> transactionDemo(
            @Valid @RequestBody TransactionDemoRequest request
    ) {
        // sp_transaction_demo owns its persistent audit row and rollback semantics.
        return ApiResponse.success(labService.transactionDemo(request));
    }
}
