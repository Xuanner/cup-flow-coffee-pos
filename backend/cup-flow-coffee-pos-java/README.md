# Cup Flow Coffee POS Java

Cup Flow Coffee POS 后端目前包含统一 API、健康检查、账号初始化、安全登录、会话恢复与退出、
角色化访问控制、结构化认证安全事件和数据库迁移。商品、订单、收款与经营看板业务 API 将在后续
Sprint 接入现有鉴权边界。

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
| `AUTH_COOKIE_SECURE` | `false` | 仅本地 HTTP 例外；HTTPS 环境必须设为 `true` |
| `AUTH_ALLOWED_ORIGINS` | `http://localhost:5173` | 允许携带 Cookie 的显式 Origin；禁止 `*` |
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

## 认证、授权与安全事件

- `GET /api/v1/auth/csrf`：公开获取短期 CSRF Token。
- `POST /api/v1/auth/login`：校验 Origin 与 `X-XSRF-TOKEN`，成功设置 Host-only、HttpOnly、
  SameSite=Lax 会话 Cookie。
- `GET /api/v1/auth/me`：从有效会话恢复最小用户身份。
- `POST /api/v1/auth/logout`：受 CSRF 防护，撤销当前会话并清 Cookie；重复调用幂等。
- `GET /api/v1/health`：批准的公开业务健康检查。

所有 `/api/v1/**` Controller 方法必须显式使用 `@PublicEndpoint`、`@AuthenticatedEndpoint` 或
`@RequiresRole`。生产端点漏标会使 ArchUnit 门禁失败，运行时对已登录请求仍默认返回 403。
`ADMIN` 继承 `CASHIER` 权限；客户端 Header、查询参数或前端状态不能提供角色依据。

认证安全日志是 JSON 结构，事件字段包括 `securityEvent`、`outcome`、`eventTime`、`traceId`、可确认
身份时的 `accountId`、最小 `target` 和必要的非敏感 `reason`。禁止记录 username、密码、密码摘要、
Session/CSRF Token、Cookie、完整 Header 或认证请求体。401、403、429 应使用响应 `traceId` 关联日志。

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
- 登录或退出 403：检查 `AUTH_ALLOWED_ORIGINS` 和 `X-XSRF-TOKEN`，不要关闭 CSRF 过滤器排障。
- 401/403/429：使用响应 `traceId` 检索 `SecurityEventRecorder` 结构化事件；禁止在工单中附带 Cookie。
