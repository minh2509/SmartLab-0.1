import { useEffect, useState } from "react";
import { X } from "lucide-react";
import type { UserAccount } from "@/lib/users-data";

export function AccountStatusDialog({
  user,
  action,
  open,
  pending,
  onClose,
  onConfirm,
}: {
  user: UserAccount | null;
  action: "lock" | "unlock";
  open: boolean;
  pending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const [confirmation, setConfirmation] = useState("");
  useEffect(() => setConfirmation(""), [user?.id, action, open]);
  if (!open || !user) return null;

  const phrase = action === "lock" ? "LOCK ACCOUNT" : "UNLOCK ACCOUNT";
  const valid = confirmation.trim() === phrase;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={pending ? undefined : onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-md rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {action === "lock" ? "Lock account" : "Unlock account"}
            </div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{user.fullName}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <div className="space-y-4 p-5">
          <p className="text-sm leading-relaxed text-ink-soft">
            {action === "lock"
              ? "The user will no longer be able to sign in. Existing project assignments, posts, tasks, evaluations, calendar events, and historical records remain unchanged."
              : "The user will be able to sign in again with their backend credentials. Historical records are not modified."}
          </p>
          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              Type {phrase}
            </div>
            <input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            />
          </label>
        </div>

        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={!valid || pending}
            onClick={onConfirm}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {pending ? "Saving..." : action === "lock" ? "Lock account" : "Unlock account"}
          </button>
        </footer>
      </div>
    </div>
  );
}
