import { apiRequest } from "@/lib/api-client";

export type AdminPage<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminDashboard = {
  users: number;
  projects: number;
  posts: number;
  joinRequests: number;
  tasks: number;
  recentActivities: AdminRecentActivity[];
};

export type AdminRecentActivity = {
  id: string;
  action: string;
  entityType: string;
  entityId: string | null;
  actorId: string | null;
  actorName: string | null;
  createdAt: string;
};

export type AdminAccountStatus = "ACTIVE" | "LOCKED" | "PENDING" | "DELETED";

export type AdminUserOption = {
  id: string;
  fullName: string;
  email: string;
  accountStatus: AdminAccountStatus;
  roleCodes: string[];
};

export type AdminProjectOption = {
  id: string;
  code: string;
  name: string;
  slug: string;
  status: "PROPOSED" | "PREPARING" | "IN_PROGRESS" | "PAUSED" | "COMPLETED" | "CLOSED";
  activeRecipientCount: number;
};

export type AdminJoinRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type AdminJoinRequest = {
  id: string;
  project: {
    id: string;
    code: string;
    name: string;
    slug: string;
  };
  requester: {
    id: string;
    fullName: string;
    email: string;
  };
  desiredPosition: string | null;
  reason: string | null;
  skills: string | null;
  experience: string | null;
  introduction: string | null;
  cvFile: {
    id: string;
    originalName: string;
    mimeType: string | null;
    fileSize: number | null;
    visibility: "PUBLIC" | "LAB_INTERNAL" | "PROJECT_INTERNAL" | "PRIVATE";
  } | null;
  status: AdminJoinRequestStatus;
  reviewedBy: {
    id: string;
    fullName: string;
    email: string;
  } | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminJoinRequestFilters = {
  projectId?: string;
  status?: AdminJoinRequestStatus;
  requesterId?: string;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
};

export type AdminNotificationSummary = {
  id: string;
  title: string;
  message: string | null;
  notificationType: string;
  relatedType: string | null;
  relatedId: string | null;
  linkUrl: string | null;
  createdBy: {
    id: string;
    fullName: string;
  } | null;
  recipientCount: number;
  readCount: number;
  createdAt: string;
};

export type AdminNotificationDetail = AdminNotificationSummary & {
  recipients: Array<{
    userId: string;
    fullName: string;
    readAt: string | null;
    hiddenAt: string | null;
    createdAt: string;
  }>;
};

export type AdminNotificationFilters = {
  notificationType?: string;
  creatorId?: string;
  relatedType?: string;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
};

export type AdminNotificationFilterOptions = {
  notificationTypes: string[];
  creatableNotificationTypes: string[];
  relatedTypes: string[];
  creators: Array<{
    id: string;
    fullName: string;
    email: string;
    accountStatus: AdminAccountStatus;
  }>;
};

export type NotificationTargetType = "USER" | "PROJECT" | "LAB";

export type CreateAdminNotificationInput = {
  title: string;
  message: string | null;
  notificationType: string;
  targetType: NotificationTargetType;
  userIds: string[];
  projectId: string | null;
  relatedType: string | null;
  relatedId: string | null;
  linkUrl: string | null;
};

type JsonRecord = Record<string, unknown>;

function recordValue(value: unknown, context: string): JsonRecord {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value as JsonRecord;
}

function stringValue(record: JsonRecord, key: string, context: string): string {
  const value = record[key];
  if (typeof value !== "string" || !value) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function nullableStringValue(record: JsonRecord, key: string, context: string): string | null {
  const value = record[key];
  if (value === null || value === undefined) return null;
  if (typeof value !== "string") {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function numberValue(record: JsonRecord, key: string, context: string): number {
  const value = record[key];
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function nullableNumberValue(record: JsonRecord, key: string, context: string): number | null {
  const value = record[key];
  if (value === null || value === undefined) return null;
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function arrayValue(record: JsonRecord, key: string, context: string): unknown[] {
  const value = record[key];
  if (!Array.isArray(value)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function listValue(value: unknown, context: string): unknown[] {
  if (!Array.isArray(value)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value;
}

function nullableRecordValue(record: JsonRecord, key: string, context: string): JsonRecord | null {
  const value = record[key];
  if (value === null || value === undefined) return null;
  return recordValue(value, context);
}

function oneOf<T extends string>(value: string, allowed: readonly T[], context: string): T {
  if (!allowed.includes(value as T)) {
    throw new Error(`The backend returned an invalid ${context} response.`);
  }
  return value as T;
}

function parseRecentActivity(value: unknown): AdminRecentActivity {
  const item = recordValue(value, "dashboard activity");
  return {
    id: stringValue(item, "id", "dashboard activity"),
    action: stringValue(item, "action", "dashboard activity"),
    entityType: stringValue(item, "entityType", "dashboard activity"),
    entityId: nullableStringValue(item, "entityId", "dashboard activity"),
    actorId: nullableStringValue(item, "actorId", "dashboard activity"),
    actorName: nullableStringValue(item, "actorName", "dashboard activity"),
    createdAt: stringValue(item, "createdAt", "dashboard activity"),
  };
}

function parseAdminDashboard(value: unknown): AdminDashboard {
  const dashboard = recordValue(value, "admin dashboard");
  return {
    users: numberValue(dashboard, "users", "admin dashboard"),
    projects: numberValue(dashboard, "projects", "admin dashboard"),
    posts: numberValue(dashboard, "posts", "admin dashboard"),
    joinRequests: numberValue(dashboard, "joinRequests", "admin dashboard"),
    tasks: numberValue(dashboard, "tasks", "admin dashboard"),
    recentActivities: arrayValue(dashboard, "recentActivities", "admin dashboard").map(
      parseRecentActivity,
    ),
  };
}

function parseAdminUserOption(value: unknown): AdminUserOption {
  const item = recordValue(value, "Admin user option");
  return {
    id: stringValue(item, "id", "Admin user option"),
    fullName: stringValue(item, "fullName", "Admin user option"),
    email: stringValue(item, "email", "Admin user option"),
    accountStatus: oneOf(
      stringValue(item, "accountStatus", "Admin user option"),
      ["ACTIVE", "LOCKED", "PENDING", "DELETED"] as const,
      "Admin user option",
    ),
    roleCodes: arrayValue(item, "roleCodes", "Admin user option").map((roleCode) => {
      if (typeof roleCode !== "string" || !roleCode) {
        throw new Error("The backend returned an invalid Admin user option response.");
      }
      return roleCode;
    }),
  };
}

function parseAdminProjectOption(value: unknown): AdminProjectOption {
  const item = recordValue(value, "Admin project option");
  const activeRecipientCount = numberValue(item, "activeRecipientCount", "Admin project option");
  if (!Number.isInteger(activeRecipientCount) || activeRecipientCount < 0) {
    throw new Error("The backend returned an invalid Admin project option response.");
  }
  return {
    id: stringValue(item, "id", "Admin project option"),
    code: stringValue(item, "code", "Admin project option"),
    name: stringValue(item, "name", "Admin project option"),
    slug: stringValue(item, "slug", "Admin project option"),
    status: oneOf(
      stringValue(item, "status", "Admin project option"),
      ["PROPOSED", "PREPARING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CLOSED"] as const,
      "Admin project option",
    ),
    activeRecipientCount,
  };
}

function parseJoinRequestUser(value: unknown, context: string) {
  const user = recordValue(value, context);
  return {
    id: stringValue(user, "id", context),
    fullName: stringValue(user, "fullName", context),
    email: stringValue(user, "email", context),
  };
}

function parseAdminJoinRequest(value: unknown): AdminJoinRequest {
  const item = recordValue(value, "join request");
  const project = recordValue(item.project, "join request project");
  const reviewedBy = nullableRecordValue(item, "reviewedBy", "join request reviewer");
  const cvFile = nullableRecordValue(item, "cvFile", "join request CV");
  return {
    id: stringValue(item, "id", "join request"),
    project: {
      id: stringValue(project, "id", "join request project"),
      code: stringValue(project, "code", "join request project"),
      name: stringValue(project, "name", "join request project"),
      slug: stringValue(project, "slug", "join request project"),
    },
    requester: parseJoinRequestUser(item.requester, "join request requester"),
    desiredPosition: nullableStringValue(item, "desiredPosition", "join request"),
    reason: nullableStringValue(item, "reason", "join request"),
    skills: nullableStringValue(item, "skills", "join request"),
    experience: nullableStringValue(item, "experience", "join request"),
    introduction: nullableStringValue(item, "introduction", "join request"),
    cvFile: cvFile
      ? {
          id: stringValue(cvFile, "id", "join request CV"),
          originalName: stringValue(cvFile, "originalName", "join request CV"),
          mimeType: nullableStringValue(cvFile, "mimeType", "join request CV"),
          fileSize: nullableNumberValue(cvFile, "fileSize", "join request CV"),
          visibility: oneOf(
            stringValue(cvFile, "visibility", "join request CV"),
            ["PUBLIC", "LAB_INTERNAL", "PROJECT_INTERNAL", "PRIVATE"] as const,
            "join request CV",
          ),
        }
      : null,
    status: oneOf(
      stringValue(item, "status", "join request"),
      ["PENDING", "APPROVED", "REJECTED", "CANCELLED"] as const,
      "join request",
    ),
    reviewedBy: reviewedBy ? parseJoinRequestUser(reviewedBy, "join request reviewer") : null,
    reviewedAt: nullableStringValue(item, "reviewedAt", "join request"),
    rejectionReason: nullableStringValue(item, "rejectionReason", "join request"),
    createdAt: stringValue(item, "createdAt", "join request"),
    updatedAt: stringValue(item, "updatedAt", "join request"),
  };
}

function parseNotificationSummaryRecord(item: JsonRecord): AdminNotificationSummary {
  const createdBy = nullableRecordValue(item, "createdBy", "notification creator");
  return {
    id: stringValue(item, "id", "notification"),
    title: stringValue(item, "title", "notification"),
    message: nullableStringValue(item, "message", "notification"),
    notificationType: stringValue(item, "notificationType", "notification"),
    relatedType: nullableStringValue(item, "relatedType", "notification"),
    relatedId: nullableStringValue(item, "relatedId", "notification"),
    linkUrl: nullableStringValue(item, "linkUrl", "notification"),
    createdBy: createdBy
      ? {
          id: stringValue(createdBy, "id", "notification creator"),
          fullName: stringValue(createdBy, "fullName", "notification creator"),
        }
      : null,
    recipientCount: numberValue(item, "recipientCount", "notification"),
    readCount: numberValue(item, "readCount", "notification"),
    createdAt: stringValue(item, "createdAt", "notification"),
  };
}

function parseAdminNotificationSummary(value: unknown): AdminNotificationSummary {
  return parseNotificationSummaryRecord(recordValue(value, "notification"));
}

function parseAdminNotificationDetail(value: unknown): AdminNotificationDetail {
  const item = recordValue(value, "notification detail");
  return {
    ...parseNotificationSummaryRecord(item),
    recipients: arrayValue(item, "recipients", "notification detail").map((recipientValue) => {
      const recipient = recordValue(recipientValue, "notification recipient");
      return {
        userId: stringValue(recipient, "userId", "notification recipient"),
        fullName: stringValue(recipient, "fullName", "notification recipient"),
        readAt: nullableStringValue(recipient, "readAt", "notification recipient"),
        hiddenAt: nullableStringValue(recipient, "hiddenAt", "notification recipient"),
        createdAt: stringValue(recipient, "createdAt", "notification recipient"),
      };
    }),
  };
}

function parseAdminNotificationFilterOptions(value: unknown): AdminNotificationFilterOptions {
  const options = recordValue(value, "notification filter options");
  const parseCodes = (key: "notificationTypes" | "creatableNotificationTypes" | "relatedTypes") =>
    arrayValue(options, key, "notification filter options").map((item) => {
      if (typeof item !== "string" || !item) {
        throw new Error("The backend returned an invalid notification filter options response.");
      }
      return item;
    });
  return {
    notificationTypes: parseCodes("notificationTypes"),
    creatableNotificationTypes: parseCodes("creatableNotificationTypes"),
    relatedTypes: parseCodes("relatedTypes"),
    creators: arrayValue(options, "creators", "notification filter options").map((creatorValue) => {
      const creator = recordValue(creatorValue, "notification creator option");
      return {
        id: stringValue(creator, "id", "notification creator option"),
        fullName: stringValue(creator, "fullName", "notification creator option"),
        email: stringValue(creator, "email", "notification creator option"),
        accountStatus: oneOf(
          stringValue(creator, "accountStatus", "notification creator option"),
          ["ACTIVE", "LOCKED", "PENDING", "DELETED"] as const,
          "notification creator option",
        ),
      };
    }),
  };
}

function parsePage<T>(
  value: unknown,
  parseItem: (item: unknown) => T,
  context: string,
): AdminPage<T> {
  const page = recordValue(value, context);
  return {
    content: arrayValue(page, "content", context).map(parseItem),
    page: numberValue(page, "page", context),
    size: numberValue(page, "size", context),
    totalElements: numberValue(page, "totalElements", context),
    totalPages: numberValue(page, "totalPages", context),
  };
}

function withQuery(path: string, values: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const encoded = query.toString();
  return encoded ? `${path}?${encoded}` : path;
}

export function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

export async function getAdminDashboard(token: string, recentLimit = 10) {
  const response = await apiRequest(withQuery("/api/admin/dashboard", { recentLimit }), { token });
  return parseAdminDashboard(response);
}

export async function getAdminUserOptions(token: string) {
  const response = await apiRequest("/api/admin/users", { token });
  return listValue(response, "Admin user options")
    .map(parseAdminUserOption)
    .sort((first, second) =>
      `${first.fullName}\u0000${first.email}`.localeCompare(
        `${second.fullName}\u0000${second.email}`,
      ),
    );
}

export async function getAdminProjectOptions(token: string) {
  const response = await apiRequest("/api/admin/projects/options", { token });
  return listValue(response, "Admin project options")
    .map(parseAdminProjectOption)
    .sort((first, second) =>
      `${first.name}\u0000${first.code}`.localeCompare(`${second.name}\u0000${second.code}`),
    );
}

export async function getAdminJoinRequests(token: string, filters: AdminJoinRequestFilters) {
  const response = await apiRequest(withQuery("/api/admin/project-join-requests", filters), {
    token,
  });
  return parsePage(response, parseAdminJoinRequest, "join-request page");
}

export async function getAdminJoinRequest(token: string, requestId: string) {
  const response = await apiRequest(`/api/admin/project-join-requests/${requestId}`, { token });
  return parseAdminJoinRequest(response);
}

export async function approveAdminJoinRequest(token: string, requestId: string) {
  const response = await apiRequest(`/api/admin/project-join-requests/${requestId}/approve`, {
    method: "PATCH",
    token,
  });
  return parseAdminJoinRequest(response);
}

export async function rejectAdminJoinRequest(token: string, requestId: string, reason: string) {
  const response = await apiRequest(`/api/admin/project-join-requests/${requestId}/reject`, {
    method: "PATCH",
    token,
    body: { reason },
  });
  return parseAdminJoinRequest(response);
}

export async function getAdminNotifications(token: string, filters: AdminNotificationFilters) {
  const response = await apiRequest(withQuery("/api/admin/notifications", filters), { token });
  return parsePage(response, parseAdminNotificationSummary, "notification page");
}

export async function getAdminNotificationFilterOptions(token: string) {
  const response = await apiRequest("/api/admin/notifications/options", { token });
  return parseAdminNotificationFilterOptions(response);
}

export async function getAdminNotification(token: string, notificationId: string) {
  const response = await apiRequest(`/api/admin/notifications/${notificationId}`, { token });
  return parseAdminNotificationDetail(response);
}

export async function createAdminNotification(token: string, input: CreateAdminNotificationInput) {
  const response = await apiRequest("/api/admin/notifications", {
    method: "POST",
    token,
    body: input,
  });
  return parseAdminNotificationDetail(response);
}

export async function hideAdminNotification(token: string, notificationId: string) {
  await apiRequest(`/api/admin/notifications/${notificationId}`, {
    method: "DELETE",
    token,
  });
}
