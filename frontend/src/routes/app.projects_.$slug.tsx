import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import {
  useProjects,
  fieldMeta,
  statusTone,
  formatDate,
  getUserName,
  getUserInitials,
} from "@/lib/projects-data";
import {
  joinRequestTone,
  useJoinRequests,
  type ProjectJoinRequest,
} from "@/lib/join-requests-data";
import { notifyOnce } from "@/lib/notifications-data";
import { PageHeader, Panel, StatusPill, EmptyState } from "@/components/app/ui";
import { ProjectEditDialog } from "@/components/app/projects/ProjectEditDialog";
import { ProjectTasksPanel } from "@/components/app/projects/ProjectTasksPanel";
import { ProjectEvaluationsPanel } from "@/components/app/projects/ProjectEvaluationsPanel";
import { ArrowLeft, Pencil, Lock, X } from "lucide-react";

export const Route = createFileRoute("/app/projects_/$slug")({
  head: () => ({
    meta: [{ title: "Project — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  notFoundComponent: () => <div className="p-8 text-sm text-ink-soft">Project not found.</div>,
  component: WorkspaceProjectDetail,
});

function WorkspaceProjectDetail() {
  const { slug } = Route.useParams();
  const { user, activeRole } = useAuth();
  const { projects, update, addMember } = useProjects();
  const { requests, approve, reject } = useJoinRequests();
  const [editing, setEditing] = useState(false);
  const [reviewing, setReviewing] = useState<ProjectJoinRequest | null>(null);
  const [reviewMode, setReviewMode] = useState<"approve" | "reject">("approve");
  const [reviewError, setReviewError] = useState<string | null>(null);

  if (!user || !activeRole) return null;
  const project = projects.find((p) => p.slug === slug);
  if (!project) throw notFound();

  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const isLeader =
    activeRole === "leader" && user.roles.includes("leader") && project.leaderIds.includes(user.id);
  const isAssignedLeader = user.roles.includes("leader") && project.leaderIds.includes(user.id);
  const isMember = project.memberIds.includes(user.id) || project.leaderIds.includes(user.id);
  const canEdit = isAdmin || isLeader;
  const canView = isAdmin || isMember;
  const canReviewRequests =
    activeRole === "leader" && user.roles.includes("leader") && isAssignedLeader;
  const projectRequests = requests.filter((r) => r.projectId === project.id);
  const visibleRequests =
    canReviewRequests || isAdmin
      ? projectRequests
      : projectRequests.filter((r) => r.userId === user.id);
  const pendingRequests = projectRequests.filter((r) => r.status === "pending");

  if (!canView) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <Lock className="h-4 w-4 text-ink-soft" />
        </div>
        <h2 className="mt-4 text-sm font-semibold text-ink">
          You don't have access to this project
        </h2>
        <p className="mt-1 text-xs text-ink-soft">
          Only members, assigned leaders, and administrators can open this workspace.
        </p>
        <Link
          to="/app/projects"
          className="mt-4 inline-flex items-center gap-1.5 text-sm text-ink hover:opacity-70"
        >
          <ArrowLeft className="h-3.5 w-3.5" /> Back to projects
        </Link>
      </div>
    );
  }

  return (
    <>
      <Link
        to="/app/projects"
        className="mb-4 inline-flex items-center gap-1.5 text-xs text-ink-soft hover:text-ink"
      >
        <ArrowLeft className="h-3.5 w-3.5" /> All projects
      </Link>

      <PageHeader
        eyebrow={`${project.code} · ${project.type}`}
        title={project.name}
        description={project.description}
        action={
          <div className="flex items-center gap-2">
            <StatusPill
              tone={
                statusTone[project.status] === "neutral" ? "neutral" : statusTone[project.status]
              }
            >
              {project.status}
            </StatusPill>
            {canEdit ? (
              <button
                onClick={() => setEditing(true)}
                className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
              >
                <Pencil className="h-3.5 w-3.5" /> Edit project
              </button>
            ) : (
              <span className="rounded-md border border-hairline px-2.5 py-1 text-[11px] text-ink-soft">
                Read only
              </span>
            )}
          </div>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-4">
          <Panel title="Objective">
            <p className="text-sm leading-relaxed text-ink">{project.objective}</p>
          </Panel>

          <Panel title="Progress" description={`${project.progress}% complete`}>
            <div className="h-1.5 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full bg-[color:var(--cyan)]"
                style={{ width: `${project.progress}%` }}
              />
            </div>
            <div className="mt-3 flex flex-wrap justify-between gap-2 text-[11px] text-ink-soft">
              <span>Start · {formatDate(project.startDate)}</span>
              <span>Expected end · {formatDate(project.expectedEnd)}</span>
            </div>
          </Panel>

          <Panel
            title="Team"
            description={`${project.leaderIds.length + project.memberIds.length} people`}
          >
            <div className="grid gap-3 sm:grid-cols-2">
              {project.leaderIds.map((id) => (
                <PersonRow key={id} id={id} role="Lead" />
              ))}
              {project.memberIds.map((id) => (
                <PersonRow key={id} id={id} role="Member" />
              ))}
              {project.leaderIds.length + project.memberIds.length === 0 ? (
                <div className="text-xs text-ink-soft">No one assigned.</div>
              ) : null}
            </div>
          </Panel>

          <ProjectTasksPanel project={project} user={user} activeRole={activeRole} />

          <ProjectEvaluationsPanel project={project} user={user} activeRole={activeRole} />

          {canReviewRequests || isAdmin ? (
            <RequestsPanel
              requests={canReviewRequests ? pendingRequests : visibleRequests}
              canReview={canReviewRequests}
              adminView={isAdmin && !canReviewRequests}
              onReview={(request, mode) => {
                setReviewing(request);
                setReviewMode(mode);
                setReviewError(null);
              }}
            />
          ) : null}
        </div>

        <aside className="space-y-4">
          <Panel title="Research fields">
            <div className="flex flex-wrap gap-1.5">
              {project.fields.map((k) => (
                <StatusPill key={k} tone={fieldMeta[k].tone}>
                  {fieldMeta[k].name}
                </StatusPill>
              ))}
            </div>
          </Panel>
          <Panel title="Metadata">
            <dl className="space-y-2 text-sm">
              <Meta label="Type" value={project.type} />
              <Meta label="Status" value={project.status} />
              <Meta label="Visibility" value={project.visibility} />
              <Meta label="Code" value={project.code} mono />
            </dl>
          </Panel>
          <Panel title="Your access">
            <div className="text-xs text-ink-soft">
              You are viewing as{" "}
              <span className="font-medium text-ink">
                {isAdmin ? "Administrator" : isLeader ? "Project Leader" : "Member"}
              </span>
              . {canEdit ? "You can edit project metadata." : "Read-only view."}
            </div>
          </Panel>
        </aside>
      </div>

      <ProjectEditDialog
        project={editing ? project : null}
        open={editing}
        onClose={() => setEditing(false)}
        canEditLeaders={isAdmin}
        onSave={(patch) => {
          if (patch.memberIds) {
            patch.memberIds
              .filter((memberId) => !project.memberIds.includes(memberId))
              .forEach((memberId) => {
                notifyOnce({
                  userId: memberId,
                  type: "project_member_added",
                  title: "Project membership updated",
                  message: `You were added as a member of ${project.name}.`,
                  link: `/app/projects/${project.slug}`,
                  eventKey: `project:${project.id}:member-added:${memberId}`,
                });
              });
          }
          update(project.id, patch);
          setEditing(false);
        }}
      />
      <ReviewRequestDialog
        request={reviewing}
        mode={reviewMode}
        error={reviewError}
        onClose={() => {
          setReviewing(null);
          setReviewError(null);
        }}
        onSubmit={(note) => {
          if (!reviewing) return;
          const currentProject = projects.find((p) => p.id === project.id);
          const currentRequest = requests.find((r) => r.id === reviewing.id);
          if (!currentProject || !currentRequest) {
            setReviewError("Project or request not found.");
            return;
          }
          if (
            activeRole !== "leader" ||
            !user.roles.includes("leader") ||
            !currentProject.leaderIds.includes(user.id)
          ) {
            setReviewError("Only assigned project leaders can review requests.");
            return;
          }
          if (currentRequest.status !== "pending") {
            setReviewError("Only pending requests can be reviewed.");
            return;
          }

          if (reviewMode === "reject") {
            const result = reject(currentRequest.id, user.id, note);
            if (!result.ok) {
              setReviewError(result.error);
              return;
            }
            notifyOnce({
              userId: currentRequest.userId,
              type: "join_request_rejected",
              title: "Join request rejected",
              message: `Your request to join ${currentProject.name} was not approved.`,
              link: `/projects/${currentProject.slug}`,
              eventKey: `join-request:${currentRequest.id}:rejected`,
            });
            setReviewing(null);
            setReviewError(null);
            return;
          }

          const wasMember = currentProject.memberIds.includes(currentRequest.userId);
          const previousMemberIds = currentProject.memberIds;
          const memberAdded = addMember(currentProject.id, currentRequest.userId);
          if (!memberAdded) {
            setReviewError("The applicant could not be added to the project member list.");
            return;
          }
          const result = approve(currentRequest.id, user.id, note);
          if (!result.ok) {
            if (!wasMember) update(currentProject.id, { memberIds: previousMemberIds });
            setReviewError(result.error);
            return;
          }
          notifyOnce({
            userId: currentRequest.userId,
            type: "join_request_approved",
            title: "Join request approved",
            message: `Your request to join ${currentProject.name} was approved.`,
            link: `/app/projects/${currentProject.slug}`,
            eventKey: `join-request:${currentRequest.id}:approved`,
          });
          notifyOnce({
            userId: currentRequest.userId,
            type: "project_member_added",
            title: "Project membership updated",
            message: `You were added as a member of ${currentProject.name}.`,
            link: `/app/projects/${currentProject.slug}`,
            eventKey: `project:${currentProject.id}:member-added:${currentRequest.userId}:${currentRequest.id}`,
          });
          setReviewing(null);
          setReviewError(null);
        }}
      />
    </>
  );
}

function RequestsPanel({
  requests,
  canReview,
  adminView,
  onReview,
}: {
  requests: ProjectJoinRequest[];
  canReview: boolean;
  adminView: boolean;
  onReview: (request: ProjectJoinRequest, mode: "approve" | "reject") => void;
}) {
  return (
    <Panel
      title="Participation requests"
      description={
        canReview
          ? `${requests.length} pending request${requests.length === 1 ? "" : "s"}`
          : "Read-only oversight"
      }
    >
      {requests.length === 0 ? (
        <EmptyState
          title={canReview ? "No pending requests" : "No requests submitted"}
          hint={
            canReview
              ? "New member requests for this project will appear here."
              : "Admin oversight is read-only in this module."
          }
        />
      ) : (
        <div className="space-y-3">
          {requests.map((request) => (
            <div key={request.id} className="rounded-lg border border-hairline p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <div className="font-medium text-ink">{getUserName(request.userId)}</div>
                    <StatusPill tone={joinRequestTone(request.status)}>{request.status}</StatusPill>
                  </div>
                  <div className="mt-1 text-xs text-ink-soft">
                    Desired role · <span className="text-ink">{request.desiredPosition}</span>
                  </div>
                </div>
                {canReview && request.status === "pending" ? (
                  <div className="flex shrink-0 items-center gap-2">
                    <button
                      onClick={() => onReview(request, "approve")}
                      className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => onReview(request, "reject")}
                      className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
                    >
                      Reject
                    </button>
                  </div>
                ) : adminView ? (
                  <span className="rounded-md border border-hairline px-2.5 py-1 text-[11px] text-ink-soft">
                    Read only
                  </span>
                ) : null}
              </div>
              <dl className="mt-4 grid gap-3 text-xs md:grid-cols-2">
                <RequestMeta label="Reason" value={request.reason} />
                <RequestMeta label="Skills" value={request.skills} />
                <RequestMeta label="Experience" value={request.experience || "Not provided"} />
                <RequestMeta label="Introduction" value={request.introduction || "Not provided"} />
              </dl>
              {request.reviewNote ? (
                <div className="mt-3 rounded-lg border border-hairline bg-muted/40 p-3 text-xs">
                  <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                    Review note
                  </div>
                  <p className="mt-1 text-ink">{request.reviewNote}</p>
                </div>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </Panel>
  );
}

function ReviewRequestDialog({
  request,
  mode,
  error,
  onClose,
  onSubmit,
}: {
  request: ProjectJoinRequest | null;
  mode: "approve" | "reject";
  error: string | null;
  onClose: () => void;
  onSubmit: (note: string) => void;
}) {
  const [note, setNote] = useState("");
  useEffect(() => setNote(""), [request?.id, mode]);
  if (!request) return null;
  const rejectInvalid = mode === "reject" && !note.trim();

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(e) => e.stopPropagation()}
        className="w-full max-w-lg rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {mode === "approve" ? "Approve request" : "Reject request"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{getUserName(request.userId)}</h2>
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
          <div className="rounded-lg border border-hairline bg-muted/30 p-3 text-xs text-ink-soft">
            <div>
              Desired role · <span className="font-medium text-ink">{request.desiredPosition}</span>
            </div>
            <p className="mt-2 text-ink">{request.reason}</p>
          </div>
          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {mode === "approve" ? "Approval note (optional)" : "Rejection reason"}
            </div>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="min-h-[96px] w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder={
                mode === "approve"
                  ? "Add a short note for the applicant."
                  : "Explain why this request is not approved yet."
              }
            />
          </label>
          {rejectInvalid ? (
            <div className="text-xs text-[color:var(--destructive)]">
              Rejection reason is required.
            </div>
          ) : null}
          {error ? (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {error}
            </div>
          ) : null}
        </div>
        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            onClick={onClose}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            disabled={rejectInvalid}
            onClick={() => onSubmit(note)}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {mode === "approve" ? "Approve" : "Reject"}
          </button>
        </footer>
      </div>
    </div>
  );
}

function RequestMeta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</dt>
      <dd className="mt-1 leading-relaxed text-ink">{value}</dd>
    </div>
  );
}

function PersonRow({ id, role }: { id: string; role: string }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-hairline p-3">
      <div className="grid h-9 w-9 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
        {getUserInitials(id)}
      </div>
      <div className="text-sm">
        <div className="font-medium text-ink">{getUserName(id)}</div>
        <div className="text-xs text-ink-soft">{role}</div>
      </div>
    </div>
  );
}

function Meta({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between">
      <dt className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</dt>
      <dd className={`text-sm text-ink capitalize ${mono ? "font-mono text-xs" : ""}`}>{value}</dd>
    </div>
  );
}
