# Cup Flow Typography Foundations

## Family Token

| 属性 | 值 |
| --- | --- |
| Figma Variable Collection | `Typography` |
| Figma Variable | `family/sans` |
| Figma Value | `PingFang SC` |
| Variable Type | `String` |
| Variable Scope | `Font family` |
| Web Code Syntax | `var(--cf-font-family-sans)` |
| 生产 CSS | `"Microsoft YaHei", "PingFang SC", sans-serif` |

Figma 设计稿统一使用 `PingFang SC`。Web 端保留 `Microsoft YaHei` 作为 Windows 首选字体，并使用 `PingFang SC` 作为 macOS 首选回退字体。

## Weight Tokens

| Token | Figma Value | 用途 |
| --- | --- | --- |
| `weight/regular` | `Regular`（400） | 正文、辅助文字、表格内容 |
| `weight/medium` | `Medium`（500） | 金额、按钮和关键标签 |
| `weight/semibold` | `Semibold`（600） | 模块标题、表格标题和强调信息 |
| `weight/bold` | `Bold`（700） | 页面标题和需要强强调的内容 |

Web 端建议映射：

```css
--cf-font-weight-regular: 400;
--cf-font-weight-medium: 500;
--cf-font-weight-semibold: 600;
--cf-font-weight-bold: 700;
```

如果当前 Figma 文件中的 `family/sans` 仍显示为 `Noto Sans SC`，需在 Local Variables 中手动将其值改为 `PingFang SC`。变量名称、作用域和 Web Code Syntax 保持不变。

当前文件已存在 `weight/regular` 和 `weight/medium` 时，保留原 Token 并确认其值分别为 `Regular` 和 `Medium`；另外新增 `weight/semibold` 与 `weight/bold`。四个变量的 Scope 均为 `Font style`。
