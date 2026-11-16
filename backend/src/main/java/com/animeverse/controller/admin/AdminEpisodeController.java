package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.EpisodeSaveRequest;
import com.animeverse.service.EpisodeService;
import com.animeverse.vo.EpisodeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Validated
@RestController
@RequestMapping("/api/admin/episodes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEpisodeController {

    private final EpisodeService episodeService;

    @GetMapping
    public ApiResponse<PageResult<EpisodeVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) Long animeId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(episodeService.page(page, size, animeId, keyword));
    }

    @PostMapping
    @OperationLog(module = "EPISODE", action = "CREATE")
    public ResponseEntity<ApiResponse<EpisodeVO>> create(@Valid @RequestBody EpisodeSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(episodeService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "EPISODE", action = "UPDATE")
    public ApiResponse<EpisodeVO> update(
            @PathVariable Long id,
            @Valid @RequestBody EpisodeSaveRequest request
    ) {
        return ApiResponse.success(episodeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "EPISODE", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        episodeService.delete(id);
        return ApiResponse.success();
    }
}
