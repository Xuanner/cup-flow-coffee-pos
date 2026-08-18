import { CircleAlert, LoaderCircle, SearchIcon, X } from "lucide-react";
import { Label } from "radix-ui";
import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
} from "react";

import { cn } from "../../lib/cn";
import { controlClassName, describedBy } from "./field";

export interface SearchProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "type"
> {
  label: ReactNode;
  error?: ReactNode;
  message?: ReactNode;
  loading?: boolean;
  onClear?: () => void;
}

export const Search = forwardRef<HTMLInputElement, SearchProps>(function Search(
  {
    className,
    error,
    id: providedId,
    label,
    loading = false,
    message,
    onClear,
    value,
    ...props
  },
  ref,
) {
  const generatedId = useId();
  const id = providedId ?? generatedId;
  const messageId = `${id}-message`;
  const errorId = `${id}-error`;
  const hasValue = typeof value === "string" && value.length > 0;

  return (
    <div className="grid gap-cf-xs">
      <Label.Root className="sr-only" htmlFor={id}>
        {label}
      </Label.Root>
      <div className="relative">
        <SearchIcon
          aria-hidden="true"
          className="pointer-events-none absolute top-1/2 left-cf-sm size-icon-md -translate-y-1/2 text-tertiary"
        />
        <input
          aria-busy={loading || undefined}
          aria-describedby={describedBy(
            messageId,
            errorId,
            Boolean(message),
            Boolean(error),
          )}
          aria-invalid={Boolean(error) || undefined}
          className={cn(
            controlClassName,
            "h-touch pr-touch pl-cf-2xl [&::-webkit-search-cancel-button]:hidden",
            className,
          )}
          id={id}
          ref={ref}
          type="search"
          value={value}
          {...props}
        />
        {loading ? (
          <LoaderCircle
            aria-label="正在搜索"
            className="absolute top-1/2 right-cf-sm size-icon-md -translate-y-1/2 animate-spin text-secondary"
            role="status"
          />
        ) : hasValue && onClear ? (
          <button
            aria-label="清除搜索内容"
            className="absolute top-1/2 right-0 grid size-touch -translate-y-1/2 place-items-center rounded-control text-secondary outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-focus"
            onClick={onClear}
            type="button"
          >
            <X aria-hidden="true" className="size-icon-md" />
          </button>
        ) : null}
      </div>
      {message ? (
        <p className="text-helper text-tertiary" id={messageId}>
          {message}
        </p>
      ) : null}
      {error ? (
        <p
          className="flex items-center gap-cf-2xs text-helper text-error"
          id={errorId}
          role="alert"
        >
          <CircleAlert aria-hidden="true" className="size-icon-sm" />
          {error}
        </p>
      ) : null}
    </div>
  );
});
