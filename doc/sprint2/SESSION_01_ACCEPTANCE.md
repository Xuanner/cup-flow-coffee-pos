# US-S2-SESSION-01 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-SESSION-01` 刷新后恢复登录态 |
| 验收 Task | `TASK-S2-SESSION-01-04` |
| 验收日期 | 2026-08-28 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite |
| 结论 | 通过；SESSION-01 可独立验收，SESSION-02 已解锁 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 应用启动先确认当前会话，再决定业务页或登录页 | `AuthProvider`、`SessionGate`；`TC-S2-FE-SESS-001` 至 `003` | 通过 |
| 会话确认期间显示加载状态，不闪现业务 Shell、菜单或内容 | `TC-S2-FE-SESS-001` | 通过 |
| 有效会话只返回账号 ID、显示名称、角色和默认页 | `TC-S2-SESS-001`；响应负断言不含密码、摘要、Cookie、Token 或会话字段 | 通过 |
| 刷新或同一浏览器会话的新应用实例恢复当前用户和有权目标地址 | `TC-S2-FE-SESS-002` 使用全新 Provider 状态恢复 `/orders?status=open` | 通过 |
| 会话凭证只由 HttpOnly Cookie 承载，不进入浏览器存储、URL 或 JavaScript 身份状态 | `SessionCookieFactoryTest`、`TC-S2-FE-AUTH-010`、`auth-api.test.ts` | 通过 |
| Cookie 的 SameSite、Secure 和跨站防护符合环境规则 | Host-only、`HttpOnly`、`SameSite=Lax`、`Path=/`、生产 `Secure`、本地非 Secure 由 `SessionCookieFactoryTest` 固化；`TC-S2-SESS-014`、`015` 验证 Origin + CSRF | 通过 |
| 未登录或无效会话查询当前身份返回 401，不创建会话 | `TC-S2-SESS-002`、`003`；伪造 Cookie 被清除且不泄露内部原因 | 通过 |
| 会话读取、刷新与撤销具备持久化和并发安全边界 | `MyBatisAuthSessionRepositoryTest` 验证摘要读取、撤销不可用和乱序刷新不回退 | 通过 |

Cookie 未设置 `Max-Age` 或 `Expires`，因此是浏览器会话 Cookie；前端不持久化当前用户或任何 Session/CSRF Token。运行中会话过期的单次提示、空闲/绝对超时边界及停用账号后的统一失效属于后续 `US-S2-SESSION-02`，本 Story 仅验收启动时恢复与无会话分流。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run check
typecheck、lint、format:check 通过
Test Files: 12 passed；Tests: 62 passed
生产构建成功
```

首次在受限沙箱执行后端门禁时，Docker socket 与 Mockito 动态代理附加被系统拒绝，导致测试环境初始化失败；使用允许访问 Testcontainers 的执行环境重跑同一命令后全部通过，业务断言无失败。最终响应、日志与测试报告抽查未发现密码、摘要、原始 Session/CSRF Token 或 Cookie 值。

## 后续交付门

- `TASK-S2-SESSION-02-01` 现为 `Ready`，负责空闲/绝对过期、账号停用和失效会话清理。
- `US-S2-SESSION-03` 与 `US-S2-AUTHZ-01` 的硬依赖已满足，但仍按 Sprint 2 顺序执行门等待 `SESSION-02-03`。
