-- AnimeVerse core schema (MySQL 8.0+, InnoDB, utf8mb4).
-- All statements are intentionally additive and repeatable.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE anime_platform;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    email VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_email UNIQUE (email),
    CONSTRAINT ck_sys_user_username CHECK (REGEXP_LIKE(username, '^[A-Za-z0-9_]{4,32}$')),
    CONSTRAINT ck_sys_user_password CHECK (password_hash LIKE '$2%'),
    CONSTRAINT ck_sys_user_email CHECK (email LIKE '%@%.%'),
    CONSTRAINT ck_sys_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Application users and BCrypt credentials';

-- Keep the database minimum aligned with the registration API and UI.
SET @username_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND CONSTRAINT_NAME = 'ck_sys_user_username'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @username_check_drop_sql = IF(
    @username_check_exists > 0,
    'ALTER TABLE sys_user DROP CHECK ck_sys_user_username',
    'SELECT 1'
);
PREPARE username_check_drop_stmt FROM @username_check_drop_sql;
EXECUTE username_check_drop_stmt;
DEALLOCATE PREPARE username_check_drop_stmt;
ALTER TABLE sys_user
    ADD CONSTRAINT ck_sys_user_username
    CHECK (REGEXP_LIKE(username, '^[A-Za-z0-9_]{4,32}$'));

CREATE TABLE IF NOT EXISTS sys_role (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_role PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (role_code),
    CONSTRAINT uk_sys_role_name UNIQUE (role_name),
    CONSTRAINT ck_sys_role_code CHECK (REGEXP_LIKE(role_code, '^[A-Z][A-Z0-9_]{1,31}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC roles';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id SMALLINT UNSIGNED NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT UNSIGNED NULL,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id)
        REFERENCES sys_role (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_assigner FOREIGN KEY (assigned_by)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    INDEX idx_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='User-to-role many-to-many assignment';

CREATE TABLE IF NOT EXISTS anime_studio (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    studio_name VARCHAR(120) NOT NULL,
    studio_name_en VARCHAR(160) NULL,
    country VARCHAR(64) NOT NULL DEFAULT '日本',
    founded_year SMALLINT UNSIGNED NULL,
    website_url VARCHAR(500) NULL,
    logo_url VARCHAR(500) NULL,
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_studio PRIMARY KEY (id),
    CONSTRAINT uk_anime_studio_name UNIQUE (studio_name),
    CONSTRAINT ck_anime_studio_founded_year CHECK (founded_year IS NULL OR founded_year BETWEEN 1900 AND 2100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Animation production studios';

CREATE TABLE IF NOT EXISTS anime_genre (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    genre_name VARCHAR(40) NOT NULL,
    genre_slug VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_genre PRIMARY KEY (id),
    CONSTRAINT uk_anime_genre_name UNIQUE (genre_name),
    CONSTRAINT uk_anime_genre_slug UNIQUE (genre_slug),
    CONSTRAINT ck_anime_genre_slug CHECK (REGEXP_LIKE(genre_slug, '^[a-z0-9-]{2,50}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime genre dictionary';

CREATE TABLE IF NOT EXISTS sys_user_profile (
    user_id BIGINT UNSIGNED NOT NULL,
    bio VARCHAR(500) NULL,
    birthday DATE NULL,
    preferred_genre_id SMALLINT UNSIGNED NULL,
    profile_visibility TINYINT UNSIGNED NOT NULL DEFAULT 1,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user_profile PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_profile_genre FOREIGN KEY (preferred_genre_id)
        REFERENCES anime_genre (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT ck_user_profile_visibility CHECK (profile_visibility IN (0, 1)),
    CONSTRAINT ck_user_profile_birthday CHECK (birthday IS NULL OR birthday >= '1900-01-01'),
    INDEX idx_user_profile_genre (preferred_genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Optional one-to-one profile extension for application users';

CREATE TABLE IF NOT EXISTS streaming_platform (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    platform_name VARCHAR(80) NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    website_url VARCHAR(500) NOT NULL,
    country VARCHAR(64) NULL,
    is_active TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_streaming_platform PRIMARY KEY (id),
    CONSTRAINT uk_streaming_platform_name UNIQUE (platform_name),
    CONSTRAINT uk_streaming_platform_code UNIQUE (platform_code),
    CONSTRAINT ck_streaming_platform_code CHECK (REGEXP_LIKE(platform_code, '^[a-z0-9-]{2,32}$')),
    CONSTRAINT ck_streaming_platform_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Legal streaming platform dictionary';

CREATE TABLE IF NOT EXISTS anime (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    title_jp VARCHAR(160) NULL,
    title_en VARCHAR(160) NULL,
    studio_id BIGINT UNSIGNED NOT NULL,
    synopsis TEXT NULL,
    cover_url VARCHAR(500) NULL,
    release_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    episode_count INT UNSIGNED NOT NULL DEFAULT 0,
    average_rating DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    rating_count INT UNSIGNED NOT NULL DEFAULT 0,
    favorite_count INT UNSIGNED NOT NULL DEFAULT 0,
    review_count INT UNSIGNED NOT NULL DEFAULT 0,
    view_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime PRIMARY KEY (id),
    CONSTRAINT fk_anime_studio FOREIGN KEY (studio_id)
        REFERENCES anime_studio (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_anime_status CHECK (status IN ('UPCOMING', 'AIRING', 'COMPLETED')),
    CONSTRAINT ck_anime_rating CHECK (average_rating BETWEEN 0.00 AND 10.00),
    CONSTRAINT ck_anime_dates CHECK (
        end_date IS NULL OR release_date IS NULL OR end_date >= release_date
    ),
    INDEX idx_anime_studio (studio_id),
    INDEX idx_anime_status_release (status, release_date),
    INDEX idx_anime_rating (average_rating),
    INDEX idx_anime_popularity (favorite_count, review_count, view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime title and maintained aggregate counters';

-- A regular UNIQUE(title, release_date) permits duplicate titles when the
-- release date is NULL. Normalize the missing date inside a functional index
-- so the business key remains unique without inventing a fake release day.
SET @anime_title_key_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'anime'
      AND INDEX_NAME = 'uk_anime_title_release_key'
);
SET @anime_title_key_index_sql = IF(
    @anime_title_key_index_exists = 0,
    'ALTER TABLE anime ADD UNIQUE INDEX uk_anime_title_release_key (title, (COALESCE(release_date, ''1900-01-01'')))',
    'SELECT 1'
);
PREPARE anime_title_key_index_stmt FROM @anime_title_key_index_sql;
EXECUTE anime_title_key_index_stmt;
DEALLOCATE PREPARE anime_title_key_index_stmt;

-- Earlier course builds also created a plain UNIQUE(title, release_date).
-- The functional unique index above covers all rows, including NULL dates,
-- so remove the legacy key when upgrading an existing database.
SET @anime_legacy_title_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'anime'
      AND INDEX_NAME = 'uk_anime_title_release'
);
SET @anime_legacy_title_key_sql = IF(
    @anime_legacy_title_key_exists > 0,
    'ALTER TABLE anime DROP INDEX uk_anime_title_release',
    'SELECT 1'
);
PREPARE anime_legacy_title_key_stmt FROM @anime_legacy_title_key_sql;
EXECUTE anime_legacy_title_key_stmt;
DEALLOCATE PREPARE anime_legacy_title_key_stmt;

CREATE TABLE IF NOT EXISTS anime_genre_relation (
    anime_id BIGINT UNSIGNED NOT NULL,
    genre_id SMALLINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_genre_relation PRIMARY KEY (anime_id, genre_id),
    CONSTRAINT fk_agr_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_agr_genre FOREIGN KEY (genre_id)
        REFERENCES anime_genre (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_agr_genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime-to-genre many-to-many relation';

CREATE TABLE IF NOT EXISTS anime_episode (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    anime_id BIGINT UNSIGNED NOT NULL,
    episode_number SMALLINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    synopsis TEXT NULL,
    duration_seconds INT UNSIGNED NULL,
    air_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_episode PRIMARY KEY (id),
    CONSTRAINT uk_anime_episode_number UNIQUE (anime_id, episode_number),
    CONSTRAINT fk_episode_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT ck_episode_number CHECK (episode_number BETWEEN 1 AND 9999),
    CONSTRAINT ck_episode_duration CHECK (duration_seconds IS NULL OR duration_seconds BETWEEN 60 AND 36000),
    INDEX idx_episode_anime_air_date (anime_id, air_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime episode inventory';

CREATE TABLE IF NOT EXISTS anime_staff (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    staff_name VARCHAR(120) NOT NULL,
    staff_name_jp VARCHAR(120) NULL,
    birth_date DATE NULL,
    country VARCHAR(64) NOT NULL DEFAULT '日本',
    avatar_url VARCHAR(500) NULL,
    bio TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_staff PRIMARY KEY (id),
    INDEX idx_staff_name (staff_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Directors, composers, creators and other production staff';

CREATE TABLE IF NOT EXISTS anime_staff_relation (
    anime_id BIGINT UNSIGNED NOT NULL,
    staff_id BIGINT UNSIGNED NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_staff_relation PRIMARY KEY (anime_id, staff_id, role_type),
    CONSTRAINT fk_asr_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_asr_staff FOREIGN KEY (staff_id)
        REFERENCES anime_staff (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_asr_role_type CHECK (
        role_type IN (
            'DIRECTOR', 'SERIES_COMPOSITION', 'MUSIC', 'ORIGINAL_CREATOR',
            'CHARACTER_DESIGN', 'VOICE_ACTOR', 'PRODUCER', 'ANIMATION_DIRECTOR'
        )
    ),
    INDEX idx_asr_staff_role (staff_id, role_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime-to-staff many-to-many relation with role';

CREATE TABLE IF NOT EXISTS anime_platform_relation (
    anime_id BIGINT UNSIGNED NOT NULL,
    platform_id SMALLINT UNSIGNED NOT NULL,
    external_url VARCHAR(500) NULL,
    available_from DATE NULL,
    available_until DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_platform_relation PRIMARY KEY (anime_id, platform_id),
    CONSTRAINT fk_apr_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_apr_platform FOREIGN KEY (platform_id)
        REFERENCES streaming_platform (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_apr_dates CHECK (
        available_until IS NULL OR available_from IS NULL OR available_until >= available_from
    ),
    INDEX idx_apr_platform (platform_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Anime-to-streaming-platform many-to-many availability';

CREATE TABLE IF NOT EXISTS user_favorite (
    user_id BIGINT UNSIGNED NOT NULL,
    anime_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_favorite PRIMARY KEY (user_id, anime_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_favorite_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX idx_favorite_anime_created (anime_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='User favorite relation; duplicate prevented by composite primary key';

CREATE TABLE IF NOT EXISTS user_watch_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    episode_id BIGINT UNSIGNED NOT NULL,
    progress_seconds INT UNSIGNED NOT NULL DEFAULT 0,
    completed TINYINT UNSIGNED NOT NULL DEFAULT 0,
    last_watched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_watch_history PRIMARY KEY (id),
    CONSTRAINT uk_watch_user_episode UNIQUE (user_id, episode_id),
    CONSTRAINT fk_watch_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_watch_episode FOREIGN KEY (episode_id)
        REFERENCES anime_episode (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT ck_watch_progress CHECK (progress_seconds BETWEEN 0 AND 36000),
    CONSTRAINT ck_watch_completed CHECK (completed IN (0, 1)),
    INDEX idx_watch_user_time (user_id, last_watched_at),
    INDEX idx_watch_episode (episode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Latest viewing progress per user and episode';

CREATE TABLE IF NOT EXISTS anime_review (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    anime_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_anime_review PRIMARY KEY (id),
    CONSTRAINT uk_review_user_anime UNIQUE (user_id, anime_id),
    CONSTRAINT fk_review_anime FOREIGN KEY (anime_id)
        REFERENCES anime (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 10),
    CONSTRAINT ck_review_content CHECK (CHAR_LENGTH(content) BETWEEN 1 AND 2000),
    CONSTRAINT ck_review_status CHECK (status IN (0, 1)),
    INDEX idx_review_anime_created (anime_id, created_at),
    INDEX idx_review_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='One review per user per anime; status is moderation visibility';

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_user_id BIGINT UNSIGNED NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id BIGINT UNSIGNED NULL,
    description VARCHAR(1000) NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_operation_log PRIMARY KEY (id),
    CONSTRAINT fk_operation_log_user FOREIGN KEY (operator_user_id)
        REFERENCES sys_user (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    INDEX idx_operation_log_user_time (operator_user_id, created_at),
    INDEX idx_operation_log_action_time (action_type, created_at),
    INDEX idx_operation_log_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Administrative and security audit trail; retained after user deletion';
