import { createFileRoute } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, Eye, Filter, Plus, RefreshCw, ShieldAlert } from "lucide-react";
import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { AdminNotificationCreateDialog } from "@/components/app/notifications/AdminNotificationCreateDialog";
import { AdminNotificationDetailDialog } from "@/components/app/notifications/AdminNotificationDetailDialog";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  createAdminNotification,
  getAdminNotification,
  getAdminNotifications,
  hideAdminNotification,
  isUuid,
  type AdminNotificationDetail,
  type AdminNotificationFilters,
  type AdminNotificationSummary,
  type AdminPage,
  type CreateAdminNotificationInput,
} from "@/lib/admin-api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/app/admin/notifications")({
  head: () => ({
    meta: [
      { title: "Notification management — Smartworkspace" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminNotificationsRoute,
});

type FilterDraft = {
  notificationType: string;
  creatorId: string;
  relatedType: string;
  createdFrom: string;
  createdTo: string;
  size: number;
};

const EMPTY_FILTERS: FilterDraft = {
  notificationType: "",
  creatorId: "",
  relatedType: "",
  createdFrom: "",
  createdTo: "",
  size: 20,
};

function AdminNotificationsRoute() {
  const { user, activeRole, accessToken } = useAuth();
  if (!user || !activeRole) return null;

  if (activeRole !== "admin" || !user.roles.includes("admin")) {
    return (
      <AccessState
        title="Admin workspace required"
        description="Switch to the Admin role before managing system notifications."
      />
    );
  }
  if (!accessToken) {
    return (
      <AccessState
        title="Admin session unavailable"
        description="Sign in again before opening backend notification management."
      />
    );
  }

  return (
    <AdminNotificationsPage
      accessToken={accessToken}
      currentUser={{ id: user.id, fullName: user.fullName, email: user.email }}
    />
  );
}

function AdminNotificationsPage({
  accessToken,
  currentUser,
}: {
  accessToken: string;
  currentUser: { id: string; fullName: string; email: string };
}) {
  const listRequestSequence = useRef(0);
  const detailRequestSequence = useRef(0);
  const [draft, setDraft] = useState<FilterDraft>(EMPTY_FILTERS);
  const [filters, setFilters] = useState<AdminNotificationFilters>({ size: 20 });
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [result, setResult] = useState<AdminPage<AdminNotificationSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createPending, setCreatePending] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<AdminNotificationDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [hidePending, setHidePending] = useState(false);

  const loadPage = useCallback(async () => {
    const requestSequence = ++listRequestSequence.current;
    setLoading(true);
    setError(null);
    try {
      const next = await getAdminNotifications(accessToken, { ...filters, page });
      if (requestSequence !== listRequestSequence.current) return;
      if (next.totalPages > 0 && page >= next.totalPages) {
        setPage(next.totalPages - 1);
        return;
      }
      setResult(next);
    } catch (loadError) {
      if (requestSequence !== listRequestSequence.current) return;
      setError(errorMessage(loadError, "Notifications could not be loaded."));
    } finally {
      if (requestSequence === listRequestSequence.current) setLoading(false);
    }
  }, [accessToken, filters, page]);

  useEffect(() => {
    void loadPage();
  }, [loadPage, reloadKey]);

  const applyFilters = (event: FormEvent) => {
    event.preventDefault();
    const validation = validateFilters(draft);
    if (validation) {
      setFilterError(validation);
      return;
    }
    setFilterError(null);
    setSuccess(null);
    setPage(0);
    setFilters(toApiFilters(draft));
    setReloadKey((key) => key + 1);
  };

  const clearFilters = () => {
    setDraft(EMPTY_FILTERS);
    setFilters({ size: 20 });
    setPage(0);
    setFilterError(null);
    setSuccess(null);
    setReloadKey((key) => key + 1);
  };

  const openDetail = async (notificationId: string) => {
    const requestSequence = ++detailRequestSequence.current;
    setSelectedId(notificationId);
    setSelected(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      const detail = await getAdminNotification(accessToken, notificationId);
      if (requestSequence !== detailRequestSequence.current) return;
      setSelected(detail);
    } catch (loadError) {
      if (requestSequence !== detailRequestSequence.current) return;
      setDetailError(errorMessage(loadError, "The notification detail could not be loaded."));
    } finally {
      if (requestSequence === detailRequestSequence.current) setDetailLoading(false);
    }
  };

  const create = async (input: CreateAdminNotificationInput) => {
    setCreatePending(true);
    setCreateError(null);
    try {
      const created = await createAdminNotification(accessToken, input);
      setCreateOpen(false);
      detailRequestSequence.current += 1;
      setSelectedId(created.id);
      setSelected(created);
      setDetailError(null);
      setSuccess(
        `Notification created for ${created.recipientCount} recipient${
          created.recipientCount === 1 ? "" : "s"
        }.`,
      );
      await loadPage();
    } catch (createFailure) {
      setCreateError(errorMessage(createFailure, "The notification could not be created."));
    } finally {
      setCreatePending(false);
    }
  };

  const hide = async () => {
    if (!selected) return;
    setHidePending(true);
    setDetailError(null);
    try {
      await hideAdminNotification(accessToken, selected.id);
      detailRequestSequence.current += 1;
      setSelectedId(null);
      setSelected(null);
      setSuccess("Notification hidden for every recipient. Its audit history was preserved.");
      await loadPage();
    } catch (hideError) {
      setDetailError(errorMessage(hideError, "The notification could not be hidden."));
    } finally {
      setHidePending(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Admin communications"
        title="System notifications"
        description="Create, inspect, filter, and non-destructively hide persisted lab notifications."
        action={
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
              disabled={loading}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-3 py-2 text-xs text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} /> Refresh
            </button>
            <button
              type="button"
              onClick={() => {
                setCreateError(null);
                setCreateOpen(true);
              }}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-2 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Plus className="h-3.5 w-3.5" /> Create notification
            </button>
          </div>
        }
      />

      <div className="mb-4 rounded-lg border border-hairline bg-muted/25 px-4 py-3 text-xs leading-relaxed text-ink-soft">
        This is the Admin system-notification view. The personal bell remains browser-backed until
        recipient list/read APIs are available.
      </div>

      {error ? <PageMessage tone="error" message={error} /> : null}
      {success ? <PageMessage tone="success" message={success} /> : null}

      <Panel title="Filters" description="Search persisted notifications by backend metadata">
        <form onSubmit={applyFilters} className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <FilterField label="Notification type">
              <input
                value={draft.notificationType}
                onChange={(event) => setDraft({ ...draft, notificationType: event.target.value })}
                className="system-filter-input font-mono"
                placeholder="ADMIN_ANNOUNCEMENT"
              />
            </FilterField>
            <FilterField label="Creator UUID">
              <input
                value={draft.creatorId}
                onChange={(event) => setDraft({ ...draft, creatorId: event.target.value })}
                className="system-filter-input font-mono"
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
            </FilterField>
            <FilterField label="Related type">
              <input
                value={draft.relatedType}
                onChange={(event) => setDraft({ ...draft, relatedType: event.target.value })}
                className="system-filter-input font-mono"
                placeholder="PROJECT"
              />
            </FilterField>
            <FilterField label="Rows per page">
              <select
                value={draft.size}
                onChange={(event) => setDraft({ ...draft, size: Number(event.target.value) })}
                className="system-filter-input"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </FilterField>
            <FilterField label="Created from">
              <input
                type="datetime-local"
                value={draft.createdFrom}
                onChange={(event) => setDraft({ ...draft, createdFrom: event.target.value })}
                className="system-filter-input"
              />
            </FilterField>
            <FilterField label="Created to">
              <input
                type="datetime-local"
                value={draft.createdTo}
                onChange={(event) => setDraft({ ...draft, createdTo: event.target.value })}
                className="system-filter-input"
              />
            </FilterField>
          </div>

          {filterError ? <PageMessage tone="error" message={filterError} compact /> : null}

          <div className="flex flex-wrap justify-end gap-2">
            <button
              type="button"
              onClick={clearFilters}
              className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
            >
              Clear
            </button>
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Filter className="h-3.5 w-3.5" /> Apply filters
            </button>
          </div>
        </form>
      </Panel>

      <Panel
        title="Notification records"
        description={
          result
            ? `${result.totalElements} visible notification${result.totalElements === 1 ? "" : "s"}`
            : "Loading backend records"
        }
        className="mt-4 overflow-hidden"
      >
        {loading && !result ? (
          <TableLoading />
        ) : result?.content.length ? (
          <div className={`-mx-5 -mb-5 overflow-x-auto ${loading ? "opacity-60" : ""}`}>
            <table className="w-full min-w-[980px] text-sm" aria-busy={loading}>
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Notification</th>
                  <th className="px-3 py-3 font-medium">Creator</th>
                  <th className="px-3 py-3 font-medium">Delivery</th>
                  <th className="px-3 py-3 font-medium">Related</th>
                  <th className="px-3 py-3 font-medium">Created</th>
                  <th className="px-5 py-3 text-right font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((notification) => (
                  <tr
                    key={notification.id}
                    className="border-t border-hairline align-top transition-colors hover:bg-muted/35"
                  >
                    <td className="max-w-sm px-5 py-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <div className="font-medium text-ink">{notification.title}</div>
                        <StatusPill tone="violet">
                          {humanize(notification.notificationType)}
                        </StatusPill>
                      </div>
                      <div className="mt-1 line-clamp-2 text-xs leading-relaxed text-ink-soft">
                        {notification.message || "No message body"}
                      </div>
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {notification.createdBy?.fullName || "System"}
                    </td>
                    <td className="px-3 py-3">
                      <div className="text-xs font-medium text-ink">
                        {notification.readCount}/{notification.recipientCount} read
                      </div>
                      <div className="mt-1 h-1.5 w-24 overflow-hidden rounded-full bg-muted">
                        <div
                          className="h-full bg-[color:var(--cyan)]"
                          style={{ width: `${readPercentage(notification)}%` }}
                        />
                      </div>
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {notification.relatedType ? (
                        <>
                          <div>{humanize(notification.relatedType)}</div>
                          <div className="mt-0.5 max-w-40 truncate font-mono text-[10px]">
                            {notification.relatedId}
                          </div>
                        </>
                      ) : (
                        "—"
                      )}
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {formatDateTime(notification.createdAt)}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end">
                        <button
                          type="button"
                          onClick={() => void openDetail(notification.id)}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted"
                        >
                          <Eye className="h-3.5 w-3.5" /> Inspect
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No notifications match"
            hint="Clear one or more filters, or create the first lab notification."
          />
        )}

        {result && result.totalPages > 0 ? (
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            totalElements={result.totalElements}
            disabled={loading}
            onPageChange={setPage}
          />
        ) : null}
      </Panel>

      <AdminNotificationCreateDialog
        open={createOpen}
        pending={createPending}
        error={createError}
        currentUser={currentUser}
        onClose={() => {
          if (!createPending) {
            setCreateOpen(false);
            setCreateError(null);
          }
        }}
        onSubmit={create}
      />

      <AdminNotificationDetailDialog
        open={selectedId !== null}
        notification={selected}
        loading={detailLoading}
        hidePending={hidePending}
        error={detailError}
        onClose={() => {
          detailRequestSequence.current += 1;
          setSelectedId(null);
          setSelected(null);
          setDetailError(null);
        }}
        onHide={hide}
      />

      <style>{`
        .system-filter-input {
          width: 100%;
          min-height: 36px;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 12px;
          color: inherit;
        }
        .system-filter-input:focus { outline: 2px solid color-mix(in oklab, var(--cyan) 40%, transparent); }
      `}</style>
    </>
  );
}

function FilterField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-ink">{label}</span>
      {children}
    </label>
  );
}

function Pagination({
  page,
  totalPages,
  totalElements,
  disabled,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  disabled: boolean;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="mt-5 flex flex-col gap-3 border-t border-hairline pt-4 text-xs text-ink-soft sm:flex-row sm:items-center sm:justify-between">
      <span>
        Page {page + 1} of {totalPages} · {totalElements} total
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          disabled={disabled || page === 0}
          onClick={() => onPageChange(page - 1)}
          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronLeft className="h-3.5 w-3.5" /> Previous
        </button>
        <button
          type="button"
          disabled={disabled || page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next <ChevronRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

function TableLoading() {
  return (
    <div className="space-y-3" aria-label="Loading notifications">
      {[0, 1, 2, 3].map((item) => (
        <div key={item} className="animate-pulse rounded-lg border border-hairline p-4">
          <div className="h-3 w-1/3 rounded bg-muted" />
          <div className="mt-2 h-2.5 w-2/3 rounded bg-muted" />
        </div>
      ))}
    </div>
  );
}

function PageMessage({
  tone,
  message,
  compact,
}: {
  tone: "error" | "success";
  message: string;
  compact?: boolean;
}) {
  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={`${compact ? "" : "mb-4"} rounded-md border px-3 py-2 text-xs ${
        tone === "error"
          ? "border-[color:var(--destructive)]/35 bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)] text-[color:var(--destructive)]"
          : "border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_8%,transparent)] text-[color:var(--emerald-ink)]"
      }`}
    >
      {message}
    </div>
  );
}

function AccessState({ title, description }: { title: string; description: string }) {
  return (
    <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
      <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
        <ShieldAlert className="h-4 w-4 text-ink-soft" />
      </div>
      <h1 className="mt-4 text-sm font-semibold text-ink">{title}</h1>
      <p className="mt-1 text-xs leading-relaxed text-ink-soft">{description}</p>
    </div>
  );
}

function validateFilters(filters: FilterDraft) {
  const creatorId = filters.creatorId.trim();
  if (creatorId && !isUuid(creatorId)) return "Creator ID must be a valid UUID.";
  if (filters.createdFrom && filters.createdTo) {
    if (new Date(filters.createdFrom) > new Date(filters.createdTo)) {
      return "Created-from must not be after created-to.";
    }
  }
  return null;
}

function toApiFilters(filters: FilterDraft): AdminNotificationFilters {
  return {
    notificationType: filters.notificationType.trim() || undefined,
    creatorId: filters.creatorId.trim() || undefined,
    relatedType: filters.relatedType.trim() || undefined,
    createdFrom: toIsoDate(filters.createdFrom),
    createdTo: toIsoDate(filters.createdTo),
    size: filters.size,
  };
}

function toIsoDate(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

function readPercentage(notification: AdminNotificationSummary) {
  if (notification.recipientCount <= 0) return 0;
  return Math.min(100, Math.round((notification.readCount / notification.recipientCount) * 100));
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
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
