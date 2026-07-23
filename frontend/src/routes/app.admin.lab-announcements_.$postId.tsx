import { createFileRoute, Link } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  adminPostStatusTone,
  contentTypeLabel,
  moderationStatusLabel,
  type AdminPostAttachment,
  type AdminPostDetail,
  type AdminPostFile,
  type AdminPostModerationHistory,
  useAdminLabAnnouncementDetail,
  visibilityLabel,
} from "@/lib/admin-posts-api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/app/admin/lab-announcements_/$postId")({
  head: () => ({
    meta: [
      { title: "Lab Announcement Detail — Smartworkspace" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: LabAnnouncementDetailPage,
});

const emptySummary = "No summary provided by the backend.";
const emptyContent = "No full content provided by the backend.";

function LabAnnouncementDetailPage() {
  const { postId } = Route.useParams();
  const { user, activeRole, accessToken, ready } = useAuth();
  const isAdmin = activeRole === "admin" && Boolean(user?.roles.includes("admin"));
  const detailRequest = useAdminLabAnnouncementDetail(
    accessToken,
    postId,
    Boolean(user && isAdmin && accessToken && postId),
  );
  const detail = detailRequest.data;

  if (!ready || !user) return null;

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <ShieldCheck className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Unauthorized announcements</h1>
        <p className="mt-1 text-xs text-ink-soft">
          Only Admin users can view lab announcement administration.
        </p>
        <Link to="/app/dashboard" className="mt-4 inline-flex text-sm text-ink hover:opacity-70">
          Back to dashboard
        </Link>
      </div>
    );
  }

  return (
    <>
      <Link
        to="/app/admin/lab-announcements"
        className="mb-4 inline-flex text-xs text-ink-soft hover:text-ink"
      >
        Back to lab announcements
      </Link>

      {detailRequest.loading ? (
        <Panel title="Loading lab announcement">
          <EmptyState
            title="Loading lab announcement"
            hint="Reading the backend announcement record..."
          />
        </Panel>
      ) : detailRequest.error ? (
        <Panel title="Lab announcement could not be loaded">
          <div className="space-y-4">
            <EmptyState title="Lab announcement unavailable" hint={detailRequest.error} />
            <div className="flex flex-wrap justify-center gap-2">
              <button
                type="button"
                onClick={detailRequest.retry}
                className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
              >
                Retry
              </button>
              <Link
                to="/app/admin/lab-announcements"
                className="rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
              >
                Back to lab announcements
              </Link>
            </div>
          </div>
        </Panel>
      ) : detail ? (
        <LabAnnouncementDetailContent detail={detail} />
      ) : (
        <Panel title="Lab announcement unavailable">
          <EmptyState
            title="Lab announcement unavailable"
            hint="The backend did not return announcement detail."
          />
        </Panel>
      )}
    </>
  );
}

function LabAnnouncementDetailContent({ detail }: { detail: AdminPostDetail }) {
  return (
    <>
      <PageHeader
        eyebrow="Admin content"
        title={detail.title}
        description={detail.summary?.trim() || emptySummary}
        action={
          <StatusPill tone={adminPostStatusTone(detail.moderationStatus)}>
            {moderationStatusLabel(detail.moderationStatus)}
          </StatusPill>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_340px]">
        <div className="space-y-4">
          <Panel
            title="Announcement content"
            description={`${contentTypeLabel(detail.contentType)} · ${visibilityLabel(detail.visibility)}`}
          >
            <article>
              <div className="flex flex-wrap items-center gap-2">
                <StatusPill tone={adminPostStatusTone(detail.moderationStatus)}>
                  {moderationStatusLabel(detail.moderationStatus)}
                </StatusPill>
                <span className="text-xs text-ink-soft">{visibilityLabel(detail.visibility)}</span>
              </div>
              <h2 className="mt-4 font-display text-3xl leading-tight text-ink">{detail.title}</h2>
              <p className="mt-3 text-base leading-relaxed text-ink-soft">
                {detail.summary?.trim() || emptySummary}
              </p>
              <div className="mt-8 space-y-5">
                {contentParagraphs(detail.content).map((paragraph) => (
                  <p key={paragraph} className="text-sm leading-7 text-ink">
                    {paragraph}
                  </p>
                ))}
              </div>
            </article>
          </Panel>

          <Panel title="Attachments" description={`${detail.attachments.length} attachment(s)`}>
            {detail.attachments.length === 0 ? (
              <EmptyState title="No attachments" hint="The backend returned no attachments." />
            ) : (
              <div className="space-y-3">
                {detail.attachments.map((attachment) => (
                  <AttachmentRow key={attachment.attachmentId} attachment={attachment} />
                ))}
              </div>
            )}
          </Panel>

          <Panel
            title="Moderation history"
            description={`${detail.moderationHistory.length} moderation event(s)`}
          >
            {detail.moderationHistory.length === 0 ? (
              <EmptyState
                title="No moderation history"
                hint="The backend returned no moderation events."
              />
            ) : (
              <div className="space-y-3">
                {detail.moderationHistory.map((entry) => (
                  <HistoryRow key={entry.id} entry={entry} />
                ))}
              </div>
            )}
          </Panel>
        </div>

        <aside className="space-y-4">
          <Panel title="Announcement actions">
            <div className="space-y-2">
              <UnavailableAction label="Edit" />
              <UnavailableAction label="Publish" />
              <UnavailableAction label="Delete" destructive />
            </div>
            <p className="mt-3 text-xs leading-relaxed text-ink-soft">
              Lab announcement mutation endpoints are unavailable until their backend dependencies
              are merged.
            </p>
          </Panel>

          <Panel title="Metadata">
            <dl className="space-y-2 text-sm">
              <Meta label="Author" value={detail.author?.fullName ?? "Unknown author"} />
              <Meta label="Slug" value={detail.slug} mono />
              <Meta label="Visibility" value={visibilityLabel(detail.visibility)} />
              <Meta label="Status" value={moderationStatusLabel(detail.moderationStatus)} />
              {detail.project ? <Meta label="Project" value={detail.project.name} /> : null}
              {detail.category ? <Meta label="Category" value={detail.category.name} /> : null}
              <Meta label="Created" value={formatDate(detail.createdAt)} />
              <Meta label="Updated" value={formatDate(detail.updatedAt)} />
              <Meta
                label="Published"
                value={detail.publishedAt ? formatDate(detail.publishedAt) : "Not published"}
              />
            </dl>
          </Panel>

          <Panel title="Cover file">
            {detail.coverFile ? (
              <FileMetadata file={detail.coverFile} />
            ) : (
              <EmptyState title="No cover file" hint="The backend returned no cover metadata." />
            )}
          </Panel>
        </aside>
      </div>
    </>
  );
}

function AttachmentRow({ attachment }: { attachment: AdminPostAttachment }) {
  return (
    <article className="rounded-lg border border-hairline p-4">
      <div className="font-medium text-ink">{attachment.originalName ?? "Unnamed attachment"}</div>
      <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-soft">
        <span>{attachment.mimeType ?? "Unknown MIME type"}</span>
        <span>{formatFileSize(attachment.fileSize)}</span>
        <span>{attachment.uploadedByName ?? "Unknown uploader"}</span>
        <span>{formatDate(attachment.createdAt)}</span>
      </div>
    </article>
  );
}

function HistoryRow({ entry }: { entry: AdminPostModerationHistory }) {
  return (
    <article className="rounded-lg border border-hairline p-4">
      <div className="flex flex-wrap items-center gap-2">
        <StatusPill tone="cyan">{moderationActionLabel(entry.action)}</StatusPill>
        <span className="text-xs text-ink-soft">{formatTransition(entry)}</span>
      </div>
      <div className="mt-2 text-xs text-ink-soft">
        {entry.actorName ?? "Unknown actor"} · {formatDate(entry.createdAt)}
      </div>
      {entry.reason?.trim() ? (
        <p className="mt-2 text-sm leading-relaxed text-ink">{entry.reason}</p>
      ) : null}
    </article>
  );
}

function FileMetadata({ file }: { file: AdminPostFile }) {
  return (
    <dl className="space-y-2 text-sm">
      <Meta label="Name" value={file.originalName} />
      <Meta label="MIME type" value={file.mimeType} />
      <Meta label="Size" value={formatFileSize(file.fileSize)} />
      <Meta label="Extension" value={file.fileExtension} />
      <Meta label="Created" value={formatDate(file.createdAt)} />
    </dl>
  );
}

function UnavailableAction({ label, destructive }: { label: string; destructive?: boolean }) {
  return (
    <button
      type="button"
      disabled
      aria-disabled="true"
      className={
        destructive
          ? "w-full cursor-not-allowed rounded-md border border-[color:var(--destructive)]/30 px-3.5 py-2 text-sm font-medium text-[color:var(--destructive)] opacity-40"
          : "w-full cursor-not-allowed rounded-md border border-hairline px-3.5 py-2 text-sm font-medium text-ink opacity-40"
      }
    >
      {label} unavailable until backend endpoint is merged
    </button>
  );
}

function Meta({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</dt>
      <dd className={`text-right text-sm text-ink ${mono ? "font-mono text-xs" : ""}`}>{value}</dd>
    </div>
  );
}

function contentParagraphs(content: string | null) {
  const trimmed = content?.trim();
  if (!trimmed) return [emptyContent];
  return trimmed
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
}

function formatTransition(entry: AdminPostModerationHistory) {
  if (!entry.fromStatus && !entry.toStatus) return "No status transition";
  const from = entry.fromStatus ? moderationStatusLabel(entry.fromStatus) : "No status";
  const to = entry.toStatus ? moderationStatusLabel(entry.toStatus) : "No status";
  return `${from} to ${to}`;
}

function moderationActionLabel(action: AdminPostModerationHistory["action"]) {
  if (action === "REQUEST_REVISION") return "Request revision";
  return action
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Unscheduled";
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatFileSize(size: number | null) {
  if (size === null) return "Unknown size";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}
