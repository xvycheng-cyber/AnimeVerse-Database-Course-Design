package com.animeverse.mapper;

import com.animeverse.entity.AnimeEpisode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AnimeEpisodeMapper extends BaseMapper<AnimeEpisode> {

    @Select("""
            SELECT id, anime_id, episode_number, title, synopsis, duration_seconds, air_date, created_at, updated_at
            FROM anime_episode
            WHERE anime_id = #{animeId}
            ORDER BY episode_number
            """)
    List<AnimeEpisode> selectByAnimeId(@Param("animeId") Long animeId);
}
