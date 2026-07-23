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
  authorId: string;
  authorName: string;
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
  if (statuses.includes(value as AdminPostStatus)) return value as AdminPostStatus;
  throw new Error("The backend returned an unsupported post moderation status.");
}

function asAdminPostContentType(value: string): AdminPostContentType {
  if (contentTypes.includes(value as AdminPostContentType)) return value as AdminPostContentType;
  throw new Error("The backend returned an unsupported post content type.");
}

function asAdminPostVisibility(value: string): AdminPostVisibility {
  if (visibilities.includes(value as AdminPostVisibility)) return value as AdminPostVisibility;
  throw new Error("The backend returned an unsupported post visibility.");
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
    authorId: readString(value, "authorId"),
    authorName: readString(value, "authorName"),
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
