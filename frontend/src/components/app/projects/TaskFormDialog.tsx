import { useEffect, useMemo, useState } from "react";
import type { Project } from "@/lib/projects-data";
import type { ProjectTask, TaskDraft, TaskPriority } from "@/lib/tasks-data";
import { useUsers } from "@/lib/users-data";
import { cn } from "@/lib/utils";
import { X } from "lucide-react";

const priorities: TaskPriority[] = ["low", "medium", "high", "critical"];

function emptyDraft(): TaskDraft {
  return {
    title: "",
    description: "",
    outputCriteria: "",
    priority: "medium",
    startDate: "",
    dueDate: "",
    assigneeIds: [],
  };
}

function toDraft(task: ProjectTask | null): TaskDraft {
  if (!task) return emptyDraft();
  return {
    title: task.title,
    description: task.description,
    outputCriteria: task.outputCriteria,
    priority: task.priority,
    startDate: task.startDate,
    dueDate: task.dueDate,
    assigneeIds: task.assigneeIds,
  };
}

export function TaskFormDialog({
  project,
  task,
  open,
  error,
  onClose,
  onSave,
}: {
  project: Project;
  task: ProjectTask | null;
  open: boolean;
  error?: string | null;
  onClose: () => void;
  onSave: (draft: TaskDraft) => void;
}) {
  const [form, setForm] = useState<TaskDraft>(() => toDraft(task));
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const { users } = useUsers();

  useEffect(() => {
    setForm(toDraft(task));
    setTouched({});
  }, [task, open]);

  const memberOptions = users.filter((user) => project.memberIds.includes(user.id));
  const errors = useMemo(() => validate(form, project.memberIds), [form, project.memberIds]);
  const valid = Object.keys(errors).length === 0;

  if (!open) return null;

  const toggleAssignee = (id: string) => {
    setForm((current) => {
      const exists = current.assigneeIds.includes(id);
      return {
        ...current,
        assigneeIds: exists
          ? current.assigneeIds.filter((item) => item !== id)
          : [...current.assigneeIds, id],
      };
    });
  };

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setTouched({
      title: true,
      description: true,
      outputCriteria: true,
      startDate: true,
      dueDate: true,
      assigneeIds: true,
    });
    if (!valid) return;
    onSave({
      title: form.title.trim(),
      description: form.description.trim(),
      outputCriteria: form.outputCriteria.trim(),
      priority: form.priority,
      startDate: form.startDate,
      dueDate: form.dueDate,
      assigneeIds: Array.from(new Set(form.assigneeIds)),
    });
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <form
        onSubmit={submit}
        onMouseDown={(event) => event.stopPropagation()}
        className="mt-10 w-full max-w-2xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {task ? "Edit task" : "Create task"}
            </div>
            <div className="mt-0.5 text-sm font-semibold text-ink">{project.name}</div>
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

        <div className="grid gap-4 p-5">
          <Field label="Title" error={touched.title ? errors.title : undefined}>
            <input
              className="task-input"
              value={form.title}
              onBlur={() => setTouched((current) => ({ ...current, title: true }))}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
            />
          </Field>
          <Field label="Description" error={touched.description ? errors.description : undefined}>
            <textarea
              className="task-input min-h-[88px]"
              value={form.description}
              onBlur={() => setTouched((current) => ({ ...current, description: true }))}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
          </Field>
          <Field
            label="Output criteria"
            error={touched.outputCriteria ? errors.outputCriteria : undefined}
          >
            <textarea
              className="task-input min-h-[76px]"
              value={form.outputCriteria}
              onBlur={() => setTouched((current) => ({ ...current, outputCriteria: true }))}
              onChange={(event) => setForm({ ...form, outputCriteria: event.target.value })}
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Priority">
              <select
                className="task-input"
                value={form.priority}
                onChange={(event) =>
                  setForm({ ...form, priority: event.target.value as TaskPriority })
                }
              >
                {priorities.map((priority) => (
                  <option key={priority} value={priority}>
                    {priority}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Start date" error={touched.startDate ? errors.startDate : undefined}>
              <input
                type="date"
                className="task-input"
                value={form.startDate}
                onBlur={() => setTouched((current) => ({ ...current, startDate: true }))}
                onChange={(event) => setForm({ ...form, startDate: event.target.value })}
              />
            </Field>
            <Field label="Due date" error={touched.dueDate ? errors.dueDate : undefined}>
              <input
                type="date"
                className="task-input"
                value={form.dueDate}
                onBlur={() => setTouched((current) => ({ ...current, dueDate: true }))}
                onChange={(event) => setForm({ ...form, dueDate: event.target.value })}
              />
            </Field>
          </div>

          <Field label="Assignees" error={touched.assigneeIds ? errors.assigneeIds : undefined}>
            {memberOptions.length === 0 ? (
              <div className="rounded-lg border border-dashed border-hairline p-4 text-xs text-ink-soft">
                This project has no members to assign yet.
              </div>
            ) : (
              <div className="flex flex-wrap gap-1.5">
                {memberOptions.map((member) => {
                  const active = form.assigneeIds.includes(member.id);
                  const disabled = member.status === "locked";
                  return (
                    <button
                      key={member.id}
                      type="button"
                      disabled={disabled}
                      onClick={() => toggleAssignee(member.id)}
                      className={cn(
                        "rounded-full border px-2.5 py-1 text-xs transition-colors",
                        active
                          ? "border-ink bg-ink text-background"
                          : "border-hairline text-ink-soft hover:text-ink",
                        disabled && "cursor-not-allowed opacity-70 hover:text-ink-soft",
                      )}
                    >
                      {member.fullName}
                      {member.status === "locked" ? " · Locked" : ""}
                    </button>
                  );
                })}
              </div>
            )}
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
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!valid}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Save task
          </button>
        </footer>
      </form>

      <style>{`
        .task-input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 13px;
          color: inherit;
        }
        .task-input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
}

function validate(form: TaskDraft, projectMemberIds: string[]) {
  const errors: Partial<Record<keyof TaskDraft, string>> = {};
  if (!form.title.trim()) errors.title = "Title is required.";
  if (!form.description.trim()) errors.description = "Description is required.";
  if (!form.outputCriteria.trim()) errors.outputCriteria = "Output criteria are required.";
  if (!form.startDate) errors.startDate = "Start date is required.";
  if (!form.dueDate) errors.dueDate = "Due date is required.";
  if (form.startDate && form.dueDate && new Date(form.dueDate) < new Date(form.startDate)) {
    errors.dueDate = "Due date cannot be before start date.";
  }
  const uniqueAssignees = Array.from(new Set(form.assigneeIds));
  if (uniqueAssignees.length === 0) errors.assigneeIds = "At least one project member is required.";
  if (uniqueAssignees.some((id) => !projectMemberIds.includes(id))) {
    errors.assigneeIds = "Assignees must be current project members.";
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
