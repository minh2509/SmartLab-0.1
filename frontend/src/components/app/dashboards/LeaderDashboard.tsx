import { PageHeader, StatTile, Panel, EmptyState, StatusPill } from "../ui";
import { leaderProjects, leaderReviewQueue } from "@/lib/dashboard-data";
import { activities } from "@/lib/lab-data";

export function LeaderDashboard() {
  const totalMembers = leaderProjects.reduce((s, p) => s + p.members, 0);
  const totalOpen = leaderProjects.reduce((s, p) => s + p.openTasks, 0);

  return (
    <>
      <PageHeader
        eyebrow="Project leader"
        title="Your projects and reviews"
        description="Track team workload, review submissions, and keep milestones on schedule."
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile label="Projects you lead" value={leaderProjects.length} tone="cyan" />
        <StatTile label="Members" value={totalMembers} tone="violet" />
        <StatTile
          label="Awaiting your review"
          value={leaderReviewQueue.length}
          hint="Oldest waiting 1d"
          tone="amber"
        />
        <StatTile label="Open tasks" value={totalOpen} tone="emerald" />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Panel
          className="lg:col-span-2"
          title="Your projects"
          description="Health and workload across your teams"
        >
          <ul className="divide-y divide-hairline">
            {leaderProjects.map((p) => (
              <li
                key={p.code}
                className="flex items-center justify-between gap-4 py-4 first:pt-0 last:pb-0"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[11px] text-ink-soft">{p.code}</span>
                    <StatusPill tone={p.tone}>{p.health}</StatusPill>
                  </div>
                  <div className="mt-1 truncate text-sm font-medium text-ink">{p.name}</div>
                  <div className="mt-1 text-xs text-ink-soft">
                    {p.members} members · {p.openTasks} open tasks · {p.pendingReviews} reviews
                    pending
                  </div>
                </div>
                <button className="shrink-0 rounded-md border border-hairline bg-surface-elev px-3 py-1.5 text-xs text-ink transition-colors hover:border-ink/20">
                  Open
                </button>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Review queue" description="Submissions waiting on you">
          {leaderReviewQueue.length === 0 ? (
            <EmptyState title="Inbox zero" hint="No submissions waiting." />
          ) : (
            <ul className="flex flex-col gap-3">
              {leaderReviewQueue.map((r, i) => (
                <li key={i} className="rounded-md border border-hairline p-3">
                  <div className="text-sm font-medium text-ink">{r.title}</div>
                  <div className="mt-1 flex items-center justify-between text-xs text-ink-soft">
                    <span>{r.author}</span>
                    <span>waiting {r.waiting}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Panel title="Upcoming activities" description="Talks, showcases and field trips">
          <ul className="divide-y divide-hairline">
            {activities.slice(0, 3).map((a) => (
              <li
                key={a.title}
                className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
              >
                <div className="min-w-0">
                  <div className="truncate text-sm text-ink">{a.title}</div>
                  <div className="text-xs text-ink-soft">{a.date}</div>
                </div>
                <StatusPill tone="cyan">{a.kind}</StatusPill>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Weekly focus" description="Suggestions from the workspace">
          <ul className="flex flex-col gap-3 text-sm text-ink-soft">
            <li>· Approve the Atlas v2 benchmark draft before Friday's reading group.</li>
            <li>· Rebalance Helix reviews — 5 pending, oldest waiting a day.</li>
            <li>· Confirm two new PhD interview slots for the Fall 2026 cohort.</li>
          </ul>
        </Panel>
      </div>
    </>
  );
}
