import { Inbox } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "../../lib/cn";

export interface EmptyStateProps {
  title: ReactNode;
  description?: ReactNode;
  icon?: ReactNode;
  primaryAction?: ReactNode;
  secondaryAction?: ReactNode;
  className?: string;
}

export function EmptyState({
  className,
  description,
  icon,
  primaryAction,
  secondaryAction,
  title,
}: EmptyStateProps) {
  return (
    <section
      className={cn(
        "flex min-h-60 flex-col items-center justify-center gap-cf-sm rounded-panel border border-dashed border-default bg-surface p-cf-xl text-center",
        className,
      )}
    >
      <span className="grid size-12 place-items-center rounded-full bg-subtle text-secondary">
        {icon ?? <Inbox aria-hidden="true" className="size-icon-lg" />}
      </span>
      <div className="grid gap-cf-2xs">
        <h3 className="text-body-large font-semibold">{title}</h3>
        {description ? <p className="text-secondary">{description}</p> : null}
      </div>
      {primaryAction || secondaryAction ? (
        <div className="mt-cf-sm flex flex-wrap justify-center gap-cf-xs">
          {primaryAction}
          {secondaryAction}
        </div>
      ) : null}
    </section>
  );
}
