import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";

import { ApiError } from "../../lib/api/api-error";
import { getCurrentUser } from "./auth-api";
import { AuthContext } from "./auth-provider-context";
import { useAuthStore } from "./auth-store";

export function AuthProvider({ children }: PropsWithChildren) {
  const beginSessionCheck = useAuthStore((state) => state.beginSessionCheck);
  const clearCurrentUser = useAuthStore((state) => state.clearCurrentUser);
  const failSessionCheck = useAuthStore((state) => state.failSessionCheck);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const [attempt, setAttempt] = useState(0);

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
