import { CircleAlert, X } from "lucide-react";
import { Dialog as DialogPrimitive } from "radix-ui";
import type { ComponentProps, ReactNode } from "react";

import { cn } from "../../lib/cn";

export interface DialogProps {
  trigger?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  error?: ReactNode;
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  closeLabel?: string;
  className?: string;
}

export function Dialog({
  children,
  className,
  closeLabel = "关闭对话框",
  defaultOpen,
  description,
  error,
  footer,
  onOpenChange,
  open,
  title,
  trigger,
}: DialogProps) {
  return (
    <DialogPrimitive.Root
      defaultOpen={defaultOpen}
      onOpenChange={onOpenChange}
      open={open}
    >
      {trigger ? (
        <DialogPrimitive.Trigger asChild>{trigger}</DialogPrimitive.Trigger>
      ) : null}
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="data-[state=closed]:animate-out data-[state=open]:animate-in fixed inset-0 z-50 bg-neutral-950/45" />
        <DialogPrimitive.Content
          className={cn(
            "fixed right-cf-md bottom-cf-md left-cf-md z-50 max-h-[calc(100vh-32px)] overflow-y-auto rounded-panel border border-subtle-border bg-surface p-cf-lg shadow-xl outline-none focus-visible:ring-2 focus-visible:ring-focus sm:top-1/2 sm:right-auto sm:bottom-auto sm:left-1/2 sm:w-[min(480px,calc(100vw-32px))] sm:-translate-x-1/2 sm:-translate-y-1/2",
            className,
          )}
        >
          <div className="grid gap-cf-md">
            <div className="pr-cf-xl">
              <DialogPrimitive.Title className="text-module-title font-semibold">
                {title}
              </DialogPrimitive.Title>
              {description ? (
                <DialogPrimitive.Description className="mt-cf-xs text-secondary">
                  {description}
                </DialogPrimitive.Description>
              ) : null}
            </div>
            <DialogPrimitive.Close
              aria-label={closeLabel}
              className="absolute top-cf-sm right-cf-sm grid size-touch place-items-center rounded-control text-secondary outline-none hover:bg-subtle hover:text-primary focus-visible:ring-2 focus-visible:ring-focus"
            >
              <X aria-hidden="true" className="size-icon-md" />
            </DialogPrimitive.Close>
            <div className="grid gap-cf-md">{children}</div>
            {error ? (
              <div
                className="flex gap-cf-xs rounded-control bg-error-bg p-cf-sm text-error"
                role="alert"
              >
                <CircleAlert
                  aria-hidden="true"
                  className="mt-0.5 size-icon-md shrink-0"
                />
                <span>{error}</span>
              </div>
            ) : null}
            {footer ? (
              <div className="flex flex-wrap justify-end gap-cf-xs border-t border-subtle-border pt-cf-md">
                {footer}
              </div>
            ) : null}
          </div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

export function DialogClose(
  props: ComponentProps<typeof DialogPrimitive.Close>,
) {
  return <DialogPrimitive.Close {...props} />;
}
