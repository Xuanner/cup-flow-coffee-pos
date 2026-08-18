import { CircleAlert, CircleCheck, Info, TriangleAlert, X } from "lucide-react";
import { Toast as ToastPrimitive } from "radix-ui";
import type { PropsWithChildren, ReactNode } from "react";

import { cn } from "../../lib/cn";

export function ToastProvider({ children }: PropsWithChildren) {
  return (
    <ToastPrimitive.Provider duration={5000} swipeDirection="right">
      {children}
      <ToastPrimitive.Viewport className="fixed right-cf-md bottom-cf-md z-[60] flex w-[min(360px,calc(100vw-32px))] flex-col gap-cf-xs outline-none" />
    </ToastPrimitive.Provider>
  );
}

export interface ToastMessageProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  tone?: "info" | "success" | "warning" | "error";
  type?: "foreground" | "background";
  action?: ReactNode;
  actionAltText?: string;
}

const toneStyles = {
  info: { icon: Info, className: "border-l-info-border text-info" },
  success: {
    icon: CircleCheck,
    className: "border-l-success-border text-success",
  },
  warning: {
    icon: TriangleAlert,
    className: "border-l-warning-border text-warning",
  },
  error: {
    icon: CircleAlert,
    className: "border-l-error-border text-error",
  },
};

export function ToastMessage({
  action,
  actionAltText = "执行操作",
  description,
  onOpenChange,
  open,
  title,
  tone = "info",
  type = "foreground",
}: ToastMessageProps) {
  const { className, icon: Icon } = toneStyles[tone];
  return (
    <ToastPrimitive.Root
      className={cn(
        "data-[state=closed]:animate-out data-[state=open]:animate-in grid grid-cols-[auto_1fr_auto] items-start gap-cf-sm rounded-panel border border-l-4 border-subtle-border bg-surface p-cf-md text-primary shadow-lg",
        className,
      )}
      onOpenChange={onOpenChange}
      open={open}
      type={type}
    >
      <Icon aria-hidden="true" className="mt-0.5 size-icon-lg" />
      <div className="min-w-0">
        <ToastPrimitive.Title className="font-semibold text-primary">
          {title}
        </ToastPrimitive.Title>
        {description ? (
          <ToastPrimitive.Description className="mt-cf-2xs text-secondary">
            {description}
          </ToastPrimitive.Description>
        ) : null}
        {action ? (
          <ToastPrimitive.Action
            altText={actionAltText}
            className="mt-cf-xs font-medium text-primary underline underline-offset-4 outline-none focus-visible:ring-2 focus-visible:ring-focus"
          >
            {action}
          </ToastPrimitive.Action>
        ) : null}
      </div>
      <ToastPrimitive.Close
        aria-label="关闭通知"
        className="grid size-8 place-items-center rounded-control text-secondary outline-none hover:bg-subtle focus-visible:ring-2 focus-visible:ring-focus"
      >
        <X aria-hidden="true" className="size-icon-sm" />
      </ToastPrimitive.Close>
    </ToastPrimitive.Root>
  );
}
