# US-S2-AUTH-02 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-AUTH-02` 安全处理登录失败 |
| 验收 Task | `TASK-S2-AUTH-02-04` |
| 验收日期 | 2026-08-27 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite |
| 结论 | 通过；M2 安全登录模块可独立交付，SESSION-01 已解锁 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 不存在账号、错误密码和停用账号不建立会话，使用相同 HTTP、业务码和消息 | `TC-S2-AUTH-006` 至 `008`；三类均为 `401 / AUTH-401-002` | 通过 |
| 统一提示不暴露账号存在或停用状态 | 接口响应字段和内容负断言 | 通过 |
| 前端保留账号、清空并聚焦密码 | `TC-S2-FE-AUTH-007` | 通过 |
| 网络、超时、服务异常与凭证错误使用独立提示且可重试 | `TC-S2-FE-AUTH-009` | 通过 |
| 同一来源与规范化账号组合前 4 次失败返回 401 | `TC-S2-RATE-001` | 通过 |
| 第 5 次失败及限制期间返回 `429 / AUTH-429-001` 和整数秒 `Retry-After` | `TC-S2-RATE-002`、`003`；接口断言 | 通过 |
| 限制期间尝试不延长截止时间，15 分钟后自动恢复 | 可控时钟 `TC-S2-RATE-003`、`004` | 通过 |
| 不同来源或账号标识相互隔离，客户端伪造转发头不能绕过 | `TC-S2-RATE-005`；接口使用变化的 `X-Forwarded-For` 仍在第 5 次受限 | 通过 |
| 成功登录清除对应组合的失败状态 | `TC-S2-RATE-006` 单元与接口验证 | 通过 |
| 401、429、500 使用统一响应和 `traceId`，不含敏感认证信息 | `AuthLoginIntegrationTest`、`GlobalExceptionHandlerTest` | 通过 |
| 前端 429 显示恢复提示并按服务器时限暂停提交 | `TC-S2-FE-AUTH-008`；请求层解析 `Retry-After` | 通过 |

限流状态为当前应用进程内状态；冻结契约允许服务重启后不持久化，但运行期间的阈值、隔离、清除和恢复行为保持一致。限流事件的结构化安全审计属于后续 `US-S2-AUDIT-01`。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run check
typecheck、lint、format:check 通过
Test Files: 12 passed；Tests: 57 passed
生产构建成功
```

测试使用专用测试凭证和保留地址段；错误响应及日志检查未发现密码、摘要、Cookie、会话标识、异常载荷或堆栈。`git diff --check` 通过后，本 Story 无阻断缺陷。

## 后续交付门

- `TASK-S2-SESSION-01-01` 现为 `Ready`，负责会话读取、校验、刷新、撤销和 `/auth/me`。
- M2 的 `SEC-01-03`、`AUTH-01-06`、`AUTH-02-04` 均已完成，安全登录模块达到独立交付门。
