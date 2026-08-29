# US-S2-SESSION-03 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-SESSION-03` 员工主动退出 |
| 验收 Task | `TASK-S2-SESSION-03-03` |
| 验收日期 | 2026-08-28 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite |
| 结论 | 通过；M3 会话保持与退出模块可独立交付 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 全局账号区展示员工显示名称、角色名称和退出入口 | `TC-S2-FE-SESS-007`；账号区显示“值班收银员”“收银员”和可访问按钮 | 通过 |
| 退出使用带跨站请求防护的非 GET 操作 | `POST /api/v1/auth/logout`；前端先取 CSRF Token；错误 Token 返回 `AUTH-403-002` 且不撤销 | 通过 |
| 成功后服务端撤销当前会话、清 Cookie，前端清除身份并进入登录页 | `TC-S2-SESS-009`、`TC-S2-FE-SESS-007` | 通过 |
| 后退、刷新或旧 Cookie 重放不能恢复访问 | 前端后退测试不渲染业务内容；服务端旧 Cookie `/auth/me` 返回 401；刷新由启动身份检查再次拒绝 | 通过 |
| 重复退出幂等，不恢复会话或返回 500 | `TC-S2-SESS-010`、`012` | 通过 |
| 同一账号并发会话相互独立 | `TC-S2-SESS-013`；退出第一会话后第二会话仍可查询身份 | 通过 |
| 后端不可达时不假装成功，保留身份并允许重试 | `TC-S2-FE-SESS-008`；请求处理中防重复，失败后提示并重试成功 | 通过 |
| 退出响应和日志不包含会话 ID 或 Cookie | 接口响应对原始 Token、摘要、Cookie 和会话字段执行负断言；服务只记录清理数量，不记录凭证 | 通过 |

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run check
typecheck、lint、format:check 通过
Test Files: 12 passed；Tests: 67 passed
生产构建成功
```

退出只撤销请求携带 Cookie 对应的当前会话，符合 Sprint 2 允许并发会话的冻结决定。错误 CSRF、网络失败和重复提交均不会误清理仍有效的前端身份。

## 后续交付门

- `SESSION-01-04`、`SESSION-02-03`、`SESSION-03-03` 均为 `Done`，M3 达到独立交付门。
- M4 与 M5 后续任务均已完成并通过验收，当前状态为 `Done`。
