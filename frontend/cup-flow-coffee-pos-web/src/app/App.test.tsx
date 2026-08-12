import { render, screen } from "@testing-library/react";

import { App } from "./App";
import { createTestRouter } from "./router";

describe("应用骨架", () => {
  it("可以访问核心模块占位路由", async () => {
    render(<App router={createTestRouter(["/orders"])} />);

    expect(
      await screen.findByRole("heading", { level: 1, name: "订单" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "主导航" }),
    ).toBeInTheDocument();
  });

  it("未知路由显示 404 状态", async () => {
    render(<App router={createTestRouter(["/does-not-exist"])} />);

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "404 · 页面不存在",
      }),
    ).toBeInTheDocument();
  });
});
