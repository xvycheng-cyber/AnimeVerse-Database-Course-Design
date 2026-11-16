# AnimeVerse Backend

Spring Boot 3.3、Java 17 字节码、MyBatis-Plus、Spring Security、JWT、BCrypt 和
SpringDoc OpenAPI 后端。接口前缀为 `/api`，统一响应为 `{code,message,data}`。

## 启动

从仓库根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

脚本读取仓库根 `.env`，不会覆盖当前进程中已经设置的变量。必要变量：

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
JWT_SECRET
```

`JWT_SECRET` 至少 32 个 UTF-8 字节。`APP_CORS_ORIGIN` 支持逗号分隔的多个来源。
脚本会依次检查 `MAVEN_CMD`、当前用户 `.codex/tools` 下的 Maven、`PATH` 中的
`mvn.cmd`/`mvn`。本项目不需要修改全局 Maven 或 JDK 配置。

## 测试与构建

```powershell
& 'C:\Users\Chengzi\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd' test
& 'C:\Users\Chengzi\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd' package
```

项目固定使用 Java 17；`pom.xml` 使用 `maven.compiler.release=17`，启动脚本会优先定位
本机 JDK 17，而不是使用更高版本的默认 Java。
测试覆盖 JWT 签发/篡改拒绝、BCrypt 注册、JWT principal 收藏、评论越权、
真实 schema 字段映射、操作日志字段和 `sp_add_anime` 参数绑定。

## 数据库契约

持久层以 `database/schema/01_tables.sql` 为准，主要映射如下：

- `anime(title,title_jp,title_en,release_date,end_date,average_rating,rating_count,episode_count)`
- `anime_studio(studio_name,studio_name_en,website_url)`
- `anime_genre(genre_name,genre_slug)`
- `anime_episode(synopsis,duration_seconds,air_date)`
- `anime_staff(staff_name,staff_name_jp,country,bio)`
- `anime_staff_relation(role_type)`
- `anime_platform_relation(external_url,available_from,available_until)`
- `streaming_platform(platform_name,platform_code,website_url,country,is_active)`
- `user_favorite(user_id,anime_id)`，复合主键，无独立 `id`
- `user_watch_history(progress_seconds,completed,last_watched_at)`
- `operation_log(operator_user_id,action_type,target_type,target_id,description,ip_address,created_at)`

对前端现有小驼峰字段做兼容输出：

- `title_jp -> originalTitle`
- `YEAR(release_date) -> releaseYear`
- `average_rating -> rating`
- `studio_name -> name/studioName`
- `duration_seconds -> durationSeconds`，同时按分钟向上取整输出 `durationMinutes`

当请求只提供 `releaseYear` 时，数据库写入该年 `01-01`；请求提供精确
`releaseDate` 时以精确日期为准。`ONGOING/PAUSED` 入库为 `AIRING`，
`FINISHED` 入库为 `COMPLETED`。

数据库没有剧集 `video_url` 字段，因此写接口不会静默丢弃视频地址；传入非空
`videoUrl` 会返回 400，并要求使用 `anime_platform_relation.external_url`。
平台表也没有 `logo_url/description` 字段，相关兼容响应返回 `null`，不伪造数据。

## 高级数据库对象

实验室接口只直接调用真实 MySQL 对象，不使用内存结果冒充数据库结果。

```sql
CALL sp_add_anime(?, ?, ?, ?, ?, ?, ?, ?, ?);
-- 8 IN + 1 OUT

CALL sp_get_studio_statistics(?, ?, ?, ?, ?, ?);
-- 1 IN + 5 OUT

CALL sp_get_anime_ranking(?);
CALL sp_generate_studio_report();
CALL sp_transaction_demo(?, ?, ?);
-- 2 IN + 1 OUT
```

视图白名单为 `v_anime_detail`、`v_studio_statistics`、`v_anime_popularity`、
`v_user_activity`。收藏实验通过真实收藏写入/删除触发 `favorite_count` 变化。
事务实验比较调用前后的 `operation_log` 行，验证提交可见、回滚不可见。

## 删除策略

- 动漫存在收藏、评论、观看历史或剧集时禁止删除；数据库没有归档状态列。
- 厂商存在作品、类型/制作人员/平台仍被引用时禁止删除。
- 剧集存在观看历史时禁止删除。
- 用户不物理删除，只允许管理员启停。
- 内置 `ADMIN`、`USER` 角色不可删除，且不能移除最后一个管理员。
