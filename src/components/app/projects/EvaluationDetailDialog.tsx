import { getUserName, formatDate } from "@/lib/projects-data";
import { scoreKeys, scoreLabels, scoreTone, type MemberEvaluation } from "@/lib/evaluations-data";
import { StatusPill } from "@/components/app/ui";
import { X } from "lucide-react";

export function EvaluationDetailDialog({
  evaluation,
  projectName,
  onClose,
}: {
  evaluation: MemberEvaluation | null;
  projectName: string;
  onClose: () => void;
}) {
  if (!evaluation) return null;
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="mt-10 w-full max-w-3xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Evaluation detail
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">
              {getUserName(evaluation.memberId)} · {evaluation.periodLabel}
            </h2>
            <div className="mt-1 text-xs text-ink-soft">
              {projectName} · {formatDate(evaluation.evaluationDate)}
            </div>
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

        <div className="space-y-5 p-5">
          <div className="flex flex-wrap items-center gap-2">
            <StatusPill tone={scoreTone(evaluation.overallScore)}>
              Overall {evaluation.overallScore.toFixed(1)} / 5
            </StatusPill>
            <span className="text-xs text-ink-soft">
              Evaluated by {getUserName(evaluation.evaluatorId)}
            </span>
          </div>

          <div className="grid gap-2 md:grid-cols-2">
            {scoreKeys.map((key) => (
              <div key={key} className="rounded-lg border border-hairline p-3">
                <div className="flex items-center justify-between gap-3">
                  <div className="text-sm font-medium text-ink">{scoreLabels[key]}</div>
                  <StatusPill tone={scoreTone(evaluation.scores[key])}>
                    {evaluation.scores[key]} / 5
                  </StatusPill>
                </div>
              </div>
            ))}
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <FeedbackBlock label="Strengths" value={evaluation.strengths} />
            <FeedbackBlock label="Weaknesses" value={evaluation.weaknesses || "Not provided"} />
            <FeedbackBlock
              label="Suggested improvements"
              value={evaluation.suggestedImprovements}
            />
            <FeedbackBlock
              label="Technical skills observed"
              value={evaluation.technicalSkillsObserved}
            />
            <FeedbackBlock
              label="Next learning direction"
              value={evaluation.nextLearningDirection || "Not provided"}
            />
            <FeedbackBlock
              label="General feedback"
              value={evaluation.generalFeedback || "Not provided"}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function FeedbackBlock({ label, value }: { label: string; value: string }) {
  return (
    <section className="rounded-lg border border-hairline p-3">
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <p className="mt-2 text-sm leading-relaxed text-ink">{value}</p>
    </section>
  );
}
