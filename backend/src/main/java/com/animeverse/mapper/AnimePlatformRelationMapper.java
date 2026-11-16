package com.animeverse.mapper;

import com.animeverse.entity.AnimePlatformRelation;
import com.animeverse.vo.PlatformLinkVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AnimePlatformRelationMapper {

    @Insert("""
            INSERT INTO anime_platform_relation
                (anime_id, platform_id, external_url, available_from, available_until, created_at)
            VALUES
                (#{animeId}, #{platformId}, #{externalUrl}, #{availableFrom}, #{availableUntil}, #{createdAt})
            """)
    int insertRelation(AnimePlatformRelation relation);

    @Delete("DELETE FROM anime_platform_relation WHERE anime_id = #{animeId}")
    int deleteByAnimeId(@Param("animeId") Long animeId);

    @Select("""
            SELECT p.id AS platform_id, p.platform_name AS name, p.platform_name,
                   p.platform_code, NULL AS logo_url, r.external_url AS watch_url,
                   r.external_url, r.available_from, r.available_until
            FROM streaming_platform p
            JOIN anime_platform_relation r ON r.platform_id = p.id
            WHERE r.anime_id = #{animeId}
            ORDER BY p.id
            """)
    List<PlatformLinkVO> selectPlatformsByAnimeId(@Param("animeId") Long animeId);

    @Select("SELECT COUNT(*) FROM anime_platform_relation WHERE platform_id = #{platformId}")
    long countByPlatformId(@Param("platformId") Long platformId);
}
