# Cup Flow Coffee POS — Sprint 2 开发 Task 拆分

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.7 |
| 上游基线 | Sprint 2 PRD、Features、API Contract v1.0；User Stories v1.1；Test Strategy v1.7 |
| 文档状态 | 已重新拆分；按 Story 依赖和模块验收门执行 |
| 门禁结果 | TASK-S2-PLAN-01 至 03、TASK-S2-ACCOUNT-01-01 至 04、TASK-S2-SEC-01-01 至 03、TASK-S2-AUTH-01-01 至 06、TASK-S2-AUTH-02-01 至 04、TASK-S2-SESSION-01-01 至 04、TASK-S2-SESSION-02-01 至 03、TASK-S2-SESSION-03-01 至 03 已完成 |

## 1. 拆分与执行规则

本文按两层顺序组织 Sprint 2 Task：

1. 第一层严格遵循 `USER_STORIES.md` 第 6 节的 Story 依赖与建议验收顺序。
2. 第二层按可交付模块组织：安全初始化账号、安全登录、会话保持与退出、角色化访问控制、安全事件与追踪。
3. 每个 Task 只指定一个“主 Story”；若产物被后续 Story 复用，只在“支持后续”中记录，不把多个 Story 混成一个交付单元。
4. 自动化测试跟随所属 Story 实现，不集中留到 Sprint 末尾。每个 Story 最后都有独立验收 Task。
5. 前一个 Story 的验收 Task `Done` 后，依赖它的 Story 才能进入 `Ready`；模块内全部 Story 通过后，模块才可交付。
6. Task 全部完成不代表 Story 自动完成；验收标准和关联 `TC-S2-*` 均有证据后，Story 验收 Task 才能标记 `Done`。

任务状态：`Draft`、`Blocked`、`Ready`、`In Progress`、`Review`、`Done`。复杂度仅用于相对比较：`S` 为局部、低风险；`M` 为跨多个类或组件；`L` 为跨层或高安全风险。单个 Task 应可在 1 至 2 个工作日内完成；超出时继续拆分。

## 2. 全局门禁（已完成）

| Task | 内容 | 状态 | 依赖 | 完成证据 |
| --- | --- | --- | --- | --- |
| TASK-S2-PLAN-01 | 冻结会话、过期时限、管理员默认页、限流、停用账号和初始化凭证渠道等产品与安全决策 | Done | 无 | PRD 第 15 节及 v1.0 需求基线 |
| TASK-S2-PLAN-02 | 冻结登录、当前身份、退出、CSRF、Cookie、错误码、返回地址与 `traceId` 契约 | Done | PLAN-01 | `API_CONTRACT.md` v1.0 |
| TASK-S2-PLAN-03 | 为全部验收标准分配 `TC-S2-*`，冻结测试层级与追踪矩阵 | Done | PLAN-01、PLAN-02 | `TEST_STRATEGY.md` v1.1 |

这些门禁是全部实现 Task 的共同前置条件，不再作为某个 Story 的交付 Task 重复列出。

## 3. Story 依赖与模块交付顺序

| 总顺序 | 交付模块 | User Story | `USER_STORIES.md` 依赖 | 顺序执行门 | 本 Story 验收门 |
| --- | --- | --- | --- | --- | --- |
| 1 | M1 安全初始化账号 | US-S2-ACCOUNT-01 | Sprint 1 数据基础 | 全局门禁 | TASK-S2-ACCOUNT-01-04 |
| 2 | M2 安全登录 | US-S2-SEC-01 | ACCOUNT-01 | ACCOUNT-01-04 | TASK-S2-SEC-01-03 |
| 3 | M2 安全登录 | US-S2-AUTH-01 | SEC-01 | SEC-01-03 | TASK-S2-AUTH-01-06 |
| 4 | M2 安全登录 | US-S2-AUTH-02 | AUTH-01 | AUTH-01-06 | TASK-S2-AUTH-02-04 |
| 5 | M3 会话保持与退出 | US-S2-SESSION-01 | AUTH-01 | AUTH-02-04 | TASK-S2-SESSION-01-04 |
| 6 | M3 会话保持与退出 | US-S2-SESSION-02 | SESSION-01 | SESSION-01-04 | TASK-S2-SESSION-02-03 |
| 7 | M3 会话保持与退出 | US-S2-SESSION-03 | SESSION-01 | SESSION-02-03 | TASK-S2-SESSION-03-03 |
| 8 | M4 角色化访问控制 | US-S2-AUTHZ-01 | SESSION-01 | SESSION-03-03 | TASK-S2-AUTHZ-01-02 |
| 9 | M4 角色化访问控制 | US-S2-AUTHZ-02 | AUTHZ-01 | AUTHZ-01-02 | TASK-S2-AUTHZ-02-02 |
| 10 | M4 角色化访问控制 | US-S2-AUTHZ-03 | SESSION-01 | AUTHZ-02-02 | TASK-S2-AUTHZ-03-04 |
| 11 | M5 安全事件与追踪 | US-S2-AUDIT-01 | 全部认证与授权 Story | AUTHZ-03-04 | TASK-S2-AUDIT-01-05 |

“`USER_STORIES.md` 依赖”表示产品/技术硬依赖；“顺序执行门”用于强制落实建议验收顺序。即使某个后续 Story 的硬依赖已经满足，也要等前序 Story 验收门 `Done` 后再领取。仅允许同一 Story 内在稳定契约下并行前后端工作。

## 4. M1 安全初始化账号

### 4.1 US-S2-ACCOUNT-01 可重复初始化最小账号集

**Story 依赖：** Sprint 1 数据基础。

**模块交付目标：** 新环境可通过受控配置幂等获得 `ACTIVE` 收银员和管理员账号；生产环境缺少 Secret 时失败关闭。

#### TASK-S2-ACCOUNT-01-01 新增认证数据迁移

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 数据库、后端 / Done / L |
| 主 Story | US-S2-ACCOUNT-01 |
| 支持后续 | US-S2-SESSION-01、US-S2-SESSION-02 |
| 依赖 | PLAN-01、PLAN-02 |

**工作内容：** 新增而不修改 V1 的版本化迁移；建立服务端会话结构、摘要字段、生命周期约束及查询/清理索引，不保存可直接使用的原始会话凭证。

**完成证据：** `V2__create_auth_sessions.sql`；`DatabaseMigrationTest` 中 `TC-S2-DATA-001` 至 `004`；后端质量门禁通过。

#### TASK-S2-ACCOUNT-01-02 实现账号与角色持久化适配器

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、数据库 / Done / M |
| 主 Story | US-S2-ACCOUNT-01 |
| 支持后续 | US-S2-AUTH-01、US-S2-AUTHZ-03 |
| 依赖 | ACCOUNT-01-01 |

**工作内容：** 实现按规范化账号查询账号与角色、创建初始化账号及绑定角色的 Repository 和 MyBatis 适配器；保持分层；查询只暴露认证所需字段，覆盖空结果、多角色、停用状态和唯一约束冲突。

**完成证据：** `AccountTest` 与 `MyBatisAccountRepositoryTest` 共 9 个针对性测试通过，覆盖账号规范化、空结果、多角色、停用状态、幂等与并发创建、角色幂等绑定、外键约束及密码摘要字符串脱敏；后端 `./mvnw clean verify` 共 22 个测试通过。

#### TASK-S2-ACCOUNT-01-03 实现环境化初始化账号

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、配置、安全 / Done / M |
| 主 Story | US-S2-ACCOUNT-01 |
| 依赖 | ACCOUNT-01-02 |

**工作内容：** 非生产验收环境创建一个收银员和一个管理员账号并关联正确角色；从批准渠道读取初始凭证并生成自适应算法摘要；初始化保持幂等且不覆盖已有调整；生产缺少必要 Secret 时失败关闭；示例配置只记录变量名和安全占位符。

**完成证据：** `AccountBootstrapIntegrationTest` 完成 `TC-S2-DATA-005` 至 `008`；停用账号实际登录失败及统一反馈由
`TC-S2-AUTH-008` 在 `AUTH-02` 验收。`BootstrapAccountTest`、
`AccountBootstrapServiceTest`、`Pbkdf2PasswordHasherTest` 与集成测试共 10 个针对性测试通过；配置、
日志和测试报告未输出测试凭证；后端 `./mvnw clean verify` 共 32 个测试通过。

#### TASK-S2-ACCOUNT-01-04 验收初始化账号 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-ACCOUNT-01 |
| 依赖 | ACCOUNT-01-01 至 03 |

**验收内容：** 对照全部验收标准复核空库、重复启动、角色、Secret 缺失、停用账号和敏感信息边界；登记 `TC-S2-DATA-001` 至 `008` 证据。

**完成证据：** [`ACCOUNT_01_ACCEPTANCE.md`](ACCOUNT_01_ACCEPTANCE.md) 已逐条登记验收标准、自动化用例和安全检查；`TC-S2-DATA-001` 至 `008` 全部通过，后端 `./mvnw clean verify` 共 32 个测试通过，无阻断缺陷。

**模块出口：** ACCOUNT-01-04 `Done` 后，M1 可独立交付，并解锁 US-S2-SEC-01。

## 5. M2 安全登录

### 5.1 US-S2-SEC-01 安全保存和校验密码

**Story 依赖：** ACCOUNT-01-04。

#### TASK-S2-SEC-01-01 实现密码哈希与安全校验组件

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、安全 / Done / M |
| 主 Story | US-S2-SEC-01 |
| 依赖 | ACCOUNT-01-04 |

**工作内容：** 封装摘要生成与验证；损坏摘要安全失败；未知账号执行等价验证路径；对象字符串、异常和调试输出不得暴露密码或摘要。

**完成证据：** `Pbkdf2PasswordHasherTest` 与 `AccountPasswordVerifierTest` 共 7 个测试通过；
`TC-S2-PASS-001` 至 `005` 覆盖正确、错误、随机盐、未知账号等价校验及损坏摘要安全失败。

#### TASK-S2-SEC-01-02 增加密码敏感信息防泄露测试

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端安全测试 / Done / S |
| 主 Story | US-S2-SEC-01 |
| 依赖 | SEC-01-01 |

**工作内容：** 使用专用假凭证检查异常、日志、测试报告和快照；断言不存在明文密码或密码摘要。

**完成证据：** `SensitiveValueScannerTest` 中 `TC-S2-AUDIT-009`、`010` 通过；植入假敏感值时扫描失败且不回显该值，正常脱敏产物和全量测试报告扫描无命中。

#### TASK-S2-SEC-01-03 验收密码安全 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-SEC-01 |
| 依赖 | SEC-01-01、SEC-01-02 |

**验收内容：** 复核数据库字段、随机盐效果、正确/错误/未知账号验证路径及敏感信息检查，逐条登记 US-S2-SEC-01 证据。

**完成证据：** [`SEC_01_ACCEPTANCE.md`](SEC_01_ACCEPTANCE.md) 已登记全部验收标准；针对性测试 9 个通过，后端 `./mvnw clean verify` 共 39 个测试通过，无阻断缺陷。

### 5.2 US-S2-AUTH-01 员工使用账号密码登录

**Story 依赖：** SEC-01-03。

#### TASK-S2-AUTH-01-01 实现登录应用服务

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端 / Done / L |
| 主 Story | US-S2-AUTH-01 |
| 依赖 | SEC-01-03、ACCOUNT-01-02 |

**工作内容：** 规范化账号但不修改密码；校验账号状态和密码；成功后轮换并建立新会话；返回最小用户信息；认证失败使用统一领域结果。

**完成证据：** `LoginService`、安全随机会话凭证和摘要持久化适配器已实现；`AuthLoginIntegrationTest` 覆盖两角色、输入规范化、旧会话轮换和响应脱敏。

#### TASK-S2-AUTH-01-02 暴露登录 API 与安全 Cookie

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端 API、安全 / Done / M |
| 主 Story | US-S2-AUTH-01 |
| 依赖 | AUTH-01-01、PLAN-02 |

**工作内容：** 实现登录端点、字段校验、统一响应和 `traceId`；按环境设置安全 Cookie；响应体不返回会话凭证；执行冻结的 CSRF 策略。

**完成证据：** `POST /api/v1/auth/login`、`GET /api/v1/auth/csrf`、同源校验及环境化 Cookie 已实现；`TC-S2-AUTH-001` 至 `005`、`009`、`010` 共 7 个接口场景通过。

#### TASK-S2-AUTH-01-03 建立前端认证 API 与数据模型

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端 / Done / M |
| 主 Story | US-S2-AUTH-01 |
| 支持后续 | US-S2-AUTH-02、US-S2-SESSION-01、US-S2-SESSION-03 |
| 依赖 | AUTH-01-02 |

**工作内容：** 基于现有请求封装实现登录调用和最小用户/角色模型；按契约携带 Cookie/CSRF；不在浏览器存储、URL 或持久状态中保存会话凭证。

**完成证据：** `auth-api.ts`、Zod 最小用户模型和 Zustand 非持久化身份状态已实现；API 单元测试覆盖 CSRF 顺序、同源 Cookie、账号规范化、密码原样提交、错误码映射和异常响应拒绝。

#### TASK-S2-AUTH-01-04 实现登录表单成功路径

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端 UI / Done / M |
| 主 Story | US-S2-AUTH-01 |
| 依赖 | AUTH-01-03 |

**工作内容：** 实现账号、密码、显示/隐藏密码、空字段校验、首错聚焦、加载和防重复提交；成功后按角色进入 `/pos` 或 `/dashboard`。

**完成证据：** `TC-S2-FE-AUTH-001` 至 `006` 通过，覆盖密码显隐、空值与首错聚焦、加载防重、两角色默认页和无障碍语义。

#### TASK-S2-AUTH-01-05 实现登录后的安全目标跳转

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端路由、安全 / Done / M |
| 主 Story | US-S2-AUTH-01 |
| 支持后续 | US-S2-AUTHZ-01 |
| 依赖 | AUTH-01-04 |

**工作内容：** 仅采用规范化站内相对目标；跳转前再次校验路由与角色；外部、畸形、无权或不存在目标进入角色默认页；已登录访问 `/login` 时进入默认有权页。

**完成证据：** 安全返回地址采用站内已知路由和角色白名单；外部、双斜杠、反斜杠、编码变体、敏感参数、无权和未知目标均回退默认页；`TC-S2-FE-ROUTE-005` 通过。

#### TASK-S2-AUTH-01-06 验收账号密码登录 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-AUTH-01 |
| 依赖 | AUTH-01-01 至 05 |

**验收内容：** 登记两角色登录、表单、重复提交、新会话、Cookie、默认页和安全返回地址证据，逐条验收 US-S2-AUTH-01。

**完成证据：** [`AUTH_01_ACCEPTANCE.md`](AUTH_01_ACCEPTANCE.md) 已登记全部验收标准和安全边界；后端 46 个、前端 49 个测试通过，前后端全量门禁成功，无阻断缺陷。

### 5.3 US-S2-AUTH-02 安全处理登录失败

**Story 依赖：** AUTH-01-06。

#### TASK-S2-AUTH-02-01 实现统一认证失败与频率限制

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、安全 / Done / L |
| 主 Story | US-S2-AUTH-02 |
| 依赖 | AUTH-01-06 |

**工作内容：** 三类认证失败使用相同状态、业务码和提示；按批准组合限流；成功清理失败状态；限制到期自动恢复；429 返回稳定业务码和 `Retry-After`，不暴露账号存在性。

**完成证据：** `LoginAttemptLimiter` 按直接来源 IP 与规范化账号组合执行 5 次/15 分钟策略，不信任客户端转发头；`TC-S2-AUTH-006` 至 `008`、`TC-S2-RATE-001` 至 `006` 全部通过。

#### TASK-S2-AUTH-02-02 实现前端登录失败恢复体验

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端 UI / Done / M |
| 主 Story | US-S2-AUTH-02 |
| 依赖 | AUTH-02-01、AUTH-01-04 |

**工作内容：** 认证失败保留账号、清空并聚焦密码；429 显示稍后重试；网络、超时和服务异常使用独立提示；恢复后允许重试。

**完成证据：** `TC-S2-FE-AUTH-007` 至 `009` 通过；认证失败保留账号、清空并聚焦密码，429 按 `Retry-After` 禁用后自动恢复，网络、超时和服务异常使用独立提示。

#### TASK-S2-AUTH-02-03 验证错误契约与脱敏边界

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端接口、安全测试 / Done / S |
| 主 Story | US-S2-AUTH-02 |
| 依赖 | AUTH-02-01 |

**工作内容：** 断言认证失败、429 和系统错误保持统一结构及 `traceId`；响应和日志不含堆栈、密码、摘要、Cookie 或会话标识。

**完成证据：** `AuthLoginIntegrationTest` 验证 401/429 统一结构、业务码、`traceId`、`Retry-After` 和敏感字段负断言；`GlobalExceptionHandlerTest` 验证 500 响应及日志不含异常载荷或堆栈。

#### TASK-S2-AUTH-02-04 验收安全登录失败 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-AUTH-02 |
| 依赖 | AUTH-02-01 至 03 |

**验收内容：** 登记统一失败、前端恢复、限流到期、网络/服务异常和错误脱敏证据，逐条验收 US-S2-AUTH-02。

**完成证据：** [`AUTH_02_ACCEPTANCE.md`](AUTH_02_ACCEPTANCE.md) 已登记全部验收标准；后端 56 个、前端 57 个测试通过，前后端全量门禁成功，无阻断缺陷。

**模块出口：** SEC-01-03、AUTH-01-06、AUTH-02-04 均 `Done` 后，M2 可独立交付与验收。

## 6. M3 会话保持与退出

### 6.1 US-S2-SESSION-01 刷新后恢复登录态

**Story 依赖：** AUTH-01-06；顺序执行门为 AUTH-02-04。

#### TASK-S2-SESSION-01-01 实现会话持久化与当前身份 API

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、数据库、API / Done / L |
| 主 Story | US-S2-SESSION-01 |
| 依赖 | AUTH-02-04、ACCOUNT-01-01 |

**工作内容：** 实现会话创建、按摘要读取、刷新和撤销；安全解析 Cookie；校验会话与账号状态；当前身份端点只返回最小用户信息；无效会话返回 401 且不创建会话。

**完成证据：** `AuthLoginIntegrationTest` 已通过 `TC-S2-SESS-001` 至 `003`，覆盖有效、缺失和伪造 Cookie、最小身份响应、无会话副作用及无效 Cookie 清理；`MyBatisAuthSessionRepositoryTest` 验证按摘要读取、撤销后不可读取/刷新以及乱序并发刷新不回退活动时间。后端全量 66 项测试通过。

#### TASK-S2-SESSION-01-02 配置 Cookie 会话与 CSRF 防护

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前后端安全契约 / Done / L |
| 主 Story | US-S2-SESSION-01 |
| 支持后续 | US-S2-SESSION-03、US-S2-AUTHZ-03 |
| 依赖 | SESSION-01-01、PLAN-02 |

**工作内容：** 配置 Origin、凭证携带、CSRF Token 获取与提交；状态变更执行冻结策略；公开端点保持最小白名单；禁止通配来源与凭证组合。

**完成证据：** `AuthLoginIntegrationTest` 已通过 `TC-S2-SESS-014`、`015` 及登录 CSRF 测试；`SessionCookieFactoryTest` 验证 Host-only、`HttpOnly`、`SameSite=Lax`、`Path=/`、生产 `Secure`、本地 HTTP 例外、会话 Cookie 无持久化期限及安全清除；`AuthSecurityPropertiesTest` 验证拒绝通配和非显式 Origin。

#### TASK-S2-SESSION-01-03 实现认证 Provider 与启动恢复

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端状态、路由 / Done / L |
| 主 Story | US-S2-SESSION-01 |
| 依赖 | SESSION-01-01、SESSION-01-02、AUTH-01-03 |

**工作内容：** 实现当前身份 API 和内存认证状态；应用启动先确认会话；期间显示应用级加载，不渲染业务 Shell 或菜单；刷新和新标签页恢复有权目标。

**完成证据：** `AuthProvider`、启动会话边界、当前身份请求及内存认证状态已实现；`App.test.tsx` 和 `auth-api.test.ts` 已通过 `TC-S2-FE-AUTH-010`、`TC-S2-FE-SESS-001` 至 `003`，验证加载期不闪现 Shell、有效会话恢复原地址、无会话进入登录页且无过期误报，以及浏览器存储和 URL 无凭证。前端全量 62 项测试及生产构建通过。

#### TASK-S2-SESSION-01-04 验收会话恢复 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-SESSION-01 |
| 依赖 | SESSION-01-01 至 03 |

**验收内容：** 登记 Cookie、CSRF、当前身份、刷新、新标签页、加载态、无会话 401 和浏览器存储检查，逐条验收 US-S2-SESSION-01。

**完成证据：** [`SESSION_01_ACCEPTANCE.md`](SESSION_01_ACCEPTANCE.md) 已登记全部验收标准、自动化证据及安全边界；后端 66 项、前端 62 项测试通过，前后端全量门禁成功，无阻断缺陷。

**Story 出口：** SESSION-01-04 `Done`，会话恢复能力可独立验收，并解锁 `TASK-S2-SESSION-02-01`。

### 6.2 US-S2-SESSION-02 会话过期或账号停用后重新登录

**Story 依赖：** SESSION-01-04。

#### TASK-S2-SESSION-02-01 实现统一会话失效处理

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、安全 / Done / M |
| 主 Story | US-S2-SESSION-02 |
| 依赖 | SESSION-01-04 |

**工作内容：** 用可控时钟校验空闲和绝对时限；停用、撤销和过期统一返回 401 且不访问业务数据；撤销并清 Cookie；删除失效满 7 天的会话，清理失败不改变即时判断。

**完成证据：** `CurrentSessionServiceTest` 使用固定时钟通过 `TC-S2-SESS-004` 至 `008`，覆盖空闲与绝对边界前/到达边界、停用账号及撤销原因；`AuthLoginIntegrationTest` 验证失效统一 `401 / AUTH-401-001`、清 Cookie 和响应脱敏；`MyBatisAuthSessionRepositoryTest`、`SessionCleanupServiceTest` 通过 `TC-S2-SESS-016`，只清理失效满 7 天记录且清理失败不影响即时校验。

#### TASK-S2-SESSION-02-02 实现前端单次过期流程

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端状态、路由 / Done / M |
| 主 Story | US-S2-SESSION-02 |
| 依赖 | SESSION-02-01、SESSION-01-03 |

**工作内容：** 并发 401 只执行一次清理、跳转和过期提示；安全保存并重新校验原目标；网络/服务异常不误报凭证错误；明确不承诺保存未提交草稿。

**完成证据：** 请求层全局处理仅响应 `AUTH-401-001`；认证 Store 对并发失效只执行一次状态转换，路由保存原站内地址并显示单一过期提示及未提交内容说明。`App.test.tsx` 已通过 `TC-S2-FE-SESS-004` 至 `006`，覆盖并发 401、单次清理/提示、安全返回地址、网络异常独立提示和重试恢复。

#### TASK-S2-SESSION-02-03 验收会话失效 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-SESSION-02 |
| 依赖 | SESSION-02-01、SESSION-02-02 |

**验收内容：** 登记两类过期、停用、无业务访问、并发 401、单次提示、安全返回及异常恢复证据，逐条验收 US-S2-SESSION-02。

**完成证据：** [`SESSION_02_ACCEPTANCE.md`](SESSION_02_ACCEPTANCE.md) 已登记全部验收标准；后端 75 项、前端 67 项测试及生产构建通过，无阻断缺陷。

### 6.3 US-S2-SESSION-03 员工主动退出

**Story 依赖：** SESSION-01-04；顺序执行门为 SESSION-02-03。

#### TASK-S2-SESSION-03-01 实现退出服务与 API

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端 API、安全 / Done / M |
| 主 Story | US-S2-SESSION-03 |
| 依赖 | SESSION-02-03、SESSION-01-02 |

**工作内容：** 使用受 CSRF 防护的非 GET 端点撤销会话并清 Cookie；重复退出幂等；旧 Cookie 不可重放；响应和日志不含会话标识。

**完成证据：** `LogoutService` 与 `POST /api/v1/auth/logout` 已实现；CSRF/Origin 过滤覆盖登录与退出。`AuthLoginIntegrationTest` 已通过 `TC-S2-SESS-009` 至 `013`，验证撤销当前会话、清 Cookie、错误 CSRF 不撤销、重复退出幂等、旧 Cookie 不可重放、并发会话互不影响及响应脱敏。

#### TASK-S2-SESSION-03-02 实现全局账号区与退出体验

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端 UI / Done / M |
| 主 Story | US-S2-SESSION-03 |
| 依赖 | SESSION-03-01、SESSION-01-03 |

**工作内容：** 展示显示名称、角色和可访问退出入口；成功后清理身份并进入登录页；失败时保留状态、提示并允许重试；防重复点击。

**完成证据：** 全局账号区展示显示名称、中文角色名称和退出入口；退出请求先获取 CSRF，成功后清理内存身份并替换到登录页，失败保留身份、显示可重试提示且提交期间防重复。`App.test.tsx` 与 `auth-api.test.ts` 已通过 `TC-S2-FE-SESS-007`、`008` 及后退保护测试。

#### TASK-S2-SESSION-03-03 验收主动退出 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Done / S |
| 主 Story | US-S2-SESSION-03 |
| 依赖 | SESSION-03-01、SESSION-03-02 |

**验收内容：** 登记账号区、CSRF、服务端撤销、后退/刷新/重放、幂等和失败重试证据，逐条验收 US-S2-SESSION-03。

**完成证据：** [`SESSION_03_ACCEPTANCE.md`](SESSION_03_ACCEPTANCE.md) 已登记全部验收标准；后端 75 项、前端 67 项测试及生产构建通过，无阻断缺陷。

**模块出口：** SESSION-01-04、SESSION-02-03、SESSION-03-03 均为 `Done`，M3 可独立交付与验收，并解锁 `TASK-S2-AUTHZ-01-01`。

## 7. M4 角色化访问控制

### 7.1 US-S2-AUTHZ-01 未登录用户访问受保护页面

**Story 依赖：** SESSION-01-04；顺序执行门为 SESSION-03-03。

#### TASK-S2-AUTHZ-01-01 实现受保护路由与安全返回地址

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端路由 / Ready / L |
| 主 Story | US-S2-AUTHZ-01 |
| 依赖 | SESSION-03-03、AUTH-01-05 |

**工作内容：** 将四个业务模块及子路由置于认证守卫；未登录不渲染受保护内容；复用并完善安全返回地址；保持 401、403、404 语义独立。

**完成证据：** `TC-S2-FE-ROUTE-001` 至 `005`、`009` 通过。

#### TASK-S2-AUTHZ-01-02 验收未登录路由保护 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Blocked / S |
| 主 Story | US-S2-AUTHZ-01 |
| 依赖 | AUTHZ-01-01 |

**验收内容：** 登记全部受保护路由、内容不闪现、安全目标、默认页与登录后 404 证据，逐条验收 US-S2-AUTHZ-01。

### 7.2 US-S2-AUTHZ-02 按角色查看和访问页面

**Story 依赖：** AUTHZ-01-02。

#### TASK-S2-AUTHZ-02-01 实现角色菜单与 403 页面

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 前端 UI、路由 / Blocked / M |
| 主 Story | US-S2-AUTHZ-02 |
| 依赖 | AUTHZ-01-02 |

**工作内容：** 按权限矩阵过滤导航；管理员继承收银员入口；收银员直达商品或看板显示 403 和返回 `/pos`，不渲染业务内容；权限未知或恢复中按未授权处理。

**完成证据：** `TC-S2-FE-ROUTE-006` 至 `008`、`010` 通过。

#### TASK-S2-AUTHZ-02-02 验收角色页面访问 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story 验收 / Blocked / S |
| 主 Story | US-S2-AUTHZ-02 |
| 依赖 | AUTHZ-02-01 |

**验收内容：** 登记两角色菜单、四模块直达、403、恢复期间无菜单闪现及页面隐藏不作为后端授权证据的检查结果。

### 7.3 US-S2-AUTHZ-03 后端独立执行接口权限

**Story 依赖：** SESSION-01-04；顺序执行门为 AUTHZ-02-02。

#### TASK-S2-AUTHZ-03-01 建立统一认证上下文

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端 / Blocked / M |
| 主 Story | US-S2-AUTHZ-03 |
| 依赖 | AUTHZ-02-02、SESSION-01-04 |

**工作内容：** 将有效会话转换为统一当前用户上下文；明确 `ADMIN` 包含 `CASHIER` 及多角色并集；请求结束清理上下文，避免线程污染。

**完成证据：** 测试覆盖两角色、多角色、无会话和并发隔离。

#### TASK-S2-AUTHZ-03-02 配置默认拒绝与角色规则

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、安全 / Blocked / L |
| 主 Story | US-S2-AUTHZ-03 |
| 依赖 | AUTHZ-03-01、SESSION-01-02 |

**工作内容：** 业务接口默认要求认证，仅显式白名单公开入口；提供角色声明；401/403 使用稳定业务码，拒绝时不执行应用服务或数据库操作。

**完成证据：** 正反权限矩阵通过；未声明测试端点默认拒绝。

#### TASK-S2-AUTHZ-03-03 增加后端权限架构约束

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端测试、架构 / Blocked / S |
| 主 Story | US-S2-AUTHZ-03 |
| 依赖 | AUTHZ-03-02 |

**工作内容：** 更新架构测试和后端约定，防止 Controller 绕过统一入口；新增业务接口必须声明访问规则。

**完成证据：** 故意无权限声明的示例可使测试失败，实际代码通过。

#### TASK-S2-AUTHZ-03-04 验收后端权限 Story

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端测试、Story 验收 / Blocked / M |
| 主 Story | US-S2-AUTHZ-03 |
| 依赖 | AUTHZ-03-01 至 03 |

**验收内容：** 执行 `TC-S2-AUTHZ-001` 至 `010`；覆盖角色矩阵、未声明接口和伪造请求；断言拒绝时无业务读写，响应含 `traceId` 且无敏感上下文。

**模块出口：** AUTHZ-01-02、AUTHZ-02-02、AUTHZ-03-04 均 `Done` 后，M4 可独立交付与验收。

## 8. M5 安全事件与追踪

### 8.1 US-S2-AUDIT-01 追踪认证与权限事件

**Story 依赖：** 前 10 个 Story 的验收 Task 全部 `Done`。

#### TASK-S2-AUDIT-01-01 实现结构化认证安全事件

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 后端、可观测性 / Blocked / L |
| 主 Story | US-S2-AUDIT-01 |
| 依赖 | AUTH-02-04、SESSION-02-03、SESSION-03-03、AUTHZ-03-04 |

**工作内容：** 统一发布登录成功/失败、退出、过期/失效、停用失效、限流、CSRF 拒绝和权限拒绝事件；记录类型、结果、服务器时间、`traceId` 和最小上下文；仅在确认身份后记录稳定账号 ID。

**完成证据：** `TC-S2-AUDIT-001` 至 `008` 通过，401/403/429 可由 `traceId` 关联日志。

#### TASK-S2-AUDIT-01-02 增加全链路敏感信息防泄露检查

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 安全测试 / Blocked / M |
| 主 Story | US-S2-AUDIT-01 |
| 依赖 | AUDIT-01-01 |

**工作内容：** 检查响应、异常、日志、测试报告和示例配置；禁止密码、摘要、会话 ID、Cookie、完整请求头和认证请求体；预期失败不打印堆栈，未知异常仅在服务端保留必要堆栈。

**完成证据：** `TC-S2-AUDIT-009`、`010` 通过；假敏感值可触发检查失败。

#### TASK-S2-AUDIT-01-03 执行按模块端到端验收

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | E2E、验收 / Blocked / L |
| 主 Story | US-S2-AUDIT-01 |
| 依赖 | M1 至 M4 全部 Story 验收门、AUDIT-01-02 |

**工作内容：** 分别执行 M2 登录/失败/限流，M3 恢复/失效/退出，M4 页面与接口权限矩阵，M5 事件与脱敏旅程。

**完成证据：** `TC-S2-E2E-001` 至 `006` 通过，无 P0/P1 缺陷；证据不含凭证。

#### TASK-S2-AUDIT-01-04 更新开发、配置与安全说明

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | 文档 / Blocked / M |
| 主 Story | US-S2-AUDIT-01 |
| 依赖 | AUDIT-01-01、实现方案稳定 |

**工作内容：** 更新根目录与前后端 README 的变量名、初始化、登录联调、Cookie/CSRF 和排障；更新认证上下文、权限声明、安全事件及敏感字段约定；仅使用安全占位符。

**完成证据：** 非作者按说明从空环境完成登录链路验证。

#### TASK-S2-AUDIT-01-05 验收安全事件 Story 与 Sprint 2

| 属性 | 内容 |
| --- | --- |
| 类型 / 状态 / 复杂度 | Story、模块、Sprint 验收 / Blocked / M |
| 主 Story | US-S2-AUDIT-01 |
| 依赖 | AUDIT-01-01 至 04、前 10 个 Story 验收门 |

**验收内容：** 逐条验收 US-S2-AUDIT-01；执行前端 `npm run check`、后端 `./mvnw clean verify`、E2E 和人工安全检查；汇总 11 条 Must Story 证据；确认无 P0/P1、真实凭证、未声明公开接口或未跟踪迁移；形成 Sprint 3 鉴权接入说明。

**模块与 Sprint 出口：** AUDIT-01-05 `Done` 后，M5 和 Sprint 2 登录体系整体可交付。

## 9. 模块交付清单

| 模块 | 包含 Story | 必须通过的 Story 验收门 | 可交付产品增量 |
| --- | --- | --- | --- |
| M1 安全初始化账号 | ACCOUNT-01 | ACCOUNT-01-04 | 新环境可安全、幂等获得最小账号集 |
| M2 安全登录 | SEC-01、AUTH-01、AUTH-02 | SEC-01-03、AUTH-01-06、AUTH-02-04 | 两角色可安全登录；失败、限流和异常可恢复 |
| M3 会话保持与退出 | SESSION-01 至 03 | SESSION-01-04、SESSION-02-03、SESSION-03-03 | 会话可恢复、会失效、可主动退出且不可重放 |
| M4 角色化访问控制 | AUTHZ-01 至 03 | AUTHZ-01-02、AUTHZ-02-02、AUTHZ-03-04 | 页面与接口均按角色保护，默认拒绝 |
| M5 安全事件与追踪 | AUDIT-01 | AUDIT-01-05 | 安全事件可追踪、全链路脱敏、Sprint 验收完成 |

模块验收记录必须包含：版本/提交、环境、执行人、Story 验收结果、关联 `TC-S2-*`、非敏感证据位置、遗留问题和结论。证据命名遵循 `TEST_STRATEGY.md`。

## 10. Task Definition of Ready

- 所属 Story 为 Ready，且 Story 前置验收门已完成。
- 依赖 Task 已完成，或存在经过评审的稳定契约。
- 数据库、API、Cookie、CSRF、角色和错误码变化已明确评审人。
- 已明确关联的 `TC-S2-*`，测试数据不需要真实生产凭证。
- 复杂度与负责人已确认，工作量适合在 1 至 2 个工作日内完成。
- Story 验收 Task 只有在该 Story 所有实现 Task `Done` 且每条验收标准已有检查步骤时才可转为 `Ready`。

## 11. Task Definition of Done

- 实现满足 Task 工作内容及主 Story 的相关验收标准。
- 自动化测试随实现提交并通过，不把本 Story 测试推迟到后续模块。
- 未修改已执行迁移，未提交真实凭证、依赖目录、构建产物或临时文件。
- 日志、异常、响应和测试输出不包含密码、摘要、会话标识或 Cookie。
- 文档与实际 API、配置、错误码和命令一致；变更已完成代码评审。
- Story 验收 Task 仅在全部验收标准通过、证据已登记且无阻断缺陷时标记 `Done`。
- 模块仅在所属 Story 验收门全部 `Done` 后宣布交付。
