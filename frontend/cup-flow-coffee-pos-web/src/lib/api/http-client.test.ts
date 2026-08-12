import { apiRequest, HttpError } from "./http-client";

describe("apiRequest", () => {
  it("解析成功的 JSON 响应", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "UP" }), {
        headers: { "content-type": "application/json" },
        status: 200,
      }),
    );

    await expect(apiRequest<{ status: string }>("/health")).resolves.toEqual({
      status: "UP",
    });
  });

  it("将非成功响应转换为 HttpError", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "不可用" }), {
        headers: { "content-type": "application/json" },
        status: 503,
      }),
    );

    await expect(apiRequest("/health")).rejects.toMatchObject({
      name: HttpError.name,
      status: 503,
    });
  });
});
