import { renderHook, waitFor } from "@testing-library/react";

import {
  apiSuccessResponse,
  testTimestamp,
  testTraceId,
} from "../../test/api-response-fixture";
import { createQueryWrapper } from "../../test/query-wrapper";
import { useHealthQuery } from "./health-query";

describe("useHealthQuery", () => {
  it("TC-S1-HEALTH-301 通过 TanStack Query 获取健康状态", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({ application: "UP", database: "UP" }),
    );

    const { result } = renderHook(() => useHealthQuery(), {
      wrapper: createQueryWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({
      application: "UP",
      database: "UP",
      timestamp: testTimestamp,
      traceId: testTraceId,
    });
  });
});
