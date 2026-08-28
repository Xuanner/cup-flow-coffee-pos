import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";

import { ApiError } from "../../lib/api/api-error";
import { registerUnauthenticatedHandler } from "../../lib/api/http-client";
import { getCurrentUser } from "./auth-api";
import { AuthContext } from "./auth-provider-context";
import { useAuthStore } from "./auth-store";

export function AuthProvider({ children }: PropsWithChildren) {
  const beginSessionCheck = useAuthStore((state) => state.beginSessionCheck);
  const clearCurrentUser = useAuthStore((state) => state.clearCurrentUser);
  const failSessionCheck = useAuthStore((state) => state.failSessionCheck);
  const expireSession = useAuthStore((state) => state.expireSession);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const [attempt, setAttempt] = useState(0);

  useEffect(
    () => registerUnauthenticatedHandler(expireSession),
    [expireSession],
  );

  useEffect(() => {
    const controller = new AbortController();
    beginSessionCheck();
    void getCurrentUser(controller.signal)
      .then(setCurrentUser)
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (
          error instanceof ApiError &&
          (error.code === "AUTH-401-001" ||
            error.category === "unauthenticated")
        ) {
          clearCurrentUser();
          return;
        }
        failSessionCheck(
          error instanceof ApiError
            ? error.message
            : "无法确认登录状态，请稍后重试。",
        );
      });
    return () => controller.abort();
  }, [
    attempt,
    beginSessionCheck,
    clearCurrentUser,
    failSessionCheck,
    setCurrentUser,
  ]);

  const retrySessionCheck = useCallback(() => {
    setAttempt((current) => current + 1);
  }, []);
  const value = useMemo(() => ({ retrySessionCheck }), [retrySessionCheck]);

  return <AuthContext value={value}>{children}</AuthContext>;
}
