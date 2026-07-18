import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { DeletePostDialog } from "@/components/app/posts/DeletePostDialog";
import { PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import {
  estimateReadMinutes,
  formatPublicDate,
  getAuthorName,
  postCategoryLabel,
  postStatusLabel,
  postStatusTone,
  usePosts,
  type LabPost,
} from "@/lib/posts-data";
import { notifyOnce } from "@/lib/notifications-data";
import { X } from "lucide-react";

export const Route = createFileRoute("/app/moderation/posts_/$postId")({
  head: () => ({
    meta: [{ title: "Review Post — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: PostModerationDetailPage,
});

type ReviewMode = "revision" | "reject";

function PostModerationDetailPage() {
  const { postId } = Route.useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { posts, approve, requestRevision, reject, publish, remove } = usePosts();
  const [reviewMode, setReviewMode] = useState<ReviewMode | null>(null);
  const [reviewing, setReviewing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!user) return null;
  const post = posts.find((item) => item.id === postId);

  if (!user.roles.includes("admin")) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <h1 className="text-sm font-semibold text-ink">Unauthorized moderation</h1>
        <p className="mt-1 text-xs text-ink-soft">Only Admin users can moderate posts.</p>
        <Link to="/app/dashboard" className="mt-4 inline-flex text-sm text-ink hover:opacity-70">
          Back to dashboard
        </Link>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <h1 className="text-sm font-semibold text-ink">Post not found</h1>
        <p className="mt-1 text-xs text-ink-soft">This post may have been deleted.</p>
        <Link
          to="/app/moderation/posts"
          className="mt-4 inline-flex text-sm text-ink hover:opacity-70"
        >
          Back to moderation
        </Link>
      </div>
    );
  }

  const runSimpleAction = (kind: "approve" | "publish") => {
    if (reviewing) return;
    setReviewing(true);
    setError(null);
    setSuccess(null);
    const result = kind === "approve" ? approve(post.id, user.id) : publish(post.id, user.id);
    setReviewing(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    notifyOnce({
      userId: result.post.authorId,
      type: kind === "approve" ? "post_approved" : "post_published",
      title: kind === "approve" ? "Post approved" : "Post published",
      message:
        kind === "approve"
          ? `${result.post.title} was approved by an admin.`
          : `${result.post.title} is now published on the public site.`,
      link: kind === "approve" ? `/app/posts` : `/posts/${result.post.slug}`,
      eventKey: `post:${result.post.id}:${kind}:${result.post.authorId}`,
    });
    setSuccess(kind === "approve" ? "Post approved. Publish it when ready." : "Post published.");
  };

  const runReasonAction = (reason: string) => {
    if (!reviewMode || reviewing) return;
    setReviewing(true);
    setError(null);
    setSuccess(null);
    const result =
      reviewMode === "revision"
        ? requestRevision(post.id, user.id, reason)
        : reject(post.id, user.id, reason);
    setReviewing(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    notifyOnce({
      userId: result.post.authorId,
      type: reviewMode === "revision" ? "post_needs_revision" : "post_rejected",
      title: reviewMode === "revision" ? "Post needs revision" : "Post rejected",
      message:
        reviewMode === "revision"
          ? `${result.post.title} needs revision before it can be approved.`
          : `${result.post.title} was rejected by an admin.`,
      link: "/app/posts",
      eventKey: `post:${result.post.id}:${reviewMode}:${result.post.authorId}`,
    });
    setReviewMode(null);
    setSuccess(reviewMode === "revision" ? "Revision requested." : "Post rejected.");
  };

  const deletePost = () => {
    if (deleting) return;
    setDeleting(true);
    setError(null);
    const result = remove(post.id);
    setDeleting(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    navigate({ to: "/app/moderation/posts" });
  };

  return (
    <>
      <Link
        to="/app/moderation/posts"
        className="mb-4 inline-flex text-xs text-ink-soft hover:text-ink"
      >
        ← Moderation queue
      </Link>
      <PageHeader
        eyebrow="Admin moderation"
        title={post.title}
        description={post.excerpt}
        action={
          <div className="flex flex-wrap items-center gap-2">
            <StatusPill tone={postStatusTone(post.status)}>
              {postStatusLabel[post.status]}
            </StatusPill>
            <button
              onClick={() => setDeleteOpen(true)}
              className="rounded-md border border-[color:var(--destructive)]/30 px-3 py-1.5 text-xs font-medium text-[color:var(--destructive)] hover:bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)]"
            >
              Delete
            </button>
          </div>
        }
      />

      {error ? (
        <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}
      {success ? (
        <div className="mb-4 rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--emerald-ink)]">
          {success}
        </div>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <Panel
          title="Post preview"
          description="Authored content is preserved during review actions."
        >
          <PostPreview post={post} />
        </Panel>

        <aside className="space-y-4">
          <Panel title="Review actions">
            <div className="space-y-2">
              <button
                disabled={reviewing || post.status !== "pending_review"}
                onClick={() => runSimpleAction("approve")}
                className="w-full rounded-md bg-primary px-3.5 py-2 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Approve
              </button>
              <button
                disabled={reviewing || post.status !== "pending_review"}
                onClick={() => setReviewMode("revision")}
                className="w-full rounded-md border border-hairline px-3.5 py-2 text-sm font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
              >
                Request revision
              </button>
              <button
                disabled={reviewing || post.status !== "pending_review"}
                onClick={() => setReviewMode("reject")}
                className="w-full rounded-md border border-hairline px-3.5 py-2 text-sm font-medium text-ink hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
              >
                Reject
              </button>
              <button
                disabled={reviewing || post.status !== "approved"}
                onClick={() => runSimpleAction("publish")}
                className="w-full rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] px-3.5 py-2 text-sm font-medium text-[color:var(--emerald-ink)] hover:bg-[color-mix(in_oklab,var(--emerald-ink)_8%,transparent)] disabled:cursor-not-allowed disabled:opacity-40"
              >
                Publish approved post
              </button>
            </div>
            <p className="mt-3 text-xs leading-relaxed text-ink-soft">
              Approval does not publish automatically. Revision and rejection require a reason.
            </p>
          </Panel>

          <Panel title="Metadata">
            <dl className="space-y-2 text-sm">
              <Meta label="Author" value={getAuthorName(post.authorId)} />
              <Meta label="Category" value={postCategoryLabel[post.category]} />
              <Meta label="Slug" value={post.slug} mono />
              <Meta label="Updated" value={formatPublicDate(post.updatedAt)} />
              {post.submittedAt ? (
                <Meta label="Submitted" value={formatPublicDate(post.submittedAt)} />
              ) : null}
              {post.reviewedAt ? (
                <Meta label="Reviewed" value={formatPublicDate(post.reviewedAt)} />
              ) : null}
              {post.publishedAt ? (
                <Meta label="Published" value={formatPublicDate(post.publishedAt)} />
              ) : null}
            </dl>
          </Panel>
        </aside>
      </div>

      <ReviewReasonDialog
        mode={reviewMode}
        post={post}
        busy={reviewing}
        error={error}
        onClose={() => {
          setReviewMode(null);
          setError(null);
        }}
        onSubmit={runReasonAction}
      />
      <DeletePostDialog
        post={post}
        open={deleteOpen}
        deleting={deleting}
        error={error}
        onClose={() => {
          setDeleteOpen(false);
          setError(null);
        }}
        onConfirm={deletePost}
      />
    </>
  );
}

function PostPreview({ post }: { post: LabPost }) {
  return (
    <article>
      <div className="flex flex-wrap items-center gap-3 text-xs text-ink-soft">
        <span>{postCategoryLabel[post.category]}</span>
        <span>{estimateReadMinutes(post.content)} min read</span>
      </div>
      <h2 className="mt-4 font-display text-3xl leading-tight text-ink">{post.title}</h2>
      <p className="mt-3 text-base leading-relaxed text-ink-soft">{post.excerpt}</p>
      <div className="mt-8 space-y-5">
        {post.content.split(/\n{2,}/).map((paragraph) => (
          <p key={paragraph} className="text-sm leading-7 text-ink">
            {paragraph}
          </p>
        ))}
      </div>
      {post.reviewReason ? (
        <div className="mt-6 rounded-lg border border-hairline bg-muted/35 p-3 text-xs">
          <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">Review reason</div>
          <p className="mt-1 leading-relaxed text-ink">{post.reviewReason}</p>
        </div>
      ) : null}
    </article>
  );
}

function ReviewReasonDialog({
  mode,
  post,
  busy,
  error,
  onClose,
  onSubmit,
}: {
  mode: ReviewMode | null;
  post: LabPost;
  busy: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (reason: string) => void;
}) {
  const [reason, setReason] = useState("");
  useEffect(() => setReason(""), [mode, post.id]);
  if (!mode) return null;
  const invalid = !reason.trim();
  const title = mode === "revision" ? "Request revision" : "Reject post";

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(event) => event.stopPropagation()}
        className="w-full max-w-lg rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{title}</div>
            <h2 className="mt-0.5 text-sm font-semibold text-ink">{post.title}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="space-y-4 p-5">
          <label className="block">
            <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
              {mode === "revision" ? "Revision reason" : "Rejection reason"}
            </div>
            <textarea
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              className="min-h-[120px] w-full rounded-md border border-hairline bg-background px-2.5 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder={
                mode === "revision"
                  ? "Explain what the author should revise before resubmitting."
                  : "Explain why this post is rejected."
              }
            />
          </label>
          {invalid ? (
            <div className="text-xs text-[color:var(--destructive)]">Reason is required.</div>
          ) : null}
          {error ? (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {error}
            </div>
          ) : null}
        </div>
        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          <button
            onClick={onClose}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Cancel
          </button>
          <button
            disabled={invalid || busy}
            onClick={() => onSubmit(reason)}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {busy ? "Saving..." : title}
          </button>
        </footer>
      </div>
    </div>
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
