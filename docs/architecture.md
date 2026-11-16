# 系统架构与模块边界

## 总体架构

```text
Vue 3 / TypeScript / Element Plus / Pinia / ECharts
                    |
              Axios REST API
                    |
Spring Boot 3 / Spring Security / JWT / MyBatis-Plus
                    |
          MySQL 8.0 InnoDB / utf8mb4
```

系统采用前后端分离架构。浏览器只访问 Spring Boot 暴露的 `/api/**` 接口；应用使用 `Chengzi@localhost` 最小权限账户连接 `anime_platform`。数据库管理员仅用于首次建库、账户授权和对象初始化。

## 后端分层

- `controller`：HTTP 接口、参数校验、权限入口。
- `service`：事务边界、业务规则和跨表编排。
- `mapper`：MyBatis-Plus 持久化及复杂 SQL。
- `entity`：数据库表映射。
- `dto` / `vo`：写入契约和展示模型。
- `security`：JWT 认证、RBAC 方法权限和当前用户上下文。
- `config`：OpenAPI、跨域、MyBatis、密码编码等配置。
- `common`：统一响应、异常处理、分页和审计日志。

## 前端路由域

- 公共域：登录、注册、首页、动漫、厂商、制作人员、排行榜。
- 用户域：收藏、观看历史、我的评论、个人中心。
- 管理域：内容主数据、用户与角色、评论、日志、统计、数据库实验室。

管理路由通过路由守卫和接口鉴权双重保护，不能仅依赖菜单隐藏。

## 数据库设计原则

- 全部业务表使用 InnoDB、utf8mb4 和显式主键。
- 多对多关系拆为关系表；复合主键表达天然唯一关系。
- 收藏、评论和观看记录通过外键与唯一约束避免重复业务记录。
- 聚合字段由触发器维护，应用侧只读；触发器处理重复事件、关系迁移和非法负数。
- 删除策略遵循业务含义：主数据限制删除，关系数据级联清理，历史操作日志保留。
- 视图承担固定统计和详情读取；存储过程承担多语句业务操作。
