# US-S1-UI-02 — Figma / Radix UI 映射

## 命名约定

- Figma 页面建议：`02 Components`。
- Component Set：`Button`、`Input`、`Select`、`Search`、`Badge`、`Table`、`Pagination`、`Dialog`、`Toast`、`EmptyState`、`Loading`。
- Variant 格式：`Style=Primary, Size=Comfortable, State=Default`。
- 图层格式：`Root`、`Leading icon`、`Label`、`Value`、`Supporting text`、`Trailing action`。
- SVG 中对应 ID：`cf-component-*`、`variant/*`、`layer/*`；导入后按以上 Figma 名称整理。

## 组件映射

| Figma 组件 | 实现映射 | 主要 Parts / 语义 | 关键状态 |
| --- | --- | --- | --- |
| `Button` | 项目封装；原生 `<button>`，Radix `Slot` 可支持 `asChild` | Root、Leading icon、Label、Trailing icon | Default、Hover、Focus、Pressed、Disabled、Loading |
| `Input` | 原生 `<input>` + Radix `Label`；项目 `Field` 封装 | Label、Control、Supporting text | Default、Hover、Focus、Disabled、Error |
| `Select` | Radix `Select` | Root、Trigger、Value、Icon、Portal、Content、Viewport、Item、ItemText、ItemIndicator | Closed/Open、Placeholder/Value、Item Highlighted/Selected/Disabled、Error |
| `Search` | 项目组合；原生 `type=search` + Label | Search icon、Control、Clear button、Supporting text | Empty、Value、Focus、Loading、No results、Error |
| `Badge` | 项目自定义 `<span>` | Label、可选 Dot/Icon | Neutral、Brand、Success、Warning、Error、Info；Solid/Subtle |
| `Table` | 原生 `<table>` 语义；复杂交互可组合 Radix Checkbox/DropdownMenu | Caption、Thead、Th、Tbody、Tr、Td | Default、Row hover、Selected、Loading、Empty、Error |
| `Pagination` | 项目自定义 `<nav aria-label="分页">` | Previous、Page、Next、Summary | Default、Hover、Focus、Current、Disabled |
| `Dialog` | Radix `Dialog`；危险确认改用 `AlertDialog` | Root、Trigger、Portal、Overlay、Content、Title、Description、Close | Closed/Open、Default、Loading、Error |
| `Toast` | Radix `Toast` | Provider、Root、Title、Description、Action、Close、Viewport | Info、Success、Warning、Error；Foreground/Background |
| `EmptyState` | 项目自定义状态容器 | Icon/Illustration、Title、Description、Primary/Secondary action | First-use、No data、No results、Filtered |
| `Loading` | Radix `Progress`（确定进度）或项目 Skeleton/Spinner | Progress/Indicator、Skeleton、status text | Indeterminate、Determinate、Inline、Page |

Radix Primitives 是无样式的底层组件，因此本设计提供视觉变体，交互与可访问性保留 Primitive 行为。Button、Input、Badge、Table、Pagination、EmptyState、Skeleton/Spinner 没有一一对应的 Radix Primitive，必须在项目层标注为 custom wrapper。

## Variant 建议

### Button

- `Style`: `Primary | Secondary | Ghost | Danger`
- `Size`: `Compact(36) | Comfortable(44) | Large(52)`
- `State`: `Default | Hover | Focus | Pressed | Disabled | Loading`
- 避免一次创建完整笛卡尔积；按 Style 拆分 Component Set，使单组不超过 30 个组合。

### Input / Search / Select

- `Size`: `Compact(36) | Comfortable(44)`
- `State`: `Default | Hover | Focus | Disabled | Error`
- `Content`: Input `Empty | Value`；Select `Placeholder | Value`；Search `Empty | Value | Loading`。
- Error 由边框、错误图标与 `Supporting text` 同时表达，不只使用颜色。

### Badge

- `Tone`: `Neutral | Brand | Success | Warning | Error | Info`
- `Style`: `Subtle | Solid`
- `Size`: `Small(24) | Medium(28)`

## Auto Layout 与尺寸

| 组件 | 方向 / 对齐 | Padding | Gap | 最小尺寸 |
| --- | --- | --- | --- | --- |
| Button | Horizontal / center | Compact `8×12`；Comfortable `10×16`；Large `14×20` | 8 | 高 36/44/52；图标按钮 44×44 |
| Input / Search / Select Trigger | Horizontal / center | `0×12` 或 `0×16` | 8 | 高 36/44；宽 Hug 或 Fill |
| Badge | Horizontal / center | `4×8` / `5×10` | 4 | 高 24/28 |
| Dialog Content | Vertical / stretch | 24 | 16/24 | Desktop 宽 480；Mobile 宽减 32 |
| Toast Root | Horizontal / start | 16 | 12 | 宽 360；最小高 72 |
| EmptyState | Vertical / center | 32 | 12/24 | 最小高 240 |

所有顶层组件 Frame 使用 Auto Layout。文字设为 Hug；可伸展字段设为 Fill；主触控动作最小 44×44。36px Compact 仅用于鼠标/键盘密集表格工具栏，不作为主要触控操作。

## 键盘、焦点与关闭规则

- `Button`：Tab 聚焦；Enter/Space 激活；Loading 时保留宽度并阻止重复提交，使用 `aria-busy=true`。
- `Input/Search`：Label 与控件关联；错误信息使用 `aria-describedby`，错误时 `aria-invalid=true`；清除按钮必须有可访问名称。
- `Select`：Trigger 通过 Enter/Space/ArrowDown 打开；列表使用方向键、Home/End 与字符检索；Enter/Space 选择；Esc 关闭并将焦点归还 Trigger。
- `Pagination`：使用 `nav`；当前页 `aria-current=page`；不可用的前后页使用真实 disabled 语义。
- `Dialog`：打开后焦点进入 Content，modal 内循环；Esc 或明确的关闭/取消按钮关闭；关闭后焦点回到 Trigger。危险确认的初始焦点放在取消动作。
- `Toast`：使用合适的 foreground/background live region；F8 聚焦 Viewport；Esc 关闭当前 Toast；Action 必须可忽略，必须响应的任务使用 AlertDialog。
- `Table`：DOM 阅读顺序与视觉顺序一致；列标题使用 `th` 和 `scope`；行操作按钮提供包含对象名称的 accessible name。
- 所有 Focus Visible 使用清晰外环；鼠标点击不强制显示键盘焦点样式。

## 响应式规则

- Mobile：Dialog 允许转换为底部面板式布局，但仍使用 Dialog 语义；Table 转为键值卡片；Pagination 保留前/后与当前页摘要。
- Tablet：表格可横向滚动，首列和操作列可固定；控件使用 44px 高度。
- Desktop：表格和工具栏可使用 36px Compact；主要提交、收款与危险确认仍使用 44px 或 52px。

## Foundation Token 对照

| 语义 | 值 |
| --- | --- |
| Brand scale（仅品牌强调） | `#DD2089` / `#D51C82` / `#CB1E83` |
| Brand Light / Background | `#FDC2DB` / `#FFE8F1` |
| Action Primary / Hover / Pressed | `#1D2129` / `#272E3B` / `#4E5969` |
| Focus | `#1D2129` 2px ring + 2px white offset |
| Selected | `#F2F3F5` surface + `#1D2129` indicator/check + medium text |
| Text Primary / Secondary / Muted | `#1D2129` / `#4E5969` / `#6B7785` |
| Surface / Canvas / Subtle | `#FFFFFF` / `#F7F8FA` / `#F2F3F5` |
| Border / Disabled | `#E5E6EB` / `#A9AEB8` |
| Success | `#15803D` on `#DCFCE7` |
| Warning | `#B45309` on `#FEF3C7` |
| Error | `#DC2626` on `#FEE2E2` |
| Info | `#2563EB` on `#DBEAFE` |
| Radius | 4 / 8 / 12 / 16 / 999 |
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 / 40 / 48 / 64 |
| Font | Figma `PingFang SC`；Web `Microsoft YaHei, PingFang SC, sans-serif` |

## 导入后的人工验收

- [ ] 组件转换为 Component Set，Variants 命名符合本文。
- [ ] 核心 Frame 使用 Auto Layout，无绝对定位内容依赖。
- [ ] Fill、Stroke、Typography、Gap、Radius 已绑定 foundation Variables。
- [ ] 主要触控目标不小于 44×44；Compact 例外有用途说明。
- [ ] 逐项验证 Tab 顺序、Focus Visible、Enter/Space、方向键和 Esc。
- [ ] Dialog 标题/描述、字段 Label、错误描述、Toast live region 与 Table 结构具备必要 ARIA 语义。
- [ ] 状态同时有文字、图标或形状提示，不只依赖颜色。
