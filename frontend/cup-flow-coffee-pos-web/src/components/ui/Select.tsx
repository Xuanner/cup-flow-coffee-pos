import { Check, ChevronDown, CircleAlert } from "lucide-react";
import { Label, Select as SelectPrimitive } from "radix-ui";
import { useId, type ReactNode } from "react";

import { cn } from "../../lib/cn";
import { controlClassName, describedBy } from "./field";

export interface SelectOption {
  value: string;
  label: ReactNode;
  disabled?: boolean;
}

export interface SelectProps {
  label: ReactNode;
  options: SelectOption[];
  value?: string;
  defaultValue?: string;
  placeholder?: string;
  disabled?: boolean;
  error?: ReactNode;
  description?: ReactNode;
  onValueChange?: (value: string) => void;
  name?: string;
  id?: string;
  className?: string;
}

export function Select({
  className,
  defaultValue,
  description,
  disabled,
  error,
  id: providedId,
  label,
  name,
  onValueChange,
  options,
  placeholder = "请选择",
  value,
}: SelectProps) {
  const generatedId = useId();
  const id = providedId ?? generatedId;
  const descriptionId = `${id}-description`;
  const errorId = `${id}-error`;

  return (
    <div className="grid gap-cf-xs">
      <Label.Root className="font-medium text-secondary" htmlFor={id}>
        {label}
      </Label.Root>
      <SelectPrimitive.Root
        defaultValue={defaultValue}
        disabled={disabled}
        name={name}
        onValueChange={onValueChange}
        value={value}
      >
        <SelectPrimitive.Trigger
          aria-describedby={describedBy(
            descriptionId,
            errorId,
            Boolean(description),
            Boolean(error),
          )}
          aria-invalid={Boolean(error) || undefined}
          className={cn(
            controlClassName,
            "flex h-touch items-center justify-between gap-cf-xs px-cf-md text-left data-[placeholder]:text-tertiary",
            className,
          )}
          id={id}
        >
          <SelectPrimitive.Value placeholder={placeholder} />
          <SelectPrimitive.Icon asChild>
            <ChevronDown aria-hidden="true" className="size-icon-md" />
          </SelectPrimitive.Icon>
        </SelectPrimitive.Trigger>
        <SelectPrimitive.Portal>
          <SelectPrimitive.Content
            className="z-50 max-h-72 min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-control border border-default bg-surface p-cf-2xs shadow-lg"
            position="popper"
            sideOffset={4}
          >
            <SelectPrimitive.Viewport>
              {options.map((option) => (
                <SelectPrimitive.Item
                  className="relative flex min-h-touch cursor-default items-center rounded-sm py-cf-xs pr-cf-md pl-cf-2xl text-primary outline-none data-[disabled]:pointer-events-none data-[disabled]:text-disabled-text data-[highlighted]:bg-subtle data-[state=checked]:font-medium"
                  disabled={option.disabled}
                  key={option.value}
                  value={option.value}
                >
                  <SelectPrimitive.ItemIndicator className="absolute left-cf-xs">
                    <Check aria-hidden="true" className="size-icon-md" />
                  </SelectPrimitive.ItemIndicator>
                  <SelectPrimitive.ItemText>
                    {option.label}
                  </SelectPrimitive.ItemText>
                </SelectPrimitive.Item>
              ))}
            </SelectPrimitive.Viewport>
          </SelectPrimitive.Content>
        </SelectPrimitive.Portal>
      </SelectPrimitive.Root>
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
}
