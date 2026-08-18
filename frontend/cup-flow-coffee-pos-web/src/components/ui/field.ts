export function describedBy(
  descriptionId: string,
  errorId: string,
  hasDescription: boolean,
  hasError: boolean,
) {
  const ids = [
    hasDescription ? descriptionId : null,
    hasError ? errorId : null,
  ];
  return ids.filter(Boolean).join(" ") || undefined;
}

export const controlClassName =
  "w-full rounded-control border border-default bg-surface text-primary outline-none transition-colors placeholder:text-tertiary hover:border-strong-border focus-visible:border-focus focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2 focus-visible:ring-offset-surface disabled:cursor-not-allowed disabled:border-subtle-border disabled:bg-disabled disabled:text-disabled-text aria-invalid:border-error-border aria-invalid:focus-visible:ring-error";
