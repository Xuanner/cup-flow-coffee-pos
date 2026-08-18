import { LoaderCircle } from "lucide-react";
import { Progress as ProgressPrimitive } from "radix-ui";
import type { HTMLAttributes } from "react";

import { cn } from "../../lib/cn";

export function Spinner({
  className,
  label = "正在加载…",
}: {
  className?: string;
  label?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-cf-xs text-secondary",
        className,
      )}
      role="status"
    >
      <LoaderCircle aria-hidden="true" className="size-icon-md animate-spin" />
      <span>{label}</span>
    </span>
  );
}

export function Skeleton({
  className,
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden="true"
      className={cn("animate-pulse rounded-control bg-subtle", className)}
      {...props}
    />
  );
}

export function Progress({
  className,
  label,
  value,
}: {
  className?: string;
  label: string;
  value: number;
}) {
  const normalizedValue = Math.min(100, Math.max(0, value));
  return (
    <div className={cn("grid gap-cf-xs", className)}>
      <div className="flex justify-between gap-cf-md text-body">
        <span>{label}</span>
        <span>{normalizedValue}%</span>
      </div>
      <ProgressPrimitive.Root
        aria-label={label}
        className="h-2 overflow-hidden rounded-full bg-subtle"
        value={normalizedValue}
      >
        <ProgressPrimitive.Indicator
          className="h-full rounded-full bg-action-primary transition-transform"
          style={{ transform: `translateX(-${100 - normalizedValue}%)` }}
        />
      </ProgressPrimitive.Root>
    </div>
  );
}
