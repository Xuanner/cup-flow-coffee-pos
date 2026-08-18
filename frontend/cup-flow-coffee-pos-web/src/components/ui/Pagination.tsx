import { ChevronLeft, ChevronRight } from "lucide-react";

import { cn } from "../../lib/cn";
import { Button } from "./Button";

export interface PaginationProps {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  summary?: string;
  className?: string;
}

export function Pagination({
  className,
  onPageChange,
  page,
  pageCount,
  summary,
}: PaginationProps) {
  const pages = getVisiblePages(page, pageCount);

  return (
    <nav
      aria-label="分页"
      className={cn(
        "flex flex-wrap items-center justify-between gap-cf-md",
        className,
      )}
    >
      <div className="flex items-center gap-cf-2xs">
        <Button
          aria-label="上一页"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
          size="icon"
          variant="secondary"
        >
          <ChevronLeft aria-hidden="true" className="size-icon-md" />
        </Button>
        {pages.map((item, index) =>
          item === "ellipsis" ? (
            <span
              aria-hidden="true"
              className="grid size-touch place-items-center text-tertiary"
              key={`ellipsis-${index}`}
            >
              …
            </span>
          ) : (
            <Button
              aria-current={item === page ? "page" : undefined}
              aria-label={`第 ${item} 页`}
              className={item === page ? "bg-selected" : undefined}
              key={item}
              onClick={() => onPageChange(item)}
              size="icon"
              variant="ghost"
            >
              {item}
            </Button>
          ),
        )}
        <Button
          aria-label="下一页"
          disabled={page >= pageCount}
          onClick={() => onPageChange(page + 1)}
          size="icon"
          variant="secondary"
        >
          <ChevronRight aria-hidden="true" className="size-icon-md" />
        </Button>
      </div>
      {summary ? <p className="text-helper text-tertiary">{summary}</p> : null}
    </nav>
  );
}

function getVisiblePages(page: number, pageCount: number) {
  if (pageCount <= 7) {
    return Array.from(
      { length: Math.max(0, pageCount) },
      (_, index) => index + 1,
    );
  }

  const values: Array<number | "ellipsis"> = [1];
  if (page > 3) values.push("ellipsis");
  for (
    let current = Math.max(2, page - 1);
    current <= Math.min(pageCount - 1, page + 1);
    current += 1
  ) {
    values.push(current);
  }
  if (page < pageCount - 2) values.push("ellipsis");
  values.push(pageCount);
  return values;
}
