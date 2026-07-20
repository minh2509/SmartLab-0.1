import { useEffect, useState } from "react";
import type { Project } from "@/lib/projects-data";
import { AlertTriangle } from "lucide-react";

export function DeleteProjectDialog({
  project,
  open,
  onClose,
  onConfirm,
}: {
  project: Project | null;
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const [typed, setTyped] = useState("");
  useEffect(() => setTyped(""), [open]);
  if (!open || !project) return null;
  const match = typed.trim() === project.code;

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-xl border border-hairline bg-surface-elev p-5 shadow-xl"
      >
        <div className="flex items-start gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-full bg-[color-mix(in_oklab,var(--destructive)_14%,transparent)]">
            <AlertTriangle className="h-5 w-5 text-[color:var(--destructive)]" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-ink">Delete project</h2>
            <p className="mt-1 text-xs text-ink-soft">
              This will permanently remove{" "}
              <span className="font-medium text-ink">{project.name}</span> and all its metadata from
              the workspace. This action cannot be undone.
            </p>
          </div>
        </div>

        <div className="mt-4">
          <label className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
            Type the project code <span className="font-mono text-ink">{project.code}</span> to
            confirm
          </label>
          <input
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            className="mt-1 w-full rounded-md border border-hairline bg-background px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-[color:var(--destructive)]/40"
            placeholder={project.code}
            autoFocus
          />
        </div>

        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            onClick={onClose}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            disabled={!match}
            onClick={onConfirm}
            className="rounded-md bg-[color:var(--destructive)] px-3.5 py-1.5 text-sm font-medium text-white transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
          >
            Delete project
          </button>
        </div>
      </div>
    </div>
  );
}
