# Cup Flow Coffee POS — Sprint 2 登录体系流程图

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 状态 | Sprint 2 已批准需求的流程基线 |
| 基线日期 | 2026-08-24 |
| 适用范围 | 登录、会话、退出、角色权限与安全失败处理 |
| 需求来源 | `PRD.md`、`API_CONTRACT.md`、`USER_STORIES.md` |

> 本文描述目标行为，用于产品、研发与测试评审，不表示所有节点均已完成开发。

## 1. 登录体系总览

```mermaid
flowchart LR
    visit([用户访问系统]) --> boot[应用启动并确认身份]
    boot --> sessionValid{当前会话有效}
    sessionValid -->|否| loginPage[进入登录页]
    loginPage --> authenticate[提交账号和密码]
    authenticate --> loginSuccess{认证成功}
    loginSuccess -->|否| loginPage
    loginSuccess -->|是| createSession[建立服务端会话]
    sessionValid -->|是| authorizeTarget[校验目标资源权限]
    createSession --> authorizeTarget
    authorizeTarget --> targetAllowed{目标存在且有权访问}
    targetAllowed -->|是| targetPage[进入原目标页面]
    targetAllowed -->|否| roleHome[进入角色默认页面]
    targetPage --> protectedAccess[访问受保护页面或接口]
    roleHome --> protectedAccess
    protectedAccess --> guard{会话和角色校验通过}
    guard -->|是| resource[返回业务资源]
    guard -->|会话失效| loginPage
    guard -->|角色不足| forbidden[返回 403]
    resource --> logout[用户主动退出]
    logout --> loginPage

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class targetPage,roleHome,resource success
    class forbidden failure
    class sessionValid,loginSuccess,targetAllowed,guard decision
    class boot,authenticate,createSession,authorizeTarget,protectedAccess,logout security
```

角色默认页面：`CASHIER` 为 `/pos`，`ADMIN` 为 `/dashboard`。`ADMIN` 继承 `CASHIER` 的业务权限。

## 2. 登录认证流程

```mermaid
flowchart TD
    loginStart([打开登录页]) --> getCsrf[获取 CSRF Token]
    getCsrf --> inputCredentials[输入账号和密码]
    inputCredentials --> clientValid{客户端格式校验通过}
    clientValid -->|否| fieldError[显示字段错误并允许修改]
    fieldError --> inputCredentials
    clientValid -->|是| submitLogin[携带 CSRF Token 提交登录]
    submitLogin --> csrfValid{CSRF 校验通过}
    csrfValid -->|否| csrfRejected[返回 403 AUTH-403-002]
    csrfRejected --> refreshCsrf[重新获取一次 Token 后重试]
    refreshCsrf --> submitLogin
    csrfValid -->|是| blocked{来源与账号组合正在限流}
    blocked -->|是| rateLimited[返回 429 AUTH-429-001]
    blocked -->|否| credentialsValid{账号有效且密码正确}
    credentialsValid -->|否| recordFailure[记录本次失败]
    recordFailure --> fifthFailure{达到第 5 次失败}
    fifthFailure -->|否| authFailed[返回 401 AUTH-401-002]
    fifthFailure -->|是| startBlock[启动 15 分钟限制并返回 429]
    authFailed --> inputCredentials
    startBlock --> inputCredentials
    credentialsValid -->|是| clearFailures[清除失败计数]
    clearFailures --> persistSession[生成随机 Token 并持久化摘要]
    persistSession --> setCookie[设置 HttpOnly 会话 Cookie]
    setCookie --> returnTarget{原目标地址安全且有权访问}
    returnTarget -->|是| originalPage[进入原目标页面]
    returnTarget -->|否| defaultPage[进入角色默认页面]

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class originalPage,defaultPage success
    class fieldError,csrfRejected,rateLimited,authFailed,startBlock failure
    class clientValid,csrfValid,blocked,credentialsValid,fifthFailure,returnTarget decision
    class getCsrf,submitLogin,clearFailures,persistSession,setCookie security
```

关键规则：

- 账号先去除首尾空白，长度为 1–64 个字符且区分大小写；密码保持原值，长度为 1–128 个字符。
- 格式不合法返回 `400`，不进行密码校验，也不计入登录失败次数。
- 账号不存在、密码错误和账号停用统一返回 `401 / AUTH-401-002`，避免泄露账号状态。
- 限流维度为“来源 IP + 规范化账号标识”。前 4 次失败返回 `401`，第 5 次起进入 15 分钟限制并返回 `429 / AUTH-429-001`；限制期内的尝试不延长窗口。
- 浏览器 Cookie 保存随机原始 Token，数据库仅保存其 SHA-256 摘要。Cookie 名为 `CUP_FLOW_SESSION`，使用 `HttpOnly`、`SameSite=Lax`、`Path=/`，生产环境启用 `Secure`。
- 返回地址仅接受以单个 `/` 开头的站内路径；`//`、协议、主机、反斜杠和控制字符均视为无效。

## 3. 启动恢复与运行中会话失效

```mermaid
flowchart TD
    appStart([打开或刷新应用]) --> loading[显示全局加载状态]
    loading --> requestMe[请求 GET /api/v1/auth/me]
    requestMe --> requestResult{请求结果}
    requestResult -->|网络或服务异常| recoverableError[保留未知状态并提供重试]
    recoverableError --> requestMe
    requestResult -->|返回 401| silentLogin[进入登录页且不显示过期提示]
    requestResult -->|返回当前用户| renderApp[恢复身份并渲染授权界面]
    renderApp --> runtimeRequest[发起受保护请求]
    runtimeRequest --> runtimeResult{响应结果}
    runtimeResult -->|成功| continueWork[继续业务操作]
    runtimeResult -->|401 AUTH-401-001| singleFlight[合并并发失效处理]
    singleFlight --> clearSession[清除 Cookie 和内存身份]
    clearSession --> expiredLogin[进入登录页并仅提示一次失效]
    runtimeResult -->|其他错误| normalError[按业务或系统错误处理]

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class renderApp,continueWork success
    class recoverableError,expiredLogin,normalError failure
    class requestResult,runtimeResult decision
    class loading,requestMe,clearSession,singleFlight security
```

服务端校验会话时，以 Token 摘要查询持久化记录，并检查撤销状态、30 分钟空闲时限、登录后 8 小时绝对时限以及账号是否仍为 `ACTIVE`。校验通过的受保护请求刷新最近活动时间，但不会延长绝对时限。

## 4. 受保护页面与接口授权

```mermaid
flowchart TD
    request([访问页面或接口]) --> publicResource{资源公开}
    publicResource -->|是| allowPublic[允许访问]
    publicResource -->|否| validSession{会话有效}
    validSession -->|否| unauthenticated[页面转登录或接口返回 401]
    validSession -->|是| resourceExists{资源存在}
    resourceExists -->|否| notFound[返回 404]
    resourceExists -->|是| policyDeclared{已声明访问策略}
    policyDeclared -->|否| defaultDeny[默认拒绝并返回 403]
    policyDeclared -->|是| roleAllowed{角色满足要求}
    roleAllowed -->|否| forbiddenAccess[页面或接口返回 403]
    roleAllowed -->|是| allowProtected[允许访问]
    forbiddenAccess --> auditDenied[记录拒绝事件与 traceId]
    defaultDeny --> auditDenied

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class allowPublic,allowProtected success
    class unauthenticated,notFound,defaultDeny,forbiddenAccess failure
    class publicResource,validSession,resourceExists,policyDeclared,roleAllowed decision
    class auditDenied security
```

前端路由和菜单用于改善体验，后端鉴权是最终安全边界。未分类业务接口默认拒绝；“资源不存在”的 `404` 与“角色不足”的 `403 / AUTH-403-001` 必须保持可区分。

| 能力 | 未登录 | `CASHIER` | `ADMIN` |
| --- | --- | --- | --- |
| 登录、CSRF | 允许 | 允许 | 允许 |
| POS、订单 | 拒绝 | 允许 | 允许 |
| 商品、经营看板 | 拒绝 | 拒绝 | 允许 |

## 5. 主动退出流程

```mermaid
flowchart TD
    clickLogout([用户点击退出]) --> submitLogout[携带 CSRF Token 提交退出]
    submitLogout --> logoutResult{请求结果}
    logoutResult -->|CSRF 失败| csrfError[返回 403 并允许安全重试]
    logoutResult -->|网络或服务异常| retryLogout[保留当前状态并允许重试]
    logoutResult -->|请求有效| revokeCurrent[撤销当前服务端会话]
    revokeCurrent --> clearCookie[通过 Set-Cookie 清除会话 Cookie]
    clearCookie --> logoutOk[返回 200]
    logoutOk --> clearClient[清除前端内存身份]
    clearClient --> loginAgain[进入登录页]
    loginAgain --> oldTokenRequest[旧 Cookie 再次访问受保护资源]
    oldTokenRequest --> rejected[返回 401 且不能恢复会话]

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class logoutOk,loginAgain success
    class csrfError,retryLogout,rejected failure
    class logoutResult decision
    class submitLogout,revokeCurrent,clearCookie,clearClient security
```

退出仅撤销当前会话并保持幂等，不影响同一账号的其他并发会话。后端不可达时，前端不能伪装退出成功。

## 6. 会话生命周期

```mermaid
flowchart LR
    loginOk([登录成功]) --> activeSession[会话有效]
    activeSession --> validRequest[受保护请求校验通过]
    validRequest --> refreshActivity[刷新最近活动时间]
    refreshActivity --> activeSession
    activeSession --> idleExpired{空闲达到 30 分钟}
    activeSession --> absoluteExpired{登录达到 8 小时}
    activeSession --> manualLogout{当前会话主动退出}
    activeSession --> accountDisabled{账号被停用}
    idleExpired --> invalidSession[会话立即失效]
    absoluteExpired --> invalidSession
    manualLogout --> invalidSession
    accountDisabled --> revokeOnRequest[下次受保护请求撤销会话]
    revokeOnRequest --> invalidSession
    invalidSession --> retainRecord[记录保留 7 天]
    retainRecord --> batchDelete[定时任务批量删除]

    classDef success fill:#CDF4D3,stroke:#66D575,color:#172B1C
    classDef failure fill:#FFCDC2,stroke:#FF7556,color:#3D1710
    classDef decision fill:#FFECBD,stroke:#FFC943,color:#3D2D00
    classDef security fill:#C2E5FF,stroke:#3DADFF,color:#102B3D
    class activeSession,validRequest success
    class invalidSession failure
    class idleExpired,absoluteExpired,manualLogout,accountDisabled decision
    class refreshActivity,revokeOnRequest,retainRecord,batchDelete security
```

浏览器关闭后，因为会话 Cookie 不持久化，浏览器侧登录态结束；服务端记录仍按自身生命周期和清理规则管理。

## 7. 接口与失败结果速查

| 场景 | 接口或入口 | 结果 |
| --- | --- | --- |
| 获取安全令牌 | `GET /api/v1/auth/csrf` | 返回 CSRF Header 名称和 Token，不建立登录会话 |
| 登录成功 | `POST /api/v1/auth/login` | `200`、返回 `CurrentUser`、设置会话 Cookie |
| 登录凭证失败 | `POST /api/v1/auth/login` | `401 / AUTH-401-002` |
| 登录限流 | `POST /api/v1/auth/login` | `429 / AUTH-429-001`，包含 `Retry-After` |
| 查询当前身份 | `GET /api/v1/auth/me` | 有效会话返回 `CurrentUser`；无效会话返回 `401 / AUTH-401-001` |
| 角色不足 | 受保护页面或接口 | `403 / AUTH-403-001` |
| CSRF 或 Origin 失败 | 登录或退出 | `403 / AUTH-403-002` |
| 主动退出 | `POST /api/v1/auth/logout` | 撤销当前会话、清除 Cookie、幂等返回 `200` |

## 8. User Story 追踪

| 流程 | 覆盖的 User Story |
| --- | --- |
| 登录认证 | `US-S2-AUTH-01`、`US-S2-AUTH-02`、`US-S2-SEC-01` |
| 启动恢复与会话失效 | `US-S2-SESSION-01`、`US-S2-SESSION-02` |
| 主动退出 | `US-S2-SESSION-03` |
| 页面与接口授权 | `US-S2-AUTHZ-01`、`US-S2-AUTHZ-02`、`US-S2-AUTHZ-03` |
| 安全事件记录 | `US-S2-AUDIT-01`，横切全部认证、会话和授权流程 |

安全事件必须包含可关联的 `traceId`，但不得记录密码、Cookie、原始 Token、密码哈希或其他凭证材料。
