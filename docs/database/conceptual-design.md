# 概念结构与 ER 设计

## 1. 实体与主键

| 实体 | 主键 | 候选键/说明 |
|---|---|---|
| 用户 | `sys_user.id` | `username`, `email` |
| 角色 | `sys_role.id` | `role_code`, `role_name` |
| 用户资料 | `sys_user_profile.user_id` | 与用户同键，一对一 |
| 制作厂商 | `anime_studio.id` | `studio_name` |
| 动漫 | `anime.id` | `(title, COALESCE(release_date, '1900-01-01'))`，由函数唯一索引实现 |
| 类型 | `anime_genre.id` | `genre_name`, `genre_slug` |
| 剧集 | `anime_episode.id` | `(anime_id, episode_number)` |
| 制作人员 | `anime_staff.id` | 名称配合人员明细使用 |
| 平台 | `streaming_platform.id` | `platform_name`, `platform_code` |
| 收藏 | `(user_id, anime_id)` | 复合主键阻止重复 |
| 观看历史 | `user_watch_history.id` | `(user_id, episode_id)` |
| 评论 | `anime_review.id` | `(user_id, anime_id)` |
| 操作日志 | `operation_log.id` | 审计流水号 |

关系型实体（类型关系、人员关系、平台关系、用户角色）使用复合主键；用户资料使用共享主键。

## 2. 关系基数

| 关系 | 基数 | 逻辑实现 |
|---|---|---|
| 用户—角色 | M:N | `sys_user_role` |
| 用户—资料 | 1:0..1 | `sys_user_profile.user_id` 同时为 PK/FK |
| 厂商—动漫 | 1:N | `anime.studio_id` |
| 动漫—类型 | M:N | `anime_genre_relation` |
| 动漫—剧集 | 1:N | `anime_episode.anime_id` |
| 动漫—制作人员 | M:N（带角色） | `anime_staff_relation(anime_id, staff_id, role_type)` |
| 动漫—平台 | M:N | `anime_platform_relation` |
| 用户—动漫 | M:N（收藏） | `user_favorite` |
| 用户—剧集 | 1:N（最新进度唯一） | `user_watch_history` |
| 用户—动漫 | 1:N（每对用户/动漫唯一评论） | `anime_review` |
| 用户—日志 | 1:N，可保留 | `operation_log.operator_user_id` 删除时置空 |

三张 Mermaid 图位于：

- `docs/diagrams/01-global-er.mmd`
- `docs/diagrams/02-production-er.mmd`
- `docs/diagrams/03-user-interaction-er.mmd`

对应 SVG/PNG 由 `scripts/render-diagrams.mjs` 生成。

## 3. 属性设计原则

- 主数据实体保留稳定业务属性，时间戳统一使用 `DATETIME`。
- 互动关系优先使用复合键表达天然唯一性，而不是仅依赖应用层查重。
- 动漫表保留 `episode_count`, `favorite_count`, `review_count`, `rating_count`, `average_rating`, `view_count` 等聚合字段，便于列表查询；一致性由触发器维护并有回算脚本，应用侧只读。
- 评论正文限制 2000 字符，状态支持管理端隐藏；隐藏评论不计入公开统计。
- `operation_log` 不随用户删除而级联删除，满足审计保留要求。

## 4. 第一范式（1NF）

所有字段均为原子值；类型、人员、平台等重复属性已拆为独立关系表，没有在单元格中存放数组或逗号分隔的业务主键。

## 5. 第二范式（2NF）

复合主键关系表的非键属性完全依赖整个主键：

- `sys_user_role` 的 `assigned_at` 依赖用户与角色的具体授权组合。
- `anime_genre_relation` 的创建时间依赖动漫与类型组合。
- `anime_staff_relation` 的角色依赖动漫、人员与职务三元组。
- `user_favorite` 的收藏时间依赖用户与动漫组合。

不存在仅依赖部分复合主键的非键字段。

## 6. 第三范式（3NF）

实体表的非键字段只描述本实体。类型名称、厂商名称、平台名称不冗余存储在动漫表中，而是通过外键和关系表引用。`v_anime_detail` 中出现的类型名称和厂商名称是只读查询视图，不是基表冗余。

若严格按教材中的纯关系模型定义，动漫表的 `episode_count`、`favorite_count`、`review_count`、`rating_count`、`average_rating` 和 `view_count` 属于从明细关系派生的冗余字段，因此不能宣称物理数据库“完全无冗余”。逻辑模型已经将多值属性和关系拆分为独立表，核心实体满足第三范式；物理层在上述字段上采用了受控反规范化。

这些派生字段只用于读取性能，由 12 个触发器、后台只读字段契约、隐藏评论过滤回算脚本和数据库测试共同维护。普通 CRUD DTO 与后台编辑表单均不提供写入入口，因此这种冗余属于可解释、可追溯的物理设计取舍，而不是未声明的传递依赖。
