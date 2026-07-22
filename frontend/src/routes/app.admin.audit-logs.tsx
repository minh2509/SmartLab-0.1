import { createFileRoute } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, RefreshCw, RotateCcw, Search, ShieldCheck } from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  compactIdentifier,
  fetchAdminAuditLogs,
  formatAuditDateTime,
  labelToken,
  toEndOfDayIso,
  toStartOfDayIso,
  type AdminAuditLogResponse,
  type AdminPageResponse,
} from "@/lib/admin-audit-api";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/admin/audit-logs")({
  head: () => ({
    meta: [{ title: "Audit Logs - Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: AdminAuditLogsPage,
});

type AuditFilters = {
  action: string;
  actorId: string;
  entityType: string;
  entityId: string;
  startDate: string;
  endDate: string;
};

const emptyFilters: AuditFilters = {
  action: "",
  actorId: "",
  entityType: "",
  entityId: "",
  startDate: "",
  endDate: "",
};

function AdminAuditLogsPage() {
  const { user, activeRole } = useAuth();
  const [draft, setDraft] = useState<AuditFilters>(emptyFilters);
  const [filters, setFilters] = useState<AuditFilters>(emptyFilters);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [refreshKey, setRefreshKey] = useState(0);
  const [data, setData] = useState<AdminPageResponse<AdminAuditLogResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const refreshToastId = useRef<ReturnType<typeof toast.loading> | null>(null);

  const isAdminRoute = !!user && activeRole === "admin" && user.roles.includes("admin");

  useEffect(() => {
    if (!isAdminRoute) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchAdminAuditLogs(
      {
        action: filters.action,
        actorId: filters.actorId,
        entityType: filters.entityType,
        entityId: filters.entityId,
        start: toStartOfDayIso(filters.startDate),
        end: toEndOfDayIso(filters.endDate),
        page,
        size,
      },
      controller.signal,
    )
      .then((nextData) => {
        setData(nextData);
        if (refreshToastId.current) {
          toast.success("Audit logs refreshed", {
            id: refreshToastId.current,
            description: `${nextData.content.length} records shown.`,
          });
          refreshToastId.current = null;
        }
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        const message =
          reason instanceof Error ? reason.message : "Audit logs could not be loaded.";
        setData(null);
        setError(message);
        toast.error("Audit logs could not be loaded", {
          id: refreshToastId.current ?? undefined,
          description: message,
        });
        refreshToastId.current = null;
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
          Audit logs are available only when an Admin user is viewing as Admin.
        </p>
      </div>
    );
  }

  const rows = data?.content ?? [];
  const hasFilters = Object.values(filters).some(Boolean);
  const dateRangeError =
    draft.startDate && draft.endDate && draft.startDate > draft.endDate
      ? "End date must be on or after start date."
      : undefined;

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (dateRangeError) {
      toast.error("Date range invalid", {
        description: dateRangeError,
      });
      return;
    }
    setPage(0);
    setFilters({ ...draft });
    toast.info("Audit filters applied");
  };

  const clearFilters = () => {
    setDraft(emptyFilters);
    setFilters(emptyFilters);
    setPage(0);
    toast.info("Audit filters cleared");
  };

  const refreshRecords = () => {
    if (loading) return;
    refreshToastId.current = toast.loading("Refreshing audit logs");
    setRefreshKey((value) => value + 1);
  };

  return (
    <>
      <PageHeader
        eyebrow={user.isMainAdmin ? "Main Admin" : "Admin"}
        title="Audit logs"
        description="Read-only backend audit activity with server-side filtering and pagination."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <a
              href="/app/admin/login-histories"
              className="rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
            >
              Login history
            </a>
            <button
              onClick={refreshRecords}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} /> Refresh
            </button>
          </div>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat label="Total records" value={data?.totalElements ?? 0} />
        <MiniStat label="Page" value={data ? data.page + 1 : 0} tone="cyan" />
        <MiniStat label="Page size" value={data?.size ?? size} tone="violet" />
      </div>

      <Panel title="Audit filters" description="Blank fields are omitted from the backend query.">
        <form onSubmit={applyFilters} className="grid gap-3 lg:grid-cols-6">
          <TextFilter
            label="Action"
            value={draft.action}
            placeholder="CREATE_USER"
            onChange={(action) => setDraft((current) => ({ ...current, action }))}
          />
          <TextFilter
            label="Actor ID"
            value={draft.actorId}
            placeholder="UUID"
            onChange={(actorId) => setDraft((current) => ({ ...current, actorId }))}
          />
          <TextFilter
            label="Entity type"
            value={draft.entityType}
            placeholder="USER"
            onChange={(entityType) => setDraft((current) => ({ ...current, entityType }))}
          />
          <TextFilter
            label="Entity ID"
            value={draft.entityId}
            placeholder="UUID"
            onChange={(entityId) => setDraft((current) => ({ ...current, entityId }))}
          />
          <DateFilter
            label="Start"
            value={draft.startDate}
            onChange={(startDate) => setDraft((current) => ({ ...current, startDate }))}
          />
          <DateFilter
            label="End"
            value={draft.endDate}
            error={dateRangeError}
            onChange={(endDate) => setDraft((current) => ({ ...current, endDate }))}
          />
          <div className="flex flex-wrap items-end gap-2 lg:col-span-6">
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Search className="h-3.5 w-3.5" /> Apply filters
            </button>
            <button
              type="button"
              onClick={clearFilters}
              disabled={!hasFilters && !Object.values(draft).some(Boolean)}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
            >
              <RotateCcw className="h-3.5 w-3.5" /> Clear
            </button>
          </div>
        </form>
      </Panel>

      <div className="mt-4">
        <Panel
          title="Audit records"
          description={
            loading
              ? "Loading from backend"
              : `${rows.length} shown of ${data?.totalElements ?? 0} total`
          }
          className="overflow-hidden"
        >
          {loading && !data ? (
            <div className="py-10 text-center text-sm text-ink-soft">Loading audit logs...</div>
          ) : rows.length === 0 ? (
            <EmptyState
              title={error ? "Audit logs unavailable" : "No audit records"}
              hint={
                error
                  ? "Check backend availability, Admin Basic Auth, and VITE_API_BASE_URL."
                  : "Try clearing filters or wait for backend audit activity."
              }
            />
          ) : (
            <>
              <div className="-mx-5 -mt-5 overflow-x-auto">
                <table className="w-full min-w-[980px] text-sm">
                  <thead>
                    <tr className="border-b border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                      <th className="px-5 py-3 font-medium">Time</th>
                      <th className="px-3 py-3 font-medium">Action</th>
                      <th className="px-3 py-3 font-medium">Actor</th>
                      <th className="px-3 py-3 font-medium">Entity</th>
                      <th className="px-3 py-3 font-medium">Network</th>
                      <th className="px-5 py-3 font-medium">Record</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((log) => (
                      <tr key={log.id} className="border-b border-hairline align-top">
                        <td className="px-5 py-3 text-xs text-ink-soft">
                          {formatAuditDateTime(log.createdAt)}
                        </td>
                        <td className="px-3 py-3">
                          <StatusPill tone={auditActionTone(log.action)}>
                            {labelToken(log.action)}
                          </StatusPill>
                        </td>
                        <td className="px-3 py-3">
                          <div className="font-medium text-ink">
                            {log.actorFullName ?? "System actor"}
                          </div>
                          <div className="mt-0.5 text-xs text-ink-soft">
                            {log.actorEmail ?? compactIdentifier(log.actorId)}
                          </div>
                        </td>
                        <td className="px-3 py-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <StatusPill tone="violet">{labelToken(log.entityType)}</StatusPill>
                            <span className="text-xs text-ink-soft">
                              {compactIdentifier(log.entityId)}
                            </span>
                          </div>
                        </td>
                        <td className="px-3 py-3 text-xs text-ink-soft">
                          <div>{log.ipAddress ?? "IP not recorded"}</div>
                          <div className="mt-1 max-w-[260px] truncate" title={log.userAgent ?? ""}>
                            {log.userAgent ?? "Agent not recorded"}
                          </div>
                        </td>
                        <td className="px-5 py-3 text-xs text-ink-soft">
                          {compactIdentifier(log.id)}
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
  error,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
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
      {error ? (
        <span className="mt-1 block text-xs text-[color:var(--destructive)]">{error}</span>
      ) : null}
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
  tone?: "cyan" | "violet";
}) {
  return (
    <div className="rounded-xl border border-hairline bg-surface-elev p-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div
        className={cn(
          "mt-2 font-display text-3xl text-ink",
          tone === "cyan" && "text-[color:var(--cyan)]",
          tone === "violet" && "text-[color:var(--violet-ink)]",
        )}
      >
        {value}
      </div>
    </div>
  );
}

function auditActionTone(action: string) {
  const normalized = action.toUpperCase();
  if (normalized.includes("DELETE") || normalized.includes("LOCK")) return "rose";
  if (normalized.includes("CREATE") || normalized.includes("UNLOCK")) return "emerald";
  if (normalized.includes("UPDATE") || normalized.includes("ROLE")) return "cyan";
  return "neutral";
}
