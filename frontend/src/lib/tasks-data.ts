import { useCallback, useEffect, useState } from "react";

export type TaskPriority = "low" | "medium" | "high" | "critical";

export type TaskStatus =
  "draft" | "todo" | "in_progress" | "in_review" | "done" | "overdue" | "cancelled";

export type StoredTaskStatus = Exclude<TaskStatus, "overdue">;

export type AssignmentStatus =
  "assigned" | "working" | "submitted" | "needs_feedback" | "completed";

export type ProjectTask = {
  id: string;
  projectId: string;
  title: string;
  description: string;
  outputCriteria: string;
  priority: TaskPriority;
  status: StoredTaskStatus;
  startDate: string;
  dueDate: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  assigneeIds: string[];
};

export type TaskAssignment = {
  id: string;
  taskId: string;
  memberId: string;
  status: AssignmentStatus;
  startedAt?: string;
  submittedAt?: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNote?: string;
};

export type TaskSubmission = {
  id: string;
  taskId: string;
  memberId: string;
  summary: string;
  artifactUrl?: string;
  notes?: string;
  submittedAt: string;
};

export type TaskDraft = Pick<
  ProjectTask,
  "title" | "description" | "outputCriteria" | "priority" | "startDate" | "dueDate" | "assigneeIds"
>;

type TaskStore = {
  tasks: ProjectTask[];
  assignments: TaskAssignment[];
  submissions: TaskSubmission[];
};

type Result<T = ProjectTask> = { ok: true; value: T } | { ok: false; error: string };

const STORAGE_KEY = "smart.projectTasks.v1";
const emptyStore: TaskStore = { tasks: [], assignments: [], submissions: [] };
const priorities: TaskPriority[] = ["low", "medium", "high", "critical"];
const taskStatuses: StoredTaskStatus[] = [
  "draft",
  "todo",
  "in_progress",
  "in_review",
  "done",
  "cancelled",
];
const assignmentStatuses: AssignmentStatus[] = [
  "assigned",
  "working",
  "submitted",
  "needs_feedback",
  "completed",
];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function readStringList(value: unknown) {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === "string")
    : [];
}

function normalizeTask(value: unknown): ProjectTask | null {
  if (!isRecord(value)) return null;
  const status = readString(value.status) as StoredTaskStatus;
  const priority = readString(value.priority) as TaskPriority;
  if (!taskStatuses.includes(status) || !priorities.includes(priority)) return null;
  const id = readString(value.id);
  const projectId = readString(value.projectId);
  const title = readString(value.title);
  const createdBy = readString(value.createdBy);
  if (!id || !projectId || !title || !createdBy) return null;
  return {
    id,
    projectId,
    title,
    description: readString(value.description),
    outputCriteria: readString(value.outputCriteria),
    priority,
    status,
    startDate: readString(value.startDate),
    dueDate: readString(value.dueDate),
    createdBy,
    createdAt: readString(value.createdAt),
    updatedAt: readString(value.updatedAt),
    assigneeIds: unique(readStringList(value.assigneeIds)),
  };
}

function normalizeAssignment(value: unknown): TaskAssignment | null {
  if (!isRecord(value)) return null;
  const status = readString(value.status) as AssignmentStatus;
  if (!assignmentStatuses.includes(status)) return null;
  const id = readString(value.id);
  const taskId = readString(value.taskId);
  const memberId = readString(value.memberId);
  if (!id || !taskId || !memberId) return null;
  return {
    id,
    taskId,
    memberId,
    status,
    startedAt: readString(value.startedAt) || undefined,
    submittedAt: readString(value.submittedAt) || undefined,
    reviewedAt: readString(value.reviewedAt) || undefined,
    reviewedBy: readString(value.reviewedBy) || undefined,
    reviewNote: readString(value.reviewNote) || undefined,
  };
}

function normalizeSubmission(value: unknown): TaskSubmission | null {
  if (!isRecord(value)) return null;
  const id = readString(value.id);
  const taskId = readString(value.taskId);
  const memberId = readString(value.memberId);
  const summary = readString(value.summary);
  const submittedAt = readString(value.submittedAt);
  if (!id || !taskId || !memberId || !summary || !submittedAt) return null;
  return {
    id,
    taskId,
    memberId,
    summary,
    artifactUrl: readString(value.artifactUrl) || undefined,
    notes: readString(value.notes) || undefined,
    submittedAt,
  };
}

function normalizeStore(value: unknown): TaskStore {
  if (!isRecord(value)) return emptyStore;
  return {
    tasks: Array.isArray(value.tasks)
      ? value.tasks.map(normalizeTask).filter((item): item is ProjectTask => !!item)
      : [],
    assignments: Array.isArray(value.assignments)
      ? value.assignments.map(normalizeAssignment).filter((item): item is TaskAssignment => !!item)
      : [],
    submissions: Array.isArray(value.submissions)
      ? value.submissions.map(normalizeSubmission).filter((item): item is TaskSubmission => !!item)
      : [],
  };
}

function load(): TaskStore {
  if (typeof window === "undefined") return emptyStore;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return emptyStore;
    return normalizeStore(JSON.parse(raw));
  } catch {
    return emptyStore;
  }
}

function save(store: TaskStore) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store));
    return true;
  } catch {
    return false;
  }
}

type Sub = (store: TaskStore) => void;
const subs = new Set<Sub>();
let cache: TaskStore | null = null;

function getStore() {
  if (!cache) cache = load();
  return cache;
}

function setStore(next: TaskStore) {
  const previous = cache;
  cache = next;
  const ok = save(next);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? emptyStore));
  return ok;
}

function makeId(prefix: string) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function unique(list: string[]) {
  return Array.from(new Set(list));
}

function cleanDraft(draft: TaskDraft): TaskDraft {
  return {
    title: draft.title.trim(),
    description: draft.description.trim(),
    outputCriteria: draft.outputCriteria.trim(),
    priority: draft.priority,
    startDate: draft.startDate,
    dueDate: draft.dueDate,
    assigneeIds: unique(draft.assigneeIds),
  };
}

function getTaskAssignments(store: TaskStore, taskId: string) {
  return store.assignments.filter((assignment) => assignment.taskId === taskId);
}

function nextStatusAfterSubmission(store: TaskStore, taskId: string): StoredTaskStatus {
  const assignments = getTaskAssignments(store, taskId);
  return assignments.some((assignment) => assignment.status === "submitted")
    ? "in_review"
    : "in_progress";
}

function nextStatusAfterReview(store: TaskStore, taskId: string): StoredTaskStatus {
  const assignments = getTaskAssignments(store, taskId);
  if (assignments.length > 0 && assignments.every((a) => a.status === "completed")) {
    return "done";
  }
  if (
    assignments.some((a) => a.status === "submitted") &&
    !assignments.some((a) => a.status === "needs_feedback")
  ) {
    return "in_review";
  }
  return "in_progress";
}

export function getEffectiveTaskStatus(task: ProjectTask, now = new Date()): TaskStatus {
  if (task.status === "done" || task.status === "cancelled" || !task.dueDate) {
    return task.status;
  }
  const due = new Date(`${task.dueDate}T23:59:59`);
  return now.getTime() > due.getTime() ? "overdue" : task.status;
}

export function taskStatusTone(status: TaskStatus) {
  if (status === "done") return "emerald";
  if (status === "in_review") return "violet";
  if (status === "in_progress") return "cyan";
  if (status === "todo" || status === "draft") return "neutral";
  if (status === "overdue" || status === "cancelled") return "rose";
  return "neutral";
}

export function assignmentStatusTone(status: AssignmentStatus) {
  if (status === "completed") return "emerald";
  if (status === "submitted") return "violet";
  if (status === "working") return "cyan";
  if (status === "needs_feedback") return "amber";
  return "neutral";
}

export function priorityTone(priority: TaskPriority) {
  if (priority === "critical") return "rose";
  if (priority === "high") return "amber";
  if (priority === "medium") return "cyan";
  return "neutral";
}

export function latestSubmission(submissions: TaskSubmission[], taskId: string, memberId: string) {
  return submissions
    .filter((s) => s.taskId === taskId && s.memberId === memberId)
    .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0];
}

export function useProjectTasks() {
  const [store, setStateStore] = useState<TaskStore>(() => getStore());

  useEffect(() => {
    const cb: Sub = (next) => setStateStore(next);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const createTask = useCallback(
    (projectId: string, createdBy: string, draft: TaskDraft): Result => {
      const cleaned = cleanDraft(draft);
      const now = new Date().toISOString();
      const task: ProjectTask = {
        id: makeId("task"),
        projectId,
        ...cleaned,
        status: "draft",
        createdBy,
        createdAt: now,
        updatedAt: now,
      };
      const assignments = cleaned.assigneeIds.map((memberId) => ({
        id: makeId("assign"),
        taskId: task.id,
        memberId,
        status: "assigned" as AssignmentStatus,
      }));
      const ok = setStore({
        ...getStore(),
        tasks: [task, ...getStore().tasks],
        assignments: [...assignments, ...getStore().assignments],
      });
      return ok ? { ok: true, value: task } : { ok: false, error: "Task could not be saved." };
    },
    [],
  );

  const updateTask = useCallback((taskId: string, draft: TaskDraft): Result => {
    const current = getStore().tasks.find((task) => task.id === taskId);
    if (!current) return { ok: false, error: "Task not found." };
    if (current.status === "done" || current.status === "cancelled") {
      return { ok: false, error: "Completed or cancelled tasks cannot be edited." };
    }
    const cleaned = cleanDraft(draft);
    const existingAssignments = getStore().assignments.filter((a) => a.taskId === taskId);
    const kept = existingAssignments.filter((a) => cleaned.assigneeIds.includes(a.memberId));
    const added = cleaned.assigneeIds
      .filter((id) => !kept.some((a) => a.memberId === id))
      .map((memberId) => ({
        id: makeId("assign"),
        taskId,
        memberId,
        status: "assigned" as AssignmentStatus,
      }));
    const nextTask = {
      ...current,
      ...cleaned,
      updatedAt: new Date().toISOString(),
    };
    const otherAssignments = getStore().assignments.filter((a) => a.taskId !== taskId);
    const ok = setStore({
      ...getStore(),
      tasks: getStore().tasks.map((task) => (task.id === taskId ? nextTask : task)),
      assignments: [...kept, ...added, ...otherAssignments],
    });
    return ok ? { ok: true, value: nextTask } : { ok: false, error: "Task update failed." };
  }, []);

  const publishTask = useCallback((taskId: string): Result => {
    const current = getStore().tasks.find((task) => task.id === taskId);
    if (!current) return { ok: false, error: "Task not found." };
    if (current.status !== "draft")
      return { ok: false, error: "Only draft tasks can be published." };
    const next = {
      ...current,
      status: "todo" as StoredTaskStatus,
      updatedAt: new Date().toISOString(),
    };
    const ok = setStore({
      ...getStore(),
      tasks: getStore().tasks.map((task) => (task.id === taskId ? next : task)),
    });
    return ok ? { ok: true, value: next } : { ok: false, error: "Task publish failed." };
  }, []);

  const cancelTask = useCallback((taskId: string): Result => {
    const current = getStore().tasks.find((task) => task.id === taskId);
    if (!current) return { ok: false, error: "Task not found." };
    if (!["draft", "todo", "in_progress"].includes(current.status)) {
      return { ok: false, error: "This task cannot be cancelled from its current status." };
    }
    const next = {
      ...current,
      status: "cancelled" as StoredTaskStatus,
      updatedAt: new Date().toISOString(),
    };
    const ok = setStore({
      ...getStore(),
      tasks: getStore().tasks.map((task) => (task.id === taskId ? next : task)),
    });
    return ok ? { ok: true, value: next } : { ok: false, error: "Task cancellation failed." };
  }, []);

  const startWork = useCallback((taskId: string, memberId: string): Result<TaskAssignment> => {
    const currentStore = getStore();
    const task = currentStore.tasks.find((item) => item.id === taskId);
    const assignment = currentStore.assignments.find(
      (item) => item.taskId === taskId && item.memberId === memberId,
    );
    if (!task || !assignment) return { ok: false, error: "Task assignment not found." };
    if (task.status !== "todo" && task.status !== "in_progress") {
      return { ok: false, error: "Only To Do or In Progress tasks can be started." };
    }
    if (assignment.status !== "assigned" && assignment.status !== "needs_feedback") {
      return { ok: false, error: "This assignment cannot be started from its current status." };
    }
    const nextAssignment = {
      ...assignment,
      status: "working" as AssignmentStatus,
      startedAt: assignment.startedAt ?? new Date().toISOString(),
    };
    const nextTask = {
      ...task,
      status: "in_progress" as StoredTaskStatus,
      updatedAt: new Date().toISOString(),
    };
    const ok = setStore({
      ...currentStore,
      tasks: currentStore.tasks.map((item) => (item.id === taskId ? nextTask : item)),
      assignments: currentStore.assignments.map((item) =>
        item.id === assignment.id ? nextAssignment : item,
      ),
    });
    return ok
      ? { ok: true, value: nextAssignment }
      : { ok: false, error: "Assignment start failed." };
  }, []);

  const submitOutput = useCallback(
    (
      taskId: string,
      memberId: string,
      payload: { summary: string; artifactUrl?: string; notes?: string },
    ): Result<TaskSubmission> => {
      const currentStore = getStore();
      const task = currentStore.tasks.find((item) => item.id === taskId);
      const assignment = currentStore.assignments.find(
        (item) => item.taskId === taskId && item.memberId === memberId,
      );
      if (!task || !assignment) return { ok: false, error: "Task assignment not found." };
      if (task.status === "done" || task.status === "cancelled") {
        return { ok: false, error: "This task is closed for submissions." };
      }
      if (assignment.status !== "working" && assignment.status !== "needs_feedback") {
        return { ok: false, error: "Start or resume this task before submitting output." };
      }
      const summary = payload.summary.trim();
      if (!summary) return { ok: false, error: "Submission summary is required." };
      const artifactUrl = payload.artifactUrl?.trim();
      const notes = payload.notes?.trim();
      const now = new Date().toISOString();
      const submission: TaskSubmission = {
        id: makeId("sub"),
        taskId,
        memberId,
        summary,
        artifactUrl: artifactUrl || undefined,
        notes: notes || undefined,
        submittedAt: now,
      };
      const nextAssignment = {
        ...assignment,
        status: "submitted" as AssignmentStatus,
        submittedAt: now,
      };
      const intermediateStore = {
        ...currentStore,
        submissions: [submission, ...currentStore.submissions],
        assignments: currentStore.assignments.map((item) =>
          item.id === assignment.id ? nextAssignment : item,
        ),
      };
      const nextTask = {
        ...task,
        status: nextStatusAfterSubmission(intermediateStore, taskId),
        updatedAt: now,
      };
      const ok = setStore({
        ...intermediateStore,
        tasks: intermediateStore.tasks.map((item) => (item.id === taskId ? nextTask : item)),
      });
      return ok
        ? { ok: true, value: submission }
        : { ok: false, error: "Submission could not be saved." };
    },
    [],
  );

  const acceptSubmission = useCallback(
    (taskId: string, memberId: string, reviewedBy: string, reviewNote: string): Result => {
      const currentStore = getStore();
      const task = currentStore.tasks.find((item) => item.id === taskId);
      const assignment = currentStore.assignments.find(
        (item) => item.taskId === taskId && item.memberId === memberId,
      );
      if (!task || !assignment) return { ok: false, error: "Task assignment not found." };
      if (task.status !== "in_review" || assignment.status !== "submitted") {
        return { ok: false, error: "Only submitted work in review can be accepted." };
      }
      const now = new Date().toISOString();
      const nextAssignment = {
        ...assignment,
        status: "completed" as AssignmentStatus,
        reviewedAt: now,
        reviewedBy,
        reviewNote: reviewNote.trim() || undefined,
      };
      const intermediateStore = {
        ...currentStore,
        assignments: currentStore.assignments.map((item) =>
          item.id === assignment.id ? nextAssignment : item,
        ),
      };
      const nextTask = {
        ...task,
        status: nextStatusAfterReview(intermediateStore, taskId),
        updatedAt: now,
      };
      const ok = setStore({
        ...intermediateStore,
        tasks: intermediateStore.tasks.map((item) => (item.id === taskId ? nextTask : item)),
      });
      return ok
        ? { ok: true, value: nextTask }
        : { ok: false, error: "Review could not be saved." };
    },
    [],
  );

  const requestChanges = useCallback(
    (taskId: string, memberId: string, reviewedBy: string, reviewNote: string): Result => {
      const currentStore = getStore();
      const task = currentStore.tasks.find((item) => item.id === taskId);
      const assignment = currentStore.assignments.find(
        (item) => item.taskId === taskId && item.memberId === memberId,
      );
      const note = reviewNote.trim();
      if (!note) return { ok: false, error: "Feedback is required when requesting changes." };
      if (!task || !assignment) return { ok: false, error: "Task assignment not found." };
      if (task.status !== "in_review" || assignment.status !== "submitted") {
        return { ok: false, error: "Only submitted work in review can receive feedback." };
      }
      const now = new Date().toISOString();
      const nextAssignment = {
        ...assignment,
        status: "needs_feedback" as AssignmentStatus,
        reviewedAt: now,
        reviewedBy,
        reviewNote: note,
      };
      const nextTask = {
        ...task,
        status: "in_progress" as StoredTaskStatus,
        updatedAt: now,
      };
      const ok = setStore({
        ...currentStore,
        tasks: currentStore.tasks.map((item) => (item.id === taskId ? nextTask : item)),
        assignments: currentStore.assignments.map((item) =>
          item.id === assignment.id ? nextAssignment : item,
        ),
      });
      return ok
        ? { ok: true, value: nextTask }
        : { ok: false, error: "Feedback could not be saved." };
    },
    [],
  );

  return {
    tasks: store.tasks,
    assignments: store.assignments,
    submissions: store.submissions,
    createTask,
    updateTask,
    publishTask,
    cancelTask,
    startWork,
    submitOutput,
    acceptSubmission,
    requestChanges,
  };
}
