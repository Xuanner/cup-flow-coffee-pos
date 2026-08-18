import { CircleAlert } from "lucide-react";
import { Label } from "radix-ui";
import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
} from "react";

import { cn } from "../../lib/cn";
import { controlClassName, describedBy } from "./field";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: ReactNode;
  description?: ReactNode;
  error?: ReactNode;
  inputSize?: "compact" | "comfortable";
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    className,
    description,
    error,
    id: providedId,
    inputSize = "comfortable",
    label,
    ...props
  },
  ref,
) {
  const generatedId = useId();
  const id = providedId ?? generatedId;
  const descriptionId = `${id}-description`;
  const errorId = `${id}-error`;

  return (
    <div className="grid gap-cf-xs">
      <Label.Root className="font-medium text-secondary" htmlFor={id}>
        {label}
      </Label.Root>
      <input
        aria-describedby={describedBy(
          descriptionId,
          errorId,
          Boolean(description),
          Boolean(error),
        )}
        aria-invalid={Boolean(error) || undefined}
        className={cn(
          controlClassName,
          inputSize === "compact" ? "h-9 px-cf-sm" : "h-touch px-cf-md",
          className,
        )}
        id={id}
        ref={ref}
        {...props}
      />
      {description ? (
        <p className="text-helper text-tertiary" id={descriptionId}>
          {description}
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
