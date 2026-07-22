import { Link } from "@tanstack/react-router";
import { Check, ExternalLink, FileText, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { StatusPill } from "@/components/app/ui";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { AdminJoinRequest, AdminJoinRequestStatus } from "@/lib/admin-api";

type Decision = "approve" | "reject" | null;

export function AdminJoinRequestDetailDialog({
  open,
  request,
  loading,
  mutationPending,
  overrideDisabled,
  error,
  onClose,
  onApprove,
  onReject,
}: {
  open: boolean;
  request: AdminJoinRequest | null;
  loading: boolean;
  mutationPending: boolean;
  overrideDisabled: boolean;
  error: string | null;
  onClose: () => void;
  onApprove: () => Promise<void>;
  onReject: (reason: string) => Promise<void>;
}) {
  const [decision, setDecision] = useState<Decision>(null);
  const [reason, setReason] = useState("");
  const normalizedReason = reason.trim();

  useEffect(() => {
    setDecision(null);
    setReason("");
  }, [open, request?.id]);

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !mutationPending) onClose();
      }}
    >
      <DialogContent className="max-h-[92vh] max-w-3xl overflow-y-auto border-hairline bg-surface-elev p-0">
        <DialogHeader className="border-b border-hairline px-5 py-4 pr-12">
          <DialogTitle className="text-base text-ink">Join request detail</DialogTitle>
          <DialogDescription className="text-xs text-ink-soft">
            Review applicant information and safe CV metadata before making a decision.
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="space-y-4 p-5" aria-label="Loading join request">
            {[0, 1, 2, 3].map((item) => (
              <div key={item} className="animate-pulse rounded-lg border border-hairline p-4">
                <div className="h-3 w-1/3 rounded bg-muted" />
                <div className="mt-3 h-2.5 w-4/5 rounded bg-muted" />
              </div>
            ))}
          </div>
        ) : !request ? (
          <div className="p-5">
            <InlineError message={error || "The join request could not be loaded."} />
          </div>
        ) : (
          <div className="space-y-5 p-5">
            <div className="flex flex-col gap-3 rounded-lg border border-hairline bg-muted/25 p-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-base font-semibold text-ink">{request.project.name}</h3>
                  <StatusPill tone={statusTone(request.status)}>{request.status}</StatusPill>
                </div>
                <div className="mt-1 font-mono text-xs text-ink-soft">
                  {request.project.code} · {request.project.id}
                </div>
              </div>
              <Link
                to="/projects/$slug"
                params={{ slug: request.project.slug }}
                className="inline-flex shrink-0 items-center gap-1 text-xs font-medium text-ink-soft hover:text-ink"
              >
                Public project <ExternalLink className="h-3.5 w-3.5" />
              </Link>
            </div>

            <section aria-labelledby="applicant-heading">
              <h3
                id="applicant-heading"
                className="text-[11px] uppercase tracking-[0.14em] text-ink-soft"
              >
                Applicant
              </h3>
              <dl className="mt-2 grid gap-3 rounded-lg border border-hairline p-4 sm:grid-cols-2">
                <Detail label="Full name" value={request.requester.fullName} />
                <Detail label="Email" value={request.requester.email} />
                <Detail label="User ID" value={request.requester.id} mono />
                <Detail
                  label="Desired position"
                  value={request.desiredPosition || "Not provided"}
                />
                <Detail label="Submitted" value={formatDateTime(request.createdAt)} />
                <Detail label="Last updated" value={formatDateTime(request.updatedAt)} />
              </dl>
            </section>

            <section aria-labelledby="application-heading">
              <h3
                id="application-heading"
                className="text-[11px] uppercase tracking-[0.14em] text-ink-soft"
              >
                Application
              </h3>
              <div className="mt-2 grid gap-3 sm:grid-cols-2">
                <TextBlock label="Reason" value={request.reason} />
                <TextBlock label="Skills" value={request.skills} />
                <TextBlock label="Experience" value={request.experience} />
                <TextBlock label="Introduction" value={request.introduction} />
              </div>
            </section>

            <section aria-labelledby="cv-heading">
              <h3 id="cv-heading" className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                CV metadata
              </h3>
              {request.cvFile ? (
                <div className="mt-2 flex items-start gap-3 rounded-lg border border-hairline p-4">
                  <div className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-muted">
                    <FileText className="h-4 w-4 text-ink-soft" />
                  </div>
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-ink">
                      {request.cvFile.originalName}
                    </div>
                    <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-soft">
                      <span>{request.cvFile.mimeType || "Unknown MIME type"}</span>
                      <span>{formatBytes(request.cvFile.fileSize)}</span>
                      <span>{humanize(request.cvFile.visibility)}</span>
                    </div>
                    <div className="mt-2 text-[11px] text-ink-soft">
                      Only safe metadata is available. Storage names and internal paths are never
                      exposed by this API.
                    </div>
                  </div>
                </div>
              ) : (
                <div className="mt-2 rounded-lg border border-dashed border-hairline px-4 py-5 text-sm text-ink-soft">
                  No CV is attached to this request.
                </div>
              )}
            </section>

            {request.status !== "PENDING" ? (
              <section className="rounded-lg border border-hairline bg-muted/20 p-4">
                <div className="text-sm font-medium text-ink">Review outcome</div>
                <div className="mt-2 grid gap-2 text-xs text-ink-soft sm:grid-cols-2">
                  <div>Reviewed by: {request.reviewedBy?.fullName || "Unknown reviewer"}</div>
                  <div>Reviewed at: {formatOptionalDateTime(request.reviewedAt)}</div>
                </div>
                {request.rejectionReason ? (
                  <p className="mt-3 text-sm leading-relaxed text-ink">
                    Rejection reason: {request.rejectionReason}
                  </p>
                ) : null}
              </section>
            ) : null}

            {error ? <InlineError message={error} /> : null}

            {request.status === "PENDING" ? (
              <section className="rounded-lg border border-hairline p-4">
                {overrideDisabled ? (
                  <div className="rounded-md border border-[color:var(--amber-ink)]/35 bg-[color-mix(in_oklab,var(--amber-ink)_8%,transparent)] px-3 py-2">
                    <div className="text-sm font-medium text-ink">Admin override is disabled</div>
                    <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                      Enable SMARTLAB_ADMIN_JOIN_REQUEST_OVERRIDE_ENABLED on the backend and restart
                      it before using Approve or Reject.
                    </p>
                  </div>
                ) : !decision ? (
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <div className="text-sm font-medium text-ink">Admin override decision</div>
                      <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                        This policy-controlled action may be disabled by backend configuration.
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => setDecision("reject")}
                        className="inline-flex items-center gap-1.5 rounded-md border border-[color:var(--destructive)]/35 px-3 py-1.5 text-xs font-medium text-[color:var(--destructive)] hover:bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)]"
                      >
                        <XCircle className="h-3.5 w-3.5" /> Reject
                      </button>
                      <button
                        type="button"
                        onClick={() => setDecision("approve")}
                        className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
                      >
                        <Check className="h-3.5 w-3.5" /> Approve
                      </button>
                    </div>
                  </div>
                ) : decision === "approve" ? (
                  <div>
                    <div className="text-sm font-medium text-ink">
                      Approve this request and add the applicant as an active project member?
                    </div>
                    <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                      The backend will record an audit entry and notify the applicant.
                    </p>
                    <DecisionButtons
                      pending={mutationPending}
                      confirmLabel="Confirm approval"
                      onCancel={() => setDecision(null)}
                      onConfirm={() => void onApprove()}
                    />
                  </div>
                ) : (
                  <form
                    onSubmit={(event) => {
                      event.preventDefault();
                      if (normalizedReason && normalizedReason.length <= 2000) {
                        void onReject(normalizedReason);
                      }
                    }}
                  >
                    <label
                      htmlFor="admin-join-rejection-reason"
                      className="text-sm font-medium text-ink"
                    >
                      Rejection reason
                    </label>
                    <textarea
                      id="admin-join-rejection-reason"
                      autoFocus
                      rows={4}
                      maxLength={2000}
                      value={reason}
                      disabled={mutationPending}
                      onChange={(event) => setReason(event.target.value)}
                      className="mt-2 w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/35 disabled:opacity-50"
                      placeholder="Explain why the request cannot be approved."
                    />
                    <div className="mt-1 flex justify-between text-[11px] text-ink-soft">
                      <span>
                        {reason.length > 0 && !normalizedReason
                          ? "A reason is required."
                          : "Required"}
                      </span>
                      <span>{reason.length}/2000</span>
                    </div>
                    <DecisionButtons
                      pending={mutationPending}
                      confirmLabel="Confirm rejection"
                      confirmDisabled={!normalizedReason}
                      onCancel={() => setDecision(null)}
                    />
                  </form>
                )}
              </section>
            ) : null}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function DecisionButtons({
  pending,
  confirmLabel,
  confirmDisabled,
  onCancel,
  onConfirm,
}: {
  pending: boolean;
  confirmLabel: string;
  confirmDisabled?: boolean;
  onCancel: () => void;
  onConfirm?: () => void;
}) {
  return (
    <div className="mt-4 flex justify-end gap-2">
      <button
        type="button"
        disabled={pending}
        onClick={onCancel}
        className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted disabled:opacity-45"
      >
        Back
      </button>
      <button
        type={onConfirm ? "button" : "submit"}
        disabled={pending || confirmDisabled}
        onClick={onConfirm}
        className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-45"
      >
        {pending ? "Saving decision…" : confirmLabel}
      </button>
    </div>
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

function TextBlock({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="rounded-lg border border-hairline p-4">
      <div className="text-[11px] uppercase tracking-[0.12em] text-ink-soft">{label}</div>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-ink">
        {value || "Not provided"}
      </p>
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

function statusTone(status: AdminJoinRequestStatus) {
  if (status === "APPROVED") return "emerald" as const;
  if (status === "REJECTED") return "rose" as const;
  if (status === "CANCELLED") return "neutral" as const;
  return "amber" as const;
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

function formatOptionalDateTime(value: string | null) {
  return value ? formatDateTime(value) : "Not recorded";
}

function formatBytes(value: number | null) {
  if (value === null || value < 0) return "Unknown size";
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}
