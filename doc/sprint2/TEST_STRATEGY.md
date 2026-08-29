# Cup Flow Coffee POS — Sprint 2 登录体系测试策略与追踪

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.9 |
| 状态 | 已批准；M1 至 M5 的 11 条 Must Story 实现与验收证据已登记，Sprint 2 验收通过 |
| 关联任务 | TASK-S2-PLAN-03 |
| 生效日期 | 2026-08-24 |
| 最近更新 | 2026-08-30（完成 AUDIT-01、M5 与 Sprint 2 整体验收） |

## 1. 测试目标

Sprint 2 测试证明：只有合法、启用的员工能建立登录态；会话可恢复、可过期、可撤销；收银员与管理员的页面和接口权限均符合矩阵；失败、限流和安全事件不泄露账号或凭证。

本文件冻结测试编号、层级和预期。代码证据已随相应实现 Task 登记；最终执行记录以各 Story 验收文档和 `EVID-S2-AUTH-20260830.md` 为准。

## 2. 测试分层

| 层级 | 责任 | 工具/入口 | 不负责 |
| --- | --- | --- | --- |
| 后端单元 | 密码验证、会话时间、角色继承、返回地址、限流状态机 | JUnit、可控 `Clock` | HTTP、真实数据库 |
| 后端接口 | 请求校验、Cookie/CSRF、HTTP/业务码、认证上下文、拒绝前不执行业务 | MockMvc、Spring 测试 | 浏览器交互 |
| 数据库 | V2+ 迁移、会话约束/索引、初始化账号幂等、Repository | PostgreSQL 18.4、Testcontainers、Flyway | 共享本地数据库 |
| 前端单元/组件/路由 | 登录表单、状态恢复、并发 401、菜单、403/404、焦点与 ARIA | Vitest、Testing Library、Memory Router、MSW/Fetch mock | 重新验证后端密码/权限规则 |
| E2E | 少量真实浏览器跨端旅程 | Vitest/MockMvc 跨层回归与本地真实浏览器验收 | 穷举所有安全边界 |
| 人工安全检查 | Cookie 浏览器属性、日志脱敏、返回地址、网络面板与异常恢复 | 浏览器开发工具、日志抽查 | 替代自动化回归 |

原则：

- P0：后端权限矩阵、会话失效、密码与凭证保护、CSRF、初始化账号。
- P1：前端状态/可访问性、限流提示、审计可追踪性。
- 时间相关测试注入可控 `Clock`，不得真实等待 30 分钟或 8 小时。
- 每个测试使用独立账号、会话和限流键，不依赖顺序或共享状态。
- 测试显示名称或说明包含稳定 `TC-S2-*`；废弃编号不复用。
- 自动化和证据不得输出真实密码、摘要、Session Cookie 或 CSRF Token。

## 3. 后端与数据测试用例

### 3.1 密码和初始化账号

| ID | 层级 | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- | --- |
| TC-S2-PASS-001 | 单元 | 正确密码验证 | 验证成功，不暴露摘要 | US-S2-SEC-01 / SEC-01-01 |
| TC-S2-PASS-002 | 单元 | 错误密码验证 | 验证失败，返回统一内部结果 | US-S2-SEC-01 / SEC-01-01 |
| TC-S2-PASS-003 | 单元 | 相同密码生成两次摘要 | 摘要不同且均可验证 | US-S2-SEC-01 / SEC-01-01 |
| TC-S2-PASS-004 | 单元 | 未知账号验证 | 走等价安全校验路径，不生成会话 | US-S2-AUTH-02、US-S2-SEC-01 / SEC-01-01 |
| TC-S2-PASS-005 | 单元 | 无效/损坏摘要 | 安全失败，不回显摘要或产生 500 泄露 | US-S2-SEC-01 / SEC-01-01 |
| TC-S2-DATA-001 | 数据库 | 空库执行全部迁移 | V1 身份表和 Sprint 2 会话结构成功建立 | US-S2-ACCOUNT-01 / ACCOUNT-01-01 |
| TC-S2-DATA-002 | 数据库 | 从已执行 V1 升级 | 不改写 V1，新增迁移成功 | US-S2-ACCOUNT-01 / ACCOUNT-01-01 |
| TC-S2-DATA-003 | 数据库 | 重复启动 Flyway | 不重复创建对象或基础数据 | US-S2-ACCOUNT-01 / ACCOUNT-01-01 |
| TC-S2-DATA-004 | 数据库 | 会话原始凭证检查 | 数据库仅保存不可反推摘要 | US-S2-SESSION-01 / ACCOUNT-01-01 |
| TC-S2-DATA-005 | 数据库 | 首次启用账号初始化 | 创建一个 CASHIER 和一个 ADMIN，角色正确 | US-S2-ACCOUNT-01 / ACCOUNT-01-03 |
| TC-S2-DATA-006 | 数据库 | 重复执行初始化 | 不重复、不覆盖已有密码/状态/展示名/角色 | US-S2-ACCOUNT-01 / ACCOUNT-01-03 |
| TC-S2-DATA-007 | 配置 | 生产启用初始化但 Secret 缺失 | 应用启动失败，不创建弱默认账号 | US-S2-ACCOUNT-01 / ACCOUNT-01-03 |
| TC-S2-DATA-008 | 数据库 | 初始化账号停用后重复初始化 | 保持 `DISABLED`，不恢复密码、状态、展示名或角色 | US-S2-ACCOUNT-01 / ACCOUNT-01-03 |

`TC-S2-DATA-001` 至 `004` 已由 `DatabaseMigrationTest` 实现，覆盖最新空库结构、V1→V2 升级、重复执行、摘要字段与生命周期约束。`AccountBootstrapIntegrationTest` 已实现 `TC-S2-DATA-005` 至 `008`。停用账号实际登录拒绝和统一用户反馈不属于初始化模块，继续由接口用例 `TC-S2-AUTH-008` 在 `US-S2-AUTH-02` 验收，避免形成 `ACCOUNT-01 → AUTH-02 → ACCOUNT-01` 循环依赖。

`TC-S2-PASS-001` 至 `005` 已由 `Pbkdf2PasswordHasherTest` 和 `AccountPasswordVerifierTest` 实现；正确与错误密码均使用摘要参数执行 PBKDF2，未知账号使用启动时随机生成的诱饵摘要执行同类校验，损坏或不支持的摘要统一安全失败。

### 3.2 登录与限流

| ID | 层级 | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- | --- |
| TC-S2-AUTH-001 | 接口 | 有效 CASHIER 登录 | 200、CurrentUser、全新安全 Cookie | US-S2-AUTH-01 / AUTH-01-01、02 |
| TC-S2-AUTH-002 | 接口 | 有效 ADMIN 登录 | 200、ADMIN、默认 `/dashboard` | US-S2-AUTH-01 / AUTH-01-01、02 |
| TC-S2-AUTH-003 | 接口 | username 首尾空白 | 去空白后正常校验 | US-S2-AUTH-01 / AUTH-01-01 |
| TC-S2-AUTH-004 | 接口 | password 含首尾空白 | 不去空白，按原值校验 | US-S2-AUTH-01 / AUTH-01-01 |
| TC-S2-AUTH-005 | 接口 | 空字段或超长字段 | 400、字段错误、不累计登录失败 | US-S2-AUTH-01 / AUTH-01-02 |
| TC-S2-AUTH-006 | 接口 | 账号不存在 | 401、AUTH-401-002、无会话 | US-S2-AUTH-02 / AUTH-01-01 |
| TC-S2-AUTH-007 | 接口 | 密码错误 | 与不存在账号相同状态、码和消息 | US-S2-AUTH-02 / AUTH-01-01 |
| TC-S2-AUTH-008 | 接口 | 停用账号 | 与不存在账号相同状态、码和消息 | US-S2-AUTH-02 / AUTH-01-01 |
| TC-S2-AUTH-009 | 接口 | 登录前提供旧会话 ID | 成功后轮换，不发生会话固定 | US-S2-AUTH-01 / AUTH-01-02 |
| TC-S2-AUTH-010 | 接口 | 缺失/错误 CSRF 登录 | 403、AUTH-403-002、不验证密码 | US-S2-AUTH-01 / AUTH-01-02 |
| TC-S2-RATE-001 | 单元/接口 | 同组合前 4 次失败 | 每次 401，不返回剩余次数 | US-S2-AUTH-02 / AUTH-02-01 |
| TC-S2-RATE-002 | 单元/接口 | 同组合第 5 次失败 | 429、AUTH-429-001、Retry-After | US-S2-AUTH-02 / AUTH-02-01 |
| TC-S2-RATE-003 | 单元/接口 | 限制期间继续尝试 | 429，限制截止时间不延长 | US-S2-AUTH-02 / AUTH-02-01 |
| TC-S2-RATE-004 | 单元/接口 | 15 分钟限制期结束 | 可重新尝试，无需管理员操作 | US-S2-AUTH-02 / AUTH-02-01 |
| TC-S2-RATE-005 | 单元/接口 | 不同来源或账号标识 | 限流键相互隔离 | US-S2-AUTH-02 / AUTH-02-01 |
| TC-S2-RATE-006 | 单元/接口 | 成功登录 | 清除对应失败状态 | US-S2-AUTH-02 / AUTH-02-01 |

`AuthLoginIntegrationTest` 已实现并通过 `TC-S2-AUTH-001` 至 `010`：成功路径覆盖两角色最小响应、账号去空白、密码原值、字段拒绝、新会话轮换、安全 Cookie 和 CSRF 先行拒绝；失败路径覆盖不存在账号、错误密码和停用账号统一反馈及无会话副作用。

`LoginAttemptLimiterTest` 与接口测试已实现并通过 `TC-S2-RATE-001` 至 `006`：前 4 次失败、第 5 次限制、限制不续期、15 分钟自动恢复、组合隔离和成功清除均由可控时钟验证；接口同时验证 429、整数秒 `Retry-After` 以及伪造 `X-Forwarded-For` 不能绕过直接来源限制。

### 3.3 会话与退出

| ID | 层级 | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- | --- |
| TC-S2-SESS-001 | 接口 | 有效 Cookie 查询 `/auth/me` | 200、最小 CurrentUser | US-S2-SESSION-01 / SESSION-01-01 |
| TC-S2-SESS-002 | 接口 | 无 Cookie 查询当前身份 | 401、AUTH-401-001，不新建会话 | US-S2-SESSION-01 / SESSION-01-01 |
| TC-S2-SESS-003 | 接口 | 伪造 Cookie | 401、清除 Cookie、不泄露内部原因 | US-S2-SESSION-01 / SESSION-01-01 |
| TC-S2-SESS-004 | 单元/接口 | 空闲 30 分钟边界前 | 会话有效 | US-S2-SESSION-02 / SESSION-02-01 |
| TC-S2-SESS-005 | 单元/接口 | 达到空闲 30 分钟 | 401、撤销并清除 Cookie | US-S2-SESSION-02 / SESSION-02-01 |
| TC-S2-SESS-006 | 单元/接口 | 绝对 8 小时边界前 | 会话有效 | US-S2-SESSION-02 / SESSION-02-01 |
| TC-S2-SESS-007 | 单元/接口 | 达到绝对 8 小时 | 401，即使持续活跃也失效 | US-S2-SESSION-02 / SESSION-02-01 |
| TC-S2-SESS-008 | 接口 | 登录后账号被停用 | 下一请求 401、撤销会话 | US-S2-SESSION-02 / SESSION-02-01 |
| TC-S2-SESS-009 | 接口 | 当前会话退出 | 200、撤销并清除 Cookie | US-S2-SESSION-03 / SESSION-03-01 |
| TC-S2-SESS-010 | 接口 | 重复退出 | 200、幂等、不生成会话 | US-S2-SESSION-03 / SESSION-03-01 |
| TC-S2-SESS-011 | 接口 | 退出缺失/错误 CSRF | 403，不把有效会话误撤销 | US-S2-SESSION-03 / SESSION-03-01 |
| TC-S2-SESS-012 | 接口 | 退出后重放旧 Cookie | 401、不可访问资源 | US-S2-SESSION-03 / SESSION-03-01 |
| TC-S2-SESS-013 | 数据库 | 两个并发会话 | 均可用；退出一个不撤销另一个 | US-S2-SESSION-03 / SESSION-03-01 |
| TC-S2-SESS-014 | 接口 | 合法 Origin + CSRF | 状态变更通过 | US-S2-SESSION-01 / SESSION-01-02 |
| TC-S2-SESS-015 | 接口 | 恶意 Origin/跨站请求 | 403、AUTH-403-002 | US-S2-SESSION-01 / SESSION-01-02 |
| TC-S2-SESS-016 | 单元/数据库 | 会话失效满 7 天批量清理 | 只删除达到保留期的失效记录；清理失败不使会话重新有效 | US-S2-SESSION-02 / SESSION-02-01 |

`AuthLoginIntegrationTest` 已实现并通过 `TC-S2-SESS-001` 至 `003`、`014`、`015`：有效会话返回最小身份并原子刷新活动时间；缺失或伪造 Cookie 返回统一 401 且不创建会话，伪造 Cookie 同时被清除；合法 Origin 与 CSRF 可执行登录，恶意 Origin 在认证前被拒绝。`MyBatisAuthSessionRepositoryTest` 补充验证摘要查询、撤销不可重放及乱序刷新不回退；`SessionCookieFactoryTest` 和 `AuthSecurityPropertiesTest` 固化 Cookie 环境属性与显式 Origin 规则。

`CurrentSessionServiceTest` 与 `AuthLoginIntegrationTest` 已实现并通过 `TC-S2-SESS-004` 至 `008`：固定时钟精确验证空闲 30 分钟和绝对 8 小时边界，过期与停用路径即时撤销、清 Cookie 并使用统一 401。`MyBatisAuthSessionRepositoryTest` 与 `SessionCleanupServiceTest` 已通过 `TC-S2-SESS-016`，物理清理仅删除保留期已满的失效行，清理失败不传播到请求期判断。

`AuthLoginIntegrationTest` 已实现并通过 `TC-S2-SESS-009` 至 `013`：退出使用受 Origin/CSRF 防护的 POST，只撤销当前会话并清 Cookie；错误 CSRF 不误撤销；重复退出幂等；旧 Cookie 不可重放；同账号另一并发会话保持可用；响应不包含原始或摘要会话标识。

### 3.4 后端权限矩阵

| ID | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- |
| TC-S2-AUTHZ-001 | 未登录访问 POS/订单接口 | 401，服务与数据库操作未执行 | US-S2-AUTHZ-03 / AUTHZ-03-02、04 |
| TC-S2-AUTHZ-002 | CASHIER 访问 POS/订单接口 | 允许 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-003 | ADMIN 访问 POS/订单接口 | 允许，验证角色继承 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-004 | CASHIER 访问商品/看板接口 | 403，服务与数据库操作未执行 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-005 | ADMIN 访问商品/看板接口 | 允许 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-006 | 任意角色访问未声明业务接口 | 403，默认拒绝 | US-S2-AUTHZ-03 / AUTHZ-03-02、03 |
| TC-S2-AUTHZ-007 | 客户端伪造角色字段 | 不提升权限，以服务端会话角色为准 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-008 | 无效/过期会话访问管理员接口 | 401 而非 403 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-009 | 已登录角色不足 | 403 而非 401，不清除会话 | US-S2-AUTHZ-03 / AUTHZ-03-04 |
| TC-S2-AUTHZ-010 | 公开 CSRF/登录/健康检查 | 无登录态可按契约访问 | US-S2-AUTHZ-03 / AUTHZ-03-02 |

`AuthorizationIntegrationTest` 已实现并通过 `TC-S2-AUTHZ-001` 至 `010`：服务端从会话恢复角色，验证匿名 401、角色矩阵、`ADMIN` 权限继承、未声明端点默认 403、伪造角色不能提权、无效会话 401 和显式公开入口；拒绝路径的业务探针保持零调用，响应包含 `traceId` 且不包含会话或内部角色规则。`RoleAuthorizationTest` 和 `CurrentUserContextTest` 补充多角色并集及请求线程隔离；`ArchitectureTest` 强制每个生产 Controller 端点声明访问规则。

## 4. 前端测试用例

### 4.1 登录表单

| ID | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- |
| TC-S2-FE-AUTH-001 | 登录页初始状态 | 密码隐藏，字段与按钮语义正确 | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-002 | 显示/隐藏密码 | 键盘可操作，无障碍名称随状态变化 | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-003 | 空字段提交 | 不发请求，聚焦首个错误并可被辅助技术感知 | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-004 | 提交中重复点击 | 只发送一次请求，显示加载状态 | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-005 | CASHIER 登录成功 | 进入 `/pos` | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-006 | ADMIN 登录成功 | 进入 `/dashboard` | US-S2-AUTH-01 / AUTH-01-04 |
| TC-S2-FE-AUTH-007 | AUTH-401-002 | 保留账号、清空密码、焦点回密码、统一提示 | US-S2-AUTH-02 / AUTH-02-02 |
| TC-S2-FE-AUTH-008 | AUTH-429-001 | 显示稍后重试，与认证失败区分 | US-S2-AUTH-02 / AUTH-02-02 |
| TC-S2-FE-AUTH-009 | 网络/超时/500 | 使用各自可恢复提示，不显示凭证错误 | US-S2-AUTH-02 / AUTH-02-02 |
| TC-S2-FE-AUTH-010 | 浏览器存储与 URL | 不包含 Session/CSRF Token | US-S2-SESSION-01 / SESSION-01-03 |

`AuthPage.test.tsx`、请求层和错误映射测试已实现并通过 `TC-S2-FE-AUTH-007` 至 `009`：认证失败保留账号、清空密码并回焦；429 按 `Retry-After` 暂停并恢复；网络、超时和 500 使用相互独立且可重试的提示。

`App.test.tsx` 与 `auth-api.test.ts` 已实现并通过 `TC-S2-FE-AUTH-010`、`TC-S2-FE-SESS-001` 至 `003`：启动时通过同源 Cookie 查询当前身份，确认期间只显示应用级加载；有效会话恢复内存用户和原路径/查询参数；无会话进入登录页且不显示运行中过期提示；恢复过程不写 localStorage、sessionStorage 或 URL 凭证。

`App.test.tsx` 已实现并通过 `TC-S2-FE-SESS-004` 至 `006`：并发 `AUTH-401-001` 只产生一次认证状态转换和一份过期提示，清理内存身份并保存原站内目标；页面明确提示未提交内容可能丢失；当前身份网络失败使用独立可重试状态，不显示密码错误。

`App.test.tsx` 与 `auth-api.test.ts` 已实现并通过 `TC-S2-FE-SESS-007`、`008`：账号区展示员工与角色；退出使用 CSRF POST；成功后清理身份、替换到登录页且后退不渲染业务内容；失败保留身份并允许重试，处理中防止重复请求。

### 4.2 会话、路由与菜单

| ID | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- |
| TC-S2-FE-SESS-001 | 应用启动查询中 | 显示加载，不闪现 Shell/菜单 | US-S2-SESSION-01 / SESSION-01-03 |
| TC-S2-FE-SESS-002 | 刷新有效会话页面 | 恢复用户和当前授权地址 | US-S2-SESSION-01 / SESSION-01-03 |
| TC-S2-FE-SESS-003 | 启动时无会话 | 进入登录页，不显示过期提示 | US-S2-SESSION-01 / SESSION-01-03 |
| TC-S2-FE-SESS-004 | 运行中收到 AUTH-401-001 | 清理身份、进入登录页、显示一次过期提示 | US-S2-SESSION-02 / SESSION-02-02 |
| TC-S2-FE-SESS-005 | 多请求并发 401 | 只执行一次退出流程和提示 | US-S2-SESSION-02 / SESSION-02-02 |
| TC-S2-FE-SESS-006 | 当前身份网络失败 | 不误报密码错误，提供重试 | US-S2-SESSION-02 / SESSION-02-02 |
| TC-S2-FE-SESS-007 | 退出成功 | 清理身份、进入登录页 | US-S2-SESSION-03 / SESSION-03-02 |
| TC-S2-FE-SESS-008 | 退出失败 | 不假装成功，保留状态并允许重试 | US-S2-SESSION-03 / SESSION-03-02 |
| TC-S2-FE-ROUTE-001 | 未登录直达受保护页面 | 进入登录页，不渲染页面内容 | US-S2-AUTHZ-01 / AUTHZ-01-01 |
| TC-S2-FE-ROUTE-002 | 有权站内返回地址 | 登录后恢复目标 | US-S2-AUTHZ-01 / AUTHZ-01-01 |
| TC-S2-FE-ROUTE-003 | 外部、`//`、畸形返回地址 | 拒绝并进入角色默认页 | US-S2-AUTHZ-01 / AUTHZ-01-01 |
| TC-S2-FE-ROUTE-004 | 无权或不存在返回地址 | 分别进入默认页；不开放重定向 | US-S2-AUTHZ-01 / AUTHZ-01-01 |
| TC-S2-FE-ROUTE-005 | 已登录访问 `/login` | 进入角色默认页 | US-S2-AUTH-01 / AUTH-01-05 |
| TC-S2-FE-ROUTE-006 | CASHIER 导航 | 只显示 POS、订单 | US-S2-AUTHZ-02 / AUTHZ-02-01 |
| TC-S2-FE-ROUTE-007 | ADMIN 导航 | 显示四个业务模块 | US-S2-AUTHZ-02 / AUTHZ-02-01 |
| TC-S2-FE-ROUTE-008 | CASHIER 直达商品/看板 | 显示 403 和返回 `/pos`，不渲染业务页 | US-S2-AUTHZ-02 / AUTHZ-02-01 |
| TC-S2-FE-ROUTE-009 | 不存在路由 | 已登录时显示 404，不误显示 403 | US-S2-AUTHZ-01 / AUTHZ-01-01 |
| TC-S2-FE-ROUTE-010 | 权限恢复中 | 不短暂显示无权菜单 | US-S2-AUTHZ-02 / AUTHZ-02-01 |

`AuthPage.test.tsx`、`auth-api.test.ts`、`safe-return-path.test.ts` 和请求层测试已实现并通过 `TC-S2-FE-AUTH-001` 至 `006`、`TC-S2-FE-ROUTE-002` 至 `005`。`App.test.tsx` 已实现并通过 `TC-S2-FE-ROUTE-001`、`006` 至 `010`：四个业务模块和子地址均受认证边界保护；两角色菜单及直达权限符合矩阵；无权页面显示 403 且不渲染业务内容；已登录未知地址保持 404；身份恢复期间不渲染业务 Shell 或无权菜单。

## 5. 安全事件与脱敏测试

| ID | 场景 | 预期 | 关联 Story / Task |
| --- | --- | --- | --- |
| TC-S2-AUDIT-001 | 登录成功 | 记录成功事件、accountId、traceId，无凭证 | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-002 | 不存在/错误密码/停用登录 | 记录统一失败事件，不通过日志泄露账号状态 | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-003 | 限流 | 记录 rate-limited 事件，可关联 429 traceId | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-004 | 空闲/绝对过期 | 记录过期事件，可关联 401 traceId | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-005 | 账号停用导致失效 | 记录内部停用事件，响应不暴露状态 | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-006 | 退出 | 记录退出事件，不含 Session/CSRF Token | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-007 | 权限拒绝 | 记录 accountId、目标与 traceId，不含内部规则明细 | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-008 | CSRF 拒绝 | 记录最小事件，不输出 Header/Cookie 全文 | US-S2-AUDIT-01 / AUDIT-01-01 |
| TC-S2-AUDIT-009 | 认证异常/响应/测试报告扫描 | 无密码、摘要、Session Cookie、CSRF Token | US-S2-SEC-01、US-S2-AUDIT-01 / SEC-01-02、AUDIT-01-02 |
| TC-S2-AUDIT-010 | 注入测试假敏感值 | 自动脱敏检查失败，证明检查有效 | US-S2-SEC-01、US-S2-AUDIT-01 / SEC-01-02、AUDIT-01-02 |

`SecurityEventRecorderTest` 与 `SecurityEventIntegrationTest` 已实现并通过 `TC-S2-AUDIT-001` 至 `008`；`SensitiveValueScannerTest` 与 `ZSensitiveArtifactScanTest` 已实现并通过 `TC-S2-AUDIT-009`、`010`，覆盖响应、日志、测试报告和示例配置的敏感值扫描。

## 6. E2E 与人工验收

| ID | 旅程 | 预期 | 优先级 |
| --- | --- | --- | --- |
| TC-S2-E2E-001 | CASHIER 登录 → POS → 刷新 → 订单 → 退出 → 后退 | 授权页面可用；刷新恢复；退出后不可恢复 | P0 |
| TC-S2-E2E-002 | CASHIER 直接访问商品/看板 | 菜单隐藏，直达 403，接口同样拒绝 | P0 |
| TC-S2-E2E-003 | ADMIN 登录 → 四模块切换 → 退出 | 默认看板，四入口可用，退出失效 | P0 |
| TC-S2-E2E-004 | 错误/停用账号与第 5 次失败 | 统一认证失败；第 5 次起 429；无账号枚举提示 | P0 |
| TC-S2-E2E-005 | 可控过期与账号停用 | 下一请求进入登录页，只提示一次且无数据访问 | P0 |
| TC-S2-E2E-006 | 后端不可用后恢复 | 不误报凭证错误或放行；恢复后可重试 | P1 |

`TC-S2-E2E-001` 至 `006` 已通过自动化回归与人工浏览器验收，执行结果见 [`EVID-S2-AUTH-20260830.md`](EVID-S2-AUTH-20260830.md)。

人工安全复核：

- [x] 生产配置测试确认 `CUP_FLOW_SESSION` 为 `HttpOnly; Secure; SameSite=Lax; Path=/` 且无 Domain。
- [x] 浏览器会话 Cookie 不持久化；同一浏览器会话刷新和新标签页可恢复。
- [x] localStorage、sessionStorage、URL、Console 和 Network 响应体无 Session ID。
- [x] 外部及双斜杠返回地址不能跳出应用 Origin。
- [x] 401、403、429 的页面反馈与登录态处理符合契约。
- [x] 日志抽查覆盖认证、会话、退出、CSRF 与授权事件，使用 `traceId` 可定位且无敏感字段。
- [x] 初始化配置和 CI Secret 不出现在仓库、构建日志或测试报告。

证据命名：`EVID-S2-AUTH-{YYYYMMDD}.md`，记录版本、环境、执行人、用例结果和非敏感证据位置。

## 7. Story—Task—Test 追踪矩阵

Task ID 在表中省略统一前缀 `TASK-S2-`。

| User Story | 主要 Task | 测试用例 |
| --- | --- | --- |
| US-S2-ACCOUNT-01 | ACCOUNT-01-01 至 04 | DATA-001 至 008 |
| US-S2-SEC-01 | SEC-01-01 至 03 | PASS-001 至 005、AUDIT-009/010 |
| US-S2-AUTH-01 | AUTH-01-01 至 06 | AUTH-001 至 005、009/010、FE-AUTH-001 至 006、FE-ROUTE-005、E2E-001/003 |
| US-S2-AUTH-02 | AUTH-02-01 至 04 | PASS-004、AUTH-006 至 008、RATE-001 至 006、FE-AUTH-007 至 009、E2E-004 |
| US-S2-SESSION-01 | SESSION-01-01 至 04 | DATA-004、SESS-001 至 003、014/015、FE-AUTH-010、FE-SESS-001 至 003、E2E-001 |
| US-S2-SESSION-02 | SESSION-02-01 至 03 | SESS-004 至 008、016、FE-SESS-004 至 006、E2E-005 |
| US-S2-SESSION-03 | SESSION-03-01 至 03 | SESS-009 至 013、FE-SESS-007/008、E2E-001/003 |
| US-S2-AUTHZ-01 | AUTHZ-01-01/02 | FE-ROUTE-001 至 005、009、E2E-001 |
| US-S2-AUTHZ-02 | AUTHZ-02-01/02 | FE-ROUTE-006 至 008、010、E2E-002/003 |
| US-S2-AUTHZ-03 | AUTHZ-03-01 至 04 | AUTHZ-001 至 010、E2E-002/003 |
| US-S2-AUDIT-01 | AUDIT-01-01 至 05 | AUDIT-001 至 010、E2E-004/005 |

## 8. CI 与验收门禁

- 每个实现 Task 先运行针对性测试；合并前执行前端 `npm run check` 和后端 `./mvnw clean verify`。
- P0 单元、接口、数据库和前端测试必须进入现有 `Frontend`/`Backend` CI，失败阻止合并。
- E2E 使用专用可重置环境；`TC-S2-E2E-001` 至 `005` 失败阻止 Sprint 验收。
- 不稳定用例必须有缺陷编号、负责人和修复期限，禁止通过无限重试或永久跳过掩盖。
- Story 验收前将测试矩阵中的“待实现”更新为实际类/文件/运行记录；没有证据不算通过。
- Sprint 2 最终验收必须确认 11 条 Must Story、完整权限矩阵和敏感信息检查全部通过。
