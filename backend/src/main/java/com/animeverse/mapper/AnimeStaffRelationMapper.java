package com.animeverse.mapper;

import com.animeverse.entity.AnimeStaffRelation;
import com.animeverse.vo.StaffCreditVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AnimeStaffRelationMapper {

    @Insert("""
            INSERT INTO anime_staff_relation (anime_id, staff_id, role_type, created_at)
            VALUES (#{animeId}, #{staffId}, #{roleType}, #{createdAt})
            """)
    int insertRelation(AnimeStaffRelation relation);

    @Delete("DELETE FROM anime_staff_relation WHERE anime_id = #{animeId}")
    int deleteByAnimeId(@Param("animeId") Long animeId);

    @Select("""
            SELECT s.id AS staff_id, s.staff_name AS name, s.staff_name,
                   s.staff_name_jp, r.role_type AS role_name, r.role_type
            FROM anime_staff s
            JOIN anime_staff_relation r ON r.staff_id = s.id
            WHERE r.anime_id = #{animeId}
            ORDER BY s.id, r.role_type
            """)
    List<StaffCreditVO> selectStaffByAnimeId(@Param("animeId") Long animeId);

    @Select("SELECT COUNT(*) FROM anime_staff_relation WHERE staff_id = #{staffId}")
    long countByStaffId(@Param("staffId") Long staffId);
}
