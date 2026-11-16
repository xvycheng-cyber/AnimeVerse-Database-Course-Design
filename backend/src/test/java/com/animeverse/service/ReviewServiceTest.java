package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.dto.ReviewRequest;
import com.animeverse.entity.Anime;
import com.animeverse.entity.AnimeReview;
import com.animeverse.mapper.AnimeReviewMapper;
import com.animeverse.security.AuthenticatedUser;
import com.animeverse.vo.ReviewVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private AnimeReviewMapper reviewMapper;
    @Mock
    private AnimeService animeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectUpdatingAnotherUsersReview() {
        authenticate(7L);
        when(reviewMapper.selectById(3L)).thenReturn(
                AnimeReview.builder().id(3L).userId(99L).animeId(1L).build()
        );
        ReviewService service = new ReviewService(reviewMapper, animeService);

        assertThatThrownBy(() -> service.update(3L, new ReviewRequest(9, "changed")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能管理自己的评论");
    }

    @Test
    void shouldCreateReviewForCurrentPrincipal() {
        authenticate(7L);
        when(animeService.requireAnime(1L)).thenReturn(Anime.builder().id(1L).build());
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.insert(any(AnimeReview.class))).thenAnswer(invocation -> {
            AnimeReview review = invocation.getArgument(0);
            review.setId(21L);
            return 1;
        });
        when(reviewMapper.selectReviewViewById(21L)).thenReturn(review(21L, 7L));
        ReviewService service = new ReviewService(reviewMapper, animeService);

        ReviewVO result = service.create(1L, new ReviewRequest(9, "很好"));

        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getAnimeId()).isEqualTo(1L);
    }

    private ReviewVO review(Long id, Long userId) {
        ReviewVO review = new ReviewVO();
        review.setId(id);
        review.setUserId(userId);
        review.setAnimeId(1L);
        return review;
    }

    private void authenticate(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(
                userId,
                "sakura",
                "hash",
                "Sakura",
                true,
                Set.of("USER")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
