import { z } from "zod";

import { ApiError } from "../../lib/api/api-error";
import { apiRequest } from "../../lib/api/http-client";
import {
  currentUserSchema,
  type CurrentUser,
  type LoginCredentials,
} from "./auth-model";

const csrfResponseSchema = z.object({
  headerName: z.literal("X-XSRF-TOKEN"),
  token: z.string().min(1),
});

export async function login(
  credentials: LoginCredentials,
  signal?: AbortSignal,
): Promise<CurrentUser> {
  const csrfResponse = await apiRequest<unknown>("/auth/csrf", { signal });
  const csrf = csrfResponseSchema.safeParse(csrfResponse.data);
  if (!csrf.success) throw invalidAuthResponse(csrf.error.flatten());

  const loginResponse = await apiRequest<unknown>("/auth/login", {
    body: JSON.stringify({
      username: credentials.username.trim(),
      password: credentials.password,
    }),
    headers: { [csrf.data.headerName]: csrf.data.token },
    method: "POST",
    signal,
  });
  const user = currentUserSchema.safeParse(loginResponse.data);
  if (!user.success) throw invalidAuthResponse(user.error.flatten());
  return user.data;
}

export async function getCurrentUser(
  signal?: AbortSignal,
): Promise<CurrentUser> {
  const response = await apiRequest<unknown>("/auth/me", { signal });
  const user = currentUserSchema.safeParse(response.data);
  if (!user.success) throw invalidAuthResponse(user.error.flatten());
  return user.data;
}

function invalidAuthResponse(details: unknown): ApiError {
  return new ApiError({
    category: "server",
    details,
    message: "登录服务响应格式无效。",
    retryable: true,
  });
}
