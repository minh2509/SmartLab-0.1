import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { useState } from "react";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { Chip } from "@/components/site/primitives";
import {
  useProjects,
  fieldMeta,
  statusTone,
  formatDate,
  getUserName,
  getUserInitials,
} from "@/lib/projects-data";
import { useAuth } from "@/lib/auth";
import {
  getLatestUserProjectRequest,
  useJoinRequests,
  type JoinRequestDraft,
} from "@/lib/join-requests-data";
import { notifyManyOnce } from "@/lib/notifications-data";
import { JoinRequestDialog } from "@/components/app/projects/JoinRequestDialog";
import { ArrowLeft, Lock } from "lucide-react";

export const Route = createFileRoute("/projects_/$slug")({
  head: ({ params }) => ({
    meta: [
      { title: `${params.slug} — SmartResearch Lab` },
      { name: "description", content: `Public project page at SmartResearch Lab.` },
    ],
  }),
  notFoundComponent: () => (
    <div className="min-h-screen bg-background">
      <SiteHeader />
      <div className="mx-auto max-w-3xl px-6 py-24 text-center">
        <h1 className="font-display text-4xl text-ink">Project not found</h1>
        <p className="mt-3 text-sm text-ink-soft">
          This project is either internal or has been retired.
        </p>
        <Link
          to="/projects"
          className="mt-6 inline-flex items-center gap-1.5 text-sm text-ink hover:opacity-70"
        >
          <ArrowLeft className="h-4 w-4" /> Back to projects
        </Link>
      </div>
      <SiteFooter />
    </div>
  ),
  component: PublicProjectDetail,
});

function PublicProjectDetail() {
  const { slug } = Route.useParams();
  const { projects } = useProjects();
  const { user, activeRole } = useAuth();
  const { requests, submit, cancel } = useJoinRequests();
  const [joining, setJoining] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const project = projects.find((p) => p.slug === slug);

  if (!project) throw notFound();

  // Public visibility gate: hide internal projects unless logged-in member
  const isMember =
    !!user && (project.leaderIds.includes(user.id) || project.memberIds.includes(user.id));
  const isAdmin = activeRole === "admin" && !!user?.roles.includes("admin");
  const canSeeInternal = isAdmin || isMember;
  const ownRequest = user ? getLatestUserProjectRequest(requests, project.id, user.id) : undefined;

  if (project.visibility === "internal" && !canSeeInternal) {
    return (
      <div className="min-h-screen bg-background">
        <SiteHeader />
        <div className="mx-auto max-w-3xl px-6 py-24 text-center">
          <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-muted">
            <Lock className="h-5 w-5 text-ink-soft" />
          </div>
          <h1 className="mt-6 font-display text-3xl text-ink">This project is internal</h1>
          <p className="mx-auto mt-3 max-w-md text-sm text-ink-soft">
            Only project members and lab administrators can view the details of this project.
          </p>
          <div className="mt-6 flex items-center justify-center gap-2">
            <Link
              to="/auth"
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
            >
              Sign in
            </Link>
            <Link
              to="/projects"
              className="rounded-md border border-hairline px-4 py-2 text-sm text-ink hover:bg-muted"
            >
              Back to projects
            </Link>
          </div>
        </div>
        <SiteFooter />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-ink">
      <SiteHeader />

      <section className="border-b border-hairline">
        <div className="mx-auto max-w-5xl px-6 pb-12 pt-12 md:pt-16">
          <Link
            to="/projects"
            className="inline-flex items-center gap-1.5 text-xs text-ink-soft hover:text-ink"
          >
            <ArrowLeft className="h-3.5 w-3.5" /> All projects
          </Link>
          <div className="mt-6 flex flex-wrap items-center gap-2 text-xs text-ink-soft">
            <span className="font-mono">{project.code}</span>
            <span>·</span>
            <span>{project.type}</span>
            <Chip
              tone={
                statusTone[project.status] === "neutral" ? undefined : statusTone[project.status]
              }
            >
              {project.status}
            </Chip>
          </div>
          <h1 className="mt-4 font-display text-4xl leading-tight text-ink md:text-5xl">
            {project.name}
          </h1>
          <p className="mt-5 max-w-3xl text-base leading-relaxed text-ink-soft">
            {project.description}
          </p>

          <div className="mt-8 flex flex-wrap gap-1.5">
            {project.fields.map((k) => (
              <Chip key={k} tone={fieldMeta[k].tone}>
                {fieldMeta[k].name}
              </Chip>
            ))}
          </div>
        </div>
      </section>

      <section>
        <div className="mx-auto grid max-w-5xl gap-8 px-6 py-12 md:grid-cols-3 md:py-16">
          <div className="md:col-span-2 space-y-8">
            <div>
              <h2 className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Objective</h2>
              <p className="mt-3 text-base leading-relaxed text-ink">{project.objective}</p>
            </div>

            <div>
              <h2 className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Progress</h2>
              <div className="mt-3 flex items-center gap-4">
                <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full bg-[color:var(--cyan)]"
                    style={{ width: `${project.progress}%` }}
                  />
                </div>
                <span className="font-mono text-sm text-ink">{project.progress}%</span>
              </div>
            </div>

            <div>
              <h2 className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">
                Project leaders
              </h2>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {project.leaderIds.map((id) => (
                  <div
                    key={id}
                    className="flex items-center gap-3 rounded-lg border border-hairline bg-surface-elev p-3"
                  >
                    <div className="grid h-9 w-9 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                      {getUserInitials(id)}
                    </div>
                    <div className="text-sm">
                      <div className="font-medium text-ink">{getUserName(id)}</div>
                      <div className="text-xs text-ink-soft">Lead</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {canSeeInternal ? (
              <div className="rounded-xl border border-hairline bg-surface-elev p-5">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-ink">Internal information</h2>
                  <Chip tone="violet">Members only</Chip>
                </div>
                <dl className="grid gap-4 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-[11px] uppercase tracking-wider text-ink-soft">
                      Team size
                    </dt>
                    <dd className="mt-1 text-ink">
                      {project.leaderIds.length + project.memberIds.length} people
                    </dd>
                  </div>
                  <div>
                    <dt className="text-[11px] uppercase tracking-wider text-ink-soft">
                      Visibility
                    </dt>
                    <dd className="mt-1 text-ink capitalize">{project.visibility}</dd>
                  </div>
                </dl>
                <Link
                  to="/app/projects/$slug"
                  params={{ slug: project.slug }}
                  className="mt-4 inline-flex text-sm text-ink hover:opacity-70"
                >
                  Open in workspace →
                </Link>
              </div>
            ) : null}
          </div>

          <aside className="space-y-4">
            <div className="rounded-xl border border-hairline bg-surface-elev p-5">
              <div className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Timeline</div>
              <div className="mt-3 text-sm text-ink">
                <div>
                  <span className="text-ink-soft">Start · </span>
                  {formatDate(project.startDate)}
                </div>
                <div className="mt-1">
                  <span className="text-ink-soft">Expected end · </span>
                  {formatDate(project.expectedEnd)}
                </div>
              </div>
            </div>
            <div className="rounded-xl border border-hairline bg-surface-elev p-5">
              <div className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Type</div>
              <div className="mt-2 text-sm text-ink">{project.type}</div>
            </div>
            <div className="rounded-xl border border-hairline bg-surface-elev p-5">
              <div className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Team</div>
              <div className="mt-2 text-sm text-ink">
                {project.leaderIds.length} leader
                {project.leaderIds.length === 1 ? "" : "s"} · {project.memberIds.length} member
                {project.memberIds.length === 1 ? "" : "s"}
              </div>
            </div>
            <JoinRequestCard
              signedIn={!!user}
              hasMemberRole={!!user?.roles.includes("member")}
              isProjectMember={!!user && project.memberIds.includes(user.id)}
              isProjectLeader={!!user && project.leaderIds.includes(user.id)}
              visibility={project.visibility}
              latestRequest={ownRequest}
              error={actionError}
              onOpen={() => {
                setActionError(null);
                setJoining(true);
              }}
              onCancel={() => {
                if (!user || !ownRequest) return;
                const result = cancel(ownRequest.id, user.id);
                setActionError(result.ok ? null : result.error);
              }}
            />
          </aside>
        </div>
      </section>

      <SiteFooter />
      <JoinRequestDialog
        projectName={project.name}
        open={joining}
        submitting={submitting}
        error={actionError}
        onClose={() => {
          if (!submitting) setJoining(false);
        }}
        onSubmit={(draft: JoinRequestDraft) => {
          if (!user || submitting) return;
          setSubmitting(true);
          setActionError(null);
          const result = submit(project.id, user.id, draft);
          if (result.ok) {
            notifyManyOnce(
              project.leaderIds.map((leaderId) => ({
                userId: leaderId,
                type: "join_request_received",
                title: "New project join request",
                message: `${user.fullName} requested to join ${project.name}.`,
                link: `/app/projects/${project.slug}`,
                eventKey: `join-request:${result.request.id}:received:${leaderId}`,
              })),
            );
            setJoining(false);
          } else {
            setActionError(result.error);
          }
          setSubmitting(false);
        }}
      />
    </div>
  );
}

function JoinRequestCard({
  signedIn,
  hasMemberRole,
  isProjectMember,
  isProjectLeader,
  visibility,
  latestRequest,
  error,
  onOpen,
  onCancel,
}: {
  signedIn: boolean;
  hasMemberRole: boolean;
  isProjectMember: boolean;
  isProjectLeader: boolean;
  visibility: "public" | "internal";
  latestRequest?: {
    status: "pending" | "approved" | "rejected" | "cancelled";
    reviewNote?: string;
  };
  error: string | null;
  onOpen: () => void;
  onCancel: () => void;
}) {
  const blocked =
    !signedIn ||
    !hasMemberRole ||
    isProjectMember ||
    isProjectLeader ||
    visibility === "internal" ||
    latestRequest?.status === "pending" ||
    latestRequest?.status === "approved";

  let title = "Join this project";
  let message = "Submit a short request so assigned leaders can review your fit.";

  if (!signedIn) {
    title = "Sign in to request access";
    message = "Members can request to join public projects after signing in.";
  } else if (!hasMemberRole) {
    title = "Member role required";
    message = "Only users with the Member role can request project access.";
  } else if (isProjectLeader) {
    title = "You already lead this project";
    message = "Project leaders already have workspace access.";
  } else if (isProjectMember) {
    title = "You are already a member";
    message = "Open the project workspace from your member portal.";
  } else if (visibility === "internal") {
    title = "Internal project";
    message = "Internal projects are not open for public join requests.";
  } else if (latestRequest?.status === "pending") {
    title = "Request pending";
    message = "Your request is waiting for an assigned leader to review.";
  } else if (latestRequest?.status === "approved") {
    title = "Request approved";
    message = "You have been approved for this project.";
  } else if (latestRequest?.status === "rejected") {
    title = "Previous request rejected";
    message = "You may submit a new request with updated details.";
  } else if (latestRequest?.status === "cancelled") {
    title = "Previous request cancelled";
    message = "You may submit a new request when you are ready.";
  }

  return (
    <div className="rounded-xl border border-hairline bg-surface-elev p-5">
      <div className="flex items-center justify-between gap-3">
        <div className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Participation</div>
        {latestRequest ? (
          <Chip tone={publicRequestTone(latestRequest.status)}>{latestRequest.status}</Chip>
        ) : null}
      </div>
      <div className="mt-3 text-sm font-medium text-ink">{title}</div>
      <p className="mt-1 text-xs leading-relaxed text-ink-soft">{message}</p>
      {latestRequest?.status === "rejected" && latestRequest.reviewNote ? (
        <div className="mt-3 rounded-lg border border-hairline bg-muted/40 p-3">
          <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
            Rejection reason
          </div>
          <p className="mt-1 text-xs text-ink">{latestRequest.reviewNote}</p>
        </div>
      ) : null}
      {error ? (
        <div className="mt-3 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}
      <div className="mt-4 flex flex-wrap gap-2">
        {!signedIn ? (
          <Link
            to="/auth"
            className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
          >
            Sign in
          </Link>
        ) : latestRequest?.status === "pending" ? (
          <button
            onClick={onCancel}
            className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
          >
            Cancel request
          </button>
        ) : (
          <button
            disabled={blocked}
            onClick={onOpen}
            className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Request to join
          </button>
        )}
      </div>
    </div>
  );
}

function publicRequestTone(status: "pending" | "approved" | "rejected" | "cancelled") {
  if (status === "approved") return "emerald";
  if (status === "pending" || status === "rejected") return "amber";
  return "neutral";
}
