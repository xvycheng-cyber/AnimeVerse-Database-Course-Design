package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.dto.AddAnimeProcedureRequest;
import com.animeverse.dto.FavoriteTriggerRequest;
import com.animeverse.dto.TransactionDemoRequest;
import com.animeverse.mapper.AnimeMapper;
import com.animeverse.mapper.LabMapper;
import com.animeverse.mapper.UserFavoriteMapper;
import com.animeverse.security.AuthenticatedUser;
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.RankingVO;
import com.animeverse.vo.StudioStatisticsVO;
import com.animeverse.vo.TransactionDemoVO;
import com.animeverse.vo.TriggerFavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabService {

    private static final Set<String> ALLOWED_VIEWS = Set.of(
            "v_anime_detail",
            "v_studio_statistics",
            "v_anime_popularity",
            "v_user_activity"
    );

    private final LabMapper labMapper;
    private final AnimeMapper animeMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final FavoriteService favoriteService;
    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> queryView(String viewName) {
        if (!ALLOWED_VIEWS.contains(viewName)) {
            throw BusinessException.badRequest("不允许访问该视图");
        }
        return labMapper.selectView(viewName);
    }

    public TriggerFavoriteVO triggerFavorite(FavoriteTriggerRequest request) {
        Long userId = SecurityUtils.currentUserId();
        long before = favoriteCount(request.animeId());
        boolean exists = favoriteMapper.countByUserAndAnime(userId, request.animeId()) > 0;
        boolean changed = request.enable() != exists;
        if (request.enable()) {
            favoriteService.add(request.animeId());
        } else {
            favoriteService.remove(request.animeId());
        }
        long after = favoriteCount(request.animeId());
        return new TriggerFavoriteVO(
                request.animeId(),
                request.enable(),
                changed,
                before,
                after
        );
    }

    public Map<String, Object> addAnime(AddAnimeProcedureRequest request) {
        LocalDate releaseDate = request.releaseDate() != null
                ? request.releaseDate()
                : request.releaseYear() == null ? null : LocalDate.of(request.releaseYear(), 1, 1);
        String titleJp = firstNonBlank(request.titleJp(), request.originalTitle());
        String synopsis = firstNonBlank(request.synopsis(), request.description());
        String status = AnimeService.normalizeStatus(
                request.status() == null ? "UPCOMING" : request.status()
        );

        return jdbcTemplate.execute((ConnectionCallback<Map<String, Object>>) connection -> {
            try (CallableStatement statement = connection.prepareCall(
                    "{call sp_add_anime(?, ?, ?, ?, ?, ?, ?, ?, ?)}"
            )) {
                statement.setString(1, request.title().trim());
                statement.setString(2, titleJp);
                statement.setString(3, trimToNull(request.titleEn()));
                statement.setLong(4, request.studioId());
                statement.setString(5, synopsis);
                statement.setString(6, trimToNull(request.coverUrl()));
                if (releaseDate == null) {
                    statement.setNull(7, Types.DATE);
                } else {
                    statement.setDate(7, Date.valueOf(releaseDate));
                }
                statement.setString(8, status);
                statement.registerOutParameter(9, Types.BIGINT);
                statement.execute();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("animeId", statement.getLong(9));
                result.put("title", request.title().trim());
                result.put("titleJp", titleJp);
                result.put("titleEn", trimToNull(request.titleEn()));
                result.put("releaseDate", releaseDate);
                result.put("status", status);
                result.put("source", "sp_add_anime");
                return result;
            }
        });
    }

    public Map<String, Object> deleteAnime(Long animeId) {
        return jdbcTemplate.execute((ConnectionCallback<Map<String, Object>>) connection -> {
            try (CallableStatement statement = connection.prepareCall(
                    "{call sp_delete_anime(?, ?)}"
            )) {
                statement.setLong(1, animeId);
                statement.registerOutParameter(2, Types.INTEGER);
                statement.execute();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("animeId", animeId);
                result.put("deletedRows", statement.getInt(2));
                result.put("source", "sp_delete_anime");
                return result;
            }
        });
    }

    public StudioStatisticsVO studioStatistics(Long studioId) {
        return jdbcTemplate.execute((ConnectionCallback<StudioStatisticsVO>) connection -> {
            try (CallableStatement statement = connection.prepareCall(
                    "{call sp_get_studio_statistics(?, ?, ?, ?, ?, ?)}"
            )) {
                statement.setLong(1, studioId);
                statement.registerOutParameter(2, Types.VARCHAR);
                statement.registerOutParameter(3, Types.INTEGER);
                statement.registerOutParameter(4, Types.DECIMAL);
                statement.registerOutParameter(5, Types.BIGINT);
                statement.registerOutParameter(6, Types.BIGINT);
                statement.execute();

                StudioStatisticsVO result = new StudioStatisticsVO();
                result.setStudioId(studioId);
                result.setStudioName(statement.getString(2));
                result.setAnimeCount(nullableLong(statement.getObject(3)));
                result.setAverageRating(nullableDecimal(statement.getObject(4)));
                result.setFavoriteTotal(nullableLong(statement.getObject(5)));
                result.setReviewTotal(nullableLong(statement.getObject(6)));
                return result;
            }
        });
    }

    public List<RankingVO> animeRanking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return labMapper.callAnimeRanking(safeLimit);
    }

    public List<StudioStatisticsVO> studioReport() {
        return labMapper.callStudioReport();
    }

    public TransactionDemoVO transactionDemo(TransactionDemoRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        long beforeId = labMapper.selectMaxOperationLogId();
        String marker = "TX-" + beforeId + "-" + UUID.randomUUID();
        String mode = request.mode().toLowerCase();
        String actionType = "TRANSACTION_DEMO_" + mode.toUpperCase();
        String processResult = null;
        String error = null;

        try {
            processResult = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                try (CallableStatement statement = connection.prepareCall(
                        "{call sp_transaction_demo(?, ?, ?)}"
                )) {
                    statement.setLong(1, user.id());
                    statement.setString(2, mode);
                    statement.registerOutParameter(3, Types.VARCHAR);
                    statement.execute();
                    return statement.getString(3);
                }
            });
        } catch (RuntimeException exception) {
            error = rootMessage(exception);
        }

        long visibleRows = labMapper.countOperationLogsAfter(user.id(), actionType, beforeId);
        boolean committed = "commit".equals(mode)
                && "committed".equals(processResult)
                && visibleRows == 1;
        boolean rolledBack = "rollback".equals(mode)
                ? "rolled_back".equals(processResult) && visibleRows == 0
                : "exception".equals(mode)
                        && error != null
                        && error.contains("intentional transaction exception")
                        && visibleRows == 0;

        return new TransactionDemoVO(
                mode,
                marker,
                committed,
                rolledBack,
                visibleRows,
                error
        );
    }

    private long favoriteCount(Long animeId) {
        Long count = animeMapper.selectFavoriteCount(animeId);
        if (count == null) {
            throw BusinessException.notFound("动漫不存在");
        }
        return count;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static BigDecimal nullableDecimal(Object value) {
        return value instanceof BigDecimal decimal
                ? decimal
                : value instanceof Number number ? new BigDecimal(number.toString()) : null;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
