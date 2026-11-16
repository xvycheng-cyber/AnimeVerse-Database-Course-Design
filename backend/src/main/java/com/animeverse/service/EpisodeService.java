package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.EpisodeSaveRequest;
import com.animeverse.entity.AnimeEpisode;
import com.animeverse.mapper.AnimeEpisodeMapper;
import com.animeverse.mapper.UserWatchHistoryMapper;
import com.animeverse.vo.EpisodeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final AnimeEpisodeMapper episodeMapper;
    private final UserWatchHistoryMapper watchHistoryMapper;
    private final AnimeService animeService;

    public List<EpisodeVO> listByAnime(Long animeId) {
        animeService.requireAnime(animeId);
        return episodeMapper.selectByAnimeId(animeId).stream().map(EpisodeVO::from).toList();
    }

    public PageResult<EpisodeVO> page(long page, long size, Long animeId, String keyword) {
        LambdaQueryWrapper<AnimeEpisode> wrapper = new LambdaQueryWrapper<AnimeEpisode>()
                .eq(animeId != null, AnimeEpisode::getAnimeId, animeId)
                .like(keyword != null && !keyword.isBlank(), AnimeEpisode::getTitle, keyword)
                .orderByAsc(AnimeEpisode::getAnimeId)
                .orderByAsc(AnimeEpisode::getEpisodeNumber);
        Page<AnimeEpisode> result = episodeMapper.selectPage(Page.of(page, size), wrapper);
        List<EpisodeVO> records = result.getRecords().stream().map(EpisodeVO::from).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public EpisodeVO create(EpisodeSaveRequest request) {
        animeService.requireAnime(request.animeId());
        ensureNumberUnique(request.animeId(), request.episodeNumber(), null);
        rejectUnsupportedVideoUrl(request.videoUrl());
        LocalDateTime now = LocalDateTime.now();
        AnimeEpisode episode = AnimeEpisode.builder()
                .animeId(request.animeId())
                .episodeNumber(request.episodeNumber())
                .title(request.title().trim())
                .synopsis(resolveSynopsis(request))
                .durationSeconds(resolveDurationSeconds(request))
                .airDate(resolveAirDate(request))
                .createdAt(now)
                .updatedAt(now)
                .build();
        episodeMapper.insert(episode);
        return EpisodeVO.from(episode);
    }

    @Transactional
    public EpisodeVO update(Long episodeId, EpisodeSaveRequest request) {
        AnimeEpisode episode = requireEpisode(episodeId);
        animeService.requireAnime(request.animeId());
        ensureNumberUnique(request.animeId(), request.episodeNumber(), episodeId);
        rejectUnsupportedVideoUrl(request.videoUrl());
        episode.setAnimeId(request.animeId());
        episode.setEpisodeNumber(request.episodeNumber());
        episode.setTitle(request.title().trim());
        episode.setSynopsis(resolveSynopsis(request));
        episode.setDurationSeconds(resolveDurationSeconds(request));
        episode.setAirDate(resolveAirDate(request));
        episode.setUpdatedAt(LocalDateTime.now());
        episodeMapper.updateById(episode);
        return EpisodeVO.from(episode);
    }

    @Transactional
    public void delete(Long episodeId) {
        requireEpisode(episodeId);
        if (watchHistoryMapper.countByEpisodeId(episodeId) > 0) {
            throw BusinessException.conflict("剧集已有观看历史，不能删除");
        }
        episodeMapper.deleteById(episodeId);
    }

    public AnimeEpisode requireEpisode(Long episodeId) {
        AnimeEpisode episode = episodeMapper.selectById(episodeId);
        if (episode == null) {
            throw BusinessException.notFound("剧集不存在");
        }
        return episode;
    }

    private void ensureNumberUnique(Long animeId, Integer episodeNumber, Long excludeId) {
        AnimeEpisode existing = episodeMapper.selectOne(new LambdaQueryWrapper<AnimeEpisode>()
                .eq(AnimeEpisode::getAnimeId, animeId)
                .eq(AnimeEpisode::getEpisodeNumber, episodeNumber)
                .last("LIMIT 1"));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw BusinessException.conflict("该动漫的剧集编号已存在");
        }
    }

    private Integer resolveDurationSeconds(EpisodeSaveRequest request) {
        if (request.durationSeconds() != null) {
            return request.durationSeconds();
        }
        return request.durationMinutes() == null ? null : request.durationMinutes() * 60;
    }

    private String resolveSynopsis(EpisodeSaveRequest request) {
        String synopsis = firstNonBlank(request.synopsis(), request.description());
        return synopsis;
    }

    private LocalDate resolveAirDate(EpisodeSaveRequest request) {
        return request.airDate() != null ? request.airDate() : request.releaseDate();
    }

    private void rejectUnsupportedVideoUrl(String videoUrl) {
        if (videoUrl != null && !videoUrl.isBlank()) {
            throw BusinessException.badRequest(
                    "anime_episode 表没有 video_url 字段；播放地址应写入 anime_platform_relation.external_url"
            );
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }
}
