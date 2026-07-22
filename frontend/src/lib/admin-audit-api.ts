import { DEMO_PASSWORD, getUserById } from "@/lib/users-data";

export type AdminPageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminAuditLogResponse = {
  id: string;
  labId: string | null;
  actorId: string | null;
  actorEmail: string | null;
  actorFullName: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
};

export type AdminLoginHistoryResponse = {
  id: string;
  userId: string | null;
  userEmail: string | null;
  userFullName: string | null;
  loginAt: string;
  ipAddress: string | null;
  userAgent: string | null;
  success: boolean | null;
  failureReason: string | null;
};

export type AdminAuditLogQuery = {
  action?: string;
  actorId?: string;
  entityType?: string;
  entityId?: string;
  start?: string;
  end?: string;
  page: number;
  size: number;
};

export type AdminLoginHistoryQuery = {
  userId?: string;
  success?: boolean;
  ipAddress?: string;
  start?: string;
  end?: string;
  page: number;
  size: number;
};

export class AdminAuditApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "AdminAuditApiError";
    this.status = status;
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
const sessionStorageKey = "smart.session.v1";

function readDemoBasicAuthHeader() {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(sessionStorageKey);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (!isRecord(parsed)) return null;
    const userId = readString(parsed.userId) || readString(parsed.id);
    const activeRole = readString(parsed.activeRole);
    const user = userId ? getUserById(userId) : null;
    if (!user || user.status !== "active") return null;
    if (activeRole !== "admin" || !user.roles.includes("admin")) return null;
    return `Basic ${window.btoa(`${user.email}:${DEMO_PASSWORD}`)}`;
  } catch {
    return null;
  }
}

function buildAdminUrl(
  path: string,
  params: Record<string, string | number | boolean | undefined>,
) {
  const origin = typeof window === "undefined" ? "http://localhost" : window.location.origin;
  const url = new URL(`${apiBaseUrl}${path}`, origin);
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined) return;
    const text = String(value).trim();
    if (text) url.searchParams.set(key, text);
  });
  return url;
}

async function requestAdminPage<T>(
  path: string,
  params: Record<string, string | number | boolean | undefined>,
  signal?: AbortSignal,
) {
  const authorization = readDemoBasicAuthHeader();
  if (!authorization) {
    throw new AdminAuditApiError(
      "No local demo Admin session is available for HTTP Basic authentication.",
      null,
    );
  }

  const response = await fetch(buildAdminUrl(path, params), {
    headers: {
      Accept: "application/json",
      Authorization: authorization,
    },
    signal,
  });

  if (!response.ok) {
    throw new AdminAuditApiError(await readErrorMessage(response), response.status);
  }

  return (await response.json()) as AdminPageResponse<T>;
}

async function readErrorMessage(response: Response) {
  try {
    const body: unknown = await response.json();
    if (isRecord(body)) {
      const detail = readString(body.message) || readString(body.error) || readString(body.detail);
      if (detail) return detail;
    }
  } catch {
    // Fall back to the HTTP status line below.
  }
  if (response.status === 401) return "Backend rejected the demo Basic Auth credentials.";
  if (response.status === 403) return "Backend reports that this account is not authorized.";
  return `Request failed with HTTP ${response.status}.`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

export function fetchAdminAuditLogs(query: AdminAuditLogQuery, signal?: AbortSignal) {
  return requestAdminPage<AdminAuditLogResponse>(
    "/api/admin/audit-logs",
    {
      action: query.action,
      actorId: query.actorId,
      entityType: query.entityType,
      entityId: query.entityId,
      start: query.start,
      end: query.end,
      page: query.page,
      size: query.size,
    },
    signal,
  );
}

export function fetchAdminLoginHistories(query: AdminLoginHistoryQuery, signal?: AbortSignal) {
  return requestAdminPage<AdminLoginHistoryResponse>(
    "/api/admin/login-histories",
    {
      userId: query.userId,
      success: query.success,
      ipAddress: query.ipAddress,
      start: query.start,
      end: query.end,
      page: query.page,
      size: query.size,
    },
    signal,
  );
}

export function fetchAdminUserLoginHistories(
  userId: string,
  query: Omit<AdminLoginHistoryQuery, "userId">,
  signal?: AbortSignal,
) {
  return requestAdminPage<AdminLoginHistoryResponse>(
    `/api/admin/users/${encodeURIComponent(userId)}/login-histories`,
    {
      success: query.success,
      ipAddress: query.ipAddress,
      start: query.start,
      end: query.end,
      page: query.page,
      size: query.size,
    },
    signal,
  );
}

export function toStartOfDayIso(date: string) {
  return date ? new Date(`${date}T00:00:00.000`).toISOString() : undefined;
}

export function toEndOfDayIso(date: string) {
  return date ? new Date(`${date}T23:59:59.999`).toISOString() : undefined;
}

export function formatAuditDateTime(value: string | null | undefined) {
  if (!value) return "Not recorded";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function compactIdentifier(value: string | null | undefined) {
  if (!value) return "Not recorded";
  if (value.length <= 12) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

export function labelToken(value: string | null | undefined) {
  if (!value) return "Not recorded";
  return value
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}
