import {
  apiSuccessResponse,
  testTimestamp,
  testTraceId,
} from "../../test/api-response-fixture";
import { apiRequest } from "./http-client";

describe("apiRequest", () => {
  it("解析成功的 JSON 响应", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(apiSuccessResponse({ status: "UP" }));

    await expect(apiRequest<{ status: string }>("/health")).resolves.toEqual({
      code: "SUCCESS",
      data: { status: "UP" },
      message: "操作成功",
      timestamp: testTimestamp,
      traceId: testTraceId,
    });
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ credentials: "same-origin" }),
    );
  });

  it("将断网转换为 network ApiError", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValueOnce(
      new TypeError("Failed to fetch"),
    );

    await expect(apiRequest("/health")).rejects.toMatchObject({
      category: "network",
      retryable: true,
    });
  });

  it("将内部超时转换为 timeout ApiError", async () => {
    vi.useFakeTimers();
    vi.spyOn(globalThis, "fetch").mockImplementationOnce((_input, init) => {
      return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => {
          reject(new DOMException("Aborted", "AbortError"));
        });
      });
    });

    const request = apiRequest("/slow", {}, 50);
    const assertion = expect(request).rejects.toMatchObject({
      category: "timeout",
      retryable: true,
    });

    await vi.advanceTimersByTimeAsync(50);
    await assertion;
  });

  it("保留 429 的稳定错误码和 Retry-After", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          code: "AUTH-429-001",
          data: null,
          message: "尝试次数过多，请稍后再试",
          timestamp: "2026-08-27T08:00:00Z",
          traceId: "rate-limit-test-001",
        }),
        {
          headers: {
            "Content-Type": "application/json",
            "Retry-After": "120",
          },
          status: 429,
        },
      ),
    );

    await expect(apiRequest("/auth/login")).rejects.toMatchObject({
      category: "rateLimited",
      code: "AUTH-429-001",
      retryAfterSeconds: 120,
      retryable: true,
      status: 429,
    });
  });
});
