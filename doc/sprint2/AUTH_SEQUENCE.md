# Cup Flow Coffee POS — Sprint 2 登录体系时序图

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 状态 | Sprint 2 已批准需求的时序基线 |
| 基线日期 | 2026-08-24 |
| 适用范围 | 登录、CSRF、限流、会话恢复与失效、授权、退出 |
| 需求来源 | `PRD.md`、`API_CONTRACT.md`、`USER_STORIES.md` |

> 本文描述目标交互，用于产品、前端、后端和测试评审，不表示所有参与方均已完成开发。为了清晰表达不同结果，每张图只描述一个具体场景。

## 1. 参与方说明

| 参与方 | 职责 |
| --- | --- |
| `User` | 收银员、管理员或未登录访问者 |
| `WebApp` | 运行于浏览器，负责页面、认证状态和路由；浏览器自动管理 `HttpOnly` Cookie |
| `AuthAPI` | 认证接口、会话校验和统一安全边界 |
| `AccountRepo` | 查询最小账号认证字段、状态与角色 |
| `SessionRepo` | 创建、查询、刷新和撤销服务端会话 |
| `RateLimiter` | 按来源 IP 与规范化账号标识管理登录失败限制 |
| `Policy` | 后端接口角色策略与默认拒绝规则 |
| `AuditLog` | 记录脱敏安全事件及 `traceId` |

## 2. 成功登录

```mermaid
sequenceDiagram
    title 成功登录并进入授权页面
    participant User
    participant WebApp
    participant AuthAPI
    participant AccountRepo
    participant SessionRepo
    participant AuditLog

    User->>WebApp: 打开登录页
    WebApp->>AuthAPI: GET /api/v1/auth/csrf
    AuthAPI-->>WebApp: 200 Header 名称与 Token
    User->>WebApp: 输入账号和密码
    WebApp->>AuthAPI: POST /api/v1/auth/login 与 CSRF Header
    AuthAPI->>AccountRepo: 查询规范化账号与角色
    AccountRepo-->>AuthAPI: 最小认证字段
    AuthAPI->>AuthAPI: 校验状态与密码
    AuthAPI->>SessionRepo: 保存新会话 Token 摘要
    SessionRepo-->>AuthAPI: 会话创建成功
    AuthAPI->>AuditLog: AUTH_LOGIN_SUCCEEDED
    AuthAPI-->>WebApp: 200 CurrentUser 与 Set-Cookie
    WebApp-->>User: 渲染原目标或角色默认页
```

登录请求携带内存中的 `X-XSRF-TOKEN`。浏览器保存原始随机会话 Token，数据库只保存不可反推的摘要。返回地址只有在属于安全站内路径且当前角色有权访问时才恢复，否则进入 `/pos` 或 `/dashboard`。

## 3. 认证失败与登录限流

账号不存在、密码错误和账号停用走等价校验路径，对客户端保持统一反馈。

```mermaid
sequenceDiagram
    title 普通凭证认证失败
    participant User
    participant WebApp
    participant AuthAPI
    participant RateLimiter
    participant AccountRepo
    participant AuditLog

    User->>WebApp: 提交错误或不可用凭证
    WebApp->>AuthAPI: POST /api/v1/auth/login
    AuthAPI->>RateLimiter: 查询当前限制状态
    RateLimiter-->>AuthAPI: 尚未限制
    AuthAPI->>AccountRepo: 查询最小认证字段
    AccountRepo-->>AuthAPI: 账号记录或空结果
    AuthAPI->>AuthAPI: 执行等价密码校验
    AuthAPI->>RateLimiter: 记录本次失败
    RateLimiter-->>AuthAPI: 未达到第 5 次失败
    AuthAPI->>AuditLog: AUTH_LOGIN_FAILED
    AuthAPI-->>WebApp: 401 AUTH-401-002
    WebApp-->>User: 保留账号并清空密码
```

同一组合继续失败并达到第 5 次时进入限制：

```mermaid
sequenceDiagram
    title 第 5 次失败启动登录限制
    participant User
    participant WebApp
    participant AuthAPI
    participant RateLimiter
    participant AuditLog

    User->>WebApp: 提交第 5 次失败
    WebApp->>AuthAPI: POST /api/v1/auth/login
    AuthAPI->>RateLimiter: 查询当前限制状态
    RateLimiter-->>AuthAPI: 尚未限制
    AuthAPI->>RateLimiter: 记录并启动 15 分钟限制
    AuthAPI->>AuditLog: AUTH_LOGIN_RATE_LIMITED
    AuthAPI-->>WebApp: 429 与 Retry-After
    WebApp-->>User: 暂停提交并提示稍后重试
```

字段为空或超长时返回 `400 / COMMON-400-001`，不进入密码验证，也不累计失败次数。限制期间的请求继续返回 `429`，但不延长限制窗口；成功登录会清除对应组合的失败状态。

## 4. CSRF 校验失败与一次恢复

```mermaid
sequenceDiagram
    title CSRF Token 失效后的有限恢复
    participant User
    participant WebApp
    participant AuthAPI
    participant AuditLog

    User->>WebApp: 提交登录或退出
    WebApp->>AuthAPI: 状态变更请求与旧 Token
    AuthAPI->>AuditLog: AUTH_CSRF_REJECTED
    AuthAPI-->>WebApp: 403 AUTH-403-002
    WebApp->>WebApp: 检查恢复重试预算
    WebApp->>AuthAPI: GET /api/v1/auth/csrf
    AuthAPI-->>WebApp: 200 新 Token
    WebApp->>AuthAPI: 原请求仅重试一次
    AuthAPI-->>WebApp: 最终业务响应
    WebApp-->>User: 展示成功或安全校验失败
```

该恢复只处理 Token 轮换或页面持有旧 Token 等可恢复情况。若已经重试过，或者重新获取后仍返回 `AUTH-403-002`，客户端必须停止自动重试，提示刷新页面并保留 `traceId`。CSRF Token 只保存在内存中，不写入 URL 或日志。

## 5. 应用启动与会话恢复

```mermaid
sequenceDiagram
    title 刷新页面后恢复有效会话
    participant User
    participant WebApp
    participant AuthAPI
    participant SessionRepo
    participant AccountRepo

    User->>WebApp: 打开或刷新应用
    WebApp-->>User: 显示全局加载状态
    WebApp->>AuthAPI: GET /api/v1/auth/me，浏览器附带 Cookie
    AuthAPI->>SessionRepo: 按 Token 摘要查询会话
    SessionRepo-->>AuthAPI: 返回有效会话
    AuthAPI->>AccountRepo: 查询账号状态与角色
    AccountRepo-->>AuthAPI: ACTIVE 与角色集合
    AuthAPI->>AuthAPI: 校验空闲和绝对时限
    AuthAPI->>SessionRepo: 乐观刷新最近活动时间
    SessionRepo-->>AuthAPI: 刷新成功
    AuthAPI-->>WebApp: 200 CurrentUser
    WebApp->>WebApp: 建立内存身份与过滤导航
    WebApp-->>User: 渲染有权目标页面
```

会话 Cookie 为 `HttpOnly`，因此 WebApp 不读取其内容，只通过 `/auth/me` 结果建立内存身份。确认完成前不得渲染受保护内容。网络或服务异常应进入可重试状态，不能伪装成未登录或凭证错误。

## 6. 运行中会话失效

```mermaid
sequenceDiagram
    title 过期撤销或停用导致会话失效
    participant User
    participant WebApp
    participant AuthAPI
    participant SessionRepo
    participant AccountRepo
    participant AuditLog

    User->>WebApp: 发起受保护操作
    WebApp->>AuthAPI: 业务请求，浏览器附带 Cookie
    AuthAPI->>SessionRepo: 查询并检查会话状态
    SessionRepo-->>AuthAPI: 会话记录
    AuthAPI->>AccountRepo: 查询当前账号状态
    AccountRepo-->>AuthAPI: ACTIVE 或 DISABLED
    AuthAPI->>AuthAPI: 判断撤销过期或停用
    AuthAPI->>SessionRepo: 必要时撤销当前会话
    AuthAPI->>AuditLog: 记录具体内部失效事件
    AuthAPI-->>WebApp: 401 AUTH-401-001 与清除 Cookie
    WebApp->>WebApp: 合并并发 401 并清除身份
    WebApp-->>User: 登录页与一次过期提示
```

30 分钟空闲过期、登录后 8 小时绝对过期、已撤销会话和停用账号均对客户端保持同一 `401 / AUTH-401-001` 边界。失效请求不得读取或修改业务数据；内部仍分别记录过期、撤销或停用事件。

## 7. 角色不足与后端独立拒绝

```mermaid
sequenceDiagram
    title 已登录用户访问无权资源
    participant User
    participant WebApp
    participant AuthAPI
    participant SessionRepo
    participant Policy
    participant AuditLog

    User->>WebApp: 直接访问管理员页面
    WebApp->>WebApp: 使用当前角色执行路由校验
    WebApp-->>User: 显示 403 页面
    User->>AuthAPI: 直接构造管理员接口请求
    AuthAPI->>SessionRepo: 校验当前会话
    SessionRepo-->>AuthAPI: 有效身份与角色
    AuthAPI->>Policy: 查询接口所需角色
    Policy-->>AuthAPI: 角色不足或规则未声明
    AuthAPI->>AuditLog: AUTH_ACCESS_DENIED
    AuthAPI-->>User: 403 AUTH-403-001
```

前端菜单和路由只负责体验，后端是最终权限权威。业务接口未声明策略时默认拒绝；不存在的资源返回 `404 / COMMON-404-001`，不能一律伪装成 `403`。

## 8. 主动退出与旧会话失效

```mermaid
sequenceDiagram
    title 主动退出当前会话
    participant User
    participant WebApp
    participant AuthAPI
    participant SessionRepo
    participant AuditLog

    User->>WebApp: 点击退出
    WebApp->>AuthAPI: POST /api/v1/auth/logout，Cookie 与 CSRF Header
    AuthAPI->>AuthAPI: 校验 CSRF 与当前会话
    AuthAPI->>SessionRepo: 撤销当前会话
    SessionRepo-->>AuthAPI: 撤销完成或已不存在
    AuthAPI->>AuditLog: AUTH_LOGOUT_SUCCEEDED
    AuthAPI-->>WebApp: 200 与过期 Set-Cookie
    WebApp->>WebApp: 清除内存身份
    WebApp-->>User: 返回登录页
    User->>AuthAPI: 重放旧 Cookie 请求
    AuthAPI-->>User: 401 AUTH-401-001
```

退出只撤销当前会话，不影响同账号的其他并发会话，并且重复退出保持 `200` 幂等。若后端不可达，WebApp 必须保留当前状态并允许重试，不能假装退出成功。

## 9. 时序与需求追踪

| 时序场景 | User Story | 关键接口或结果 |
| --- | --- | --- |
| 成功登录 | `US-S2-AUTH-01`、`US-S2-SEC-01` | CSRF、`POST /auth/login`、`CurrentUser`、新会话 |
| 认证失败与限流 | `US-S2-AUTH-02`、`US-S2-AUDIT-01` | `400`、`AUTH-401-002`、`AUTH-429-001` |
| CSRF 有限恢复 | `US-S2-AUTH-01`、`US-S2-SESSION-03` | `AUTH-403-002`、最多重新获取并重试一次 |
| 应用启动恢复 | `US-S2-SESSION-01` | `GET /auth/me`、全局加载、`CurrentUser` |
| 运行中会话失效 | `US-S2-SESSION-02`、`US-S2-AUDIT-01` | `AUTH-401-001`、清除 Cookie、单次过期提示 |
| 角色不足 | `US-S2-AUTHZ-02`、`US-S2-AUTHZ-03` | 前端 403、后端 `AUTH-403-001`、默认拒绝 |
| 主动退出 | `US-S2-SESSION-03`、`US-S2-AUDIT-01` | `POST /auth/logout`、撤销、清除 Cookie、幂等 |

所有认证响应沿用统一响应结构和 `traceId`。日志与响应不得包含密码、密码摘要、原始或摘要会话 ID、Cookie、CSRF Token、完整请求头或完整认证请求体。
