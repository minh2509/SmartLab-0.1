import { createFileRoute } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, RefreshCw, RotateCcw, Search, ShieldCheck } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  compactIdentifier,
  fetchAdminLoginHistories,
  fetchAdminUserLoginHistories,
  formatAuditDateTime,
  toEndOfDayIso,
  toStartOfDayIso,
  type AdminLoginHistoryResponse,
  type AdminPageResponse,
} from "@/lib/admin-audit-api";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/admin/login-histories")({
  head: () => ({
    meta: [{ title: "Login Histories - Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: AdminLoginHistoriesPage,
});

type HistoryScope = "all" | "user";
type SuccessFilter = "all" | "true" | "false";

type LoginFilters = {
  scope: HistoryScope;
  userId: string;
  success: SuccessFilter;
  ipAddress: string;
  startDate: string;
  endDate: string;
};

const emptyFilters: LoginFilters = {
  scope: "all",
  userId: "",
  success: "all",
  ipAddress: "",
  startDate: "",
  endDate: "",
};

function AdminLoginHistoriesPage() {
  const { user, activeRole } = useAuth();
  const [draft, setDraft] = useState<LoginFilters>(emptyFilters);
  const [filters, setFilters] = useState<LoginFilters>(emptyFilters);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [refreshKey, setRefreshKey] = useState(0);
  const [data, setData] = useState<AdminPageResponse<AdminLoginHistoryResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAdminRoute = !!user && activeRole === "admin" && user.roles.includes("admin");

  useEffect(() => {
    if (!isAdminRoute) return;
    if (filters.scope === "user" && !filters.userId.trim()) {
      setData(null);
      setLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();
    const query = {
      success: filters.success === "all" ? undefined : filters.success === "true",
      ipAddress: filters.ipAddress,
      start: toStartOfDayIso(filters.startDate),
      end: toEndOfDayIso(filters.endDate),
      page,
      size,
    };

    setLoading(true);
    setError(null);
    const request =
      filters.scope === "user"
        ? fetchAdminUserLoginHistories(filters.userId.trim(), query, controller.signal)
        : fetchAdminLoginHistories({ ...query, userId: filters.userId }, controller.signal);

    request
      .then(setData)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setData(null);
        setError(reason instanceof Error ? reason.message : "Login histories could not be loaded.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [filters, isAdminRoute, page, refreshKey, size]);

  if (!user || !activeRole) return null;

  if (!isAdminRoute) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <ShieldCheck className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Admin workspace required</h1>
        <p className="mt-1 text-xs text-ink-soft">
          Login histories are available only when an Admin user is viewing as Admin.
        </p>
      </div>
    );
  }

  const rows = data?.content ?? [];
  const waitingForUserId = filters.scope === "user" && !filters.userId.trim();
  const hasFilters = Object.entries(filters).some(
    ([key, value]) => key !== "scope" && value !== "" && value !== "all",
  );

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setFilters({ ...draft });
  };

  const clearFilters = () => {
    setDraft(emptyFilters);
    setFilters(emptyFilters);
    setPage(0);
  };

  const successCount = rows.filter((row) => row.success === true).length;
  const failedCount = rows.filter((row) => row.success === false).length;

  return (
    <>
      <PageHeader
        eyebrow={user.isMainAdmin ? "Main Admin" : "Admin"}
        title="Login history"
        description="Read-only backend authentication history with global and user-specific views."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <a
              href="/app/admin/audit-logs"
              className="rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
            >
              Audit logs
            </a>
            <button
              onClick={() => setRefreshKey((value) => value + 1)}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} /> Refresh
            </button>
          </div>
        }
      />

      {error ? (
        <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}

      <AuthNotice />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat label="Total records" value={data?.totalElements ?? 0} />
        <MiniStat label="Successful on page" value={successCount} tone="emerald" />
        <MiniStat label="Failed on page" value={failedCount} tone="rose" />
      </div>

      <Panel
        title="History filters"
        description={
          draft.scope === "user"
            ? "User-specific mode calls the backend user login-history endpoint."
            : "Global mode calls the backend login-history endpoint."
        }
      >
        <form onSubmit={applyFilters} className="grid gap-3 lg:grid-cols-6">
          <div className="lg:col-span-6">
            <div className="inline-flex rounded-md border border-hairline bg-background p-0.5">
              {[
                { key: "all", label: "Global" },
                { key: "user", label: "User-specific" },
              ].map((item) => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() =>
                    setDraft((current) => ({ ...current, scope: item.key as HistoryScope }))
                  }
                  className={cn(
                    "rounded px-2.5 py-1 text-xs transition-colors",
                    draft.scope === item.key
                      ? "bg-muted font-medium text-ink"
                      : "text-ink-soft hover:text-ink",
                  )}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>
          <TextFilter
            label="User ID"
            value={draft.userId}
            placeholder={draft.scope === "user" ? "Required UUID" : "Optional UUID"}
            onChange={(userId) => setDraft((current) => ({ ...current, userId }))}
          />
          <label className="block text-xs">
            <span className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">Result</span>
            <select
              value={draft.success}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  success: event.target.value as SuccessFilter,
                }))
              }
              className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-xs text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            >
              <option value="all">All results</option>
              <option value="true">Successful</option>
              <option value="false">Failed</option>
            </select>
          </label>
          <TextFilter
            label="IP address"
            value={draft.ipAddress}
            placeholder="127.0.0.1"
            onChange={(ipAddress) => setDraft((current) => ({ ...current, ipAddress }))}
          />
          <DateFilter
            label="Start"
            value={draft.startDate}
            onChange={(startDate) => setDraft((current) => ({ ...current, startDate }))}
          />
          <DateFilter
            label="End"
            value={draft.endDate}
            onChange={(endDate) => setDraft((current) => ({ ...current, endDate }))}
          />
          <div className="flex flex-wrap items-end gap-2">
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Search className="h-3.5 w-3.5" /> Apply
            </button>
            <button
              type="button"
              onClick={clearFilters}
              disabled={!hasFilters && draft.scope === "all"}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
            >
              <RotateCcw className="h-3.5 w-3.5" /> Clear
            </button>
          </div>
        </form>
      </Panel>

      <div className="mt-4">
        <Panel
          title="Login records"
          description={
            loading
              ? "Loading from backend"
              : `${rows.length} shown of ${data?.totalElements ?? 0} total`
          }
          className="overflow-hidden"
        >
          {waitingForUserId ? (
            <EmptyState
              title="User ID required"
              hint="Enter a backend user UUID to load the user-specific login-history endpoint."
            />
          ) : loading && !data ? (
            <div className="py-10 text-center text-sm text-ink-soft">
              Loading login histories...
            </div>
          ) : rows.length === 0 ? (
            <EmptyState
              title={error ? "Login histories unavailable" : "No login records"}
              hint={
                error
                  ? "Check backend availability, Admin Basic Auth, and VITE_API_BASE_URL."
                  : "Try a different user, result, IP address, or date range."
              }
            />
          ) : (
            <>
              <div className="-mx-5 -mt-5 overflow-x-auto">
                <table className="w-full min-w-[980px] text-sm">
                  <thead>
                    <tr className="border-b border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                      <th className="px-5 py-3 font-medium">Login time</th>
                      <th className="px-3 py-3 font-medium">Result</th>
                      <th className="px-3 py-3 font-medium">User</th>
                      <th className="px-3 py-3 font-medium">Network</th>
                      <th className="px-3 py-3 font-medium">Failure</th>
                      <th className="px-5 py-3 font-medium">Record</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((history) => (
                      <tr key={history.id} className="border-b border-hairline align-top">
                        <td className="px-5 py-3 text-xs text-ink-soft">
                          {formatAuditDateTime(history.loginAt)}
                        </td>
                        <td className="px-3 py-3">
                          <StatusPill tone={loginResultTone(history.success)}>
                            {loginResultLabel(history.success)}
                          </StatusPill>
                        </td>
                        <td className="px-3 py-3">
                          <div className="font-medium text-ink">
                            {history.userFullName ?? "Unknown user"}
                          </div>
                          <div className="mt-0.5 text-xs text-ink-soft">
                            {history.userEmail ?? compactIdentifier(history.userId)}
                          </div>
                        </td>
                        <td className="px-3 py-3 text-xs text-ink-soft">
                          <div>{history.ipAddress ?? "IP not recorded"}</div>
                          <div
                            className="mt-1 max-w-[260px] truncate"
                            title={history.userAgent ?? ""}
                          >
                            {history.userAgent ?? "Agent not recorded"}
                          </div>
                        </td>
                        <td className="px-3 py-3 text-xs text-ink-soft">
                          {history.failureReason ?? "None"}
                        </td>
                        <td className="px-5 py-3 text-xs text-ink-soft">
                          {compactIdentifier(history.id)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <PageControls
                page={data?.page ?? page}
                size={size}
                totalPages={data?.totalPages ?? 0}
                loading={loading}
                onPageChange={setPage}
                onSizeChange={(nextSize) => {
                  setSize(nextSize);
                  setPage(0);
                }}
              />
            </>
          )}
        </Panel>
      </div>
    </>
  );
}

function AuthNotice() {
  return (
    <div className="mb-4 rounded-md border border-hairline bg-muted/35 px-3 py-2 text-xs text-ink-soft">
      This frontend uses the current local demo Admin session and shared demo password for backend
      HTTP Basic Auth. Backend records may not exist until database-backed login tracking is wired.
    </div>
  );
}

function TextFilter({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string;
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="block text-xs">
      <span className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-xs text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
      />
    </label>
  );
}

function DateFilter({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="block text-xs">
      <span className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</span>
      <input
        type="date"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-xs text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
      />
    </label>
  );
}

function PageControls({
  page,
  size,
  totalPages,
  loading,
  onPageChange,
  onSizeChange,
}: {
  page: number;
  size: number;
  totalPages: number;
  loading: boolean;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}) {
  const hasPages = totalPages > 0;
  return (
    <div className="-mx-5 -mb-5 flex flex-col gap-3 border-t border-hairline px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="text-xs text-ink-soft">
        {hasPages ? `Page ${page + 1} of ${totalPages}` : "Page 0 of 0"}
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <select
          value={size}
          onChange={(event) => onSizeChange(Number(event.target.value))}
          className="rounded-md border border-hairline bg-background px-2 py-1.5 text-xs text-ink"
        >
          {[10, 20, 50, 100].map((option) => (
            <option key={option} value={option}>
              {option} per page
            </option>
          ))}
        </select>
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={loading || page <= 0}
          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronLeft className="h-3.5 w-3.5" /> Previous
        </button>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={loading || !hasPages || page >= totalPages - 1}
          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next <ChevronRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

function MiniStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "emerald" | "rose";
}) {
  return (
    <div className="rounded-xl border border-hairline bg-surface-elev p-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div
        className={cn(
          "mt-2 font-display text-3xl text-ink",
          tone === "emerald" && "text-[color:var(--emerald-ink)]",
          tone === "rose" && "text-[color:var(--destructive)]",
        )}
      >
        {value}
      </div>
    </div>
  );
}

function loginResultTone(success: boolean | null) {
  if (success === true) return "emerald";
  if (success === false) return "rose";
  return "neutral";
}

function loginResultLabel(success: boolean | null) {
  if (success === true) return "Successful";
  if (success === false) return "Failed";
  return "Unknown";
}
