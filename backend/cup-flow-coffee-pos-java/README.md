# Cup Flow Coffee POS Java

Cup Flow Coffee POS 的后端基础工程，对应 Sprint 1 的 `BE-01` 至 `BE-07`、`DATA-01` 至
`DATA-05`。当前只包含工程骨架、统一 API 基础、健康检查、数据库迁移和测试，不包含真实登录、
商品、订单或收款业务。

## 技术基线

- Eclipse Temurin/OpenJDK `25`
- Spring Boot `4.0.7`
- Maven `3.9.16`（通过 Maven Wrapper `3.3.4`）
- Spring MVC、MyBatis `4.0.1`
- PostgreSQL `18.4`、Flyway
- JUnit、MockMvc、Testcontainers、ArchUnit

## 首次启动

要求 JDK 25、Docker Engine 和 Docker Compose 已可用，不需要全局安装 Maven 或 PostgreSQL。

macOS 当前终端需确保 `JAVA_HOME` 指向 JDK 25：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
java -version
```

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

默认地址：

- 业务健康检查：<http://localhost:8080/api/v1/health>
- Actuator 健康检查：<http://localhost:8080/actuator/health>

停止应用后，可保留数据停止数据库：

```bash
docker compose stop postgres
```

如需删除本项目的本地数据库数据卷，明确确认不再需要数据后再执行：

```bash
docker compose down --volumes
```

## 环境配置

应用使用环境变量覆盖本地安全默认值：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/cup_flow` | JDBC 地址 |
| `DB_USERNAME` | `cup_flow` | 本地数据库用户 |
| `DB_PASSWORD` | `cup_flow_local` | 仅限本地的示例密码 |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `AUTH_BOOTSTRAP_ENABLED` | `false` | 显式启用收银员和管理员账号初始化 |
| `AUTH_BOOTSTRAP_CASHIER_USERNAME` | 无 | 收银员账号，去首尾空白后 1–64 字符 |
| `AUTH_BOOTSTRAP_CASHIER_PASSWORD` | 无 | 仅由 Secret 注入，12–128 字符 |
| `AUTH_BOOTSTRAP_CASHIER_DISPLAY_NAME` | 无 | 收银员展示名，1–64 字符 |
| `AUTH_BOOTSTRAP_ADMIN_USERNAME` | 无 | 管理员账号，规则同上 |
| `AUTH_BOOTSTRAP_ADMIN_PASSWORD` | 无 | 仅由 Secret 注入，12–128 字符 |
| `AUTH_BOOTSTRAP_ADMIN_DISPLAY_NAME` | 无 | 管理员展示名，1–64 字符 |

`.env.example` 仅用于说明变量；Spring Boot 不会自动读取 `.env`。可由 IDE、Shell 或部署平台注入。
真实密码、Token 和个人地址不得提交。

账号初始化默认关闭。启用时必须同时提供两个账号的全部配置，应用会在启动阶段先完整校验配置，再在
单个事务内创建缺失账号并绑定 `CASHIER` 或 `ADMIN`。已有 username 会被完整保留，不修改密码、
状态、展示名或角色。配置缺失或无效时应用失败关闭，不创建部分账号或弱默认账号。初始化密码使用
带独立随机盐的 PBKDF2-HMAC-SHA256 摘要，环境变量中的明文不得写入仓库、日志或命令记录。

## 常用命令

```bash
./mvnw test              # 单元、接口、架构和 PostgreSQL 集成测试
./mvnw spotless:check    # 格式检查
./mvnw spotless:apply    # 自动格式化
./mvnw clean verify      # 完整质量门禁和可执行 JAR 构建
```

数据库测试通过 Testcontainers 启动独立 PostgreSQL 18.4，不依赖 Compose 数据库，但要求 Docker
Engine 正在运行。

## 模块边界

```text
com.cupflow.pos
├── auth        # 账号、角色、登录态；Sprint 2 开始实现
├── catalog     # 分类、商品、选项和售卖状态
├── ordering    # 订单聚合、计价快照和状态机
├── payment     # 收款、找零和幂等
├── reporting   # 看板查询模型
├── system      # 健康检查和运行诊断
└── shared      # API、错误、日志及少量通用值对象
```

业务模块内部按需要使用 `api → application → domain ← infrastructure`。领域代码不依赖 Spring、
MyBatis 或持久化注解，模块不得直接调用其他模块的 Mapper。ArchUnit 在测试中验证这些基础约束。

详细约定见 [`docs/backend-conventions.md`](docs/backend-conventions.md)。

## 数据迁移

Flyway 在应用启动时执行 `src/main/resources/db/migration`。已执行的迁移文件不得修改；任何结构或
基础数据变化都必须新增版本迁移。V1 只创建 Sprint 2 所需的账号、角色和账号角色关联，不创建默认
账号或任何真实密码。

## 故障排查

- `Connection refused`：确认 `docker compose ps` 中 PostgreSQL 为 `healthy`。
- `port is already allocated`：确认本机 `5432` 或 `8080` 未被其他程序占用。
- Testcontainers 无法启动：先运行 `docker info`，确认当前终端能连接 Docker Engine。
- `JAVA_HOME environment variable is not defined correctly`：按首次启动章节重新设置 `JAVA_HOME`。
- JDK 校验失败：运行 `java -version`，必须是 25.x；Maven Enforcer 会阻止错误版本构建。
