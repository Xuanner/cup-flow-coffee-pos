import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { ApiError } from "../lib/api/api-error";
import { apiRequest } from "../lib/api/http-client";
import { useAuthStore } from "../features/auth/auth-store";
import { App } from "./App";
import { createTestRouter } from "./router";

const getCurrentUserMock = vi.hoisted(() => vi.fn());
const logoutMock = vi.hoisted(() => vi.fn());
vi.mock("../features/auth/auth-api", () => ({
  getCurrentUser: getCurrentUserMock,
  login: vi.fn(),
  logout: logoutMock,
}));

const cashier = {
  id: "01KSESSIONCASHIER",
  displayName: "值班收银员",
  roles: ["CASHIER"],
  defaultPath: "/pos" as const,
};
const admin = {
  id: "01KSESSIONADMIN",
  displayName: "值班管理员",
  roles: ["ADMIN"],
  defaultPath: "/dashboard" as const,
};

beforeEach(() => {
  getCurrentUserMock.mockReset();
  getCurrentUserMock.mockResolvedValue(cashier);
  logoutMock.mockReset();
  logoutMock.mockResolvedValue(undefined);
  useAuthStore.setState({
    currentUser: null,
    sessionError: null,
    sessionExpired: false,
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

  it("TC-S2-FE-SESS-004/005 并发会话失效只进入一次过期状态并安全保存原目标", async () => {
    const router = createTestRouter(["/orders?status=open"]);
    render(<App router={router} />);
    expect(
      await screen.findByRole("heading", { level: 1, name: "订单" }),
    ).toBeInTheDocument();
    const expiredResponse = () =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            code: "AUTH-401-001",
            data: null,
            message: "登录状态已失效，请重新登录",
            timestamp: "2026-08-28T05:00:00Z",
            traceId: "expired-session-test",
          }),
          { headers: { "Content-Type": "application/json" }, status: 401 },
        ),
      );
    vi.spyOn(globalThis, "fetch")
      .mockImplementationOnce(expiredResponse)
      .mockImplementationOnce(expiredResponse);

    await Promise.allSettled([
      apiRequest("/orders/first"),
      apiRequest("/orders/second"),
    ]);

    expect(
      await screen.findByRole("heading", { name: "员工登录" }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("登录已过期，请重新登录。")).toHaveLength(1);
    expect(screen.getByText("未提交的内容可能不会保留。")).toBeVisible();
    expect(router.state.location.state).toEqual({
      from: "/orders?status=open",
    });
    expect(useAuthStore.getState()).toMatchObject({
      currentUser: null,
      sessionExpired: true,
      sessionStatus: "anonymous",
    });
  });

  it("TC-S2-FE-SESS-006 当前身份网络失败显示独立提示并可重试", async () => {
    const user = userEvent.setup();
    getCurrentUserMock
      .mockRejectedValueOnce(
        new ApiError({
          category: "network",
          message: "无法连接到服务，请检查网络后重试。",
          retryable: true,
        }),
      )
      .mockResolvedValueOnce(cashier);
    render(<App router={createTestRouter(["/orders"])} />);

    expect(await screen.findByText("暂时无法确认登录状态")).toBeVisible();
    expect(screen.queryByText("账号或密码错误")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "重新检查" }));
    expect(
      await screen.findByRole("heading", { level: 1, name: "订单" }),
    ).toBeVisible();
  });

  it("TC-S2-FE-SESS-007 账号区展示身份，退出成功后清理并阻止后退访问", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(["/orders"]);
    render(<App router={router} />);

    expect(await screen.findByText("值班收银员")).toBeVisible();
    expect(screen.getByText("收银员")).toBeVisible();
    await user.click(screen.getByRole("button", { name: "退出登录" }));

    expect(logoutMock).toHaveBeenCalledTimes(1);
    expect(
      await screen.findByRole("heading", { name: "员工登录" }),
    ).toBeVisible();
    expect(useAuthStore.getState().currentUser).toBeNull();
    await router.navigate(-1);
    expect(
      screen.queryByRole("heading", { name: "订单" }),
    ).not.toBeInTheDocument();
  });

  it("TC-S2-FE-SESS-008 退出失败保留身份并允许重试且防止重复提交", async () => {
    const user = userEvent.setup();
    let rejectLogout: ((reason?: unknown) => void) | undefined;
    logoutMock.mockImplementationOnce(
      () =>
        new Promise((_resolve, reject) => {
          rejectLogout = reject;
        }),
    );
    render(<App router={createTestRouter(["/orders"])} />);
    expect(await screen.findByText("值班收银员")).toBeVisible();
    const logoutButton = screen.getByRole("button", { name: "退出登录" });

    await user.click(logoutButton);
    await user.click(logoutButton);
    expect(logoutMock).toHaveBeenCalledTimes(1);
    rejectLogout?.(new TypeError("test-only network failure"));
    expect(
      await screen.findByText("暂时无法退出，请检查网络后重试。"),
    ).toBeVisible();
    expect(screen.getByText("值班收银员")).toBeVisible();

    await user.click(screen.getByRole("button", { name: "退出登录" }));
    expect(logoutMock).toHaveBeenCalledTimes(2);
    expect(
      await screen.findByRole("heading", { name: "员工登录" }),
    ).toBeVisible();
  });

  it.each([
    ["/pos", "收银台"],
    ["/orders/archived", "订单"],
    ["/products", "商品管理"],
    ["/dashboard/detail", "经营看板"],
  ])(
    "TC-S2-FE-ROUTE-001 未登录直达受保护地址 %s 不渲染业务内容",
    async (path, protectedHeading) => {
      getCurrentUserMock.mockRejectedValueOnce(
        new ApiError({
          category: "unauthenticated",
          code: "AUTH-401-001",
          message: "登录状态已失效，请重新登录。",
          status: 401,
        }),
      );
      const router = createTestRouter([path]);
      render(<App router={router} />);

      expect(
        await screen.findByRole("heading", { name: "员工登录" }),
      ).toBeVisible();
      expect(
        screen.queryByRole("heading", { name: protectedHeading }),
      ).not.toBeInTheDocument();
      expect(router.state.location.state).toEqual({ from: path });
    },
  );

  it("TC-S2-FE-ROUTE-006 CASHIER 只看到收银台和订单入口", async () => {
    render(<App router={createTestRouter(["/pos"])} />);

    const navigation = await screen.findByRole("navigation", {
      name: "主导航",
    });
    expect(navigation).toHaveTextContent("收银台");
    expect(navigation).toHaveTextContent("订单");
    expect(navigation).not.toHaveTextContent("商品");
    expect(navigation).not.toHaveTextContent("经营看板");
  });

  it("TC-S2-FE-ROUTE-007 ADMIN 继承收银员入口并看到四个业务模块", async () => {
    getCurrentUserMock.mockResolvedValueOnce(admin);
    render(<App router={createTestRouter(["/dashboard"])} />);

    const navigation = await screen.findByRole("navigation", {
      name: "主导航",
    });
    expect(navigation).toHaveTextContent("收银台");
    expect(navigation).toHaveTextContent("订单");
    expect(navigation).toHaveTextContent("商品");
    expect(navigation).toHaveTextContent("经营看板");
  });

  it.each([
    ["/products", "商品管理"],
    ["/dashboard", "经营看板"],
  ])(
    "TC-S2-FE-ROUTE-008 CASHIER 直达 %s 显示 403 且不渲染业务页",
    async (path, businessHeading) => {
      render(<App router={createTestRouter([path])} />);

      expect(
        await screen.findByRole("heading", { name: "403 · 没有访问权限" }),
      ).toBeVisible();
      expect(
        screen.queryByRole("heading", { name: businessHeading }),
      ).not.toBeInTheDocument();
      expect(screen.getByRole("link", { name: "返回收银台" })).toHaveAttribute(
        "href",
        "/pos",
      );
    },
  );

  it("TC-S2-FE-ROUTE-009 已登录访问不存在路由显示 404 而非 403", async () => {
    render(<App router={createTestRouter(["/unknown-business-page"])} />);

    expect(
      await screen.findByRole("heading", { name: "404 · 页面不存在" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("heading", { name: "403 · 没有访问权限" }),
    ).not.toBeInTheDocument();
  });
});
