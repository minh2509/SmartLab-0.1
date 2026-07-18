import { useCallback, useEffect, useState } from "react";

export type JoinRequestStatus = "pending" | "approved" | "rejected" | "cancelled";

export type ProjectJoinRequest = {
  id: string;
  projectId: string;
  userId: string;
  reason: string;
  skills: string;
  experience: string;
  desiredPosition: string;
  introduction: string;
  status: JoinRequestStatus;
  submittedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNote?: string;
  cancelledAt?: string;
};

export type JoinRequestDraft = Pick<
  ProjectJoinRequest,
  "reason" | "skills" | "experience" | "desiredPosition" | "introduction"
>;

type Result<T = ProjectJoinRequest> = { ok: true; request: T } | { ok: false; error: string };

const STORAGE_KEY = "nova.projectJoinRequests.v1";
const statuses: JoinRequestStatus[] = ["pending", "approved", "rejected", "cancelled"];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function normalizeRequest(value: unknown): ProjectJoinRequest | null {
  if (!isRecord(value)) return null;
  const status = readString(value.status) as JoinRequestStatus;
  if (!statuses.includes(status)) return null;
  const id = readString(value.id);
  const projectId = readString(value.projectId);
  const userId = readString(value.userId);
  const submittedAt = readString(value.submittedAt);
  if (!id || !projectId || !userId || !submittedAt) return null;

  return {
    id,
    projectId,
    userId,
    reason: readString(value.reason),
    skills: readString(value.skills),
    experience: readString(value.experience),
    desiredPosition: readString(value.desiredPosition),
    introduction: readString(value.introduction),
    status,
    submittedAt,
    reviewedAt: readString(value.reviewedAt) || undefined,
    reviewedBy: readString(value.reviewedBy) || undefined,
    reviewNote: readString(value.reviewNote) || undefined,
    cancelledAt: readString(value.cancelledAt) || undefined,
  };
}

function load(): ProjectJoinRequest[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => normalizeRequest(item))
      .filter((item): item is ProjectJoinRequest => !!item);
  } catch {
    return [];
  }
}

function save(list: ProjectJoinRequest[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    return true;
  } catch {
    return false;
  }
}

type Sub = (list: ProjectJoinRequest[]) => void;
const subs = new Set<Sub>();
let cache: ProjectJoinRequest[] | null = null;

function getAll() {
  if (!cache) cache = load();
  return cache;
}

function setAll(next: ProjectJoinRequest[]) {
  const previous = cache;
  cache = next;
  const ok = save(next);
  if (!ok) cache = previous;
  subs.forEach((s) => s(cache ?? []));
  return ok;
}

function makeId() {
  return `jr_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function cleanDraft(draft: JoinRequestDraft): JoinRequestDraft {
  return {
    reason: draft.reason.trim(),
    skills: draft.skills.trim(),
    experience: draft.experience.trim(),
    desiredPosition: draft.desiredPosition.trim(),
    introduction: draft.introduction.trim(),
  };
}

export function getLatestUserProjectRequest(
  requests: ProjectJoinRequest[],
  projectId: string,
  userId: string,
) {
  return requests
    .filter((r) => r.projectId === projectId && r.userId === userId)
    .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0];
}

export function getOpenUserProjectRequest(
  requests: ProjectJoinRequest[],
  projectId: string,
  userId: string,
) {
  return requests.find(
    (r) =>
      r.projectId === projectId &&
      r.userId === userId &&
      (r.status === "pending" || r.status === "approved"),
  );
}

export function joinRequestTone(status: JoinRequestStatus) {
  if (status === "approved") return "emerald";
  if (status === "rejected") return "rose";
  if (status === "cancelled") return "neutral";
  return "amber";
}

export function useJoinRequests() {
  const [list, setList] = useState<ProjectJoinRequest[]>(() => getAll());

  useEffect(() => {
    const cb: Sub = (requests) => setList(requests);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const submit = useCallback(
    (projectId: string, userId: string, draft: JoinRequestDraft): Result => {
      if (getOpenUserProjectRequest(getAll(), projectId, userId)) {
        return {
          ok: false,
          error: "You already have an open request for this project.",
        };
      }
      const cleaned = cleanDraft(draft);
      const request: ProjectJoinRequest = {
        id: makeId(),
        projectId,
        userId,
        ...cleaned,
        status: "pending",
        submittedAt: new Date().toISOString(),
      };
      const ok = setAll([request, ...getAll()]);
      return ok
        ? { ok: true, request }
        : {
            ok: false,
            error: "The request could not be saved in this browser. Please try again.",
          };
    },
    [],
  );

  const cancel = useCallback((requestId: string, userId: string): Result => {
    const current = getAll().find((r) => r.id === requestId);
    if (!current) return { ok: false, error: "Request not found." };
    if (current.userId !== userId) {
      return { ok: false, error: "You can only cancel your own request." };
    }
    if (current.status !== "pending") {
      return { ok: false, error: "Only pending requests can be cancelled." };
    }
    const next: ProjectJoinRequest = {
      ...current,
      status: "cancelled",
      cancelledAt: new Date().toISOString(),
    };
    const ok = setAll(getAll().map((r) => (r.id === requestId ? next : r)));
    return ok
      ? { ok: true, request: next }
      : { ok: false, error: "The cancellation could not be saved." };
  }, []);

  const approve = useCallback(
    (requestId: string, reviewedBy: string, reviewNote: string): Result => {
      const current = getAll().find((r) => r.id === requestId);
      if (!current) return { ok: false, error: "Request not found." };
      if (current.status !== "pending") {
        return { ok: false, error: "Only pending requests can be approved." };
      }
      const next: ProjectJoinRequest = {
        ...current,
        status: "approved",
        reviewedAt: new Date().toISOString(),
        reviewedBy,
        reviewNote: reviewNote.trim() || undefined,
      };
      const ok = setAll(getAll().map((r) => (r.id === requestId ? next : r)));
      return ok
        ? { ok: true, request: next }
        : { ok: false, error: "The approval could not be saved." };
    },
    [],
  );

  const reject = useCallback(
    (requestId: string, reviewedBy: string, reviewNote: string): Result => {
      const current = getAll().find((r) => r.id === requestId);
      if (!current) return { ok: false, error: "Request not found." };
      if (current.status !== "pending") {
        return { ok: false, error: "Only pending requests can be rejected." };
      }
      const note = reviewNote.trim();
      if (!note) return { ok: false, error: "Rejection reason is required." };
      const next: ProjectJoinRequest = {
        ...current,
        status: "rejected",
        reviewedAt: new Date().toISOString(),
        reviewedBy,
        reviewNote: note,
      };
      const ok = setAll(getAll().map((r) => (r.id === requestId ? next : r)));
      return ok
        ? { ok: true, request: next }
        : { ok: false, error: "The rejection could not be saved." };
    },
    [],
  );

  return { requests: list, submit, cancel, approve, reject };
}
