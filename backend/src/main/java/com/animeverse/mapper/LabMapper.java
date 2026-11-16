package com.animeverse.mapper;

import com.animeverse.vo.RankingVO;
import com.animeverse.vo.StudioStatisticsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface LabMapper {

    @Select("SELECT * FROM ${viewName}")
    List<Map<String, Object>> selectView(@Param("viewName") String viewName);

    @Select("CALL sp_get_anime_ranking(#{limit})")
    List<RankingVO> callAnimeRanking(@Param("limit") int limit);

    @Select("CALL sp_generate_studio_report()")
    List<StudioStatisticsVO> callStudioReport();

    @Select("SELECT COALESCE(MAX(id), 0) FROM operation_log")
    long selectMaxOperationLogId();

    @Select("""
            SELECT COUNT(*)
            FROM operation_log
            WHERE operator_user_id = #{operatorUserId}
              AND action_type = #{actionType}
              AND id > #{afterId}
            """)
    long countOperationLogsAfter(
            @Param("operatorUserId") Long operatorUserId,
            @Param("actionType") String actionType,
            @Param("afterId") long afterId
    );
}
