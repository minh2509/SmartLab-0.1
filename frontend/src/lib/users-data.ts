import { useCallback, useEffect, useState } from "react";
import {
  clearStoredSession,
  isBackendConfigured,
  readStoredSession,
  type BackendSession,
} from "@/lib/backend-api";
import {
  createAdminUser,
  deleteAdminUser,
  fetchAdminUsers,
  lockAdminUser,
  resetAdminUserPassword,
  unlockAdminUser,
  updateAdminUser,
  updateAdminUserRoles,
} from "@/lib/admin-users-api";
import type { Project } from "@/lib/projects-data";

export type Role = "admin" | "leader" | "member";
export type AccountStatus = "active" | "locked";

export type UserAccount = {
  id: string;
  email: string;
  fullName: string;
  initials: string;
  title: string;
  roles: Role[];
  status: AccountStatus;
  isMainAdmin: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string;
  username?: string;
  labId?: string;
  avatarFileId?: string | null;
};

export type UserDraft = {
  fullName: string;
  email: string;
  title: string;
  roles: Role[];
  status: AccountStatus;
  password: string;
};

export type UserUpdate = Pick<UserDraft, "fullName" | "email" | "title">;

export type UserActor = {
  id: string;
  email?: string;
  roles: Role[];
  isMainAdmin: boolean;
  labId?: string;
};

export type Result<T = UserAccount> = { ok: true; value: T } | { ok: false; error: string };

const roles: Role[] = ["admin", "leader", "member"];
const statuses: AccountStatus[] = ["active", "locked"];

export const roleLabel: Record<Role, string> = {
  admin: "Admin",
  leader: "Project Leader",
  member: "Member",
};

type Sub = (list: UserAccount[]) => void;
const subs = new Set<Sub>();
let cache: UserAccount[] = [];

export function hydrateCurrentUser(session: BackendSession) {
  setUsers([session.user, ...cache.filter((user) => user.id !== session.user.id)]);
}

export function getUsers() {
  return cache;
}

function setUsers(next: UserAccount[]) {
  cache = dedupeUsers(next);
  subs.forEach((sub) => sub(cache));
  return true;
}

export function subscribeToUsers(sub: Sub) {
  subs.add(sub);
  return () => {
    subs.delete(sub);
  };
}

export function getUserById(id: string) {
  return getUsers().find((user) => user.id === id);
}

export function getUserByEmail(email: string) {
  const normalized = normalizeEmail(email);
  return getUsers().find((user) => user.email === normalized);
}

export function formatUserName(id: string) {
  return getUserById(id)?.fullName ?? "Unknown";
}

export function formatUserInitials(id: string) {
  return getUserById(id)?.initials ?? "?";
}

export function isUserLocked(id: string) {
  return getUserById(id)?.status === "locked";
}

export function getActiveUsers() {
  return getUsers().filter((user) => user.status === "active");
}

export function getLeaderRoleRemovalBlock(userId: string, nextRoles: Role[], projects: Project[]) {
  if (nextRoles.includes("leader")) return null;
  const assigned = projects.filter((project) => project.leaderIds.includes(userId));
  if (assigned.length === 0) return null;
  return `This user is still assigned as a Leader on ${assigned.length} project${
    assigned.length === 1 ? "" : "s"
  }. Reassign those projects before removing the Leader role.`;
}

export function clearSessionIfUser(userId: string) {
  if (readStoredSession()?.user.id === userId) clearStoredSession();
}

export function useUsers() {
  const [users, setList] = useState<UserAccount[]>(() => getUsers());
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const remoteEnabled = isBackendConfigured();

  useEffect(() => subscribeToUsers(setList), []);

  const refresh = useCallback(async (): Promise<Result<UserAccount[]>> => {
    if (!remoteEnabled) {
      const message = "VITE_API_BASE_URL is not configured.";
      setLoadError(message);
      return { ok: false, error: message };
    }
    setLoading(true);
    setLoadError(null);
    try {
      const remoteUsers = await fetchAdminUsers();
      setUsers(remoteUsers);
      setList(remoteUsers);
      return { ok: true, value: remoteUsers };
    } catch (error) {
      const message = getErrorMessage(error);
      setLoadError(message);
      return { ok: false, error: message };
    } finally {
      setLoading(false);
    }
  }, [remoteEnabled]);

  const create = useCallback(
    async (actor: UserActor, draft: UserDraft): Promise<Result> => {
      const cleaned = cleanDraft(draft);
      const validationError =
        validateUserDraft(cleaned, users) ?? validateRolesForActor(actor, null, cleaned.roles);
      if (validationError) return { ok: false, error: validationError };
      const labId = actor.labId ?? users.find((user) => user.labId)?.labId;
      if (!labId)
        return { ok: false, error: "Backend lab ID is unavailable. Refresh users first." };
      try {
        const created = await createAdminUser(cleaned, labId);
        setUsers([created, ...cache.filter((user) => user.id !== created.id)]);
        setList(getUsers());
        return { ok: true, value: created };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const update = useCallback(
    async (actor: UserActor, userId: string, patch: UserUpdate): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      const fullName = patch.fullName.trim();
      const email = normalizeEmail(patch.email);
      if (!fullName) return { ok: false, error: "Full name is required." };
      if (!isEmail(email)) return { ok: false, error: "Email is invalid." };
      if (!validateEmailUnique(email, userId, users))
        return { ok: false, error: "Email already exists." };
      try {
        const updated = await updateAdminUser(userId, {
          fullName,
          email,
          title: patch.title.trim(),
        });
        setUsers(cache.map((user) => (user.id === userId ? updated : user)));
        setList(getUsers());
        return { ok: true, value: updated };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const updateRoles = useCallback(
    async (
      actor: UserActor,
      userId: string,
      nextRoles: Role[],
      projects: Project[],
    ): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      const cleanedRoles = Array.from(new Set(nextRoles));
      const roleError = validateRolesForActor(actor, current, cleanedRoles);
      if (roleError) return { ok: false, error: roleError };
      const leaderBlock = getLeaderRoleRemovalBlock(userId, cleanedRoles, projects);
      if (leaderBlock) return { ok: false, error: leaderBlock };
      try {
        const assignedRoles = await updateAdminUserRoles(userId, cleanedRoles);
        const value = { ...current, roles: assignedRoles, updatedAt: new Date().toISOString() };
        setUsers(cache.map((user) => (user.id === userId ? value : user)));
        setList(getUsers());
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const lock = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be locked." };
      if (current.id === actor.id)
        return { ok: false, error: "Admins cannot lock their own account." };
      try {
        const value = await lockAdminUser(userId);
        setUsers(cache.map((user) => (user.id === userId ? value : user)));
        setList(getUsers());
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const unlock = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      try {
        const value = await unlockAdminUser(userId);
        setUsers(cache.map((user) => (user.id === userId ? value : user)));
        setList(getUsers());
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const resetPassword = useCallback(
    async (actor: UserActor, userId: string, password: string): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      if (password.trim().length < 8) {
        return { ok: false, error: "Temporary password must be at least 8 characters." };
      }
      try {
        const value = await resetAdminUserPassword(userId, password.trim());
        setUsers(cache.map((user) => (user.id === userId ? value : user)));
        setList(getUsers());
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  const remove = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be deleted." };
      if (current.id === actor.id)
        return { ok: false, error: "Admins cannot delete their own account." };
      try {
        await deleteAdminUser(userId);
        setUsers(cache.filter((user) => user.id !== userId));
        setList(getUsers());
        return { ok: true, value: current };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [users],
  );

  return {
    users,
    create,
    update,
    updateRoles,
    lock,
    unlock,
    resetPassword,
    deleteUser: remove,
    refresh,
    loading,
    loadError,
    remoteEnabled,
  };
}

function canManageUser(actor: UserActor, target: UserAccount) {
  if (!actor.roles.includes("admin")) return "Only Admin users can manage accounts.";
  if (actor.id === target.id || actor.email === target.email) {
    return "Admins cannot modify their own account through this UI.";
  }
  if (actor.isMainAdmin) return null;
  if (target.isMainAdmin) return "Regular Admins cannot modify the Main Admin.";
  if (target.roles.includes("admin")) return "Regular Admins cannot modify Admin accounts.";
  return null;
}

function validateUserDraft(draft: UserDraft, source: UserAccount[], excludeId?: string) {
  if (!draft.fullName) return "Full name is required.";
  if (!isEmail(draft.email)) return "Email is invalid.";
  if (!validateEmailUnique(draft.email, excludeId, source)) return "Email already exists.";
  if (!statuses.includes(draft.status)) return "Invalid account status.";
  if (!draft.password || draft.password.length < 8) {
    return "Temporary password must be at least 8 characters.";
  }
  return null;
}

function validateEmailUnique(email: string, excludeId: string | undefined, source: UserAccount[]) {
  return !source.some((user) => user.id !== excludeId && user.email === normalizeEmail(email));
}

function validateRolesForActor(actor: UserActor, target: UserAccount | null, nextRoles: Role[]) {
  if (nextRoles.length === 0) return "At least one role is required.";
  if (!nextRoles.every((role) => roles.includes(role))) return "Invalid role assignment.";
  if (!actor.roles.includes("admin")) return "Only Admin users can update roles.";
  if (actor.isMainAdmin) {
    if (target?.id === actor.id && !nextRoles.includes("admin")) {
      return "The Main Admin cannot remove their own Admin role.";
    }
    return null;
  }
  if (nextRoles.includes("admin")) return "Regular Admins cannot grant the Admin role.";
  if (target?.roles.includes("admin")) return "Regular Admins cannot edit Admin roles.";
  if (target?.id === actor.id || target?.email === actor.email) {
    return "Admins cannot remove their own role through this UI.";
  }
  return null;
}

function cleanDraft(draft: UserDraft): UserDraft {
  return {
    fullName: draft.fullName.trim(),
    email: normalizeEmail(draft.email),
    title: draft.title.trim(),
    roles: Array.from(new Set(draft.roles)),
    status: draft.status,
    password: draft.password.trim(),
  };
}

function dedupeUsers(list: UserAccount[]) {
  const seen = new Set<string>();
  const result: UserAccount[] = [];
  for (const user of list) {
    if (seen.has(user.id)) continue;
    seen.add(user.id);
    result.push({ ...user, email: normalizeEmail(user.email) });
  }
  return result;
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

function isEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Request failed.";
}
