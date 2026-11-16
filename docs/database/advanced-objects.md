# 高级数据库对象

## 1. 视图

| 视图 | 用途 | 更新限制 |
|---|---|---|
| `v_anime_detail` | 动漫、厂商和聚合类型详情 | 含 `GROUP_CONCAT` 派生列和连接，通常不可直接更新；应写基表 |
| `v_studio_statistics` | 厂商作品数、平均分、收藏/评论/观看总量 | 含聚合，不可插入/更新 |
| `v_anime_popularity` | 加权热度和窗口排名 | 含表达式和窗口函数，不可更新 |
| `v_user_activity` | 用户角色、收藏、评论、观看汇总 | 多表聚合视图，不可更新 |

应用端只读视图，写操作走基表或存储过程。

## 2. 触发器

| 触发器 | 时机 | 逻辑 |
|---|---|---|
| `trg_user_favorite_after_insert` | AFTER INSERT | `favorite_count + 1` |
| `trg_user_favorite_after_update` | AFTER UPDATE | 收藏迁移时旧作品减一、新作品加一 |
| `trg_user_favorite_after_delete` | AFTER DELETE | `favorite_count - 1`，下限 0 |
| `trg_anime_review_after_insert` | AFTER INSERT | 可见评论更新平均分和计数 |
| `trg_anime_review_after_update` | AFTER UPDATE | 处理改分、隐藏、恢复及迁移作品 |
| `trg_anime_review_after_delete` | AFTER DELETE | 可见评论删除后回退平均分和计数 |
| `trg_watch_history_after_insert` | AFTER INSERT | 对应动漫 `view_count + 1` |
| `trg_watch_history_after_update` | AFTER UPDATE | 剧集迁移时旧作品减一、新作品加一 |
| `trg_watch_history_after_delete` | AFTER DELETE | `view_count - 1`，下限 0 |
| `trg_anime_episode_after_insert` | AFTER INSERT | 对应动漫 `episode_count + 1` |
| `trg_anime_episode_after_update` | AFTER UPDATE | 集数迁移时旧作品减一、新作品加一 |
| `trg_anime_episode_after_delete` | AFTER DELETE | `episode_count - 1`，下限 0 |

触发器总数为 12。重复收藏由 `user_favorite` 复合主键阻止；重复评论由 `uk_review_user_anime` 阻止。负数计数由无符号字段和 `GREATEST(..., 0)` 双重保护。评论改分、隐藏、恢复和迁移均从可见评论重新计算平均值与计数，避免增量舍入漂移。

`anime` 的 `episode_count`、`average_rating`、`rating_count`、`favorite_count`、`review_count` 和 `view_count` 是触发器维护的派生字段。应用写入 DTO 和后台表单不提供这些字段，读取和统计接口可以展示它们。

## 3. 函数

- `fn_anime_weighted_score(anime_id)`：70% 评分 + 20% 收藏（封顶 100）+ 10% 评论（封顶 100）。
- `fn_user_watch_minutes(user_id)`：用户已记录观看秒数折算分钟。
- `fn_visible_review_average(anime_id)`：实时计算可见评论平均分，可用于核对触发器结果。

## 4. 存储过程

| 过程 | 参数 | 用途 |
|---|---|---|
| `sp_add_anime` | 8 个 IN + 1 个 OUT | 校验厂商/状态，事务插入，OUT 返回新 ID |
| `sp_get_studio_statistics` | 1 个 IN + 5 个 OUT | 校验厂商并返回作品数、均分、收藏/评论总量 |
| `sp_get_anime_ranking` | 1 个 IN | 返回限长排名结果集 |
| `sp_delete_anime` | 1 个 IN + 1 个 OUT | 校验存在，事务删除，OUT 返回影响行数 |
| `sp_transaction_demo` | 2 个 IN + 1 个 OUT | commit/rollback/exception 三种事务模式 |
| `sp_generate_studio_report` | 无 | 游标遍历厂商，生成临时报表结果集 |

非法输入由 `SIGNAL SQLSTATE '45000'` 拒绝；写过程使用 `EXIT HANDLER FOR SQLEXCEPTION`、`ROLLBACK` 和 `RESIGNAL`。

## 5. 游标

`sp_generate_studio_report` 真实使用：

```sql
DECLARE studio_cursor CURSOR FOR ...;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
OPEN studio_cursor;
read_loop: LOOP
    FETCH ... INTO ...;
    IF v_done THEN LEAVE read_loop; END IF;
    INSERT INTO tmp_studio_report ...;
END LOOP;
CLOSE studio_cursor;
```

结果包含厂商名称、作品数量、平均评分、收藏总数，逐行写入临时表后返回。

## 6. 事务

- 提交：`sp_transaction_demo(..., 'commit', ...)` 写入审计行并提交。
- 回滚：`sp_transaction_demo(..., 'rollback', ...)` 写入后显式回滚。
- 异常：`sp_transaction_demo(..., 'exception', ...)` 插入后 `SIGNAL`，异常处理器回滚并重新抛出。
- 原始示例：`database/transactions/transaction_demo.sql`。
- 可重复断言：`database/transactions/transaction_assertions.sql`。

## 7. 调用示例

`database/procedures/call_examples.sql` 包含函数/过程/游标/事务的可执行调用。异常调用以注释形式标出，因为它是预期失败场景，应由测试脚本以非零退出码验证。
