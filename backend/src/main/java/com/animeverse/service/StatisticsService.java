package com.animeverse.service;

import com.animeverse.mapper.StatisticsMapper;
import com.animeverse.vo.StatisticsOverviewVO;
import com.animeverse.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsOverviewVO overview() {
        return new StatisticsOverviewVO(
                statisticsMapper.countUsers(),
                statisticsMapper.countAnime(),
                statisticsMapper.countEpisodes(),
                statisticsMapper.countStudios(),
                statisticsMapper.countReviews(),
                statisticsMapper.countFavorites(),
                statisticsMapper.countWatchRecords()
        );
    }

    public List<TrendPointVO> trend(int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        Map<String, Long> users = toMap(statisticsMapper.countUserTrend(safeDays));
        Map<String, Long> reviews = toMap(statisticsMapper.countReviewTrend(safeDays));
        Map<String, Long> favorites = toMap(statisticsMapper.countFavoriteTrend(safeDays));

        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        List<TrendPointVO> result = new ArrayList<>(safeDays);
        for (int offset = 0; offset < safeDays; offset++) {
            String date = start.plusDays(offset).toString();
            TrendPointVO point = new TrendPointVO();
            point.setDate(date);
            point.setUserCount(users.getOrDefault(date, 0L));
            point.setReviewCount(reviews.getOrDefault(date, 0L));
            point.setFavoriteCount(favorites.getOrDefault(date, 0L));
            point.setCount(point.getReviewCount() + point.getFavoriteCount());
            result.add(point);
        }
        return result;
    }

    private Map<String, Long> toMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dateValue = row.getOrDefault("date", row.get("DATE"));
            Object totalValue = row.getOrDefault("total", row.get("TOTAL"));
            if (dateValue != null && totalValue instanceof Number number) {
                result.put(dateValue.toString(), number.longValue());
            }
        }
        return result;
    }
}
