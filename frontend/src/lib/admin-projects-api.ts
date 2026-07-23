import { useCallback, useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api-client";
import {
  type FieldKey,
  type Project,
  type ProjectStatus,
  type ProjectType,
  type ProjectVisibility,
} from "@/lib/projects-data";

type AdminProjectPage = {
  items: Project[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type RequestState<T> = { data: T; loading: boolean; error: string | null };

const EMPTY_PAGE: AdminProjectPage = {
  items: [],
  page: 0,
  size: 100,
  totalElements: 0,
  totalPages: 0,
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown, field: string) {
  if (!isRecord(value) || typeof value[field] !== "string" || !value[field]) {
    throw new Error(`The backend returned an invalid project ${field}.`);
  }
  return value[field];
}

function readOptionalString(value: unknown, field: string) {
  if (!isRecord(value) || (value[field] !== null && typeof value[field] !== "string")) {
    throw new Error(`The backend returned an invalid project ${field}.`);
  }
  return typeof value[field] === "string" ? value[field] : "";
}

function readNumber(value: unknown, field: string) {
  if (!isRecord(value) || typeof value[field] !== "number" || !Number.isFinite(value[field])) {
    throw new Error(`The backend returned an invalid project ${field}.`);
  }
  return value[field];
}

function readStringArray(value: unknown, field: string) {
  if (!isRecord(value) || !Array.isArray(value[field])) {
    throw new Error(`The backend returned an invalid project ${field}.`);
  }
  const entries = value[field];
  if (!entries.every((entry) => typeof entry === "string")) {
    throw new Error(`The backend returned an invalid project ${field}.`);
  }
  return entries;
}

function asProjectStatus(value: string): ProjectStatus {
  if (
    value === "Planning" ||
    value === "Active" ||
    value === "Publishing" ||
    value === "Completed" ||
    value === "On hold"
  )
    return value;
  throw new Error("The backend returned an unsupported project status.");
}

function asProjectType(value: string): ProjectType {
  if (value === "Research" || value === "Production") return value;
  throw new Error("The backend returned an unsupported project type.");
}

function asProjectVisibility(value: string): ProjectVisibility {
  if (value === "public" || value === "internal") return value;
  throw new Error("The backend returned an unsupported project visibility.");
}

function asFieldKeys(values: string[]): FieldKey[] {
  if (!values.every((value) => value === "ai" || value === "robotics" || value === "se")) {
    throw new Error("The backend returned an unsupported research field.");
  }
  return values as FieldKey[];
}

function normalizeProject(value: unknown): Project {
  const progress = readNumber(value, "progress");
  if (progress < 0 || progress > 100) {
    throw new Error("The backend returned project progress outside 0-100.");
  }
  return {
    id: readString(value, "id"),
    slug: readString(value, "slug"),
    code: readString(value, "code"),
    name: readString(value, "name"),
    description: readOptionalString(value, "description"),
    objective: readOptionalString(value, "objective"),
    type: asProjectType(readString(value, "type")),
    fields: asFieldKeys(readStringArray(value, "fields")),
    leaderIds: readStringArray(value, "leaderIds"),
    memberIds: readStringArray(value, "memberIds"),
    startDate: readString(value, "startDate"),
    expectedEnd: readString(value, "expectedEnd"),
    status: asProjectStatus(readString(value, "status")),
    progress,
    visibility: asProjectVisibility(readString(value, "visibility")),
  };
}

function normalizePage(value: unknown): AdminProjectPage {
  if (!isRecord(value) || !Array.isArray(value.items)) {
    throw new Error("The backend returned an invalid project page.");
  }
  return {
    items: value.items.map(normalizeProject),
    page: readNumber(value, "page"),
    size: readNumber(value, "size"),
    totalElements: readNumber(value, "totalElements"),
    totalPages: readNumber(value, "totalPages"),
  };
}

export function adminProjectErrorMessage(
  error: unknown,
  fallback = "The project request could not be completed.",
) {
  if (error instanceof ApiError && error.status === 401) {
    return "Your session has expired. Sign in again to load projects.";
  }
  if (error instanceof ApiError && error.status === 403) {
    return error.message;
  }
  return error instanceof Error ? error.message : fallback;
}

function toSavePayload(project: Project) {
  return {
    code: project.code.trim(),
    name: project.name.trim(),
    description: project.description.trim(),
    objective: project.objective.trim(),
    type: project.type,
    fields: project.fields,
    leaderIds: project.leaderIds,
    startDate: project.startDate || null,
    expectedEnd: project.expectedEnd || null,
    status: project.status,
    progress: project.progress,
    visibility: project.visibility,
  };
}

export async function createAdminProject(token: string, project: Project) {
  return normalizeProject(
    await apiRequest("/api/admin/projects", { token, body: toSavePayload(project) }),
  );
}

export async function updateAdminProject(token: string, project: Project) {
  return normalizeProject(
    await apiRequest(`/api/admin/projects/${project.id}`, {
      token,
      method: "PUT",
      body: toSavePayload(project),
    }),
  );
}

export async function deleteAdminProject(token: string, projectId: string) {
  await apiRequest(`/api/admin/projects/${projectId}`, { token, method: "DELETE" });
}

async function fetchAdminProjects(
  token: string,
  page: number,
  size: number,
  status: ProjectStatus | "all",
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "name,asc",
  });
  if (status !== "all") params.set("status", status);
  return normalizePage(await apiRequest(`/api/admin/projects?${params.toString()}`, { token }));
}

async function fetchAdminProject(token: string, projectId: string) {
  return normalizeProject(await apiRequest(`/api/admin/projects/${projectId}`, { token }));
}

export function useAdminProjects(
  token: string | null,
  enabled: boolean,
  page = 0,
  size = 10,
  status: ProjectStatus | "all" = "all",
) {
  const [state, setState] = useState<RequestState<AdminProjectPage>>({
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
    void fetchAdminProjects(token, page, size, status)
      .then((data) => {
        if (current) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (current)
          setState({
            data: EMPTY_PAGE,
            loading: false,
            error: adminProjectErrorMessage(error, "Projects could not be loaded."),
          });
      });
    return () => {
      current = false;
    };
  }, [enabled, page, revision, size, status, token]);

  return { ...state, retry };
}

export function useAdminProjectDetail(
  token: string | null,
  slug: string,
  enabled: boolean,
  projects: Project[],
) {
  const [state, setState] = useState<RequestState<Project | null>>({
    data: null,
    loading: enabled,
    error: null,
  });
  const [revision, setRevision] = useState(0);
  const retry = useCallback(() => setRevision((value) => value + 1), []);
  const projectId =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(slug)
      ? slug
      : (projects.find((project) => project.slug === slug)?.id ?? null);

  useEffect(() => {
    if (!enabled || !token || !projectId) {
      setState({ data: null, loading: false, error: null });
      return;
    }
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    void fetchAdminProject(token, projectId)
      .then((data) => {
        if (current) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (current)
          setState({
            data: null,
            loading: false,
            error: adminProjectErrorMessage(error, "Project details could not be loaded."),
          });
      });
    return () => {
      current = false;
    };
  }, [enabled, projectId, revision, token]);

  return { ...state, retry };
}
export type AdminProjectMember = {
  membershipId: string;
  userId: string;
  fullName: string;
  email: string;
  projectRole: "PROJECT_LEADER" | "PROJECT_MEMBER";
  memberStatus: "ACTIVE" | "REMOVED" | "LEFT";
  joinedAt: string;
  leftAt: string | null;
};

function normalizeAdminProjectMember(value: unknown): AdminProjectMember {
  if (!isRecord(value)) throw new Error("The backend returned an invalid project member.");
  const projectRole = readString(value, "projectRole");
  const memberStatus = readString(value, "memberStatus");
  if (projectRole !== "PROJECT_LEADER" && projectRole !== "PROJECT_MEMBER") {
    throw new Error("The backend returned an invalid project member role.");
  }
  if (memberStatus !== "ACTIVE" && memberStatus !== "REMOVED" && memberStatus !== "LEFT") {
    throw new Error("The backend returned an invalid project member status.");
  }
  const leftAt = value.leftAt;
  if (leftAt !== null && typeof leftAt !== "string") {
    throw new Error("The backend returned an invalid project member leftAt.");
  }
  return {
    membershipId: readString(value, "membershipId"),
    userId: readString(value, "userId"),
    fullName: readString(value, "fullName"),
    email: readString(value, "email"),
    projectRole,
    memberStatus,
    joinedAt: readString(value, "joinedAt"),
    leftAt,
  };
}

export function useAdminProjectMembers(
  token: string | null,
  projectId: string,
  enabled: boolean,
) {
  const [state, setState] = useState<RequestState<AdminProjectMember[]>>({
    data: [],
    loading: enabled,
    error: null,
  });
  const [revision, setRevision] = useState(0);
  const retry = useCallback(() => setRevision((value) => value + 1), []);

  useEffect(() => {
    if (!enabled || !token || !projectId) {
      setState({ data: [], loading: false, error: null });
      return;
    }
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    void apiRequest(`/api/admin/projects/${projectId}/members?memberStatus=ACTIVE`, { token })
      .then((value) => {
        if (!Array.isArray(value)) throw new Error("The backend returned an invalid member list.");
        if (current) {
          setState({ data: value.map(normalizeAdminProjectMember), loading: false, error: null });
        }
      })
      .catch((error: unknown) => {
        if (current) {
          setState({
            data: [],
            loading: false,
            error: adminProjectErrorMessage(error, "Project members could not be loaded."),
          });
        }
      });
    return () => {
      current = false;
    };
  }, [enabled, projectId, revision, token]);

  return { ...state, retry };
}

export async function addAdminProjectMember(
  token: string,
  projectId: string,
  userId: string,
  role: AdminProjectMember["projectRole"],
) {
  return normalizeAdminProjectMember(
    await apiRequest(`/api/admin/projects/${projectId}/members`, {
      token,
      body: { userId, role },
    }),
  );
}

export async function updateAdminProjectMemberRole(
  token: string,
  projectId: string,
  userId: string,
  role: AdminProjectMember["projectRole"],
) {
  return normalizeAdminProjectMember(
    await apiRequest(`/api/admin/projects/${projectId}/members/${userId}/role`, {
      token,
      method: "PATCH",
      body: { role },
    }),
  );
}

export async function removeAdminProjectMember(
  token: string,
  projectId: string,
  userId: string,
) {
  return normalizeAdminProjectMember(
    await apiRequest(`/api/admin/projects/${projectId}/members/${userId}/remove`, {
      token,
      method: "PATCH",
    }),
  );
}
