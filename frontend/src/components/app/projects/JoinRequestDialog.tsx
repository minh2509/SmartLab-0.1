import { useEffect, useMemo, useState } from "react";
import type { JoinRequestDraft } from "@/lib/join-requests-data";
import { X } from "lucide-react";

const initialForm: JoinRequestDraft = {
  reason: "",
  skills: "",
  experience: "",
  desiredPosition: "",
  introduction: "",
};

const INTRO_MAX = 500;
const REASON_MIN = 20;

export function JoinRequestDialog({
  projectName,
  open,
  submitting,
  error,
  onClose,
  onSubmit,
}: {
  projectName: string;
  open: boolean;
  submitting: boolean;
  error?: string | null;
  onClose: () => void;
  onSubmit: (draft: JoinRequestDraft) => void;
}) {
  const [form, setForm] = useState(initialForm);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (open) {
      setForm(initialForm);
      setTouched({});
    }
  }, [open]);

  const errors = useMemo(() => validate(form), [form]);
  const valid = Object.keys(errors).length === 0;

  if (!open) return null;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setTouched({
      reason: true,
      skills: true,
      desiredPosition: true,
      introduction: true,
    });
    if (!valid || submitting) return;
    onSubmit({
      reason: form.reason.trim(),
      skills: form.skills.trim(),
      experience: form.experience.trim(),
      desiredPosition: form.desiredPosition.trim(),
      introduction: form.introduction.trim(),
    });
  };

  const update = (key: keyof JoinRequestDraft, value: string) => {
    setForm((current) => ({ ...current, [key]: value }));
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
        onMouseDown={(e) => e.stopPropagation()}
        className="mt-10 w-full max-w-2xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Request to join
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{projectName}</h2>
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

        <div className="grid gap-4 p-5">
          <Field label="Reason for joining" error={touched.reason ? errors.reason : undefined}>
            <textarea
              className="join-input min-h-[92px]"
              value={form.reason}
              onBlur={() => setTouched((v) => ({ ...v, reason: true }))}
              onChange={(e) => update("reason", e.target.value)}
              placeholder="Tell the leaders why this project fits your research goals."
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Relevant skills" error={touched.skills ? errors.skills : undefined}>
              <input
                className="join-input"
                value={form.skills}
                onBlur={() => setTouched((v) => ({ ...v, skills: true }))}
                onChange={(e) => update("skills", e.target.value)}
                placeholder="Python, Java, LLM evaluation..."
              />
            </Field>
            <Field
              label="Desired role or position"
              error={touched.desiredPosition ? errors.desiredPosition : undefined}
            >
              <input
                className="join-input"
                value={form.desiredPosition}
                onBlur={() => setTouched((v) => ({ ...v, desiredPosition: true }))}
                onChange={(e) => update("desiredPosition", e.target.value)}
                placeholder="Research assistant"
              />
            </Field>
          </div>

          <Field label="Previous experience">
            <textarea
              className="join-input min-h-[76px]"
              value={form.experience}
              onChange={(e) => update("experience", e.target.value)}
              placeholder="Mention courses, labs, papers, products, or projects."
            />
          </Field>

          <Field
            label={`Short self-introduction (${form.introduction.length}/${INTRO_MAX})`}
            error={touched.introduction ? errors.introduction : undefined}
          >
            <textarea
              className="join-input min-h-[92px]"
              value={form.introduction}
              onBlur={() => setTouched((v) => ({ ...v, introduction: true }))}
              onChange={(e) => update("introduction", e.target.value)}
              placeholder="A concise introduction for the project leaders."
            />
          </Field>

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
            {submitting ? "Submitting..." : "Submit request"}
          </button>
        </footer>
      </form>

      <style>{`
        .join-input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 13px;
          color: inherit;
        }
        .join-input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
}

function validate(form: JoinRequestDraft) {
  const errors: Partial<Record<keyof JoinRequestDraft, string>> = {};
  if (form.reason.trim().length < REASON_MIN) {
    errors.reason = `Reason must be at least ${REASON_MIN} characters.`;
  }
  if (!form.skills.trim()) errors.skills = "Relevant skills are required.";
  if (!form.desiredPosition.trim()) {
    errors.desiredPosition = "Desired role or position is required.";
  }
  if (form.introduction.trim().length > INTRO_MAX) {
    errors.introduction = `Introduction must be ${INTRO_MAX} characters or fewer.`;
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
