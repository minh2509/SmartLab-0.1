import type { Role, UserAccount } from "@/lib/users-data";

type BackendAccountStatus = "ACTIVE" | "LOCKED" | "PENDING" | "DELETED";
type BackendRoleCode = "SUPER_ADMIN" | "ADMIN" | "LEADER" | "MEMBER";

export type BackendSession = {
  authorization: string;
  user: UserAccount;
  activeRole: Role;
};

type BackendCurrentUserResponse = {
  id: string;
  labId: string;
  email: string;
  fullName: string;
  accountStatus: BackendAccountStatus;
  roleCodes: string[];
};

type RequestOptions = {
  method?: string;
  body?: unknown;
  authorization?: string | null;
  signal?: AbortSignal;
};

const SESSION_STORAGE_KEY = "smart.session.v1";

export function getApiBaseUrl() {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (!raw || !raw.trim()) return null;
  return raw.trim().replace(/\/+$/, "");
}

export function isBackendConfigured() {
  return getApiBaseUrl() !== null;
}

export function readStoredSession(): BackendSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (!isRecord(parsed)) return null;
    const authorization = readString(parsed.authorization);
    const user = normalizeStoredUser(parsed.user);
    const activeRole = readString(parsed.activeRole) as Role;
    if (!authorization || !user || !user.roles.includes(activeRole)) return null;
    return { authorization, user, activeRole };
  } catch {
    return null;
  }
}

export function persistSession(session: BackendSession | null) {
  if (typeof window === "undefined") return;
  try {
    if (session) localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(SESSION_STORAGE_KEY);
  } catch {
    // Ignore storage failures; the in-memory auth state still works for this render.
  }
}

export function clearStoredSession() {
  persistSession(null);
}

export function getStoredAuthorizationHeader() {
  return readStoredSession()?.authorization ?? null;
}

export async function signInWithBackend(email: string, password: string) {
  const authorization = `Basic ${encodeBasic(`${email.trim().toLowerCase()}:${password}`)}`;
  const user = await backendRequest<BackendCurrentUserResponse>("/api/auth/me", { authorization });
  const mappedUser = mapCurrentUser(user);
  if (mappedUser.roles.length === 0) {
    throw new Error("This account does not have an active workspace role.");
  }
  return {
    authorization,
    user: mappedUser,
    activeRole: mappedUser.roles[0],
  } satisfies BackendSession;
}

export async function refreshCurrentUser(authorization: string) {
  return mapCurrentUser(
    await backendRequest<BackendCurrentUserResponse>("/api/auth/me", { authorization }),
  );
}

export async function backendRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) throw new Error("VITE_API_BASE_URL is not configured.");
  const authorization = options.authorization ?? getStoredAuthorizationHeader();
  if (!authorization) throw new Error("Please sign in before using backend data.");

  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("Authorization", authorization);
  if (options.body !== undefined) headers.set("Content-Type", "application/json");

  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export function mapBackendRole(roleCode: string): Role | null {
  const normalized = roleCode.toUpperCase();
  if (normalized === "ADMIN") return "admin";
  if (normalized === "LEADER") return "leader";
  if (normalized === "MEMBER") return "member";
  return null;
}

export function mapFrontendRole(role: Role): BackendRoleCode {
  if (role === "admin") return "ADMIN";
  if (role === "leader") return "LEADER";
  return "MEMBER";
}

export function normalizeBackendRoleCode(value: unknown): BackendRoleCode | null {
  const code = readString(value).toUpperCase();
  if (code === "SUPER_ADMIN" || code === "ADMIN" || code === "LEADER" || code === "MEMBER") {
    return code;
  }
  return null;
}

export function mapRoleCodes(roleCodes: string[]) {
  return Array.from(
    new Set(roleCodes.map(mapBackendRole).filter((role): role is Role => role !== null)),
  );
}

export function makeInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

async function readErrorMessage(response: Response) {
  try {
    const value = (await response.json()) as unknown;
    if (isRecord(value)) {
      return (
        readString(value.message) ||
        readString(value.error) ||
        `Request failed (${response.status}).`
      );
    }
  } catch {
    // Fall through to the generic message.
  }
  if (response.status === 401) return "Invalid email or password.";
  if (response.status === 403) return "This account is not authorized for that action.";
  return `Request failed (${response.status}).`;
}

function mapCurrentUser(user: BackendCurrentUserResponse): UserAccount {
  const roleCodes = user.roleCodes
    .map(normalizeBackendRoleCode)
    .filter((code): code is BackendRoleCode => !!code);
  return {
    id: user.id,
    email: user.email.trim().toLowerCase(),
    fullName: user.fullName,
    initials: makeInitials(user.fullName),
    title: "",
    roles: mapRoleCodes(roleCodes),
    status: user.accountStatus === "LOCKED" ? "locked" : "active",
    isMainAdmin: roleCodes.includes("SUPER_ADMIN"),
    createdAt: "",
    updatedAt: "",
    labId: user.labId,
  };
}

function normalizeStoredUser(value: unknown): UserAccount | null {
  if (!isRecord(value)) return null;
  const id = readString(value.id);
  const email = readString(value.email).trim().toLowerCase();
  const fullName = readString(value.fullName);
  const roles = Array.isArray(value.roles)
    ? value.roles.filter(
        (role): role is Role => role === "admin" || role === "leader" || role === "member",
      )
    : [];
  if (!id || !email || !fullName || roles.length === 0) return null;
  return {
    id,
    email,
    fullName,
    initials: readString(value.initials) || makeInitials(fullName),
    title: readString(value.title),
    roles,
    status: readString(value.status) === "locked" ? "locked" : "active",
    isMainAdmin: Boolean(value.isMainAdmin),
    createdAt: readString(value.createdAt),
    updatedAt: readString(value.updatedAt),
    lastLoginAt: readString(value.lastLoginAt) || undefined,
    username: readString(value.username) || undefined,
    labId: readString(value.labId) || undefined,
    avatarFileId: readString(value.avatarFileId) || null,
  };
}

function encodeBasic(value: string) {
  return btoa(unescape(encodeURIComponent(value)));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}
