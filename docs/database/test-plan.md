# 数据库测试计划

## 1. 测试目标

验证数据库结构、约束、种子数据、触发器、视图、函数、存储过程、游标、事务、安全初始化和脚本幂等性。实测结果写入 `test-report.md`，静态检查与真实 MySQL 执行分开记录。

## 2. 测试层级

| 层级 | 内容 | 脚本 |
|---|---|---|
| DDL 结构 | 17 表、InnoDB/utf8mb4、PK/FK/UK/CHECK/索引 | `tests/01_integrity_checks.sql` |
| 种子规模 | 30 动漫、60 集、20 人员等 | 同上 |
| 计数字段 | 收藏、评论、评分、观看、集数与来源一致 | 同上 |
| 触发器 | 插入/删除/修改/隐藏/恢复 | `tests/02_trigger_tests.sql` |
| 函数 | 3 个函数正常返回 | `tests/03_object_tests.sql` |
| 存储过程 | OUT 参数、排名、游标、事务 | 同上 + `test-database.ps1` |
| CRUD | 17 表真实新增/查询/更新/删除并回滚 | `crud/core_crud.sql` |
| 重复约束 | 重复收藏、重复评论必须失败 | `expected_duplicate_*.sql` |
| 域约束 | 评分 11 必须失败 | `expected_invalid_rating.sql` |
| 用户名域约束 | 3 字符用户名必须失败 | `expected_invalid_username.sql` |
| NULL 日期候选键 | 同名且发行日期均为 NULL 必须失败 | `expected_duplicate_null_release_title.sql` |
| 视图更新能力 | 课程视图实际 UPDATE 必须被拒绝 | `expected_view_update.sql` |
| 统计回算 | 隐藏评论不得计入评论数和平均分 | `04_recalculate_counter_tests.sql` |
| 异常事务 | `SIGNAL` 后回滚且无日志残留 | `test-database.ps1` |
| 复杂查询 | 18 条查询全部执行 | `queries/complex_queries.sql` |
| 脚本安全 | 无 DROP DATABASE、无硬编码真实 DB 密码、PS 语法通过 | `test-database.ps1` |
| 权限 | 应用账户连接和目标库最小权限 | `bootstrap-database.ps1` + `SHOW GRANTS` |
| API 契约 | 登录、注册、RBAC、六种排序、实验室和事务模式 | `scripts/test-api.ps1` |
| 浏览器 | 用户主路径、管理端 13 路由、CRUD、实验室、截图 | `scripts/browser-focused-check.mjs` |

## 3. 正向测试

1. 管理员创建库和账户。
2. 应用账户可连接并访问目标库。
3. 重复执行安装脚本不删除业务数据。
4. 所有种子计数达到阈值。
5. 新增/删除收藏、评论、观看、剧集后聚合值变化正确。
6. 存储过程 OUT 参数、排名结果、游标结果集正确。
7. commit 日志可见，rollback/exception 日志不可见。

## 4. 负向测试

1. 同一用户重复收藏同一动漫 → 1062。
2. 同一用户重复评论同一动漫 → 1062。
3. 评论评分 11 → CHECK 3819。
4. 用户名长度 3 → CHECK 3819。
5. 同名动漫且发行日期均为 NULL → 1062。
6. 对课程视图执行 UPDATE → 1288。
7. 不存在厂商/作品调用过程 → SQLSTATE 45000。
8. 事务 exception 模式 → SQLSTATE 45000 且回滚。

## 5. 测试隔离

- 触发器测试均在 `START TRANSACTION` 中，最后 `ROLLBACK`。
- CRUD 测试全部回滚。
- SP 成功添加的临时动漫由 `sp_delete_anime` 清理。
- 事务 commit 测试写入的审计行由测试脚本明确删除。
- 不使用 `DROP DATABASE`。

## 6. 通过标准

- PowerShell 测试脚本退出码 0。
- 断言输出中没有 `FAIL` 行。
- 所有预期失败均以正确错误码非零退出。
- 脚本安全静态检查无命中。
