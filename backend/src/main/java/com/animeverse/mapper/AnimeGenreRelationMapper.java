package com.animeverse.mapper;

import com.animeverse.entity.AnimeGenreRelation;
import com.animeverse.vo.GenreVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AnimeGenreRelationMapper {

    @Insert("""
            INSERT INTO anime_genre_relation (anime_id, genre_id, created_at)
            VALUES (#{animeId}, #{genreId}, #{createdAt})
            """)
    int insertRelation(AnimeGenreRelation relation);

    @Delete("DELETE FROM anime_genre_relation WHERE anime_id = #{animeId}")
    int deleteByAnimeId(@Param("animeId") Long animeId);

    @Select("""
            SELECT g.id, g.genre_name AS name, g.genre_slug AS slug, g.description,
                   g.genre_name, g.genre_slug, g.created_at
            FROM anime_genre g
            JOIN anime_genre_relation r ON r.genre_id = g.id
            WHERE r.anime_id = #{animeId}
            ORDER BY g.id
            """)
    List<GenreVO> selectGenresByAnimeId(@Param("animeId") Long animeId);

    @Select("SELECT COUNT(*) FROM anime_genre_relation WHERE genre_id = #{genreId}")
    long countByGenreId(@Param("genreId") Long genreId);
}
