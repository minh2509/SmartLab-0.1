import { useEffect, useMemo, useState } from "react";
import { getUserName, type Project } from "@/lib/projects-data";
import { useUsers } from "@/lib/users-data";
import {
  calculateOverallScore,
  emptyScores,
  findPeriodEvaluation,
  scoreKeys,
  scoreLabels,
  type EvaluationDraft,
  type MemberEvaluation,
} from "@/lib/evaluations-data";
import { cn } from "@/lib/utils";
import { X } from "lucide-react";

function emptyDraft(project: Project): EvaluationDraft {
  return {
    memberId: project.memberIds[0] ?? "",
    periodLabel: "",
    evaluationDate: new Date().toISOString().slice(0, 10),
    scores: emptyScores(),
    strengths: "",
    weaknesses: "",
    suggestedImprovements: "",
    technicalSkillsObserved: "",
    nextLearningDirection: "",
    generalFeedback: "",
  };
}

function toDraft(project: Project, evaluation: MemberEvaluation | null): EvaluationDraft {
  if (!evaluation) return emptyDraft(project);
  return {
    memberId: evaluation.memberId,
    periodLabel: evaluation.periodLabel,
    evaluationDate: evaluation.evaluationDate,
    scores: evaluation.scores,
    strengths: evaluation.strengths,
    weaknesses: evaluation.weaknesses,
    suggestedImprovements: evaluation.suggestedImprovements,
    technicalSkillsObserved: evaluation.technicalSkillsObserved,
    nextLearningDirection: evaluation.nextLearningDirection,
    generalFeedback: evaluation.generalFeedback,
  };
}

export function EvaluationFormDialog({
  project,
  existingEvaluations,
  editing,
  evaluatorId,
  open,
  saving,
  error,
  onClose,
  onSave,
}: {
  project: Project;
  existingEvaluations: MemberEvaluation[];
  editing: MemberEvaluation | null;
  evaluatorId: string;
  open: boolean;
  saving: boolean;
  error?: string | null;
  onClose: () => void;
  onSave: (draft: EvaluationDraft, existingId?: string) => void;
}) {
  const [form, setForm] = useState<EvaluationDraft>(() => toDraft(project, editing));
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const { users } = useUsers();

  useEffect(() => {
    setForm(toDraft(project, editing));
    setTouched({});
  }, [project, editing, open]);

  const memberOptions = users.filter(
    (user) =>
      project.memberIds.includes(user.id) &&
      user.id !== evaluatorId &&
      (user.status === "active" || user.id === editing?.memberId),
  );
  const periodMatch = findPeriodEvaluation(
    existingEvaluations,
    project.id,
    form.memberId,
    form.periodLabel,
  );
  const duplicate = periodMatch && periodMatch.id !== editing?.id ? periodMatch : null;
  const errors = useMemo(
    () => validate(form, project.memberIds, evaluatorId),
    [form, project.memberIds, evaluatorId],
  );
  const valid = Object.keys(errors).length === 0;
  const overall = calculateOverallScore(form.scores);

  if (!open) return null;

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setTouched({
      memberId: true,
      periodLabel: true,
      evaluationDate: true,
      strengths: true,
      suggestedImprovements: true,
      technicalSkillsObserved: true,
      scores: true,
    });
    if (!valid || saving) return;
    onSave(
      {
        ...form,
        periodLabel: form.periodLabel.trim(),
        strengths: form.strengths.trim(),
        weaknesses: form.weaknesses.trim(),
        suggestedImprovements: form.suggestedImprovements.trim(),
        technicalSkillsObserved: form.technicalSkillsObserved.trim(),
        nextLearningDirection: form.nextLearningDirection.trim(),
        generalFeedback: form.generalFeedback.trim(),
      },
      editing?.id ?? duplicate?.id,
    );
  };

  const updateScore = (key: keyof EvaluationDraft["scores"], value: number) => {
    setForm((current) => ({
      ...current,
      scores: { ...current.scores, [key]: value },
    }));
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={saving ? undefined : onClose}
    >
      <form
        onSubmit={submit}
        onMouseDown={(event) => event.stopPropagation()}
        className="mt-10 w-full max-w-3xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {editing
                ? "Edit evaluation"
                : duplicate
                  ? "Edit existing period"
                  : "Create evaluation"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{project.name}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <div className="grid gap-5 p-5">
          <div className="grid gap-4 md:grid-cols-3">
            <Field label="Project member" error={touched.memberId ? errors.memberId : undefined}>
              <select
                className="eval-input"
                value={form.memberId}
                onBlur={() => setTouched((current) => ({ ...current, memberId: true }))}
                onChange={(event) => setForm({ ...form, memberId: event.target.value })}
              >
                {memberOptions.length === 0 ? <option value="">No members</option> : null}
                {memberOptions.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.fullName}
                    {member.status === "locked" ? " · Locked" : ""}
                  </option>
                ))}
              </select>
            </Field>
            <Field
              label="Evaluation period"
              error={touched.periodLabel ? errors.periodLabel : undefined}
            >
              <input
                className="eval-input"
                value={form.periodLabel}
                onBlur={() => setTouched((current) => ({ ...current, periodLabel: true }))}
                onChange={(event) => setForm({ ...form, periodLabel: event.target.value })}
                placeholder="Spring 2026"
              />
            </Field>
            <Field
              label="Evaluation date"
              error={touched.evaluationDate ? errors.evaluationDate : undefined}
            >
              <input
                type="date"
                className="eval-input"
                value={form.evaluationDate}
                onBlur={() => setTouched((current) => ({ ...current, evaluationDate: true }))}
                onChange={(event) => setForm({ ...form, evaluationDate: event.target.value })}
              />
            </Field>
          </div>

          {duplicate ? (
            <div className="rounded-md border border-hairline bg-muted/40 px-3 py-2 text-xs text-ink-soft">
              An evaluation already exists for {getUserName(form.memberId)} in this period. Saving
              will update that record instead of creating a duplicate.
            </div>
          ) : null}

          <div>
            <div className="mb-2 flex items-center justify-between gap-3">
              <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                Score breakdown
              </div>
              <div className="rounded-full border border-hairline px-2.5 py-1 text-xs text-ink">
                Overall {overall.toFixed(1)} / 5
              </div>
            </div>
            <div className="grid gap-2 md:grid-cols-2">
              {scoreKeys.map((key) => (
                <label key={key} className="rounded-lg border border-hairline p-3">
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-sm font-medium text-ink">{scoreLabels[key]}</span>
                    <select
                      value={form.scores[key]}
                      onChange={(event) => updateScore(key, Number(event.target.value))}
                      className="rounded-md border border-hairline bg-background px-2 py-1 text-sm text-ink"
                    >
                      {[1, 2, 3, 4, 5].map((score) => (
                        <option key={score} value={score}>
                          {score}
                        </option>
                      ))}
                    </select>
                  </div>
                </label>
              ))}
            </div>
            {touched.scores && errors.scores ? (
              <div className="mt-1 text-xs text-[color:var(--destructive)]">{errors.scores}</div>
            ) : null}
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Strengths" error={touched.strengths ? errors.strengths : undefined}>
              <textarea
                className="eval-input min-h-[92px]"
                value={form.strengths}
                onBlur={() => setTouched((current) => ({ ...current, strengths: true }))}
                onChange={(event) => setForm({ ...form, strengths: event.target.value })}
              />
            </Field>
            <Field label="Weaknesses">
              <textarea
                className="eval-input min-h-[92px]"
                value={form.weaknesses}
                onChange={(event) => setForm({ ...form, weaknesses: event.target.value })}
              />
            </Field>
            <Field
              label="Suggested improvements"
              error={touched.suggestedImprovements ? errors.suggestedImprovements : undefined}
            >
              <textarea
                className="eval-input min-h-[92px]"
                value={form.suggestedImprovements}
                onBlur={() =>
                  setTouched((current) => ({ ...current, suggestedImprovements: true }))
                }
                onChange={(event) =>
                  setForm({ ...form, suggestedImprovements: event.target.value })
                }
              />
            </Field>
            <Field
              label="Technical skills observed"
              error={touched.technicalSkillsObserved ? errors.technicalSkillsObserved : undefined}
            >
              <textarea
                className="eval-input min-h-[92px]"
                value={form.technicalSkillsObserved}
                onBlur={() =>
                  setTouched((current) => ({ ...current, technicalSkillsObserved: true }))
                }
                onChange={(event) =>
                  setForm({ ...form, technicalSkillsObserved: event.target.value })
                }
              />
            </Field>
            <Field label="Next learning direction">
              <textarea
                className="eval-input min-h-[80px]"
                value={form.nextLearningDirection}
                onChange={(event) =>
                  setForm({ ...form, nextLearningDirection: event.target.value })
                }
              />
            </Field>
            <Field label="General feedback">
              <textarea
                className="eval-input min-h-[80px]"
                value={form.generalFeedback}
                onChange={(event) => setForm({ ...form, generalFeedback: event.target.value })}
              />
            </Field>
          </div>

          {error ? (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {error}
            </div>
          ) : null}
        </div>

        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!valid || saving}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {saving ? "Saving..." : "Save evaluation"}
          </button>
        </footer>
      </form>

      <style>{`
        .eval-input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 13px;
          color: inherit;
        }
        .eval-input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
}

function validate(form: EvaluationDraft, projectMemberIds: string[], evaluatorId: string) {
  const errors: Partial<Record<keyof EvaluationDraft | "scores", string>> = {};
  if (!form.memberId) errors.memberId = "Project member is required.";
  if (form.memberId === evaluatorId) errors.memberId = "Leaders cannot evaluate themselves.";
  if (form.memberId && !projectMemberIds.includes(form.memberId)) {
    errors.memberId = "Selected user must be a current project member.";
  }
  if (!form.periodLabel.trim()) errors.periodLabel = "Evaluation period is required.";
  if (!form.evaluationDate) errors.evaluationDate = "Evaluation date is required.";
  if (
    scoreKeys.some(
      (key) => !Number.isInteger(form.scores[key]) || form.scores[key] < 1 || form.scores[key] > 5,
    )
  ) {
    errors.scores = "Every score must be an integer from 1 through 5.";
  }
  if (!form.strengths.trim()) errors.strengths = "Strengths are required.";
  if (!form.suggestedImprovements.trim()) {
    errors.suggestedImprovements = "Suggested improvements are required.";
  }
  if (!form.technicalSkillsObserved.trim()) {
    errors.technicalSkillsObserved = "Technical feedback is required.";
  }
  return errors;
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      {children}
      {error ? <div className="mt-1 text-xs text-[color:var(--destructive)]">{error}</div> : null}
    </label>
  );
}
