import { createFileRoute, Link } from "@tanstack/react-router";
import { FileText, Plus } from "lucide-react";
import { useState } from "react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import {
  estimateReadMinutes,
  formatPublicDate,
  postCategoryLabel,
  postStatusLabel,
  postStatusTone,
  usePosts,
} from "@/lib/posts-data";

export const Route = createFileRoute("/app/posts")({
  head: () => ({
    meta: [{ title: "My Posts — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: MemberPostsPage,
});

function MemberPostsPage() {
  const { user } = useAuth();
  const { posts, submitForReview } = usePosts();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!user) return null;
  const canUseMemberPosts = user.roles.includes("member");
  const myPosts = posts
    .filter((post) => post.authorId === user.id)
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

  const submit = (postId: string) => {
    if (busyId) return;
    setBusyId(postId);
    setError(null);
    setSuccess(null);
    const result = submitForReview(postId, user.id);
    if (result.ok) setSuccess("Post submitted for admin review.");
    else setError(result.error);
    setBusyId(null);
  };

  if (!canUseMemberPosts) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <FileText className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Member access required</h1>
        <p className="mt-1 text-xs text-ink-soft">
          Only users who possess the Member role can create and manage their own posts.
        </p>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow="Member publishing"
        title="My posts"
        description="Draft research-lab updates, submit them for admin review, and track publication status."
        action={
          <Link
            to="/app/posts/new"
            className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
          >
            <Plus className="h-3.5 w-3.5" /> New post
          </Link>
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

      <Panel title="Post history" description={`${myPosts.length} authored post(s)`}>
        {myPosts.length === 0 ? (
          <EmptyState
            title="No Member posts"
            hint="Create a draft before submitting it to the admin moderation queue."
          />
        ) : (
          <div className="space-y-3">
            {myPosts.map((post) => {
              const editable = post.status === "draft" || post.status === "needs_revision";
              return (
                <article key={post.id} className="rounded-lg border border-hairline p-4">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusPill tone={postStatusTone(post.status)}>
                          {postStatusLabel[post.status]}
                        </StatusPill>
                        <span className="text-xs text-ink-soft">
                          {postCategoryLabel[post.category]}
                        </span>
                      </div>
                      <h2 className="mt-2 text-base font-semibold text-ink">{post.title}</h2>
                      <p className="mt-1 max-w-2xl text-sm leading-relaxed text-ink-soft">
                        {post.excerpt}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      {editable ? (
                        <Link
                          to="/app/posts/$postId/edit"
                          params={{ postId: post.id }}
                          className="rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
                        >
                          Edit
                        </Link>
                      ) : null}
                      {editable ? (
                        <button
                          disabled={busyId === post.id}
                          onClick={() => submit(post.id)}
                          className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          {busyId === post.id ? "Submitting..." : "Submit"}
                        </button>
                      ) : null}
                    </div>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-4 border-t border-hairline pt-3 text-xs text-ink-soft">
                    <span>Updated · {formatPublicDate(post.updatedAt)}</span>
                    {post.publishedAt ? (
                      <span>Published · {formatPublicDate(post.publishedAt)}</span>
                    ) : null}
                    <span>{estimateReadMinutes(post.content)} min read</span>
                  </div>
                  {post.reviewReason ? (
                    <div className="mt-3 rounded-lg border border-hairline bg-muted/35 p-3 text-xs">
                      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        Review reason
                      </div>
                      <p className="mt-1 leading-relaxed text-ink">{post.reviewReason}</p>
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        )}
      </Panel>
    </>
  );
}
