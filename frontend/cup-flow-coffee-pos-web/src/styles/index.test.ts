import { readFileSync } from "node:fs";
import { join } from "node:path";

describe("Tailwind Token 命名", () => {
  it("自定义间距使用 cf 前缀，避免覆盖 max-w-sm 到 max-w-4xl", () => {
    const css = readFileSync(
      join(process.cwd(), "src/styles/index.css"),
      "utf8",
    );

    expect(css).toContain("--spacing-cf-4xl: var(--cf-spacing-4xl)");
    expect(css).not.toMatch(/--spacing-(?:2xs|xs|sm|md|lg|xl|2xl|3xl|4xl)\s*:/);
  });
});
