# US-S2-AUTHZ-03 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-AUTHZ-03` 后端独立执行接口权限 |
| 验收 Task | `TASK-S2-AUTHZ-03-04` |
| 验收日期 | 2026-08-28 |
| 环境 | JDK 25、Spring Boot 4、PostgreSQL 18.4 Testcontainers、MockMvc、ArchUnit |
| 结论 | 通过；M4 角色化访问控制模块可独立交付 |

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 业务接口默认要求有效会话，公开入口必须显式批准 | `TC-S2-AUTHZ-001`、`006`、`010`；仅 CSRF、登录、退出、当前身份和健康检查按契约显式声明 | 通过 |
| 未登录或无效会话返回 401，不执行业务 | `TC-S2-AUTHZ-001`、`008`；业务探针零调用，无效 Cookie 被清除 | 通过 |
| 已登录但角色不足返回 403，不执行业务 | `TC-S2-AUTHZ-004`、`009`；稳定码 `AUTH-403-001`，拒绝请求不增加探针调用 | 通过 |
| `CASHIER` 可访问收银权限，`ADMIN` 继承并可访问管理员权限 | `TC-S2-AUTHZ-002`、`003`、`005`；`RoleAuthorizationTest` 覆盖继承和多角色并集 | 通过 |
| 未声明的新业务端点默认拒绝 | `TC-S2-AUTHZ-006` 验证运行时 403；架构测试要求所有生产 Controller 端点显式声明 | 通过 |
| URL、客户端状态或伪造角色字段不能提升权限 | `TC-S2-AUTHZ-007`；查询参数和 `X-Role` 均被忽略，角色只来自服务端会话 | 通过 |
| 401/403 使用统一结构和 `traceId`，不泄露敏感上下文 | 权限接口测试断言稳定业务码、非空 `traceId`，响应不含 Token、Cookie、角色规则或所需角色 | 通过 |
| 请求身份上下文不会跨线程污染 | `CurrentUserContextTest` 验证并发线程隔离；拦截器在请求开始和结束时清理 | 通过 |

当前 Sprint 尚无真实 POS、订单、商品或看板业务 API。权限矩阵由仅存在于集成测试中的业务探针端点验证；未向生产 API 暴露占位业务接口。后续新增真实业务 Controller 时必须使用 `@RequiresRole`、`@AuthenticatedEndpoint` 或 `@PublicEndpoint`，否则架构门禁失败；即使绕过架构测试，运行时也默认拒绝。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

AuthorizationIntegrationTest: 7 passed
RoleAuthorizationTest + CurrentUserContextTest: 4 passed
ArchitectureTest: 3 passed
```

## 后续交付门

- `TASK-S2-AUTHZ-03-01` 至 `04` 均为 `Done`。
- `AUTHZ-01-02`、`AUTHZ-02-02`、`AUTHZ-03-04` 均为 `Done`，M4 达到独立交付门。
- `TASK-S2-AUDIT-01-01` 已解锁为 `Ready`。
