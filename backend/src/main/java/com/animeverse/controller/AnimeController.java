package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.PageResult;
import com.animeverse.service.AnimeService;
import com.animeverse.service.EpisodeService;
import com.animeverse.service.ReviewService;
import com.animeverse.vo.AnimeDetailVO;
import com.animeverse.vo.AnimeListVO;
import com.animeverse.vo.EpisodeVO;
import com.animeverse.vo.RankingVO;
import com.animeverse.vo.ReviewVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/anime")
@RequiredArgsConstructor
public class AnimeController {

    private final AnimeService animeService;
    private final EpisodeService episodeService;
    private final ReviewService reviewService;

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
                animeService.page(page, size, keyword, genreId, studioId, status, sort, true)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AnimeDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(animeService.get(id));
    }

    @GetMapping("/{id}/episodes")
    public ApiResponse<List<EpisodeVO>> episodes(@PathVariable Long id) {
        return ApiResponse.success(episodeService.listByAnime(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<PageResult<ReviewVO>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size
    ) {
        return ApiResponse.success(reviewService.publicPage(id, page, size));
    }

    @GetMapping("/ranking")
    public ApiResponse<List<RankingVO>> ranking(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.success(animeService.ranking(limit));
    }
}
