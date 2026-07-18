import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatDate, getUserName, useProjects } from "@/lib/projects-data";
import { scoreTone, useEvaluations, type MemberEvaluation } from "@/lib/evaluations-data";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { EvaluationDetailDialog } from "@/components/app/projects/EvaluationDetailDialog";
import { ExternalLink, Eye } from "lucide-react";

export const Route = createFileRoute("/app/evaluations")({
  head: () => ({
    meta: [{ title: "Evaluations — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: EvaluationsPage,
});

function EvaluationsPage() {
  const { user, activeRole } = useAuth();
  const { projects } = useProjects();
  const { evaluations } = useEvaluations();
  const [viewing, setViewing] = useState<MemberEvaluation | null>(null);

  if (!user || !activeRole) return null;

  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const visible = isAdmin
    ? evaluations
    : evaluations.filter((evaluation) => evaluation.memberId === user.id);
  const projectName = viewing
    ? (projects.find((project) => project.id === viewing.projectId)?.name ?? "Project not found")
    : "";

  return (
    <>
      <PageHeader
        eyebrow={isAdmin ? "System oversight" : "Member workspace"}
        title="Evaluations"
        description={
          isAdmin
            ? "Read-only view of member evaluations across projects."
            : "Your evaluation history and technical feedback across lab projects."
        }
      />

      <Panel
        title={isAdmin ? "All evaluations" : "Your evaluation history"}
        description={`${visible.length} evaluation${visible.length === 1 ? "" : "s"}`}
      >
        {visible.length === 0 ? (
          <EmptyState
            title={isAdmin ? "No evaluations recorded" : "No evaluations yet"}
            hint={
              isAdmin
                ? "Leader-created evaluations will appear here."
                : "Your project leaders' evaluations will appear here after they are saved."
            }
          />
        ) : (
          <div className="grid gap-3">
            {visible.map((evaluation) => {
              const project = projects.find((item) => item.id === evaluation.projectId);
              return (
                <article key={evaluation.id} className="rounded-lg border border-hairline p-4">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="text-sm font-semibold text-ink">
                          {project?.name ?? "Project not found"}
                        </h2>
                        <StatusPill tone={scoreTone(evaluation.overallScore)}>
                          Overall {evaluation.overallScore.toFixed(1)} / 5
                        </StatusPill>
                      </div>
                      <div className="mt-1 text-xs text-ink-soft">
                        {evaluation.periodLabel} · {formatDate(evaluation.evaluationDate)}
                        {isAdmin ? ` · ${getUserName(evaluation.memberId)}` : ""}
                      </div>
                      <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-ink-soft">
                        {evaluation.technicalSkillsObserved}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-1.5">
                      {project ? (
                        <Link
                          to="/app/projects/$slug"
                          params={{ slug: project.slug }}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Project
                        </Link>
                      ) : null}
                      <button
                        onClick={() => setViewing(evaluation)}
                        className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                      >
                        <Eye className="h-3.5 w-3.5" /> View
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </Panel>

      <EvaluationDetailDialog
        evaluation={viewing}
        projectName={projectName}
        onClose={() => setViewing(null)}
      />
    </>
  );
}
