# Cup Flow — Figma 与代码 Token 映射

## 范围

本文档只覆盖 `US-S1-UI-03 设计与代码 Token 映射`。仓库尚未搭建前端工程，因此交付使用无依赖的 CSS、TypeScript 和 manifest 文件；后续前端工程可直接复制或由构建流程消费这些文件。

## 来源与产物

| 层级 | 文件 | 维护方式 |
| --- | --- | --- |
| Figma 导出真源 | `../../foundations/color-primitives.json` | 由设计负责人从 Figma 导出并覆盖 |
| Figma 导出真源 | `../../foundations/color-semantics.json` | 由设计负责人从 Figma 导出并覆盖 |
| Figma 导出真源 | `../../foundations/dimensions.json` | 由设计负责人从 Figma 导出并覆盖 |
| Figma 导出真源 | `../../foundations/typography.json` | 由设计负责人从 Figma 导出并覆盖 |
| 生成规则 | `generate-tokens.mjs` | 前端 Token 负责人维护 |
| Web Variables | `tokens.css` | 自动生成，禁止手改 |
| TypeScript 引用 | `tokens.ts` | 自动生成，禁止手改 |
| 完整映射 | `token-manifest.json` | 自动生成，保留 Figma ID、Alias、Scope 和代码名 |
| 使用验证 | `examples.css`、`verification.html` | 前端 Token 负责人维护 |

## 命名映射规则

1. 优先采用 Figma 导出的 `com.figma.codeSyntax.WEB`，例如 `color/bg/canvas` → `--cf-color-bg-canvas`。
2. Figma Alias 在 CSS 中保持 Alias，而不是展开为重复色值，例如 `color/bg/canvas` → `var(--cf-color-neutral-50)`。
3. 数字尺寸统一输出 `px`；字体字重保持无单位数字。
4. 导出中缺少 Web Code Syntax 的字重补为 `--cf-font-weight-regular|medium|semibold|bold`。
5. TypeScript 只暴露 CSS Variable 引用，不复制视觉值，避免 CSS 与 TS 产生双真源。
6. 组件级 Token 使用 `--cf-component-*`，它们引用 foundation Token，不直接写十六进制颜色。

## 建议语义 Token

| 设计语义 | Figma / Foundation | CSS |
| --- | --- | --- |
| 页面背景 | `color/bg/canvas` | `--cf-color-bg-canvas` |
| 表面 | `color/bg/surface` | `--cf-color-bg-surface` |
| 主文字 | `color/text/primary` | `--cf-color-text-primary` |
| 次文字 | `color/text/secondary` | `--cf-color-text-secondary` |
| 默认边框 | `color/border/default` | `--cf-color-border-default` |
| 错误背景/文字 | `color/bg/error`、`color/text/error` | `--cf-color-bg-error`、`--cf-color-text-error` |
| 成功背景/文字 | `color/bg/success`、`color/text/success` | `--cf-color-bg-success`、`--cf-color-text-success` |
| 间距 | `spacing/{scale}` | `--cf-spacing-{scale}` |
| 圆角 | `radius/{scale}` | `--cf-radius-{scale}` |
| 字号 | `font-size/{role}` | `--cf-font-size-{role}` |
| 字重 | `weight/{role}` | `--cf-font-weight-{role}` |

完整 105 项映射以 `token-manifest.json` 为准。

## 组件契约 Token

| 用途 | CSS Token | Foundation 引用 |
| --- | --- | --- |
| Primary Button 默认 | `--cf-component-action-primary-bg` | `--cf-color-neutral-950` |
| Primary Button Hover | `--cf-component-action-primary-bg-hover` | `--cf-color-neutral-900` |
| Primary Button Pressed | `--cf-component-action-primary-bg-pressed` | `--cf-color-neutral-700` |
| Focus Ring | `--cf-component-focus-ring` | `--cf-color-neutral-950` |
| Selected 背景 | `--cf-component-selected-bg` | `--cf-color-neutral-100` |
| Selected 指示 | `--cf-component-selected-indicator` | `--cf-color-neutral-950` |
| Success Badge 背景 | `--cf-component-badge-success-bg` | `--cf-color-bg-success` |
| Success Badge 文字 | `--cf-component-badge-success-text` | `--cf-color-text-success` |

## 已知差异与处理

### Focus Token 待同步

当前 Figma 导出 `color/border/focus` 仍指向 `brand/accent`（`#F5319D`），但已批准的 `US-S1-UI-02` 组件规范要求 Focus 为 `#1D2129`。为保证导出可追溯，生成器不篡改 `--cf-color-border-focus`；组件统一使用 `--cf-component-focus-ring: var(--cf-color-neutral-950)`。

设计负责人下次同步 Figma 时应将 `color/border/focus` 改为 `neutral/950`。完成并重新导出后，可评审是否移除组件兼容别名。

### 字体平台差异

Figma 导出值为 `Noto Sans SC`，其描述明确指定生产 CSS 使用 `"Microsoft YaHei", "PingFang SC", sans-serif`。因此 `tokens.css` 使用生产字体栈；manifest 同时保留 Figma 原始值。

### 品牌色使用边界

品牌 Token 会完整生成，但仅用于品牌标记和品牌身份展示。主动作、Focus 和 Selected 使用组件契约中的中性色 Token。

## 维护责任

| 责任角色 | 职责 |
| --- | --- |
| Design System Owner（设计负责人） | 修改 Figma Variables、处理 Alias/Scope、导出四个 JSON、记录视觉变更 |
| Frontend Token Owner（前端负责人） | 维护生成器和组件契约、运行生成与校验、在前端工程接入产物 |
| Reviewer（设计 + 前端） | 审核 manifest 与生成 diff，确认命名和组件用途一致 |
| QA | 验证 Button、Badge、Focus、Selected 和语义状态，不只检查色值 |

## 更新流程

```bash
node doc/sprint1/handoff/tokens/generate-tokens.mjs
node doc/sprint1/handoff/tokens/verify-token-map.mjs
```

1. 设计负责人更新 Figma Variables 并重新导出四个 JSON。
2. 前端负责人运行生成器，提交源 JSON 与生成产物的同一组变更。
3. 运行校验脚本，必须满足源 Token 数与 manifest 一致、所有 CSS 引用可解析、组件契约不漂移。
4. 设计和前端共同评审 Button 与 Success Badge 示例。

## 验证证据

- `Button / Primary`：使用中性 Primary、Hover、Pressed、Focus、尺寸、间距和圆角 Token。
- `Badge / Success`：使用成功背景、成功文字、成功图标、间距和 Full Radius Token。
- `verification.html` 可在无工程环境直接加载 `tokens.css` 与 `examples.css`。
- `verify-token-map.mjs` 提供机器可重复的一致性检查。
