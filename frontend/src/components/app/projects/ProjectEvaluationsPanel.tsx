import { useState } from "react";
import { type Role } from "@/lib/auth";
import { formatDate, getUserName, type Project } from "@/lib/projects-data";
import {
  scoreTone,
  useEvaluations,
  type EvaluationDraft,
  type MemberEvaluation,
} from "@/lib/evaluations-data";
import { EmptyState, Panel, StatusPill } from "@/components/app/ui";
import { EvaluationFormDialog } from "./EvaluationFormDialog";
import { EvaluationDetailDialog } from "./EvaluationDetailDialog";
import { Eye, Pencil } from "lucide-react";
import { notifyOnce } from "@/lib/notifications-data";
import { getUserById, useUsers, type UserAccount } from "@/lib/users-data";

export function ProjectEvaluationsPanel({
  project,
  user,
  activeRole,
}: {
  project: Project;
  user: UserAccount;
  activeRole: Role;
}) {
  const { evaluations, upsertEvaluation } = useEvaluations();
  const { users } = useUsers();
  const [editing, setEditing] = useState<MemberEvaluation | null>(null);
  const [creating, setCreating] = useState(false);
  const [viewing, setViewing] = useState<MemberEvaluation | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAssignedLeader =
    activeRole === "leader" && user.roles.includes("leader") && project.leaderIds.includes(user.id);
  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const projectEvaluations = evaluations.filter((item) => item.projectId === project.id);
  const visibleEvaluations =
    isAssignedLeader || isAdmin
      ? projectEvaluations
      : projectEvaluations.filter((item) => item.memberId === user.id);
  const memberOptions = users.filter(
    (member) => project.memberIds.includes(member.id) && member.status === "active",
  );
  const canCreate = isAssignedLeader && memberOptions.some((member) => member.id !== user.id);

  const saveEvaluation = (draft: EvaluationDraft, existingId?: string) => {
    if (!isAssignedLeader) {
      setError("Only assigned project leaders can save evaluations.");
      return;
    }
    if (!project.memberIds.includes(draft.memberId)) {
      setError("The evaluated user must be a current project member.");
      return;
    }
    const targetAccount = getUserById(draft.memberId);
    if (!targetAccount) {
      setError("Member not found.");
      return;
    }
    if (targetAccount.status === "locked") {
      setError("Locked accounts cannot receive new evaluations.");
      return;
    }
    if (draft.memberId === user.id) {
      setError("Leaders cannot evaluate themselves.");
      return;
    }
    const alreadyExists = projectEvaluations.some(
      (evaluation) =>
        evaluation.id === existingId ||
        (evaluation.memberId === draft.memberId &&
          evaluation.periodLabel.trim().toLowerCase() === draft.periodLabel.trim().toLowerCase()),
    );
    setSaving(true);
    const result = upsertEvaluation(project.id, user.id, draft, existingId);
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    if (!alreadyExists) {
      notifyOnce({
        userId: result.value.memberId,
        type: "evaluation_received",
        title: "New evaluation available",
        message: `A new evaluation for ${project.name} is available in your workspace.`,
        link: "/app/evaluations",
        eventKey: `evaluation:${result.value.id}:received:${result.value.memberId}`,
      });
    }
    setCreating(false);
    setEditing(null);
    setError(null);
  };

  return (
    <>
      <Panel
        title="Evaluations"
        description={
          isAssignedLeader
            ? `${projectEvaluations.length} evaluation${projectEvaluations.length === 1 ? "" : "s"} recorded`
            : isAdmin
              ? "Read-only evaluation oversight"
              : `${visibleEvaluations.length} evaluation${visibleEvaluations.length === 1 ? "" : "s"} visible to you`
        }
        action={
          isAssignedLeader ? (
            <button
              disabled={!canCreate}
              onClick={() => {
                setCreating(true);
                setEditing(null);
                setError(null);
              }}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
            >
              New evaluation
            </button>
          ) : undefined
        }
      >
        {error ? (
          <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
            {error}
          </div>
        ) : null}

        {project.memberIds.length === 0 && isAssignedLeader ? (
          <EmptyState
            title="No project members"
            hint="Add members to the project before creating evaluations."
          />
        ) : visibleEvaluations.length === 0 ? (
          <EmptyState
            title="No evaluations yet"
            hint={
              isAssignedLeader
                ? "Create an evaluation for a project member and period."
                : "Your evaluation history for this project will appear here."
            }
          />
        ) : (
          <div className="grid gap-3">
            {visibleEvaluations.map((evaluation) => (
              <article key={evaluation.id} className="rounded-lg border border-hairline p-4">
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-sm font-semibold text-ink">
                        {getUserName(evaluation.memberId)}
                      </h3>
                      <StatusPill tone={scoreTone(evaluation.overallScore)}>
                        Overall {evaluation.overallScore.toFixed(1)} / 5
                      </StatusPill>
                    </div>
                    <div className="mt-1 text-xs text-ink-soft">
                      {evaluation.periodLabel} · {formatDate(evaluation.evaluationDate)} · Evaluated
                      by {getUserName(evaluation.evaluatorId)}
                    </div>
                    <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-ink-soft">
                      {evaluation.technicalSkillsObserved}
                    </p>
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-1.5">
                    <button
                      onClick={() => setViewing(evaluation)}
                      className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                    >
                      <Eye className="h-3.5 w-3.5" /> View
                    </button>
                    {isAssignedLeader ? (
                      <button
                        onClick={() => {
                          setEditing(evaluation);
                          setCreating(false);
                          setError(null);
                        }}
                        className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                      >
                        <Pencil className="h-3.5 w-3.5" /> Edit
                      </button>
                    ) : isAdmin ? (
                      <span className="rounded-md border border-hairline px-2.5 py-1 text-[11px] text-ink-soft">
                        Read only
                      </span>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>

      <EvaluationFormDialog
        project={project}
        existingEvaluations={projectEvaluations}
        editing={editing}
        evaluatorId={user.id}
        open={creating || !!editing}
        saving={saving}
        error={error}
        onClose={() => {
          if (!saving) {
            setCreating(false);
            setEditing(null);
            setError(null);
          }
        }}
        onSave={saveEvaluation}
      />

      <EvaluationDetailDialog
        evaluation={viewing}
        projectName={project.name}
        onClose={() => setViewing(null)}
      />
    </>
  );
}
