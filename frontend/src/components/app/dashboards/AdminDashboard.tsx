import { Link } from "@tanstack/react-router";
import { ArrowRight, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { EmptyState, PageHeader, Panel, StatTile, StatusPill } from "../ui";
import { getAdminDashboard, type AdminDashboard as AdminDashboardData } from "@/lib/admin-api";
import { useAuth } from "@/lib/auth";

export function AdminDashboard() {
  const { accessToken } = useAuth();
  const [dashboard, setDashboard] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    if (!accessToken) {
      setDashboard(null);
      setError("Your Admin session is unavailable. Sign in again to continue.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      setDashboard(await getAdminDashboard(accessToken, 10));
    } catch (loadError) {
      setError(errorMessage(loadError, "The Admin dashboard could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  return (
    <>
      <PageHeader
        eyebrow="Admin overview"
        title="Lab operations at a glance"
        description="Live, lab-scoped totals and audited activity from the SmartLab backend."
        action={
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadDashboard()}
              disabled={loading}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-3 py-2 text-xs text-ink transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </button>
            <Link
              to="/app/admin/notifications"
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-2 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90"
            >
              Manage notifications <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        }
      />

      {error ? (
        <div
          role="alert"
          className="mb-5 flex flex-col gap-3 rounded-lg border border-[color:var(--destructive)]/35 bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)] px-4 py-3 text-sm text-[color:var(--destructive)] sm:flex-row sm:items-center sm:justify-between"
        >
          <span>{error}</span>
          <button
            type="button"
            onClick={() => void loadDashboard()}
            disabled={loading}
            className="self-start rounded-md border border-current px-2.5 py-1 text-xs font-medium disabled:opacity-45 sm:self-auto"
          >
            Try again
          </button>
        </div>
      ) : null}

      <div aria-busy={loading} className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <StatTile
          label="Users"
          value={metric(dashboard?.users, loading)}
          hint="Current lab"
          tone="cyan"
        />
        <StatTile
          label="Projects"
          value={metric(dashboard?.projects, loading)}
          hint="Non-deleted records"
          tone="emerald"
        />
        <StatTile
          label="Posts"
          value={metric(dashboard?.posts, loading)}
          hint="Non-deleted records"
          tone="violet"
        />
        <StatTile
          label="Join requests"
          value={metric(dashboard?.joinRequests, loading)}
          hint="All workflow statuses"
          tone="amber"
        />
        <StatTile
          label="Tasks"
          value={metric(dashboard?.tasks, loading)}
          hint="Non-deleted records"
          tone="cyan"
        />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Panel
          className="lg:col-span-2"
          title="Recent audited activity"
          description="Latest persisted actions across the current lab"
          action={
            dashboard ? (
              <span className="text-xs text-ink-soft">
                {dashboard.recentActivities.length} latest
              </span>
            ) : null
          }
        >
          {loading && !dashboard ? (
            <ActivityLoading />
          ) : dashboard?.recentActivities.length ? (
            <ul className="divide-y divide-hairline">
              {dashboard.recentActivities.map((activity) => (
                <li
                  key={activity.id}
                  className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-medium text-ink">
                        {activity.actorName || "System"}
                      </span>
                      <StatusPill tone={activityTone(activity.action)}>
                        {humanize(activity.action)}
                      </StatusPill>
                    </div>
                    <div className="mt-1 truncate text-xs text-ink-soft">
                      {humanize(activity.entityType)}
                      {activity.entityId ? ` · ${activity.entityId}` : ""}
                    </div>
                  </div>
                  <time dateTime={activity.createdAt} className="shrink-0 text-xs text-ink-soft">
                    {formatDateTime(activity.createdAt)}
                  </time>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              title="No audited activity yet"
              hint="Admin actions will appear here after they are recorded."
            />
          )}
        </Panel>

        <Panel title="Admin workflows" description="Open the current management workspaces">
          <div className="space-y-2">
            <QuickLink
              to="/app/join-requests"
              title="Project join requests"
              description="Filter, inspect, approve, or reject requests."
            />
            <QuickLink
              to="/app/admin/notifications"
              title="System notifications"
              description="Create, inspect, and hide lab notifications."
            />
            <QuickLink
              to="/app/admin/users"
              title="User administration"
              description="Open the existing account-management workspace."
            />
          </div>
        </Panel>
      </div>
    </>
  );
}

function QuickLink({
  to,
  title,
  description,
}: {
  to: "/app/join-requests" | "/app/admin/notifications" | "/app/admin/users";
  title: string;
  description: string;
}) {
  return (
    <Link
      to={to}
      className="group flex items-center justify-between gap-3 rounded-lg border border-hairline px-3 py-3 transition-colors hover:bg-muted/55"
    >
      <span>
        <span className="block text-sm font-medium text-ink">{title}</span>
        <span className="mt-0.5 block text-xs leading-relaxed text-ink-soft">{description}</span>
      </span>
      <ArrowRight className="h-4 w-4 shrink-0 text-ink-soft transition-transform group-hover:translate-x-0.5" />
    </Link>
  );
}

function ActivityLoading() {
  return (
    <div className="space-y-3" aria-label="Loading recent activity">
      {[0, 1, 2, 3].map((item) => (
        <div key={item} className="animate-pulse border-b border-hairline pb-3 last:border-0">
          <div className="h-3 w-2/5 rounded bg-muted" />
          <div className="mt-2 h-2.5 w-3/5 rounded bg-muted" />
        </div>
      ))}
    </div>
  );
}

function metric(value: number | undefined, loading: boolean) {
  if (value !== undefined) return value;
  return loading ? "…" : "—";
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
}

function activityTone(action: string): "emerald" | "rose" | "violet" | "cyan" {
  if (action.includes("APPROVE") || action.includes("CREATE")) return "emerald";
  if (action.includes("REJECT") || action.includes("DELETE")) return "rose";
  if (action.includes("ROLE")) return "violet";
  return "cyan";
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown time";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}
