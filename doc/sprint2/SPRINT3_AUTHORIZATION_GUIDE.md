# Sprint 3 业务接口鉴权接入说明

Sprint 3 新增 POS、订单、商品或看板接口时，必须复用 Sprint 2 的服务端会话与授权入口。

## Controller 声明

每个生产 Controller 方法必须且只能按实际需要选择一种访问声明：

- `@PublicEndpoint`：仅用于经过评审的公开入口，不得用于普通业务 API。
- `@AuthenticatedEndpoint`：任意有效员工会话可访问。
- `@RequiresRole(EndpointRole.CASHIER)`：收银员与管理员均可访问。
- `@RequiresRole(EndpointRole.ADMIN)`：仅管理员可访问。

POS、订单操作默认使用 `CASHIER`；商品维护与经营看板默认使用 `ADMIN`。新增公开入口必须同步更新
API 契约、安全评审、正反权限测试和公开白名单证据。

## 应用服务与当前身份

Controller 通过 `CurrentUserContext.requireCurrentUser()` 取得服务端确认的账号 ID 与角色，不接受
请求 Header、查询参数或请求体中的角色和账号 ID 作为授权依据。拦截器会在 Controller 之前完成
会话和角色判断，并在请求结束清理线程上下文。

业务写入需要操作者时，把当前账号 ID 显式传给应用服务，业务模块不得读取 Session Cookie、调用
会话 Mapper 或自行解析 Token。

## 测试门禁

每个新增接口至少提供：匿名 401、最低允许角色成功、角色不足 403、拒绝时应用服务/数据库零调用、
伪造客户端角色不提权，以及响应 `traceId` 和敏感字段负断言。生产端点漏标访问声明会由
`ArchitectureTest.everyControllerEndpointDeclaresItsAccessRule` 阻止。

权限拒绝由统一入口记录 `AUTHORIZATION_DENIED`，业务代码不得记录 Cookie、Session/CSRF Token、
密码、密码摘要、完整 Header 或认证请求体。
