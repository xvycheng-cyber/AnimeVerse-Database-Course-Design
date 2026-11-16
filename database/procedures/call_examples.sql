USE anime_platform;

-- sp_get_studio_statistics: OUT parameters.
CALL sp_get_studio_statistics(2, @studio_name, @anime_count, @avg_rating, @favorite_total, @review_total);
SELECT @studio_name AS studio_name,
       @anime_count AS anime_count,
       @avg_rating AS average_rating,
       @favorite_total AS favorite_total,
       @review_total AS review_total;

-- sp_get_anime_ranking: result set.
CALL sp_get_anime_ranking(10);

-- Cursor procedure: iterates every studio and returns a report result set.
CALL sp_generate_studio_report();

-- sp_add_anime: commit on success; an invalid studio should trigger SIGNAL/rollback.
CALL sp_add_anime(
    'Procedure Demo Anime',
    NULL,
    'Procedure Demo Anime',
    2,
    'Called by procedure example.',
    NULL,
    '2026-11-16',
    'UPCOMING',
    @new_anime_id
);
SELECT @new_anime_id AS new_anime_id;

-- sp_delete_anime: removes the demonstration row and cascades its dependent relations.
CALL sp_delete_anime(@new_anime_id, @deleted_rows);
SELECT @deleted_rows AS deleted_rows;

-- Transaction demonstration. "commit" writes one audit row.
CALL sp_transaction_demo(1, 'commit', @tx_result);
SELECT @tx_result AS transaction_result;

-- "rollback" inserts and then rolls back; the result is no audit row.
CALL sp_transaction_demo(1, 'rollback', @tx_result);
SELECT @tx_result AS transaction_result;

-- "exception" intentionally raises SQLSTATE 45000 after the handler rolls back.
-- Run the following separately to observe the expected error:
-- CALL sp_transaction_demo(1, 'exception', @tx_result);

