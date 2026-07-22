import { ExternalLink, EyeOff, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { StatusPill } from "@/components/app/ui";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { AdminNotificationDetail } from "@/lib/admin-api";

export function AdminNotificationDetailDialog({
  open,
  notification,
  loading,
  hidePending,
  error,
  onClose,
  onHide,
}: {
  open: boolean;
  notification: AdminNotificationDetail | null;
  loading: boolean;
  hidePending: boolean;
  error: string | null;
  onClose: () => void;
  onHide: () => Promise<void>;
}) {
  const [confirmingHide, setConfirmingHide] = useState(false);

  useEffect(() => {
    setConfirmingHide(false);
  }, [open, notification?.id]);

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !hidePending) onClose();
      }}
    >
      <DialogContent className="max-h-[92vh] max-w-3xl overflow-y-auto border-hairline bg-surface-elev p-0">
        <DialogHeader className="border-b border-hairline px-5 py-4 pr-12">
          <DialogTitle className="text-base text-ink">Notification detail</DialogTitle>
          <DialogDescription className="text-xs text-ink-soft">
            Inspect delivery, read state, related data, and recipients before hiding this record.
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="space-y-4 p-5" aria-label="Loading notification detail">
            {[0, 1, 2].map((item) => (
              <div key={item} className="animate-pulse rounded-lg border border-hairline p-4">
                <div className="h-3 w-1/3 rounded bg-muted" />
                <div className="mt-3 h-2.5 w-4/5 rounded bg-muted" />
              </div>
            ))}
          </div>
        ) : !notification ? (
          <div className="p-5">
            <InlineError message={error || "The notification could not be loaded."} />
          </div>
        ) : (
          <div className="space-y-5 p-5">
            <section className="rounded-lg border border-hairline bg-muted/20 p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill tone="violet">{humanize(notification.notificationType)}</StatusPill>
                    <span className="text-xs text-ink-soft">
                      {formatDateTime(notification.createdAt)}
                    </span>
                  </div>
                  <h3 className="mt-3 text-base font-semibold text-ink">{notification.title}</h3>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-ink-soft">
                    {notification.message || "No message body was supplied."}
                  </p>
                </div>
                {notification.linkUrl ? (
                  <a
                    href={notification.linkUrl}
                    target={isExternal(notification.linkUrl) ? "_blank" : undefined}
                    rel={isExternal(notification.linkUrl) ? "noreferrer" : undefined}
                    className="inline-flex shrink-0 items-center gap-1 text-xs font-medium text-ink-soft hover:text-ink"
                  >
                    Open destination <ExternalLink className="h-3.5 w-3.5" />
                  </a>
                ) : null}
              </div>
            </section>

            <dl className="grid gap-3 rounded-lg border border-hairline p-4 sm:grid-cols-2">
              <Detail label="Notification ID" value={notification.id} mono />
              <Detail label="Created by" value={notification.createdBy?.fullName || "System"} />
              <Detail label="Recipients" value={String(notification.recipientCount)} />
              <Detail label="Read" value={String(notification.readCount)} />
              <Detail label="Related type" value={notification.relatedType || "Not linked"} />
              <Detail label="Related UUID" value={notification.relatedId || "Not linked"} mono />
            </dl>

            <section aria-labelledby="recipient-heading">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h3 id="recipient-heading" className="text-sm font-semibold text-ink">
                    Recipients
                  </h3>
                  <p className="mt-0.5 text-xs text-ink-soft">
                    Per-user delivery, read, and hidden state
                  </p>
                </div>
                <div className="inline-flex items-center gap-1.5 text-xs text-ink-soft">
                  <Users className="h-3.5 w-3.5" /> {notification.recipients.length}
                </div>
              </div>

              {notification.recipients.length ? (
                <div className="mt-3 overflow-x-auto rounded-lg border border-hairline">
                  <table className="w-full min-w-[640px] text-sm">
                    <thead>
                      <tr className="bg-muted/30 text-left text-[11px] uppercase tracking-[0.12em] text-ink-soft">
                        <th className="px-3 py-2.5 font-medium">Recipient</th>
                        <th className="px-3 py-2.5 font-medium">Delivery</th>
                        <th className="px-3 py-2.5 font-medium">Read at</th>
                        <th className="px-3 py-2.5 font-medium">Created</th>
                      </tr>
                    </thead>
                    <tbody>
                      {notification.recipients.map((recipient) => (
                        <tr key={recipient.userId} className="border-t border-hairline">
                          <td className="px-3 py-3">
                            <div className="font-medium text-ink">{recipient.fullName}</div>
                            <div className="mt-0.5 font-mono text-[11px] text-ink-soft">
                              {recipient.userId}
                            </div>
                          </td>
                          <td className="px-3 py-3">
                            <StatusPill tone={recipient.hiddenAt ? "neutral" : "emerald"}>
                              {recipient.hiddenAt ? "Hidden" : "Visible"}
                            </StatusPill>
                          </td>
                          <td className="px-3 py-3 text-xs text-ink-soft">
                            {recipient.readAt ? formatDateTime(recipient.readAt) : "Unread"}
                          </td>
                          <td className="px-3 py-3 text-xs text-ink-soft">
                            {formatDateTime(recipient.createdAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="mt-3 rounded-lg border border-dashed border-hairline p-5 text-center text-xs text-ink-soft">
                  No recipients were returned.
                </div>
              )}
            </section>

            {error ? <InlineError message={error} /> : null}

            <section className="rounded-lg border border-hairline p-4">
              {!confirmingHide ? (
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="text-sm font-medium text-ink">Hide this notification</div>
                    <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                      It will be hidden for every recipient, while the notification and audit
                      history remain stored.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setConfirmingHide(true)}
                    className="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-md border border-[color:var(--destructive)]/35 px-3 py-1.5 text-xs font-medium text-[color:var(--destructive)] hover:bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)]"
                  >
                    <EyeOff className="h-3.5 w-3.5" /> Hide globally
                  </button>
                </div>
              ) : (
                <div>
                  <div className="text-sm font-medium text-ink">
                    Hide this notification for all {notification.recipientCount} recipients?
                  </div>
                  <p className="mt-1 text-xs text-ink-soft">
                    It will disappear from the Admin list after this operation.
                  </p>
                  <div className="mt-4 flex justify-end gap-2">
                    <button
                      type="button"
                      disabled={hidePending}
                      onClick={() => setConfirmingHide(false)}
                      className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted disabled:opacity-45"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      disabled={hidePending}
                      onClick={() => void onHide()}
                      className="rounded-md bg-[color:var(--destructive)] px-3 py-1.5 text-xs font-medium text-white hover:opacity-90 disabled:opacity-45"
                    >
                      {hidePending ? "Hiding…" : "Confirm hide"}
                    </button>
                  </div>
                </div>
              )}
            </section>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Detail({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="text-[11px] uppercase tracking-[0.12em] text-ink-soft">{label}</dt>
      <dd className={`mt-1 break-words text-sm text-ink ${mono ? "font-mono text-xs" : ""}`}>
        {value}
      </dd>
    </div>
  );
}

function InlineError({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="rounded-md border border-[color:var(--destructive)]/35 bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]"
    >
      {message}
    </div>
  );
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown time";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function isExternal(value: string) {
  return value.startsWith("http://") || value.startsWith("https://");
}
