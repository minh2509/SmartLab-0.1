import { useEffect, useState } from "react";
import {
  type Project,
  type ProjectStatus,
  type ProjectType,
  type ProjectVisibility,
  type FieldKey,
  fieldMeta,
} from "@/lib/projects-data";
import { useUsers } from "@/lib/users-data";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

const STATUSES: ProjectStatus[] = ["Planning", "Active", "Publishing", "On hold", "Completed"];
const TYPES: ProjectType[] = ["Research", "Production"];
const VIS: ProjectVisibility[] = ["public", "internal"];
const FIELDS: FieldKey[] = ["ai", "robotics", "se"];

export function ProjectEditDialog({
  project,
  open,
  onClose,
  onSave,
  canEditLeaders,
  accessToken,
  pending = false,
  serverError,
}: {
  project: Project | null;
  open: boolean;
  onClose: () => void;
  onSave: (patch: Partial<Project>) => void;
  canEditLeaders: boolean;
  accessToken?: string | null;
  pending?: boolean;
  serverError?: string | null;
}) {
  const [form, setForm] = useState<Project | null>(project);
  const [error, setError] = useState<string | null>(null);
  const [remoteError, setRemoteError] = useState<string | null>(serverError ?? null);
  const { users, loading: usersLoading, loadError: usersError } = useUsers(accessToken);
  const creating = !project?.id;
  const databaseMode = Boolean(accessToken);

  useEffect(() => {
    setForm(project);
    setError(null);
  }, [project, open]);

  useEffect(() => setRemoteError(serverError ?? null), [serverError]);

  if (!open || !form) return null;

  const updateForm = (patch: Partial<Project>) => {
    setForm((current) => (current ? { ...current, ...patch } : current));
    setError(null);
    setRemoteError(null);
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.code.trim()) {
      setError("Project code is required.");
      return;
    }
    if (!form.name.trim() || !form.description.trim() || !form.objective.trim()) {
      setError("Name, description, and objective are required.");
      return;
    }
    if (!form.startDate || !form.expectedEnd) {
      setError("Start date and expected end date are required.");
      return;
    }
    if (form.expectedEnd < form.startDate) {
      setError("Expected end date must not be before the start date.");
      return;
    }
    if (form.leaderIds.length === 0) {
      setError("At least one project leader is required.");
      return;
    }
    if (form.progress < 0 || form.progress > 100) {
      setError("Progress must be between 0 and 100.");
      return;
    }
    if (form.fields.length === 0) {
      setError("At least one research field is required.");
      return;
    }
    setError(null);
    onSave({
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim(),
      objective: form.objective.trim(),
      type: form.type,
      fields: form.fields,
      leaderIds: form.leaderIds,
      memberIds: form.memberIds,
      startDate: form.startDate,
      expectedEnd: form.expectedEnd,
      status: form.status,
      progress: Number(form.progress),
      visibility: form.visibility,
    });
  };

  const toggle = <T,>(list: T[], item: T): T[] =>
    list.includes(item) ? list.filter((x) => x !== item) : [...list, item];
  const leaderOptions = users.filter(
    (user) =>
      form.leaderIds.includes(user.id) ||
      (user.status === "active" && (user.roles.includes("leader") || user.roles.includes("admin"))),
  );
  const memberOptions = users.filter(
    (user) => form.memberIds.includes(user.id) || user.status === "active",
  );

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <form
        onSubmit={submit}
        onMouseDown={(e) => e.stopPropagation()}
        className="mt-10 w-full max-w-2xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {creating ? "Create project" : "Edit project"}
            </div>
            {!creating ? <div className="font-mono text-xs text-ink-soft">{form.code}</div> : null}
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
          <Field label="Project code">
            <input
              className="input font-mono"
              value={form.code}
              onChange={(e) => updateForm({ code: e.target.value })}
              maxLength={100}
              required
            />
          </Field>
          <Field label="Name">
            <input
              className="input"
              value={form.name}
              onChange={(e) => updateForm({ name: e.target.value })}
              required
            />
          </Field>
          <Field label="Description">
            <textarea
              className="input min-h-[80px]"
              value={form.description}
              onChange={(e) => updateForm({ description: e.target.value })}
              required
            />
          </Field>
          <Field label="Objective">
            <textarea
              className="input min-h-[64px]"
              value={form.objective}
              onChange={(e) => updateForm({ objective: e.target.value })}
              required
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Type">
              <select
                className="input"
                value={form.type}
                onChange={(e) => updateForm({ type: e.target.value as ProjectType })}
              >
                {TYPES.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </Field>
            <Field label="Status">
              <select
                className="input"
                value={form.status}
                onChange={(e) => updateForm({ status: e.target.value as ProjectStatus })}
              >
                {STATUSES.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </Field>
            <Field label="Start date *">
              <input
                type="date"
                className="input"
                value={form.startDate}
                max={form.expectedEnd || undefined}
                onChange={(e) => updateForm({ startDate: e.target.value })}
                required
              />
            </Field>
            <Field label="Expected end *">
              <input
                type="date"
                className="input"
                value={form.expectedEnd}
                min={form.startDate || undefined}
                onChange={(e) => updateForm({ expectedEnd: e.target.value })}
                required
              />
            </Field>
            <Field label="Progress (%)">
              <input
                type="number"
                min={0}
                max={100}
                className="input"
                value={form.progress}
                onChange={(e) => updateForm({ progress: Number(e.target.value) })}
                required
              />
            </Field>
            <Field label="Visibility">
              <select
                className="input"
                value={form.visibility}
                onChange={(e) => updateForm({ visibility: e.target.value as ProjectVisibility })}
              >
                {VIS.map((v) => (
                  <option key={v} value={v}>
                    {v === "public" ? "Public" : "Internal"}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <Field label="Research fields">
            <div className="flex flex-wrap gap-1.5">
              {FIELDS.map((k) => {
                const active = form.fields.includes(k);
                return (
                  <button
                    type="button"
                    key={k}
                    aria-pressed={active}
                    onClick={() => updateForm({ fields: toggle(form.fields, k) })}
                    className={cn(
                      "rounded-full border px-2.5 py-1 text-xs transition-colors",
                      active
                        ? "border-ink bg-ink text-background"
                        : "border-hairline text-ink-soft hover:text-ink",
                    )}
                  >
                    {fieldMeta[k].name}
                  </button>
                );
              })}
            </div>
          </Field>

          <Field label={canEditLeaders ? "Leaders" : "Leaders (admin can change assignments)"}>
            <div className="flex flex-wrap gap-1.5">
              {leaderOptions.map((u) => {
                const active = form.leaderIds.includes(u.id);
                const disabled =
                  !canEditLeaders ||
                  u.status === "locked" ||
                  (!u.roles.includes("leader") && !u.roles.includes("admin"));
                return (
                  <button
                    key={u.id}
                    type="button"
                    disabled={disabled}
                    aria-pressed={active}
                    onClick={() => updateForm({ leaderIds: toggle(form.leaderIds, u.id) })}
                    className={cn(
                      "rounded-full border px-2.5 py-1 text-xs transition-colors",
                      active
                        ? "border-ink bg-ink text-background"
                        : "border-hairline text-ink-soft",
                      !disabled ? "hover:text-ink" : "cursor-not-allowed opacity-70",
                    )}
                  >
                    {u.fullName}
                    {u.status === "locked" ? " · Locked" : ""}
                  </button>
                );
              })}
            </div>
            {usersLoading ? (
              <div className="mt-2 text-xs text-ink-soft">Loading users...</div>
            ) : null}
            {usersError ? (
              <div className="mt-2 text-xs text-[color:var(--destructive)]">{usersError}</div>
            ) : null}
            {!usersLoading && !usersError ? (
              <div className="mt-2 text-xs text-ink-soft">
                {form.leaderIds.length} leader{form.leaderIds.length === 1 ? "" : "s"} selected
              </div>
            ) : null}
          </Field>

          {!databaseMode ? (
            <Field label="Members">
              <div className="flex flex-wrap gap-1.5">
                {memberOptions.map((u) => {
                  const active = form.memberIds.includes(u.id);
                  const disabled = u.status === "locked";
                  return (
                    <button
                      key={u.id}
                      type="button"
                      disabled={disabled}
                      aria-pressed={active}
                      onClick={() => updateForm({ memberIds: toggle(form.memberIds, u.id) })}
                      className={cn(
                        "rounded-full border px-2.5 py-1 text-xs transition-colors",
                        active
                          ? "border-ink bg-ink text-background"
                          : "border-hairline text-ink-soft hover:text-ink",
                        disabled && "cursor-not-allowed opacity-70 hover:text-ink-soft",
                      )}
                    >
                      {u.fullName}
                      {u.status === "locked" ? " · Locked" : ""}
                    </button>
                  );
                })}
              </div>
            </Field>
          ) : null}

          {error || remoteError ? (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {error ?? remoteError}
            </div>
          ) : null}
        </div>

        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={pending || usersLoading || Boolean(usersError)}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {pending ? "Saving..." : creating ? "Create project" : "Save changes"}
          </button>
        </footer>
      </form>

      <style>{`
        .input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 6px 10px;
          font-size: 13px;
          color: inherit;
        }
        .input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      {children}
    </label>
  );
}
