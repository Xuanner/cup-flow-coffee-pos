# US-S2-SESSION-02 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-SESSION-02` 会话过期或账号停用后重新登录 |
| 验收 Task | `TASK-S2-SESSION-02-03` |
| 验收日期 | 2026-08-28 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Node.js 24、Vitest、Vite |
| 结论 | 通过；SESSION-03 顺序执行门已解锁并完成 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 空闲 30 分钟或绝对 8 小时到达边界后立即失效 | `TC-S2-SESS-004` 至 `007`；固定时钟验证边界前有效、边界时失效 | 通过 |
| 失效请求返回统一 401、撤销会话并清 Cookie，不继续读取身份数据 | `CurrentSessionServiceTest` 验证在时限失败后不读取账号；`AuthLoginIntegrationTest` 验证 `AUTH-401-001` 和清 Cookie | 通过 |
| 登录后账号停用使下一次会话校验失效且不泄露状态 | `TC-S2-SESS-008`；撤销原因为内部 `ACCOUNT_DISABLED`，响应无账号状态或会话标识 | 通过 |
| 失效记录保留 7 天后清理，清理失败不改变即时判断 | `TC-S2-SESS-016`；Repository 数据库测试与清理服务故障隔离测试 | 通过 |
| 前端清除内存身份并进入登录页，只显示一次过期提示 | `TC-S2-FE-SESS-004`、`005`；两个并发 401 只产生一个提示和一次状态转换 | 通过 |
| 保存并在重新登录时重新校验安全站内目标 | 并发失效测试保存 `/orders?status=open`；既有 `safeReturnPath` 和登录回跳测试再次按角色校验 | 通过 |
| 页面不承诺保留未提交内容 | 过期提示明确显示“未提交的内容可能不会保留” | 通过 |
| 网络或服务异常不显示为密码错误，并可重试 | `TC-S2-FE-SESS-006` | 通过 |

当前 Sprint 尚无已实现的订单写接口；“不读取或修改业务数据”由会话服务在超时判断后不查询账号的单元断言，以及 `/auth/me` 在身份返回前拒绝的接口断言证明。后续 `US-S2-AUTHZ-03` 将把同一会话校验接入全部业务 API，并补充服务/数据库零调用权限测试。

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

最终错误响应、Cookie 清理和日志抽查未发现密码、摘要、原始 Session/CSRF Token、Cookie 值或账号停用状态泄露。

## 后续交付门

- `TASK-S2-SESSION-03-01` 至 `03` 已按顺序完成。
- 会话失效机制将在 `US-S2-AUTHZ-03` 接入全部业务接口授权边界。
