import {
  apiSuccessResponse,
  testTimestamp,
  testTraceId,
} from "../../test/api-response-fixture";
import { getHealth } from "./health-api";

describe("getHealth", () => {
  it("解析 UP 健康响应", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({ application: "UP", database: "UP" }),
    );

    await expect(getHealth()).resolves.toEqual({
      application: "UP",
      database: "UP",
      timestamp: testTimestamp,
      traceId: testTraceId,
    });
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/health",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
  });

  it("拒绝无效的健康响应", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({ application: "UNKNOWN", database: "UP" }),
    );

    await expect(getHealth()).rejects.toMatchObject({
      category: "server",
      message: "健康检查响应格式无效。",
    });
  });

  it("将 DOWN 状态视为服务异常", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({ application: "UP", database: "DOWN" }),
    );

    await expect(getHealth()).rejects.toMatchObject({
      category: "server",
      message: "后端服务当前不可用。",
    });
  });
});
