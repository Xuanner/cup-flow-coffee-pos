import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";

import { cn } from "../../lib/cn";

const badgeVariants = cva(
  "inline-flex w-fit items-center justify-center gap-cf-2xs rounded-full font-medium",
  {
    variants: {
      size: {
        small: "min-h-6 px-cf-xs text-helper",
        medium: "min-h-7 px-[10px] text-body",
      },
      tone: {
        neutral: "bg-subtle text-secondary",
        brand: "bg-brand-soft text-brand",
        success: "bg-success-bg text-success",
        warning: "bg-warning-bg text-warning",
        error: "bg-error-bg text-error",
        info: "bg-info-bg text-info",
      },
      appearance: {
        subtle: "",
        solid: "bg-primary text-on-action",
      },
    },
    compoundVariants: [
      { tone: "brand", appearance: "solid", className: "bg-brand-primary" },
      { tone: "success", appearance: "solid", className: "bg-success" },
      { tone: "warning", appearance: "solid", className: "bg-warning" },
      { tone: "error", appearance: "solid", className: "bg-error" },
      { tone: "info", appearance: "solid", className: "bg-info" },
    ],
    defaultVariants: {
      appearance: "subtle",
      size: "medium",
      tone: "neutral",
    },
  },
);

export interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {
  dot?: boolean;
}

export function Badge({
  appearance,
  children,
  className,
  dot,
  size,
  tone,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(badgeVariants({ appearance, size, tone }), className)}
      {...props}
    >
      {dot ? (
        <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      ) : null}
      {children}
    </span>
  );
}
