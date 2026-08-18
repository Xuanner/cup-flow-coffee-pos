import { apiErrorFromResponse } from "./api-error";

describe("apiErrorFromResponse", () => {
  it.each([
    [400, "validation"],
    [422, "validation"],
    [401, "unauthenticated"],
    [403, "forbidden"],
    [409, "conflict"],
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
});
