# Cup Flow Coffee POS Web

Sprint 1 前端基础交付。工程位于全栈仓库的 `frontend/cup-flow-coffee-pos-web/`，包含可运行骨架、统一请求与状态基础，以及设计系统通用组件；暂不包含真实业务功能。

## 环境基线

- Node.js `24.19.0`
- npm `11.17.0`

`.nvmrc`、`.node-version`、`package.json#engines` 和 `package.json#packageManager` 共同固定运行环境。禁止在本工程混用 pnpm、Yarn 或其他 lockfile。

```bash
nvm use
node --version
npm --version
npm ci
```

## 常用命令

```bash
npm run dev          # 本地开发服务器 http://localhost:5173
npm run build        # TypeScript 检查并生成生产构建
npm run preview      # 预览生产构建 http://localhost:4173
npm run typecheck    # TypeScript 静态类型检查
npm run lint         # ESLint 静态检查
npm run format:check # Prettier 格式检查
npm run test         # Vitest 基础测试
npm run test:coverage
npm run check        # 执行全部质量门禁
```

## 环境配置

复制 `.env.example` 为 `.env.local`，按本地后端环境设置：

```dotenv
VITE_API_BASE_URL=/api/v1
```

只允许将非敏感的前端配置暴露为 `VITE_*`。生产地址、Token 和凭证不得提交仓库。

## 目录边界

```text
src/
├── app/               # 应用入口、Provider 和路由
├── components/
│   ├── layout/        # 全局布局和页面骨架
│   └── ui/            # 无业务规则的通用组件
├── features/
│   ├── auth/          # 登录
│   ├── pos/           # POS 与收款
│   ├── orders/        # 订单
│   ├── products/      # 商品
│   ├── dashboard/     # 经营看板
│   └── system/        # 全局状态、健康检查与异常页
├── lib/
│   ├── api/           # 原生 fetch 基础封装
│   └── query/         # TanStack Query 配置
├── state/             # Zustand 跨页面客户端状态
├── styles/            # Tailwind 入口和设计 Token
└── test/              # 测试环境配置
```

## 状态职责

- TanStack Query：后端数据、请求状态和缓存。
- Zustand：购物车等跨页面客户端状态；当前只提供移动导航示例。
- React state：组件局部交互状态。
- URL：分页、筛选、日期等可分享状态。
- React Hook Form + Zod：在首个真实表单故事中接入。

当前 Zod 同时校验前端环境配置；表单能力将在首个真实表单故事中接入。

## 请求与错误基础

`apiRequest` 将 HTTP、网络与超时失败统一转换为 `ApiError`：

| 来源                          | `ApiError.category` |
| ----------------------------- | ------------------- |
| HTTP 400 / 422                | `validation`        |
| HTTP 401                      | `unauthenticated`   |
| HTTP 403                      | `forbidden`         |
| HTTP 409                      | `conflict`          |
| HTTP 5xx                      | `server`            |
| `fetch` 网络失败              | `network`           |
| 请求超过内部超时时间          | `timeout`           |
| 调用方通过 `AbortSignal` 取消 | `cancelled`         |

本地后端地址为 `http://localhost:8080/api/v1/health`。开发服务器将前端请求 `GET /api/v1/health` 代理到该地址，生产环境通过 `VITE_API_BASE_URL` 覆盖。当前约定最小成功响应为：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "application": "UP",
    "database": "UP"
  },
  "traceId": "dfacef6b5eb74be28854c0c46ae610ce",
  "timestamp": "2026-08-18T03:37:47.291437Z"
}
```

前端公共请求层统一解析 `code`、`message`、`data`、`traceId` 和 `timestamp`；健康模块再校验 `data.application` 与 `data.database`。任一状态为 `DOWN`、非 2xx 或响应格式无效都会进入失败状态，并允许用户手动重新检查。

## 样式职责

- Tailwind CSS 4：布局、响应式和原子化样式。
- Radix UI Primitives：交互语义、焦点管理和可访问性。
- `src/styles/tokens.css`：来自 `doc/sprint1/handoff/tokens/tokens.css` 的设计 Token 快照，是视觉值的唯一来源。
- `@theme inline`：只把 CSS Variable 映射为语义化 Tailwind Utility，不复制色值。

设计 Token 更新后，先在仓库根目录运行生成与校验，再同步快照：

```bash
node doc/sprint1/handoff/tokens/generate-tokens.mjs
node doc/sprint1/handoff/tokens/verify-token-map.mjs
cp doc/sprint1/handoff/tokens/tokens.css frontend/cup-flow-coffee-pos-web/src/styles/tokens.css
```

## 通用组件

`src/components/ui/` 提供 Button、Input、Search、Select、Badge、Table、Pagination、Dialog、Toast、EmptyState、Spinner、Skeleton 和 Progress。组件遵循 `doc/sprint1/components/cup-flow-components.svg` 及 `RADIX_MAPPING.md`，包含统一的 Focus Visible、禁用、加载和错误状态。

开发环境访问 `/system/components` 可查看全部组件及状态。交互测试覆盖键盘焦点、加载防重复提交、字段错误关联、禁用语义、Dialog Esc 关闭与焦点归还、分页当前页和 Toast 动作播报。

## 依赖升级

1. 在独立变更中修改直接依赖，禁止一次跨多个主版本。
2. 查阅 Node、Vite、React、Tailwind 和测试工具迁移说明。
3. 使用 `npm install --save-exact <package>@<version>` 更新并提交 `package-lock.json`。
4. 执行 `npm run check`。
5. 在干净环境再次执行 `npm ci && npm run check`。
