import { useCallback, useEffect, useState } from "react";

export type NotificationType =
  | "join_request_received"
  | "join_request_approved"
  | "join_request_rejected"
  | "project_member_added"
  | "task_assigned"
  | "task_updated"
  | "task_submitted"
  | "task_reviewed"
  | "project_announcement"
  | "post_approved"
  | "post_rejected"
  | "post_needs_revision"
  | "post_published"
  | "evaluation_received"
  | "calendar_event_created";

export type AppNotification = {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  link?: string;
  createdAt: string;
  readAt?: string;
  eventKey?: string;
};

export type NotificationInput = Pick<
  AppNotification,
  "userId" | "type" | "title" | "message" | "link"
> & {
  eventKey?: string;
};

type Result<T = AppNotification> = { ok: true; value: T } | { ok: false; error: string };
type ActionResult = { ok: true } | { ok: false; error: string };

const STORAGE_KEY = "nova.notifications.v1";
const EXPIRATION_DAYS = 30;
const types: NotificationType[] = [
  "join_request_received",
  "join_request_approved",
  "join_request_rejected",
  "project_member_added",
  "task_assigned",
  "task_updated",
  "task_submitted",
  "task_reviewed",
  "project_announcement",
  "post_approved",
  "post_rejected",
  "post_needs_revision",
  "post_published",
  "evaluation_received",
  "calendar_event_created",
];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function makeId() {
  return `notif_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function isNotificationExpired(createdAt: string, now = new Date()) {
  const created = new Date(createdAt);
  if (Number.isNaN(created.getTime())) return true;
  const ageMs = now.getTime() - created.getTime();
  return ageMs > EXPIRATION_DAYS * 24 * 60 * 60 * 1000;
}

export function formatNotificationTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Unknown time";
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function normalizeNotification(value: unknown): AppNotification | null {
  if (!isRecord(value)) return null;
  const type = readString(value.type) as NotificationType;
  const id = readString(value.id);
  const userId = readString(value.userId);
  const title = readString(value.title);
  const message = readString(value.message);
  const createdAt = readString(value.createdAt);
  if (!id || !userId || !title || !message || !createdAt || !types.includes(type)) return null;
  return {
    id,
    userId,
    type,
    title,
    message,
    link: readString(value.link) || undefined,
    createdAt,
    readAt: readString(value.readAt) || undefined,
    eventKey: readString(value.eventKey) || undefined,
  };
}

function sortNotifications(list: AppNotification[]) {
  return [...list].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );
}

function save(list: AppNotification[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sortNotifications(list)));
    return true;
  } catch {
    return false;
  }
}

function purgeExpired(list: AppNotification[], now = new Date()) {
  return list.filter((notification) => !isNotificationExpired(notification.createdAt, now));
}

function load(): AppNotification[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    const normalized = parsed
      .map((item) => normalizeNotification(item))
      .filter((item): item is AppNotification => !!item);
    const cleaned = purgeExpired(normalized);
    if (cleaned.length !== normalized.length) save(cleaned);
    return sortNotifications(cleaned);
  } catch {
    return [];
  }
}

type Sub = (list: AppNotification[]) => void;
const subs = new Set<Sub>();
let cache: AppNotification[] | null = null;

function getAll() {
  if (!cache) cache = load();
  return cache;
}

function setAll(next: AppNotification[]) {
  const previous = cache;
  cache = sortNotifications(purgeExpired(next));
  const ok = save(cache);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? []));
  return ok;
}

function cleanInput(input: NotificationInput): NotificationInput {
  return {
    userId: input.userId,
    type: input.type,
    title: input.title.trim(),
    message: input.message.trim(),
    link: input.link?.trim() || undefined,
    eventKey: input.eventKey?.trim() || undefined,
  };
}

export function notifyOnce(input: NotificationInput): Result {
  const cleaned = cleanInput(input);
  if (!cleaned.userId || !cleaned.title || !cleaned.message) {
    return { ok: false, error: "Notification is missing required fields." };
  }
  const existing = cleaned.eventKey
    ? getAll().find(
        (notification) =>
          notification.userId === cleaned.userId && notification.eventKey === cleaned.eventKey,
      )
    : undefined;
  if (existing) return { ok: true, value: existing };
  const notification: AppNotification = {
    id: makeId(),
    ...cleaned,
    createdAt: new Date().toISOString(),
  };
  const ok = setAll([notification, ...getAll()]);
  return ok
    ? { ok: true, value: notification }
    : { ok: false, error: "Notification could not be saved." };
}

export function notifyManyOnce(inputs: NotificationInput[]) {
  return inputs.map((input) => notifyOnce(input));
}

export function notificationTone(type: NotificationType) {
  if (
    type === "join_request_approved" ||
    type === "post_approved" ||
    type === "post_published" ||
    type === "evaluation_received"
  ) {
    return "emerald";
  }
  if (type === "join_request_rejected" || type === "post_rejected") return "rose";
  if (type === "post_needs_revision" || type === "task_reviewed") return "amber";
  if (type.startsWith("task")) return "cyan";
  return "violet";
}

export function useNotifications(userId?: string) {
  const [list, setList] = useState<AppNotification[]>(() => getAll());

  useEffect(() => {
    const cb: Sub = (next) => setList(next);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const visible = userId
    ? sortNotifications(
        list.filter((item) => item.userId === userId && !isNotificationExpired(item.createdAt)),
      )
    : [];

  const markRead = useCallback((notificationId: string, ownerId: string): Result => {
    const current = getAll().find((item) => item.id === notificationId);
    if (!current) return { ok: false, error: "Notification not found." };
    if (current.userId !== ownerId) {
      return { ok: false, error: "You can only update your own notifications." };
    }
    if (current.readAt) return { ok: true, value: current };
    const next = { ...current, readAt: new Date().toISOString() };
    const ok = setAll(getAll().map((item) => (item.id === notificationId ? next : item)));
    return ok ? { ok: true, value: next } : { ok: false, error: "Notification update failed." };
  }, []);

  const markAllRead = useCallback((ownerId: string) => {
    const now = new Date().toISOString();
    return setAll(
      getAll().map((item) =>
        item.userId === ownerId && !item.readAt ? { ...item, readAt: now } : item,
      ),
    );
  }, []);

  const remove = useCallback((notificationId: string, ownerId: string): ActionResult => {
    const current = getAll().find((item) => item.id === notificationId);
    if (!current) return { ok: false, error: "Notification not found." };
    if (current.userId !== ownerId) {
      return { ok: false, error: "You can only delete your own notifications." };
    }
    return setAll(getAll().filter((item) => item.id !== notificationId))
      ? { ok: true }
      : { ok: false, error: "Notification delete failed." };
  }, []);

  const cleanupExpired = useCallback(() => {
    const before = getAll().length;
    const cleaned = purgeExpired(getAll());
    const ok = setAll(cleaned);
    return { ok, removed: before - cleaned.length };
  }, []);

  return {
    notifications: visible,
    unreadCount: visible.filter((item) => !item.readAt).length,
    markRead,
    markAllRead,
    remove,
    cleanupExpired,
  };
}
