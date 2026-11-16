USE anime_platform;
CREATE OR REPLACE VIEW v_user_activity AS
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    u.email,
    u.status,
    COALESCE(r.roles, '') AS roles,
    COALESCE(f.favorite_count, 0) AS favorite_count,
    COALESCE(v.review_count, 0) AS review_count,
    COALESCE(w.watch_count, 0) AS watch_count,
    COALESCE(w.completed_count, 0) AS completed_count,
    CASE
        WHEN GREATEST(
            COALESCE(f.last_favorite_at, '1970-01-01 00:00:00'),
            COALESCE(v.last_review_at, '1970-01-01 00:00:00'),
            COALESCE(w.last_watched_at, '1970-01-01 00:00:00')
        ) = '1970-01-01 00:00:00' THEN NULL
        ELSE GREATEST(
            COALESCE(f.last_favorite_at, '1970-01-01 00:00:00'),
            COALESCE(v.last_review_at, '1970-01-01 00:00:00'),
            COALESCE(w.last_watched_at, '1970-01-01 00:00:00')
        )
    END AS last_activity_at
FROM sys_user u
LEFT JOIN (
    SELECT ur.user_id, GROUP_CONCAT(r.role_code ORDER BY r.id SEPARATOR ',') AS roles
    FROM sys_user_role ur
    JOIN sys_role r ON r.id = ur.role_id
    GROUP BY ur.user_id
) r ON r.user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) AS favorite_count, MAX(created_at) AS last_favorite_at
    FROM user_favorite
    GROUP BY user_id
) f ON f.user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) AS review_count, MAX(created_at) AS last_review_at
    FROM anime_review
    GROUP BY user_id
) v ON v.user_id = u.id
LEFT JOIN (
    SELECT
        user_id,
        COUNT(*) AS watch_count,
        SUM(completed) AS completed_count,
        MAX(last_watched_at) AS last_watched_at
    FROM user_watch_history
    GROUP BY user_id
) w ON w.user_id = u.id;

