import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { SectionHeading, Chip, Card } from "@/components/site/primitives";
import {
  useProjects,
  fieldMeta,
  statusTone,
  formatDate,
  type FieldKey,
  type ProjectStatus,
} from "@/lib/projects-data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/projects")({
  head: () => ({
    meta: [
      { title: "Projects — SmartResearch Lab" },
      {
        name: "description",
        content:
          "Browse public research and production projects at SmartResearch Lab across AI, Robotics, and Software Engineering.",
      },
      { property: "og:title", content: "Projects — SmartResearch Lab" },
      {
        property: "og:description",
        content:
          "Live and completed projects from SmartResearch Lab, with fields, status, and timelines.",
      },
    ],
  }),
  component: ProjectsPage,
});

const FIELD_OPTIONS: { key: "all" | FieldKey; label: string }[] = [
  { key: "all", label: "All fields" },
  { key: "ai", label: "AI" },
  { key: "robotics", label: "Robotics" },
  { key: "se", label: "Software Eng." },
];

const STATUS_OPTIONS: ("all" | ProjectStatus)[] = [
  "all",
  "Active",
  "Publishing",
  "Planning",
  "On hold",
  "Completed",
];

function ProjectsPage() {
  const { projects } = useProjects();
  const [field, setField] = useState<"all" | FieldKey>("all");
  const [status, setStatus] = useState<"all" | ProjectStatus>("all");

  const publicProjects = useMemo(
    () => projects.filter((p) => p.visibility === "public"),
    [projects],
  );

  const filtered = useMemo(
    () =>
      publicProjects.filter((p) => {
        if (field !== "all" && !p.fields.includes(field)) return false;
        if (status !== "all" && p.status !== status) return false;
        return true;
      }),
    [publicProjects, field, status],
  );

  return (
    <div className="min-h-screen bg-background text-ink">
      <SiteHeader />

      <section className="border-b border-hairline">
        <div className="mx-auto max-w-7xl px-6 pb-10 pt-16 md:pt-20">
          <SectionHeading
            eyebrow="Projects"
            title="Research and production work, in the open"
            description="A live catalogue of the lab's public projects. Internal documents, task boards, and unpublished results live in the member portal."
          />
        </div>
      </section>

      <section className="border-b border-hairline bg-muted/30">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-6 py-5 md:flex-row md:items-center md:justify-between">
          <FilterGroup label="Field">
            {FIELD_OPTIONS.map((f) => (
              <FilterChip key={f.key} active={field === f.key} onClick={() => setField(f.key)}>
                {f.label}
              </FilterChip>
            ))}
          </FilterGroup>
          <FilterGroup label="Status">
            {STATUS_OPTIONS.map((s) => (
              <FilterChip key={s} active={status === s} onClick={() => setStatus(s)}>
                {s === "all" ? "All statuses" : s}
              </FilterChip>
            ))}
          </FilterGroup>
        </div>
      </section>

      <section>
        <div className="mx-auto max-w-7xl px-6 py-12 md:py-16">
          <div className="mb-6 flex items-center justify-between text-sm text-ink-soft">
            <span>
              Showing <span className="font-medium text-ink">{filtered.length}</span> of{" "}
              {publicProjects.length} public projects
            </span>
          </div>

          {filtered.length === 0 ? (
            <div className="rounded-xl border border-dashed border-hairline p-12 text-center">
              <div className="text-sm font-medium text-ink">No projects match these filters</div>
              <p className="mx-auto mt-1 max-w-md text-xs text-ink-soft">
                Try clearing a filter or exploring another research field.
              </p>
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {filtered.map((p) => (
                <Link key={p.id} to="/projects/$slug" params={{ slug: p.slug }} className="group">
                  <Card className="flex h-full flex-col justify-between gap-6 group-hover:border-ink/30">
                    <div>
                      <div className="flex items-center justify-between text-xs text-ink-soft">
                        <span className="font-mono">{p.code}</span>
                        <Chip
                          tone={
                            statusTone[p.status] === "neutral" ? undefined : statusTone[p.status]
                          }
                        >
                          {p.status}
                        </Chip>
                      </div>
                      <h3 className="mt-4 font-display text-2xl leading-tight text-ink">
                        {p.name}
                      </h3>
                      <p className="mt-3 line-clamp-3 text-sm leading-relaxed text-ink-soft">
                        {p.description}
                      </p>
                    </div>
                    <div className="space-y-4 border-t border-hairline pt-4">
                      <div className="flex flex-wrap gap-1.5">
                        {p.fields.map((k) => (
                          <Chip key={k} tone={fieldMeta[k].tone}>
                            {fieldMeta[k].name}
                          </Chip>
                        ))}
                        <span className="ml-auto inline-flex items-center rounded-full border border-hairline px-2 py-0.5 text-[10.5px] font-medium text-ink-soft">
                          {p.type}
                        </span>
                      </div>
                      <div>
                        <div className="mb-1.5 flex items-center justify-between text-[11px] text-ink-soft">
                          <span>Progress</span>
                          <span className="font-mono">{p.progress}%</span>
                        </div>
                        <div className="h-1 overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full bg-[color:var(--cyan)]"
                            style={{ width: `${p.progress}%` }}
                          />
                        </div>
                      </div>
                      <div className="text-[11px] text-ink-soft">
                        {formatDate(p.startDate)} → {formatDate(p.expectedEnd)}
                      </div>
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <SiteFooter />
    </div>
  );
}

function FilterGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</span>
      <div className="flex flex-wrap gap-1.5">{children}</div>
    </div>
  );
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "rounded-full border px-2.5 py-1 text-xs transition-colors",
        active
          ? "border-ink bg-ink text-background"
          : "border-hairline bg-surface-elev text-ink-soft hover:text-ink",
      )}
    >
      {children}
    </button>
  );
}
