import { useEffect, useState } from "react";
import type { LabPost } from "@/lib/posts-data";
import { AlertTriangle } from "lucide-react";

export function DeletePostDialog({
  post,
  open,
  deleting,
  error,
  onClose,
  onConfirm,
}: {
  post: LabPost | null;
  open: boolean;
  deleting: boolean;
  error?: string | null;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const [typed, setTyped] = useState("");
  useEffect(() => setTyped(""), [open, post?.id]);
  if (!open || !post) return null;
  const match = typed.trim() === post.title;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-lg rounded-xl border border-hairline bg-surface-elev p-5 shadow-xl"
      >
        <div className="flex items-start gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-full bg-[color-mix(in_oklab,var(--destructive)_14%,transparent)]">
            <AlertTriangle className="h-5 w-5 text-[color:var(--destructive)]" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-ink">Permanently delete post</h2>
            <p className="mt-1 text-xs leading-relaxed text-ink-soft">
              This removes <span className="font-medium text-ink">{post.title}</span> from the local
              post store. This action cannot be undone in the demo workspace.
            </p>
          </div>
        </div>

        <div className="mt-4">
          <label className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
            Type the exact post title to confirm
          </label>
          <input
            value={typed}
            onChange={(event) => setTyped(event.target.value)}
            className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-[color:var(--destructive)]/40"
            placeholder={post.title}
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
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            disabled={!match || deleting}
            onClick={onConfirm}
            className="rounded-md bg-[color:var(--destructive)] px-3.5 py-1.5 text-sm font-medium text-white transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
          >
            {deleting ? "Deleting..." : "Delete permanently"}
          </button>
        </div>
      </div>
    </div>
  );
}
