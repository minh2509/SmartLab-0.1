import { createFileRoute, Link } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import {
  formatPublicDate,
  getAuthorName,
  postCategoryLabel,
  postStatusLabel,
  postStatusTone,
  usePosts,
} from "@/lib/posts-data";

export const Route = createFileRoute("/app/moderation/posts")({
  head: () => ({
    meta: [{ title: "Post Moderation — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: PostModerationPage,
});

function PostModerationPage() {
  const { user } = useAuth();
  const { posts } = usePosts();

  if (!user) return null;
  if (!user.roles.includes("admin")) {
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

  const ordered = [...posts].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  );
  const pending = ordered.filter((post) => post.status === "pending_review");

  return (
    <>
      <PageHeader
        eyebrow="Admin moderation"
        title="Post moderation"
        description="Review submitted Member posts, approve them, request revisions, reject unsuitable drafts, or publish approved posts."
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <Panel title="Moderation queue" description={`${pending.length} pending review post(s)`}>
          {pending.length === 0 ? (
            <EmptyState
              title="Empty moderation queue"
              hint="Submitted Member posts will appear here before approval."
            />
          ) : (
            <PostRows posts={pending} />
          )}
        </Panel>

        <Panel title="Oversight" description="All post statuses">
          <dl className="space-y-2 text-sm">
            {["draft", "pending_review", "needs_revision", "approved", "published", "rejected"].map(
              (status) => (
                <div key={status} className="flex items-center justify-between gap-4">
                  <dt className="text-ink-soft">
                    {postStatusLabel[status as keyof typeof postStatusLabel]}
                  </dt>
                  <dd className="font-medium text-ink">
                    {posts.filter((post) => post.status === status).length}
                  </dd>
                </div>
              ),
            )}
          </dl>
        </Panel>
      </div>

      <div className="mt-4">
        <Panel title="All posts" description="Read-only list with detail actions for Admin users.">
          {ordered.length === 0 ? (
            <EmptyState
              title="No posts"
              hint="No drafts or published content exist in localStorage."
            />
          ) : (
            <PostRows posts={ordered} />
          )}
        </Panel>
      </div>
    </>
  );
}

function PostRows({ posts }: { posts: ReturnType<typeof usePosts>["posts"] }) {
  return (
    <div className="space-y-3">
      {posts.map((post) => (
        <article key={post.id} className="rounded-lg border border-hairline p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <StatusPill tone={postStatusTone(post.status)}>
                  {postStatusLabel[post.status]}
                </StatusPill>
                <span className="text-xs text-ink-soft">{postCategoryLabel[post.category]}</span>
              </div>
              <h2 className="mt-2 text-base font-semibold text-ink">{post.title}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-relaxed text-ink-soft">{post.excerpt}</p>
              <div className="mt-2 text-xs text-ink-soft">
                {getAuthorName(post.authorId)} · Updated {formatPublicDate(post.updatedAt)}
              </div>
            </div>
            <Link
              to="/app/moderation/posts/$postId"
              params={{ postId: post.id }}
              className="shrink-0 rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink hover:bg-muted"
            >
              Open
            </Link>
          </div>
        </article>
      ))}
    </div>
  );
}
