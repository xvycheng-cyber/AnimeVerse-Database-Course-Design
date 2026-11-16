package com.animeverse.vo;

import com.animeverse.entity.AnimeEpisode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EpisodeVO(
        Long id,
        Long animeId,
        Integer episodeNumber,
        String title,
        String synopsis,
        Integer durationSeconds,
        LocalDate airDate,
        Integer durationMinutes,
        String videoUrl,
        String description,
        LocalDate releaseDate,
        LocalDateTime createdAt
) {
    public static EpisodeVO from(AnimeEpisode episode) {
        Integer durationMinutes = episode.getDurationSeconds() == null
                ? null
                : (episode.getDurationSeconds() + 59) / 60;
        return new EpisodeVO(
                episode.getId(),
                episode.getAnimeId(),
                episode.getEpisodeNumber(),
                episode.getTitle(),
                episode.getSynopsis(),
                episode.getDurationSeconds(),
                episode.getAirDate(),
                durationMinutes,
                null,
                episode.getSynopsis(),
                episode.getAirDate(),
                episode.getCreatedAt()
        );
    }
}
