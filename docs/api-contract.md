# 前后端接口契约

## 通用约定

- API 前缀：`/api`
- 认证头：`Authorization: Bearer <JWT>`
- JSON 字段：小驼峰
- 时间：`yyyy-MM-dd HH:mm:ss`
- 响应：`{ "code": 200, "message": "success", "data": ... }`
- 分页：`{ "records": [], "total": 0, "page": 1, "size": 10 }`

## 认证

- `POST /auth/register`：`{username,password,nickname,email}`
- `POST /auth/login`：`{username,password}`，返回 `{token,user}`
- `GET /auth/me`

## 公共内容

- `GET /anime?page&size&keyword&genreId&studioId&status&sort`
- `GET /anime/{id}`
- `GET /anime/ranking?limit`
- `GET /studios?page&size&keyword`
- `GET /studios/{id}`
- `GET /staff?page&size&keyword`
- `GET /genres`
- `GET /platforms`
- `GET /anime/{id}/episodes`
- `GET /anime/{id}/reviews?page&size`

## 用户互动

- `GET /user/favorites`
- `POST /user/favorites/{animeId}`
- `DELETE /user/favorites/{animeId}`
- `GET /user/watch-history`
- `POST /user/watch-history`：`{episodeId,progressSeconds}`
- `GET /user/reviews`
- `POST /anime/{animeId}/reviews`：`{rating,content}`
- `PUT /user/reviews/{id}`：`{rating,content}`
- `DELETE /user/reviews/{id}`
- `GET /user/profile`
- `PUT /user/profile`：`{nickname,email,avatarUrl}`

## 管理内容

以下写接口需要 `ADMIN` 角色：

- `/admin/anime` CRUD
- `/admin/studios` CRUD
- `/admin/genres` CRUD
- `/admin/episodes` CRUD
- `/admin/staff` CRUD
- `/admin/platforms` CRUD
- `/admin/users` 查询、启停、角色分配
- `/admin/roles` 查询与维护
- `/admin/reviews` 查询、隐藏、删除
- `/admin/logs`
- `/admin/statistics/overview`
- `/admin/statistics/trend?days=30`

## 数据库实验室

- `GET /lab/views/{viewName}`：白名单视图查询。
- `POST /lab/trigger/favorite`：`{animeId,enable}`，返回操作前后计数。
- `POST /lab/procedure/add-anime`：调用 `sp_add_anime`。
- `GET /lab/procedure/studio-statistics/{studioId}`
- `GET /lab/procedure/anime-ranking?limit`
- `GET /lab/cursor/studio-report`
- `POST /lab/transaction/demo`：`{mode:"commit"|"rollback"|"exception"}`。

## 测试账号

- 管理员：`admin / Admin123!`
- 普通用户：`sakura / User123!`

密码仅写入种子 SQL 的 BCrypt 哈希，不保存明文。
