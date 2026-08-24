# Cup Flow Coffee POS Agent Guide

本文件用于指导在本仓库中工作的编码代理。修改代码前先阅读相关 PRD、用户故事、模块 README
和现有实现；遵循现有架构与命名，不擅自扩大需求范围。

## 项目概览

Cup Flow Coffee POS 是面向单品牌、单门店的简体中文 Web POS。

```text
.
├── frontend/cup-flow-coffee-pos-web/  # React + TypeScript 前端
├── backend/cup-flow-coffee-pos-java/  # Spring Boot 后端与 PostgreSQL
├── doc/                               # PRD、用户故事、设计和工程文档
└── .github/workflows/ci.yml           # GitHub Actions 持续集成
```

开始工作前优先阅读：

- `README.md`：环境、启动、质量门禁和故障排查。
- `doc/sprint2/PRD.md`、`doc/sprint2/FEATURES.md` 与 `doc/sprint2/USER_STORIES.md`：已批准的
  Sprint 2 登录体系需求、Feature 和验收标准。
- `doc/sprint2/API_CONTRACT.md`、`doc/sprint2/TEST_STRATEGY.md` 与 `doc/sprint2/TASKS.md`：认证
  契约、测试追踪、任务依赖和 Ready/Done 标准。
- `doc/sprint1/PRD.md` 与 `doc/sprint1/USER_STORIES.md`：工程基础需求和已交付基线。
- `doc/sprint1/TEST_STRATEGY.md`：测试分层、稳定编号和验收追踪。
- `backend/cup-flow-coffee-pos-java/docs/backend-conventions.md`：后端、数据和日志约定。
- 前后端子项目中的 `README.md`。

## 固定技术版本

- Node.js：`>=24.19.0 <25`。
- npm：`>=11.17.0 <12`，以 `package.json#packageManager` 为准。
- JDK：`25.x`。
- Maven：只使用仓库中的 `./mvnw`。
- PostgreSQL：`18.4`，本地通过 Docker Compose 启动。

不要更换包管理器，不要生成 pnpm、Yarn 或其他 lockfile。依赖升级必须使用精确版本，并同步提交
`package-lock.json` 或对应 Maven 配置。

## 工作原则

1. 先确认 PRD、所属 Feature、User Story 及验收标准，再拆分 Task 和修改代码。
2. 保留用户已有变更；不要覆盖、回退或格式化无关文件。
3. 优先复用现有组件、Token、请求封装、错误模型和测试工具。
4. 修改范围保持最小，避免顺手重构不相关模块。
5. 行为变更必须增加或更新相应层级的测试。
6. 完成后执行与风险相匹配的检查，并报告实际结果和未验证项。
7. 不自动执行提交、推送、合并、发布、删除数据卷或其他不可逆操作。

## 需求拆解与交付流程

PRD 是产品需求基线，保留产品目标、用户、范围、业务规则和成功指标；Feature 是从 PRD 中
提炼出的、用户可感知的业务能力。

统一使用以下拆解关系：

```text
PRD
  ↓
Feature
  ↓
User Story
  ↓
Acceptance Criteria
  ↓
Task
  ↓
开发、测试与代码评审
  ↓
按 User Story 验收的可运行产品增量
```

各层级职责：

- PRD：回答为什么建设、服务谁、范围和业务规则是什么，以及如何衡量成功。
- Feature：描述产品需要提供的完整业务能力，可以包含多个 User Story，也可以跨 Sprint 完成。
- User Story：从具体角色出发描述一项可独立验收的用户价值，必须引用所属 Feature 和 PRD 需求。
- Acceptance Criteria：定义 User Story 的可观察完成条件，必须清晰、可测试且覆盖关键异常路径。
- Task：实现 Story 所需的前端、后端、数据库、设计、测试或文档工作，不单独代表用户价值。

执行规则：

1. 需求进入开发前，先确认 PRD 和 Feature，再将 Feature 拆成足够小的 User Story。
2. User Story 必须先具备验收标准，再拆分 Task；不要用技术 Task 代替 User Story。
3. Task 应引用对应 User Story；代码、测试和文档继续使用稳定编号建立追踪关系。
4. 优先完成少量端到端 Story，避免同时开始大量只有部分 Task 完成的工作。
5. Task 全部完成不代表 User Story 自动完成；只有全部验收标准通过，Story 才能标记完成。
6. Feature 只有在所属必要 Story 均完成、整体业务能力可用后才能标记完成。
7. 需求变更先更新 PRD、Feature 或 Story 的对应规则和验收标准，再同步 Task、测试与实现。

推荐的工作闭环：

```text
需求梳理 → Backlog Refinement → Sprint Planning → Task 实现
→ 自动测试与 CI → 代码评审 → User Story 验收 → 交付产品增量
→ 收集反馈 → 调整 Backlog 或 PRD
```

## 前端约定

工作目录：`frontend/cup-flow-coffee-pos-web/`。

- 使用 React、TypeScript、React Router、TanStack Query、Zustand、Tailwind CSS 和 Radix UI。
- 路由和 Provider 放在 `src/app/`；业务能力放在 `src/features/`。
- 通用无业务组件放在 `src/components/ui/`，不得耦合页面业务规则。
- 服务端状态使用 TanStack Query；跨页面客户端状态使用 Zustand；局部交互使用 React state；
  分页、筛选和日期等可分享状态使用 URL。
- API 基础地址来自环境配置；业务代码不得硬编码生产地址。
- 颜色、字体、间距和圆角使用 `src/styles/tokens.css` 中的语义 Token。
- 保留 Radix UI 的键盘导航、焦点管理和 ARIA 行为。
- 用户提示不得展示内部堆栈、原始异常或敏感信息。

常用命令：

```bash
npm ci
npm run dev
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
npm run check
```

前端改动交付前通常执行 `npm run check`。只修改单一测试模块时可先运行针对性测试，但最终应按
影响范围决定是否执行完整门禁。

## 后端约定

工作目录：`backend/cup-flow-coffee-pos-java/`。

- 业务 API 前缀固定为 `/api/v1`。
- 使用模块化单体和轻量 DDD；业务模块按需遵循
  `api → application → domain ← infrastructure`。
- Controller 不直接访问 Mapper；事务边界放在应用服务。
- 领域代码不得依赖 Spring、MyBatis 或持久化框架。
- 人民币金额以整数分存储和计算，不使用浮点数。
- 数据库时间使用 `TIMESTAMPTZ`，系统时刻按 UTC 表达，门店自然日按 `Asia/Shanghai` 计算。
- 已执行 Flyway 迁移禁止修改；结构或基础数据变化必须新增迁移文件。
- 订单创建、收款和关键状态变化需要考虑幂等、唯一约束和并发冲突。
- API 响应保持 `code`、`message`、`data`、`traceId`、`timestamp` 结构。

常用命令：

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw spotless:check
./mvnw spotless:apply
./mvnw clean verify
```

后端接口和数据库测试使用 Testcontainers，需要 Docker Engine 正常运行。

## 测试与追踪

- 前端组件测试使用 Vitest 和 Testing Library。
- 后端单元、接口及数据库测试使用 JUnit、MockMvc 和 Testcontainers。
- 测试应放在能最早发现错误的最低合理层级，E2E 只覆盖关键跨端旅程。
- 金额、权限、订单状态机、幂等和看板口径属于 P0 自动化重点。
- 使用稳定编号关联需求和证据：
  - PRD：如 `BE-05`、`FE-07`。
  - 用户故事：如 `US-S1-ENG-03`。
  - 开发任务：如 `TASK-S1-ENG-03-01`。
  - 测试用例：如 `TC-S1-HEALTH-001`。
- 新增需求测试时，在测试显示名称或说明中加入对应 `TC-*`，并更新
  `doc/sprint1/TEST_STRATEGY.md` 的追踪矩阵。

## 数据库与本地服务

从后端目录启动数据库：

```bash
docker compose up -d postgres
docker compose ps
```

保留数据停止服务：

```bash
docker compose stop postgres
```

`docker compose down --volumes` 会删除本项目本地数据库数据。除非用户明确要求并确认目标，否则
不得执行。

## 安全要求

- 不提交或输出密码、密码哈希、Token、Cookie、数据库凭证和完整支付敏感信息。
- 不提交 `.env`、`.env.local`、个人地址、IDE 配置、构建产物或依赖目录。
- 日志使用 `traceId` 定位问题，未知异常只在服务端保留必要堆栈。
- 健康检查不得暴露数据库地址、账户、迁移 SQL 或内部组件详情。
- CI 使用最小权限；不得增加打印 Secret 或完整环境变量的调试步骤。

## Git 与 CI

- 默认从功能分支开发，通过 Pull Request 合并到 `main`。
- 不修改无关提交历史，不使用破坏性 reset，不强制推送，除非用户明确授权。
- `.github/workflows/ci.yml` 在 Pull Request、推送到 `main` 或手动触发时运行。
- CI 中的 `Frontend` 和 `Backend` 检查均应通过后再合并。
- 提交信息应描述实际变化，并引用相关用户故事或任务编号。

## 完成标准

交付前确认：

- 实现满足用户故事的每条验收标准。
- 新增或变更行为有自动化测试或明确的人工验证记录。
- 格式、静态检查、测试和构建按影响范围执行成功。
- 没有提交敏感信息、临时文件、依赖目录或构建产物。
- 文档、命令、测试编号和实际实现保持一致。
- 最终说明列出修改文件、验证结果以及仍需人工处理的事项。
