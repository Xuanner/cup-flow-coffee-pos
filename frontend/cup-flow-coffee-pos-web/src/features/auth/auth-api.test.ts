import {
  apiSuccessResponse,
  testTimestamp,
  testTraceId,
} from "../../test/api-response-fixture";
import { getCurrentUser, login, logout } from "./auth-api";

describe("auth api", () => {
  it("使用同源 Cookie 查询并解析当前身份", async () => {
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({
        id: "01KSESSIONUSER",
        displayName: "值班员工",
        roles: ["CASHIER"],
        defaultPath: "/pos",
      }),
    );

    await expect(getCurrentUser()).resolves.toMatchObject({
      id: "01KSESSIONUSER",
      roles: ["CASHIER"],
    });
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.stringContaining("/auth/me"),
      expect.objectContaining({ credentials: "same-origin" }),
    );
  });

  it("先获取 CSRF，再原样提交密码并解析最小用户模型", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        apiSuccessResponse({
          headerName: "X-XSRF-TOKEN",
          token: "csrf-test-token",
        }),
      )
      .mockResolvedValueOnce(
        apiSuccessResponse({
          id: "01KTESTUSER",
          displayName: "收银员",
          roles: ["CASHIER"],
          defaultPath: "/pos",
        }),
      );

    await expect(
      login({ username: "  cashier  ", password: " secret " }),
    ).resolves.toEqual({
      id: "01KTESTUSER",
      displayName: "收银员",
      roles: ["CASHIER"],
      defaultPath: "/pos",
    });

    expect(fetchSpy).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining("/auth/csrf"),
      expect.objectContaining({ credentials: "same-origin" }),
    );
    expect(fetchSpy).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/auth/login"),
      expect.objectContaining({
        body: JSON.stringify({ username: "cashier", password: " secret " }),
        credentials: "same-origin",
        headers: expect.objectContaining({
          "X-XSRF-TOKEN": "csrf-test-token",
        }),
        method: "POST",
      }),
    );
  });

  it("拒绝包含会话敏感字段的登录响应", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        apiSuccessResponse({
          headerName: "X-XSRF-TOKEN",
          token: "csrf-test-token",
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: "SUCCESS",
            data: { sessionToken: "must-not-be-returned" },
            message: "操作成功",
            timestamp: testTimestamp,
            traceId: testTraceId,
          }),
          { headers: { "Content-Type": "application/json" }, status: 200 },
        ),
      );

    await expect(
      login({ username: "cashier", password: "secret" }),
    ).rejects.toMatchObject({ category: "server" });
  });

  it("TC-S2-FE-SESS-007 退出先获取 CSRF 并使用同源 Cookie 发送 POST", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        apiSuccessResponse({
          headerName: "X-XSRF-TOKEN",
          token: "logout-csrf-test-token",
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: "SUCCESS",
            message: "操作成功",
            timestamp: testTimestamp,
            traceId: testTraceId,
          }),
          { headers: { "Content-Type": "application/json" }, status: 200 },
        ),
      );

    await expect(logout()).resolves.toBeUndefined();

    expect(fetchSpy).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/auth/logout"),
      expect.objectContaining({
        credentials: "same-origin",
        headers: expect.objectContaining({
          "X-XSRF-TOKEN": "logout-csrf-test-token",
        }),
        method: "POST",
      }),
    );
  });
});
