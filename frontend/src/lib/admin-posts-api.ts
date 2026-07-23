import { useCallback, useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api-client";

export type AdminPostStatus =
  "DRAFT" | "PENDING_REVIEW" | "NEEDS_REVISION" | "APPROVED" | "PUBLISHED" | "REJECTED";

export type AdminPostContentType =
  | "NEWS"
  | "LAB_ANNOUNCEMENT"
  | "PROJECT_ANNOUNCEMENT"
  | "MEMBER_BLOG"
  | "EXPERIENCE_SHARING"
  | "ACADEMIC_POST"
  | "RESEARCH_RESULT"
  | "EVENT_CONTENT";

export type AdminPostVisibility = "PUBLIC" | "LAB_INTERNAL" | "PROJECT_INTERNAL" | "PRIVATE";

export type AdminPostSummary = {
  id: string;
  title: string;
  slug: string;
  summary: string | null;
  contentType: AdminPostContentType;
  visibility: AdminPostVisibility;
  moderationStatus: AdminPostStatus;
  authorId: string | null;
  authorName: string | null;
  projectId: string | null;
  projectName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  coverFileId: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminPostPage = {
  content: AdminPostSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type AdminPostAuthor = {
  id: string;
  fullName: string;
};

export type AdminPostRelation = {
  id: string;
  name: string;
};

export type AdminPostFile = {
  id: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  fileExtension: string;
  createdAt: string;
};

export type AdminPostAttachment = {
  attachmentId: string;
  fileId: string | null;
  originalName: string | null;
  mimeType: string | null;
  fileSize: number | null;
  fileExtension: string | null;
  uploadedById: string | null;
  uploadedByName: string | null;
  createdAt: string;
};

export type AdminPostModerationAction =
  "CREATE" | "SUBMIT" | "APPROVE" | "REQUEST_REVISION" | "REJECT" | "PUBLISH" | "UNPUBLISH";

export type AdminPostModerationHistory = {
  id: string;
  action: AdminPostModerationAction;
  fromStatus: AdminPostStatus | null;
  toStatus: AdminPostStatus | null;
  actorId: string | null;
  actorName: string | null;
  reason: string | null;
  createdAt: string;
};

export type AdminPostDetail = {
  id: string;
  title: string;
  slug: string;
  summary: string | null;
  content: string | null;
  contentType: AdminPostContentType;
  visibility: AdminPostVisibility;
  moderationStatus: AdminPostStatus;
  author: AdminPostAuthor | null;
  project: AdminPostRelation | null;
  category: AdminPostRelation | null;
  coverFile: AdminPostFile | null;
  attachments: AdminPostAttachment[];
  moderationHistory: AdminPostModerationHistory[];
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminPostApproveResponse = {
  postId: string;
  action: "APPROVE";
  fromStatus: "PENDING_REVIEW";
  toStatus: "APPROVED";
  moderationStatus: "APPROVED";
  reviewedById: string | null;
  reviewedByName: string | null;
  reviewedAt: string;
};

type RequestState<T> = { data: T; loading: boolean; error: string | null };
type StatusPillTone = "neutral" | "cyan" | "emerald" | "violet" | "amber" | "rose";

const EMPTY_PAGE: AdminPostPage = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

export const adminPostStatusLabel: Record<AdminPostStatus, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending review",
  NEEDS_REVISION: "Needs revision",
  APPROVED: "Approved",
  PUBLISHED: "Published",
  REJECTED: "Rejected",
};

export const adminPostContentTypeLabel: Record<AdminPostContentType, string> = {
  NEWS: "News",
  LAB_ANNOUNCEMENT: "Lab announcement",
  PROJECT_ANNOUNCEMENT: "Project announcement",
  MEMBER_BLOG: "Member blog",
  EXPERIENCE_SHARING: "Experience sharing",
  ACADEMIC_POST: "Academic post",
  RESEARCH_RESULT: "Research result",
  EVENT_CONTENT: "Event content",
};

export const adminPostVisibilityLabel: Record<AdminPostVisibility, string> = {
  PUBLIC: "Public",
  LAB_INTERNAL: "Lab internal",
  PROJECT_INTERNAL: "Project internal",
  PRIVATE: "Private",
};

export function moderationStatusLabel(status: AdminPostStatus) {
  return adminPostStatusLabel[status];
}

export function contentTypeLabel(contentType: AdminPostContentType) {
  return adminPostContentTypeLabel[contentType];
}

export function visibilityLabel(visibility: AdminPostVisibility) {
  return adminPostVisibilityLabel[visibility];
}

const statuses: readonly AdminPostStatus[] = [
  "DRAFT",
  "PENDING_REVIEW",
  "NEEDS_REVISION",
  "APPROVED",
  "PUBLISHED",
  "REJECTED",
];

const contentTypes: readonly AdminPostContentType[] = [
  "NEWS",
  "LAB_ANNOUNCEMENT",
  "PROJECT_ANNOUNCEMENT",
  "MEMBER_BLOG",
  "EXPERIENCE_SHARING",
  "ACADEMIC_POST",
  "RESEARCH_RESULT",
  "EVENT_CONTENT",
];

const visibilities: readonly AdminPostVisibility[] = [
  "PUBLIC",
  "LAB_INTERNAL",
  "PROJECT_INTERNAL",
  "PRIVATE",
];

const moderationActions: readonly AdminPostModerationAction[] = [
  "CREATE",
  "SUBMIT",
  "APPROVE",
  "REQUEST_REVISION",
  "REJECT",
  "PUBLISH",
  "UNPUBLISH",
];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown, field: string) {
  if (!isRecord(value) || typeof value[field] !== "string" || value[field].trim() === "") {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return value[field];
}

function readNullableString(value: unknown, field: string) {
  if (!isRecord(value) || (value[field] !== null && typeof value[field] !== "string")) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return typeof value[field] === "string" ? value[field] : null;
}

function readNumber(value: unknown, field: string) {
  if (!isRecord(value) || typeof value[field] !== "number" || !Number.isFinite(value[field])) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return value[field];
}

function readNullableNumber(value: unknown, field: string) {
  if (!isRecord(value) || (value[field] !== null && typeof value[field] !== "number")) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  if (typeof value[field] === "number" && !Number.isFinite(value[field])) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return typeof value[field] === "number" ? value[field] : null;
}

function readArray(value: unknown, field: string) {
  if (!isRecord(value) || !Array.isArray(value[field])) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return value[field];
}

function readPageNumber(value: unknown, field: string, minimum: number) {
  if (
    !isRecord(value) ||
    typeof value[field] !== "number" ||
    !Number.isInteger(value[field]) ||
    value[field] < minimum
  ) {
    throw new Error(`The backend returned invalid post pagination metadata: ${field}.`);
  }
  return value[field];
}

function readBoolean(value: unknown, field: string) {
  if (!isRecord(value) || typeof value[field] !== "boolean") {
    throw new Error(`The backend returned invalid post pagination metadata: ${field}.`);
  }
  return value[field];
}

function readTimestamp(value: unknown, field: string) {
  const timestamp = readString(value, field);
  if (Number.isNaN(new Date(timestamp).getTime())) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return timestamp;
}

function readNullableTimestamp(value: unknown, field: string) {
  const timestamp = readNullableString(value, field);
  if (timestamp !== null && Number.isNaN(new Date(timestamp).getTime())) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return timestamp;
}

function asAdminPostStatus(value: string): AdminPostStatus {
  for (const status of statuses) {
    if (value === status) return status;
  }
  throw new Error("The backend returned an unsupported post moderation status.");
}

function asNullableAdminPostStatus(value: unknown, field: string): AdminPostStatus | null {
  if (value === null) return null;
  if (typeof value !== "string") {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return asAdminPostStatus(value);
}

function asAdminPostContentType(value: string): AdminPostContentType {
  for (const contentType of contentTypes) {
    if (value === contentType) return contentType;
  }
  throw new Error("The backend returned an unsupported post content type.");
}

function asAdminPostVisibility(value: string): AdminPostVisibility {
  for (const visibility of visibilities) {
    if (value === visibility) return visibility;
  }
  throw new Error("The backend returned an unsupported post visibility.");
}

function asAdminPostModerationAction(value: string): AdminPostModerationAction {
  for (const action of moderationActions) {
    if (value === action) return action;
  }
  throw new Error("The backend returned an unsupported post moderation action.");
}

function readOptionalRecord(value: unknown, field: string) {
  if (!isRecord(value)) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  const entry = value[field];
  if (entry === undefined || entry === null) return null;
  if (!isRecord(entry)) {
    throw new Error(`The backend returned an invalid post ${field}.`);
  }
  return entry;
}

function normalizePost(value: unknown): AdminPostSummary {
  return {
    id: readString(value, "id"),
    title: readString(value, "title"),
    slug: readString(value, "slug"),
    summary: readNullableString(value, "summary"),
    contentType: asAdminPostContentType(readString(value, "contentType")),
    visibility: asAdminPostVisibility(readString(value, "visibility")),
    moderationStatus: asAdminPostStatus(readString(value, "moderationStatus")),
    authorId: readNullableString(value, "authorId"),
    authorName: readNullableString(value, "authorName"),
    projectId: readNullableString(value, "projectId"),
    projectName: readNullableString(value, "projectName"),
    categoryId: readNullableString(value, "categoryId"),
    categoryName: readNullableString(value, "categoryName"),
    coverFileId: readNullableString(value, "coverFileId"),
    publishedAt: readNullableTimestamp(value, "publishedAt"),
    createdAt: readTimestamp(value, "createdAt"),
    updatedAt: readTimestamp(value, "updatedAt"),
  };
}

function normalizeAuthor(value: unknown): AdminPostAuthor {
  return {
    id: readString(value, "id"),
    fullName: readString(value, "fullName"),
  };
}

function normalizeRelation(value: unknown): AdminPostRelation {
  return {
    id: readString(value, "id"),
    name: readString(value, "name"),
  };
}

function normalizeFile(value: unknown): AdminPostFile {
  return {
    id: readString(value, "id"),
    originalName: readString(value, "originalName"),
    mimeType: readString(value, "mimeType"),
    fileSize: readNumber(value, "fileSize"),
    fileExtension: readString(value, "fileExtension"),
    createdAt: readTimestamp(value, "createdAt"),
  };
}

function normalizeAttachment(value: unknown): AdminPostAttachment {
  return {
    attachmentId: readString(value, "attachmentId"),
    fileId: readNullableString(value, "fileId"),
    originalName: readNullableString(value, "originalName"),
    mimeType: readNullableString(value, "mimeType"),
    fileSize: readNullableNumber(value, "fileSize"),
    fileExtension: readNullableString(value, "fileExtension"),
    uploadedById: readNullableString(value, "uploadedById"),
    uploadedByName: readNullableString(value, "uploadedByName"),
    createdAt: readTimestamp(value, "createdAt"),
  };
}

function normalizeModerationHistory(value: unknown): AdminPostModerationHistory {
  if (!isRecord(value)) {
    throw new Error("The backend returned an invalid post moderationHistory.");
  }
  return {
    id: readString(value, "id"),
    action: asAdminPostModerationAction(readString(value, "action")),
    fromStatus: asNullableAdminPostStatus(value.fromStatus, "fromStatus"),
    toStatus: asNullableAdminPostStatus(value.toStatus, "toStatus"),
    actorId: readNullableString(value, "actorId"),
    actorName: readNullableString(value, "actorName"),
    reason: readNullableString(value, "reason"),
    createdAt: readTimestamp(value, "createdAt"),
  };
}

function normalizeDetail(value: unknown): AdminPostDetail {
  const author = readOptionalRecord(value, "author");
  const project = readOptionalRecord(value, "project");
  const category = readOptionalRecord(value, "category");
  const coverFile = readOptionalRecord(value, "coverFile");
  return {
    id: readString(value, "id"),
    title: readString(value, "title"),
    slug: readString(value, "slug"),
    summary: readNullableString(value, "summary"),
    content: readNullableString(value, "content"),
    contentType: asAdminPostContentType(readString(value, "contentType")),
    visibility: asAdminPostVisibility(readString(value, "visibility")),
    moderationStatus: asAdminPostStatus(readString(value, "moderationStatus")),
    author: author ? normalizeAuthor(author) : null,
    project: project ? normalizeRelation(project) : null,
    category: category ? normalizeRelation(category) : null,
    coverFile: coverFile ? normalizeFile(coverFile) : null,
    attachments: readArray(value, "attachments").map(normalizeAttachment),
    moderationHistory: readArray(value, "moderationHistory").map(normalizeModerationHistory),
    publishedAt: readNullableTimestamp(value, "publishedAt"),
    createdAt: readTimestamp(value, "createdAt"),
    updatedAt: readTimestamp(value, "updatedAt"),
  };
}

function normalizeApproveResponse(value: unknown): AdminPostApproveResponse {
  const action = readString(value, "action");
  const fromStatus = readString(value, "fromStatus");
  const toStatus = readString(value, "toStatus");
  const moderationStatus = readString(value, "moderationStatus");
  if (
    action !== "APPROVE" ||
    fromStatus !== "PENDING_REVIEW" ||
    toStatus !== "APPROVED" ||
    moderationStatus !== "APPROVED"
  ) {
    throw new Error("The backend returned an invalid post approval response.");
  }
  return {
    postId: readString(value, "postId"),
    action,
    fromStatus,
    toStatus,
    moderationStatus,
    reviewedById: readNullableString(value, "reviewedById"),
    reviewedByName: readNullableString(value, "reviewedByName"),
    reviewedAt: readTimestamp(value, "reviewedAt"),
  };
}

function normalizePage(value: unknown): AdminPostPage {
  if (!isRecord(value) || !Array.isArray(value.content)) {
    throw new Error("The backend returned an invalid post page.");
  }
  return {
    content: value.content.map(normalizePost),
    page: readPageNumber(value, "page", 0),
    size: readPageNumber(value, "size", 1),
    totalElements: readPageNumber(value, "totalElements", 0),
    totalPages: readPageNumber(value, "totalPages", 0),
    first: readBoolean(value, "first"),
    last: readBoolean(value, "last"),
  };
}

function requireLabAnnouncementPage(page: AdminPostPage) {
  if (page.content.some((post) => post.contentType !== "LAB_ANNOUNCEMENT")) {
    throw new Error(
      "The backend returned a non-lab-announcement post in the lab announcements page.",
    );
  }
  return page;
}

function requireLabAnnouncementDetail(detail: AdminPostDetail) {
  if (detail.contentType !== "LAB_ANNOUNCEMENT") {
    throw new Error("The backend returned a non-lab-announcement post detail.");
  }
  return detail;
}

export function adminPostStatusTone(status: AdminPostStatus): StatusPillTone {
  if (status === "PUBLISHED" || status === "APPROVED") return "emerald";
  if (status === "PENDING_REVIEW") return "amber";
  if (status === "NEEDS_REVISION") return "violet";
  if (status === "REJECTED") return "rose";
  return "neutral";
}

export function adminPostErrorMessage(
  error: unknown,
  fallback = "The post request could not be completed.",
) {
  if (error instanceof ApiError && error.status === 0) {
    return "Unable to reach the SmartLab backend. Check that the backend is running.";
  }
  if (error instanceof ApiError && error.status === 401) {
    return "Your session has expired. Sign in again to load moderation posts.";
  }
  if (error instanceof ApiError && error.status === 403) {
    return "Admin access denied for post moderation.";
  }
  if (error instanceof ApiError && error.status === 404) {
    return "Post not found. It may have been removed or is outside your lab.";
  }
  if (error instanceof ApiError && error.status === 409) {
    return error.message;
  }
  return error instanceof Error ? error.message : fallback;
}

export async function fetchAdminPosts(token: string, page: number, size: number) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return normalizePage(await apiRequest(`/api/admin/posts?${params.toString()}`, { token }));
}

export async function fetchPendingAdminPosts(token: string, page: number, size: number) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return normalizePage(
    await apiRequest(`/api/admin/posts/pending?${params.toString()}`, { token }),
  );
}

export async function fetchAdminPostDetail(token: string, postId: string) {
  return normalizeDetail(
    await apiRequest(`/api/admin/posts/${encodeURIComponent(postId)}`, { token }),
  );
}

export async function approveAdminPost(token: string, postId: string) {
  return normalizeApproveResponse(
    await apiRequest(`/api/admin/posts/${encodeURIComponent(postId)}/approve`, {
      token,
      method: "POST",
    }),
  );
}

export async function fetchAdminLabAnnouncements(token: string, page: number, size: number) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return requireLabAnnouncementPage(
    normalizePage(await apiRequest(`/api/admin/lab-announcements?${params.toString()}`, { token })),
  );
}

export async function fetchAdminLabAnnouncementDetail(token: string, postId: string) {
  return requireLabAnnouncementDetail(
    normalizeDetail(
      await apiRequest(`/api/admin/lab-announcements/${encodeURIComponent(postId)}`, { token }),
    ),
  );
}

function useAdminPostPage(
  token: string | null,
  enabled: boolean,
  page: number,
  size: number,
  fetchPage: (token: string, page: number, size: number) => Promise<AdminPostPage>,
  fallback: string,
) {
  const [state, setState] = useState<RequestState<AdminPostPage>>({
    data: EMPTY_PAGE,
    loading: enabled,
    error: null,
  });
  const [revision, setRevision] = useState(0);
  const retry = useCallback(() => setRevision((value) => value + 1), []);

  useEffect(() => {
    if (!enabled || !token) {
      setState({ data: EMPTY_PAGE, loading: false, error: null });
      return;
    }
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    void fetchPage(token, page, size)
      .then((data) => {
        if (current) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (current)
          setState({
            data: EMPTY_PAGE,
            loading: false,
            error: adminPostErrorMessage(error, fallback),
          });
      });
    return () => {
      current = false;
    };
  }, [enabled, fallback, fetchPage, page, revision, size, token]);

  return { ...state, retry };
}

export function useAdminPosts(token: string | null, enabled: boolean, page = 0, size = 10) {
  return useAdminPostPage(
    token,
    enabled,
    page,
    size,
    fetchAdminPosts,
    "Posts could not be loaded.",
  );
}

export function usePendingAdminPosts(token: string | null, enabled: boolean, page = 0, size = 10) {
  return useAdminPostPage(
    token,
    enabled,
    page,
    size,
    fetchPendingAdminPosts,
    "Pending moderation posts could not be loaded.",
  );
}

export function useAdminPostDetail(token: string | null, postId: string | null, enabled: boolean) {
  const [state, setState] = useState<RequestState<AdminPostDetail | null>>({
    data: null,
    loading: enabled,
    error: null,
  });
  const [revision, setRevision] = useState(0);
  const retry = useCallback(() => setRevision((value) => value + 1), []);

  useEffect(() => {
    if (!enabled || !token || !postId) {
      setState({ data: null, loading: false, error: null });
      return;
    }
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    void fetchAdminPostDetail(token, postId)
      .then((data) => {
        if (current) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (current)
          setState({
            data: null,
            loading: false,
            error: adminPostErrorMessage(error, "Post details could not be loaded."),
          });
      });
    return () => {
      current = false;
    };
  }, [enabled, postId, revision, token]);

  return { ...state, retry };
}

export function useAdminLabAnnouncements(
  token: string | null,
  enabled: boolean,
  page = 0,
  size = 10,
) {
  return useAdminPostPage(
    token,
    enabled,
    page,
    size,
    fetchAdminLabAnnouncements,
    "Lab announcements could not be loaded.",
  );
}

export function useAdminLabAnnouncementDetail(
  token: string | null,
  postId: string | null,
  enabled: boolean,
) {
  const [state, setState] = useState<RequestState<AdminPostDetail | null>>({
    data: null,
    loading: enabled,
    error: null,
  });
  const [revision, setRevision] = useState(0);
  const retry = useCallback(() => setRevision((value) => value + 1), []);

  useEffect(() => {
    if (!enabled || !token || !postId) {
      setState({ data: null, loading: false, error: null });
      return;
    }
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    void fetchAdminLabAnnouncementDetail(token, postId)
      .then((data) => {
        if (current) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (current)
          setState({
            data: null,
            loading: false,
            error: adminPostErrorMessage(error, "Lab announcement details could not be loaded."),
          });
      });
    return () => {
      current = false;
    };
  }, [enabled, postId, revision, token]);

  return { ...state, retry };
}
