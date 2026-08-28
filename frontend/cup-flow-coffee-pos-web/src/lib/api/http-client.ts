import { environment } from "../../app/env";
import { apiErrorFromCaughtValue, apiErrorFromResponse } from "./api-error";
import { type ApiResponse, parseApiResponse } from "./api-response";

const DEFAULT_TIMEOUT_MS = 10_000;

function apiUrl(path: string): string {
  const baseUrl = environment.VITE_API_BASE_URL;
  return `${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
}

async function responseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;
  const contentType = response.headers.get("content-type") ?? "";
  return contentType.includes("application/json")
    ? response.json()
    : response.text();
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  timeoutMs = DEFAULT_TIMEOUT_MS,
): Promise<ApiResponse<T>> {
  const timeoutController = new AbortController();
  const timeoutId = window.setTimeout(
    () => timeoutController.abort(),
    timeoutMs,
  );
  const signal = init.signal
    ? AbortSignal.any([init.signal, timeoutController.signal])
    : timeoutController.signal;

  try {
    const response = await fetch(apiUrl(path), {
      ...init,
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...init.headers,
      },
      signal,
    });
    const body = await responseBody(response);

    if (!response.ok) {
      throw apiErrorFromResponse(
        response.status,
        body,
        response.headers.get("Retry-After"),
      );
    }

    return parseApiResponse<T>(body);
  } catch (error) {
    throw apiErrorFromCaughtValue(error, {
      externallyAborted: init.signal?.aborted ?? false,
      timedOut: timeoutController.signal.aborted,
    });
  } finally {
    window.clearTimeout(timeoutId);
  }
}
