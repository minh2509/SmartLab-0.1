import { useEffect, useState } from "react";
import type { LabCalendarEvent } from "@/lib/calendar-data";
import { AlertTriangle } from "lucide-react";

export function DeleteCalendarEventDialog({
  event,
  open,
  deleting,
  error,
  onClose,
  onConfirm,
}: {
  event: LabCalendarEvent | null;
  open: boolean;
  deleting: boolean;
  error?: string | null;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const [typed, setTyped] = useState("");
  useEffect(() => setTyped(""), [open, event?.id]);
  if (!open || !event) return null;
  const match = typed.trim() === event.title;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={deleting ? undefined : onClose}
    >
      <div
        onMouseDown={(mouseEvent) => mouseEvent.stopPropagation()}
        className="w-full max-w-lg rounded-xl border border-hairline bg-surface-elev p-5 shadow-xl"
      >
        <div className="flex items-start gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-full bg-[color-mix(in_oklab,var(--destructive)_14%,transparent)]">
            <AlertTriangle className="h-5 w-5 text-[color:var(--destructive)]" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-ink">Delete calendar event</h2>
            <p className="mt-1 text-xs leading-relaxed text-ink-soft">
              This removes <span className="font-medium text-ink">{event.title}</span> from the
              local calendar store. Past and future event history is not otherwise auto-deleted.
            </p>
          </div>
        </div>

        <div className="mt-4">
          <label className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
            Type the exact event title to confirm
          </label>
          <input
            value={typed}
            onChange={(changeEvent) => setTyped(changeEvent.target.value)}
            className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-[color:var(--destructive)]/40"
            placeholder={event.title}
            autoFocus
          />
        </div>

        {error ? (
          <div className="mt-3 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
            {error}
          </div>
        ) : null}

        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            onClick={onClose}
            disabled={deleting}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            disabled={!match || deleting}
            onClick={onConfirm}
            className="rounded-md bg-[color:var(--destructive)] px-3.5 py-1.5 text-sm font-medium text-white transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
          >
            {deleting ? "Deleting..." : "Delete event"}
          </button>
        </div>
      </div>
    </div>
  );
}
