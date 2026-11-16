USE anime_platform;

-- All four course views are read-oriented projections. A no-op UPDATE must
-- still be rejected by MySQL instead of silently changing base-table data.
START TRANSACTION;
UPDATE v_anime_popularity
SET title = title
WHERE id = 1;
ROLLBACK;
