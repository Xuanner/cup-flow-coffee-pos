import { cva, type VariantProps } from "class-variance-authority";
import { Slot } from "radix-ui";
import { LoaderCircle } from "lucide-react";
import type { ButtonHTMLAttributes, ReactNode } from "react";

import { cn } from "../../lib/cn";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-cf-xs rounded-control font-medium transition-colors outline-none focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2 focus-visible:ring-offset-surface disabled:pointer-events-none disabled:bg-disabled disabled:text-disabled-text disabled:opacity-100",
  {
    variants: {
      size: {
        compact: "min-h-9 px-cf-sm text-body",
        comfortable: "min-h-touch px-cf-md text-body",
        large: "min-h-[52px] px-cf-lg text-body-large",
        icon: "size-touch shrink-0 p-0",
      },
      variant: {
        primary:
          "bg-action-primary text-on-action hover:bg-action-primary-hover active:bg-action-primary-pressed",
        secondary:
          "border border-default bg-surface text-primary hover:bg-subtle",
        ghost:
          "bg-transparent text-secondary hover:bg-subtle hover:text-primary",
        danger: "bg-error text-on-action hover:bg-error/90 active:bg-error/80",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "comfortable",
    },
  },
);

export interface ButtonProps
  extends
    ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
  loadingLabel?: ReactNode;
}

export function Button({
  asChild,
  className,
  disabled,
  loading = false,
  loadingLabel = "处理中",
  size = "comfortable",
  variant = "primary",
  children,
  ...props
}: ButtonProps) {
  const Component = asChild ? Slot.Root : "button";

  return (
    <Component
      aria-busy={loading || undefined}
      className={cn(buttonVariants({ size, variant }), className)}
      disabled={asChild ? undefined : disabled || loading}
      {...props}
    >
      {loading ? (
        <>
          <LoaderCircle
            aria-hidden="true"
            className="size-icon-md animate-spin"
          />
          <span>{loadingLabel}</span>
        </>
      ) : (
        children
      )}
    </Component>
  );
}
