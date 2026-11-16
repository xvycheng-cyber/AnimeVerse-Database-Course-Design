USE anime_platform;

SELECT 'view_anime_detail_rows' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 30, 'PASS', 'FAIL') AS result
FROM v_anime_detail;

SELECT 'view_studio_statistics_rows' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 8, 'PASS', 'FAIL') AS result
FROM v_studio_statistics;

SELECT 'view_anime_popularity_rows' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 30, 'PASS', 'FAIL') AS result
FROM v_anime_popularity;

SELECT 'view_user_activity_rows' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) >= 10, 'PASS', 'FAIL') AS result
FROM v_user_activity;

SELECT 'function_weighted_score' AS check_name,
       fn_anime_weighted_score(8) AS actual_value,
       IF(fn_anime_weighted_score(8) >= 0, 'PASS', 'FAIL') AS result;

SELECT 'function_watch_minutes' AS check_name,
       fn_user_watch_minutes(1) AS actual_value,
       IF(fn_user_watch_minutes(1) > 0, 'PASS', 'FAIL') AS result;

SELECT 'function_visible_average' AS check_name,
       fn_visible_review_average(8) AS actual_value,
       IF(fn_visible_review_average(8) BETWEEN 0 AND 10, 'PASS', 'FAIL') AS result;

CALL sp_get_studio_statistics(2, @studio_name, @anime_count, @avg_rating, @favorite_total, @review_total);
SELECT 'procedure_studio_statistics' AS check_name,
       @studio_name AS studio_name,
       @anime_count AS anime_count,
       @avg_rating AS average_rating,
       IF(@anime_count >= 1, 'PASS', 'FAIL') AS result;

-- Result-set procedures are invoked directly. The PowerShell test runner
-- validates that CALL exits successfully and that the expected rows are present.
CALL sp_get_anime_ranking(5);
CALL sp_generate_studio_report();

-- Keep this assertion independent of API/lab demonstrations that intentionally
-- leave a committed transaction-audit row in the local course database.
DELETE FROM operation_log
WHERE action_type IN (
    'TRANSACTION_DEMO_COMMIT',
    'TRANSACTION_DEMO_ROLLBACK',
    'TRANSACTION_DEMO_EXCEPTION'
);

CALL sp_transaction_demo(1, 'commit', @tx_commit_result);
SELECT 'transaction_commit' AS check_name,
       @tx_commit_result AS actual_value,
       IF(@tx_commit_result = 'committed', 'PASS', 'FAIL') AS result;

SELECT 'transaction_commit_visible' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 1, 'PASS', 'FAIL') AS result
FROM operation_log
WHERE action_type = 'TRANSACTION_DEMO_COMMIT'
  AND description = 'transaction demo mode=commit';

CALL sp_transaction_demo(1, 'rollback', @tx_rollback_result);
SELECT 'transaction_rollback' AS check_name,
       @tx_rollback_result AS actual_value,
       IF(@tx_rollback_result = 'rolled_back', 'PASS', 'FAIL') AS result;

SELECT 'transaction_rollback_invisible' AS check_name,
       COUNT(*) AS actual_value,
       IF(COUNT(*) = 0, 'PASS', 'FAIL') AS result
FROM operation_log
WHERE action_type = 'TRANSACTION_DEMO_ROLLBACK'
  AND description = 'transaction demo mode=rollback';

DELETE FROM operation_log
WHERE action_type = 'TRANSACTION_DEMO_COMMIT'
  AND description = 'transaction demo mode=commit';
