import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Megaphone, ShieldCheck } from "lucide-react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  adminPostStatusTone,
  moderationStatusLabel,
  type AdminPostPage,
  type AdminPostSummary,
  useAdminLabAnnouncements,
  visibilityLabel,
} from "@/lib/admin-posts-api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/app/admin/lab-announcements")({
  head: () => ({
    meta: [{ title: "Lab Announcements — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: LabAnnouncementsPage,
});

const PAGE_SIZE = 10;
const emptySummary = "No summary provided by the backend.";

function LabAnnouncementsPage() {
  const { user, activeRole, accessToken, ready } = useAuth();
  const [page, setPage] = useState(0);
  const isAdmin = activeRole === "admin" && Boolean(user?.roles.includes("admin"));
  const announcements = useAdminLabAnnouncements(
    accessToken,
    Boolean(user && isAdmin && accessToken),
    page,
    PAGE_SIZE,
  );

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
      </div>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow="Admin content"
        title="Lab announcements"
        description="Review backend lab announcement records and their publication metadata."
        action={
          <button
            type="button"
            disabled
            className="inline-flex cursor-not-allowed items-center gap-2 rounded-md border border-hairline px-3.5 py-2 text-sm font-medium text-ink-soft opacity-70"
          >
            <Megaphone className="h-4 w-4" />
            Create announcement
          </button>
        }
      />

      <Panel
        title="Announcement catalogue"
        description={`${announcements.data.totalElements} total announcement(s) · ${formatPagePosition(announcements.data)}`}
        action={
          <PaginationControls
            page={announcements.data}
            onPrevious={() => setPage((current) => Math.max(0, current - 1))}
            onNext={() => setPage((current) => current + 1)}
          />
        }
      >
        <div className="mb-4 rounded-lg border border-hairline bg-muted/35 px-4 py-3 text-xs leading-relaxed text-ink-soft">
          Create announcement is unavailable until the lab announcement create endpoint is merged.
        </div>
        <AnnouncementListContent state={announcements} />
      </Panel>
    </>
  );
}

function AnnouncementListContent({
  state,
}: {
  state: {
    data: AdminPostPage;
    loading: boolean;
    error: string | null;
    retry: () => void;
  };
}) {
  if (state.loading) {
    return (
      <EmptyState
        title="Loading lab announcements"
        hint="Reading lab announcements from the backend..."
      />
    );
  }
  if (state.error) {
    return (
      <div className="py-8 text-center">
        <EmptyState title="Lab announcements could not be loaded" hint={state.error} />
        <button
          type="button"
          onClick={state.retry}
          className="mt-3 rounded-md border border-hairline px-3 py-1.5 text-xs text-ink hover:bg-muted"
        >
          Retry
        </button>
      </div>
    );
  }
  if (state.data.content.length === 0) {
    return (
      <EmptyState
        title="No lab announcements"
        hint="The backend returned no lab announcements for this page."
      />
    );
  }
  return (
    <div className="space-y-3">
      {state.data.content.map((announcement) => (
        <AnnouncementRow key={announcement.id} announcement={announcement} />
      ))}
    </div>
  );
}

function AnnouncementRow({ announcement }: { announcement: AdminPostSummary }) {
  return (
    <article className="rounded-lg border border-hairline p-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <StatusPill tone={adminPostStatusTone(announcement.moderationStatus)}>
              {moderationStatusLabel(announcement.moderationStatus)}
            </StatusPill>
            <span className="text-xs text-ink-soft">
              {visibilityLabel(announcement.visibility)}
            </span>
          </div>
          <h2 className="mt-2 text-base font-semibold text-ink">{announcement.title}</h2>
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-ink-soft">
            {announcement.summary?.trim() || emptySummary}
          </p>
          <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-soft">
            <span>{announcement.authorName?.trim() || "Unknown author"}</span>
            <span>Created {formatDate(announcement.createdAt)}</span>
            <span>Updated {formatDate(announcement.updatedAt)}</span>
            {announcement.publishedAt ? (
              <span>Published {formatDate(announcement.publishedAt)}</span>
            ) : null}
          </div>
        </div>
        <Link
          to="/app/admin/lab-announcements/$postId"
          params={{ postId: announcement.id }}
          className="shrink-0 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
        >
          View details
        </Link>
      </div>
    </article>
  );
}

function PaginationControls({
  page,
  onPrevious,
  onNext,
}: {
  page: AdminPostPage;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={onPrevious}
        disabled={page.first}
        className="rounded-md border border-hairline px-2 py-1 text-xs text-ink disabled:cursor-not-allowed disabled:opacity-40 enabled:hover:bg-muted"
      >
        Previous
      </button>
      <button
        type="button"
        onClick={onNext}
        disabled={page.last}
        className="rounded-md border border-hairline px-2 py-1 text-xs text-ink disabled:cursor-not-allowed disabled:opacity-40 enabled:hover:bg-muted"
      >
        Next
      </button>
    </div>
  );
}

function formatPagePosition(page: AdminPostPage) {
  if (page.totalPages === 0) return "page 0 of 0";
  return `page ${page.page + 1} of ${page.totalPages}`;
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
