import { z } from "zod";

import { ApiError } from "../../lib/api/api-error";
import { apiRequest } from "../../lib/api/http-client";

const systemHealthSchema = z.object({
  application: z.enum(["UP", "DOWN"]),
  database: z.enum(["UP", "DOWN"]),
});

export type HealthResponse = z.infer<typeof systemHealthSchema> & {
  timestamp: string;
  traceId: string;
};

export async function getHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const response = await apiRequest<unknown>("/health", { signal });
  const result = systemHealthSchema.safeParse(response.data);

  if (!result.success) {
    throw new ApiError({
      category: "server",
      details: result.error.flatten(),
      message: "健康检查响应格式无效。",
      retryable: true,
    });
  }

  if (result.data.application === "DOWN" || result.data.database === "DOWN") {
    throw new ApiError({
      category: "server",
      details: result.data,
      message: "后端服务当前不可用。",
      retryable: true,
    });
  }

  return {
    ...result.data,
    timestamp: response.timestamp,
    traceId: response.traceId,
  };
}
