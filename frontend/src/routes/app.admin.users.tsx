import { createFileRoute } from "@tanstack/react-router";
import { Lock, Pencil, Plus, Search, Trash2, Unlock, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { AccountStatusDialog } from "@/components/app/users/AccountStatusDialog";
import { DeleteUserDialog } from "@/components/app/users/DeleteUserDialog";
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
  const { user, activeRole, accessToken } = useAuth();
  const { projects } = useProjects();
  const { users, loading, loadError, create, update, updateRoles, lock, unlock, remove } =
    useUsers(accessToken);
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<UserAccount | null>(null);
  const [statusTarget, setStatusTarget] = useState<UserAccount | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserAccount | null>(null);
  const [statusAction, setStatusAction] = useState<"lock" | "unlock">("lock");
  const [saving, setSaving] = useState(false);
  const [statusPending, setStatusPending] = useState(false);
  const [deletePending, setDeletePending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (loadError) toast.error(loadError);
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

  if (!user || !activeRole) return null;
  const actor: UserActor = {
    id: user.id,
    roles: user.roles,
    isMainAdmin: user.isMainAdmin,
  };
  const isAdminRoute = activeRole === "admin" && user.roles.includes("admin");

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
    setError(null);
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
      setError(result.error);
      toast.error(result.error);
      return;
    }
    setCreating(false);
    setEditing(null);
    if (editing) {
      toast.success("User account updated.");
    } else if ("temporaryPassword" in result.value && result.value.temporaryPassword) {
      toast.success(`User account created. Temporary password: ${result.value.temporaryPassword}`);
    } else {
      toast.success("User account created.");
    }
  };

  const runStatusAction = async () => {
    if (!statusTarget || statusPending) return;
    setStatusPending(true);
    setError(null);
    const result =
      statusAction === "lock"
        ? await lock(actor, statusTarget.id)
        : await unlock(actor, statusTarget.id);
    setStatusPending(false);
    if (!result.ok) {
      setError(result.error);
      toast.error(result.error);
      return;
    }
    if (statusAction === "lock") clearSessionIfUser(statusTarget.id);
    toast.success(statusAction === "lock" ? "Account locked." : "Account unlocked.");
    setStatusTarget(null);
  };

  const runDeleteAction = async () => {
    if (!deleteTarget || deletePending) return;
    setDeletePending(true);
    setError(null);
    const result = await remove(actor, deleteTarget.id);
    setDeletePending(false);
    if (!result.ok) {
      setError(result.error);
      toast.error(result.error);
      return;
    }
    clearSessionIfUser(deleteTarget.id);
    toast.success("User account deleted.");
    setDeleteTarget(null);
  };

  return (
    <>
      <PageHeader
        eyebrow={user.isMainAdmin ? "Main Admin" : "Admin"}
        title="User management"
        description="Manage backend lab accounts, role assignments, and account status."
        action={
          <button
            onClick={() => {
              setCreating(true);
              setEditing(null);
              setError(null);
            }}
            className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
          >
            <Plus className="h-3.5 w-3.5" /> Create user
          </button>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <MiniStat label="Total users" value={users.length} />
        <MiniStat label="Active" value={activeCount} tone="emerald" />
        <MiniStat label="Admins" value={adminCount} tone="violet" />
      </div>

      <Panel
        title="Accounts"
        description={
          loading ? "Loading from database..." : `${filtered.length} shown from database`
        }
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
        {loading ? (
          <EmptyState title="Loading users" hint="Fetching accounts from the backend database." />
        ) : filtered.length === 0 ? (
          <EmptyState
            title={users.length === 0 ? "No users" : "No users match these filters"}
            hint="Try a different search, role, or account-status filter."
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[920px] text-sm">
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
                            disabled={!!editBlocked}
                            onClick={() => {
                              setEditing(account);
                              setCreating(false);
                              setError(null);
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
                            disabled={!!statusBlocked}
                            onClick={() => {
                              setStatusTarget(account);
                              setStatusAction(account.status === "active" ? "lock" : "unlock");
                              setError(null);
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
                            disabled={!!deleteBlocked}
                            onClick={() => {
                              setDeleteTarget(account);
                              setError(null);
                            }}
                            title={deleteBlocked ?? "Delete user"}
                            className={cn(
                              "inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs",
                              deleteBlocked
                                ? "cursor-not-allowed text-ink-soft/40"
                                : "text-ink-soft hover:bg-muted hover:text-[color:var(--destructive)]",
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
        error={error}
        onClose={() => {
          if (!saving) {
            setCreating(false);
            setEditing(null);
            setError(null);
          }
        }}
        onSave={saveUser}
      />

      <AccountStatusDialog
        user={statusTarget}
        action={statusAction}
        open={!!statusTarget}
        pending={statusPending}
        error={error}
        onClose={() => {
          if (!statusPending) {
            setStatusTarget(null);
            setError(null);
          }
        }}
        onConfirm={runStatusAction}
      />

      <DeleteUserDialog
        user={deleteTarget}
        open={!!deleteTarget}
        pending={deletePending}
        onClose={() => {
          if (!deletePending) {
            setDeleteTarget(null);
            setError(null);
          }
        }}
        onConfirm={runDeleteAction}
      />
    </>
  );
}

function getEditBlock(actor: UserActor, target: UserAccount) {
  if (actor.id === target.id) return "Admins cannot edit their own account through this UI.";
  if (actor.isMainAdmin) return null;
  if (target.isMainAdmin) return "Regular Admins cannot modify the Main Admin.";
  if (target.roles.includes("admin")) return "Regular Admins cannot edit Admin accounts.";
  return null;
}

function getStatusBlock(actor: UserActor, target: UserAccount) {
  if (actor.id === target.id) return "Admins cannot lock their own account.";
  if (target.isMainAdmin) return "Main Admin cannot be locked.";
  if (actor.isMainAdmin) return null;
  if (target.roles.includes("admin")) return "Regular Admins cannot lock or unlock Admin accounts.";
  return null;
}

function getDeleteBlock(actor: UserActor, target: UserAccount) {
  if (actor.id === target.id) return "Admins cannot delete their own account.";
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
