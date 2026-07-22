import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Eye,
  Filter,
  RefreshCw,
  ShieldAlert,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { AdminJoinRequestDetailDialog } from "@/components/app/join-requests/AdminJoinRequestDetailDialog";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  approveAdminJoinRequest,
  getAdminJoinRequest,
  getAdminJoinRequests,
  isUuid,
  rejectAdminJoinRequest,
  type AdminJoinRequest,
  type AdminJoinRequestFilters,
  type AdminJoinRequestStatus,
  type AdminPage,
} from "@/lib/admin-api";
import { useAuth } from "@/lib/auth";
import { ApiError } from "@/lib/api-client";
import {
  joinRequestTone,
  useJoinRequests,
  type ProjectJoinRequest,
} from "@/lib/join-requests-data";
import { formatDate, useProjects } from "@/lib/projects-data";

export const Route = createFileRoute("/app/join-requests")({
  head: () => ({
    meta: [{ title: "Join requests — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: JoinRequestsPage,
});

function JoinRequestsPage() {
  const { user, activeRole, accessToken } = useAuth();
  if (!user || !activeRole) return null;

  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  if (isAdmin) {
    return accessToken ? (
      <AdminJoinRequestsPage accessToken={accessToken} />
    ) : (
      <AccessState
        title="Admin session unavailable"
        description="Sign in again before opening backend join requests."
      />
    );
  }

  if (activeRole === "member" && user.roles.includes("member")) {
    return <MemberJoinRequestsPage userId={user.id} />;
  }

  return (
    <AccessState
      title="Member or Admin workspace required"
      description="Switch to the Member or Admin role to open this page. Assigned Leader reviews remain inside each project workspace."
    />
  );
}

type FilterDraft = {
  projectId: string;
  requesterId: string;
  status: "ALL" | AdminJoinRequestStatus;
  createdFrom: string;
  createdTo: string;
  size: number;
};

const EMPTY_FILTERS: FilterDraft = {
  projectId: "",
  requesterId: "",
  status: "ALL",
  createdFrom: "",
  createdTo: "",
  size: 20,
};

function AdminJoinRequestsPage({ accessToken }: { accessToken: string }) {
  const listRequestSequence = useRef(0);
  const detailRequestSequence = useRef(0);
  const [draft, setDraft] = useState<FilterDraft>(EMPTY_FILTERS);
  const [filters, setFilters] = useState<AdminJoinRequestFilters>({ size: 20 });
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [result, setResult] = useState<AdminPage<AdminJoinRequest> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<AdminJoinRequest | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [mutationPending, setMutationPending] = useState(false);
  const [overrideDisabled, setOverrideDisabled] = useState(false);

  const loadPage = useCallback(async () => {
    const requestSequence = ++listRequestSequence.current;
    setLoading(true);
    setError(null);
    try {
      const next = await getAdminJoinRequests(accessToken, { ...filters, page });
      if (requestSequence !== listRequestSequence.current) return;
      if (next.totalPages > 0 && page >= next.totalPages) {
        setPage(next.totalPages - 1);
        return;
      }
      setResult(next);
    } catch (loadError) {
      if (requestSequence !== listRequestSequence.current) return;
      setError(errorMessage(loadError, "Join requests could not be loaded."));
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

  const openDetail = async (requestId: string) => {
    const requestSequence = ++detailRequestSequence.current;
    setSelectedId(requestId);
    setSelected(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      const detail = await getAdminJoinRequest(accessToken, requestId);
      if (requestSequence !== detailRequestSequence.current) return;
      setSelected(detail);
    } catch (loadError) {
      if (requestSequence !== detailRequestSequence.current) return;
      setDetailError(errorMessage(loadError, "The join request detail could not be loaded."));
    } finally {
      if (requestSequence === detailRequestSequence.current) setDetailLoading(false);
    }
  };

  const approve = async () => {
    if (!selected) return;
    setMutationPending(true);
    setDetailError(null);
    try {
      const updated = await approveAdminJoinRequest(accessToken, selected.id);
      setSelected(updated);
      setSuccess(`${updated.requester.fullName}'s join request was approved.`);
      await loadPage();
    } catch (approveError) {
      if (isOverrideDisabledError(approveError)) setOverrideDisabled(true);
      if (approveError instanceof ApiError && approveError.status === 409) {
        await refreshAfterConflict(selected.id);
      }
      setDetailError(errorMessage(approveError, "The join request could not be approved."));
    } finally {
      setMutationPending(false);
    }
  };

  const reject = async (reason: string) => {
    if (!selected) return;
    setMutationPending(true);
    setDetailError(null);
    try {
      const updated = await rejectAdminJoinRequest(accessToken, selected.id, reason);
      setSelected(updated);
      setSuccess(`${updated.requester.fullName}'s join request was rejected.`);
      await loadPage();
    } catch (rejectError) {
      if (isOverrideDisabledError(rejectError)) setOverrideDisabled(true);
      if (rejectError instanceof ApiError && rejectError.status === 409) {
        await refreshAfterConflict(selected.id);
      }
      setDetailError(errorMessage(rejectError, "The join request could not be rejected."));
    } finally {
      setMutationPending(false);
    }
  };

  const refreshAfterConflict = async (requestId: string) => {
    try {
      setSelected(await getAdminJoinRequest(accessToken, requestId));
    } catch {
      // Keep the original conflict visible when the detail can no longer be reloaded.
    }
    await loadPage();
  };

  return (
    <>
      <PageHeader
        eyebrow="System oversight"
        title="Project join requests"
        description="Live, lab-scoped requests from PostgreSQL with safe applicant and CV metadata."
        action={
          <button
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-3 py-2 text-xs text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} /> Refresh
          </button>
        }
      />

      <div className="mb-4 rounded-lg border border-hairline bg-muted/25 px-4 py-3 text-xs leading-relaxed text-ink-soft">
        This Admin view shows requests persisted by the backend. Member demo submissions remain in
        browser storage until a Member join-request API is available.
      </div>

      {error ? <PageMessage tone="error" message={error} /> : null}
      {success ? <PageMessage tone="success" message={success} /> : null}

      <Panel title="Filters" description="Narrow results before reviewing an application">
        <form onSubmit={applyFilters} className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <FilterField label="Project UUID" hint="Optional">
              <input
                value={draft.projectId}
                onChange={(event) => setDraft({ ...draft, projectId: event.target.value })}
                className="admin-filter-input"
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
            </FilterField>
            <FilterField label="Requester UUID" hint="Optional">
              <input
                value={draft.requesterId}
                onChange={(event) => setDraft({ ...draft, requesterId: event.target.value })}
                className="admin-filter-input"
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
            </FilterField>
            <FilterField label="Status">
              <select
                value={draft.status}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    status: event.target.value as FilterDraft["status"],
                  })
                }
                className="admin-filter-input"
              >
                <option value="ALL">All statuses</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </FilterField>
            <FilterField label="Rows per page">
              <select
                value={draft.size}
                onChange={(event) => setDraft({ ...draft, size: Number(event.target.value) })}
                className="admin-filter-input"
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
                className="admin-filter-input"
              />
            </FilterField>
            <FilterField label="Created to">
              <input
                type="datetime-local"
                value={draft.createdTo}
                onChange={(event) => setDraft({ ...draft, createdTo: event.target.value })}
                className="admin-filter-input"
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
        title="Requests"
        description={
          result
            ? `${result.totalElements} request${result.totalElements === 1 ? "" : "s"} in this lab`
            : "Loading backend records"
        }
        className="mt-4 overflow-hidden"
      >
        {loading && !result ? (
          <TableLoading />
        ) : result?.content.length ? (
          <div className={`-mx-5 -mb-5 overflow-x-auto ${loading ? "opacity-60" : ""}`}>
            <table className="w-full min-w-[960px] text-sm" aria-busy={loading}>
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Project</th>
                  <th className="px-3 py-3 font-medium">Applicant</th>
                  <th className="px-3 py-3 font-medium">Position</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Submitted</th>
                  <th className="px-5 py-3 text-right font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((request) => (
                  <tr
                    key={request.id}
                    className="border-t border-hairline align-top transition-colors hover:bg-muted/35"
                  >
                    <td className="px-5 py-3">
                      <div className="font-medium text-ink">{request.project.name}</div>
                      <div className="mt-0.5 font-mono text-[11px] text-ink-soft">
                        {request.project.code}
                      </div>
                    </td>
                    <td className="px-3 py-3">
                      <div className="text-sm text-ink">{request.requester.fullName}</div>
                      <div className="mt-0.5 text-xs text-ink-soft">{request.requester.email}</div>
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {request.desiredPosition || "Not provided"}
                    </td>
                    <td className="px-3 py-3">
                      <StatusPill tone={adminStatusTone(request.status)}>
                        {humanize(request.status)}
                      </StatusPill>
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {formatBackendDateTime(request.createdAt)}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end">
                        <button
                          type="button"
                          onClick={() => void openDetail(request.id)}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted"
                        >
                          <Eye className="h-3.5 w-3.5" /> Review
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
            title="No join requests match"
            hint="Clear one or more filters, or wait for a backend-submitted request."
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

      <AdminJoinRequestDetailDialog
        open={selectedId !== null}
        request={selected}
        loading={detailLoading}
        mutationPending={mutationPending}
        overrideDisabled={overrideDisabled}
        error={detailError}
        onClose={() => {
          detailRequestSequence.current += 1;
          setSelectedId(null);
          setSelected(null);
          setDetailError(null);
        }}
        onApprove={approve}
        onReject={reject}
      />

      <style>{`
        .admin-filter-input {
          width: 100%;
          min-height: 36px;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 12px;
          color: inherit;
        }
        .admin-filter-input:focus { outline: 2px solid color-mix(in oklab, var(--cyan) 40%, transparent); }
      `}</style>
    </>
  );
}

function MemberJoinRequestsPage({ userId }: { userId: string }) {
  const { projects } = useProjects();
  const { requests, cancel } = useJoinRequests();
  const [error, setError] = useState<string | null>(null);
  const visible = requests.filter((request) => request.userId === userId);

  return (
    <>
      <PageHeader
        eyebrow="Member workspace"
        title="Join requests"
        description="Track your project participation requests and cancel pending submissions stored in this browser."
      />

      <Panel
        title="Your request history"
        description={`${visible.length} request${visible.length === 1 ? "" : "s"}`}
        className="overflow-hidden"
      >
        {error ? <PageMessage tone="error" message={error} /> : null}
        {visible.length === 0 ? (
          <EmptyState
            title="No join requests yet"
            hint="Open a public project page to request participation."
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Project</th>
                  <th className="px-3 py-3 font-medium">Desired role</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Submitted</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((request) => (
                  <MemberRequestRow
                    key={request.id}
                    request={request}
                    project={projects.find((project) => project.id === request.projectId)}
                    onCancel={() => {
                      const result = cancel(request.id, userId);
                      setError(result.ok ? null : result.error);
                    }}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </>
  );
}

function MemberRequestRow({
  request,
  project,
  onCancel,
}: {
  request: ProjectJoinRequest;
  project: { name: string; code: string; slug: string } | undefined;
  onCancel: () => void;
}) {
  return (
    <tr className="border-t border-hairline align-top transition-colors hover:bg-muted/40">
      <td className="px-5 py-3">
        <div className="font-medium text-ink">{project?.name ?? "Project not found"}</div>
        <div className="mt-0.5 font-mono text-[11px] text-ink-soft">
          {project?.code ?? request.projectId}
        </div>
        {request.status === "rejected" && request.reviewNote ? (
          <p className="mt-2 max-w-md text-xs text-ink-soft">
            Rejection reason: <span className="text-ink">{request.reviewNote}</span>
          </p>
        ) : null}
      </td>
      <td className="px-3 py-3 text-xs text-ink-soft">{request.desiredPosition}</td>
      <td className="px-3 py-3">
        <StatusPill tone={joinRequestTone(request.status)}>{request.status}</StatusPill>
      </td>
      <td className="px-3 py-3 text-xs text-ink-soft">{formatDate(request.submittedAt)}</td>
      <td className="px-5 py-3">
        <div className="flex items-center justify-end gap-1">
          {project ? (
            <Link
              to="/projects/$slug"
              params={{ slug: project.slug }}
              className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
            >
              <ExternalLink className="h-3.5 w-3.5" /> Public
            </Link>
          ) : null}
          {request.status === "pending" ? (
            <button
              type="button"
              onClick={onCancel}
              className="rounded-md border border-hairline px-2 py-1 text-xs text-ink hover:bg-muted"
            >
              Cancel
            </button>
          ) : null}
        </div>
      </td>
    </tr>
  );
}

function FilterField({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 flex items-center justify-between text-xs font-medium text-ink">
        {label}
        {hint ? <span className="font-normal text-ink-soft">{hint}</span> : null}
      </span>
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
    <div className="space-y-3" aria-label="Loading join requests">
      {[0, 1, 2, 3].map((item) => (
        <div key={item} className="animate-pulse rounded-lg border border-hairline p-4">
          <div className="h-3 w-1/4 rounded bg-muted" />
          <div className="mt-2 h-2.5 w-3/5 rounded bg-muted" />
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
  const projectId = filters.projectId.trim();
  const requesterId = filters.requesterId.trim();
  if (projectId && !isUuid(projectId)) return "Project ID must be a valid UUID.";
  if (requesterId && !isUuid(requesterId)) return "Requester ID must be a valid UUID.";
  if (filters.createdFrom && filters.createdTo) {
    const from = new Date(filters.createdFrom);
    const to = new Date(filters.createdTo);
    if (from > to) return "Created-from must not be after created-to.";
  }
  return null;
}

function toApiFilters(filters: FilterDraft): AdminJoinRequestFilters {
  return {
    projectId: filters.projectId.trim() || undefined,
    requesterId: filters.requesterId.trim() || undefined,
    status: filters.status === "ALL" ? undefined : filters.status,
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

function adminStatusTone(status: AdminJoinRequestStatus) {
  if (status === "APPROVED") return "emerald" as const;
  if (status === "REJECTED") return "rose" as const;
  if (status === "CANCELLED") return "neutral" as const;
  return "amber" as const;
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
}

function formatBackendDateTime(value: string) {
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

function isOverrideDisabledError(error: unknown) {
  return (
    error instanceof ApiError &&
    error.status === 403 &&
    error.message === "Admin join-request override is disabled."
  );
}
