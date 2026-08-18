import type {
  HTMLAttributes,
  TableHTMLAttributes,
  TdHTMLAttributes,
  ThHTMLAttributes,
  ReactNode,
} from "react";

import { cn } from "../../lib/cn";

export function Table({
  className,
  ...props
}: TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto rounded-panel border border-subtle-border bg-surface">
      <table
        className={cn("w-full border-collapse text-left text-table", className)}
        {...props}
      />
    </div>
  );
}

export function TableHeader({
  className,
  ...props
}: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead className={cn("bg-subtle text-secondary", className)} {...props} />
  );
}

export function TableBody({
  className,
  ...props
}: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody
      className={cn("divide-y divide-subtle-border", className)}
      {...props}
    />
  );
}

export function TableRow({
  className,
  selected,
  ...props
}: HTMLAttributes<HTMLTableRowElement> & { selected?: boolean }) {
  return (
    <tr
      aria-selected={selected || undefined}
      className={cn(
        "transition-colors hover:bg-subtle aria-selected:bg-selected",
        className,
      )}
      {...props}
    />
  );
}

export function TableHead({
  className,
  scope = "col",
  ...props
}: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn("h-touch px-cf-md font-medium", className)}
      scope={scope}
      {...props}
    />
  );
}

export function TableCell({
  className,
  ...props
}: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn("h-[52px] px-cf-md", className)} {...props} />;
}

export function TableStatus({
  children,
  colSpan,
  loading,
}: {
  children: ReactNode;
  colSpan: number;
  loading?: boolean;
}) {
  return (
    <TableRow>
      <TableCell
        aria-busy={loading || undefined}
        className="h-32 text-center text-secondary"
        colSpan={colSpan}
      >
        {children}
      </TableCell>
    </TableRow>
  );
}
