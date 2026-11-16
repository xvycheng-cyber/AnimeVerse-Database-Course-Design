package com.animeverse.mapper;

import com.animeverse.entity.Anime;
import com.animeverse.vo.AnimeListVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AnimeMapper extends BaseMapper<Anime> {

    @Select("""
            <script>
            SELECT a.id, a.title, a.title_jp, a.title_en, a.title_jp AS original_title,
                   a.studio_id, s.studio_name,
                   a.cover_url, a.synopsis, a.release_date, YEAR(a.release_date) AS release_year,
                   a.end_date, a.status, a.episode_count, a.average_rating,
                   a.average_rating AS rating, a.rating_count,
                   COALESCE(a.favorite_count, 0) AS favorite_count,
                   COALESCE(a.review_count, 0) AS review_count,
                   COALESCE(a.view_count, 0) AS view_count,
                   EXISTS (
                     SELECT 1 FROM user_favorite uf
                     WHERE uf.anime_id = a.id AND uf.user_id = #{currentUserId}
                   ) AS favorited,
                   a.created_at
            FROM anime a
            LEFT JOIN anime_studio s ON s.id = a.studio_id
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND (a.title LIKE CONCAT('%', #{keyword}, '%')
                   OR a.title_jp LIKE CONCAT('%', #{keyword}, '%')
                   OR a.title_en LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="genreId != null">
              AND EXISTS (
                SELECT 1 FROM anime_genre_relation agr
                WHERE agr.anime_id = a.id AND agr.genre_id = #{genreId}
              )
            </if>
            <if test="studioId != null">
              AND a.studio_id = #{studioId}
            </if>
            <if test="status != null and status != ''">
              AND a.status = #{status}
            </if>
            ORDER BY
              CASE WHEN #{sort} = 'rating' THEN a.average_rating END DESC,
              CASE WHEN #{sort} = 'releaseDate' THEN a.release_date END DESC,
              CASE WHEN #{sort} = 'favorites' THEN a.favorite_count END DESC,
              CASE WHEN #{sort} = 'views' THEN a.view_count END DESC,
              CASE WHEN #{sort} = 'latest' THEN a.release_date END DESC,
              a.id DESC
            </script>
            """)
    IPage<AnimeListVO> selectAnimePage(
            Page<AnimeListVO> page,
            @Param("keyword") String keyword,
            @Param("genreId") Long genreId,
            @Param("studioId") Long studioId,
            @Param("status") String status,
            @Param("sort") String sort,
            @Param("publicOnly") boolean publicOnly,
            @Param("currentUserId") Long currentUserId
    );

    @Select("""
            SELECT a.id, a.title, a.title_jp, a.title_en, a.title_jp AS original_title,
                   a.studio_id, s.studio_name,
                   a.cover_url, a.synopsis, a.release_date, YEAR(a.release_date) AS release_year,
                   a.end_date, a.status, a.episode_count, a.average_rating,
                   a.average_rating AS rating, a.rating_count,
                   COALESCE(a.favorite_count, 0) AS favorite_count,
                   COALESCE(a.review_count, 0) AS review_count,
                   COALESCE(a.view_count, 0) AS view_count,
                   EXISTS (
                     SELECT 1 FROM user_favorite uf
                     WHERE uf.anime_id = a.id AND uf.user_id = #{currentUserId}
                   ) AS favorited,
                   a.created_at
            FROM anime a
            LEFT JOIN anime_studio s ON s.id = a.studio_id
            WHERE a.id = #{animeId}
            """)
    AnimeListVO selectAnimeView(
            @Param("animeId") Long animeId,
            @Param("currentUserId") Long currentUserId
    );

    @Select("SELECT COALESCE(favorite_count, 0) FROM anime WHERE id = #{animeId}")
    Long selectFavoriteCount(@Param("animeId") Long animeId);
}
