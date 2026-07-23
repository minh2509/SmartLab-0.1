import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
import {
  adminProjectErrorMessage,
  createAdminProject,
  deleteAdminProject,
  updateAdminProject,
  useAdminProjects,
} from "@/lib/admin-projects-api";
import {
  useProjects,
  fieldMeta,
  statusTone,
  formatDate,
  type Project,
  type ProjectStatus,
} from "@/lib/projects-data";
import { PageHeader, Panel, StatusPill, EmptyState } from "@/components/app/ui";
import { ProjectEditDialog } from "@/components/app/projects/ProjectEditDialog";
import { DeleteProjectDialog } from "@/components/app/projects/DeleteProjectDialog";
import { Pencil, Trash2, ExternalLink, Plus, Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { notifyOnce } from "@/lib/notifications-data";

export const Route = createFileRoute("/app/projects")({
  head: () => ({
    meta: [{ title: "Projects — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: AppProjectsIndex,
});

function AppProjectsIndex() {
  const { user, activeRole, accessToken } = useAuth();
  const { projects: localProjects, update } = useProjects();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<"all" | ProjectStatus>("all");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Project | null>(null);
  const [deleting, setDeleting] = useState<Project | null>(null);
  const [mutationPending, setMutationPending] = useState(false);
  const [mutationError, setMutationError] = useState<string | null>(null);

  const userId = user?.id ?? "";
  const isAdmin = activeRole === "admin" && Boolean(user?.roles.includes("admin"));
  const isLeader = activeRole === "leader" && Boolean(user?.roles.includes("leader"));
  const adminProjects = useAdminProjects(accessToken, isAdmin, page, 10, status);
  const projects = isAdmin ? adminProjects.data.items : localProjects;

  const visible = useMemo(() => {
    if (!user) return [];
    if (isAdmin) return projects;
    if (isLeader) return projects.filter((p) => p.leaderIds.includes(userId));
    // members: only projects they belong to (read-only)
    return projects.filter((p) => p.leaderIds.includes(userId) || p.memberIds.includes(userId));
  }, [projects, isAdmin, isLeader, user, userId]);

  const filtered = useMemo(
    () =>
      visible.filter((p) => {
        if (!isAdmin && status !== "all" && p.status !== status) return false;
        if (!q.trim()) return true;
        const t = q.toLowerCase();
        return (
          p.name.toLowerCase().includes(t) ||
          p.code.toLowerCase().includes(t) ||
          p.description.toLowerCase().includes(t)
        );
      }),
    [visible, q, status, isAdmin],
  );

  const activeCount = visible.filter((p) => p.status === "Active").length;
  const publishingCount = visible.filter((p) => p.status === "Publishing").length;

  if (!user || !activeRole) return null;

  const canEdit = (p: Project) => isAdmin || (isLeader && p.leaderIds.includes(user.id));

  return (
    <>
      <PageHeader
        eyebrow={isAdmin ? "Administration" : isLeader ? "Project leader" : "Your projects"}
        title="Projects"
        description={
          isAdmin
            ? "Manage every project across the lab. Only administrators can delete projects."
            : isLeader
              ? "Projects where you are assigned as a leader. You can edit metadata and progress."
              : "Projects you are a member of. Contact a leader to request changes."
        }
        action={
          isAdmin ? (
            <button
              type="button"
              onClick={() => {
                setMutationError(null);
                setEditing(newProjectDraft());
              }}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Plus className="h-3.5 w-3.5" /> Create project
            </button>
          ) : undefined
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat
          label={isAdmin ? "Database projects" : "Visible projects"}
          value={isAdmin ? adminProjects.data.totalElements : visible.length}
        />
        <MiniStat label={isAdmin ? "Active on page" : "Active"} value={activeCount} tone="cyan" />
        <MiniStat
          label={isAdmin ? "Publishing on page" : "Publishing"}
          value={publishingCount}
          tone="emerald"
        />
      </div>

      <Panel
        title={isAdmin ? "All projects" : "Assigned projects"}
        description={`${filtered.length} shown`}
        action={
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-soft" />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search name or code…"
                className="w-56 rounded-md border border-hairline bg-background py-1.5 pl-7 pr-2 text-xs text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              />
            </div>
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as "all" | ProjectStatus);
                setPage(0);
              }}
              className="rounded-md border border-hairline bg-background px-2 py-1.5 text-xs text-ink"
            >
              <option value="all">All statuses</option>
              {(
                ["Active", "Publishing", "Planning", "On hold", "Completed"] as ProjectStatus[]
              ).map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
        }
        className="overflow-hidden"
      >
        {isAdmin && adminProjects.loading ? (
          <EmptyState title="Loading projects" hint="Reading the Admin project catalogue..." />
        ) : isAdmin && adminProjects.error ? (
          <div className="py-8 text-center">
            <EmptyState title="Projects could not be loaded" hint={adminProjects.error} />
            <button
              type="button"
              onClick={adminProjects.retry}
              className="mt-3 rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
            >
              Try again
            </button>
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title={
              visible.length === 0
                ? isAdmin
                  ? "No projects in the database"
                  : isLeader
                    ? "You are not leading any projects yet"
                    : "No projects assigned to you"
                : "No projects match your filters"
            }
            hint={
              visible.length === 0
                ? "An administrator can assign you to a project."
                : "Try clearing the search or switching status."
            }
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[880px] text-sm">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Project</th>
                  <th className="px-3 py-3 font-medium">Fields</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Progress</th>
                  <th className="px-3 py-3 font-medium">Timeline</th>
                  <th className="px-3 py-3 font-medium">Visibility</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((p) => (
                  <tr
                    key={p.id}
                    className="border-t border-hairline transition-colors hover:bg-muted/40"
                  >
                    <td className="px-5 py-3">
                      <div className="font-mono text-[11px] text-ink-soft">{p.code}</div>
                      <div className="font-medium text-ink">{p.name}</div>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex flex-wrap gap-1">
                        {p.fields.map((k) => (
                          <StatusPill key={k} tone={fieldMeta[k].tone}>
                            {fieldMeta[k].name.split(" ")[0]}
                          </StatusPill>
                        ))}
                      </div>
                    </td>
                    <td className="px-3 py-3">
                      <StatusPill
                        tone={statusTone[p.status] === "neutral" ? "neutral" : statusTone[p.status]}
                      >
                        {p.status}
                      </StatusPill>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex items-center gap-2">
                        <div className="h-1 w-24 overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full bg-[color:var(--cyan)]"
                            style={{ width: `${p.progress}%` }}
                          />
                        </div>
                        <span className="font-mono text-[11px] text-ink-soft">{p.progress}%</span>
                      </div>
                    </td>
                    <td className="px-3 py-3 text-[12px] text-ink-soft">
                      {formatDate(p.startDate)} → <br className="lg:hidden" />
                      {formatDate(p.expectedEnd)}
                    </td>
                    <td className="px-3 py-3 text-xs capitalize text-ink-soft">{p.visibility}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <Link
                          to="/app/projects/$slug"
                          params={{ slug: isAdmin ? p.id : p.slug }}
                          className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Open
                        </Link>
                        <button
                          disabled={!canEdit(p)}
                          onClick={() => {
                            if (!canEdit(p)) return;
                            setMutationError(null);
                            setEditing(p);
                          }}
                          className={cn(
                            "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                            canEdit(p)
                              ? "text-ink-soft hover:bg-muted hover:text-ink"
                              : "cursor-not-allowed text-ink-soft/40",
                          )}
                          title={
                            canEdit(p) ? "Edit project" : "Only assigned leaders or admins can edit"
                          }
                        >
                          <Pencil className="h-3.5 w-3.5" /> Edit
                        </button>
                        <button
                          disabled={!isAdmin}
                          onClick={() => {
                            if (!isAdmin) return;
                            setMutationError(null);
                            setDeleting(p);
                          }}
                          className={cn(
                            "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                            isAdmin
                              ? "text-ink-soft hover:bg-muted hover:text-[color:var(--destructive)]"
                              : "cursor-not-allowed text-ink-soft/40",
                          )}
                          title={isAdmin ? "Delete project" : "Only administrators can delete"}
                        >
                          <Trash2 className="h-3.5 w-3.5" /> Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {isAdmin &&
      !adminProjects.loading &&
      !adminProjects.error &&
      adminProjects.data.totalPages > 1 ? (
        <div className="mt-4 flex items-center justify-end gap-3 text-xs text-ink-soft">
          <button
            type="button"
            disabled={adminProjects.data.page === 0}
            onClick={() => setPage((value) => Math.max(0, value - 1))}
            className="rounded-md border border-hairline px-3 py-1.5 text-ink disabled:cursor-not-allowed disabled:opacity-40"
          >
            Previous
          </button>
          <span>
            Page {adminProjects.data.page + 1} of {adminProjects.data.totalPages}
          </span>
          <button
            type="button"
            disabled={adminProjects.data.page + 1 >= adminProjects.data.totalPages}
            onClick={() => setPage((value) => value + 1)}
            className="rounded-md border border-hairline px-3 py-1.5 text-ink disabled:cursor-not-allowed disabled:opacity-40"
          >
            Next
          </button>
        </div>
      ) : null}

      <ProjectEditDialog
        project={editing}
        open={!!editing}
        onClose={() => setEditing(null)}
        canEditLeaders={isAdmin}
        accessToken={isAdmin ? accessToken : null}
        pending={mutationPending}
        serverError={mutationError}
        onSave={(patch) => {
          if (editing) {
            if (isAdmin) {
              if (!accessToken) {
                setMutationError("Your session has expired. Sign in again to save this project.");
                return;
              }
              setMutationPending(true);
              setMutationError(null);
              const candidate = { ...editing, ...patch };
              void (
                editing.id
                  ? updateAdminProject(accessToken, candidate)
                  : createAdminProject(accessToken, candidate)
              )
                .then(() => {
                  setEditing(null);
                  adminProjects.retry();
                })
                .catch((error: unknown) => {
                  setMutationError(adminProjectErrorMessage(error));
                })
                .finally(() => setMutationPending(false));
              return;
            }
            if (patch.memberIds) {
              patch.memberIds
                .filter((memberId) => !editing.memberIds.includes(memberId))
                .forEach((memberId) => {
                  notifyOnce({
                    userId: memberId,
                    type: "project_member_added",
                    title: "Project membership updated",
                    message: `You were added as a member of ${editing.name}.`,
                    link: `/app/projects/${editing.slug}`,
                    eventKey: `project:${editing.id}:member-added:${memberId}`,
                  });
                });
            }
            update(editing.id, patch);
          }
          setEditing(null);
        }}
      />
      <DeleteProjectDialog
        project={deleting}
        open={!!deleting}
        pending={mutationPending}
        error={mutationError}
        onClose={() => {
          if (mutationPending) return;
          setDeleting(null);
          setMutationError(null);
        }}
        onConfirm={() => {
          if (!deleting || !accessToken) {
            setMutationError("Your session has expired. Sign in again to delete this project.");
            return;
          }
          setMutationPending(true);
          setMutationError(null);
          void deleteAdminProject(accessToken, deleting.id)
            .then(() => {
              setDeleting(null);
              if (adminProjects.data.items.length === 1 && page > 0) setPage(page - 1);
              else adminProjects.retry();
            })
            .catch((error: unknown) => setMutationError(adminProjectErrorMessage(error)))
            .finally(() => setMutationPending(false));
        }}
      />
    </>
  );
}

function newProjectDraft(): Project {
  const start = new Date();
  const expectedEnd = new Date(start);
  expectedEnd.setFullYear(expectedEnd.getFullYear() + 1);
  const isoDate = (value: Date) => value.toISOString().slice(0, 10);
  return {
    id: "",
    slug: "",
    code: "",
    name: "",
    description: "",
    objective: "",
    type: "Research",
    fields: [],
    leaderIds: [],
    memberIds: [],
    startDate: isoDate(start),
    expectedEnd: isoDate(expectedEnd),
    status: "Planning",
    progress: 0,
    visibility: "internal",
  };
}

function MiniStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "cyan" | "emerald";
}) {
  const bar =
    tone === "cyan"
      ? "bg-[color:var(--cyan)]"
      : tone === "emerald"
        ? "bg-[color:var(--emerald-ink)]"
        : "bg-hairline";
  return (
    <div className="relative overflow-hidden rounded-xl border border-hairline bg-surface-elev p-4">
      <div className={`absolute inset-x-0 top-0 h-0.5 ${bar}`} />
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div className="mt-1 font-display text-2xl text-ink">{value}</div>
    </div>
  );
}
