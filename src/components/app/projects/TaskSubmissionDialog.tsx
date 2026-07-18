import { useMemo, useState } from "react";
import type { ProjectTask, TaskAssignment } from "@/lib/tasks-data";
import { X } from "lucide-react";

export function TaskSubmissionDialog({
  task,
  assignment,
  open,
  submitting,
  error,
  onClose,
  onSubmit,
}: {
  task: ProjectTask | null;
  assignment: TaskAssignment | null;
  open: boolean;
  submitting: boolean;
  error?: string | null;
  onClose: () => void;
  onSubmit: (payload: { summary: string; artifactUrl?: string; notes?: string }) => void;
}) {
  const [summary, setSummary] = useState("");
  const [artifactUrl, setArtifactUrl] = useState("");
  const [notes, setNotes] = useState("");
  const [touched, setTouched] = useState(false);

  const errors = useMemo(() => {
    const next: { summary?: string; artifactUrl?: string } = {};
    if (!summary.trim()) next.summary = "Submission summary is required.";
    if (artifactUrl.trim() && !isValidUrl(artifactUrl.trim())) {
      next.artifactUrl = "External artifact URL must be valid.";
    }
    return next;
  }, [summary, artifactUrl]);
  const valid = Object.keys(errors).length === 0;

  if (!open || !task || !assignment) return null;

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!valid || submitting) return;
    onSubmit({
      summary: summary.trim(),
      artifactUrl: artifactUrl.trim() || undefined,
      notes: notes.trim() || undefined,
    });
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={submitting ? undefined : onClose}
    >
      <form
        onSubmit={submit}
        onMouseDown={(event) => event.stopPropagation()}
        className="mt-10 w-full max-w-xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Submit output
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{task.title}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <div className="space-y-4 p-5">
          {assignment.reviewNote ? (
            <div className="rounded-lg border border-hairline bg-muted/40 p-3 text-xs">
              <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                Previous feedback
              </div>
              <p className="mt-1 text-ink">{assignment.reviewNote}</p>
            </div>
          ) : null}

          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Summary
            </div>
            <textarea
              value={summary}
              onBlur={() => setTouched(true)}
              onChange={(event) => setSummary(event.target.value)}
              className="min-h-[110px] w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
            {touched && errors.summary ? (
              <div className="mt-1 text-xs text-[color:var(--destructive)]">{errors.summary}</div>
            ) : null}
          </label>

          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              External artifact URL
            </div>
            <input
              value={artifactUrl}
              onBlur={() => setTouched(true)}
              onChange={(event) => setArtifactUrl(event.target.value)}
              placeholder="https://..."
              className="w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
            {touched && errors.artifactUrl ? (
              <div className="mt-1 text-xs text-[color:var(--destructive)]">
                {errors.artifactUrl}
              </div>
            ) : null}
          </label>

          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">Notes</div>
            <textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              className="min-h-[72px] w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
          </label>

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
            disabled={submitting}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!valid || submitting}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {submitting ? "Submitting..." : "Submit output"}
          </button>
        </footer>
      </form>
    </div>
  );
}

function isValidUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}
