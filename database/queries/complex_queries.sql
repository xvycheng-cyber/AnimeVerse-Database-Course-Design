USE anime_platform;

-- Q01 Multi-table detail view with studio and aggregated genres.
SELECT id, title, studio_name, genres, average_rating, favorite_count
FROM v_anime_detail
ORDER BY average_rating DESC, favorite_count DESC
LIMIT 10;

-- Q02 Studio ranking with GROUP BY/HAVING/aggregation.
SELECT
    s.studio_name,
    COUNT(a.id) AS anime_count,
    ROUND(AVG(a.average_rating), 2) AS avg_rating,
    SUM(a.favorite_count) AS favorite_total
FROM anime_studio s
JOIN anime a ON a.studio_id = s.id
GROUP BY s.id, s.studio_name
HAVING COUNT(a.id) >= 3 AND SUM(a.favorite_count) > 0
ORDER BY favorite_total DESC, avg_rating DESC;

-- Q03 Genre popularity using a multi-table relation.
SELECT
    g.genre_name,
    COUNT(DISTINCT agr.anime_id) AS anime_count,
    COALESCE(SUM(a.favorite_count), 0) AS favorite_total,
    ROUND(AVG(a.average_rating), 2) AS avg_rating
FROM anime_genre g
JOIN anime_genre_relation agr ON agr.genre_id = g.id
JOIN anime a ON a.id = agr.anime_id
GROUP BY g.id, g.genre_name
ORDER BY favorite_total DESC, anime_count DESC;

-- Q04 Users with activity and EXISTS filtering.
SELECT
    u.username,
    u.nickname,
    COUNT(DISTINCT f.anime_id) AS favorites,
    COUNT(DISTINCT r.id) AS reviews
FROM sys_user u
LEFT JOIN user_favorite f ON f.user_id = u.id
LEFT JOIN anime_review r ON r.user_id = u.id
WHERE u.status = 1
  AND EXISTS (
      SELECT 1
      FROM user_watch_history w
      WHERE w.user_id = u.id
  )
GROUP BY u.id, u.username, u.nickname
ORDER BY reviews DESC, favorites DESC;

-- Q05 Staff-role matrix for selected anime.
SELECT
    a.title,
    st.staff_name,
    asr.role_type
FROM anime_staff_relation asr
JOIN anime a ON a.id = asr.anime_id
JOIN anime_staff st ON st.id = asr.staff_id
WHERE a.id IN (8, 9, 13, 17, 25)
ORDER BY a.title, asr.role_type, st.staff_name;

-- Q06 Platform availability summary.
SELECT
    p.platform_name,
    COUNT(apr.anime_id) AS anime_count,
    GROUP_CONCAT(a.title ORDER BY a.average_rating DESC SEPARATOR ' | ') AS sample_titles
FROM streaming_platform p
LEFT JOIN anime_platform_relation apr ON apr.platform_id = p.id
LEFT JOIN anime a ON a.id = apr.anime_id
GROUP BY p.id, p.platform_name
ORDER BY anime_count DESC, p.platform_name;

-- Q07 Window-function ranking.
SELECT id, title, studio_name, average_rating, favorite_count, popularity_rank
FROM v_anime_popularity
ORDER BY popularity_rank, id
LIMIT 15;

-- Q08 Date filtering across episodes with fuzzy title matching.
SELECT
    a.title,
    e.episode_number,
    e.air_date
FROM anime_episode e
JOIN anime a ON a.id = e.anime_id
WHERE e.air_date >= '2020-01-01'
  AND a.title LIKE '%的%'
ORDER BY e.air_date DESC, a.title, e.episode_number
LIMIT 20;

-- Q09 Multi-condition catalogue filter.
SELECT id, title, studio_name, status, release_date, average_rating
FROM v_anime_detail
WHERE status = 'COMPLETED'
  AND release_date BETWEEN '2010-01-01' AND '2019-12-31'
  AND average_rating >= 8.00
ORDER BY average_rating DESC, release_date DESC;

-- Q10 Anime with no visible review using NOT EXISTS.
SELECT a.id, a.title
FROM anime a
WHERE NOT EXISTS (
    SELECT 1
    FROM anime_review r
    WHERE r.anime_id = a.id
      AND r.status = 1
)
ORDER BY a.id;

-- Q11 Correlated subquery: titles above their studio average.
SELECT
    a.title,
    a.average_rating,
    s.studio_name,
    (
        SELECT ROUND(AVG(a2.average_rating), 2)
        FROM anime a2
        WHERE a2.studio_id = a.studio_id
          AND a2.rating_count > 0
    ) AS studio_average
FROM anime a
JOIN anime_studio s ON s.id = a.studio_id
WHERE a.rating_count > 0
  AND a.average_rating > (
      SELECT AVG(a2.average_rating)
      FROM anime a2
      WHERE a2.studio_id = a.studio_id
        AND a2.rating_count > 0
  )
ORDER BY a.average_rating DESC;

-- Q12 CTE plus window function for genre popularity.
WITH genre_stats AS (
    SELECT
        g.genre_name,
        COUNT(*) AS anime_count,
        SUM(a.favorite_count) AS favorite_total
    FROM anime_genre g
    JOIN anime_genre_relation agr ON agr.genre_id = g.id
    JOIN anime a ON a.id = agr.anime_id
    GROUP BY g.id, g.genre_name
)
SELECT
    genre_name,
    anime_count,
    favorite_total,
    RANK() OVER (ORDER BY favorite_total DESC, anime_count DESC) AS popularity_rank
FROM genre_stats
ORDER BY popularity_rank;

-- Q13 Monthly review trend.
SELECT
    DATE_FORMAT(created_at, '%Y-%m') AS review_month,
    COUNT(*) AS review_count,
    ROUND(AVG(rating), 2) AS avg_rating
FROM anime_review
WHERE status = 1
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY review_month;

-- Q14 Watch completion rate with conditional aggregation.
SELECT
    a.id,
    a.title,
    COUNT(w.id) AS watched_entries,
    SUM(CASE WHEN w.completed = 1 THEN 1 ELSE 0 END) AS completed_entries,
    ROUND(
        SUM(CASE WHEN w.completed = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(w.id), 0) * 100,
        2
    ) AS completion_rate_percent
FROM anime a
JOIN anime_episode e ON e.anime_id = a.id
JOIN user_watch_history w ON w.episode_id = e.id
GROUP BY a.id, a.title
ORDER BY completion_rate_percent DESC, watched_entries DESC;

-- Q15 Staff collaboration pairs based on shared anime.
SELECT
    s1.staff_name AS staff_a,
    s2.staff_name AS staff_b,
    COUNT(DISTINCT r1.anime_id) AS shared_anime_count,
    GROUP_CONCAT(DISTINCT a.title ORDER BY a.title SEPARATOR ' | ') AS shared_titles
FROM anime_staff_relation r1
JOIN anime_staff_relation r2
  ON r2.anime_id = r1.anime_id
 AND r2.staff_id > r1.staff_id
JOIN anime_staff s1 ON s1.id = r1.staff_id
JOIN anime_staff s2 ON s2.id = r2.staff_id
JOIN anime a ON a.id = r1.anime_id
GROUP BY s1.id, s1.staff_name, s2.id, s2.staff_name
HAVING COUNT(DISTINCT r1.anime_id) >= 1
ORDER BY shared_anime_count DESC, staff_a, staff_b;

-- Q16 Active users without duplicate roles; anti-join consistency check.
SELECT
    u.id,
    u.username,
    GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS roles
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role r ON r.id = ur.role_id
WHERE u.status = 1
GROUP BY u.id, u.username
ORDER BY u.id;

-- Q17 HAVING with correlated count to find multi-role users.
SELECT
    u.username,
    COUNT(ur.role_id) AS role_count
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
GROUP BY u.id, u.username
HAVING COUNT(ur.role_id) >= 2;

-- Q18 Latest watch progress per user.
SELECT
    u.username,
    a.title,
    e.episode_number,
    w.progress_seconds,
    w.completed,
    w.last_watched_at
FROM user_watch_history w
JOIN sys_user u ON u.id = w.user_id
JOIN anime_episode e ON e.id = w.episode_id
JOIN anime a ON a.id = e.anime_id
WHERE w.last_watched_at = (
    SELECT MAX(w2.last_watched_at)
    FROM user_watch_history w2
    WHERE w2.user_id = w.user_id
)
ORDER BY w.last_watched_at DESC;

