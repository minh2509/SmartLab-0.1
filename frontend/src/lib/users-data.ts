import { useCallback, useEffect, useState } from "react";
import {
  createAdminUser,
  deleteAdminUser,
  fetchAdminUsers,
  isAdminUsersApiConfigured,
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

const STORAGE_KEY = "smart.users.v1";
const SESSION_STORAGE_KEY = "smart.session.v1";
const roles: Role[] = ["admin", "leader", "member"];
const statuses: AccountStatus[] = ["active", "locked"];
const seedCreatedAt = "2026-01-01T00:00:00.000Z";

export const DEMO_PASSWORD = "smart2026";

export const roleLabel: Record<Role, string> = {
  admin: "Admin",
  leader: "Project Leader",
  member: "Member",
};

export const seedUsers: UserAccount[] = [
  {
    id: "u_admin",
    email: "admin@smart.lab",
    fullName: "Alex Nguyen",
    initials: "AN",
    title: "Lab Administrator",
    roles: ["admin"],
    status: "active",
    isMainAdmin: true,
    createdAt: seedCreatedAt,
    updatedAt: seedCreatedAt,
  },
  {
    id: "u_amara",
    email: "amara@smart.lab",
    fullName: "Amara Osei",
    initials: "AO",
    title: "Research Engineer · Program Lead",
    roles: ["admin", "leader"],
    status: "active",
    isMainAdmin: false,
    createdAt: seedCreatedAt,
    updatedAt: seedCreatedAt,
  },
  {
    id: "u_tran",
    email: "tran@smart.lab",
    fullName: "Dr. Minh Tran",
    initials: "MT",
    title: "Principal Investigator · Atlas",
    roles: ["leader", "member"],
    status: "active",
    isMainAdmin: false,
    createdAt: seedCreatedAt,
    updatedAt: seedCreatedAt,
  },
  {
    id: "u_linh",
    email: "linh@smart.lab",
    fullName: "Linh Pham",
    initials: "LP",
    title: "PhD Researcher · Robotics",
    roles: ["member"],
    status: "active",
    isMainAdmin: false,
    createdAt: seedCreatedAt,
    updatedAt: seedCreatedAt,
  },
];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function readBoolean(value: unknown) {
  return typeof value === "boolean" ? value : false;
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

function uniqueRoles(value: unknown): Role[] {
  if (!Array.isArray(value)) return [];
  return Array.from(new Set(value.filter((item): item is Role => roles.includes(item as Role))));
}

function makeInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function makeId() {
  return `u_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function cleanDraft(draft: UserDraft): UserDraft {
  return {
    fullName: draft.fullName.trim(),
    email: normalizeEmail(draft.email),
    title: draft.title.trim(),
    roles: Array.from(new Set(draft.roles)),
    status: draft.status,
  };
}

function normalizeUser(value: unknown): UserAccount | null {
  if (!isRecord(value)) return null;
  const id = readString(value.id);
  const email = normalizeEmail(readString(value.email));
  const fullName = readString(value.fullName) || readString(value.name);
  const createdAt = readString(value.createdAt) || seedCreatedAt;
  const updatedAt = readString(value.updatedAt) || createdAt;
  const status = readString(value.status) as AccountStatus;
  const normalizedRoles = uniqueRoles(value.roles);
  if (!id || !email || !fullName || normalizedRoles.length === 0) return null;
  return {
    id,
    email,
    fullName,
    initials: readString(value.initials) || makeInitials(fullName),
    title: readString(value.title),
    roles: normalizedRoles,
    status: statuses.includes(status) ? status : "active",
    isMainAdmin: readBoolean(value.isMainAdmin),
    createdAt,
    updatedAt,
    lastLoginAt: readString(value.lastLoginAt) || undefined,
  };
}

function mergeWithSeed(list: UserAccount[]) {
  const byId = new Map(list.map((user) => [user.id, user]));
  const withSeeds = seedUsers.map((seed) => ({
    ...seed,
    ...byId.get(seed.id),
    id: seed.id,
    isMainAdmin: seed.id === "u_admin",
  }));
  const seedIds = new Set(seedUsers.map((user) => user.id));
  const extras = list.filter((user) => !seedIds.has(user.id));
  return [...withSeeds, ...extras];
}

function dedupeUsers(list: UserAccount[]) {
  const seenEmails = new Set<string>();
  const seenIds = new Set<string>();
  const result: UserAccount[] = [];
  for (const user of list) {
    const email = normalizeEmail(user.email);
    if (seenIds.has(user.id) || seenEmails.has(email)) continue;
    seenIds.add(user.id);
    seenEmails.add(email);
    result.push({ ...user, email });
  }
  return result;
}

function normalizeUsers(value: unknown) {
  if (!Array.isArray(value)) return seedUsers;
  const normalized = value.map(normalizeUser).filter((user): user is UserAccount => !!user);
  const merged = dedupeUsers(mergeWithSeed(normalized));
  return merged.map((user) => ({ ...user, isMainAdmin: user.id === "u_admin" }));
}

function load(): UserAccount[] {
  if (typeof window === "undefined") return seedUsers;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedUsers;
    return normalizeUsers(JSON.parse(raw));
  } catch {
    return seedUsers;
  }
}

function save(list: UserAccount[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    return true;
  } catch {
    return false;
  }
}

type Sub = (list: UserAccount[]) => void;
const subs = new Set<Sub>();
let cache: UserAccount[] | null = null;

export function getUsers() {
  if (!cache) cache = load();
  return cache;
}

function setUsers(next: UserAccount[]) {
  const previous = cache;
  cache = dedupeUsers(mergeWithSeed(next)).map((user) => ({
    ...user,
    isMainAdmin: user.id === "u_admin",
  }));
  const ok = save(cache);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? seedUsers));
  return ok;
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

function validateEmailUnique(email: string, excludeId?: string, source = getUsers()) {
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

export function getLeaderRoleRemovalBlock(userId: string, nextRoles: Role[], projects: Project[]) {
  if (nextRoles.includes("leader")) return null;
  const assigned = projects.filter((project) => project.leaderIds.includes(userId));
  if (assigned.length === 0) return null;
  return `This user is still assigned as a Leader on ${assigned.length} project${
    assigned.length === 1 ? "" : "s"
  }. Reassign those projects before removing the Leader role.`;
}

export function createUser(actor: UserActor, draft: UserDraft): Result {
  const cleaned = cleanDraft(draft);
  const roleError = validateRolesForActor(actor, null, cleaned.roles);
  if (roleError) return { ok: false, error: roleError };
  if (!cleaned.fullName) return { ok: false, error: "Full name is required." };
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleaned.email)) {
    return { ok: false, error: "Email is invalid." };
  }
  if (!validateEmailUnique(cleaned.email)) return { ok: false, error: "Email already exists." };
  if (!statuses.includes(cleaned.status)) return { ok: false, error: "Invalid account status." };
  const now = new Date().toISOString();
  const user: UserAccount = {
    id: makeId(),
    email: cleaned.email,
    fullName: cleaned.fullName,
    initials: makeInitials(cleaned.fullName),
    title: cleaned.title,
    roles: cleaned.roles,
    status: cleaned.status,
    isMainAdmin: false,
    createdAt: now,
    updatedAt: now,
  };
  const ok = setUsers([user, ...getUsers()]);
  return ok ? { ok: true, value: user } : { ok: false, error: "User could not be saved." };
}

export function updateUser(actor: UserActor, userId: string, patch: UserUpdate): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  const fullName = patch.fullName.trim();
  const email = normalizeEmail(patch.email);
  if (!fullName) return { ok: false, error: "Full name is required." };
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return { ok: false, error: "Email is invalid." };
  if (!validateEmailUnique(email, userId)) return { ok: false, error: "Email already exists." };
  const next = {
    ...current,
    fullName,
    email,
    title: patch.title.trim(),
    initials: makeInitials(fullName),
    updatedAt: new Date().toISOString(),
  };
  const ok = setUsers(getUsers().map((user) => (user.id === userId ? next : user)));
  return ok ? { ok: true, value: next } : { ok: false, error: "User update failed." };
}

export function updateUserRoles(
  actor: UserActor,
  userId: string,
  nextRoles: Role[],
  projects: Project[],
): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  const cleanedRoles = Array.from(new Set(nextRoles));
  const roleError = validateRolesForActor(actor, current, cleanedRoles);
  if (roleError) return { ok: false, error: roleError };
  const leaderBlock = getLeaderRoleRemovalBlock(userId, cleanedRoles, projects);
  if (leaderBlock) return { ok: false, error: leaderBlock };
  const next = { ...current, roles: cleanedRoles, updatedAt: new Date().toISOString() };
  const ok = setUsers(getUsers().map((user) => (user.id === userId ? next : user)));
  return ok ? { ok: true, value: next } : { ok: false, error: "Role update failed." };
}

export function lockUser(actor: UserActor, userId: string): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be locked." };
  if (current.id === actor.id) return { ok: false, error: "Admins cannot lock their own account." };
  if (current.status === "locked") return { ok: true, value: current };
  const next = {
    ...current,
    status: "locked" as AccountStatus,
    updatedAt: new Date().toISOString(),
  };
  const ok = setUsers(getUsers().map((user) => (user.id === userId ? next : user)));
  return ok ? { ok: true, value: next } : { ok: false, error: "Account lock failed." };
}

export function unlockUser(actor: UserActor, userId: string): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  if (current.status === "active") return { ok: true, value: current };
  const next = {
    ...current,
    status: "active" as AccountStatus,
    updatedAt: new Date().toISOString(),
  };
  const ok = setUsers(getUsers().map((user) => (user.id === userId ? next : user)));
  return ok ? { ok: true, value: next } : { ok: false, error: "Account unlock failed." };
}

export function resetUserPassword(actor: UserActor, userId: string): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  return { ok: true, value: current };
}

export function deleteUser(actor: UserActor, userId: string): Result {
  const current = getUserById(userId);
  if (!current) return { ok: false, error: "User not found." };
  const permissionError = canManageUser(actor, current);
  if (permissionError) return { ok: false, error: permissionError };
  if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be deleted." };
  if (current.id === actor.id)
    return { ok: false, error: "Admins cannot delete their own account." };
  const ok = setUsers(getUsers().filter((user) => user.id !== userId));
  return ok ? { ok: true, value: current } : { ok: false, error: "User deletion failed." };
}

export function recordLogin(userId: string) {
  const current = getUserById(userId);
  if (!current) return false;
  const next = { ...current, lastLoginAt: new Date().toISOString(), updatedAt: current.updatedAt };
  return setUsers(getUsers().map((user) => (user.id === userId ? next : user)));
}

export function resetUsers() {
  return setUsers(seedUsers);
}

export function clearSessionIfUser(userId: string) {
  if (typeof window === "undefined") return;
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    if (
      isRecord(parsed) &&
      (readString(parsed.userId) === userId || readString(parsed.id) === userId)
    ) {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    }
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY);
  }
}

export function useUsers() {
  const [users, setList] = useState<UserAccount[]>(() => getUsers());
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const remoteEnabled = isAdminUsersApiConfigured();

  useEffect(() => subscribeToUsers(setList), []);

  const refresh = useCallback(
    async (actor?: UserActor): Promise<Result<UserAccount[]>> => {
      if (!remoteEnabled) {
        const next = getUsers();
        setList(next);
        setLoadError(null);
        return { ok: true, value: next };
      }
      setLoading(true);
      setLoadError(null);
      try {
        const remoteUsers = preserveLocalDisplayFields(await fetchAdminUsers(actor));
        setList(remoteUsers);
        return { ok: true, value: remoteUsers };
      } catch (error) {
        const message = getErrorMessage(error);
        setLoadError(message);
        return { ok: false, error: message };
      } finally {
        setLoading(false);
      }
    },
    [remoteEnabled],
  );

  const create = useCallback(
    async (actor: UserActor, draft: UserDraft): Promise<Result> => {
      if (!remoteEnabled) return createUser(actor, draft);
      const cleaned = cleanDraft(draft);
      const roleError = validateRolesForActor(actor, null, cleaned.roles);
      if (roleError) return { ok: false, error: roleError };
      if (!cleaned.fullName) return { ok: false, error: "Full name is required." };
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleaned.email)) {
        return { ok: false, error: "Email is invalid." };
      }
      if (!validateEmailUnique(cleaned.email, undefined, users)) {
        return { ok: false, error: "Email already exists." };
      }
      const labId = actor.labId ?? users.find((user) => user.labId)?.labId;
      if (!labId) {
        return {
          ok: false,
          error: "Backend lab ID is unavailable. Refresh users before creating.",
        };
      }
      try {
        const created = await createAdminUser(actor, cleaned, labId);
        const value = preserveLocalDisplayField(created, cleaned);
        setList((current) => [value, ...current.filter((user) => user.id !== value.id)]);
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );

  const update = useCallback(
    async (actor: UserActor, userId: string, patch: UserUpdate): Promise<Result> => {
      if (!remoteEnabled) return updateUser(actor, userId, patch);
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      const fullName = patch.fullName.trim();
      const email = normalizeEmail(patch.email);
      if (!fullName) return { ok: false, error: "Full name is required." };
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        return { ok: false, error: "Email is invalid." };
      if (!validateEmailUnique(email, userId, users))
        return { ok: false, error: "Email already exists." };
      try {
        const updated = await updateAdminUser(actor, userId, {
          fullName,
          email,
          title: patch.title.trim(),
        });
        const value = preserveLocalDisplayField(updated, {
          fullName,
          email,
          title: patch.title.trim(),
          roles: updated.roles,
          status: updated.status,
        });
        setList((currentList) => currentList.map((user) => (user.id === userId ? value : user)));
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );
  const updateRoles = useCallback(
    async (
      actor: UserActor,
      userId: string,
      nextRoles: Role[],
      projects: Project[],
    ): Promise<Result> => {
      if (!remoteEnabled) return updateUserRoles(actor, userId, nextRoles, projects);
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
        const roles = await updateAdminUserRoles(actor, userId, cleanedRoles);
        const value = { ...current, roles, updatedAt: new Date().toISOString() };
        setList((currentList) => currentList.map((user) => (user.id === userId ? value : user)));
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );
  const lock = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      if (!remoteEnabled) return lockUser(actor, userId);
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be locked." };
      if (current.id === actor.id)
        return { ok: false, error: "Admins cannot lock their own account." };
      try {
        const value = preserveLocalDisplayField(await lockAdminUser(actor, userId), current);
        setList((currentList) => currentList.map((user) => (user.id === userId ? value : user)));
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );
  const unlock = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      if (!remoteEnabled) return unlockUser(actor, userId);
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      try {
        const value = preserveLocalDisplayField(await unlockAdminUser(actor, userId), current);
        setList((currentList) => currentList.map((user) => (user.id === userId ? value : user)));
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );
  const resetPassword = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      if (!remoteEnabled) return resetUserPassword(actor, userId);
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      try {
        const value = preserveLocalDisplayField(
          await resetAdminUserPassword(actor, userId),
          current,
        );
        setList((currentList) => currentList.map((user) => (user.id === userId ? value : user)));
        return { ok: true, value };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
  );
  const remove = useCallback(
    async (actor: UserActor, userId: string): Promise<Result> => {
      if (!remoteEnabled) return deleteUser(actor, userId);
      const current = users.find((user) => user.id === userId);
      if (!current) return { ok: false, error: "User not found." };
      const permissionError = canManageUser(actor, current);
      if (permissionError) return { ok: false, error: permissionError };
      if (current.isMainAdmin) return { ok: false, error: "Main Admin cannot be deleted." };
      if (current.id === actor.id)
        return { ok: false, error: "Admins cannot delete their own account." };
      try {
        await deleteAdminUser(actor, userId);
        setList((currentList) => currentList.filter((user) => user.id !== userId));
        return { ok: true, value: current };
      } catch (error) {
        return { ok: false, error: getErrorMessage(error) };
      }
    },
    [remoteEnabled, users],
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
    resetUsers,
  };
}

function preserveLocalDisplayFields(users: UserAccount[]) {
  return users.map((user) => preserveLocalDisplayField(user));
}

function preserveLocalDisplayField(user: UserAccount, fallback?: Partial<UserDraft | UserAccount>) {
  const local = getUsers().find((item) => item.id === user.id || item.email === user.email);
  const title = fallback?.title ?? local?.title ?? user.title;
  return {
    ...user,
    title,
    createdAt: user.createdAt || local?.createdAt || seedCreatedAt,
    updatedAt: user.updatedAt || local?.updatedAt || seedCreatedAt,
    lastLoginAt: user.lastLoginAt ?? local?.lastLoginAt,
  };
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Request failed.";
}
