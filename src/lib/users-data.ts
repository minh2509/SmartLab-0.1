import { useCallback, useEffect, useState } from "react";
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
  roles: Role[];
  isMainAdmin: boolean;
};

type Result<T = UserAccount> = { ok: true; value: T } | { ok: false; error: string };

const STORAGE_KEY = "nova.users.v1";
const SESSION_STORAGE_KEY = "nova.session.v1";
const roles: Role[] = ["admin", "leader", "member"];
const statuses: AccountStatus[] = ["active", "locked"];
const seedCreatedAt = "2026-01-01T00:00:00.000Z";

export const DEMO_PASSWORD = "nova2026";

export const roleLabel: Record<Role, string> = {
  admin: "Admin",
  leader: "Project Leader",
  member: "Member",
};

export const seedUsers: UserAccount[] = [
  {
    id: "u_admin",
    email: "admin@nova.lab",
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
    email: "amara@nova.lab",
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
    email: "tran@nova.lab",
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
    email: "linh@nova.lab",
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
  if (actor.id === target.id) return "Admins cannot modify their own account through this UI.";
  if (actor.isMainAdmin) return null;
  if (target.isMainAdmin) return "Regular Admins cannot modify the Main Admin.";
  if (target.roles.includes("admin")) return "Regular Admins cannot modify Admin accounts.";
  return null;
}

function validateEmailUnique(email: string, excludeId?: string) {
  return !getUsers().some((user) => user.id !== excludeId && user.email === normalizeEmail(email));
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
  if (target?.id === actor.id) return "Admins cannot remove their own role through this UI.";
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
  useEffect(() => subscribeToUsers(setList), []);

  const create = useCallback((actor: UserActor, draft: UserDraft) => createUser(actor, draft), []);
  const update = useCallback(
    (actor: UserActor, userId: string, patch: UserUpdate) => updateUser(actor, userId, patch),
    [],
  );
  const updateRoles = useCallback(
    (actor: UserActor, userId: string, nextRoles: Role[], projects: Project[]) =>
      updateUserRoles(actor, userId, nextRoles, projects),
    [],
  );
  const lock = useCallback((actor: UserActor, userId: string) => lockUser(actor, userId), []);
  const unlock = useCallback((actor: UserActor, userId: string) => unlockUser(actor, userId), []);

  return { users, create, update, updateRoles, lock, unlock, resetUsers };
}
