import type { AccountStatus, Role, UserAccount, UserDraft, UserUpdate } from "@/lib/users-data";
import {
  backendRequest,
  isBackendConfigured,
  makeInitials,
  mapFrontendRole,
  mapRoleCodes,
  normalizeBackendRoleCode,
} from "@/lib/backend-api";

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

let roleCatalogCache: RoleCatalogItem[] | null = null;

export function isAdminUsersApiConfigured() {
  return isBackendConfigured();
}

export async function fetchAdminUsers(): Promise<UserAccount[]> {
  const [activeUsers, lockedUsers] = await Promise.all([
    request<unknown[]>("/api/admin/users?status=ACTIVE"),
    request<unknown[]>("/api/admin/users?status=LOCKED"),
  ]);
  const users = [...activeUsers, ...lockedUsers].filter(isBackendUserResponse);
  const uniqueUsers = dedupeById(users);
  const usersWithRoles = await Promise.all(
    uniqueUsers.map(async (user) => {
      const roleCodes = await fetchBackendRoleCodesForUser(user.id);
      return mapUser(user, roleCodes);
    }),
  );
  return usersWithRoles;
}

export async function createAdminUser(draft: UserDraft, labId: string): Promise<UserAccount> {
  const created = await request<unknown>("/api/admin/users", {
    method: "POST",
    body: {
      labId,
      username: makeUsername(draft.email),
      email: draft.email,
      password: draft.password,
      fullName: draft.fullName,
      avatarFileId: null,
    },
  });
  if (!isBackendUserResponse(created)) {
    throw new Error("Backend returned an invalid user response.");
  }
  await updateAdminUserRoles(created.id, draft.roles);
  if (draft.status === "locked") {
    await request<unknown>(`/api/admin/users/${encodeURIComponent(created.id)}/lock`, {
      method: "PATCH",
    });
  }
  return fetchAdminUser(created.id);
}

export async function updateAdminUser(userId: string, patch: UserUpdate): Promise<UserAccount> {
  const current = await fetchAdminUser(userId);
  const updated = await request<unknown>(`/api/admin/users/${encodeURIComponent(userId)}`, {
    method: "PUT",
    body: {
      username: current.username ?? makeUsername(patch.email),
      email: patch.email,
      fullName: patch.fullName,
      avatarFileId: null,
      clearAvatarFile: false,
    },
  });
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId);
  return mapUser(updated, roleCodes);
}

export async function updateAdminUserRoles(userId: string, nextRoles: Role[]): Promise<Role[]> {
  const [catalog, currentRoles] = await Promise.all([
    fetchRoleCatalog(),
    fetchBackendRolesForUser(userId),
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
      request(`/api/admin/users/${encodeURIComponent(userId)}/roles`, {
        method: "POST",
        body: { roleId },
      }),
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
      ),
    );
  }

  await Promise.all(requests);
  const updatedRoleCodes = await fetchBackendRoleCodesForUser(userId);
  return mapRoleCodes(updatedRoleCodes);
}

export async function lockAdminUser(userId: string) {
  return changeAdminUserStatus(userId, "lock");
}

export async function unlockAdminUser(userId: string) {
  return changeAdminUserStatus(userId, "unlock");
}

export async function resetAdminUserPassword(userId: string, password: string) {
  const updated = await request<unknown>(
    `/api/admin/users/${encodeURIComponent(userId)}/reset-password`,
    { method: "PATCH", body: { password } },
  );
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId);
  return mapUser(updated, roleCodes);
}

export async function deleteAdminUser(userId: string) {
  await request(`/api/admin/users/${encodeURIComponent(userId)}`, { method: "DELETE" });
}

async function changeAdminUserStatus(userId: string, action: "lock" | "unlock") {
  const updated = await request<unknown>(
    `/api/admin/users/${encodeURIComponent(userId)}/${action}`,
    { method: "PATCH" },
  );
  if (!isBackendUserResponse(updated)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId);
  return mapUser(updated, roleCodes);
}

async function fetchAdminUser(userId: string) {
  const user = await request<unknown>(`/api/admin/users/${encodeURIComponent(userId)}`);
  if (!isBackendUserResponse(user)) {
    throw new Error("Backend returned an invalid user response.");
  }
  const roleCodes = await fetchBackendRoleCodesForUser(userId);
  return mapUser(user, roleCodes);
}

async function fetchRoleCatalog() {
  if (roleCatalogCache) return roleCatalogCache;
  const response = await request<unknown[]>("/api/admin/roles");
  roleCatalogCache = response
    .filter(isBackendRoleCatalogResponse)
    .map((role) => ({ id: role.id, code: normalizeBackendRoleCode(role.code) }))
    .filter((role): role is RoleCatalogItem => !!role.code);
  return roleCatalogCache;
}

async function fetchBackendRolesForUser(userId: string) {
  const response = await request<unknown[]>(`/api/admin/users/${encodeURIComponent(userId)}/roles`);
  return response
    .filter(isBackendUserRoleResponse)
    .filter((role) => !role.status || role.status === "ACTIVE");
}

async function fetchBackendRoleCodesForUser(userId: string) {
  const roles = await fetchBackendRolesForUser(userId);
  return roles
    .map((role) => normalizeBackendRoleCode(role.code ?? role.roleCode))
    .filter((code): code is BackendRoleCode => !!code);
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return backendRequest<T>(path, options);
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

function mapAccountStatus(status: BackendAccountStatus): AccountStatus {
  return status === "LOCKED" ? "locked" : "active";
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
