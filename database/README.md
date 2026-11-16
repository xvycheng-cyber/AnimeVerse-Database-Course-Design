# 数据库脚本执行顺序

推荐使用 `scripts/setup-database.ps1`，不要手工拆散执行。脚本按以下顺序运行，并在每一步检查 MySQL 退出码：

1. `schema/00_create_database.sql`：管理员检查并创建 `anime_platform`。
2. `schema/01_tables.sql`：创建 16 张核心表和 `sys_user_profile` 一对一辅助表。
3. `data/01_reference_and_users.sql`：角色、BCrypt 用户、厂商、类型、平台和用户资料。
4. `data/02_anime_content.sql`：真实动漫作品、类型关系、剧集、人员、平台关系和审计种子。
5. `data/03_interactions.sql`：收藏、观看和评论。
6. `data/05_cover_images.sql`：为公开作品补齐可重复执行的真实封面地址。
7. `functions/*.sql`：业务函数。
8. `views/*.sql`：四个只读统计/详情视图。
9. `cursors/sp_generate_studio_report.sql`：游标过程。
10. `procedures/*.sql`：其余存储过程。
11. `triggers/*.sql`：计数一致性触发器。
12. `data/04_recalculate_counters.sql`：从来源关系回算全部聚合字段。

重复执行不会删除数据库或业务数据。种子使用 `INSERT IGNORE`；函数、过程和视图使用 `CREATE OR REPLACE` / `DROP ... IF EXISTS` 重建；触发器也按名称安全重建。

真实执行与测试：

```powershell
$env:DB_PASSWORD = '<local app password>'
$env:MYSQL_ADMIN_USER = 'root'
$env:MYSQL_ADMIN_PASSWORD = '<local admin password>'
.\scripts\setup-database.ps1
.\scripts\test-database.ps1
```

不要把真实密码写入 `.env.example`、SQL、文档或 Git。
