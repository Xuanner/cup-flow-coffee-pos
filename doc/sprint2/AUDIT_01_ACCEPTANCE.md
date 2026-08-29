# US-S2-AUDIT-01 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-AUDIT-01` 追踪认证与权限事件 |
| 验收 Task | `TASK-S2-AUDIT-01-05` |
| 验收日期 | 2026-08-30 |
| 环境 | JDK 25、Spring Boot 4、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite、本地浏览器 |
| 结论 | 通过；M5 与 Sprint 2 登录体系可交付 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 登录成功/失败、退出、会话过期/失效、账号停用、限流、CSRF 和权限拒绝产生结构化事件 | `SecurityEventIntegrationTest` 覆盖 `TC-S2-AUDIT-001` 至 `008` | 通过 |
| 事件包含类型、结果、服务器时间、`traceId` 和最小上下文 | `SecurityEventRecorderTest` 检查稳定结构字段和 MDC `traceId` | 通过 |
| 仅在确认身份后记录稳定账号 ID，登录失败不泄露账号是否存在 | 登录成功记录 `accountId`；统一登录失败事件不记录账号标识或存在状态 | 通过 |
| 不记录密码、摘要、会话 ID、Cookie、完整请求头或完整认证请求体 | `SensitiveValueScannerTest`、`ZSensitiveArtifactScanTest` 覆盖日志、响应、报告和示例配置 | 通过 |
| 401、403、429 可通过 `traceId` 关联日志 | 登录限流、CSRF、权限拒绝与会话失效集成测试检查响应和事件关联 | 通过 |
| 可预期失败不打印堆栈，未知异常仅保留必要服务端堆栈 | 安全事件不携带异常；`GlobalExceptionHandlerTest` 验证未知异常响应脱敏和服务端安全堆栈 | 通过 |
| 自动化或日志抽查覆盖各类事件并检查敏感字段不存在 | `TC-S2-AUDIT-001` 至 `010` 全部通过 | 通过 |

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 92, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run check
Test Files: 12 passed
Tests: 76 passed
typecheck, lint, format check and production build passed
```

E2E 和人工安全复核见 [`EVID-S2-AUTH-20260830.md`](EVID-S2-AUTH-20260830.md)。未发现 P0/P1 阻断缺陷，验收材料未记录真实凭证。

## 11 条 Must Story 交付证据

| 顺序 | Story | 验收证据 | 结果 |
| --- | --- | --- | --- |
| 1 | `US-S2-ACCOUNT-01` | [`ACCOUNT_01_ACCEPTANCE.md`](ACCOUNT_01_ACCEPTANCE.md) | 通过 |
| 2 | `US-S2-SEC-01` | [`SEC_01_ACCEPTANCE.md`](SEC_01_ACCEPTANCE.md) | 通过 |
| 3 | `US-S2-AUTH-01` | [`AUTH_01_ACCEPTANCE.md`](AUTH_01_ACCEPTANCE.md) | 通过 |
| 4 | `US-S2-AUTH-02` | [`AUTH_02_ACCEPTANCE.md`](AUTH_02_ACCEPTANCE.md) | 通过 |
| 5 | `US-S2-SESSION-01` | [`SESSION_01_ACCEPTANCE.md`](SESSION_01_ACCEPTANCE.md) | 通过 |
| 6 | `US-S2-SESSION-02` | [`SESSION_02_ACCEPTANCE.md`](SESSION_02_ACCEPTANCE.md) | 通过 |
| 7 | `US-S2-SESSION-03` | [`SESSION_03_ACCEPTANCE.md`](SESSION_03_ACCEPTANCE.md) | 通过 |
| 8 | `US-S2-AUTHZ-01` | [`AUTHZ_01_ACCEPTANCE.md`](AUTHZ_01_ACCEPTANCE.md) | 通过 |
| 9 | `US-S2-AUTHZ-02` | [`AUTHZ_02_ACCEPTANCE.md`](AUTHZ_02_ACCEPTANCE.md) | 通过 |
| 10 | `US-S2-AUTHZ-03` | [`AUTHZ_03_ACCEPTANCE.md`](AUTHZ_03_ACCEPTANCE.md) | 通过 |
| 11 | `US-S2-AUDIT-01` | 本验收记录 | 通过 |

## Sprint 2 出口检查

- `TASK-S2-AUDIT-01-01` 至 `05` 均为 `Done`。
- M1 至 M5 的 11 条 Must Story 验收门均为 `Done`。
- 数据库迁移保持为可追踪的 V1/V2，未修改已执行迁移。
- 生产 Controller 的公开/认证/角色权限声明仍受架构测试约束，未增加未声明公开接口。
- 前后端全量质量门、E2E 与人工安全检查通过，无 P0/P1 阻断缺陷。
- Sprint 3 新业务接口按 [`SPRINT3_AUTHORIZATION_GUIDE.md`](SPRINT3_AUTHORIZATION_GUIDE.md) 接入鉴权。
