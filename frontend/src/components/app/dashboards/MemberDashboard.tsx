import { PageHeader, StatTile, Panel, EmptyState, StatusPill } from "../ui";
import {
  memberTasks,
  memberPosts,
  memberNotifications,
  type PostStatus,
} from "@/lib/dashboard-data";

const postTone: Record<PostStatus, "neutral" | "cyan" | "emerald" | "violet" | "amber" | "rose"> = {
  Draft: "neutral",
  "Pending Review": "amber",
  "Needs Revision": "rose",
  Approved: "emerald",
  Published: "cyan",
  Rejected: "rose",
};

export function MemberDashboard() {
  const unread = memberNotifications.filter((n) => n.unread).length;
  const pending = memberPosts.filter((p) => p.status === "Pending Review").length;
  const revisions = memberPosts.filter((p) => p.status === "Needs Revision").length;

  return (
    <>
      <PageHeader
        eyebrow="Member workspace"
        title="Your week in the lab"
        description="Your tasks, submissions, and the messages that need your attention."
        action={
          <button className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-2 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90">
            New post draft
          </button>
        }
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile label="Active project" value="Atlas" hint="NL-24-07" tone="cyan" />
        <StatTile label="Open tasks" value={memberTasks.length} tone="violet" />
        <StatTile label="Awaiting review" value={pending} hint="Sent to leader" tone="amber" />
        <StatTile label="Needs revision" value={revisions} tone="emerald" />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Panel
          className="lg:col-span-2"
          title="Your posts"
          description="Track submissions through the review pipeline"
        >
          {memberPosts.length === 0 ? (
            <EmptyState title="No posts yet" hint="Start a draft to share results with the lab." />
          ) : (
            <ul className="divide-y divide-hairline">
              {memberPosts.map((p) => (
                <li
                  key={p.title}
                  className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-ink">{p.title}</div>
                    <div className="mt-0.5 text-xs text-ink-soft">Updated {p.updated}</div>
                  </div>
                  <StatusPill tone={postTone[p.status]}>{p.status}</StatusPill>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Notifications" description={unread ? `${unread} unread` : "All caught up"}>
          {memberNotifications.length === 0 ? (
            <EmptyState title="No notifications" />
          ) : (
            <ul className="flex flex-col gap-3">
              {memberNotifications.map((n, i) => (
                <li key={i} className="rounded-md border border-hairline p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="text-sm font-medium text-ink">{n.title}</div>
                    {n.unread ? (
                      <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-[color:var(--cyan)]" />
                    ) : null}
                  </div>
                  <div className="mt-1 text-xs text-ink-soft">{n.detail}</div>
                  <div className="mt-1 text-[11px] text-ink-soft">{n.when}</div>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <div className="mt-6">
        <Panel title="This week's tasks" description="Owned by you across Atlas">
          <ul className="divide-y divide-hairline">
            {memberTasks.map((t) => (
              <li
                key={t.title}
                className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
              >
                <div className="min-w-0">
                  <div className="truncate text-sm text-ink">{t.title}</div>
                  <div className="text-xs text-ink-soft">Due {t.due}</div>
                </div>
                <StatusPill tone={t.state === "In progress" ? "cyan" : "neutral"}>
                  {t.state}
                </StatusPill>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </>
  );
}
