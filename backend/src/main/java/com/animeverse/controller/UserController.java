package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.common.PageResult;
import com.animeverse.dto.ProfileUpdateRequest;
import com.animeverse.dto.ReviewRequest;
import com.animeverse.dto.WatchHistoryRequest;
import com.animeverse.service.FavoriteService;
import com.animeverse.service.ReviewService;
import com.animeverse.service.UserService;
import com.animeverse.service.WatchHistoryService;
import com.animeverse.vo.FavoriteVO;
import com.animeverse.vo.ReviewVO;
import com.animeverse.vo.UserVO;
import com.animeverse.vo.WatchHistoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final FavoriteService favoriteService;
    private final WatchHistoryService watchHistoryService;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteVO>> favorites() {
        return ApiResponse.success(favoriteService.listCurrentUser());
    }

    @PostMapping("/favorites/{animeId}")
    @OperationLog(module = "FAVORITE", action = "ADD")
    public ApiResponse<Void> addFavorite(@PathVariable Long animeId) {
        favoriteService.add(animeId);
        return ApiResponse.success();
    }

    @DeleteMapping("/favorites/{animeId}")
    @OperationLog(module = "FAVORITE", action = "REMOVE")
    public ApiResponse<Void> removeFavorite(@PathVariable Long animeId) {
        favoriteService.remove(animeId);
        return ApiResponse.success();
    }

    @GetMapping("/watch-history")
    public ApiResponse<List<WatchHistoryVO>> watchHistory() {
        return ApiResponse.success(watchHistoryService.listCurrentUser());
    }

    @PostMapping("/watch-history")
    @OperationLog(module = "WATCH_HISTORY", action = "UPSERT")
    public ApiResponse<Void> recordWatch(@Valid @RequestBody WatchHistoryRequest request) {
        watchHistoryService.record(request);
        return ApiResponse.success();
    }

    @GetMapping("/reviews")
    public ApiResponse<PageResult<ReviewVO>> reviews(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size
    ) {
        return ApiResponse.success(reviewService.currentUserPage(page, size));
    }

    @PutMapping("/reviews/{id}")
    @OperationLog(module = "REVIEW", action = "UPDATE_OWN")
    public ApiResponse<ReviewVO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(reviewService.update(id, request));
    }

    @DeleteMapping("/reviews/{id}")
    @OperationLog(module = "REVIEW", action = "DELETE_OWN")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteOwn(id);
        return ApiResponse.success();
    }

    @GetMapping("/profile")
    public ApiResponse<UserVO> profile() {
        return ApiResponse.success(userService.currentProfile());
    }

    @PutMapping("/profile")
    @OperationLog(module = "PROFILE", action = "UPDATE")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateCurrentProfile(request));
    }
}
