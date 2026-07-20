import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatDate, getUserName, useProjects } from "@/lib/projects-data";
import {
  joinRequestTone,
  useJoinRequests,
  type ProjectJoinRequest,
} from "@/lib/join-requests-data";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { ExternalLink } from "lucide-react";

export const Route = createFileRoute("/app/join-requests")({
  head: () => ({
    meta: [{ title: "Join requests — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: JoinRequestsPage,
});

function JoinRequestsPage() {
  const { user, activeRole } = useAuth();
  const { projects } = useProjects();
  const { requests, cancel } = useJoinRequests();
  const [error, setError] = useState<string | null>(null);

  if (!user || !activeRole) return null;

  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const visible = isAdmin ? requests : requests.filter((request) => request.userId === user.id);

  return (
    <>
      <PageHeader
        eyebrow={isAdmin ? "System oversight" : "Member workspace"}
        title="Join requests"
        description={
          isAdmin
            ? "Review the status of project participation requests across the lab. Leader approval actions stay inside assigned project workspaces."
            : "Track your project participation requests and cancel pending submissions."
        }
      />

      <Panel
        title={isAdmin ? "All requests" : "Your request history"}
        description={`${visible.length} request${visible.length === 1 ? "" : "s"}`}
        className="overflow-hidden"
      >
        {error ? (
          <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
            {error}
          </div>
        ) : null}

        {visible.length === 0 ? (
          <EmptyState
            title={isAdmin ? "No requests submitted" : "No join requests yet"}
            hint={
              isAdmin
                ? "Submitted member requests will appear here for oversight."
                : "Open a public project page to request participation."
            }
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[880px] text-sm">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Project</th>
                  {isAdmin ? <th className="px-3 py-3 font-medium">Applicant</th> : null}
                  <th className="px-3 py-3 font-medium">Desired role</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Submitted</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((request) => {
                  const project = projects.find((p) => p.id === request.projectId);
                  return (
                    <tr
                      key={request.id}
                      className="border-t border-hairline align-top transition-colors hover:bg-muted/40"
                    >
                      <td className="px-5 py-3">
                        <div className="font-medium text-ink">
                          {project?.name ?? "Project not found"}
                        </div>
                        <div className="mt-0.5 font-mono text-[11px] text-ink-soft">
                          {project?.code ?? request.projectId}
                        </div>
                        {request.status === "rejected" && request.reviewNote ? (
                          <p className="mt-2 max-w-md text-xs text-ink-soft">
                            Rejection reason: <span className="text-ink">{request.reviewNote}</span>
                          </p>
                        ) : null}
                      </td>
                      {isAdmin ? (
                        <td className="px-3 py-3 text-xs text-ink-soft">
                          {getUserName(request.userId)}
                        </td>
                      ) : null}
                      <td className="px-3 py-3 text-xs text-ink-soft">{request.desiredPosition}</td>
                      <td className="px-3 py-3">
                        <StatusPill tone={joinRequestTone(request.status)}>
                          {request.status}
                        </StatusPill>
                      </td>
                      <td className="px-3 py-3 text-xs text-ink-soft">
                        {formatDate(request.submittedAt)}
                      </td>
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
                          {!isAdmin && request.status === "pending" ? (
                            <button
                              onClick={() => {
                                const result = cancel(request.id, user.id);
                                setError(result.ok ? null : result.error);
                              }}
                              className="rounded-md border border-hairline px-2 py-1 text-xs text-ink hover:bg-muted"
                            >
                              Cancel
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </>
  );
}
