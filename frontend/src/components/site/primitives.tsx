import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function SectionHeading({
  eyebrow,
  title,
  description,
  align = "left",
  action,
}: {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  align?: "left" | "center";
  action?: ReactNode;
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4",
        align === "center" && "items-center text-center",
        action && "md:flex-row md:items-end md:justify-between",
      )}
    >
      <div className={cn("max-w-2xl", align === "center" && "mx-auto")}>
        {eyebrow ? (
          <div className="mb-3 flex items-center gap-2 text-xs uppercase tracking-[0.18em] text-ink-soft">
            <span className="h-px w-6 bg-hairline" />
            {eyebrow}
          </div>
        ) : null}
        <h2 className="font-display text-3xl leading-[1.05] text-ink md:text-4xl lg:text-5xl">
          {title}
        </h2>
        {description ? (
          <p className="mt-3 text-base leading-relaxed text-ink-soft">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

export function Chip({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "neutral" | "cyan" | "emerald" | "violet" | "amber";
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
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-medium tracking-tight",
        tones[tone],
      )}
    >
      {children}
    </span>
  );
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "rounded-xl border border-hairline bg-surface-elev p-6 transition-colors hover:border-ink/20",
        className,
      )}
    >
      {children}
    </div>
  );
}
