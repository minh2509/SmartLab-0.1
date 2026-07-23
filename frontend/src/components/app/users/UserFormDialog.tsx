import { useEffect, useMemo, useState } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  getLeaderRoleRemovalBlock,
  roleLabel,
  type AccountStatus,
  type Role,
  type UserAccount,
  type UserActor,
  type UserDraft,
} from "@/lib/users-data";
import type { Project } from "@/lib/projects-data";

const allRoles: Role[] = ["admin", "leader", "member"];
const regularAdminRoles: Role[] = ["leader", "member"];

function emptyDraft(actor: UserActor): UserDraft {
  return {
    fullName: "",
    email: "",
    title: "",
    roles: actor.isMainAdmin ? ["member"] : ["member"],
    status: "active",
    temporaryPassword: "",
  };
}

function toDraft(user: UserAccount | null, actor: UserActor): UserDraft {
  if (!user) return emptyDraft(actor);
  return {
    fullName: user.fullName,
    email: user.email,
    title: user.title,
    roles: user.roles,
    status: user.status,
    temporaryPassword: "",
  };
}

export function UserFormDialog({
  open,
  mode,
  user,
  actor,
  users,
  projects,
  saving,
  error,
  onClose,
  onSave,
}: {
  open: boolean;
  mode: "create" | "edit";
  user: UserAccount | null;
  actor: UserActor;
  users: UserAccount[];
  projects: Project[];
  saving: boolean;
  error?: string | null;
  onClose: () => void;
  onSave: (draft: UserDraft) => void;
}) {
  const [form, setForm] = useState<UserDraft>(() => toDraft(user, actor));
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setForm(toDraft(user, actor));
    setTouched({});
  }, [user, actor, open]);

  const allowedRoles = actor.isMainAdmin ? allRoles : regularAdminRoles;
  const errors = useMemo(
    () => validate(form, users, actor, mode === "edit" ? user : null, projects),
    [actor, form, mode, projects, user, users],
  );
  const valid = Object.keys(errors).length === 0;
  const title = mode === "create" ? "Create user account" : "Edit user account";

  if (!open) return null;

  const toggleRole = (role: Role) => {
    if (!allowedRoles.includes(role)) return;
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(role)
        ? current.roles.filter((item) => item !== role)
        : [...current.roles, role],
    }));
  };

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setTouched({ fullName: true, email: true, roles: true, temporaryPassword: true });
    if (!valid || saving) return;
    onSave({
      fullName: form.fullName.trim(),
      email: form.email.trim().toLowerCase(),
      title: form.title.trim(),
      roles: Array.from(new Set(form.roles)),
      status: form.status,
      temporaryPassword: form.temporaryPassword?.trim(),
    });
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
        className="mt-10 w-full max-w-2xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Admin user management
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{title}</h2>
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
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full name" error={touched.fullName ? errors.fullName : undefined}>
              <input
                className="user-input"
                value={form.fullName}
                onBlur={() => setTouched((current) => ({ ...current, fullName: true }))}
                onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              />
            </Field>
            <Field label="Email" error={touched.email ? errors.email : undefined}>
              <input
                className="user-input"
                type="email"
                value={form.email}
                onBlur={() => setTouched((current) => ({ ...current, email: true }))}
                onChange={(event) => setForm({ ...form, email: event.target.value })}
              />
            </Field>
          </div>

          {mode === "create" ? (
            <Field
              label="Temporary password"
              error={touched.temporaryPassword ? errors.temporaryPassword : undefined}
            >
              <input
                className="user-input"
                type="password"
                value={form.temporaryPassword ?? ""}
                autoComplete="new-password"
                onBlur={() => setTouched((current) => ({ ...current, temporaryPassword: true }))}
                onChange={(event) => setForm({ ...form, temporaryPassword: event.target.value })}
              />
            </Field>
          ) : null}

          <Field label="Title">
            <input
              className="user-input"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              placeholder="Research assistant · Software Engineering"
            />
          </Field>

          <Field label="Roles" error={touched.roles ? errors.roles : undefined}>
            <div className="flex flex-wrap gap-1.5">
              {allRoles.map((role) => {
                const allowed = allowedRoles.includes(role);
                const active = form.roles.includes(role);
                return (
                  <button
                    key={role}
                    type="button"
                    disabled={!allowed}
                    onClick={() => toggleRole(role)}
                    className={cn(
                      "rounded-full border px-2.5 py-1 text-xs transition-colors",
                      active
                        ? "border-ink bg-ink text-background"
                        : "border-hairline text-ink-soft",
                      allowed ? "hover:text-ink" : "cursor-not-allowed opacity-45",
                    )}
                    title={allowed ? roleLabel[role] : "Regular Admins cannot assign Admin role"}
                  >
                    {roleLabel[role]}
                  </button>
                );
              })}
            </div>
          </Field>

          <Field label="Account status">
            <select
              className="user-input"
              value={form.status}
              disabled={mode === "edit"}
              onChange={(event) =>
                setForm({ ...form, status: event.target.value as AccountStatus })
              }
            >
              <option value="active">Active</option>
              <option value="locked">Locked</option>
            </select>
          </Field>

          {errors.policy ? (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {errors.policy}
            </div>
          ) : null}
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
            {saving ? "Saving..." : mode === "create" ? "Create user" : "Save changes"}
          </button>
        </footer>
      </form>

      <style>{`
        .user-input {
          width: 100%;
          border: 1px solid var(--hairline, hsl(0 0% 90%));
          background: var(--background);
          border-radius: 6px;
          padding: 7px 10px;
          font-size: 13px;
          color: inherit;
        }
        .user-input:focus { outline: 2px solid var(--cyan); outline-offset: 0; }
      `}</style>
    </div>
  );
}

function validate(
  form: UserDraft,
  users: UserAccount[],
  actor: UserActor,
  editing: UserAccount | null,
  projects: Project[],
) {
  const errors: Partial<Record<keyof UserDraft | "policy", string>> = {};
  const email = form.email.trim().toLowerCase();
  if (!form.fullName.trim()) errors.fullName = "Full name is required.";
  if (!email) errors.email = "Email is required.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "Email is invalid.";
  else if (users.some((user) => user.id !== editing?.id && user.email === email)) {
    errors.email = "Email already exists.";
  }
  if (form.roles.length === 0) errors.roles = "At least one role is required.";
  if (!editing) {
    const temporaryPassword = form.temporaryPassword?.trim() ?? "";
    if (!temporaryPassword) errors.temporaryPassword = "Temporary password is required.";
    else if (temporaryPassword.length < 12 || temporaryPassword.length > 72) {
      errors.temporaryPassword = "Temporary password must be between 12 and 72 characters.";
    }
  }
  if (!actor.isMainAdmin && form.roles.includes("admin")) {
    errors.policy = "Regular Admins cannot assign the Admin role.";
  }
  if (!actor.isMainAdmin && editing?.roles.includes("admin")) {
    errors.policy = "Regular Admins cannot edit Admin accounts.";
  }
  if (editing?.isMainAdmin && !form.roles.includes("admin")) {
    errors.policy = "The Main Admin must keep the Admin role.";
  }
  if (editing) {
    const leaderBlock = getLeaderRoleRemovalBlock(editing.id, form.roles, projects);
    if (leaderBlock) errors.policy = leaderBlock;
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
