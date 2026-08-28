import { Coffee, Eye, EyeOff } from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router";

import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { ApiError } from "../../lib/api/api-error";
import { login } from "./auth-api";
import { useAuthStore } from "./auth-store";
import { safeReturnPath } from "./safe-return-path";

interface FieldErrors {
  username?: string;
  password?: string;
}

export function AuthPage() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const initiallyAuthenticated = useRef(Boolean(currentUser));
  const location = useLocation();
  const navigate = useNavigate();
  const usernameRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0);

  useEffect(() => {
    if (initiallyAuthenticated.current && currentUser) {
      navigate(currentUser.defaultPath, { replace: true });
    }
  }, [currentUser, navigate]);

  useEffect(() => {
    if (rateLimitSeconds <= 0) return;
    const timer = window.setInterval(() => {
      setRateLimitSeconds((seconds) => Math.max(0, seconds - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [rateLimitSeconds]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting || rateLimitSeconds > 0) return;

    const errors: FieldErrors = {};
    if (!username.trim()) errors.username = "请输入账号。";
    if (!password) errors.password = "请输入密码。";
    setFieldErrors(errors);
    setSubmitError("");
    if (errors.username) {
      usernameRef.current?.focus();
      return;
    }
    if (errors.password) {
      passwordRef.current?.focus();
      return;
    }

    setSubmitting(true);
    try {
      const user = await login({ username, password });
      setCurrentUser(user);
      navigate(safeReturnPath(location.state?.from, user), { replace: true });
    } catch (error) {
      setPassword("");
      setSubmitError(
        error instanceof ApiError ? error.message : "登录失败，请稍后重试。",
      );
      if (
        error instanceof ApiError &&
        error.category === "rateLimited" &&
        error.retryAfterSeconds
      ) {
        setRateLimitSeconds(error.retryAfterSeconds);
      }
      passwordRef.current?.focus();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen place-items-center bg-canvas p-4">
      <section className="w-full max-w-md rounded-panel border border-subtle-border bg-surface p-8 shadow-sm">
        <span className="grid size-12 place-items-center rounded-full bg-brand-soft text-brand">
          <Coffee aria-hidden="true" />
        </span>
        <p className="mt-6 text-sm font-medium text-brand">
          Cup Flow Coffee POS
        </p>
        <h1 className="mt-2 text-2xl font-semibold">员工登录</h1>
        <p className="mt-3 text-sm leading-6 text-secondary">
          使用门店员工账号进入收银与管理工作台。
        </p>

        <form
          className="mt-8 grid gap-cf-md"
          noValidate
          onSubmit={handleSubmit}
        >
          <Input
            autoComplete="username"
            error={fieldErrors.username}
            label="账号"
            maxLength={64}
            onChange={(event) => setUsername(event.target.value)}
            ref={usernameRef}
            value={username}
          />
          <div className="grid gap-cf-xs">
            <Input
              autoComplete="current-password"
              error={fieldErrors.password}
              label="密码"
              maxLength={128}
              onChange={(event) => setPassword(event.target.value)}
              ref={passwordRef}
              type={showPassword ? "text" : "password"}
              value={password}
            />
            <Button
              aria-label={showPassword ? "隐藏密码" : "显示密码"}
              className="justify-self-end"
              onClick={() => setShowPassword((visible) => !visible)}
              size="compact"
              type="button"
              variant="ghost"
            >
              {showPassword ? (
                <EyeOff aria-hidden="true" className="size-icon-sm" />
              ) : (
                <Eye aria-hidden="true" className="size-icon-sm" />
              )}
              {showPassword ? "隐藏密码" : "显示密码"}
            </Button>
          </div>
          {submitError ? (
            <p className="text-sm text-error" role="alert">
              {submitError}
            </p>
          ) : null}
          {rateLimitSeconds > 0 ? (
            <p aria-live="polite" className="text-sm text-secondary">
              {rateLimitSeconds} 秒后可重新尝试。
            </p>
          ) : null}
          <Button
            className="mt-cf-xs w-full"
            disabled={rateLimitSeconds > 0}
            loading={submitting}
            loadingLabel="正在登录"
            type="submit"
          >
            {rateLimitSeconds > 0
              ? `请稍后再试（${rateLimitSeconds}s）`
              : "登录"}
          </Button>
        </form>
      </section>
    </main>
  );
}
