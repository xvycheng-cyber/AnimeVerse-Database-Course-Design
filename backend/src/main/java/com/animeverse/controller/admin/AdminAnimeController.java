package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.AnimeSaveRequest;
import com.animeverse.service.AnimeService;
import com.animeverse.vo.AnimeDetailVO;
import com.animeverse.vo.AnimeListVO;
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
@RequestMapping("/api/admin/anime")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnimeController {

    private final AnimeService animeService;

    @GetMapping
    public ApiResponse<PageResult<AnimeListVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Long studioId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ApiResponse.success(
                animeService.page(page, size, keyword, genreId, studioId, status, sort, false)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AnimeDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(animeService.get(id));
    }

    @PostMapping
    @OperationLog(module = "ANIME", action = "CREATE")
    public ResponseEntity<ApiResponse<AnimeDetailVO>> create(@Valid @RequestBody AnimeSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(animeService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "ANIME", action = "UPDATE")
    public ApiResponse<AnimeDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody AnimeSaveRequest request
    ) {
        return ApiResponse.success(animeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "ANIME", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        animeService.delete(id);
        return ApiResponse.success();
    }
}
