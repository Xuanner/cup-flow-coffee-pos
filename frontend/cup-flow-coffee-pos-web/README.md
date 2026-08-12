# Cup Flow Coffee POS Web

`US-S1-FE-01 可运行的前端骨架`交付。工程位于全栈仓库的 `frontend/cup-flow-coffee-pos-web/`，当前只提供应用入口、全局布局和六个核心模块边界，不包含真实业务功能。

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
VITE_API_BASE_URL=/api
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

## 依赖升级

1. 在独立变更中修改直接依赖，禁止一次跨多个主版本。
2. 查阅 Node、Vite、React、Tailwind 和测试工具迁移说明。
3. 使用 `npm install --save-exact <package>@<version>` 更新并提交 `package-lock.json`。
4. 执行 `npm run check`。
5. 在干净环境再次执行 `npm ci && npm run check`。
