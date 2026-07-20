import { useState } from "react";
import { getUserName } from "@/lib/projects-data";
import type { ProjectTask, TaskAssignment, TaskSubmission } from "@/lib/tasks-data";
import { X } from "lucide-react";

export function TaskReviewDialog({
  task,
  assignment,
  submission,
  mode,
  error,
  onClose,
  onSubmit,
}: {
  task: ProjectTask | null;
  assignment: TaskAssignment | null;
  submission: TaskSubmission | null;
  mode: "accept" | "changes";
  error?: string | null;
  onClose: () => void;
  onSubmit: (note: string) => void;
}) {
  const [note, setNote] = useState("");
  if (!task || !assignment || !submission) return null;
  const needsNote = mode === "changes" && !note.trim();

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {mode === "accept" ? "Accept output" : "Request changes"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{task.title}</h2>
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
          <div className="rounded-lg border border-hairline bg-muted/30 p-3 text-xs">
            <div className="text-ink-soft">
              Submitted by{" "}
              <span className="font-medium text-ink">{getUserName(assignment.memberId)}</span>
            </div>
            <p className="mt-2 leading-relaxed text-ink">{submission.summary}</p>
            {submission.artifactUrl ? (
              <a
                href={submission.artifactUrl}
                target="_blank"
                rel="noreferrer"
                className="mt-2 inline-flex text-xs text-ink underline-offset-4 hover:underline"
              >
                Open external artifact
              </a>
            ) : null}
            {submission.notes ? (
              <p className="mt-2 leading-relaxed text-ink-soft">{submission.notes}</p>
            ) : null}
          </div>

          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {mode === "accept" ? "Review note (optional)" : "Feedback"}
            </div>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              className="min-h-[96px] w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
            {needsNote ? (
              <div className="mt-1 text-xs text-[color:var(--destructive)]">
                Feedback is required when requesting changes.
              </div>
            ) : null}
          </label>

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
            disabled={needsNote}
            onClick={() => onSubmit(note)}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {mode === "accept" ? "Accept output" : "Request changes"}
          </button>
        </footer>
      </div>
    </div>
  );
}
