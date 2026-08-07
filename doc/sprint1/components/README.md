# Cup Flow Coffee POS — US-S1-UI-02 组件交接

## 交付范围

本目录只覆盖 `US-S1-UI-02 可复用组件与状态`。不包含业务页面、设计 Token 的跨端维护方案或其他 Sprint 1 user story。

| 文件 | 用途 |
| --- | --- |
| `cup-flow-components.svg` | 可导入 Figma 的组件、状态、尺寸和交互说明画板 |
| `RADIX_MAPPING.md` | Figma 命名、Radix UI 映射、键盘/焦点/ARIA 与导入后设置 |

设计属性来源：`../foundations/cup-flow-foundations.svg` 与 `../foundations/TYPOGRAPHY.md`。远端 Figma 文件因 Starter 套餐 MCP 调用额度限制无法读取；本次没有虚构或覆盖远端变量。

## 视觉原则

- `#DD2089` 只用于品牌标记、品牌说明和明确的品牌身份展示，不用于主动作、选中或焦点。
- Primary 主动作使用 `#1D2129`；Hover / Pressed 使用 `#272E3B` / `#4E5969`。
- Focus Visible 使用深中性色；选中状态使用浅中性底色、深色指示条、勾选图标和加粗文字组合表达。
- 默认界面以白色、`#F7F8FA`、`#E5E6EB`、`#4E5969`、`#1D2129` 构成。
- 成功、警告、错误、信息分别使用 foundation 中的绿、琥珀、红、蓝语义色；状态同时提供图标或文字。
- 桌面紧凑控件高度为 36px；默认控件与主要触控目标为 44px；大号主动作 52px。
- Focus Visible 使用 2px `#1D2129` 外环，并保留 2px 白色间隔；错误状态使用 `#DC2626` 边框及字段说明。

## SVG 导入

1. 在 Figma 中使用 **Place image** 或拖入 `cup-flow-components.svg`。
2. SVG 顶层按 `cf-section-*` 命名，各组件按 `cf-component-*` 命名，变体按 `variant/属性=值` 命名。
3. 对照 `RADIX_MAPPING.md` 将每组转换为 Component / Component Set；SVG 导入不会自动生成 Figma Variants、Variables 或 Auto Layout。
4. 把画板中标注的填充、边框、文字、间距和圆角绑定到现有 foundation Variables。
5. 为核心 Frame 应用 Auto Layout，并执行键盘顺序、Focus Visible、Esc 关闭和触控尺寸走查。

## 验收证据

- 组件覆盖：Button、Input、Select、Search、Badge、Table、Pagination、Dialog、Toast、Empty、Loading。
- 必要状态覆盖：Default、Hover、Focus Visible、Pressed/Open、Disabled、Loading、Error；Select 另含 selected/unselected 与 open/closed。
- 密度覆盖：Compact 36、Comfortable/Touch 44；主动作可选 Large 52。
- SVG 中包含开发可读的尺寸、颜色、字体、间距和交互注释；完整实现映射见 `RADIX_MAPPING.md`。
