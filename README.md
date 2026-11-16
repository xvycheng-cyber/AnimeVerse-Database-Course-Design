# 星漫云 AnimeVerse Management System

星漫云是一个面向动漫资源、制作厂商、制作人员、播放平台和用户互动场景的数据库课程设计项目。项目使用 MySQL 8.0 保存真实业务数据，以 Spring Boot 3 + MyBatis-Plus 提供接口，以 Vue 3 + TypeScript 构建用户端和管理员端。

## 目录

- `database/`：建库、建表、种子数据、查询、视图、触发器、过程、函数、游标、事务及测试脚本。
- `backend/`：Spring Boot REST API、JWT 认证、角色权限和数据库实验室接口。
- `frontend/`：Vue 3 用户端与管理后台。
- `docs/`：需求、设计、数据字典、图表、截图、报告和手册。
- `scripts/`：Windows 初始化、启动、测试和打包脚本。
- `dist/`：最终可提交压缩包。

## 本地启动

完整步骤见 `docs/manuals/部署与运行手册.md`。最短路径：

1. 将 `.env.example` 复制为 `.env` 并填写本地数据库密码。
2. 以 MySQL 管理员执行 `scripts/setup-database.ps1`。
3. 启动后端：`scripts/start-backend.ps1`。
4. 启动前端：`scripts/start-frontend.ps1`。
5. 打开 `http://localhost:5173`。

课程演示账号与数据库对象清单会在初始化完成后写入 `docs/manuals/测试账号.md`。

## 验收与测试材料

- 第三方独立审查：`docs/reports/third-party-independent-review.md`
- 问题整改记录：`docs/reports/third-party-correction-report.md`
- 整改后复测：`docs/reports/third-party-retest-report.md`
- 最终模拟评分：`docs/reports/final-score-report.md`
- 浏览器证据：`docs/screenshots/browser-report.json`

> 本项目中的数据库密码仅用于本地课程演示，不可用于公网部署。
