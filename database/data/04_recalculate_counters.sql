-- Reconcile denormalized counters with the source relations.
-- This is also a data-integrity test fixture and is safe to run repeatedly.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE anime_platform;

UPDATE anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*) AS episode_count
    FROM anime_episode
    GROUP BY anime_id
) e ON e.anime_id = a.id
SET a.episode_count = COALESCE(e.episode_count, 0);

UPDATE anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*) AS favorite_count
    FROM user_favorite
    GROUP BY anime_id
) f ON f.anime_id = a.id
SET a.favorite_count = COALESCE(f.favorite_count, 0);

UPDATE anime a
LEFT JOIN (
    SELECT anime_id,
           COUNT(*) AS review_count,
           COUNT(*) AS rating_count,
           ROUND(AVG(rating), 2) AS average_rating
    FROM anime_review
    WHERE status = 1
    GROUP BY anime_id
) r ON r.anime_id = a.id
SET a.review_count = COALESCE(r.review_count, 0),
    a.rating_count = COALESCE(r.rating_count, 0),
    a.average_rating = COALESCE(r.average_rating, 0.00);

UPDATE anime a
LEFT JOIN (
    SELECT e.anime_id, COUNT(*) AS view_count
    FROM user_watch_history w
    JOIN anime_episode e ON e.id = w.episode_id
    GROUP BY e.anime_id
) v ON v.anime_id = a.id
SET a.view_count = COALESCE(v.view_count, 0);
