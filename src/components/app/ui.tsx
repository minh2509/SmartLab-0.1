import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div className="max-w-2xl">
        {eyebrow ? (
          <div className="mb-2 text-[11px] uppercase tracking-[0.18em] text-ink-soft">
            {eyebrow}
          </div>
        ) : null}
        <h1 className="font-display text-3xl leading-tight text-ink md:text-4xl">{title}</h1>
        {description ? <p className="mt-2 text-sm text-ink-soft">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function StatTile({
  label,
  value,
  hint,
  tone,
}: {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: "cyan" | "emerald" | "violet" | "amber";
}) {
  const bar: Record<string, string> = {
    cyan: "bg-[color:var(--cyan)]",
    emerald: "bg-[color:var(--emerald-ink)]",
    violet: "bg-[color:var(--violet-ink)]",
    amber: "bg-[color:var(--amber-ink)]",
  };
  return (
    <div className="relative overflow-hidden rounded-xl border border-hairline bg-surface-elev p-5">
      {tone ? <div className={cn("absolute inset-x-0 top-0 h-0.5", bar[tone])} /> : null}
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div className="mt-2 font-display text-3xl text-ink">{value}</div>
      {hint ? <div className="mt-1 text-xs text-ink-soft">{hint}</div> : null}
    </div>
  );
}

export function Panel({
  title,
  description,
  action,
  children,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("rounded-xl border border-hairline bg-surface-elev", className)}>
      <header className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
        <div>
          <h2 className="text-sm font-semibold text-ink">{title}</h2>
          {description ? <p className="mt-0.5 text-xs text-ink-soft">{description}</p> : null}
        </div>
        {action}
      </header>
      <div className="p-5">{children}</div>
    </section>
  );
}

export function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-hairline px-6 py-10 text-center">
      <div className="text-sm font-medium text-ink">{title}</div>
      {hint ? <div className="mt-1 text-xs text-ink-soft">{hint}</div> : null}
    </div>
  );
}

export function StatusPill({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "neutral" | "cyan" | "emerald" | "violet" | "amber" | "rose";
}) {
  const tones: Record<string, string> = {
    neutral: "bg-muted text-ink-soft",
    cyan: "bg-[color-mix(in_oklab,var(--cyan)_14%,transparent)] text-[color:var(--cyan)]",
    emerald:
      "bg-[color-mix(in_oklab,var(--emerald-ink)_14%,transparent)] text-[color:var(--emerald-ink)]",
    violet:
      "bg-[color-mix(in_oklab,var(--violet-ink)_14%,transparent)] text-[color:var(--violet-ink)]",
    amber:
      "bg-[color-mix(in_oklab,var(--amber-ink)_16%,transparent)] text-[color:var(--amber-ink)]",
    rose: "bg-[color-mix(in_oklab,var(--destructive)_12%,transparent)] text-[color:var(--destructive)]",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-[10.5px] font-medium tracking-tight",
        tones[tone],
      )}
    >
      {children}
    </span>
  );
}
