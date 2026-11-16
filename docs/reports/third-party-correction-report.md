# 星漫云第三方问题整改报告

整改日期：2026-11-16  
整改依据：`third-party-independent-review.md` 中的实际数据库、API、浏览器、安全和文档检查结果  
整改原则：只修复能够安全自动整改的问题；不删除现有数据库，不修改 MySQL 全局配置，不修改 Windows 防火墙，不伪造测试结果。

## 1. 整改摘要

| 级别 | 数量 | 结果 |
|---|---:|---|
| P0 | 0 | 无阻断问题 |
| P1 | 2 | 均已整改并复测 |
| P2 | 9 | 已整改代码、测试或文档 |
| P3 | 3 类 | 保留为生产化优化建议 |

本轮没有执行 `DROP DATABASE`、`DROP SCHEMA`、数据库重建、全局授权或破坏性安全测试。

## 2. P0 问题

未发现 P0 问题。当前项目可以连接数据库、启动后端和前端、执行测试并进入验收演示。

## 3. P1 整改

### 3.1 隐藏评论统计回算

问题：隐藏评论不应继续计入公开评论数、评分人数和平均分。若全量回算只按全部评论统计，会与触发器维护的可见评论统计不一致。

整改文件：

- `database/data/04_recalculate_counters.sql`
- `database/tests/04_recalculate_counter_tests.sql`

整改内容：

- 回算评论数、评分人数和平均分时只统计 `status = 1` 的可见评论。
- 增加隐藏评论回算测试，直接比较回算结果与可见明细来源。

复测证据：

- 数据库测试输出 `Counter recalculation excludes hidden reviews` 为 PASS。
- 最终数据库测试 20/20 通过。

### 3.2 动漫候选键与 NULL 日期一致性

问题：MySQL 普通 `UNIQUE(title, release_date)` 无法阻止多条“标题相同且日期均为 NULL”的记录；旧文档仍把 `(title, release_date)` 写成完整候选键。

整改文件：

- `database/schema/01_tables.sql`
- `database/tests/expected_duplicate_null_release_title.sql`
- `docs/database/conceptual-design.md`
- `docs/database/relational-schema.md`
- `docs/database/data-dictionary.md`
- `docs/reports/数据库课程设计报告.md`

整改内容：

- 不保留会误导的普通标题日期唯一约束。
- 使用函数唯一索引 `uk_anime_title_release_key(title, COALESCE(release_date, '1900-01-01'))`。
- 文档统一改为“标题 + 日期 NULL 归一化后的业务候选键”。
- 明确物理表存在受控派生聚合字段，不宣称完全无冗余。

复测证据：

- 同名且日期均为 NULL 的插入返回 MySQL 1062。
- 实库统计为 13 个 UNIQUE 约束、50 个统计索引。

## 4. P2 整改

### 4.1 动漫排序契约

问题：前端使用 `favorites`, `rating`, `latest`, `title` 等排序值，后端旧别名和实际字段契约存在不一致风险。

整改文件：

- `backend/src/main/java/com/animeverse/mapper/AnimeMapper.java`
- `backend/src/main/java/com/animeverse/service/AnimeService.java`
- `frontend/src/views/user/AnimeListView.vue`
- `scripts/test-api.ps1`
- `scripts/browser-focused-check.mjs`

整改内容：

- 兼容前端排序值和历史别名。
- 将 `popular` 映射为收藏热度，`score` 映射为评分，`title` 映射为标题升序。
- API 测试覆盖六种排序契约。
- 浏览器实际选择四种排序，并与 API 第一页首条结果比较。

复测证据：

- API 测试 `动漫列表前端排序值与兼容别名均返回数据库顺序` PASS。
- 用户浏览器批次四种排序 PASS。

### 4.2 注册用户名长度

问题：数据库用户名 CHECK 已收紧为 4 到 32 字符，前端注册页仍可能允许 3 字符输入，造成数据库拒绝而不是即时提示。

整改文件：

- `database/schema/01_tables.sql`
- `frontend/src/views/auth/RegisterView.vue`
- `database/tests/expected_invalid_username.sql`
- `scripts/test-api.ps1`
- `scripts/browser-focused-check.mjs`

整改内容：

- 前端规则与数据库 CHECK 统一为 4 到 32 字符。
- API 增加短用户名负向测试。
- 数据库增加短用户名 CHECK 违约测试。
- 浏览器验证空字段和 3 字符用户名被界面拒绝。

复测证据：

- 数据库负向测试返回 3819。
- API 短用户名返回 HTTP 400。
- 浏览器显示“用户名长度应为 4 至 32 个字符”并停留在注册页。

### 4.3 课程视图更新能力测试

问题：`information_schema.views.IS_UPDATABLE` 对 `v_anime_popularity` 单报 `YES`，不能只依据元数据宣称视图不可更新。

整改文件：

- `database/tests/expected_view_update.sql`
- `scripts/test-database.ps1`
- `docs/database/test-report.md`

整改内容：

- 对真实视图执行 UPDATE。
- 期望并验证 MySQL 错误 1288。
- 文档声明以实际执行结果为准。

复测证据：

```text
ERROR 1288 (HY000): The target table v_anime_popularity of the UPDATE is not updatable
```

### 4.4 数据库测试覆盖

整改文件：

- `scripts/test-database.ps1`
- `database/tests/04_recalculate_counter_tests.sql`
- `database/tests/expected_invalid_username.sql`
- `database/tests/expected_view_update.sql`

整改结果：

- 数据库测试由旧阶段 17 项扩展为当前 20 项。
- 当前实际汇总：

```text
Database test summary: 20/20 passed.
```

### 4.5 浏览器测试基础设施

问题：长浏览器脚本一次性执行全部业务时，浏览器进程被外部回收后可能产生 `TargetCloseError: Page closed!`。此外，`waitForResponse` 若在点击失败后未被等待，会在关闭阶段产生孤立 Promise，掩盖真实业务错误。

整改文件：

- `scripts/browser-focused-check.mjs`

整改内容：

- 拆分为 `user`, `admin`, `lab`, `screens` 四个短批次。
- 对响应等待器附加孤立拒绝处理。
- 关闭阶段容错并记录 warning。
- 增加进程级 `unhandledRejection` 记录，保证仍能写出报告。
- 每个批次独立生成 `qa-output/browser-<focus>-report.json`。
- screens 批次汇总用户、管理、实验室结果并更新 `docs/screenshots/browser-report.json`。

复测证据：

- user 2/2 PASS。
- admin 4/4 PASS。
- lab 1/1 PASS。
- screens 4/4 PASS。
- 合计 11/11，高层任务 0 issue、0 warning。

### 4.6 文档计数和测试结论

问题：旧文档保留 14 个唯一约束、51 个索引、17/17 数据库测试、22/22 API 测试、19/19 浏览器结果和 `98/100` 评分。

整改文件：

- `docs/database/conceptual-design.md`
- `docs/database/relational-schema.md`
- `docs/database/data-dictionary.md`
- `docs/database/test-plan.md`
- `docs/database/test-report.md`
- `docs/manuals/部署与运行手册.md`
- `docs/manuals/测试账号.md`
- `docs/reports/数据库课程设计报告.md`
- `docs/reports/AI交互报告.md`
- `docs/reports/final-independent-review.md`
- `docs/reports/final-correction-report.md`
- `docs/reports/final-acceptance-report.md`

整改内容：

- 当前计数统一为 13 个 UNIQUE、50 个索引。
- 当前数据库结果统一为 20/20。
- 当前 API 结果统一为 27/27。
- 浏览器结果统一为四个短批次共 11/11。
- 三份旧报告增加显著“历史材料，已被取代”声明，不再充当当前有效验收结论。
- 课程报告和 AI 报告补充受控反规范化、前端短用户名、排序契约、只读视图和隐藏评论回算。

### 4.7 交付清单

整改文件：

- `scripts/package-release.ps1`
- `README.md`
- `.env.example`
- `docs/screenshots/browser-report.json`
- `docs/screenshots/01-home.png` 至 `08-admin-anime-mobile.png`

整改内容：

- ZIP 必需文件增加四份第三方报告和当前浏览器报告。
- 打包脚本输出 ZIP 绝对路径、大小和 SHA256。
- README 增加当前验收材料入口。
- 8 张截图重新从真实运行页面生成。
- `.env.example` 的数据库账户改为通用 `animeverse_app`，不再沿用本机账户名。

### 4.8 DOCX 版面

问题：

- 两份 DOCX 的封面标题继承 Word `Title` 样式蓝色下边框，不符合无装饰横线的交付要求。
- 生成脚本把每个二级标题都设置为强制分页，造成正文页出现不必要的整页留白；部分大图在页尾无法容纳时也留下了过大空白。

整改文件：

- `scripts/build-report-docx.py`
- `docs/reports/数据库课程设计报告.docx`
- `docs/reports/AI交互报告.docx`

整改内容：

- 移除标题样式的段落边框。
- 仅对“摘要”和“第 X 章”章节标题分页，普通二级标题连续排版。
- 将正文图片最大高度收紧到 6.2 英寸，在保持可读性的同时减少页尾空白。

复测证据：

- 主报告由 31 页收紧为 30 页，AI 报告为 12 页。
- 使用 Word 实际导出 PDF，再由 Poppler 转为逐页 PNG 并完成人工视觉检查。
- 封面无标题横线，图表、图注和表格未发现裁切、重叠或错页。

### 4.9 环境模板

问题：`.env.example` 的 `DB_USER` 仍是本机账户名，和文档中“模板仅包含占位配置”的表述不一致。

整改文件：

- `.env.example`

整改内容：

- 将 `DB_USER` 改为通用账户名 `animeverse_app`。
- 实际 `.env`、当前测试账户和数据库授权不做改动。

### 4.10 最终交付包

- `scripts/package-release.ps1` 已执行成功。
- ZIP 包含 313 个文件，必需入口齐全。
- `.env`、`node_modules`、`target`、`.git`、`frontend/dist`、`qa-output` 等禁止项未进入压缩包。
- 脚本自动解压并检查关键文件，解压结果正常。

## 5. P3 未强制整改项

以下问题属于课程设计边界或生产化要求，不影响本地验收：

1. 封面依赖远程 URL，离线环境会显示备用图或加载失败状态。
2. Vite 输出 `index` 和 ECharts 大 chunk 警告，不影响构建成功和页面使用。
3. JWT 存储于 `localStorage`，生产环境更适合安全 Cookie 和 CSP 配套。
4. Swagger/OpenAPI 和登录接口公开可访问，生产环境应增加鉴权、流控、验证码或失败锁定。
5. 本地 MySQL 连接未启用 TLS，仅用于本机课程演示。
6. 本轮未执行高并发、死锁和长时间稳定性测试。

## 6. 整改结论

所有能够安全自动修复的 P0、P1、P2 问题已完成整改。未发现需要删除数据库、修改全局配置或进行破坏性测试才能解决的问题。当前剩余项目主要是 P3 生产化建议和非阻断环境限制。
