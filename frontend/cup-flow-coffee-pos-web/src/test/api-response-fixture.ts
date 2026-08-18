export const testTimestamp = "2026-08-18T03:37:47.291437Z";
export const testTraceId = "test-trace-id";

export function apiSuccessResponse(data: unknown) {
  return jsonResponse({
    code: "SUCCESS",
    data,
    message: "操作成功",
    timestamp: testTimestamp,
    traceId: testTraceId,
  });
}

export function apiFailureResponse(
  status: number,
  code: string,
  message: string,
) {
  return jsonResponse(
    {
      code,
      data: null,
      message,
      timestamp: testTimestamp,
      traceId: testTraceId,
    },
    status,
  );
}

export function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    headers: { "content-type": "application/json" },
    status,
  });
}
