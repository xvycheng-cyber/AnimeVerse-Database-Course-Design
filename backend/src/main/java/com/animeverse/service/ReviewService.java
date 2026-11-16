package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.ReviewRequest;
import com.animeverse.entity.AnimeReview;
import com.animeverse.mapper.AnimeReviewMapper;
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.ReviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final AnimeReviewMapper reviewMapper;
    private final AnimeService animeService;

    public PageResult<ReviewVO> publicPage(Long animeId, long page, long size) {
        animeService.requireAnime(animeId);
        IPage<ReviewVO> result = reviewMapper.selectReviewPage(
                Page.of(page, size), animeId, null, null, 1, false
        );
        return PageResult.of(result);
    }

    public PageResult<ReviewVO> currentUserPage(long page, long size) {
        IPage<ReviewVO> result = reviewMapper.selectReviewPage(
                Page.of(page, size), null, SecurityUtils.currentUserId(), null, null, true
        );
        return PageResult.of(result);
    }

    public PageResult<ReviewVO> adminPage(long page, long size, String keyword, Integer status) {
        IPage<ReviewVO> result = reviewMapper.selectReviewPage(
                Page.of(page, size), null, null, keyword, status, true
        );
        return PageResult.of(result);
    }

    @Transactional
    public ReviewVO create(Long animeId, ReviewRequest request) {
        Long userId = SecurityUtils.currentUserId();
        animeService.requireAnime(animeId);
        long existing = reviewMapper.selectCount(new LambdaQueryWrapper<AnimeReview>()
                .eq(AnimeReview::getUserId, userId)
                .eq(AnimeReview::getAnimeId, animeId));
        if (existing > 0) {
            throw BusinessException.conflict("每个用户对同一部动漫只能发表一条评论");
        }
        LocalDateTime now = LocalDateTime.now();
        AnimeReview review = AnimeReview.builder()
                .userId(userId)
                .animeId(animeId)
                .rating(request.rating())
                .content(request.content())
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        reviewMapper.insert(review);
        return getReview(review.getId());
    }

    @Transactional
    public ReviewVO update(Long reviewId, ReviewRequest request) {
        AnimeReview review = requireOwnedReview(reviewId);
        review.setRating(request.rating());
        review.setContent(request.content());
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        return getReview(reviewId);
    }

    @Transactional
    public void deleteOwn(Long reviewId) {
        AnimeReview review = requireOwnedReview(reviewId);
        reviewMapper.deleteById(review.getId());
    }

    @Transactional
    public ReviewVO setHidden(Long reviewId, boolean hidden) {
        AnimeReview review = requireReview(reviewId);
        review.setStatus(hidden ? 0 : 1);
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        return getReview(reviewId);
    }

    @Transactional
    public void adminDelete(Long reviewId) {
        requireReview(reviewId);
        reviewMapper.deleteById(reviewId);
    }

    public ReviewVO getReview(Long reviewId) {
        ReviewVO review = reviewMapper.selectReviewViewById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("评论不存在");
        }
        return review;
    }

    private AnimeReview requireOwnedReview(Long reviewId) {
        AnimeReview review = requireReview(reviewId);
        if (!review.getUserId().equals(SecurityUtils.currentUserId())) {
            throw BusinessException.forbidden("只能管理自己的评论");
        }
        return review;
    }

    private AnimeReview requireReview(Long reviewId) {
        AnimeReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("评论不存在");
        }
        return review;
    }
}
