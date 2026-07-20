import { Link, useNavigate } from "@tanstack/react-router";
import { Bell, CheckCheck, ExternalLink, Trash2 } from "lucide-react";
import { useState } from "react";
import {
  formatNotificationTime,
  notificationTone,
  useNotifications,
  type AppNotification,
} from "@/lib/notifications-data";
import { StatusPill } from "./ui";
import { cn } from "@/lib/utils";

export function NotificationButton({ userId }: { userId: string }) {
  const navigate = useNavigate();
  const { notifications, unreadCount, markRead, markAllRead, remove } = useNotifications(userId);
  const [open, setOpen] = useState(false);
  const recent = notifications.slice(0, 5);

  const openNotification = (notification: AppNotification) => {
    markRead(notification.id, userId);
    setOpen(false);
    if (notification.link) navigate({ to: notification.link });
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        onBlur={() => setTimeout(() => setOpen(false), 160)}
        className="relative inline-flex h-9 w-9 items-center justify-center rounded-md border border-hairline bg-surface-elev text-ink-soft transition-colors hover:text-ink"
        aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ""}`}
      >
        <Bell className="h-4 w-4" />
        {unreadCount ? (
          <span className="absolute -right-1 -top-1 grid min-h-4 min-w-4 place-items-center rounded-full bg-[color:var(--destructive)] px-1 text-[10px] font-semibold leading-none text-white">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="absolute right-0 top-full z-50 mt-2 w-[min(360px,calc(100vw-2rem))] overflow-hidden rounded-xl border border-hairline bg-surface-elev shadow-xl">
          <header className="flex items-center justify-between gap-3 border-b border-hairline px-4 py-3">
            <div>
              <div className="text-sm font-semibold text-ink">Notifications</div>
              <div className="text-xs text-ink-soft">
                {unreadCount ? `${unreadCount} unread` : "All caught up"}
              </div>
            </div>
            <button
              type="button"
              disabled={!unreadCount}
              onMouseDown={(event) => {
                event.preventDefault();
                markAllRead(userId);
              }}
              className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-[11px] text-ink-soft hover:bg-muted hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Read all
            </button>
          </header>

          <div className="max-h-[360px] overflow-y-auto p-2">
            {recent.length === 0 ? (
              <div className="rounded-lg border border-dashed border-hairline px-4 py-8 text-center">
                <div className="text-sm font-medium text-ink">No notifications</div>
                <div className="mt-1 text-xs text-ink-soft">
                  Workflow updates for this account will appear here.
                </div>
              </div>
            ) : (
              <div className="space-y-1">
                {recent.map((notification) => (
                  <div
                    key={notification.id}
                    className={cn(
                      "group rounded-lg border border-transparent p-3 transition-colors hover:border-hairline hover:bg-muted/30",
                      !notification.readAt && "bg-muted/35",
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <button
                        type="button"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => openNotification(notification)}
                        className="min-w-0 flex-1 text-left"
                      >
                        <div className="flex flex-wrap items-center gap-2">
                          <StatusPill tone={notificationTone(notification.type)}>
                            {notification.readAt ? "Read" : "Unread"}
                          </StatusPill>
                          <span className="text-[11px] text-ink-soft">
                            {formatNotificationTime(notification.createdAt)}
                          </span>
                        </div>
                        <div className="mt-2 text-sm font-medium text-ink">
                          {notification.title}
                        </div>
                        <p className="mt-1 line-clamp-2 text-xs leading-relaxed text-ink-soft">
                          {notification.message}
                        </p>
                      </button>
                      <button
                        type="button"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => remove(notification.id, userId)}
                        className="rounded-md p-1 text-ink-soft opacity-100 hover:bg-muted hover:text-[color:var(--destructive)] md:opacity-0 md:group-hover:opacity-100"
                        aria-label="Delete notification"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <footer className="flex items-center justify-between border-t border-hairline px-4 py-3">
            <Link
              to="/app/notifications"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => setOpen(false)}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-ink hover:opacity-70"
            >
              Open notification center
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
            <span className="text-[11px] text-ink-soft">30-day history</span>
          </footer>
        </div>
      ) : null}
    </div>
  );
}
