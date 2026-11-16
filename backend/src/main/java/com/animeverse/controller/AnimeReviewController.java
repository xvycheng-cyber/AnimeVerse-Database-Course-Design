package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.dto.ReviewRequest;
import com.animeverse.service.ReviewService;
import com.animeverse.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anime/{animeId}/reviews")
@RequiredArgsConstructor
public class AnimeReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @OperationLog(module = "REVIEW", action = "CREATE")
    public ResponseEntity<ApiResponse<ReviewVO>> create(
            @PathVariable Long animeId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(reviewService.create(animeId, request)));
    }
}
