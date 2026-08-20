# Cup Flow Coffee POS

Cup Flow Coffee POS 是面向单品牌、单门店的简体中文 Web POS。

## 仓库结构

```text
.
├── frontend/cup-flow-coffee-pos-web/  # React + TypeScript 前端
├── backend/cup-flow-coffee-pos-java/  # Spring Boot 后端与 Compose 数据库
└── doc/                               # PRD、用户故事和设计交付
```

## 必要工具与支持版本

| 工具 | 支持版本 | 说明 |
| --- | --- | --- |
| Git | 2.x | 获取代码 |
| Node.js | `>=24.19.0 <25` | 前端运行时，版本记录在 `.nvmrc` 和 `.node-version` |
| npm | `>=11.17.0 <12` | 随 Node 安装；必须使用仓库内 `package-lock.json` |
| JDK | `25.x` | Eclipse Temurin 或兼容 OpenJDK |
| Docker Engine | 当前受支持稳定版 | 运行本地 PostgreSQL 和后端集成测试 |
| Docker Compose | v2，使用 `docker compose` | 随 Docker Desktop/Engine 插件提供 |

不需要全局安装 Maven 或 PostgreSQL：后端使用 Maven Wrapper，数据库由 Compose 启动。

开始前检查版本：

```bash
git --version
node --version
npm --version
java -version
docker --version
docker compose version
docker info
```

macOS 使用多个 JDK 时，可在当前终端选择 JDK 25：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
java -version
```

使用 nvm 时，可在前端目录执行 `nvm install`（首次）和 `nvm use`。其他 Node 版本管理器应读取
`.node-version` 中的版本。

## 环境变量

本地基础链路使用已提交的安全默认值，无需个人账号、云端凭证或私有 Token。

| 变量 | 本地默认值 | 使用方 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/cup_flow` | 后端 |
| `DB_USERNAME` | `cup_flow` | 后端 |
| `DB_PASSWORD` | `cup_flow_local` | 后端，仅本地示例凭证 |
| `SERVER_PORT` | `8080` | 后端 |
| `VITE_API_BASE_URL` | `/api/v1` | 前端；开发服务器代理到 `localhost:8080` |

示例分别位于
[`backend/cup-flow-coffee-pos-java/.env.example`](backend/cup-flow-coffee-pos-java/.env.example)
和
[`frontend/cup-flow-coffee-pos-web/.env.example`](frontend/cup-flow-coffee-pos-web/.env.example)。
Spring Boot 不会自动读取 `.env`；需要覆盖默认值时，请由 Shell 或 IDE 注入。前端通常无需创建
`.env.local`，需要覆盖时再从 `.env.example` 复制。不要提交真实密码、Token、个人地址或本地环境文件。

## 首次启动完整环境

以下命令均从仓库根目录开始。请按“数据库 → 后端 → 前端”的顺序启动，并分别保留后端和
前端终端。

### 1. 安装前端依赖

```bash
cd frontend/cup-flow-coffee-pos-web
npm ci
cd ../..
```

`npm ci` 严格使用 lockfile，适合首次启动和 CI；不要改用 `npm install` 生成不同依赖版本。

### 2. 启动并确认数据库

```bash
cd backend/cup-flow-coffee-pos-java
docker compose up -d postgres
docker compose ps
cd ../..
```

等待 `postgres` 状态变为 `healthy`。数据库监听 `localhost:5432`，首次启动时 Flyway 会由后端
自动执行版本化迁移。

### 3. 启动后端（终端 A）

```bash
cd backend/cup-flow-coffee-pos-java
./mvnw spring-boot:run
```

日志显示应用已启动后，在另一个终端验证：

```bash
curl --fail http://localhost:8080/api/v1/health
curl --fail http://localhost:8080/actuator/health
```

业务健康检查应返回 `code: "SUCCESS"`，且 `data.application` 与 `data.database` 均为 `UP`。

### 4. 启动前端（终端 B）

```bash
cd frontend/cup-flow-coffee-pos-web
npm run dev
```

访问：

- 应用：<http://localhost:5173/>
- 前后端联调状态：<http://localhost:5173/system>
- 基础组件预览：<http://localhost:5173/system/components>

`/system` 显示“后端服务运行正常”即表示前端、后端和数据库基础链路全部可用。

## 构建、测试与静态检查

前端完整质量门禁：

```bash
cd frontend/cup-flow-coffee-pos-web
npm ci
npm run check
```

也可分别执行 `npm run typecheck`、`npm run lint`、`npm run format:check`、`npm run test` 和
`npm run build`。`npm run check` 已按该顺序执行全部命令。

后端完整质量门禁：

```bash
cd backend/cup-flow-coffee-pos-java
./mvnw clean verify
```

也可分别执行 `./mvnw test` 和 `./mvnw spotless:check`。数据库集成测试使用 Testcontainers，
不依赖 Compose 中的数据库，但 Docker Engine 必须运行。

## 持续集成

GitHub Actions 工作流位于 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)，在以下事件触发：

- 所有 Pull Request 的创建与后续提交。
- 推送到 `main` 分支。
- GitHub Actions 页面手动触发。

前端和后端并行检查，每个阶段显示为独立步骤，失败时可直接定位到依赖安装、类型检查、Lint、
格式检查、测试或构建。前端先安装 `packageManager` 对应的固定 npm 版本，再用 `npm ci` 和已提交
的 `package-lock.json` 安装；后端始终使用已提交且固定 Maven 版本的 `mvnw`。Actions 缓存只保存
npm/Maven 下载缓存，不能跳过 lockfile、Wrapper 或完整检查。

工作流使用只读仓库权限，不注入业务环境变量或个人凭证，也不得新增打印密码、Token、数据库
密钥或完整敏感连接信息的步骤。GitHub 仓库应将 `Frontend` 和 `Backend` 检查设为 `main` 分支
合并前的必需状态检查。

## 停止与清理

前端和后端在各自终端按 `Ctrl+C` 停止。保留本地数据并停止 PostgreSQL：

```bash
cd backend/cup-flow-coffee-pos-java
docker compose stop postgres
```

只有在明确不再需要本项目本地数据时，才删除 Compose 容器及数据卷：

```bash
cd backend/cup-flow-coffee-pos-java
docker compose down --volumes
```

该命令会永久删除本项目 Compose 数据卷中的本地数据库数据。

## 常见故障排查

| 现象 | 处理方式 |
| --- | --- |
| `npm` 报 `EBADENGINE` | 检查 `node --version` 和 `npm --version`；切换到表中支持版本后重新执行 `npm ci`。 |
| `JAVA_HOME environment variable is not defined correctly` 或 Enforcer 拒绝构建 | 运行 `java -version`，确保当前终端使用 JDK 25；macOS 可执行上方 `JAVA_HOME` 命令。 |
| 后端报数据库 `Connection refused` | 运行 `docker compose ps`，等待 PostgreSQL 为 `healthy`；再检查 `DB_URL` 和 Docker 状态。 |
| Testcontainers 无法连接 Docker | 运行 `docker info`；启动 Docker Desktop/Engine，并确认当前用户可访问 Docker daemon。 |
| `5432`、`8080` 或 `5173` 端口被占用 | 停止占用进程；后端也可用 `SERVER_PORT` 覆盖，但同时要调整 Vite 代理配置。前端固定使用 `5173`。 |
| `/system` 显示网络失败 | 先直接访问后端健康检查；确认后端为 `8080` 且前端通过 `npm run dev` 启动，而非直接打开构建文件。 |
| 健康检查显示数据库异常 | 查看后端日志中的 `traceId` 和 `docker compose logs postgres`；不要在问题记录中粘贴密码或 Token。 |
| Maven/Node 下载依赖失败 | 检查网络、代理和公司镜像配置；恢复后重试 Wrapper 或 `npm ci`，不要手工提交依赖目录。 |

更深入的工程说明见[前端 README](frontend/cup-flow-coffee-pos-web/README.md)和
[后端 README](backend/cup-flow-coffee-pos-java/README.md)。首次启动的独立复验记录见
[`doc/sprint1/ENG-01-VALIDATION.md`](doc/sprint1/ENG-01-VALIDATION.md)。
