# 关系模式与删除策略

## 1. 关系模式

下划线语义以 PK/FK 标注表达：

- `sys_user` (`id` PK, `username` UK, `password_hash`, `nickname`, `email` UK, `avatar_url`, `status`, `last_login_at`, `created_at`, `updated_at`)
- `sys_role` (`id` PK, `role_code` UK, `role_name` UK, `description`, `created_at`)
- `sys_user_role` (`user_id` PK/FK, `role_id` PK/FK, `assigned_at`, `assigned_by` FK)
- `sys_user_profile` (`user_id` PK/FK, `bio`, `birthday`, `preferred_genre_id` FK, `profile_visibility`, `updated_at`)
- `anime_studio` (`id` PK, `studio_name` UK, `studio_name_en`, `country`, `founded_year`, `website_url`, `logo_url`, `description`, timestamps)
- `anime` (`id` PK, `title`, `title_jp`, `title_en`, `studio_id` FK, `synopsis`, `cover_url`, `release_date`, `end_date`, `status`, aggregate fields, timestamps)
- `anime_genre` (`id` PK, `genre_name` UK, `genre_slug` UK, `description`, `created_at`)
- `anime_genre_relation` (`anime_id` PK/FK, `genre_id` PK/FK, `created_at`)
- `anime_episode` (`id` PK, `anime_id` FK, `episode_number`, `title`, `synopsis`, `duration_seconds`, `air_date`, timestamps)
- `anime_staff` (`id` PK, `staff_name`, `staff_name_jp`, `birth_date`, `country`, `avatar_url`, `bio`, timestamps)
- `anime_staff_relation` (`anime_id` PK/FK, `staff_id` PK/FK, `role_type` PK, `created_at`)
- `streaming_platform` (`id` PK, `platform_name` UK, `platform_code` UK, `website_url`, `country`, `is_active`, timestamps)
- `anime_platform_relation` (`anime_id` PK/FK, `platform_id` PK/FK, `external_url`, `available_from`, `available_until`, `created_at`)
- `user_favorite` (`user_id` PK/FK, `anime_id` PK/FK, `created_at`)
- `user_watch_history` (`id` PK, `user_id` FK, `episode_id` FK, `progress_seconds`, `completed`, `last_watched_at`, `created_at`)
- `anime_review` (`id` PK, `anime_id` FK, `user_id` FK, `rating`, `content`, `status`, timestamps)
- `operation_log` (`id` PK, `operator_user_id` FK nullable, `action_type`, `target_type`, `target_id`, `description`, `ip_address`, `created_at`)

## 2. 外键删除/更新策略

| 外键 | 删除策略 | 原因 |
|---|---|---|
| `sys_user_role.user_id` | CASCADE | 用户删除后授权失去主体 |
| `sys_user_role.role_id` | RESTRICT | 仍被授予的角色不能误删 |
| `sys_user_role.assigned_by` | SET NULL | 保留授权事实，去除失效操作者 |
| `sys_user_profile.user_id` | CASCADE | 一对一资料不能独立存在 |
| `sys_user_profile.preferred_genre_id` | SET NULL | 类型删除不删除用户资料 |
| `anime.studio_id` | RESTRICT | 有作品的厂商必须由管理员先处理作品 |
| `anime_genre_relation.*` | anime CASCADE / genre RESTRICT | 删作品清关系，删类型前保证无引用 |
| `anime_episode.anime_id` | CASCADE | 剧集属于作品 |
| `anime_staff_relation.*` | anime CASCADE / staff RESTRICT | 删作品清演职员关系，人员主数据限制删除 |
| `anime_platform_relation.*` | anime CASCADE / platform RESTRICT | 删作品清发行关系，平台主数据限制删除 |
| `user_favorite.*` | CASCADE | 用户或作品删除后收藏无意义 |
| `user_watch_history.*` | CASCADE | 用户或剧集删除后观看记录无意义 |
| `anime_review.*` | CASCADE | 用户或作品删除后评论无意义 |
| `operation_log.operator_user_id` | SET NULL | 保留审计事件，不保留已删除用户的引用 |

所有外键更新策略为 `RESTRICT`，主键不随业务操作修改。

动漫聚合字段是数据库派生数据。触发器负责新增、删除、改分和关系迁移时的一致性，`data/04_recalculate_counters.sql` 提供全量回算；应用写入 DTO 不包含这些字段，普通 CRUD 只能读取聚合结果。

## 3. 唯一约束

- 用户 `username`、`email`。
- 角色 `role_code`、`role_name`。
- 厂商 `studio_name`。
- 动漫标题与发行日期的业务候选键由函数唯一索引 `uk_anime_title_release_key(title, COALESCE(release_date, '1900-01-01'))` 实现；空日期统一映射为 `1900-01-01` 后参与去重，不再保留会放过多个 NULL 组合的普通唯一索引。
- 类型 `genre_name`、`genre_slug`。
- 集数 `(anime_id, episode_number)`。
- 平台 `platform_name`、`platform_code`。
- 收藏主键 `(user_id, anime_id)`。
- 观看 `(user_id, episode_id)`。
- 评论 `(user_id, anime_id)`。

## 4. CHECK 完整性

- 状态值、可见性、完成标记限定为 0/1 或枚举字符串。
- 动漫评分 0 到 10，评论评分 1 到 10。
- 剧集编号 1 到 9999，时长 60 到 36000 秒。
- 观看进度 0 到 36000 秒。
- 评论正文长度 1 到 2000。
- 日期区间不得倒置：作品 `end_date >= release_date`，平台 `available_until >= available_from`。
- 用户名、角色代码、类型 slug、平台代码格式通过 `REGEXP_LIKE` 校验。

## 5. 索引设计

除主键和唯一索引外，查询热点索引包括：

- 动漫：厂商、状态+发布日期、评分、互动热度。
- 剧集：动漫+播出日期。
- 人员关系：人员+职务。
- 收藏：动漫+创建时间。
- 观看：用户+最近观看时间、剧集。
- 评论：动漫+创建时间、用户。
- 日志：用户+时间、动作+时间、目标对象。

`uk_anime_title_release_key` 是 MySQL 8.0 表达式索引，用来补齐“标题 + 发行日期”候选键在 NULL 日期上的语义。2026-11-16 实库核验共有 13 个 UNIQUE 约束和 50 个统计索引。
