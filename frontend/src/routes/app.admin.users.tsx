import { createFileRoute } from "@tanstack/react-router";
import {
  KeyRound,
  Lock,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Unlock,
  Users,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { AccountStatusDialog } from "@/components/app/users/AccountStatusDialog";
import { UserFormDialog } from "@/components/app/users/UserFormDialog";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import { formatDate, useProjects } from "@/lib/projects-data";
import {
  clearSessionIfUser,
  roleLabel,
  useUsers,
  type AccountStatus,
  type Role,
  type UserAccount,
  type UserActor,
  type UserDraft,
} from "@/lib/users-data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/admin/users")({
  head: () => ({
    meta: [{ title: "User Management — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: AdminUsersPage,
});

type RoleFilter = "all" | Role;
type StatusFilter = "all" | AccountStatus;

function AdminUsersPage() {
  const { user, activeRole } = useAuth();
  const { projects } = useProjects();
  const {
    users,
    create,
    update,
    updateRoles,
    lock,
    unlock,
    resetPassword,
    deleteUser,
    refresh,
    loading,
    loadError,
    remoteEnabled,
  } = useUsers();
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<UserAccount | null>(null);
  const [statusTarget, setStatusTarget] = useState<UserAccount | null>(null);
  const [statusAction, setStatusAction] = useState<"lock" | "unlock">("lock");
  const [confirmTarget, setConfirmTarget] = useState<UserAccount | null>(null);
  const [confirmAction, setConfirmAction] = useState<"reset-password" | "delete">("reset-password");
  const [saving, setSaving] = useState(false);
  const [statusPending, setStatusPending] = useState(false);
  const [confirmPending, setConfirmPending] = useState(false);

  const actor: UserActor | null = useMemo(
    () =>
      user
        ? {
            id: user.id,
            email: user.email,
            roles: user.roles,
            isMainAdmin: user.isMainAdmin,
            labId: user.labId,
          }
        : null,
    [user],
  );
  const isAdminRoute = !!user && activeRole === "admin" && user.roles.includes("admin");

  useEffect(() => {
    if (!actor || !isAdminRoute) return;
    void refresh();
  }, [actor, isAdminRoute, refresh]);

  useEffect(() => {
    if (!loadError) return;
    toast.error("Users could not be loaded", {
      description: loadError,
    });
  }, [loadError]);

  const filtered = useMemo(
    () =>
      users.filter((account) => {
        const text = query.trim().toLowerCase();
        if (
          text &&
          !account.fullName.toLowerCase().includes(text) &&
          !account.email.toLowerCase().includes(text)
        ) {
          return false;
        }
        if (roleFilter !== "all" && !account.roles.includes(roleFilter)) return false;
        if (statusFilter !== "all" && account.status !== statusFilter) return false;
        return true;
      }),
    [query, roleFilter, statusFilter, users],
  );

  if (!user || !activeRole || !actor) return null;

  if (!isAdminRoute) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <Users className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Admin workspace required</h1>
        <p className="mt-1 text-xs text-ink-soft">
          User management is available only when an Admin user is viewing as Admin.
        </p>
      </div>
    );
  }

  const activeCount = users.filter((account) => account.status === "active").length;
  const adminCount = users.filter((account) => account.roles.includes("admin")).length;

  const saveUser = async (draft: UserDraft) => {
    if (saving) return;
    setSaving(true);
    const result = editing
      ? await (async () => {
          const roleResult = await updateRoles(actor, editing.id, draft.roles, projects);
          return roleResult.ok
            ? update(actor, editing.id, {
                fullName: draft.fullName,
                email: draft.email,
                title: draft.title,
              })
            : roleResult;
        })()
      : await create(actor, draft);
    setSaving(false);
    if (!result.ok) {
      toast.error(editing ? "User update failed" : "User creation failed", {
        description: result.error,
      });
      return;
    }
    setCreating(false);
    setEditing(null);
    toast.success(editing ? "User account updated" : "User account created", {
      description: result.value.email,
    });
  };

  const runStatusAction = async () => {
    if (!statusTarget || statusPending) return;
    setStatusPending(true);
    const result =
      statusAction === "lock"
        ? await lock(actor, statusTarget.id)
        : await unlock(actor, statusTarget.id);
    setStatusPending(false);
    if (!result.ok) {
      toast.error(statusAction === "lock" ? "Account lock failed" : "Account unlock failed", {
        description: result.error,
      });
      return;
    }
    if (statusAction === "lock") clearSessionIfUser(statusTarget.id);
    toast.success(statusAction === "lock" ? "Account locked" : "Account unlocked", {
      description: statusTarget.email,
    });
    setStatusTarget(null);
  };

  const refreshUsers = async () => {
    if (loading) return;
    const result = await refresh();
    if (!result.ok) {
      toast.error("Refresh failed", {
        description: result.error,
      });
      return;
    }
    toast.success(remoteEnabled ? "Backend users refreshed" : "Backend is not configured", {
      description: `${result.value.length} users loaded.`,
    });
  };

  const runConfirmAction = async (confirmPassword = "") => {
    if (!confirmTarget || confirmPending) return;
    setConfirmPending(true);
    const result =
      confirmAction === "reset-password"
        ? await resetPassword(actor, confirmTarget.id, confirmPassword)
        : await deleteUser(actor, confirmTarget.id);
    setConfirmPending(false);
    if (!result.ok) {
      toast.error(
        confirmAction === "reset-password" ? "Password reset failed" : "User delete failed",
        {
          description: result.error,
        },
      );
      return;
    }
    if (confirmAction === "delete") clearSessionIfUser(confirmTarget.id);
    toast.success(confirmAction === "reset-password" ? "Password reset" : "User account deleted", {
      description: confirmTarget.email,
    });
    setConfirmTarget(null);
  };

  return (
    <>
      <PageHeader
        eyebrow={user.isMainAdmin ? "Main Admin" : "Admin"}
        title="User management"
        description={
          remoteEnabled
            ? "Manage backend lab accounts, role assignments, password resets, and account status."
            : "Connect VITE_API_BASE_URL to manage database-backed lab accounts."
        }
        action={
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={refreshUsers}
              disabled={loading}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            >
              <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} /> Refresh
            </button>
            <button
              onClick={() => {
                setCreating(true);
                setEditing(null);
              }}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Plus className="h-3.5 w-3.5" /> Create user
            </button>
          </div>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat label="Total users" value={users.length} />
        <MiniStat label="Active" value={activeCount} tone="emerald" />
        <MiniStat label="Admins" value={adminCount} tone="violet" />
      </div>

      <Panel
        title="Accounts"
        description={`${filtered.length} shown from database`}
        action={
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-soft" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search users..."
                className="w-full rounded-md border border-hairline bg-background py-1.5 pl-7 pr-2 text-xs text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40 sm:w-56"
              />
            </div>
            <select
              value={roleFilter}
              onChange={(event) => setRoleFilter(event.target.value as RoleFilter)}
              className="rounded-md border border-hairline bg-background px-2 py-1.5 text-xs text-ink"
            >
              <option value="all">All roles</option>
              <option value="admin">Admin</option>
              <option value="leader">Leader</option>
              <option value="member">Member</option>
            </select>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
              className="rounded-md border border-hairline bg-background px-2 py-1.5 text-xs text-ink"
            >
              <option value="all">All statuses</option>
              <option value="active">Active</option>
              <option value="locked">Locked</option>
            </select>
          </div>
        }
        className="overflow-hidden"
      >
        {filtered.length === 0 ? (
          <EmptyState
            title={users.length === 0 ? "No users" : "No users match these filters"}
            hint="Try a different search, role, or account-status filter."
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[1080px] text-sm">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">User</th>
                  <th className="px-3 py-3 font-medium">Roles</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Last login</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((account) => {
                  const editBlocked = getEditBlock(actor, account);
                  const statusBlocked = getStatusBlock(actor, account);
                  const resetBlocked = editBlocked;
                  const deleteBlocked = getDeleteBlock(actor, account);
                  return (
                    <tr key={account.id} className="border-t border-hairline align-top">
                      <td className="px-5 py-3">
                        <div className="flex items-start gap-3">
                          <div className="grid h-9 w-9 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                            {account.initials}
                          </div>
                          <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                              <div className="font-medium text-ink">{account.fullName}</div>
                              {account.isMainAdmin ? (
                                <StatusPill tone="violet">Main Admin</StatusPill>
                              ) : null}
                            </div>
                            <div className="mt-0.5 text-xs text-ink-soft">{account.email}</div>
                            {account.title ? (
                              <div className="mt-0.5 text-xs text-ink-soft">{account.title}</div>
                            ) : null}
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <div className="flex flex-wrap gap-1">
                          {account.roles.map((role) => (
                            <StatusPill key={role} tone={roleTone(role)}>
                              {roleLabel[role]}
                            </StatusPill>
                          ))}
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <StatusPill tone={account.status === "active" ? "emerald" : "rose"}>
                          {account.status === "active" ? "Active" : "Locked"}
                        </StatusPill>
                      </td>
                      <td className="px-3 py-3 text-xs text-ink-soft">
                        {account.lastLoginAt ? formatDate(account.lastLoginAt) : "Never"}
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            disabled={!!editBlocked || saving}
                            onClick={() => {
                              setEditing(account);
                              setCreating(false);
                            }}
                            title={editBlocked ?? "Edit user"}
                            className={cn(
                              "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                              editBlocked
                                ? "cursor-not-allowed text-ink-soft/40"
                                : "text-ink-soft hover:bg-muted hover:text-ink",
                            )}
                          >
                            <Pencil className="h-3.5 w-3.5" /> Edit
                          </button>
                          <button
                            disabled={!!statusBlocked || statusPending}
                            onClick={() => {
                              setStatusTarget(account);
                              setStatusAction(account.status === "active" ? "lock" : "unlock");
                            }}
                            title={statusBlocked ?? "Change account status"}
                            className={cn(
                              "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                              statusBlocked
                                ? "cursor-not-allowed text-ink-soft/40"
                                : "text-ink-soft hover:bg-muted hover:text-ink",
                            )}
                          >
                            {account.status === "active" ? (
                              <Lock className="h-3.5 w-3.5" />
                            ) : (
                              <Unlock className="h-3.5 w-3.5" />
                            )}
                            {account.status === "active" ? "Lock" : "Unlock"}
                          </button>
                          <button
                            disabled={!!resetBlocked || confirmPending}
                            onClick={() => {
                              setConfirmTarget(account);
                              setConfirmAction("reset-password");
                            }}
                            title={resetBlocked ?? "Reset password"}
                            className={cn(
                              "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                              resetBlocked
                                ? "cursor-not-allowed text-ink-soft/40"
                                : "text-ink-soft hover:bg-muted hover:text-ink",
                            )}
                          >
                            <KeyRound className="h-3.5 w-3.5" /> Reset
                          </button>
                          <button
                            disabled={!!deleteBlocked || confirmPending}
                            onClick={() => {
                              setConfirmTarget(account);
                              setConfirmAction("delete");
                            }}
                            title={deleteBlocked ?? "Delete user"}
                            className={cn(
                              "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                              deleteBlocked
                                ? "cursor-not-allowed text-ink-soft/40"
                                : "text-[color:var(--destructive)] hover:bg-muted",
                            )}
                          >
                            <Trash2 className="h-3.5 w-3.5" /> Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <UserFormDialog
        open={creating || !!editing}
        mode={editing ? "edit" : "create"}
        user={editing}
        actor={actor}
        users={users}
        projects={projects}
        saving={saving}
        onClose={() => {
          if (!saving) {
            setCreating(false);
            setEditing(null);
          }
        }}
        onSave={saveUser}
      />

      <AccountStatusDialog
        user={statusTarget}
        action={statusAction}
        open={!!statusTarget}
        pending={statusPending}
        onClose={() => {
          if (!statusPending) {
            setStatusTarget(null);
          }
        }}
        onConfirm={runStatusAction}
      />

      <ConfirmUserActionDialog
        user={confirmTarget}
        action={confirmAction}
        open={!!confirmTarget}
        pending={confirmPending}
        onClose={() => {
          if (!confirmPending) {
            setConfirmTarget(null);
          }
        }}
        onConfirm={runConfirmAction}
      />
    </>
  );
}

function isActorSelf(actor: UserActor, target: UserAccount) {
  return actor.id === target.id || actor.email === target.email;
}

function getEditBlock(actor: UserActor, target: UserAccount) {
  if (isActorSelf(actor, target)) return "Admins cannot edit their own account through this UI.";
  if (actor.isMainAdmin) return null;
  if (target.isMainAdmin) return "Regular Admins cannot modify the Main Admin.";
  if (target.roles.includes("admin")) return "Regular Admins cannot edit Admin accounts.";
  return null;
}

function getStatusBlock(actor: UserActor, target: UserAccount) {
  if (isActorSelf(actor, target)) return "Admins cannot lock their own account.";
  if (target.isMainAdmin) return "Main Admin cannot be locked.";
  if (actor.isMainAdmin) return null;
  if (target.roles.includes("admin")) return "Regular Admins cannot lock or unlock Admin accounts.";
  return null;
}

function getDeleteBlock(actor: UserActor, target: UserAccount) {
  if (isActorSelf(actor, target)) return "Admins cannot delete their own account.";
  if (target.isMainAdmin) return "Main Admin cannot be deleted.";
  if (actor.isMainAdmin) return null;
  if (target.roles.includes("admin")) return "Regular Admins cannot delete Admin accounts.";
  return null;
}

function roleTone(role: Role) {
  if (role === "admin") return "violet";
  if (role === "leader") return "cyan";
  return "neutral";
}

function MiniStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "emerald" | "violet";
}) {
  return (
    <div className="rounded-xl border border-hairline bg-surface-elev p-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div
        className={cn(
          "mt-2 font-display text-3xl text-ink",
          tone === "emerald" && "text-[color:var(--emerald-ink)]",
          tone === "violet" && "text-[color:var(--violet-ink)]",
        )}
      >
        {value}
      </div>
    </div>
  );
}

function ConfirmUserActionDialog({
  user,
  action,
  open,
  pending,
  onClose,
  onConfirm,
}: {
  user: UserAccount | null;
  action: "reset-password" | "delete";
  open: boolean;
  pending: boolean;
  onClose: () => void;
  onConfirm: (temporaryPassword: string) => void;
}) {
  const [confirmation, setConfirmation] = useState("");
  const [temporaryPassword, setTemporaryPassword] = useState("");
  useEffect(() => {
    setConfirmation("");
    setTemporaryPassword("");
  }, [user?.id, action, open]);
  if (!open || !user) return null;

  const destructive = action === "delete";
  const phrase = destructive ? "DELETE USER" : "RESET PASSWORD";
  const valid =
    confirmation.trim() === phrase && (destructive || temporaryPassword.trim().length >= 8);

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={pending ? undefined : onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-md rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {destructive ? "Delete user" : "Reset password"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{user.fullName}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <div className="space-y-4 p-5">
          <p className="text-sm leading-relaxed text-ink-soft">
            {destructive
              ? "This soft-deletes the backend account. Existing audit records remain available."
              : "This sets a new temporary backend password for the account."}
          </p>
          {!destructive ? (
            <label className="block">
              <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                New temporary password
              </div>
              <input
                type="password"
                value={temporaryPassword}
                onChange={(event) => setTemporaryPassword(event.target.value)}
                className="w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              />
              {temporaryPassword && temporaryPassword.trim().length < 8 ? (
                <div className="mt-1 text-xs text-[color:var(--destructive)]">
                  Temporary password must be at least 8 characters.
                </div>
              ) : null}
            </label>
          ) : null}
          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Type {phrase}
            </div>
            <input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
          </label>
        </div>

        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={!valid || pending}
            onClick={() => onConfirm(temporaryPassword.trim())}
            className={cn(
              "rounded-md px-3.5 py-1.5 text-sm font-medium transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40",
              destructive
                ? "bg-[color:var(--destructive)] text-white"
                : "bg-primary text-primary-foreground",
            )}
          >
            {pending ? "Saving..." : destructive ? "Delete user" : "Reset password"}
          </button>
        </footer>
      </div>
    </div>
  );
}
