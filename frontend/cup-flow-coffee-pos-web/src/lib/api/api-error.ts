export type ApiErrorCategory =
  | "validation"
  | "unauthenticated"
  | "forbidden"
  | "conflict"
  | "server"
  | "network"
  | "timeout"
  | "cancelled"
  | "unknown";

interface ApiErrorOptions {
  category: ApiErrorCategory;
  message: string;
  status?: number;
  details?: unknown;
  retryable?: boolean;
  cause?: unknown;
}

export class ApiError extends Error {
  readonly category: ApiErrorCategory;
  readonly status?: number;
  readonly details?: unknown;
  readonly retryable: boolean;

  constructor({
    category,
    cause,
    details,
    message,
    retryable = false,
    status,
  }: ApiErrorOptions) {
    super(message, { cause });
    this.name = "ApiError";
    this.category = category;
    this.status = status;
    this.details = details;
    this.retryable = retryable;
  }
}

const categoryByStatus = (status: number): ApiErrorCategory => {
  if (status === 400 || status === 422) return "validation";
  if (status === 401) return "unauthenticated";
  if (status === 403) return "forbidden";
  if (status === 409) return "conflict";
  if (status >= 500) return "server";
  return "unknown";
};

const defaultMessage: Record<ApiErrorCategory, string> = {
  validation: "提交内容有误，请检查后重试。",
  unauthenticated: "登录状态已失效，请重新登录。",
  forbidden: "你没有执行此操作的权限。",
  conflict: "数据已发生变化，请刷新后重试。",
  server: "服务暂时不可用，请稍后重试。",
  network: "无法连接到服务，请检查网络后重试。",
  timeout: "请求超时，请确认当前状态后重试。",
  cancelled: "请求已取消。",
  unknown: "请求失败，请稍后重试。",
};

function responseMessage(body: unknown): string | undefined {
  if (!body || typeof body !== "object") return undefined;
  const message = Reflect.get(body, "message");
  return typeof message === "string" && message.trim() ? message : undefined;
}

export function apiErrorFromResponse(status: number, body: unknown): ApiError {
  const category = categoryByStatus(status);
  const mayUseResponseMessage =
    category === "validation" || category === "conflict";

  return new ApiError({
    category,
    details: body,
    message:
      (mayUseResponseMessage ? responseMessage(body) : undefined) ??
      defaultMessage[category],
    retryable: category === "conflict" || category === "server",
    status,
  });
}

export function apiErrorFromCaughtValue(
  error: unknown,
  context: { externallyAborted: boolean; timedOut: boolean },
): ApiError {
  if (error instanceof ApiError) return error;

  if (context.timedOut) {
    return new ApiError({
      category: "timeout",
      cause: error,
      message: defaultMessage.timeout,
      retryable: true,
    });
  }

  if (context.externallyAborted) {
    return new ApiError({
      category: "cancelled",
      cause: error,
      message: defaultMessage.cancelled,
    });
  }

  if (error instanceof TypeError) {
    return new ApiError({
      category: "network",
      cause: error,
      message: defaultMessage.network,
      retryable: true,
    });
  }

  return new ApiError({
    category: "unknown",
    cause: error,
    message: defaultMessage.unknown,
  });
}
