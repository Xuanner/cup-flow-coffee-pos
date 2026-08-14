# Cup Flow Coffee POS — US-S1-POS-01 快速点单设计

## 交付范围

本目录包含两个彼此独立的 POS 设计交付：`US-S1-POS-01 快速点单设计` 与 `US-S1-POS-02 商品定制设计`。不包含收款设计、业务代码或其他 Sprint 1 User Story。

| 文件 | 用途 |
| --- | --- |
| `cup-flow-pos-quick-order.svg` | 可导入 Figma 的快速点单页面、关键状态与交接画板 |
| `cup-flow-pos-product-customization.svg` | 可导入 Figma 的商品定制 Drawer、状态与交接画板 |

## SVG 覆盖内容

- 1280×800 导航、商品区和购物车同屏工作区。
- 分类、商品名称搜索、名称、基础价格和售罄状态。
- 售罄商品的文字标签、视觉降级和禁用添加操作。
- 搜索无结果、分类无可售商品和空购物车状态。
- 购物车数量、配置摘要、编辑、删除、整单备注、小计和总额。
- 空购物车禁用结算。
- 清空购物车二次确认、影响数量/金额和不可撤销说明。
- Desktop、Tablet、Mobile 的布局变化原则。
- 键盘顺序、Focus Visible、disabled 和 AlertDialog 交接规则。

## Story 边界

- 点击商品或购物车的“编辑”进入 `US-S1-POS-02 商品定制设计`，本稿不展开定制选项。
- 点击“去结算”进入 `US-S1-PAY-01 收款与找零设计`，本稿不定义支付方式。
- 本稿不执行或替代上述 User Story。

## 视觉约束

- 品牌色 `#DD2089` 仅用于 Cup Flow Logo。
- 主动作和 Focus Visible 使用 `#1D2129`。
- 选中分类使用 `#F2F3F5` 背景、`#1D2129` 指示条和中等字重。
- 售罄与空状态同时使用文字、图形和控件状态，不只依赖颜色。
- 主要触控目标不小于 44×44px。

## 导入 Figma

1. 将 `cup-flow-pos-quick-order.svg` 拖入 Figma，或使用 **Place image** 导入。
2. 将 `cf-pos-frame-*` 分组整理到 `04 POS & Payment` 页面并转换为 Frame。
3. 使用现有 Navigation、Search、Tabs、Product Card、Button、Input、Dialog 等组件替换静态矢量。
4. 将颜色、字体、间距、圆角绑定至 Foundation Variables，并为核心结构设置 Auto Layout。
5. 按画板末尾的验收表走查售罄、空状态、购物车、清空确认和焦点顺序。

SVG 导入不会自动创建 Figma Component、Variable、Auto Layout 或 Prototype 连接，这些需要导入后完成。

## US-S1-POS-02 覆盖内容

- 杯型、温度、糖度、加料、数量和商品备注。
- 必选/可选、单选/多选、默认值、选中、Focus Visible 和不可用状态。
- 基础价、规格差价、加料价、商品单价、数量与合计实时联动。
- 缺少必选项时的错误摘要、字段级提示、阻止提交和首错聚焦。
- 从商品卡新增时使用“加入购物车”；从已有订单项进入时使用“保存修改”。
- Desktop Drawer、Tablet Drawer 和 Mobile 全屏 Dialog 的响应式规则。

商品定制 SVG 导入后，应使用 RadioGroup、Checkbox、Button、Textarea 与 Dialog/Drawer 等现有组件替换静态矢量，并按画板说明补充焦点限制和错误关联。
