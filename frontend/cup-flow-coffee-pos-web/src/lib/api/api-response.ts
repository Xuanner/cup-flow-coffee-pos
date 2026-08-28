import { z } from "zod";

import { ApiError } from "./api-error";

const apiResponseSchema = z.object({
  code: z.string().min(1),
  data: z.unknown().optional(),
  message: z.string(),
  timestamp: z.string().min(1),
  traceId: z.string().min(1),
});

export interface ApiResponse<T> {
  code: string;
  data: T;
  message: string;
  timestamp: string;
  traceId: string;
}

export function parseApiResponse<T>(payload: unknown): ApiResponse<T> {
  const result = apiResponseSchema.safeParse(payload);

  if (!result.success) {
    throw new ApiError({
      category: "server",
      details: result.error.flatten(),
      message: "服务响应格式无效。",
      retryable: true,
    });
  }

  if (result.data.code !== "SUCCESS") {
    throw new ApiError({
      category: "unknown",
      details: result.data,
      message: result.data.message || "请求未成功。",
    });
  }

  return {
    ...result.data,
    data: result.data.data ?? null,
  } as ApiResponse<T>;
}
