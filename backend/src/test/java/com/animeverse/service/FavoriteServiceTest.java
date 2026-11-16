package com.animeverse.service;

import com.animeverse.entity.Anime;
import com.animeverse.entity.UserFavorite;
import com.animeverse.mapper.UserFavoriteMapper;
import com.animeverse.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserFavoriteMapper favoriteMapper;
    @Mock
    private AnimeService animeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseJwtPrincipalInsteadOfClientSuppliedUserId() {
        authenticate(88L);
        when(animeService.requireAnime(5L)).thenReturn(Anime.builder().id(5L).build());
        when(favoriteMapper.countByUserAndAnime(88L, 5L)).thenReturn(0L);
        FavoriteService service = new FavoriteService(favoriteMapper, animeService);

        service.add(5L);

        ArgumentCaptor<UserFavorite> captor = ArgumentCaptor.forClass(UserFavorite.class);
        verify(favoriteMapper).insertFavorite(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(88L);
        assertThat(captor.getValue().getAnimeId()).isEqualTo(5L);
    }

    @Test
    void shouldNotInsertDuplicateFavorite() {
        authenticate(88L);
        when(animeService.requireAnime(5L)).thenReturn(Anime.builder().id(5L).build());
        when(favoriteMapper.countByUserAndAnime(88L, 5L)).thenReturn(1L);
        FavoriteService service = new FavoriteService(favoriteMapper, animeService);

        service.add(5L);

        verify(favoriteMapper, org.mockito.Mockito.never()).insertFavorite(any(UserFavorite.class));
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
