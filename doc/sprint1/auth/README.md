# Cup Flow Coffee POS — US-S1-AUTH-01 登录体验设计

## 交付范围

本目录只覆盖 `US-S1-AUTH-01 登录体验设计`，不包含登录功能实现、接口定义或其他 Sprint 1 User Story。

| 文件 | 用途 |
| --- | --- |
| `cup-flow-auth-login.svg` | 可导入 Figma 的登录页与状态画板 |

## SVG 覆盖内容

- Desktop 1280×800 默认登录页。
- 账号、密码、显示/隐藏密码和登录操作。
- 空字段校验、字段级错误与首个错误字段聚焦规则。
- 未知账号、错误密码和停用账号共用的安全认证失败提示。
- 登录提交中状态及防重复提交规则。
- 会话失效提示，并明确“未提交的操作未保存；已保存数据不受影响”。
- 收银员登录后进入 `/pos`，管理员登录后进入 `/dashboard`。
- Desktop 与 Mobile 的响应式布局规则。

## 视觉约束

- 品牌色 `#DD2089` 仅用于 Logo 与品牌装饰。
- 主按钮、Focus Visible 使用 `#1D2129`。
- 选中、结构和页面主体使用中性色；错误和信息使用语义色。
- 主要输入和操作高度为 48px，触控目标不小于 44×44px。

## 导入 Figma

1. 将 `cup-flow-auth-login.svg` 拖入 Figma，或使用 **Place image** 导入。
2. 按 `cf-auth-frame-*` 顶层分组整理为 `03 Auth` 页面中的 Frame。
3. 将颜色、字体、间距和圆角绑定至现有 Foundation Variables。
4. 使用现有 Input / Button 组件替换画板中的静态矢量，并设置 Auto Layout。
5. 根据画板末尾的交接表验证 Tab 顺序、首错聚焦、显隐密码、提交中与错误朗读。

SVG 导入不会自动创建 Figma Component、Variable、Auto Layout 或 Prototype 连接，这些需要在导入后完成。
