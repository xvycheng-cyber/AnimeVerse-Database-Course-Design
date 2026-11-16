# 星漫云最终验收报告

验收日期：2026-11-16  
项目名称：星漫云——全网动漫资源与制作厂商管理平台  
项目目录：`AnimeVerse-Database-Course-Design`

## 1. 最终功能完成情况

项目已实现真实可运行的 MySQL、Spring Boot 3、Vue 3 系统，覆盖需求、E-R 设计、关系模式、完整性、CRUD、复杂查询、视图、触发器、存储过程、游标、函数、事务、前后端、UI、安全、测试、Markdown/DOCX 报告和 ZIP 交付。

普通用户端和 Web 登录/注册共 13 个页面，管理员端 13 个页面。管理员七类内容实体使用真实 API 完成 CRUD，数据库实验室调用真实数据库对象。未使用 SQLite、H2、静态假数据或 Java 代码冒充数据库高级对象。

## 2. 数据库对象统计

| 对象 | 实测数量 |
|---|---:|
| 业务表 | 17 |
| 视图 | 4 |
| 触发器 | 12 |
| 存储过程 | 6 |
| 函数 | 3 |
| 游标型存储过程 | 1 |
| 外键 | 20 |
| CHECK 约束 | 23 |
| UNIQUE 约束 | 14 |
| 索引 | 51 |
| SQL 文件 | 44 |

4 个视图为 `v_anime_detail`、`v_studio_statistics`、`v_anime_popularity`、`v_user_activity`。

12 个触发器覆盖收藏、评论、观看历史、剧集的新增、修改和删除。存储过程覆盖动漫新增/删除、厂商统计、排行榜、游标厂商报表和三种事务模式。函数覆盖加权评分、观看分钟数和可见评论平均分。

数据库使用 MySQL 8.0.43、InnoDB、utf8mb4。应用账户 `Chengzi@localhost` 仅有 `SELECT`、`INSERT`、`UPDATE`、`DELETE`、`EXECUTE`、`SHOW VIEW` 权限，未授予全局 DBA 权限。

## 3. 前后端实现

后端为 Java 17、Spring Boot 3.3.7、MyBatis-Plus、Spring Security、JWT、BCrypt 和 SpringDoc OpenAPI。129 个 Java 主源码文件，9 个测试源码文件。

前端为 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios 和 ECharts。用户端使用深色蓝紫动漫视觉，管理员端使用工作台式浅色界面。页面具备加载、错误、空数据、分页、搜索、筛选、删除确认和图片失败回退。

数据库实验室包含视图、触发器、存储过程、游标和事务五个真实页签。管理端具备概览统计、操作日志、用户角色、评论审核和七类内容管理。

## 4. 测试通过情况

| 测试项目 | 最终结果 |
|---|---:|
| 数据库测试 | 17/17 PASS |
| 后端 clean package | 17/17 PASS |
| 前端单元测试 | 4/4 PASS |
| TypeScript + Vite | 2361 模块，PASS |
| API 联调 | 22/22 PASS |
| 浏览器回归 | 19/19 PASS |
| 控制台 / HTTP / 横向溢出 | 0 / 0 / 0 |
| 浏览器 warning | 0 |
| 运行截图 | 8 |

最终浏览器覆盖七个后台实体 CRUD、用户角色恢复、聚合字段只读、评论隐藏/恢复/删除、收藏首次点击、实验室查询、桌面和移动截图。

API 最终覆盖登录、RBAC、无效 JWT、停用账户、角色 CRUD、收藏、观看、评论、管理统计、四视图、触发器、存储过程、审计、游标和事务三种模式。

## 5. 最终模拟评分

| 评分项目 | 满分 | 实际得分 | 扣分理由 |
|---|---:|---:|---|
| 概念、逻辑设计 | 20 | 20 | 无扣分 |
| 表创建及基础操作 | 30 | 30 | 无扣分 |
| 数据库高级编程 | 20 | 19 | 写过程事务未提供调用方嵌套组合验证 |
| UI 设计 | 30 | 29 | 大 chunk 警告与远程封面网络依赖 |
| 总分 | 100 | 98 | 非阻断工程边界 |

## 6. 未解决问题与残余风险

1. 远程 AniList 封面依赖网络，动漫 ID 1 有本地备用封面。
2. Vite 构建保留 ECharts 大 chunk 警告，不影响运行。
3. 当前为本地课程设计，未做生产级登录限流、CSP、密钥托管和审计归档。
4. JWT 存储于浏览器 `localStorage`，公网部署前需重新评估。
5. 写存储过程自行管理事务，当前系统不嵌套调用多个写过程。

除上述边界外，没有未修复的阻断性缺陷。

## 7. 交付文件清单

1. `README.md`：项目概览、启动入口和目录说明。
2. `.env.example`：数据库、JWT、CORS 和前端 API 配置模板。
3. `backend/`：Spring Boot 源码、测试和 `pom.xml`。
4. `frontend/`：Vue 3 源码、测试、配置和 `package.json`。
5. `database/`：表、种子、CRUD、查询、视图、触发器、过程、函数、游标、事务和测试 SQL。
6. `docs/database/`：需求、概念、关系模式、数据字典、高级对象、安全和测试报告。
7. `docs/diagrams/`：Mermaid 源文件与 PNG/SVG E-R 图。
8. `docs/screenshots/`：8 张真实运行截图和 `browser-report.json`。
9. `docs/reports/数据库课程设计报告.md/.docx`。
10. `docs/reports/AI交互报告.md/.docx`。
11. `docs/reports/final-independent-review.md`。
12. `docs/reports/final-correction-report.md`。
13. `docs/reports/final-acceptance-report.md`。
14. `docs/manuals/`：部署运行、用户操作、故障排查和测试账号。
15. `scripts/`：数据库初始化、启动、测试、图表、DOCX 和打包脚本。
16. `dist/AnimeVerse-Database-Course-Design.zip`：最终交付压缩包。

## 8. 启动方法

### 8.1 环境准备

1. 安装 MySQL 8.0、Java 17、Maven 和 Node.js。
2. 将 `.env.example` 复制为 `.env`，填写本地数据库管理员密码、应用密码和至少 32 字节 JWT 密钥。
3. 确认 `.env` 不进入 Git 和 ZIP。

### 8.2 初始化数据库

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-database.ps1
```

### 8.3 启动后端

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

后端地址：`http://127.0.0.1:8080`  
OpenAPI：`http://127.0.0.1:8080/v3/api-docs`

### 8.4 启动前端

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
```

前端地址：`http://127.0.0.1:5173`

### 8.5 最终测试

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-database.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-api.ps1
node .\scripts\browser-smoke.mjs
```

## 9. 最终 ZIP

目标路径：

```text
dist/AnimeVerse-Database-Course-Design.zip
```

最终打包状态：已完成。主报告 DOCX 为 30 页，AI 交互报告 DOCX 为 12 页；两份文档均经 Word 导出 PDF、Poppler 转 PNG 后逐页复核。ZIP 已执行必需文件检查、禁止项检查和临时目录解压完整性复验。  
归档排除：`.env`、`.git`、`node_modules`、`target`、`frontend/dist`、缓存、日志和 QA 临时文件。

## 10. 验收结论

项目已达到课程设计的最终完成标准，可提交源码、SQL、报告、截图、手册和压缩包。最终模拟得分为 `98/100`。
