# AnimeVerse 数据字典

数据库：`anime_platform`  
DBMS：MySQL 8.0.43（目标 MySQL 8.0+）  
存储引擎：全部 InnoDB  
字符集/排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`

## 1. sys_user

用户登录主体和 RBAC 关联主体。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 用户编号 |
| username | VARCHAR(32) | N | - | UK，格式 CHECK | 登录名 |
| password_hash | VARCHAR(100) | N | - | BCrypt CHECK | BCrypt 密码哈希 |
| nickname | VARCHAR(80) | N | - | - | 展示昵称 |
| email | VARCHAR(255) | N | - | UK，格式 CHECK | 邮箱 |
| avatar_url | VARCHAR(500) | Y | NULL | - | 头像地址 |
| status | TINYINT UNSIGNED | N | 1 | CHECK IN (0,1) | 启用/停用 |
| last_login_at | DATETIME | Y | NULL | - | 最近登录时间 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：`pk_sys_user(id)`、`uk_sys_user_username`、`uk_sys_user_email`。

## 2. sys_role

RBAC 角色字典。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | SMALLINT UNSIGNED | N | AUTO_INCREMENT | PK | 角色编号 |
| role_code | VARCHAR(32) | N | - | UK，格式 CHECK | 代码，如 ADMIN/USER |
| role_name | VARCHAR(64) | N | - | UK | 角色名称 |
| description | VARCHAR(255) | Y | NULL | - | 说明 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |

索引：主键、角色代码唯一、角色名称唯一。

## 3. sys_user_role

用户和角色 M:N 关系。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| user_id | BIGINT UNSIGNED | N | - | PK, FK→sys_user CASCADE | 用户 |
| role_id | SMALLINT UNSIGNED | N | - | PK, FK→sys_role RESTRICT | 角色 |
| assigned_at | DATETIME | N | CURRENT_TIMESTAMP | - | 授权时间 |
| assigned_by | BIGINT UNSIGNED | Y | NULL | FK→sys_user SET NULL | 操作者 |

索引：复合主键 `(user_id, role_id)`、`idx_user_role_role(role_id)`。

## 4. sys_user_profile

用户严格一对一扩展资料。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| user_id | BIGINT UNSIGNED | N | - | PK/FK→sys_user CASCADE | 用户，一对一共享主键 |
| bio | VARCHAR(500) | Y | NULL | - | 个人简介 |
| birthday | DATE | Y | NULL | CHECK 日期下限 | 生日 |
| preferred_genre_id | SMALLINT UNSIGNED | Y | NULL | FK→anime_genre SET NULL | 偏好类型 |
| profile_visibility | TINYINT UNSIGNED | N | 1 | CHECK IN (0,1) | 资料可见性 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：`pk_sys_user_profile(user_id)`、`idx_user_profile_genre`。

## 5. anime_studio

动画制作厂商主数据。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 厂商编号 |
| studio_name | VARCHAR(120) | N | - | UK | 厂商名称 |
| studio_name_en | VARCHAR(160) | Y | NULL | - | 英文名称 |
| country | VARCHAR(64) | N | 日本 | - | 国家/地区 |
| founded_year | SMALLINT UNSIGNED | Y | NULL | CHECK 1900..2100 | 成立年份 |
| website_url | VARCHAR(500) | Y | NULL | - | 官方网站 |
| logo_url | VARCHAR(500) | Y | NULL | - | Logo 地址 |
| description | TEXT | Y | NULL | - | 简介 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：主键、`studio_name` 唯一。

## 6. anime

动漫作品主数据及受控聚合字段。聚合字段由触发器维护，应用写入契约不得直接赋值。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 动漫编号 |
| title | VARCHAR(160) | N | - | UK 组合 | 中文标题 |
| title_jp | VARCHAR(160) | Y | NULL | - | 日文标题 |
| title_en | VARCHAR(160) | Y | NULL | - | 英文标题 |
| studio_id | BIGINT UNSIGNED | N | - | FK→anime_studio RESTRICT | 制作厂商 |
| synopsis | TEXT | Y | NULL | - | 简介 |
| cover_url | VARCHAR(500) | Y | NULL | - | 封面地址 |
| release_date | DATE | Y | NULL | UK 组合 | 首播/发行日期 |
| end_date | DATE | Y | NULL | CHECK 日期顺序 | 完结日期 |
| status | VARCHAR(20) | N | COMPLETED | CHECK 枚举 | UPCOMING/AIRING/COMPLETED |
| episode_count | INT UNSIGNED | N | 0 | 触发器维护，应用只读 | 集数统计 |
| average_rating | DECIMAL(4,2) | N | 0.00 | CHECK 0..10，触发器维护，应用只读 | 可见评论平均分 |
| rating_count | INT UNSIGNED | N | 0 | 触发器维护，应用只读 | 计入平均分的评论数 |
| favorite_count | INT UNSIGNED | N | 0 | 触发器维护，应用只读 | 收藏数 |
| review_count | INT UNSIGNED | N | 0 | 触发器维护，应用只读 | 可见评论数 |
| view_count | BIGINT UNSIGNED | N | 0 | 触发器维护，应用只读 | 观看历史记录数 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：`uk_anime_title_release(title, release_date)`、函数唯一索引 `uk_anime_title_release_key(title, COALESCE(release_date, '1900-01-01'))`、厂商、状态+日期、评分、互动热度。函数索引用于关闭 MySQL 普通唯一索引在 NULL 日期上的重复漏洞。

## 7. anime_genre

类型字典。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | SMALLINT UNSIGNED | N | AUTO_INCREMENT | PK | 类型编号 |
| genre_name | VARCHAR(40) | N | - | UK | 类型名称 |
| genre_slug | VARCHAR(50) | N | - | UK，格式 CHECK | URL/API 别名 |
| description | VARCHAR(255) | Y | NULL | - | 说明 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |

索引：主键、名称唯一、slug 唯一。

## 8. anime_genre_relation

动漫和类型 M:N 关系。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| anime_id | BIGINT UNSIGNED | N | - | PK/FK→anime CASCADE | 动漫 |
| genre_id | SMALLINT UNSIGNED | N | - | PK/FK→anime_genre RESTRICT | 类型 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 关系创建时间 |

索引：复合主键、`idx_agr_genre(genre_id)`。

## 9. anime_episode

动漫剧集。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 剧集编号 |
| anime_id | BIGINT UNSIGNED | N | - | FK→anime CASCADE | 所属动漫 |
| episode_number | SMALLINT UNSIGNED | N | - | UK 组合，CHECK 1..9999 | 集数序号 |
| title | VARCHAR(160) | N | - | - | 剧集标题 |
| synopsis | TEXT | Y | NULL | - | 剧集简介 |
| duration_seconds | INT UNSIGNED | Y | NULL | CHECK 60..36000 | 时长（秒） |
| air_date | DATE | Y | NULL | - | 播出日期 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：`uk_anime_episode_number(anime_id, episode_number)`、`idx_episode_anime_air_date`。

## 10. anime_staff

导演、原作者、作曲、声优等制作人员主数据。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 人员编号 |
| staff_name | VARCHAR(120) | N | - | 普通索引 | 名称 |
| staff_name_jp | VARCHAR(120) | Y | NULL | - | 日文名称 |
| birth_date | DATE | Y | NULL | - | 出生日期 |
| country | VARCHAR(64) | N | 日本 | - | 国家/地区 |
| avatar_url | VARCHAR(500) | Y | NULL | - | 头像地址 |
| bio | TEXT | Y | NULL | - | 简介 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：主键、`idx_staff_name`。

## 11. anime_staff_relation

动漫、人员和职务三元关系。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| anime_id | BIGINT UNSIGNED | N | - | PK/FK→anime CASCADE | 动漫 |
| staff_id | BIGINT UNSIGNED | N | - | PK/FK→anime_staff RESTRICT | 人员 |
| role_type | VARCHAR(32) | N | - | PK，枚举 CHECK | 导演/原作/音乐等 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 关系创建时间 |

允许职务：`DIRECTOR`, `SERIES_COMPOSITION`, `MUSIC`, `ORIGINAL_CREATOR`, `CHARACTER_DESIGN`, `VOICE_ACTOR`, `PRODUCER`, `ANIMATION_DIRECTOR`。

索引：复合主键、`idx_asr_staff_role(staff_id, role_type)`。

## 12. streaming_platform

合法播放平台字典。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | SMALLINT UNSIGNED | N | AUTO_INCREMENT | PK | 平台编号 |
| platform_name | VARCHAR(80) | N | - | UK | 平台名称 |
| platform_code | VARCHAR(32) | N | - | UK，格式 CHECK | 平台代码 |
| website_url | VARCHAR(500) | N | - | - | 官方地址 |
| country | VARCHAR(64) | Y | NULL | - | 国家/地区 |
| is_active | TINYINT UNSIGNED | N | 1 | CHECK IN (0,1) | 是否启用 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：主键、名称唯一、代码唯一。

## 13. anime_platform_relation

动漫和平台 M:N 发行关系。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| anime_id | BIGINT UNSIGNED | N | - | PK/FK→anime CASCADE | 动漫 |
| platform_id | SMALLINT UNSIGNED | N | - | PK/FK→platform RESTRICT | 平台 |
| external_url | VARCHAR(500) | Y | NULL | - | 平台入口 |
| available_from | DATE | Y | NULL | CHECK 日期顺序 | 上架日期 |
| available_until | DATE | Y | NULL | CHECK 日期顺序 | 下架日期 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 关系创建时间 |

索引：复合主键、`idx_apr_platform(platform_id)`。

## 14. user_favorite

用户收藏动漫关系。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| user_id | BIGINT UNSIGNED | N | - | PK/FK→sys_user CASCADE | 用户 |
| anime_id | BIGINT UNSIGNED | N | - | PK/FK→anime CASCADE | 动漫 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 收藏时间 |

索引：复合主键、`idx_favorite_anime_created`。重复收藏由复合 PK 拒绝。

## 15. user_watch_history

用户对剧集的最新观看进度。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 记录编号 |
| user_id | BIGINT UNSIGNED | N | - | FK→sys_user CASCADE | 用户 |
| episode_id | BIGINT UNSIGNED | N | - | FK→anime_episode CASCADE | 剧集 |
| progress_seconds | INT UNSIGNED | N | 0 | CHECK 0..36000 | 已观看秒数 |
| completed | TINYINT UNSIGNED | N | 0 | CHECK IN (0,1) | 是否完成 |
| last_watched_at | DATETIME | N | CURRENT_TIMESTAMP | - | 最近观看时间 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 首次记录时间 |

索引：`uk_watch_user_episode(user_id, episode_id)`、`idx_watch_user_time`、`idx_watch_episode`。

## 16. anime_review

用户对动漫的评论与评分。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 评论编号 |
| anime_id | BIGINT UNSIGNED | N | - | FK→anime CASCADE | 动漫 |
| user_id | BIGINT UNSIGNED | N | - | FK→sys_user CASCADE | 用户 |
| rating | TINYINT UNSIGNED | N | - | CHECK 1..10 | 评分 |
| content | VARCHAR(2000) | N | - | CHECK 长度 1..2000 | 评论正文 |
| status | TINYINT UNSIGNED | N | 1 | CHECK IN (0,1) | 可见/隐藏 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |

索引：`uk_review_user_anime(user_id, anime_id)`、`idx_review_anime_created`、`idx_review_user`。

## 17. operation_log

管理动作和事务演示的保留型审计日志。

| 字段 | 类型 | 空 | 默认 | 约束 | 含义 |
|---|---|---|---|---|---|
| id | BIGINT UNSIGNED | N | AUTO_INCREMENT | PK | 日志编号 |
| operator_user_id | BIGINT UNSIGNED | Y | NULL | FK→sys_user SET NULL | 操作者 |
| action_type | VARCHAR(50) | N | - | - | 动作类型 |
| target_type | VARCHAR(50) | Y | NULL | - | 目标类型 |
| target_id | BIGINT UNSIGNED | Y | NULL | - | 目标编号 |
| description | VARCHAR(1000) | Y | NULL | - | 描述 |
| ip_address | VARCHAR(45) | Y | NULL | - | IPv4/IPv6 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | - | 发生时间 |

索引：`idx_operation_log_user_time`、`idx_operation_log_action_time`、`idx_operation_log_target`。

## 18. 全局约束与统计

- 17 张业务表全部使用 InnoDB、utf8mb4。
- 主键全部显式定义。
- 实测 20 个外键、23 个 CHECK 约束、14 个唯一约束和 51 个索引，覆盖用户、内容、互动和审计边界。
- 每张核心表至少一个业务约束或唯一索引；关系表使用复合主键避免重复。
- 动漫聚合字段只由触发器、回算脚本和数据库实验室的受控操作维护，后端普通 CRUD 不提供写入字段。
- 计数回算脚本：`database/data/04_recalculate_counters.sql`。
- DDL：`database/schema/01_tables.sql`。
