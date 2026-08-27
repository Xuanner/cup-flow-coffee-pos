# US-S2-AUTH-01 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-AUTH-01` 员工使用账号密码登录 |
| 验收 Task | `TASK-S2-AUTH-01-06` |
| 验收日期 | 2026-08-27 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite |
| 结论 | 通过；账号密码登录成功路径可交付，AUTH-02 已解锁 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 登录页包含账号、密码、显隐和登录按钮，密码默认隐藏 | `TC-S2-FE-AUTH-001`、`002` | 通过 |
| 空字段不请求，显示字段错误并聚焦首错 | `TC-S2-FE-AUTH-003` | 通过 |
| 账号去首尾空白，密码保持原值 | `TC-S2-AUTH-003`、`004`；`auth-api.test.ts` | 通过 |
| CASHIER 进入 `/pos`，ADMIN 进入 `/dashboard` | `TC-S2-AUTH-001`、`002`；`TC-S2-FE-AUTH-005`、`006` | 通过 |
| 有权站内目标可恢复，外部、畸形、无权和未知目标被拒绝 | `safe-return-path.test.ts`、`AuthPage.test.tsx` | 通过 |
| 加载期间阻止同表单重复提交 | `TC-S2-FE-AUTH-004` | 通过 |
| 成功创建全新服务端会话并轮换旧标识 | `TC-S2-AUTH-001`、`009`；数据库只保存 SHA-256 摘要 | 通过 |
| Cookie 和 CSRF 符合冻结契约 | `TC-S2-AUTH-001`、`010`；HttpOnly、SameSite=Lax、Path=/、host-only、会话 Cookie；`Secure` 由环境配置 | 通过 |
| 响应、前端状态和浏览器持久化不包含会话凭证 | 最小 `CurrentUser` Zod 模型、非持久化 Zustand Store、响应字段负断言 | 通过 |
| 已登录访问 `/login` 进入默认有权页 | `TC-S2-FE-ROUTE-005` | 通过 |

会话刷新恢复、完整受保护路由守卫、失败限流和退出分别属于后续 `SESSION-01`、`AUTHZ-01`、`AUTH-02` 和 `SESSION-03`，本 Story 未越界实现。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run check
typecheck、lint、format:check 通过
Test Files: 12 passed；Tests: 49 passed
生产构建成功
```

前端额外检查确认登录请求使用 `credentials: same-origin`，CSRF Token 只在调用栈内传递；代码未使用 localStorage、sessionStorage 或 URL 保存身份凭证。后端响应只返回账号 ID、显示名、角色和默认路径，原始会话值只通过 HttpOnly Cookie 发送。

## 后续交付门

- `TASK-S2-AUTH-02-01` 现为 `Ready`，负责不存在账号、错误密码和停用账号的统一失败、限流及恢复。
- `SESSION-01` 将增加 `/auth/me` 与启动恢复；`AUTHZ-01` 将把安全返回地址接入完整受保护路由守卫。
