# 数据库安全设计

## 1. 账户分离

| 用途 | 账户 | 权限 |
|---|---|---|
| 建库、授权、DDL、安装过程/触发器 | 管理员（默认 root，仅本地） | 管理员自身权限 |
| 后端业务连接 | `Chengzi@localhost` | 仅 `anime_platform.*` 的 SELECT/INSERT/UPDATE/DELETE/EXECUTE/SHOW VIEW |

应用账户不拥有 `CREATE DATABASE`、`DROP`、`GRANT OPTION`、`SUPER`、`FILE` 等全局能力。

## 2. 安全初始化流程

`scripts/bootstrap-database.ps1`：

1. 查询 `information_schema.SCHEMATA` 判断库是否存在；存在则只提示，不删除。
2. 查询 `mysql.user` 判断 `Chengzi@localhost` 是否存在。
3. 不存在时 `CREATE USER`；存在时只刷新该账户密码。
4. 对目标库追加最小业务授权。
5. 显示 `SHOW GRANTS`，若检测到超范围全局权限只警告，不撤销其他库权限。
6. 使用应用账户重新连接验证。

脚本不包含 `DROP DATABASE` 或 `DROP SCHEMA`，不做跨库 `REVOKE`，不修改其他数据库。

## 3. 凭据管理

- `.env.example` 只包含占位符 `change_me`。
- `.env` 被 `.gitignore` 忽略。
- PowerShell 从参数或环境变量读取 `DB_PASSWORD` 和 `MYSQL_ADMIN_PASSWORD`。
- 密码通过子进程环境变量 `MYSQL_PWD` 传给 MySQL，不出现在进程命令行参数中。
- 交互模式使用 `Read-Host -AsSecureString`。
- SQL 种子只保存 BCrypt 哈希；`admin/Admin123!` 和 `sakura/User123!` 是课程演示凭据。

## 4. SQL 注入边界

- 初始化脚本对库名和用户名使用白名单正则。
- 密码值进入 SQL 前拒绝换行/NUL，并转义反斜杠和单引号。
- 应用后端应继续使用 MyBatis 参数绑定，不拼接用户输入。
- 数据库实验室的视图名必须使用 API 契约要求白名单，不允许任意标识符执行。

## 5. 审计与保留

- `operation_log` 对用户外键使用 `ON DELETE SET NULL`，删除用户后保留操作事实。
- `action_type`、`target_type`、`target_id` 支持按动作和目标检索。
- 事务实验室三种模式都会产生可观察的日志写入/回滚行为。

## 6. 风险说明

本地演示密码不适合公网环境。生产部署应使用密钥管理、TLS、单独迁移账户、审计归档和定期权限复核。

