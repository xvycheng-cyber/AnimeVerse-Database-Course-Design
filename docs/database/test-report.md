# 数据库测试报告

## 1. 执行结论

本报告只记录实际命令、实际数据库对象和真实退出码。测试环境为 Windows、MySQL 8.0.43、目标库 `anime_platform`。

| 项目 | 结果 |
|---|---|
| 首次数据库初始化 | PASS |
| 二次重复初始化 | PASS，库已存在时不删除/不重建 |
| 应用账户连接 | PASS，`Chengzi@localhost` |
| 最小权限 | PASS，目标库六类业务权限，无全局管理权限 |
| 结构/种子/计数断言 | PASS |
| 12 个触发器管理员清单 | PASS |
| 触发器行为测试 | PASS |
| 函数/过程/游标/事务测试 | PASS |
| 17 表 CRUD 回滚测试 | PASS |
| 重复收藏/重复评论负向测试 | PASS，均返回 1062 |
| 评分越界负向测试 | PASS，返回 3819 |
| 同标题且发行日期为 NULL 的重复测试 | PASS，返回 1062 |
| 18 条复杂查询 | PASS |
| `mysqlcheck --check` 物理检查 | PASS，17/17 表 OK |
| 静态安全/语法检查 | PASS |
| BCrypt 实际校验 | PASS，`admin=True`, `sakura=True` |
| 最终 PowerShell 测试汇总 | **17/17 PASS** |

## 2. 实际数据库对象

| 对象 | 实测数量 |
|---|---:|
| 业务表 | 17（16 核心 + `sys_user_profile`） |
| 外键 | 20 |
| CHECK 约束 | 23 |
| 唯一约束 | 14 |
| 索引 | 51 |
| 视图 | 4 |
| 触发器 | 12 |
| 存储过程 | 6 |
| 函数 | 3 |
| 复杂查询 | 18 |
| SQL 文件 | 44 |

17 张表全部实测为 `InnoDB`，排序规则为 `utf8mb4_0900_ai_ci`。

触发器实测清单：

- `trg_user_favorite_after_insert`
- `trg_user_favorite_after_update`
- `trg_user_favorite_after_delete`
- `trg_anime_review_after_insert`
- `trg_anime_review_after_update`
- `trg_anime_review_after_delete`
- `trg_watch_history_after_insert`
- `trg_watch_history_after_update`
- `trg_watch_history_after_delete`
- `trg_anime_episode_after_insert`
- `trg_anime_episode_after_update`
- `trg_anime_episode_after_delete`

过程/函数实测清单：

- `sp_add_anime`
- `sp_get_studio_statistics`
- `sp_get_anime_ranking`
- `sp_delete_anime`
- `sp_generate_studio_report`
- `sp_transaction_demo`
- `fn_anime_weighted_score`
- `fn_user_watch_minutes`
- `fn_visible_review_average`

## 3. 种子与计数实测

| 数据 | 实测行数 |
|---|---:|
| 厂商 | 8 |
| 动漫 | 30 |
| 类型 | 10 |
| 剧集 | 60 |
| 制作人员 | 20 |
| 播放平台 | 6 |
| 用户 | 10 |
| 用户资料 | 10 |
| 收藏 | 30 |
| 观看记录 | 30 |
| 评论 | 20 |

聚合字段回算后的实测总和：

```text
favorite_sum=30
review_sum=20
view_sum=30
episode_sum=60
```

收藏、评论、评分、观看和集数计数器的不一致行数均为 0；负数计数器行数为 0。

最终测试清理检查：临时动漫 0、临时 CRUD 用户 0、事务演示日志 0。核心种子仍为动漫 30、剧集 60、收藏 30、观看 30、评论 20、用户资料 10。

## 4. 实际执行步骤

### 4.1 首次初始化

执行：

```powershell
$env:DB_PASSWORD='<local app password>'
$env:MYSQL_ADMIN_PASSWORD='<local admin password>'
.\scripts\setup-database.ps1
```

实际结果：

- 创建 `anime_platform`。
- 创建 `Chengzi@localhost`。
- 授权：`SELECT, INSERT, UPDATE, DELETE, EXECUTE, SHOW VIEW ON anime_platform.*`。
- 管理员连接显示仅有 `GRANT USAGE ON *.*` 和上述目标库权限。
- 依次加载 schema、种子、函数、视图、游标过程、过程、触发器和计数回算。
- 首次种子验证输出：`8 30 60 20 6 10 30 30 20`。

### 4.2 重复初始化

再次执行同一脚本，实际结果：

- 提示数据库已存在，没有删除或重建。
- 应用账户已存在，仅刷新密码，未撤销其他库权限。
- 预检列出已有 17 张预期表；没有发现项目外表，因此允许继续幂等加载。
- 所有对象重建成功。
- 种子计数仍为 `8 30 60 20 6 10 30 30 20`。

### 4.3 完整测试

执行：

```powershell
$env:DB_PASSWORD='<local app password>'
$env:MYSQL_ADMIN_PASSWORD='<local admin password>'
.\scripts\test-database.ps1
```

最终实际输出摘要：

```text
[PASS] Schema and seed integrity assertions
[PASS] Administrator sees all 12 triggers - count=12
[PASS] Trigger counter assertions
[PASS] Function, procedure, cursor and transaction assertions
[PASS] All core and supporting tables CRUD transaction
[PASS] Duplicate favorite is rejected - 1062
[PASS] Duplicate review is rejected - 1062
[PASS] Rating above 10 is rejected by CHECK - 3819
[PASS] Duplicate title with NULL release date is rejected - 1062
[PASS] Transaction exception rolls back and raises SIGNAL
[PASS] Exception transaction left no log row - count=0
[PASS] sp_add_anime commits valid row
[PASS] sp_delete_anime removes transient row
[PASS] 18 complex queries execute
[PASS] No DROP DATABASE/DROP SCHEMA in database scripts
[PASS] No hard-coded 123456 database password in SQL
[PASS] PowerShell syntax parse

Database test summary: 17/17 passed.
```

## 5. BCrypt 实测

从数据库读取 `sys_user.password_hash` 后，使用 Python `bcrypt` 5.0.0 对指定课程凭据执行 `checkpw`：

```text
admin True
sakura True
```

数据库中只保存 `$2b$10$...` 哈希，没有保存明文。

## 6. 首轮失败与修正

首轮测试为 14/15 通过。失败项是：

```text
trigger_count_9  0  FAIL
```

原因：测试使用最小权限应用账户，MySQL 不向缺少 `TRIGGER` 权限的账户展示 `information_schema.TRIGGERS` 元数据。业务触发器本身已经通过行为测试。

修正：没有给 `Chengzi` 增加 `TRIGGER` 权限；把静态触发器清单校验移到管理员连接，应用账户继续只执行 DML/过程。后续补充回归项后复跑为 17/17 通过。

最终浏览器/API 联调还暴露了一个测试隔离问题：API 实验室会保留一条成功的
`TRANSACTION_DEMO_COMMIT` 审计记录，数据库测试如果直接按动作类型计数就会同时看到这条
历史记录。数据库测试已在执行 commit 断言前清理三类事务演示标记，并在断言后删除本次标记，
因此测试结果不再依赖其他测试的执行顺序。业务过程本身没有重复写入。

独立数据库审查又发现并修复了以下一致性问题：

1. 管理员动漫写入契约仍可提交触发器维护的评分和计数字段。现已从 `AnimeSaveRequest` 和前端表单移除这些输入，应用只读取聚合结果。
2. 评论改分原先基于旧平均值做增量计算，多次改分可能产生舍入漂移。现在评论触发器从可见评论重新计算平均值、评分人数和评论数。
3. 收藏、观看和剧集关系发生键值迁移时，原触发器只处理插入/删除。现新增三个 UPDATE 触发器，使旧作品计数减一、新作品计数加一。
4. MySQL 普通 `UNIQUE(title, release_date)` 允许多行日期为 NULL 的同名作品。现增加函数唯一索引 `uk_anime_title_release_key`，将空日期归一化后再参与唯一性判断。

上述改动新增了计数器迁移、重复改分和空日期同名作品回归测试，最终数据库测试由 16 项扩展为 17 项。

最终复验还修复了测试工具自身的 Windows PowerShell 5.1 兼容问题。`mysql.exe` 在预期重复键、CHECK 违反和事务 SIGNAL 场景会向 stderr 输出错误；原封装在 `$ErrorActionPreference='Stop'` 下会提前终止脚本。现在原生命令执行期间使用 `Continue` 收集输出，并严格以退出码断言预期失败，因此负向测试不再依赖调用 shell 的版本或错误流行为。

## 7. 静态校验

以下结果来自脚本执行，不是人工假定：

- 所有 PowerShell 文件通过 PowerShell AST 语法解析。
- `database/**/*.sql` 与数据库 PowerShell 脚本中没有 `DROP DATABASE` / `DROP SCHEMA`。
- SQL 文件中未检出生产式 `123456` 密码赋值。
- 应用账户没有使用全局管理员权限。
- 三张 Mermaid 源文件已由 Playwright + Mermaid 11 实际渲染为 SVG/PNG，并完成视觉检查。
- 四个视图均执行了实际行数断言：详情 30、厂商 8、热度 30、用户活动 10。
- 事务断言同时验证 commit 日志可见、rollback 日志不可见、exception 日志不可见。
- 管理员连接实测触发器为 12 个，其中收藏、观看和剧集各包含 INSERT、UPDATE、DELETE 三种事件。
- `uk_anime_title_release_key` 已阻止同标题且发行日期为 NULL 的重复插入。

## 8. 已知边界

- 种子中的用户、收藏、观看、评论和操作日志是课程演示数据，不冒充真实用户行为。
- 播放平台版权会变化，平台关系是本地演示快照。
- 本报告不验证 `backend/` 与 `frontend/` 的编译、接口联调和 UI，因为本任务明确限定数据库范围。
- 本机管理员凭据只通过执行进程环境变量传入，没有写入仓库文件。
