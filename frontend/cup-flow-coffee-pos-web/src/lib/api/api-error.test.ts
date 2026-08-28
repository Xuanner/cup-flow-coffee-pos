import { apiErrorFromResponse } from "./api-error";

describe("apiErrorFromResponse", () => {
  it.each([
    [400, "validation"],
    [422, "validation"],
    [401, "unauthenticated"],
    [403, "forbidden"],
    [409, "conflict"],
    [429, "rateLimited"],
    [500, "server"],
    [503, "server"],
  ] as const)("将 HTTP %i 映射为 %s", (status, category) => {
    expect(
      apiErrorFromResponse(status, { message: "服务端详情" }),
    ).toMatchObject({
      category,
      name: "ApiError",
      status,
    });
  });

  it("只向用户展示可安全使用的参数错误消息", () => {
    expect(
      apiErrorFromResponse(400, { message: "商品名称不能为空" }).message,
    ).toBe("商品名称不能为空");
    expect(
      apiErrorFromResponse(500, { message: "数据库密码错误" }).message,
    ).toBe("服务暂时不可用，请稍后重试。");
  });

  it.each([
    [401, "AUTH-401-002", "authenticationFailed"],
    [403, "AUTH-403-002", "securityValidation"],
    [429, "AUTH-429-001", "rateLimited"],
  ] as const)("按稳定错误码映射认证错误", (status, code, category) => {
    expect(apiErrorFromResponse(status, { code })).toMatchObject({
      category,
      code,
    });
  });

  it("解析 429 的整数秒 Retry-After", () => {
    expect(
      apiErrorFromResponse(429, { code: "AUTH-429-001" }, "37"),
    ).toMatchObject({
      category: "rateLimited",
      retryAfterSeconds: 37,
      retryable: true,
    });
    expect(
      apiErrorFromResponse(429, { code: "AUTH-429-001" }, "invalid")
        .retryAfterSeconds,
    ).toBeUndefined();
  });
});
