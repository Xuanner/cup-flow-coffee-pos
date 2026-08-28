import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";

import { ApiError } from "../../lib/api/api-error";
import { AuthPage } from "./AuthPage";
import { useAuthStore } from "./auth-store";

const loginMock = vi.hoisted(() => vi.fn());
vi.mock("./auth-api", () => ({ login: loginMock }));

const cashier = {
  id: "01KTESTCASHIER",
  displayName: "收银员",
  roles: ["CASHIER"],
  defaultPath: "/pos" as const,
};
const admin = {
  id: "01KTESTADMIN",
  displayName: "管理员",
  roles: ["ADMIN"],
  defaultPath: "/dashboard" as const,
};

function renderLogin(state?: { from?: string }) {
  const router = createMemoryRouter(
    [
      { path: "/login", Component: AuthPage },
      { path: "/pos", element: <h1>收银工作台</h1> },
      { path: "/orders", element: <h1>订单列表</h1> },
      { path: "/dashboard", element: <h1>管理看板</h1> },
    ],
    { initialEntries: [{ pathname: "/login", state }] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

beforeEach(() => {
  loginMock.mockReset();
  useAuthStore.getState().clearCurrentUser();
});

describe("AuthPage", () => {
  it("提供可访问的账号密码表单且密码默认隐藏", async () => {
    const user = userEvent.setup();
    renderLogin();

    expect(screen.getByRole("heading", { name: "员工登录" })).toBeVisible();
    const password = screen.getByLabelText("密码");
    expect(password).toHaveAttribute("type", "password");
    await user.click(screen.getByRole("button", { name: "显示密码" }));
    expect(password).toHaveAttribute("type", "text");
  });

  it("空提交显示字段错误并聚焦第一个错误字段", async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole("button", { name: "登录" }));
    expect(screen.getByText("请输入账号。")).toBeVisible();
    expect(screen.getByText("请输入密码。")).toBeVisible();
    expect(screen.getByLabelText("账号")).toHaveFocus();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("成功后保存最小用户并进入默认页", async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValueOnce(cashier);
    renderLogin();

    await user.type(screen.getByLabelText("账号"), " cashier ");
    await user.type(screen.getByLabelText("密码"), " secret ");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(
      await screen.findByRole("heading", { name: "收银工作台" }),
    ).toBeVisible();
    expect(loginMock).toHaveBeenCalledWith({
      username: " cashier ",
      password: " secret ",
    });
    expect(useAuthStore.getState().currentUser).toEqual(cashier);
  });

  it("管理员成功登录后进入管理看板", async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValueOnce(admin);
    renderLogin();

    await user.type(screen.getByLabelText("账号"), "admin");
    await user.type(screen.getByLabelText("密码"), "secret");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(
      await screen.findByRole("heading", { name: "管理看板" }),
    ).toBeVisible();
  });

  it("只回跳到当前角色允许的站内页面", async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValueOnce(cashier);
    renderLogin({ from: "/orders" });

    await user.type(screen.getByLabelText("账号"), "cashier");
    await user.type(screen.getByLabelText("密码"), "secret");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(
      await screen.findByRole("heading", { name: "订单列表" }),
    ).toBeVisible();
  });

  it("拒绝外部回跳地址并回退到角色默认页", async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValueOnce(cashier);
    renderLogin({ from: "//evil.example/steal" });

    await user.type(screen.getByLabelText("账号"), "cashier");
    await user.type(screen.getByLabelText("密码"), "secret");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(
      await screen.findByRole("heading", { name: "收银工作台" }),
    ).toBeVisible();
  });

  it("已有内存身份访问登录页时进入默认有权页", async () => {
    useAuthStore.getState().setCurrentUser(cashier);
    renderLogin();

    expect(
      await screen.findByRole("heading", { name: "收银工作台" }),
    ).toBeVisible();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("提交期间禁用按钮并阻止重复请求，失败时显示安全消息", async () => {
    const user = userEvent.setup();
    let rejectLogin: ((error: unknown) => void) | undefined;
    loginMock.mockImplementationOnce(
      () =>
        new Promise((_resolve, reject) => {
          rejectLogin = reject;
        }),
    );
    renderLogin();
    await user.type(screen.getByLabelText("账号"), "cashier");
    await user.type(screen.getByLabelText("密码"), "secret");
    const submit = screen.getByRole("button", { name: "登录" });

    await user.click(submit);
    expect(screen.getByRole("button", { name: "正在登录" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "正在登录" }));
    expect(loginMock).toHaveBeenCalledTimes(1);

    rejectLogin?.(
      new ApiError({
        category: "authenticationFailed",
        message: "账号或密码错误，或账号不可用。",
      }),
    );
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(
        "账号或密码错误，或账号不可用。",
      ),
    );
    expect(screen.getByLabelText("账号")).toHaveValue("cashier");
    expect(screen.getByLabelText("密码")).toHaveValue("");
    expect(screen.getByLabelText("密码")).toHaveFocus();
  });

  it("收到 429 后按 Retry-After 暂停提交，到期后允许重试", async () => {
    const user = userEvent.setup();
    loginMock.mockRejectedValueOnce(
      new ApiError({
        category: "rateLimited",
        message: "尝试次数过多，请稍后再试。",
        retryAfterSeconds: 1,
        retryable: true,
      }),
    );
    renderLogin();
    await user.type(screen.getByLabelText("账号"), "cashier");
    await user.type(screen.getByLabelText("密码"), "secret");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "尝试次数过多，请稍后再试。",
    );
    expect(
      screen.getByRole("button", { name: "请稍后再试（1s）" }),
    ).toBeDisabled();
    expect(screen.getByText("1 秒后可重新尝试。")).toBeVisible();

    await waitFor(
      () => expect(screen.getByRole("button", { name: "登录" })).toBeEnabled(),
      { timeout: 2000 },
    );
  });

  it.each([
    ["network", "无法连接到服务，请检查网络后重试。"],
    ["timeout", "请求超时，请确认当前状态后重试。"],
    ["server", "服务暂时不可用，请稍后重试。"],
  ] as const)("将 %s 与凭证错误区分并允许恢复", async (category, message) => {
    const user = userEvent.setup();
    loginMock.mockRejectedValueOnce(
      new ApiError({ category, message, retryable: true }),
    );
    renderLogin();
    await user.type(screen.getByLabelText("账号"), "cashier");
    await user.type(screen.getByLabelText("密码"), "secret");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(screen.getByRole("alert")).toHaveTextContent(message);
    expect(screen.getByRole("button", { name: "登录" })).toBeEnabled();
    expect(screen.getByLabelText("账号")).toHaveValue("cashier");
    expect(screen.getByLabelText("密码")).toHaveValue("");
  });
});
