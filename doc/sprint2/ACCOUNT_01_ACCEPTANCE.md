# US-S2-ACCOUNT-01 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-ACCOUNT-01` 可重复初始化最小账号集 |
| 验收 Task | `TASK-S2-ACCOUNT-01-04` |
| 验收日期 | 2026-08-27 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Flyway、Spring Boot |
| 结论 | 通过；M1 安全初始化账号可独立交付 |

## 验收范围说明

初始化模块负责安全、幂等地创建最小账号集，并确保停用账号不会被再次初始化恢复。停用账号通过登录接口时的统一失败反馈属于后续 `US-S2-AUTH-02`，由 `TC-S2-AUTH-008` 验收。这样保持 Story 依赖单向，不要求初始化 Story 依赖尚未实现的登录 Story。

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 启用初始化后存在一个 `ACTIVE` 收银员和一个 `ACTIVE` 管理员 | `TC-S2-DATA-005`，`AccountBootstrapIntegrationTest` | 通过 |
| 两个账号分别绑定 `CASHIER`、`ADMIN` | `TC-S2-DATA-005`，断言角色集合准确且无多余角色 | 通过 |
| 数据库只写入带随机盐的自描述密码摘要 | `TC-S2-DATA-005`、`Pbkdf2PasswordHasherTest`；PBKDF2-HMAC-SHA256，600,000 次迭代 | 通过 |
| 启用但 Secret 缺失时失败关闭且写库前终止 | `TC-S2-DATA-007` | 通过 |
| 重复执行不重复、不覆盖人工调整 | `TC-S2-DATA-003`、`006` | 通过 |
| 空库和 V1 升级可重复执行迁移 | `TC-S2-DATA-001` 至 `003`，`DatabaseMigrationTest` | 通过 |
| 停用账号不会被初始化重新激活或修复 | `TC-S2-DATA-008` | 通过 |
| 仓库、示例配置、日志与测试报告不包含可用初始凭证 | `.env.example` 默认关闭且凭证留空；测试报告敏感测试值扫描无命中 | 通过 |

管理员继承收银员业务权限是授权策略，不通过向初始化账号额外写入 `CASHIER` 角色实现；该行为继续由授权 Story 的权限矩阵测试验收。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

同时执行了测试报告敏感测试值扫描和 `git diff --check`，均通过。测试数据库为隔离的临时容器；验收不会向开发者本地 Compose 数据库写入共享账号。

## 遗留项

- `TC-S2-AUTH-008`：停用账号登录与不存在账号使用相同状态、业务码和消息；由 `US-S2-AUTH-02` 实现和验收，不阻塞 M1。
- 初始化账号的有效密码须由部署环境 Secret 渠道提供，验收记录不保存或展示该值。
