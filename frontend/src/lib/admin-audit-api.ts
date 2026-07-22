import { backendRequest, getApiBaseUrl } from "@/lib/backend-api";

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

function buildAdminUrl(
  path: string,
  params: Record<string, string | number | boolean | undefined>,
) {
  const apiBaseUrl = getApiBaseUrl();
  if (!apiBaseUrl) throw new AdminAuditApiError("VITE_API_BASE_URL is not configured.");
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
  try {
    return await backendRequest<AdminPageResponse<T>>(urlPathWithParams(path, params), { signal });
  } catch (error) {
    throw new AdminAuditApiError(error instanceof Error ? error.message : "Request failed.");
  }
}

function urlPathWithParams(
  path: string,
  params: Record<string, string | number | boolean | undefined>,
) {
  const url = buildAdminUrl(path, params);
  return `${url.pathname}${url.search}`;
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
