-- Executable CRUD demonstration for every core table.
-- Every row created here is removed and the transaction is rolled back.
-- This validates DELETE behavior and trigger maintenance without polluting demo data.

USE anime_platform;
START TRANSACTION;

-- sys_role / sys_user / sys_user_role
INSERT INTO sys_role (role_code, role_name, description)
VALUES ('CRUD_TEST', 'CRUD验证角色', 'Transaction-scoped CRUD test');
SET @role_id = LAST_INSERT_ID();

INSERT INTO sys_user (username, password_hash, nickname, email)
VALUES (
    'crud_check_user',
    '$2b$10$y1TuHGglRKug1C2QkMbnfuTEICEU2FaBPVbCmDpeALCzZNmtDSlxC',
    'CRUD验证用户',
    'crud_check_user@example.test'
);
SET @user_id = LAST_INSERT_ID();

INSERT INTO sys_user_role (user_id, role_id, assigned_by)
VALUES (@user_id, @role_id, NULL);
UPDATE sys_user SET nickname = 'CRUD验证用户已更新' WHERE id = @user_id;
SELECT id, username, nickname FROM sys_user WHERE id = @user_id;
UPDATE sys_user_role SET assigned_at = CURRENT_TIMESTAMP WHERE user_id = @user_id AND role_id = @role_id;

-- sys_user_profile (supplementary one-to-one table)
INSERT INTO sys_user_profile (user_id, bio, profile_visibility)
VALUES (@user_id, 'CRUD profile test', 1);
UPDATE sys_user_profile SET bio = 'CRUD profile test updated' WHERE user_id = @user_id;

-- anime_studio
INSERT INTO anime_studio
    (studio_name, studio_name_en, country, founded_year, website_url, description)
VALUES ('CRUD验证制作所', 'CRUD Test Studio', '日本', 2020, 'https://example.test/studio', 'CRUD test');
SET @studio_id = LAST_INSERT_ID();
UPDATE anime_studio SET description = 'CRUD test updated' WHERE id = @studio_id;

-- anime_genre
INSERT INTO anime_genre (genre_name, genre_slug, description)
VALUES ('CRUD验证类型', 'crud-test-genre', 'CRUD test');
SET @genre_id = LAST_INSERT_ID();
UPDATE anime_genre SET description = 'CRUD test updated' WHERE id = @genre_id;

-- streaming_platform
INSERT INTO streaming_platform (platform_name, platform_code, website_url, country)
VALUES ('CRUD验证平台', 'crud-test-platform', 'https://example.test/platform', '中国');
SET @platform_id = LAST_INSERT_ID();
UPDATE streaming_platform SET country = '日本' WHERE id = @platform_id;

-- anime
INSERT INTO anime (
    title, title_jp, title_en, studio_id, synopsis, release_date, status
)
VALUES (
    'CRUD验证动画', 'CRUD検証アニメ', 'CRUD Test Anime', @studio_id,
    'CRUD test', '2026-01-01', 'UPCOMING'
);
SET @anime_id = LAST_INSERT_ID();
UPDATE anime SET synopsis = 'CRUD test updated' WHERE id = @anime_id;

-- anime_genre_relation
INSERT INTO anime_genre_relation (anime_id, genre_id) VALUES (@anime_id, @genre_id);
UPDATE anime_genre_relation
SET created_at = CURRENT_TIMESTAMP
WHERE anime_id = @anime_id AND genre_id = @genre_id;

-- anime_episode
INSERT INTO anime_episode (anime_id, episode_number, title, air_date)
VALUES (@anime_id, 1, 'CRUD Episode', '2026-01-01');
SET @episode_id = LAST_INSERT_ID();
UPDATE anime_episode SET title = 'CRUD Episode Updated' WHERE id = @episode_id;

-- anime_staff / anime_staff_relation
INSERT INTO anime_staff (staff_name, staff_name_jp, country, bio)
VALUES ('CRUD Test Staff', 'CRUD検証人員', '日本', 'CRUD test');
SET @staff_id = LAST_INSERT_ID();
UPDATE anime_staff SET bio = 'CRUD test updated' WHERE id = @staff_id;
INSERT INTO anime_staff_relation (anime_id, staff_id, role_type)
VALUES (@anime_id, @staff_id, 'DIRECTOR');
UPDATE anime_staff_relation
SET created_at = CURRENT_TIMESTAMP
WHERE anime_id = @anime_id AND staff_id = @staff_id AND role_type = 'DIRECTOR';

-- anime_platform_relation
INSERT INTO anime_platform_relation (anime_id, platform_id, external_url)
VALUES (@anime_id, @platform_id, 'https://example.test/watch');
UPDATE anime_platform_relation
SET external_url = 'https://example.test/watch-updated'
WHERE anime_id = @anime_id AND platform_id = @platform_id;

-- user_favorite
INSERT INTO user_favorite (user_id, anime_id) VALUES (@user_id, @anime_id);
UPDATE user_favorite SET created_at = CURRENT_TIMESTAMP
WHERE user_id = @user_id AND anime_id = @anime_id;

-- user_watch_history
INSERT INTO user_watch_history
    (user_id, episode_id, progress_seconds, completed, last_watched_at)
VALUES (@user_id, @episode_id, 300, 0, CURRENT_TIMESTAMP);
SET @watch_id = LAST_INSERT_ID();
UPDATE user_watch_history
SET progress_seconds = 600, completed = 1, last_watched_at = CURRENT_TIMESTAMP
WHERE id = @watch_id;

-- anime_review
INSERT INTO anime_review (anime_id, user_id, rating, content)
VALUES (@anime_id, @user_id, 8, 'CRUD test review');
SET @review_id = LAST_INSERT_ID();
UPDATE anime_review SET rating = 9, content = 'CRUD test review updated' WHERE id = @review_id;

-- operation_log
INSERT INTO operation_log (operator_user_id, action_type, target_type, target_id, description)
VALUES (@user_id, 'CRUD_TEST', 'ANIME', @anime_id, 'CRUD test log');
SET @log_id = LAST_INSERT_ID();
UPDATE operation_log SET description = 'CRUD test log updated' WHERE id = @log_id;

SELECT
    @role_id AS role_id,
    @user_id AS user_id,
    @studio_id AS studio_id,
    @genre_id AS genre_id,
    @platform_id AS platform_id,
    @anime_id AS anime_id,
    @episode_id AS episode_id,
    @staff_id AS staff_id,
    @watch_id AS watch_id,
    @review_id AS review_id,
    @log_id AS log_id;

-- DELETE demonstrations in dependency order.
DELETE FROM user_watch_history WHERE id = @watch_id;
DELETE FROM user_favorite WHERE user_id = @user_id AND anime_id = @anime_id;
DELETE FROM anime_review WHERE id = @review_id;
DELETE FROM anime_platform_relation WHERE anime_id = @anime_id AND platform_id = @platform_id;
DELETE FROM anime_staff_relation
WHERE anime_id = @anime_id AND staff_id = @staff_id AND role_type = 'DIRECTOR';
DELETE FROM anime_episode WHERE id = @episode_id;
DELETE FROM anime_genre_relation WHERE anime_id = @anime_id AND genre_id = @genre_id;
DELETE FROM anime WHERE id = @anime_id;
DELETE FROM anime_staff WHERE id = @staff_id;
DELETE FROM anime_genre WHERE id = @genre_id;
DELETE FROM streaming_platform WHERE id = @platform_id;
DELETE FROM anime_studio WHERE id = @studio_id;
DELETE FROM sys_user_profile WHERE user_id = @user_id;
DELETE FROM sys_user_role WHERE user_id = @user_id AND role_id = @role_id;
DELETE FROM sys_user WHERE id = @user_id;
DELETE FROM sys_role WHERE id = @role_id;
DELETE FROM operation_log WHERE id = @log_id;

ROLLBACK;

SELECT 'BASIC_CRUD_ROLLBACK_OK' AS result,
       (SELECT COUNT(*) FROM sys_user WHERE username = 'crud_check_user') AS leaked_user_count;
