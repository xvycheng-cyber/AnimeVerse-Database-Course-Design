# 星漫云最终整改报告

> **历史材料：已被本轮第三方整改报告取代。** 本文仍引用旧的 `17/17`、`22/22`、`19/19` 等阶段结果；当前整改与复测结果见 `third-party-correction-report.md` 和 `third-party-retest-report.md`。

整改日期：2026-11-16  
整改范围：数据库一致性、后端安全与审计、Vue 交互契约、自动化测试工具、运行资源和文档计数。

## 1. 整改方法

最终独立审查发现问题后，按以下顺序处理：

1. 用实时数据库、接口响应和浏览器行为复现问题。
2. 对照 SQL、Mapper、DTO、Service、前端表单和测试假设定位根因。
3. 只在必要的结构、契约和测试边界内修正。
4. 执行针对性回归，再执行数据库、后端、前端、API 和浏览器全量复测。
5. 把实测计数同步到主报告、AI 报告、部署手册和测试账号说明。

没有执行 `DROP DATABASE`，没有修改 MySQL 全局配置，没有删除用户现有数据库或其他项目文件，没有把本地 `.env` 放入交付包。

## 2. 数据库一致性与高级对象整改

### 2.1 聚合字段只读

问题：动漫保存 DTO 和后台表单曾可以提交 `rating`、`averageRating`、`episodeCount` 等触发器派生值。

风险：应用写入可能覆盖触发器维护结果，导致评分、评论数、收藏数或集数不一致。

整改：

- 从 `AnimeSaveRequest` 移除聚合字段。
- 从管理员动漫新增/编辑表单移除评分、集数等可编辑控件。
- 列表和详情继续读取 `averageRating` 等只读展示字段。
- 浏览器回归断言编辑弹窗不存在“评分”“集数”输入。

复测：管理端评分列显示 `averageRating`，聚合字段 `editableAggregates=none`。

### 2.2 评论平均值重算

问题：评论 INSERT/UPDATE/DELETE 的旧逻辑基于历史平均值做增量调整，多次改分可能产生舍入漂移，隐藏和恢复也需要明确处理。

整改：

- 触发器从 `status=1` 的可见评论重新计算平均分、评分人数和评论数。
- 评论从一部作品迁移到另一部作品时，同时重算旧作品和新作品。
- 隐藏评论立即从平均分中排除，恢复后重新计入。

复测：重复改分、隐藏、恢复、删除和迁移均通过数据库测试。

### 2.3 关系迁移触发器

问题：收藏、观看历史和剧集只处理 INSERT/DELETE，关系键发生 UPDATE 时旧作品和新作品计数可能错误。

整改：新增以下 UPDATE 触发器：

- `trg_user_favorite_after_update`
- `trg_watch_history_after_update`
- `trg_anime_episode_after_update`

复测：旧作品计数减一、新作品计数加一，全部由数据库断言验证。

### 2.4 NULL 发行日期唯一性

问题：MySQL 普通唯一约束允许 `release_date IS NULL` 时出现多行同名作品。

整改：增加函数唯一索引：

```sql
uk_anime_title_release_key (
  title,
  (COALESCE(release_date, '1900-01-01'))
)
```

复测：同名且发行日期为 NULL 的重复插入返回 1062。

## 3. 安全与审计整改

### 3.1 无效 Bearer 令牌

问题：公开动漫接口携带无效/过期 JWT 时曾被安全链阻断。

整改：JWT 过滤器在令牌无效、用户不存在或账户不可用时清空安全上下文并继续过滤器链。公开接口保持匿名可访问，受保护接口由授权链返回 401。

复测：

- 无效 JWT + `/anime`：HTTP 200。
- 无效 JWT + `/user/profile`：HTTP 401。

### 3.2 停用账户

问题：停用账户登录的真实状态需要固定为 403，并验证账户可恢复。

整改与测试：API 测试以管理员停用 `sakura`，断言登录返回 403，然后立即恢复账户；`finally` 还会再次恢复，避免中断时残留停用状态。

### 3.3 实验室写操作审计

问题：实验室收藏、新增动漫和删除动漫属于管理写操作，需要统一操作日志。

整改：

- 收藏触发器实验增加 `@OperationLog`。
- `sp_add_anime` 实验接口增加 `@OperationLog`。
- 新增 `DELETE /api/lab/procedure/anime/{animeId}`，调用 `sp_delete_anime` 并增加 `@OperationLog`。
- 事务演示不重复加注解，因为 `sp_transaction_demo` 自身拥有审计和回滚语义。

复测：LAB 日志中出现 `LAB_TRIGGER_FAVORITE`、`LAB_PROCEDURE_ADD_ANIME`、`LAB_PROCEDURE_DELETE_ANIME`。

## 4. UI 与接口契约整改

### 4.1 角色管理

问题：角色表单曾发送与后端 `RoleSaveRequest` 不一致的字段；用户编辑时已有角色字符串没有映射回角色 ID。

整改：

- 表单统一发送 `roleCode`、`roleName` 和说明。
- 读取角色列表后把已有角色代码映射为角色 ID。
- 浏览器验证角色 CRUD 和用户角色未修改保存。

### 4.2 评论审核

问题：`status=0` 曾被前端误判为不可恢复状态。

整改：隐藏评论显示“已隐藏/恢复显示”，恢复后显示“显示中/隐藏”，删除流程保持可用。

复测：可见 → 隐藏 → 恢复 → 删除的完整生命周期通过。

### 4.3 收藏首次点击

问题：动漫卡未从 `anime.favorited` 初始化，第一次点击可能先新增而不是取消已有收藏。

整改：卡片以服务端 `favorited` 初始化状态，并正确调用新增/删除接口。

复测：已收藏卡片首次加载为 active，第一次点击直接调用 DELETE 并恢复非 active 状态。

### 4.4 其他字段与空状态

- 类型链接使用 `genreId`，不再错误地作为关键词搜索。
- 剧集页面不暴露 null `videoUrl`。
- 数据库实验室区分“成功但结果为空”和“尚未执行”。
- 认证视觉面板不再包含不可聚焦的伪链接。
- 管理动漫分数字段统一读取 `averageRating`。
- 增加真实 SVG favicon，消除本地 `/favicon.ico` 404。

## 5. 测试工具整改

### 5.1 PowerShell 5.1 UTF-8

问题：Windows PowerShell 5.1 在响应未显式携带 charset 时可能按本地代码页解析 JSON，导致中文标题和角色名比较失败。

整改：API 测试客户端读取原始响应字节并以 UTF-8 解码后再执行 `ConvertFrom-Json`。

### 5.2 HTTP 状态获取

问题：测试脚本使用 PowerShell 7 才支持的 `-SkipHttpErrorCheck`。

整改：统一改用 `Invoke-WebRequest -UseBasicParsing` 和 `try/catch`，在 PowerShell 5.1 下读取异常响应状态码。

### 5.3 预期数据库失败

问题：`$ErrorActionPreference='Stop'` 会把 mysql.exe 写入 stderr 的预期 1062/CHECK/SIGNAL 提前升级为终止错误。

整改：原生命令执行期间临时使用 `Continue` 收集输出，恢复后严格按 `$LASTEXITCODE` 判定。负向用例继续要求非零退出码和期望错误码，不把真实失败误判为通过。

### 5.4 控制台 404 规则

浏览器测试移除对本地/API 404 的笼统抑制。外部图片失败保留为警告，本地资源和 API 错误仍为失败。实际 favicon 404 通过增加资源解决，最终结果为 0 issue、0 warning。

## 6. 最终复测结果

| 测试 | 最终结果 |
|---|---:|
| 数据库测试 | 17/17 PASS |
| 后端 clean package | 17/17 PASS |
| Java 主源码编译 | 129 |
| 前端单元测试 | 4/4 PASS |
| TypeScript + Vite | 2361 模块，BUILD SUCCESS |
| API 联调 | 22/22 PASS |
| 浏览器回归 | 19/19 PASS |
| 浏览器 issue / warning | 0 / 0 |
| 运行截图 | 8 |

数据库实时对象复核：

```text
tables        17
views          4
triggers      12
procedures     6
functions      3
foreign_keys  20
checks        23
uniques       14
indexes       51
```

## 7. 整改结论

所有可在本地自主完成的审查问题均已修复并复测。当前保留的非阻断风险仅为远程封面网络依赖、Vite 大 chunk 警告、本地课程级安全加固边界和写过程事务组合验证边界。项目可进入最终打包和验收。

## 8. DOCX 交付版排版整改

最终 DOCX 依据 Markdown 源稿重新构建，并使用 Word 导出 PDF、Poppler 转 PNG 后逐页复核。主报告 30 页，AI 交互报告 12 页。

本轮完成的排版整改包括：

- 数字列表保留 Markdown 原始编号，不再受 Word 共享 `List Number` 状态影响。功能需求、业务规则和问题记录均从 1 开始连续编号。
- 表格行设置 `cantSplit`；不超过 9 行的表格继续把除末行外的单元格与下一行绑定，避免 2.4 数据边界表和 4.3 外键策略表出现表头或单行跨页。
- 7.5 事务结果与测试隔离说明压缩为一段，消除单独占页的事务续文。
- 5.4 建表代码在保持内容完整的前提下收束空行和约束行，避免两行代码溢出到近空白页。
- 390×3068 的移动端长截图按上、中、下切成三块，以三栏横向排列并统一保留图注，避免整图被压缩到不可读。

最终 PDF 和 PNG 复核确认：两份报告无空白页、无表头单独跨页、无图片遮挡、无文字溢出，编号和图表顺序一致。
