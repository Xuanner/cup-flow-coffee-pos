import {
  CircleCheck,
  CircleX,
  LoaderCircle,
  RefreshCw,
  Server,
} from "lucide-react";

import { Button } from "../../components/ui/Button";
import { ApiError } from "../../lib/api/api-error";
import type { HealthResponse } from "./health-api";
import { useHealthQuery } from "./health-query";

const categoryLabel = {
  validation: "请求参数错误",
  authenticationFailed: "账号或密码错误",
  securityValidation: "安全校验失败",
  rateLimited: "请求过于频繁",
  unauthenticated: "登录状态失效",
  forbidden: "没有访问权限",
  conflict: "数据状态冲突",
  server: "服务异常",
  network: "网络连接失败",
  timeout: "请求超时",
  cancelled: "请求已取消",
  unknown: "未知错误",
} as const;

export function SystemPage() {
  const healthQuery = useHealthQuery();

  return (
    <section className="mx-auto max-w-4xl p-4 sm:p-6 lg:p-8">
      <header className="mb-6">
        <p className="mb-2 text-sm font-medium text-brand">
          Cup Flow Coffee POS
        </p>
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
          系统状态
        </h1>
        <p className="mt-2 text-sm leading-6 text-secondary sm:text-base">
          检查前端与后端服务的连接状态。检查失败不会改变业务数据。
        </p>
      </header>

      <div className="rounded-panel border border-subtle-border bg-surface p-6 shadow-sm">
        <div className="flex items-center gap-3 border-b border-subtle-border pb-4">
          <span className="grid size-10 place-items-center rounded-control bg-subtle text-secondary">
            <Server aria-hidden="true" size={21} />
          </span>
          <div>
            <h2 className="font-semibold">后端健康检查</h2>
            <p className="text-sm text-tertiary">GET /api/v1/health</p>
          </div>
        </div>

        <div className="pt-6">
          {healthQuery.isPending ? <HealthLoading /> : null}
          {healthQuery.isSuccess ? (
            <HealthSuccess health={healthQuery.data} />
          ) : null}
          {healthQuery.isError ? (
            <HealthFailure
              error={healthQuery.error}
              isRetrying={healthQuery.isFetching}
              onRetry={() => void healthQuery.refetch()}
            />
          ) : null}
        </div>
      </div>
    </section>
  );
}

function HealthLoading() {
  return (
    <div aria-live="polite" className="flex items-start gap-3" role="status">
      <LoaderCircle
        aria-hidden="true"
        className="mt-0.5 animate-spin text-secondary"
        size={22}
      />
      <div>
        <p className="font-medium">正在检查后端服务</p>
        <p className="mt-1 text-sm text-secondary">
          请稍候，通常只需要几秒钟。
        </p>
      </div>
    </div>
  );
}

function HealthSuccess({ health }: { health: HealthResponse }) {
  return (
    <div aria-live="polite" className="flex items-start gap-3" role="status">
      <CircleCheck
        aria-hidden="true"
        className="mt-0.5 text-success"
        size={22}
      />
      <div>
        <p className="font-medium text-success">后端服务运行正常</p>
        <p className="mt-1 text-sm text-secondary">
          应用服务与数据库连接均正常。
        </p>
        <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
          <div className="rounded-control bg-subtle p-3">
            <dt className="text-tertiary">应用服务</dt>
            <dd className="mt-1 font-medium text-success">
              {health.application}
            </dd>
          </div>
          <div className="rounded-control bg-subtle p-3">
            <dt className="text-tertiary">数据库</dt>
            <dd className="mt-1 font-medium text-success">{health.database}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}

interface HealthFailureProps {
  error: Error;
  isRetrying: boolean;
  onRetry: () => void;
}

function HealthFailure({ error, isRetrying, onRetry }: HealthFailureProps) {
  const apiError =
    error instanceof ApiError
      ? error
      : new ApiError({ category: "unknown", message: "健康检查失败。" });

  return (
    <div className="flex items-start gap-3" role="alert">
      <CircleX aria-hidden="true" className="mt-0.5 text-error" size={22} />
      <div className="flex-1">
        <p className="font-medium text-error">
          {categoryLabel[apiError.category]}
        </p>
        <p className="mt-1 text-sm text-secondary">{apiError.message}</p>
        <Button
          className="mt-5"
          disabled={isRetrying}
          onClick={onRetry}
          variant="secondary"
        >
          <RefreshCw
            aria-hidden="true"
            className={isRetrying ? "animate-spin" : undefined}
            size={18}
          />
          {isRetrying ? "正在重试" : "重新检查"}
        </Button>
      </div>
    </div>
  );
}
