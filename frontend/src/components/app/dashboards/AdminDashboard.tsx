import { Link } from "@tanstack/react-router";
import { PageHeader, StatTile, Panel, EmptyState, StatusPill } from "../ui";
import { pendingApprovals, recentActivity, systemStatus } from "@/lib/dashboard-data";
import { achievements, researchFields } from "@/lib/lab-data";

export function AdminDashboard() {
  return (
    <>
      <PageHeader
        eyebrow="Admin overview"
        title="Lab operations at a glance"
        description="Lab-wide health, pending approvals, and recent activity across every project."
        action={
          <Link
            to="/app/admin/users"
            className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-3 py-2 text-xs text-ink-soft transition-colors hover:text-ink"
          >
            Invite user
          </Link>
        }
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile label="Active projects" value="17" hint="+2 this semester" tone="cyan" />
        <StatTile
          label="Pending approvals"
          value={pendingApprovals.length}
          hint="Across all projects"
          tone="amber"
        />
        <StatTile
          label="Members & alumni"
          value={achievements[3].metric}
          hint="68 today"
          tone="violet"
        />
        <StatTile
          label="Grants secured"
          value={achievements[2].metric}
          hint="Since 2021"
          tone="emerald"
        />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Panel
          className="lg:col-span-2"
          title="Pending approvals"
          description="Posts awaiting review before publication"
          action={
            <button className="text-xs text-ink-soft transition-colors hover:text-ink">
              View all
            </button>
          }
        >
          {pendingApprovals.length === 0 ? (
            <EmptyState title="Nothing waiting" hint="Every submitted post has been reviewed." />
          ) : (
            <ul className="divide-y divide-hairline">
              {pendingApprovals.map((p) => (
                <li
                  key={p.id}
                  className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-ink">{p.title}</div>
                    <div className="mt-0.5 text-xs text-ink-soft">
                      {p.author} · {p.project} · {p.submitted}
                    </div>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <StatusPill tone="amber">{p.kind}</StatusPill>
                    <button className="rounded-md border border-hairline bg-surface-elev px-2.5 py-1 text-xs text-ink transition-colors hover:border-ink/20">
                      Review
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="System status" description="Workspace services">
          <ul className="flex flex-col gap-3">
            {systemStatus.map((s) => (
              <li key={s.label} className="flex items-center justify-between">
                <span className="text-sm text-ink">{s.label}</span>
                <StatusPill tone={s.tone}>{s.status}</StatusPill>
              </li>
            ))}
          </ul>
        </Panel>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Panel title="Recent activity" description="Latest changes across the workspace">
          <ul className="divide-y divide-hairline">
            {recentActivity.map((a, i) => (
              <li
                key={i}
                className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
              >
                <div className="text-sm text-ink">
                  <span className="font-medium">{a.who}</span>{" "}
                  <span className="text-ink-soft">{a.what}</span>
                </div>
                <div className="shrink-0 text-xs text-ink-soft">{a.when}</div>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Research fields" description="Distribution of active projects">
          <ul className="flex flex-col gap-3">
            {researchFields.map((f) => (
              <li key={f.key} className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-medium text-ink">{f.name}</div>
                  <div className="text-xs text-ink-soft">{f.projects} active projects</div>
                </div>
                <div className="h-1.5 w-32 overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full bg-[color:var(--cyan)]"
                    style={{ width: `${(f.projects / 6) * 100}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </>
  );
}
