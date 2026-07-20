import { useEffect, useMemo, useState } from "react";
import type { Project } from "@/lib/projects-data";
import {
  fromDateTimeLocalValue,
  toDateTimeLocalValue,
  validateCalendarDraft,
  type CalendarEventDraft,
  type CalendarEventScope,
  type LabCalendarEvent,
} from "@/lib/calendar-data";
import { X } from "lucide-react";

function emptyDraft(scope: CalendarEventScope, projectId?: string): CalendarEventDraft {
  return {
    scope,
    projectId: scope === "project" ? projectId : undefined,
    title: "",
    description: "",
    location: "",
    startAt: "",
    endAt: "",
  };
}

function toDraft(
  event: LabCalendarEvent | null,
  fallbackScope: CalendarEventScope,
  projectId?: string,
) {
  if (!event) return emptyDraft(fallbackScope, projectId);
  return {
    scope: event.scope,
    projectId: event.projectId,
    title: event.title,
    description: event.description,
    location: event.location ?? "",
    startAt: toDateTimeLocalValue(event.startAt),
    endAt: toDateTimeLocalValue(event.endAt),
  };
}

export function CalendarEventDialog({
  event,
  open,
  saving,
  error,
  mode,
  manageableProjects,
  canCreateLab,
  onClose,
  onSave,
}: {
  event: LabCalendarEvent | null;
  open: boolean;
  saving: boolean;
  error?: string | null;
  mode: "admin" | "leader";
  manageableProjects: Project[];
  canCreateLab: boolean;
  onClose: () => void;
  onSave: (draft: CalendarEventDraft) => void;
}) {
  const fallbackScope: CalendarEventScope = canCreateLab ? "lab" : "project";
  const [form, setForm] = useState<CalendarEventDraft>(() =>
    toDraft(event, fallbackScope, manageableProjects[0]?.id),
  );
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setForm(toDraft(event, fallbackScope, manageableProjects[0]?.id));
    setTouched({});
  }, [event, open, fallbackScope, manageableProjects]);

  const normalizedForm = useMemo<CalendarEventDraft>(
    () => ({
      ...form,
      projectId: form.scope === "project" ? form.projectId : undefined,
      startAt: fromDateTimeLocalValue(form.startAt),
      endAt: fromDateTimeLocalValue(form.endAt),
    }),
    [form],
  );
  const errors = useMemo(() => validateCalendarDraft(normalizedForm), [normalizedForm]);
  const valid = Object.keys(errors).length === 0;

  if (!open) return null;

  const submit = (submitEvent: React.FormEvent) => {
    submitEvent.preventDefault();
    setTouched({
      title: true,
      description: true,
      projectId: true,
      startAt: true,
      endAt: true,
    });
    if (!valid || saving) return;
    onSave({
      ...normalizedForm,
      title: normalizedForm.title.trim(),
      description: normalizedForm.description.trim(),
      location: normalizedForm.location?.trim() || undefined,
    });
  };

  const projectEventUnavailable = form.scope === "project" && manageableProjects.length === 0;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={saving ? undefined : onClose}
    >
      <form
        onSubmit={submit}
        onMouseDown={(mouseEvent) => mouseEvent.stopPropagation()}
        className="mt-10 w-full max-w-2xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {event ? "Edit calendar event" : "Create calendar event"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">
              {mode === "admin" ? "Lab calendar" : "Project calendar"}
            </h2>
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

        <div className="grid gap-4 p-5">
          {canCreateLab && !event ? (
            <Field label="Scope">
              <select
                className="calendar-input"
                value={form.scope}
                onChange={(changeEvent) => {
                  const scope = changeEvent.target.value as CalendarEventScope;
                  setForm({
                    ...form,
                    scope,
                    projectId: scope === "project" ? manageableProjects[0]?.id : undefined,
                  });
                }}
              >
                <option value="lab">Lab-wide</option>
                <option value="project" disabled={manageableProjects.length === 0}>
                  Project-specific
                </option>
              </select>
            </Field>
          ) : null}

          {form.scope === "project" ? (
            <Field label="Project" error={touched.projectId ? errors.projectId : undefined}>
              {manageableProjects.length === 0 ? (
                <div className="rounded-lg border border-dashed border-hairline p-4 text-xs text-ink-soft">
                  No relevant project is available for project events.
                </div>
              ) : (
                <select
                  className="calendar-input"
                  value={form.projectId ?? ""}
                  disabled={!!event}
                  onBlur={() => setTouched((current) => ({ ...current, projectId: true }))}
                  onChange={(changeEvent) =>
                    setForm({ ...form, projectId: changeEvent.target.value })
                  }
                >
                  {manageableProjects.map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          ) : null}

          <Field label="Title" error={touched.title ? errors.title : undefined}>
            <input
              className="calendar-input"
              value={form.title}
              onBlur={() => setTouched((current) => ({ ...current, title: true }))}
              onChange={(changeEvent) => setForm({ ...form, title: changeEvent.target.value })}
              placeholder="Research forum, sprint review, field study..."
            />
          </Field>

          <Field label="Description" error={touched.description ? errors.description : undefined}>
            <textarea
              className="calendar-input min-h-[96px]"
              value={form.description}
              onBlur={() => setTouched((current) => ({ ...current, description: true }))}
              onChange={(changeEvent) =>
                setForm({ ...form, description: changeEvent.target.value })
              }
              placeholder="Purpose, audience, and expected preparation."
            />
          </Field>

          <Field label="Location">
            <input
              className="calendar-input"
              value={form.location ?? ""}
              onChange={(changeEvent) => setForm({ ...form, location: changeEvent.target.value })}
              placeholder="Seminar room, robotics lab, online..."
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Start" error={touched.startAt ? errors.startAt : undefined}>
              <input
                type="datetime-local"
                className="calendar-input"
                value={form.startAt}
                onBlur={() => setTouched((current) => ({ ...current, startAt: true }))}
                onChange={(changeEvent) => setForm({ ...form, startAt: changeEvent.target.value })}
              />
            </Field>
            <Field label="End" error={touched.endAt ? errors.endAt : undefined}>
              <input
                type="datetime-local"
                className="calendar-input"
                value={form.endAt}
                onBlur={() => setTouched((current) => ({ ...current, endAt: true }))}
                onChange={(changeEvent) => setForm({ ...form, endAt: changeEvent.target.value })}
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
            disabled={!valid || saving || projectEventUnavailable}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {saving ? "Saving..." : "Save event"}
          </button>
        </footer>
      </form>

      <style>{`
        .calendar-input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 13px;
          color: inherit;
        }
        .calendar-input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
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
