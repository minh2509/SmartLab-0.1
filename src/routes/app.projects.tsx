import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
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
import { Pencil, Trash2, RotateCcw, ExternalLink, Search, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { notifyOnce } from "@/lib/notifications-data";

export const Route = createFileRoute("/app/projects")({
  head: () => ({
    meta: [{ title: "Projects — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: AppProjectsIndex,
});

function AppProjectsIndex() {
  const { user, activeRole } = useAuth();
  const { projects, update, remove, reset } = useProjects();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<"all" | ProjectStatus>("all");
  const [editing, setEditing] = useState<Project | null>(null);
  const [deleting, setDeleting] = useState<Project | null>(null);
  const [resetOpen, setResetOpen] = useState(false);

  const userId = user?.id ?? "";
  const isAdmin = activeRole === "admin" && user?.roles.includes("admin");
  const isLeader = activeRole === "leader" && user?.roles.includes("leader");

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
        if (status !== "all" && p.status !== status) return false;
        if (!q.trim()) return true;
        const t = q.toLowerCase();
        return (
          p.name.toLowerCase().includes(t) ||
          p.code.toLowerCase().includes(t) ||
          p.description.toLowerCase().includes(t)
        );
      }),
    [visible, q, status],
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
              onClick={() => setResetOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-3 py-1.5 text-xs text-ink-soft hover:text-ink"
            >
              <RotateCcw className="h-3.5 w-3.5" /> Reset demo data
            </button>
          ) : undefined
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat label="Visible projects" value={visible.length} />
        <MiniStat label="Active" value={activeCount} tone="cyan" />
        <MiniStat label="Publishing" value={publishingCount} tone="emerald" />
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
              onChange={(e) => setStatus(e.target.value as "all" | ProjectStatus)}
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
        {filtered.length === 0 ? (
          <EmptyState
            title={
              visible.length === 0
                ? isLeader
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
                          params={{ slug: p.slug }}
                          className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Open
                        </Link>
                        <button
                          disabled={!canEdit(p)}
                          onClick={() => canEdit(p) && setEditing(p)}
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
                          onClick={() => isAdmin && setDeleting(p)}
                          className={cn(
                            "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                            isAdmin
                              ? "text-[color:var(--destructive)] hover:bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)]"
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

      <ProjectEditDialog
        project={editing}
        open={!!editing}
        onClose={() => setEditing(null)}
        canEditLeaders={isAdmin}
        onSave={(patch) => {
          if (editing) {
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
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          if (deleting) remove(deleting.id);
          setDeleting(null);
        }}
      />
      <ResetProjectsDialog
        open={resetOpen}
        onClose={() => setResetOpen(false)}
        onConfirm={() => {
          reset();
          setResetOpen(false);
        }}
      />
    </>
  );
}

function ResetProjectsDialog({
  open,
  onClose,
  onConfirm,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const [confirmation, setConfirmation] = useState("");
  if (!open) return null;
  const valid = confirmation.trim() === "RESET PROJECTS";
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-md rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Reset demo projects
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">Restore project seed data</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="space-y-4 p-5">
          <p className="text-sm leading-relaxed text-ink-soft">
            This resets the project catalogue only. Related demo records in other modules remain in
            localStorage.
          </p>
          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Type RESET PROJECTS
            </div>
            <input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
          </label>
        </div>
        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={!valid}
            onClick={onConfirm}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Reset projects
          </button>
        </footer>
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
