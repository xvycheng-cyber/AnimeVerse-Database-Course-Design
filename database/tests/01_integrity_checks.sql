USE anime_platform;

-- Each row returns PASS/FAIL and a machine-readable actual value.
SELECT 'all_table_count_17' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 17, 'PASS', 'FAIL') AS result
FROM information_schema.tables
WHERE table_schema = 'anime_platform'
  AND table_type = 'BASE TABLE';

SELECT 'core_table_count_16' AS check_name,
       COUNT(DISTINCT table_name) AS actual_value,
       IF(COUNT(DISTINCT table_name) = 16, 'PASS', 'FAIL') AS result
FROM information_schema.tables
WHERE table_schema = 'anime_platform'
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'sys_user', 'sys_role', 'sys_user_role', 'anime_studio', 'anime',
      'anime_genre', 'anime_genre_relation', 'anime_episode', 'anime_staff',
      'anime_staff_relation', 'streaming_platform', 'anime_platform_relation',
      'user_favorite', 'user_watch_history', 'anime_review', 'operation_log'
  );

SELECT 'one_to_one_user_profile' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 1, 'PASS', 'FAIL') AS result
FROM information_schema.statistics
WHERE table_schema = 'anime_platform'
  AND table_name = 'sys_user_profile'
  AND index_name = 'PRIMARY';

SELECT 'nullable_title_unique_index' AS check_name,
       COUNT(DISTINCT index_name) AS actual_value,
       IF(COUNT(DISTINCT index_name) = 1, 'PASS', 'FAIL') AS result
FROM information_schema.statistics
WHERE table_schema = 'anime_platform'
  AND table_name = 'anime'
  AND index_name = 'uk_anime_title_release_key';

SELECT 'legacy_nullable_title_key_removed' AS check_name,
       COUNT(DISTINCT index_name) AS actual_value,
       IF(COUNT(DISTINCT index_name) = 0, 'PASS', 'FAIL') AS result
FROM information_schema.statistics
WHERE table_schema = 'anime_platform'
  AND table_name = 'anime'
  AND index_name = 'uk_anime_title_release';

SELECT 'all_tables_innodb_utf8mb4' AS check_name,
       COUNT(*) AS noncompliant_tables,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM information_schema.tables
WHERE table_schema = 'anime_platform'
  AND table_type = 'BASE TABLE'
  AND (engine <> 'InnoDB' OR table_collation NOT LIKE 'utf8mb4%');

SELECT 'foreign_key_count' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 16, 'PASS', 'FAIL') AS result
FROM information_schema.referential_constraints
WHERE constraint_schema = 'anime_platform';

SELECT 'check_constraint_count' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 15, 'PASS', 'FAIL') AS result
FROM information_schema.table_constraints
WHERE constraint_schema = 'anime_platform'
  AND constraint_type = 'CHECK';

SELECT 'studio_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 8, 'PASS', 'FAIL') AS result FROM anime_studio;
SELECT 'anime_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 30, 'PASS', 'FAIL') AS result FROM anime;
SELECT 'genre_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 10, 'PASS', 'FAIL') AS result FROM anime_genre;
SELECT 'episode_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 60, 'PASS', 'FAIL') AS result FROM anime_episode;
SELECT 'staff_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 20, 'PASS', 'FAIL') AS result FROM anime_staff;
SELECT 'platform_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 6, 'PASS', 'FAIL') AS result FROM streaming_platform;
SELECT 'user_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 10, 'PASS', 'FAIL') AS result FROM sys_user;
SELECT 'favorite_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 30, 'PASS', 'FAIL') AS result FROM user_favorite;
SELECT 'watch_history_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 30, 'PASS', 'FAIL') AS result FROM user_watch_history;
SELECT 'review_count' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) >= 20, 'PASS', 'FAIL') AS result FROM anime_review;

SELECT 'duplicate_favorites' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM (
    SELECT user_id, anime_id
    FROM user_favorite
    GROUP BY user_id, anime_id
    HAVING COUNT(*) > 1
) d;

SELECT 'duplicate_reviews' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM (
    SELECT user_id, anime_id
    FROM anime_review
    GROUP BY user_id, anime_id
    HAVING COUNT(*) > 1
) d;

SELECT 'duplicate_null_release_titles' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM (
    SELECT title, COALESCE(release_date, '1900-01-01')
    FROM anime
    GROUP BY title, COALESCE(release_date, '1900-01-01')
    HAVING COUNT(*) > 1
) d;

SELECT 'favorite_counter_mismatches' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*) AS source_count
    FROM user_favorite
    GROUP BY anime_id
) f ON f.anime_id = a.id
WHERE a.favorite_count <> COALESCE(f.source_count, 0);

SELECT 'review_counter_mismatches' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*) AS source_count, ROUND(AVG(rating), 2) AS source_average
    FROM anime_review
    WHERE status = 1
    GROUP BY anime_id
) r ON r.anime_id = a.id
WHERE a.review_count <> COALESCE(r.source_count, 0)
   OR a.rating_count <> COALESCE(r.source_count, 0)
   OR a.average_rating <> COALESCE(r.source_average, 0.00);

SELECT 'watch_counter_mismatches' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime a
LEFT JOIN (
    SELECT e.anime_id, COUNT(*) AS source_count
    FROM user_watch_history w
    JOIN anime_episode e ON e.id = w.episode_id
    GROUP BY e.anime_id
) v ON v.anime_id = a.id
WHERE a.view_count <> COALESCE(v.source_count, 0);

SELECT 'episode_counter_mismatches' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*) AS source_count
    FROM anime_episode
    GROUP BY anime_id
) e ON e.anime_id = a.id
WHERE a.episode_count <> COALESCE(e.source_count, 0);

SELECT 'negative_counters' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime
WHERE favorite_count < 0 OR review_count < 0 OR rating_count < 0
   OR view_count < 0 OR episode_count < 0;

SELECT 'orphan_episode' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime_episode e
LEFT JOIN anime a ON a.id = e.anime_id
WHERE a.id IS NULL;

SELECT 'orphan_favorite' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM user_favorite f
LEFT JOIN sys_user u ON u.id = f.user_id
LEFT JOIN anime a ON a.id = f.anime_id
WHERE u.id IS NULL OR a.id IS NULL;

SELECT 'orphan_review' AS check_name, COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM anime_review r
LEFT JOIN sys_user u ON u.id = r.user_id
LEFT JOIN anime a ON a.id = r.anime_id
WHERE u.id IS NULL OR a.id IS NULL;

SELECT 'view_count_4' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 4, 'PASS', 'FAIL') AS result
FROM information_schema.views
WHERE table_schema = 'anime_platform'
  AND table_name IN (
      'v_anime_detail', 'v_studio_statistics', 'v_anime_popularity', 'v_user_activity'
  );

SELECT 'procedure_count_6' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 6, 'PASS', 'FAIL') AS result
FROM information_schema.routines
WHERE routine_schema = 'anime_platform'
  AND routine_type = 'PROCEDURE';

SELECT 'function_count_3' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 3, 'PASS', 'FAIL') AS result
FROM information_schema.routines
WHERE routine_schema = 'anime_platform'
  AND routine_type = 'FUNCTION';
