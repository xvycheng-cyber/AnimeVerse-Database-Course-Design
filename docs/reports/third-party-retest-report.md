# 星漫云第三方整改复测报告

复测日期：2026-11-16  
复测对象：整改后的源码、真实 MySQL、运行中的 Spring Boot、运行中的 Vue 3 页面、8 张截图和最终打包脚本  
复测原则：使用当前命令退出码、HTTP 响应、数据库错误码和浏览器实际行为；不以 Markdown 描述替代运行结果。

## 1. 复测环境

| 项目 | 实测值 |
|---|---|
| MySQL | 8.0.43，`127.0.0.1:3306` |
| 数据库 | `anime_platform` |
| 后端 | Java 17，Spring Boot 3.3.7，端口 8080 |
| 前端 | Vue 3 + Vite，端口 5173 |
| 健康检查 | HTTP 200，`UP` |
| 浏览器 | Headless Chrome，桌面 1440×1000，移动 390×844 |
| 测试日期 | 2026-11-16 |

## 2. 数据库对象复测

| 对象/约束 | 结果 |
|---|---:|
| 基表 | 17 |
| 视图 | 4 |
| 触发器 | 12 |
| 存储过程 | 6 |
| 函数 | 3 |
| 外键 | 20 |
| CHECK | 23 |
| UNIQUE | 13 |
| 统计索引 | 50 |
| 基表字段 | 123 |

`mysqlcheck --check` 已逐表执行，17/17 张表均返回 `OK`。

应用账户授权：

```text
GRANT USAGE ON *.* TO `Chengzi`@`localhost`
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE, SHOW VIEW ON `anime_platform`.* TO `Chengzi`@`localhost`
```

结论：数据库运行状态已验证，结构对象来自当前 MySQL，而不是根据 SQL 文件推断。

## 3. 数据库功能复测

执行命令：

```powershell
.\scripts\test-database.ps1
```

实际结果：

```text
Database test summary: 20/20 passed.
```

关键证据：

| 测试 | 实际结果 |
|---|---|
| Schema and seed integrity assertions | PASS |
| Administrator sees all 12 triggers | PASS，count=12 |
| Trigger counter assertions | PASS |
| Function, procedure, cursor and transaction assertions | PASS |
| Counter recalculation excludes hidden reviews | PASS |
| All core and supporting tables CRUD transaction | PASS |
| Duplicate favorite | MySQL 1062 |
| Duplicate review | MySQL 1062 |
| Rating above 10 | MySQL 3819 |
| Three-character username | MySQL 3819 |
| Duplicate title with NULL release date | MySQL 1062 |
| Course view rejects UPDATE | MySQL 1288 |
| Transaction exception rolls back | PASS |
| Exception transaction left no log row | PASS，count=0 |
| sp_add_anime / sp_delete_anime | PASS |
| 18 complex queries | PASS |
| Script safety and PowerShell syntax | PASS |

游标过程实库定义检查：

- `DECLARE CURSOR`：存在。
- `OPEN`：存在。
- `FETCH`：存在。
- `CLOSE`：存在。
- `HANDLER`：存在。
- 实际返回厂商行数：8。

事务复测：

- COMMIT：`committed=true`，可见日志 1。
- ROLLBACK：`rolledBack=true`，可见日志 0。
- EXCEPTION：业务异常被捕获并转为可观察演示结果，`rolledBack=true`，可见日志 0。

## 4. API 复测

执行命令：

```powershell
.\scripts\test-api.ps1
```

实际结果：

```text
API test summary: 27/27 passed.
```

覆盖内容包括：

- 管理员和普通用户登录、角色返回。
- Actuator 健康检查。
- 短用户名 HTTP 400。
- 合法注册和临时用户清理。
- 公开列表、详情、排行榜和 OpenAPI。
- 无令牌 401、普通用户管理员接口 403、普通用户实验室 403。
- 无效 Bearer 令牌对公开接口和受保护接口的隔离行为。
- SQL 注入式关键词不扩大结果集。
- 六种动漫排序值和兼容别名。
- 收藏、观看、评论新增修改删除。
- 管理员统计、日志、用户和角色。
- 停用账户登录 403 与恢复。
- 四视图、触发器、过程、游标、事务三模式。
- 实验室写操作审计。
- 源码无 `v-html`。

清理结果：临时注册用户和 API 测试资源均被测试脚本删除。

## 5. 构建与单元测试复测

后端：

```text
mvn clean package
主源码：129
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端：

```text
Frontend tests: 4/4 passed
Vite build: 2361 modules transformed
Build result: success
```

非阻断警告：

- `index` chunk 约 886.89 kB。
- ECharts 相关 chunk 约 1117.55 kB。

### 5.1 DOCX 交付复测

两份 DOCX 均从当前 Markdown 重新生成，并使用 Word 实际导出 PDF、Poppler 转逐页 PNG 后人工检查：

| 文件 | 实测页数 | 复测结果 |
|---|---:|---|
| `数据库课程设计报告.docx` | 30 | PASS |
| `AI交互报告.docx` | 12 | PASS |

复核结果：

- 封面标题无下边框和装饰横线。
- 普通二级标题不再逐项强制分页。
- 表格、E-R 图、运行截图和图注未发现裁切、重叠或错页。
- 第 19 页原先由大图导致的过大页尾留白已消除。

## 6. 浏览器复测

浏览器已拆成四个可独立复现批次。

### 6.1 用户批次

命令：

```powershell
$env:BROWSER_FOCUS='user'; node .\scripts\browser-focused-check.mjs
```

结果：

```text
Focused browser summary (user): 2/2 checks passed; 0 issue(s); 0 warning(s).
```

实际覆盖：

- 注册空必填字段和 3 字符用户名被拒绝。
- 登录普通用户。
- 首页动漫卡片和排行条目。
- 动漫列表第 1 页到第 2 页确实换页。
- 收藏最多、评分最高、最新上线、标题排序四种 UI 排序与 API 第一页一致。
- 动漫详情、剧集控件、厂商列表、厂商详情、制作人员、排行榜、收藏、观看历史、我的评论、个人中心。
- 普通用户访问 `/admin` 被重定向。

用户端 13 个路由均已实际打开。

### 6.2 管理批次

命令：

```powershell
$env:BROWSER_FOCUS='admin'; node .\scripts\browser-focused-check.mjs
```

结果：

```text
Focused browser summary (admin): 4/4 checks passed; 0 issue(s); 0 warning(s).
```

实际覆盖：

- 13 个管理路由全部打开并显示对应标题。
- 统计卡片和至少两张 ECharts 画布像素非空。
- 类型管理通过 UI 完成新增、搜索、修改、删除。
- 用户角色对话框回填现有 USER 角色并保存。
- 评论通过 UI 完成隐藏、恢复、删除。

管理端 13 个路由均已实际打开。

### 6.3 数据库实验室批次

命令：

```powershell
$env:BROWSER_FOCUS='lab'; node .\scripts\browser-focused-check.mjs
```

结果：

```text
Focused browser summary (lab): 1/1 checks passed; 0 issue(s); 0 warning(s).
```

实际覆盖：

- 视图查询实际返回表格行。
- 收藏触发器使计数 `before -> before+1 -> before`。
- 厂商统计过程返回正确厂商。
- 动漫排行过程返回 6 行。
- 游标返回 8 家厂商。
- COMMIT、ROLLBACK、EXCEPTION 三模式均验证响应体标志和可见日志行数。

### 6.4 截图批次

命令：

```powershell
$env:BROWSER_FOCUS='screens'; node .\scripts\browser-focused-check.mjs
```

结果：

```text
Focused browser summary (screens): 4/4 checks passed; 0 issue(s); 0 warning(s).
```

重新生成：

- `docs/screenshots/01-home.png`
- `docs/screenshots/02-anime-list.png`
- `docs/screenshots/03-anime-detail.png`
- `docs/screenshots/04-ranking.png`
- `docs/screenshots/05-admin-dashboard.png`
- `docs/screenshots/06-database-lab.png`
- `docs/screenshots/07-mobile-home.png`
- `docs/screenshots/08-admin-anime-mobile.png`

人工视觉复核未发现全局横向溢出、明显按钮遮挡、图表空白或桌面布局破版。

## 7. 安全复测

| 检查项 | 结果 |
|---|---|
| 数据库应用账户 | PASS，仅目标库六项权限 |
| `.env` 跟踪 | PASS，未跟踪且历史中未出现 |
| `.env.example` | PASS，数据库账户使用通用占位名，密码仅为 `change_me` |
| 密码哈希 | PASS，`admin` 和 `sakura` 均为 60 字符 `$2b$10$` 哈希 |
| JWT 签名、过期、issuer 和 type | PASS，代码与 API 测试一致 |
| 无令牌管理接口 | HTTP 401 |
| 普通用户管理接口 | HTTP 403 |
| 普通用户实验室 | HTTP 403 |
| SQL 参数化 | PASS，常规查询使用 `#{}` 或 `CallableStatement` |
| 唯一动态视图名 | PASS，经四视图白名单 |
| `v-html` | 0 命中 |
| 操作日志 | PASS |

## 8. 数据清理复测

本轮浏览器和 API 复测后再次查询：

```text
apitest_users=0
ui_users=0
ui_anime_titles=0
db_test_titles=0
review_test_content=0
transaction_exception_logs=0
```

保留的 `LAB_TRIGGER_FAVORITE` 和 `TRANSACTION_DEMO_COMMIT` 日志属于课程演示审计记录，不是临时业务数据。临时收藏、类型、评论和注册账号均已恢复或删除。

## 9. 复测结论

- 数据库测试：20/20 PASS。
- API 测试：27/27 PASS。
- 后端测试：17/17 PASS，构建成功。
- 前端单元测试：4/4 PASS，构建成功。
- 浏览器专项：四批合计 11/11 PASS，0 issue，0 warning。
- DOCX：主报告 30 页、AI 报告 12 页，逐页渲染检查 PASS。
- MySQL 物理检查：17/17 表 OK。
- 临时数据：指定测试模式残留为 0。

整改后未发现新的 P0 或 P1 缺陷。最终评分与是否建议提交见 `final-score-report.md`。

## 10. 最终 ZIP 复测

执行：

```powershell
.\scripts\package-release.ps1
```

实际结果：

- 打包成功，共 313 个文件。
- ZIP 自动解压成功。
- 关键入口文件全部存在。
- `.env`、`node_modules`、`target`、`.git`、`frontend/dist`、`qa-output` 等禁止项均未出现。
- 最终报告定稿后会再次执行同一打包脚本，最终 SHA256 以最后一次命令输出为准。
