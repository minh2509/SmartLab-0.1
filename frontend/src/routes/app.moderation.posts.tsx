import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  adminPostStatusTone,
  contentTypeLabel,
  moderationStatusLabel,
  type AdminPostPage,
  type AdminPostSummary,
  useAdminPosts,
  usePendingAdminPosts,
  visibilityLabel,
} from "@/lib/admin-posts-api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/app/moderation/posts")({
  head: () => ({
    meta: [{ title: "Post Moderation — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: PostModerationPage,
});

const PAGE_SIZE = 10;
const emptySummary = "No summary provided by the backend.";

function PostModerationPage() {
  const { user, activeRole, accessToken } = useAuth();
  const [pendingPage, setPendingPage] = useState(0);
  const [allPostsPage, setAllPostsPage] = useState(0);
  const isAdmin = activeRole === "admin";
  const requestsEnabled = Boolean(user && isAdmin && accessToken);
  const pendingPosts = usePendingAdminPosts(accessToken, requestsEnabled, pendingPage, PAGE_SIZE);
  const allPosts = useAdminPosts(accessToken, requestsEnabled, allPostsPage, PAGE_SIZE);

  if (!user) return null;
  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <ShieldCheck className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Unauthorized moderation</h1>
        <p className="mt-1 text-xs text-ink-soft">Only Admin users can review and publish posts.</p>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow="Admin moderation"
        title="Post moderation"
        description="Review the backend moderation queue and inspect the administrator post catalogue."
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <Panel
          title="Moderation queue"
          description={`${pendingPosts.data.totalElements} pending review post(s)`}
          action={
            <PaginationControls
              page={pendingPosts.data}
              onPrevious={() => setPendingPage((page) => Math.max(0, page - 1))}
              onNext={() => setPendingPage((page) => page + 1)}
            />
          }
        >
          <PostPanelContent
            state={pendingPosts}
            loadingTitle="Loading moderation queue"
            loadingHint="Reading pending review posts from the backend..."
            errorTitle="Moderation queue could not be loaded"
            emptyTitle="Empty moderation queue"
            emptyHint="Submitted Member posts will appear here before approval."
          />
        </Panel>

        <Panel title="Oversight" description="Backend page metadata">
          <dl className="space-y-2 text-sm">
            <MetadataRow label="Total posts" value={allPosts.data.totalElements} />
            <MetadataRow label="Pending review posts" value={pendingPosts.data.totalElements} />
            <MetadataRow label="All-posts page" value={formatPagePosition(allPosts.data)} />
            <MetadataRow label="Pending page" value={formatPagePosition(pendingPosts.data)} />
          </dl>
        </Panel>
      </div>

      <div className="mt-4">
        <Panel
          title="All posts"
          description={`${allPosts.data.totalElements} total backend post(s)`}
          action={
            <PaginationControls
              page={allPosts.data}
              onPrevious={() => setAllPostsPage((page) => Math.max(0, page - 1))}
              onNext={() => setAllPostsPage((page) => page + 1)}
            />
          }
        >
          <PostPanelContent
            state={allPosts}
            loadingTitle="Loading posts"
            loadingHint="Reading the Admin post catalogue from the backend..."
            errorTitle="Posts could not be loaded"
            emptyTitle="No posts"
            emptyHint="The backend returned no posts for this page."
          />
        </Panel>
      </div>
    </>
  );
}

function PostPanelContent({
  state,
  loadingTitle,
  loadingHint,
  errorTitle,
  emptyTitle,
  emptyHint,
}: {
  state: {
    data: AdminPostPage;
    loading: boolean;
    error: string | null;
    retry: () => void;
  };
  loadingTitle: string;
  loadingHint: string;
  errorTitle: string;
  emptyTitle: string;
  emptyHint: string;
}) {
  if (state.loading) {
    return <EmptyState title={loadingTitle} hint={loadingHint} />;
  }
  if (state.error) {
    return (
      <div className="py-8 text-center">
        <EmptyState title={errorTitle} hint={state.error} />
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
    return <EmptyState title={emptyTitle} hint={emptyHint} />;
  }
  return <PostRows posts={state.data.content} />;
}

function PostRows({ posts }: { posts: AdminPostSummary[] }) {
  return (
    <div className="space-y-3">
      {posts.map((post) => (
        <article key={post.id} className="rounded-lg border border-hairline p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <StatusPill tone={adminPostStatusTone(post.moderationStatus)}>
                  {moderationStatusLabel(post.moderationStatus)}
                </StatusPill>
                <span className="text-xs text-ink-soft">{contentTypeLabel(post.contentType)}</span>
                <span className="text-xs text-ink-soft">{visibilityLabel(post.visibility)}</span>
              </div>
              <h2 className="mt-2 text-base font-semibold text-ink">{post.title}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-relaxed text-ink-soft">
                {post.summary?.trim() || emptySummary}
              </p>
              <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-soft">
                <span>{post.authorName}</span>
                <span>Updated {formatDate(post.updatedAt)}</span>
                {post.projectName ? <span>{post.projectName}</span> : null}
                {post.categoryName ? <span>{post.categoryName}</span> : null}
              </div>
            </div>
            <span
              aria-disabled="true"
              className="shrink-0 cursor-not-allowed rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink-soft opacity-70"
            >
              Detail pending
            </span>
          </div>
        </article>
      ))}
    </div>
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

function MetadataRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-ink-soft">{label}</dt>
      <dd className="font-medium text-ink">{value}</dd>
    </div>
  );
}

function formatPagePosition(page: AdminPostPage) {
  if (page.totalPages === 0) return "0 of 0";
  return `${page.page + 1} of ${page.totalPages}`;
}

function formatDate(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Unscheduled";
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
