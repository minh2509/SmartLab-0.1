import { Check, ChevronsUpDown, X } from "lucide-react";
import { useState } from "react";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

export type AdminPickerOption = {
  value: string;
  label: string;
  description?: string;
  keywords?: string;
  disabled?: boolean;
};

type PickerAccessibility = {
  id?: string;
  required?: boolean;
  "aria-required"?: boolean;
  "aria-invalid"?: boolean;
  "aria-describedby"?: string;
};

export function AdminEntityPicker({
  options,
  value,
  onChange,
  placeholder,
  searchPlaceholder,
  emptyMessage,
  allLabel,
  loading,
  error,
  disabled,
  className,
  required,
  ...accessibility
}: {
  options: AdminPickerOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  searchPlaceholder: string;
  emptyMessage: string;
  allLabel?: string;
  loading?: boolean;
  error?: string | null;
  disabled?: boolean;
  className?: string;
} & PickerAccessibility) {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);
  const unavailableValue = value && !selected ? value : null;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          role="combobox"
          aria-expanded={open}
          disabled={disabled}
          aria-required={required || accessibility["aria-required"]}
          className={cn(
            "flex min-h-9 w-full items-center justify-between gap-2 rounded-md border border-hairline bg-background px-2.5 py-2 text-left text-xs text-ink outline-none hover:bg-muted/35 focus-visible:ring-2 focus-visible:ring-[color:var(--cyan)]/35 disabled:cursor-not-allowed disabled:opacity-55",
            className,
          )}
          {...accessibility}
        >
          <span className="min-w-0">
            <span className={cn("block truncate", !selected && "text-ink-soft")}>
              {selected?.label ||
                (unavailableValue ? "Unavailable selection" : allLabel || placeholder)}
            </span>
            {selected?.description ? (
              <span className="mt-0.5 block truncate text-[10px] text-ink-soft">
                {selected.description}
              </span>
            ) : unavailableValue ? (
              <span className="mt-0.5 block truncate text-[10px] text-ink-soft">
                Refresh the choices or clear this selection.
              </span>
            ) : null}
          </span>
          <ChevronsUpDown className="h-3.5 w-3.5 shrink-0 text-ink-soft" />
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-[var(--radix-popover-trigger-width)] min-w-72 p-0">
        <Command>
          <CommandInput placeholder={searchPlaceholder} />
          <CommandList>
            {loading ? (
              <div className="px-3 py-6 text-center text-xs text-ink-soft">Loading options…</div>
            ) : error ? (
              <div role="alert" className="px-3 py-4 text-xs text-[color:var(--destructive)]">
                {error}
              </div>
            ) : (
              <>
                <CommandEmpty>{emptyMessage}</CommandEmpty>
                <CommandGroup>
                  {allLabel ? (
                    <CommandItem
                      value={`all ${allLabel}`}
                      onSelect={() => {
                        onChange("");
                        setOpen(false);
                      }}
                    >
                      <Check className={cn("h-3.5 w-3.5", value ? "opacity-0" : "opacity-100")} />
                      <span>{allLabel}</span>
                    </CommandItem>
                  ) : null}
                  {options.map((option) => (
                    <CommandItem
                      key={option.value}
                      value={`${option.label} ${option.description || ""} ${option.keywords || ""} ${option.value}`}
                      disabled={option.disabled}
                      onSelect={() => {
                        onChange(option.value);
                        setOpen(false);
                      }}
                    >
                      <Check
                        className={cn(
                          "h-3.5 w-3.5 shrink-0",
                          option.value === value ? "opacity-100" : "opacity-0",
                        )}
                      />
                      <span className="min-w-0">
                        <span className="block truncate">{option.label}</span>
                        {option.description ? (
                          <span className="block truncate text-[10px] text-ink-soft">
                            {option.description}
                          </span>
                        ) : null}
                      </span>
                    </CommandItem>
                  ))}
                </CommandGroup>
              </>
            )}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

export function AdminEntityMultiPicker({
  options,
  values,
  onChange,
  placeholder,
  searchPlaceholder,
  emptyMessage,
  loading,
  error,
  disabled,
  className,
  required,
  ...accessibility
}: {
  options: AdminPickerOption[];
  values: string[];
  onChange: (values: string[]) => void;
  placeholder: string;
  searchPlaceholder: string;
  emptyMessage: string;
  loading?: boolean;
  error?: string | null;
  disabled?: boolean;
  className?: string;
} & PickerAccessibility) {
  const [open, setOpen] = useState(false);
  const selected = values
    .map((value) => options.find((option) => option.value === value))
    .filter((option): option is AdminPickerOption => Boolean(option));
  const unavailableValues = values.filter(
    (value) => !options.some((option) => option.value === value),
  );

  const toggle = (value: string) => {
    onChange(values.includes(value) ? values.filter((item) => item !== value) : [...values, value]);
  };

  return (
    <div className={className}>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            type="button"
            role="combobox"
            aria-expanded={open}
            disabled={disabled}
            aria-required={required || accessibility["aria-required"]}
            className="flex min-h-9 w-full items-center justify-between gap-2 rounded-md border border-hairline bg-background px-2.5 py-2 text-left text-xs text-ink outline-none hover:bg-muted/35 focus-visible:ring-2 focus-visible:ring-[color:var(--cyan)]/35 disabled:cursor-not-allowed disabled:opacity-55"
            {...accessibility}
          >
            <span className={cn("truncate", values.length === 0 && "text-ink-soft")}>
              {values.length === 0
                ? placeholder
                : `${values.length} recipient${values.length === 1 ? "" : "s"} selected`}
            </span>
            <ChevronsUpDown className="h-3.5 w-3.5 shrink-0 text-ink-soft" />
          </button>
        </PopoverTrigger>
        <PopoverContent
          align="start"
          className="w-[var(--radix-popover-trigger-width)] min-w-80 p-0"
        >
          <Command>
            <CommandInput placeholder={searchPlaceholder} />
            <CommandList>
              {loading ? (
                <div className="px-3 py-6 text-center text-xs text-ink-soft">
                  Loading recipients…
                </div>
              ) : error ? (
                <div role="alert" className="px-3 py-4 text-xs text-[color:var(--destructive)]">
                  {error}
                </div>
              ) : (
                <>
                  <CommandEmpty>{emptyMessage}</CommandEmpty>
                  <CommandGroup>
                    {options.map((option) => (
                      <CommandItem
                        key={option.value}
                        value={`${option.label} ${option.description || ""} ${option.keywords || ""} ${option.value}`}
                        disabled={option.disabled}
                        onSelect={() => toggle(option.value)}
                      >
                        <span
                          className={cn(
                            "grid h-4 w-4 shrink-0 place-items-center rounded border",
                            values.includes(option.value)
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-hairline",
                          )}
                        >
                          {values.includes(option.value) ? <Check className="h-3 w-3" /> : null}
                        </span>
                        <span className="min-w-0">
                          <span className="block truncate">{option.label}</span>
                          {option.description ? (
                            <span className="block truncate text-[10px] text-ink-soft">
                              {option.description}
                            </span>
                          ) : null}
                          <span className="sr-only">
                            {values.includes(option.value) ? "Selected" : "Not selected"}
                          </span>
                        </span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                </>
              )}
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {values.length > 0 ? (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {selected.map((option) => (
            <span
              key={option.value}
              className="inline-flex max-w-full items-center gap-1 rounded-full border border-hairline bg-muted/45 py-1 pl-2.5 pr-1 text-[11px] text-ink"
            >
              <span className="max-w-48 truncate">{option.label}</span>
              <button
                type="button"
                disabled={disabled}
                aria-label={`Remove ${option.label}`}
                onClick={() => toggle(option.value)}
                className="grid h-5 w-5 place-items-center rounded-full text-ink-soft hover:bg-background hover:text-ink disabled:opacity-45"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
          {unavailableValues.map((value) => (
            <span
              key={value}
              className="inline-flex max-w-full items-center gap-1 rounded-full border border-[color:var(--destructive)]/30 bg-[color-mix(in_oklab,var(--destructive)_7%,transparent)] py-1 pl-2.5 pr-1 text-[11px] text-[color:var(--destructive)]"
            >
              <span className="max-w-48 truncate">Unavailable recipient</span>
              <button
                type="button"
                disabled={disabled}
                aria-label="Remove unavailable recipient"
                onClick={() => toggle(value)}
                className="grid h-5 w-5 place-items-center rounded-full hover:bg-background disabled:opacity-45"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      ) : null}
      <span className="sr-only" aria-live="polite">
        {values.length} recipient{values.length === 1 ? "" : "s"} selected.
      </span>
    </div>
  );
}
