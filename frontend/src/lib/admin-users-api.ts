import type {
  AccountStatus,
  Role,
  UserAccount,
  UserActor,
  UserDraft,
  UserUpdate,
} from "@/lib/users-data";

type BackendAccountStatus = "ACTIVE" | "LOCKED" | "PENDING" | "DELETED";
type BackendRoleCode = "SUPER_ADMIN" | "ADMIN" | "LEADER" | "MEMBER";

type BackendUserResponse = {
  id: string;
  labId: string;
  username: string;
  email: string;
  fullName: string;
  avatarFileId: string | null;
  accountStatus: BackendAccountStatus;
};

type BackendRoleCatalogResponse = {
  id: string;
  code: string;
  name: string;
  description: string;
};

type BackendUserRoleResponse = {
  assignmentId?: string;
  id?: string;
  roleId?: string;
  code?: string;
  roleCode?: string;
  name?: string;
  roleName?: string;
  status?: "ACTIVE" | "INACTIVE";
};

type RequestOptions = {
  method?: string;
  body?: unknown;
};

type RoleCatalogItem = {
  id: string;
  code: BackendRoleCode;
};

const DEMO_PASSWORD = "smart2026";
const DEMO_PASSWORD_HASH = "$2a$10$d7kQAwLf4KgqyRAueDTGc.gmv2ubaO6PidzJzDvMO8p3IfRjQi.wC";

let roleCatalogCache: RoleCatalogItem[] | null = null;

export function isAdminUsersApiConfigured() {
  return getApiBaseUrl() !== null;
}

export async function fetchAdminUsers(actor?: UserActor): Promise<UserAccount[]> {
  const [activeUsers, lockedUsers] = await Promise.all([
    request<unknown[]>("/api/admin/users?status=ACTIVE", {}, actor),
    request<unknown[]>("/api/admin/users?status=LOCKED", {}, actor),
  ]);
  const users = [...activeUsers, ...lockedUsers].filter(isBackendUserResponse);
  const uniqueUsers = dedupeById(users);
  const usersWithRoles = await Promise.all(
    uniqueUsers.map(async (user) => {
      const roleCodes = await fetchBackendRoleCodesForUser(user.id, actor);
      return mapUser(user, roleCodes);
    }),
  );
  return usersWithRoles;
}

export async function createAdminUser(
  actor: UserActor | undefined,
  draft: UserDraft,
  labId: string,
): Promise<UserAccount> {
  const created = await request<unknown>(
    "/api/admin/users",
    {
      method: "POST",
      body: {
        labId,
        username: makeUsername(draft.email),
        email: draft.email,
        passwordHash: DEMO_PASSWORD_HASH,
        fullName: draft.fullName,
        avatarFileId: null,
      },
    },
    actor,
  );
  if (!isBackendUserResponse(created)) {
    throw new Error("Backend returned an invalid user response.");
  }
  await updateAdminUserRoles(actor, created.id, draft.roles);
  if (draft.status === "locked") {
    await request<unknown>(
      `/api/admin/users/${encodeURIComponent(created.id)}/lock`,
      { method: "PATCH" },
      actor,
    );
  }
  return fetchAdminUser(actor, created.id);
}

export async function updateAdminUser(
  actor: UserActor | undefined,
  userId: string,
  patch: UserUpdate,
): Promise<UserAccount> {
  const current = await fetchAdminUser(actor, userId);
  const updated = await request<unknown>(
    `/api/admin/users/${encodeURIComponent(userId)}`,
    {
      method: "PUT",
      body: {
        username: current.username ?? makeUsername(patch.email),
        email: patch.email,
        fullName: patch.fullName,
        avatarFileId: null,
        clearAvatarFile: false,
      },
    },
    actor,
  );
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId, actor);
  return mapUser(updated, roleCodes);
}

export async function updateAdminUserRoles(
  actor: UserActor | undefined,
  userId: string,
  nextRoles: Role[],
): Promise<Role[]> {
  const [catalog, currentRoles] = await Promise.all([
    fetchRoleCatalog(actor),
    fetchBackendRolesForUser(userId, actor),
  ]);
  const desiredCodes = new Set(nextRoles.map(mapFrontendRole));
  const currentManagedCodes = new Set(
    currentRoles
      .map((role) => normalizeBackendRoleCode(role.code ?? role.roleCode))
      .filter((code): code is BackendRoleCode => !!code && code !== "SUPER_ADMIN"),
  );

  const requests: Promise<unknown>[] = [];
  for (const desiredCode of desiredCodes) {
    if (currentManagedCodes.has(desiredCode)) continue;
    const roleId = catalog.find((role) => role.code === desiredCode)?.id;
    if (!roleId) throw new Error(`Backend role ${desiredCode} was not found.`);
    requests.push(
      request(
        `/api/admin/users/${encodeURIComponent(userId)}/roles`,
        { method: "POST", body: { roleId } },
        actor,
      ),
    );
  }
  for (const currentCode of currentManagedCodes) {
    if (desiredCodes.has(currentCode)) continue;
    const roleId = catalog.find((role) => role.code === currentCode)?.id;
    if (!roleId) throw new Error(`Backend role ${currentCode} was not found.`);
    requests.push(
      request(
        `/api/admin/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`,
        { method: "DELETE" },
        actor,
      ),
    );
  }

  await Promise.all(requests);
  const updatedRoleCodes = await fetchBackendRoleCodesForUser(userId, actor);
  return mapRoleCodes(updatedRoleCodes);
}

export async function lockAdminUser(actor: UserActor | undefined, userId: string) {
  return changeAdminUserStatus(actor, userId, "lock");
}

export async function unlockAdminUser(actor: UserActor | undefined, userId: string) {
  return changeAdminUserStatus(actor, userId, "unlock");
}

export async function resetAdminUserPassword(actor: UserActor | undefined, userId: string) {
  const updated = await request<unknown>(
    `/api/admin/users/${encodeURIComponent(userId)}/reset-password`,
    { method: "PATCH", body: { passwordHash: DEMO_PASSWORD_HASH } },
    actor,
  );
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId, actor);
  return mapUser(updated, roleCodes);
}

export async function deleteAdminUser(actor: UserActor | undefined, userId: string) {
  await request(`/api/admin/users/${encodeURIComponent(userId)}`, { method: "DELETE" }, actor);
}

async function changeAdminUserStatus(
  actor: UserActor | undefined,
  userId: string,
  action: "lock" | "unlock",
) {
  const updated = await request<unknown>(
    `/api/admin/users/${encodeURIComponent(userId)}/${action}`,
    { method: "PATCH" },
    actor,
  );
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId, actor);
  return mapUser(updated, roleCodes);
}

async function fetchAdminUser(actor: UserActor | undefined, userId: string) {
  const user = await request<unknown>(`/api/admin/users/${encodeURIComponent(userId)}`, {}, actor);
  if (!isBackendUserResponse(user)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId, actor);
  return mapUser(user, roleCodes);
}

async function fetchRoleCatalog(actor?: UserActor) {
  if (roleCatalogCache) return roleCatalogCache;
  const response = await request<unknown[]>("/api/admin/roles", {}, actor);
  roleCatalogCache = response
    .filter(isBackendRoleCatalogResponse)
    .map((role) => ({ id: role.id, code: normalizeBackendRoleCode(role.code) }))
    .filter((role): role is RoleCatalogItem => !!role.code);
  return roleCatalogCache;
}

async function fetchBackendRolesForUser(userId: string, actor?: UserActor) {
  const response = await request<unknown[]>(
    `/api/admin/users/${encodeURIComponent(userId)}/roles`,
    {},
    actor,
  );
  return response
    .filter(isBackendUserRoleResponse)
    .filter((role) => !role.status || role.status === "ACTIVE");
}

async function fetchBackendRoleCodesForUser(userId: string, actor?: UserActor) {
  const roles = await fetchBackendRolesForUser(userId, actor);
  return roles
    .map((role) => normalizeBackendRoleCode(role.code ?? role.roleCode))
    .filter((code): code is BackendRoleCode => !!code);
}

async function request<T>(
  path: string,
  options: RequestOptions = {},
  actor?: UserActor,
): Promise<T> {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) throw new Error("VITE_API_BASE_URL is not configured.");
  const headers = new Headers();
  headers.set("Accept", "application/json");
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (actor?.email) {
    headers.set("Authorization", `Basic ${encodeBasic(`${actor.email}:${DEMO_PASSWORD}`)}`);
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
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
  return `Request failed (${response.status}).`;
}

function getApiBaseUrl() {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (!raw || !raw.trim()) return null;
  return raw.trim().replace(/\/+$/, "");
}

function encodeBasic(value: string) {
  return btoa(unescape(encodeURIComponent(value)));
}

function mapUser(user: BackendUserResponse, roleCodes: BackendRoleCode[]): UserAccount {
  return {
    id: user.id,
    email: user.email.trim().toLowerCase(),
    fullName: user.fullName,
    initials: makeInitials(user.fullName),
    title: "",
    roles: mapRoleCodes(roleCodes),
    status: mapAccountStatus(user.accountStatus),
    isMainAdmin: roleCodes.includes("SUPER_ADMIN"),
    createdAt: "",
    updatedAt: "",
    username: user.username,
    labId: user.labId,
    avatarFileId: user.avatarFileId,
  };
}

function mapRoleCodes(roleCodes: BackendRoleCode[]) {
  return Array.from(
    new Set(roleCodes.map(mapBackendRole).filter((role): role is Role => role !== null)),
  );
}

function mapBackendRole(roleCode: BackendRoleCode): Role | null {
  if (roleCode === "ADMIN") return "admin";
  if (roleCode === "LEADER") return "leader";
  if (roleCode === "MEMBER") return "member";
  return null;
}

function mapFrontendRole(role: Role): BackendRoleCode {
  if (role === "admin") return "ADMIN";
  if (role === "leader") return "LEADER";
  return "MEMBER";
}

function mapAccountStatus(status: BackendAccountStatus): AccountStatus {
  return status === "LOCKED" ? "locked" : "active";
}

function makeInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function makeUsername(email: string) {
  return (
    email
      .trim()
      .toLowerCase()
      .split("@")[0]
      ?.replace(/[^a-z0-9._-]/g, "") || "user"
  );
}

function dedupeById(users: BackendUserResponse[]) {
  const seen = new Set<string>();
  const result: BackendUserResponse[] = [];
  for (const user of users) {
    if (seen.has(user.id)) continue;
    seen.add(user.id);
    result.push(user);
  }
  return result;
}

function normalizeBackendRoleCode(value: unknown): BackendRoleCode | null {
  const code = readString(value).toUpperCase();
  if (code === "SUPER_ADMIN" || code === "ADMIN" || code === "LEADER" || code === "MEMBER") {
    return code;
  }
  return null;
}

function isBackendUserResponse(value: unknown): value is BackendUserResponse {
  if (!isRecord(value)) return false;
  const status = readString(value.accountStatus).toUpperCase();
  return (
    !!readString(value.id) &&
    !!readString(value.labId) &&
    !!readString(value.email) &&
    !!readString(value.fullName) &&
    (status === "ACTIVE" || status === "LOCKED" || status === "PENDING" || status === "DELETED")
  );
}

function isBackendRoleCatalogResponse(value: unknown): value is BackendRoleCatalogResponse {
  return isRecord(value) && !!readString(value.id) && !!readString(value.code);
}

function isBackendUserRoleResponse(value: unknown): value is BackendUserRoleResponse {
  if (!isRecord(value)) return false;
  const status = readString(value.status).toUpperCase();
  const hasRoleId = !!readString(value.roleId) || !!readString(value.id);
  const hasRoleCode = !!readString(value.code) || !!readString(value.roleCode);
  return hasRoleId && hasRoleCode && (!status || status === "ACTIVE" || status === "INACTIVE");
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}
