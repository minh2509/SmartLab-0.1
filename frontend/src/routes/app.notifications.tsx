import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Bell, Check, CheckCheck, Trash2 } from "lucide-react";
import { useState } from "react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import {
  formatNotificationTime,
  notificationTone,
  useNotifications,
  type AppNotification,
} from "@/lib/notifications-data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/notifications")({
  head: () => ({
    meta: [{ title: "Notifications — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: NotificationsPage,
});

type Filter = "all" | "unread" | "read";

function NotificationsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { notifications, unreadCount, markRead, markAllRead, remove, cleanupExpired } =
    useNotifications(user?.id);
  const [filter, setFilter] = useState<Filter>("all");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;

  const visible = notifications.filter((notification) => {
    if (filter === "unread") return !notification.readAt;
    if (filter === "read") return !!notification.readAt;
    return true;
  });

  const openNotification = (notification: AppNotification) => {
    const result = markRead(notification.id, user.id);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    if (notification.link) navigate({ to: notification.link });
    else setMessage("This notification has no destination link.");
  };

  const deleteNotification = (notificationId: string) => {
    const result = remove(notificationId, user.id);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setMessage("Notification deleted.");
    setError(null);
  };

  const cleanup = () => {
    const result = cleanupExpired();
    if (!result.ok) {
      setError("Expired notifications could not be cleaned up.");
      return;
    }
    setMessage(
      result.removed
        ? `${result.removed} expired notification${result.removed === 1 ? "" : "s"} removed.`
        : "No expired notifications to remove.",
    );
    setError(null);
  };

  return (
    <>
      <PageHeader
        eyebrow="Website notifications"
        title="Notifications"
        description="Recent workflow updates for your account. Notifications expire after 30 days."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <button
              disabled={!unreadCount}
              onClick={() => {
                const ok = markAllRead(user.id);
                setMessage(ok ? "All notifications marked as read." : null);
                setError(ok ? null : "Notifications could not be updated.");
              }}
              className="inline-flex items-center gap-1.5 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
            >
              <CheckCheck className="h-3.5 w-3.5" /> Mark all read
            </button>
            <button
              onClick={cleanup}
              className="rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
            >
              Cleanup expired
            </button>
          </div>
        }
      />

      {error ? (
        <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}
      {message ? (
        <div className="mb-4 rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--emerald-ink)]">
          {message}
        </div>
      ) : null}

      <Panel
        title="Notification center"
        description={`${notifications.length} active notification${notifications.length === 1 ? "" : "s"} · ${unreadCount} unread`}
        action={<FilterTabs filter={filter} onChange={setFilter} />}
      >
        {visible.length === 0 ? (
          <EmptyState
            title={
              filter === "unread"
                ? "No unread notifications"
                : filter === "read"
                  ? "No read notifications"
                  : "No notifications"
            }
            hint="Workflow notifications for this signed-in account will appear here."
          />
        ) : (
          <div className="space-y-3">
            {visible.map((notification) => (
              <article
                key={notification.id}
                className={cn(
                  "rounded-lg border border-hairline p-4",
                  !notification.readAt && "bg-muted/30",
                )}
              >
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <button
                    onClick={() => openNotification(notification)}
                    className="min-w-0 flex-1 text-left"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusPill tone={notificationTone(notification.type)}>
                        {notification.readAt ? "Read" : "Unread"}
                      </StatusPill>
                      <span className="text-xs text-ink-soft">
                        {formatNotificationTime(notification.createdAt)}
                      </span>
                    </div>
                    <h2 className="mt-2 text-sm font-semibold text-ink">{notification.title}</h2>
                    <p className="mt-1 text-sm leading-relaxed text-ink-soft">
                      {notification.message}
                    </p>
                    {!notification.link ? (
                      <div className="mt-2 text-xs text-ink-soft">
                        No destination link is attached.
                      </div>
                    ) : null}
                  </button>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    {!notification.readAt ? (
                      <button
                        onClick={() => {
                          const result = markRead(notification.id, user.id);
                          if (!result.ok) setError(result.error);
                        }}
                        className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted"
                      >
                        <Check className="h-3.5 w-3.5" /> Read
                      </button>
                    ) : null}
                    {notification.link ? (
                      <Link
                        to={notification.link}
                        onClick={() => markRead(notification.id, user.id)}
                        className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted"
                      >
                        <Bell className="h-3.5 w-3.5" /> Open
                      </Link>
                    ) : null}
                    <button
                      onClick={() => deleteNotification(notification.id)}
                      className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink-soft hover:bg-muted hover:text-[color:var(--destructive)]"
                    >
                      <Trash2 className="h-3.5 w-3.5" /> Delete
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>
    </>
  );
}

function FilterTabs({ filter, onChange }: { filter: Filter; onChange: (filter: Filter) => void }) {
  const filters: { key: Filter; label: string }[] = [
    { key: "all", label: "All" },
    { key: "unread", label: "Unread" },
    { key: "read", label: "Read" },
  ];
  return (
    <div className="flex rounded-md border border-hairline bg-background p-0.5">
      {filters.map((item) => (
        <button
          key={item.key}
          onClick={() => onChange(item.key)}
          className={cn(
            "rounded px-2.5 py-1 text-xs transition-colors",
            filter === item.key ? "bg-muted font-medium text-ink" : "text-ink-soft hover:text-ink",
          )}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}
