# Cup Flow Coffee POS — Sprint 2 认证 API 与安全契约

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 状态 | 已批准，作为 Sprint 2 前后端实现基线 |
| 关联任务 | TASK-S2-PLAN-02 |
| 生效日期 | 2026-08-24 |

## 1. 契约范围

本文冻结登录、当前身份、退出、Cookie、CSRF、错误码和安全返回地址。所有接口沿用 `/api/v1`、统一响应结构和 `X-Request-Id`/`traceId` 约定。

Sprint 2 不提供注册、密码修改/重置、账号管理、角色切换、记住登录或远程下线 API。

## 2. 已冻结安全参数

| 参数 | 决定 |
| --- | --- |
| 会话形态 | 服务端持久化的不透明会话；浏览器仅持有随机会话 ID Cookie |
| 空闲过期 | 30 分钟；受保护请求通过后刷新最近活动时间 |
| 绝对过期 | 登录成功起 8 小时，不因活动延长 |
| 浏览器关闭 | 会话 Cookie 不设置 `Max-Age`/`Expires`，关闭浏览器会话后不自动恢复 |
| 并发会话 | Sprint 2 允许同一账号存在多个会话；退出只撤销当前会话 |
| 停用账号 | 下一次受保护请求立即拒绝并撤销当前会话 |
| 物理清理 | 过期或撤销即时失效，记录保留 7 天后由应用定时任务批量删除 |
| 登录限流 | 同一“来源 IP + 规范化账号标识”15 分钟内第 5 次失败起限制 15 分钟 |
| 管理员默认页 | `/dashboard` |
| 收银员默认页 | `/pos` |
| 初始化凭证 | 通过部署 Secret/环境变量注入；仓库不保存可用明文密码 |

登录限流补充规则：

- 前 4 次失败返回统一认证失败；第 5 次及限制期间返回 429。
- 限制期从第 5 次失败开始计算，期间请求不延长限制期。
- 成功登录清除对应组合的失败状态；服务重启后的状态持久性属于实现设计，但不得削弱接口行为测试。
- 只信任部署明确配置的反向代理来源头；否则使用直接连接来源，禁止任意客户端伪造 IP 绕过限制。
- 429 响应包含整数秒 `Retry-After` Header，但不返回剩余失败次数。

## 3. 通用响应

成功和失败均使用：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "traceId": "01J...",
  "timestamp": "2026-08-24T08:00:00Z"
}
```

规则：

- `timestamp` 为 UTC ISO-8601 时刻。
- `traceId` 与响应 Header `X-Request-Id` 一致。
- 认证响应不得返回密码、密码摘要、原始会话 ID、Cookie 内容或内部授权规则。
- 错误时 `data` 通常为 `null`；字段校验失败可使用既有字段错误数组。
- 客户端以 HTTP 状态和稳定 `code` 判断类别，不匹配内部异常文本。

## 4. 数据模型

### 4.1 当前用户 `CurrentUser`

```json
{
  "id": "6dff38d8-1e6f-4d39-910d-a20e47b2b800",
  "displayName": "门店管理员",
  "roles": ["ADMIN"],
  "defaultPath": "/dashboard"
}
```

约束：

- `id` 为账号 UUID 字符串。
- `displayName` 用于全局账号区域展示。
- `roles` 只包含已批准的 `CASHIER`、`ADMIN`，顺序不表达优先级。
- `ADMIN` 在授权计算中包含 `CASHIER` 权限；响应无需重复返回 `CASHIER`。
- 多角色按权限并集；`defaultPath` 优先选择管理员默认页。
- 不返回 `username`、账号状态、密码字段或会话过期内部时间。

### 4.2 登录请求 `LoginRequest`

```json
{
  "username": "cashier01",
  "password": "example-only-not-a-real-password"
}
```

字段规则：

| 字段 | 规则 |
| --- | --- |
| `username` | 必填；字符串；去除首尾空白后 1–64 字符；大小写敏感 |
| `password` | 必填；字符串；1–128 字符；不去空白、不改变大小写或 Unicode 内容 |

超过上限或空字段返回字段校验错误，不进入密码验证或失败次数累计。

## 5. 接口

### 5.1 获取 CSRF Token

`GET /api/v1/auth/csrf`

用途：登录前和 Token 轮换后获取状态变更请求所需的 CSRF Token。端点允许未登录访问，不建立登录态。

成功 `200`：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "headerName": "X-XSRF-TOKEN",
    "token": "csrf-token-value"
  },
  "traceId": "01J...",
  "timestamp": "2026-08-24T08:00:00Z"
}
```

客户端只在内存中持有 CSRF Token，并在后续状态变更请求的 `X-XSRF-TOKEN` Header 发送。CSRF Token 不是登录凭证，但仍不得写入 URL或日志。

### 5.2 登录

`POST /api/v1/auth/login`

Headers：

- `Content-Type: application/json`
- `X-XSRF-TOKEN: <token>`

成功 `200`：`data` 为 `CurrentUser`，同时通过 `Set-Cookie` 建立会话。

失败：

- 字段无效：`400 / COMMON-400-001`。
- 账号不存在、密码错误或停用：`401 / AUTH-401-002`。
- CSRF 无效：`403 / AUTH-403-002`。
- 达到登录限制：`429 / AUTH-429-001`，同时返回 `Retry-After`。
- 未知异常：`500 / COMMON-500-001`。

成功登录必须生成全新会话，不能复用登录前客户端提供的会话标识。

### 5.3 查询当前身份

`GET /api/v1/auth/me`

成功 `200`：`data` 为 `CurrentUser`。

无 Cookie、伪造、已撤销、空闲过期、绝对过期或账号停用均返回 `401 / AUTH-401-001`。过期或停用路径同时清除 Cookie；停用不会在响应中暴露账号状态。

### 5.4 退出

`POST /api/v1/auth/logout`

Headers：`X-XSRF-TOKEN: <token>`。

成功或会话已经不存在时均返回 `200`：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": null,
  "traceId": "01J...",
  "timestamp": "2026-08-24T08:00:00Z"
}
```

服务端撤销当前会话并通过过期的 `Set-Cookie` 清除会话 Cookie。重复退出保持幂等；CSRF 无效仍返回 `403 / AUTH-403-002`。

## 6. Cookie 契约

### 6.1 会话 Cookie

| 属性 | 值 |
| --- | --- |
| 名称 | `CUP_FLOW_SESSION` |
| 内容 | 至少 256 bit 密码学安全随机值；服务端数据库只保存不可反推的摘要 |
| `HttpOnly` | `true` |
| `Secure` | 生产 `true`；本地 HTTP 开发环境 `false` |
| `SameSite` | `Lax` |
| `Path` | `/` |
| `Domain` | 不设置，使用 host-only Cookie |
| `Max-Age` / `Expires` | 登录时不设置；退出或失效清理时设为立即过期 |

前端 `fetch` 统一使用 `credentials: "same-origin"`；如后续改为不同 Origin，必须重新评审 CORS、Cookie 与 CSRF，不得只改为通配 CORS。

### 6.2 CSRF Token

- CSRF Token 与会话 Cookie 是不同值，不得互相复用。
- 登录、退出及后续所有状态变更请求必须校验 `X-XSRF-TOKEN`。
- Token 获取接口可公开，但恶意站点无法读取同源响应；服务端仍验证 Origin/同源策略。
- Token 轮换后，客户端丢弃旧值并重新获取；遇到 `AUTH-403-002` 时可刷新页面或重新获取一次后重试，禁止无限自动重试。

## 7. 错误码

| HTTP | code | 用户消息 | 使用场景 | 前端分类 |
| --- | --- | --- | --- | --- |
| 400 | `COMMON-400-001` | 请求参数不正确 | 空字段、长度或格式无效 | `validation` |
| 401 | `AUTH-401-001` | 登录状态已失效，请重新登录 | 缺少/无效/过期/撤销会话、账号停用后的受保护请求 | `unauthenticated` |
| 401 | `AUTH-401-002` | 账号或密码错误，或账号不可用 | 账号不存在、密码错误、停用账号登录 | `authenticationFailed` |
| 403 | `AUTH-403-001` | 没有执行此操作的权限 | 已登录但角色不足 | `forbidden` |
| 403 | `AUTH-403-002` | 请求安全校验失败，请刷新后重试 | CSRF/Origin 校验失败 | `securityValidation` |
| 404 | `COMMON-404-001` | 请求的资源不存在 | 不存在的 API | `notFound` |
| 429 | `AUTH-429-001` | 尝试次数过多，请稍后再试 | 登录限流 | `rateLimited` |
| 500 | `COMMON-500-001` | 服务暂时不可用，请稍后重试 | 未知服务异常 | `server` |

客户端处理：

- 全局 401 处理只针对 `AUTH-401-001`，并发响应只执行一次清理、跳转与过期提示。
- 登录端点的 `AUTH-401-002` 留在表单内处理，不触发全局“会话过期”。
- 403 不清除有效登录态；页面权限不足与 CSRF 失败分别处理。
- 429 显示可恢复提示；可使用 `Retry-After` 禁用提交，但不自行推算剩余失败次数。
- 网络失败/超时不等同于任何业务错误码。

## 8. 安全返回地址

返回地址仅用于未登录访问受保护页面后的恢复：

- 只接受以单个 `/` 开头的站内路径；拒绝 `//`、协议、主机、反斜杠、控制字符和解码后改变含义的输入。
- 登录成功后必须基于服务端返回角色再次验证目标权限。
- 目标不存在、无权限或验证失败时使用角色默认页。
- 返回地址不得被后端作为任意 `Location` 目标，也不得携带会话或 CSRF Token。

## 9. 页面与接口权限

| 能力 | 公开 | `CASHIER` | `ADMIN` |
| --- | --- | --- | --- |
| CSRF、登录、健康检查 | 允许 | 允许 | 允许 |
| 当前身份、退出 | 401/退出幂等 | 允许 | 允许 |
| POS、订单 | 401 | 允许 | 允许 |
| 商品、经营看板 | 401 | 403 | 允许 |
| 未声明业务接口 | 401 | 403 | 403 |

后端是最终权限权威；前端菜单与路由控制只负责体验。

## 10. 初始化账号配置契约

实现使用以下语义配置，实际 Spring 属性名可按项目约定映射，但含义不得变化：

| 配置 | 规则 |
| --- | --- |
| `AUTH_BOOTSTRAP_ENABLED` | 默认 `false`；需要初始化账号时显式启用 |
| `AUTH_BOOTSTRAP_CASHIER_USERNAME` | 收银员账号；去首尾空白后 1–64 字符 |
| `AUTH_BOOTSTRAP_CASHIER_PASSWORD` | 仅从 Secret 注入；12–128 字符 |
| `AUTH_BOOTSTRAP_CASHIER_DISPLAY_NAME` | 收银员展示名；1–64 字符 |
| `AUTH_BOOTSTRAP_ADMIN_USERNAME` | 管理员账号；规则同上 |
| `AUTH_BOOTSTRAP_ADMIN_PASSWORD` | 仅从 Secret 注入；12–128 字符 |
| `AUTH_BOOTSTRAP_ADMIN_DISPLAY_NAME` | 管理员展示名；1–64 字符 |

幂等规则：

- 仅在目标 username 不存在时创建账号并分配对应角色。
- username 已存在时不修改密码、状态、展示名或角色，并记录不含凭证的结果。
- 启用初始化但配置缺失/无效时应用启动失败，不生成弱默认账号。
- 文档和 `.env.example` 只能包含明显不可用的占位符，不能包含团队共享或生产可用密码。

## 11. 安全事件契约

事件类型固定为：

- `AUTH_LOGIN_SUCCEEDED`
- `AUTH_LOGIN_FAILED`
- `AUTH_LOGIN_RATE_LIMITED`
- `AUTH_SESSION_EXPIRED`
- `AUTH_SESSION_REVOKED`
- `AUTH_ACCOUNT_DISABLED`
- `AUTH_LOGOUT_SUCCEEDED`
- `AUTH_ACCESS_DENIED`
- `AUTH_CSRF_REJECTED`

每条结构化事件包含 `eventType`、`result`、服务器 `timestamp`、`traceId`，以及可用时的 `accountId` 和最小来源上下文。不得记录 password、passwordHash、原始/摘要 session ID、Cookie、CSRF Token、完整请求头或完整认证请求体。

## 12. 变更规则

下列变化必须先更新本文并由前端、后端、测试共同评审：

- 接口路径、方法、字段、Cookie 名称或属性。
- 会话与限流时限。
- CSRF/CORS/Origin 策略。
- 401、403、429 的业务码或客户端处理。
- 角色继承、默认页和公开白名单。
- 初始化凭证来源或幂等语义。
