# US-S1-ENG-01 首次启动验证记录

本记录用于证明根目录说明可以在不依赖个人凭证和口头指导的环境中复现。验证人不得是本次待验
代码的原作者；后续影响工具版本、环境变量、端口或启动命令的变更应追加一次复验记录。

## 2026-08-19 独立复验

- 验证角色：独立工程验证（Codex，非前后端基础工程原作者）
- 操作系统：macOS
- Git：`2.39.3`
- Node.js：`24.19.0`
- npm：`11.17.0`
- JDK：Eclipse Temurin `25.0.4`
- Docker Engine：`28.5.1`
- Docker Compose：`2.40.2`
- 凭证：仅使用仓库内本地默认配置；未使用个人凭证、云端账号或私有 Token
- 干净环境措施：前端以 `npm ci` 重建依赖；后端以 `clean` 删除原构建输出；Compose 新建空数据卷，
  由 Flyway 从空 PostgreSQL 迁移到 V1

### 验证清单

| 检查项 | 命令或入口 | 结果 |
| --- | --- | --- |
| 前端可复现安装 | `npm ci` | 通过；按 lockfile 安装 347 个包 |
| 前端类型、Lint、格式、测试与构建 | `npm run check` | 通过；9 个测试文件、29 项测试全部通过，Vite 构建成功 |
| 后端格式、单元、接口、架构、迁移与构建 | `./mvnw clean verify` | 通过；10 项测试全部通过，Spotless 与 JAR 构建成功 |
| PostgreSQL 启动 | `docker compose up -d postgres`、`docker compose ps` | 通过；新建项目数据卷，PostgreSQL 18.4 状态为 `healthy` |
| 后端业务健康检查 | `GET http://localhost:8080/api/v1/health` | 通过；HTTP 2xx，`application` 与 `database` 均为 `UP` |
| Actuator 健康检查 | `GET http://localhost:8080/actuator/health` | 通过；HTTP 2xx，状态为 `UP` |
| 前端开发服务器 | `GET http://localhost:5173/` | 通过；HTTP 2xx，返回应用入口 HTML |
| 前端代理完整链路 | `GET http://localhost:5173/api/v1/health`（`/system` 使用同一路径） | 通过；经 Vite 代理返回应用与数据库 `UP` |

### 验证中发现并修正的问题

1. 仓库根目录缺少统一入口，新成员必须在前后端子目录间猜测启动顺序。已新增根目录
   `README.md`，明确数据库、后端、前端的顺序及每一步成功判据。
2. 原说明未集中列出 Git、Node、npm、JDK、Docker 与 Compose 版本要求。已在根目录建立工具矩阵
   和版本检查命令。
3. 原说明未在同一处覆盖完整质量门禁和跨端故障排查。已补充前后端检查命令、端口冲突、代理、
   Docker/Testcontainers、数据库连接和敏感信息处理说明。
4. 仅有 `java -version` 正确仍不足以保证 Maven Wrapper 启动；验证首次因 `JAVA_HOME` 未指向有效
   JDK 而中止。根目录说明已将 macOS 的 `JAVA_HOME` 设置放在启动前，并保留对应故障排查项。
5. Testcontainers 在受限执行环境中无法访问 Docker socket。切换到允许访问本机 Docker 的普通
   开发终端后复验通过；根目录说明已要求先执行 `docker info`，便于在构建前发现该问题。

### 结论

上表全部通过。根目录说明覆盖必要工具、支持版本、环境变量、数据库/后端/前端启动顺序、成功
判据、质量命令、停止清理和常见故障；全程只使用仓库内本地默认配置，不要求个人凭证。

`US-S1-ENG-01` 满足技术验收条件。
