import { useCallback, useEffect, useState } from "react";

export type CalendarEventScope = "lab" | "project";

export type LabCalendarEvent = {
  id: string;
  scope: CalendarEventScope;
  projectId?: string;
  title: string;
  description: string;
  location?: string;
  startAt: string;
  endAt: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type CalendarEventDraft = Pick<
  LabCalendarEvent,
  "scope" | "projectId" | "title" | "description" | "location" | "startAt" | "endAt"
>;

type Result<T = LabCalendarEvent> = { ok: true; value: T } | { ok: false; error: string };
type ActionResult = { ok: true } | { ok: false; error: string };

const STORAGE_KEY = "nova.calendarEvents.v1";
const scopes: CalendarEventScope[] = ["lab", "project"];

const seed: LabCalendarEvent[] = [
  {
    id: "cal_lab_research_forum_2026_07",
    scope: "lab",
    title: "Monthly research forum",
    description: "Lab-wide forum for project updates, paper discussions, and cross-team critique.",
    location: "Building E3 · Seminar room 4",
    startAt: "2026-07-18T08:30:00.000+07:00",
    endAt: "2026-07-18T10:00:00.000+07:00",
    createdBy: "u_admin",
    createdAt: "2026-07-01T09:00:00.000+07:00",
    updatedAt: "2026-07-01T09:00:00.000+07:00",
  },
  {
    id: "cal_atlas_sprint_review_2026_07",
    scope: "project",
    projectId: "prj_atlas",
    title: "Atlas perception sprint review",
    description:
      "Review localization traces, benchmark drift, and open risks for the next warehouse run.",
    location: "Robotics lab",
    startAt: "2026-07-20T14:00:00.000+07:00",
    endAt: "2026-07-20T15:30:00.000+07:00",
    createdBy: "u_tran",
    createdAt: "2026-07-04T10:20:00.000+07:00",
    updatedAt: "2026-07-04T10:20:00.000+07:00",
  },
  {
    id: "cal_helix_rebuttal_clinic_2026_07",
    scope: "project",
    projectId: "prj_helix",
    title: "Helix rebuttal clinic",
    description:
      "Prepare reviewer-response notes and assign experiment follow-ups for the program repair paper.",
    location: "Software studio",
    startAt: "2026-07-22T09:30:00.000+07:00",
    endAt: "2026-07-22T11:00:00.000+07:00",
    createdBy: "u_amara",
    createdAt: "2026-07-05T13:10:00.000+07:00",
    updatedAt: "2026-07-05T13:10:00.000+07:00",
  },
  {
    id: "cal_lab_demo_retro_2026_07",
    scope: "lab",
    title: "Demo day retrospective",
    description:
      "Short retrospective on demo flow, visitor questions, and follow-up documentation.",
    location: "Online",
    startAt: "2026-07-03T16:00:00.000+07:00",
    endAt: "2026-07-03T17:00:00.000+07:00",
    createdBy: "u_admin",
    createdAt: "2026-06-25T11:00:00.000+07:00",
    updatedAt: "2026-06-25T11:00:00.000+07:00",
  },
];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function isValidIso(value: string) {
  return !!value && !Number.isNaN(new Date(value).getTime());
}

function normalizeEvent(value: unknown): LabCalendarEvent | null {
  if (!isRecord(value)) return null;
  const id = readString(value.id);
  const scope = readString(value.scope) as CalendarEventScope;
  const title = readString(value.title);
  const description = readString(value.description);
  const startAt = readString(value.startAt);
  const endAt = readString(value.endAt);
  const createdBy = readString(value.createdBy);
  const createdAt = readString(value.createdAt);
  const updatedAt = readString(value.updatedAt) || createdAt;
  const projectId = readString(value.projectId) || undefined;

  if (
    !id ||
    !scopes.includes(scope) ||
    !title ||
    !description ||
    !createdBy ||
    !isValidIso(startAt) ||
    !isValidIso(endAt) ||
    !isValidIso(createdAt) ||
    !isValidIso(updatedAt) ||
    new Date(endAt).getTime() <= new Date(startAt).getTime()
  ) {
    return null;
  }
  if (scope === "project" && !projectId) return null;
  if (scope === "lab" && projectId) return null;

  return {
    id,
    scope,
    projectId,
    title,
    description,
    location: readString(value.location) || undefined,
    startAt,
    endAt,
    createdBy,
    createdAt,
    updatedAt,
  };
}

function load(): LabCalendarEvent[] {
  if (typeof window === "undefined") return seed;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seed;
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return seed;
    return parsed
      .map((item) => normalizeEvent(item))
      .filter((item): item is LabCalendarEvent => !!item);
  } catch {
    return seed;
  }
}

function save(list: LabCalendarEvent[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sortEvents(list)));
    return true;
  } catch {
    return false;
  }
}

type Sub = (list: LabCalendarEvent[]) => void;
const subs = new Set<Sub>();
let cache: LabCalendarEvent[] | null = null;

function getAll() {
  if (!cache) cache = load();
  return cache;
}

function setAll(next: LabCalendarEvent[]) {
  const previous = cache;
  cache = sortEvents(next);
  const ok = save(cache);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? []));
  return ok;
}

function makeId() {
  return `cal_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function sortEvents(list: LabCalendarEvent[]) {
  return [...list].sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());
}

function cleanDraft(draft: CalendarEventDraft): CalendarEventDraft {
  return {
    scope: draft.scope,
    projectId: draft.scope === "project" ? draft.projectId : undefined,
    title: draft.title.trim(),
    description: draft.description.trim(),
    location: draft.location?.trim() || undefined,
    startAt: draft.startAt,
    endAt: draft.endAt,
  };
}

export function validateCalendarDraft(draft: CalendarEventDraft) {
  const errors: Partial<Record<keyof CalendarEventDraft, string>> = {};
  if (!draft.title.trim()) errors.title = "Title is required.";
  if (!draft.description.trim()) errors.description = "Description is required.";
  if (!draft.startAt) errors.startAt = "Start date and time are required.";
  if (!draft.endAt) errors.endAt = "End date and time are required.";
  if (draft.scope === "project" && !draft.projectId) {
    errors.projectId = "Project is required for project events.";
  }
  if (draft.scope === "lab" && draft.projectId) {
    errors.projectId = "Lab-wide events cannot be tied to a project.";
  }
  if (draft.startAt && draft.endAt) {
    const start = new Date(draft.startAt);
    const end = new Date(draft.endAt);
    if (Number.isNaN(start.getTime())) errors.startAt = "Start date is invalid.";
    if (Number.isNaN(end.getTime())) errors.endAt = "End date is invalid.";
    if (!Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime()) && end <= start) {
      errors.endAt = "End must be after start.";
    }
  }
  return errors;
}

export function toDateTimeLocalValue(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
}

export function fromDateTimeLocalValue(value: string) {
  return value ? new Date(value).toISOString() : "";
}

export function formatCalendarDateTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Invalid date";
  return date.toLocaleString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatCalendarDateGroup(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Invalid date";
  return date.toLocaleDateString(undefined, {
    weekday: "long",
    month: "long",
    day: "numeric",
    year: "numeric",
  });
}

export function isPastEvent(event: LabCalendarEvent, now = new Date()) {
  return new Date(event.endAt).getTime() < now.getTime();
}

export function calendarScopeTone(scope: CalendarEventScope) {
  return scope === "lab" ? "violet" : "cyan";
}

export function useCalendarEvents() {
  const [list, setList] = useState<LabCalendarEvent[]>(() => getAll());

  useEffect(() => {
    const cb: Sub = (events) => setList(events);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const createEvent = useCallback((createdBy: string, draft: CalendarEventDraft): Result => {
    const errors = validateCalendarDraft(draft);
    if (Object.keys(errors).length > 0) return { ok: false, error: "Calendar event is invalid." };
    const cleaned = cleanDraft(draft);
    const now = new Date().toISOString();
    const event: LabCalendarEvent = {
      id: makeId(),
      ...cleaned,
      createdBy,
      createdAt: now,
      updatedAt: now,
    };
    const ok = setAll([event, ...getAll()]);
    return ok
      ? { ok: true, value: event }
      : { ok: false, error: "Calendar event could not be saved." };
  }, []);

  const updateEvent = useCallback((eventId: string, draft: CalendarEventDraft): Result => {
    const current = getAll().find((event) => event.id === eventId);
    if (!current) return { ok: false, error: "Event not found." };
    const errors = validateCalendarDraft(draft);
    if (Object.keys(errors).length > 0) return { ok: false, error: "Calendar event is invalid." };
    const next: LabCalendarEvent = {
      ...current,
      ...cleanDraft(draft),
      updatedAt: new Date().toISOString(),
    };
    const ok = setAll(getAll().map((event) => (event.id === eventId ? next : event)));
    return ok ? { ok: true, value: next } : { ok: false, error: "Calendar event update failed." };
  }, []);

  const removeEvent = useCallback((eventId: string): ActionResult => {
    const current = getAll().find((event) => event.id === eventId);
    if (!current) return { ok: false, error: "Event not found." };
    return setAll(getAll().filter((event) => event.id !== eventId))
      ? { ok: true }
      : { ok: false, error: "Calendar event delete failed." };
  }, []);

  return { events: list, createEvent, updateEvent, removeEvent };
}
