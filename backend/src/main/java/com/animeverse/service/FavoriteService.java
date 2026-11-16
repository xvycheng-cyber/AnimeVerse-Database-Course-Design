package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.entity.UserFavorite;
import com.animeverse.mapper.UserFavoriteMapper;
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final AnimeService animeService;

    public List<FavoriteVO> listCurrentUser() {
        return favoriteMapper.selectByUserId(SecurityUtils.currentUserId());
    }

    @Transactional
    public void add(Long animeId) {
        Long userId = SecurityUtils.currentUserId();
        animeService.requireAnime(animeId);
        if (favoriteMapper.countByUserAndAnime(userId, animeId) > 0) {
            return;
        }
        favoriteMapper.insertFavorite(UserFavorite.builder()
                .userId(userId)
                .animeId(animeId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void remove(Long animeId) {
        Long userId = SecurityUtils.currentUserId();
        favoriteMapper.deleteFavorite(userId, animeId);
    }
}
