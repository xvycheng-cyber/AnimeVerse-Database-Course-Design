package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.dto.WatchHistoryRequest;
import com.animeverse.entity.AnimeEpisode;
import com.animeverse.entity.UserWatchHistory;
import com.animeverse.mapper.UserWatchHistoryMapper;
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.WatchHistoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private final UserWatchHistoryMapper watchHistoryMapper;
    private final EpisodeService episodeService;

    public List<WatchHistoryVO> listCurrentUser() {
        return watchHistoryMapper.selectByUserId(SecurityUtils.currentUserId());
    }

    @Transactional
    public void record(WatchHistoryRequest request) {
        Long userId = SecurityUtils.currentUserId();
        AnimeEpisode episode = episodeService.requireEpisode(request.episodeId());
        if (episode.getDurationSeconds() != null
                && request.progressSeconds() > episode.getDurationSeconds()) {
            throw BusinessException.badRequest("观看进度不能超过剧集时长");
        }
        int completed = request.completed() != null
                ? (request.completed() ? 1 : 0)
                : episode.getDurationSeconds() != null
                        && request.progressSeconds() >= episode.getDurationSeconds() ? 1 : 0;
        UserWatchHistory history = watchHistoryMapper.selectOne(new LambdaQueryWrapper<UserWatchHistory>()
                .eq(UserWatchHistory::getUserId, userId)
                .eq(UserWatchHistory::getEpisodeId, request.episodeId())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (history == null) {
            watchHistoryMapper.insert(UserWatchHistory.builder()
                    .userId(userId)
                    .episodeId(request.episodeId())
                    .progressSeconds(request.progressSeconds())
                    .completed(completed)
                    .lastWatchedAt(now)
                    .createdAt(now)
                    .build());
        } else {
            history.setProgressSeconds(request.progressSeconds());
            history.setCompleted(completed);
            history.setLastWatchedAt(now);
            watchHistoryMapper.updateById(history);
        }
    }
}
