# Cup Flow Coffee POS — Sprint 1 业务 UI SVG 交付

本交付只覆盖以下八个 User Story。SVG 可分别导入 Figma，对应的顶层 `frame-*` 分组可转换为 Frame。

| User Story | SVG |
| --- | --- |
| US-S1-PAY-01 收款与找零设计 | `payment/cup-flow-payment-and-change.svg` |
| US-S1-PAY-02 价格或状态冲突设计 | `payment/cup-flow-payment-conflict.svg` |
| US-S1-ORDER-01 查询订单设计 | `orders/cup-flow-order-query.svg` |
| US-S1-ORDER-02 订单状态操作设计 | `orders/cup-flow-order-state-actions.svg` |
| US-S1-PRODUCT-01 商品与分类管理设计 | `products/cup-flow-catalog-management.svg` |
| US-S1-PRODUCT-02 商品选项配置设计 | `products/cup-flow-product-option-config.svg` |
| US-S1-DASH-01 经营看板设计 | `dashboard/cup-flow-operations-dashboard.svg` |
| US-S1-SYS-01 全局异常与权限状态 | `system/cup-flow-global-states.svg` |

## Figma 导入

1. 将每个 SVG 拖入对应 Figma 页面，或使用 **Place image**。
2. 将顶层业务场景分组转换为 Frame，并使用现有组件替换静态矢量。
3. 将颜色、字体、间距和圆角绑定至 Foundations Variables。
4. 为页面、列表、表单、Dialog、Drawer 和状态容器补充 Auto Layout。
5. 根据各 SVG 末尾的交接说明配置键盘、Focus Visible、ARIA 和 Prototype 流程。

SVG 导入不会自动创建 Component、Variable、Auto Layout 或 Prototype 连接。

## 统一视觉约束

- `#DD2089` 只用于品牌 Logo 和品牌标识。
- Primary、Selected 和 Focus Visible 使用 `#1D2129`。
- 成功、警告、错误、信息使用语义色，并同时提供文字、图标或形状提示。
- 主要触控目标不小于 44×44px。
- 错误和冲突不展示堆栈、令牌、内部主机或服务信息。

## 生成

八个 SVG 由同目录下的 `generate-business-ui-svg.mjs` 生成：

```bash
node doc/sprint1/generate-business-ui-svg.mjs
```
