import { render, screen } from "@testing-library/react";

import { ApiError } from "../lib/api/api-error";
import { useAuthStore } from "../features/auth/auth-store";
import { App } from "./App";
import { createTestRouter } from "./router";

const getCurrentUserMock = vi.hoisted(() => vi.fn());
vi.mock("../features/auth/auth-api", () => ({
  getCurrentUser: getCurrentUserMock,
  login: vi.fn(),
}));

const cashier = {
  id: "01KSESSIONCASHIER",
  displayName: "值班收银员",
  roles: ["CASHIER"],
  defaultPath: "/pos" as const,
};

beforeEach(() => {
  getCurrentUserMock.mockReset();
  getCurrentUserMock.mockResolvedValue(cashier);
  useAuthStore.setState({
    currentUser: null,
    sessionError: null,
    sessionStatus: "checking",
  });
});

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

  it("TC-S2-FE-SESS-001 会话确认期间不渲染业务 Shell 或菜单", () => {
    getCurrentUserMock.mockReturnValue(new Promise(() => undefined));

    render(<App router={createTestRouter(["/orders"])} />);

    expect(screen.getByRole("status")).toHaveTextContent("正在确认登录状态…");
    expect(
      screen.queryByRole("heading", { name: "订单" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("navigation", { name: "主导航" }),
    ).not.toBeInTheDocument();
  });

  it("TC-S2-FE-SESS-002 刷新有效会话后恢复用户和原授权地址", async () => {
    const router = createTestRouter(["/orders?status=open"]);

    render(<App router={router} />);

    expect(
      await screen.findByRole("heading", { level: 1, name: "订单" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/orders");
    expect(router.state.location.search).toBe("?status=open");
    expect(useAuthStore.getState().currentUser).toEqual(cashier);
  });

  it("TC-S2-FE-SESS-003 启动时无会话进入登录页且不显示过期提示", async () => {
    getCurrentUserMock.mockRejectedValueOnce(
      new ApiError({
        category: "unauthenticated",
        code: "AUTH-401-001",
        message: "登录状态已失效，请重新登录。",
        status: 401,
      }),
    );
    const router = createTestRouter(["/orders"]);

    render(<App router={router} />);

    expect(
      await screen.findByRole("heading", { name: "员工登录" }),
    ).toBeInTheDocument();
    expect(screen.queryByText("登录状态已失效")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("navigation", { name: "主导航" }),
    ).not.toBeInTheDocument();
    expect(router.state.location.state).toEqual({ from: "/orders" });
  });

  it("TC-S2-FE-AUTH-010 恢复身份时不写浏览器存储或 URL 凭证", async () => {
    const localStorageSpy = vi.spyOn(Storage.prototype, "setItem");
    const router = createTestRouter(["/pos"]);

    render(<App router={router} />);

    expect(
      await screen.findByRole("heading", { level: 1, name: "收银台" }),
    ).toBeInTheDocument();
    expect(localStorageSpy).not.toHaveBeenCalled();
    expect(router.state.location.pathname).toBe("/pos");
    expect(router.state.location.search).not.toMatch(/token|session|csrf/i);
  });
});
