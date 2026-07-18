import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { Chip } from "@/components/site/primitives";
import {
  estimateReadMinutes,
  formatPublicDate,
  getAuthorName,
  postCategoryLabel,
  postCategoryTone,
  usePosts,
} from "@/lib/posts-data";

export const Route = createFileRoute("/posts_/$slug")({
  head: () => ({
    meta: [{ title: "Post — Nova Research Lab" }],
  }),
  component: PublicPostDetailPage,
});

function PublicPostDetailPage() {
  const { slug } = Route.useParams();
  const { publicPosts } = usePosts();
  const post = publicPosts.find((item) => item.slug === slug);

  if (!post) {
    return (
      <div className="min-h-screen bg-background text-ink">
        <SiteHeader />
        <main className="mx-auto max-w-3xl px-6 py-20">
          <div className="rounded-xl border border-hairline bg-surface-elev p-8 text-center">
            <h1 className="font-display text-3xl text-ink">Post unavailable</h1>
            <p className="mt-2 text-sm text-ink-soft">
              This post is not published or no longer exists.
            </p>
            <Link
              to="/posts"
              className="mt-5 inline-flex rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
            >
              View public posts
            </Link>
          </div>
        </main>
        <SiteFooter />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-ink">
      <SiteHeader />
      <main>
        <article className="mx-auto max-w-3xl px-6 py-16 md:py-20">
          <Link to="/posts" className="text-sm text-ink-soft hover:text-ink">
            ← Public posts
          </Link>
          <div className="mt-8 flex flex-wrap items-center gap-3 text-xs text-ink-soft">
            <Chip tone={postCategoryTone(post.category)}>{postCategoryLabel[post.category]}</Chip>
            <span className="font-mono">{formatPublicDate(post.publishedAt)}</span>
            <span>{estimateReadMinutes(post.content)} min read</span>
          </div>
          <h1 className="mt-5 font-display text-4xl leading-tight text-ink md:text-5xl">
            {post.title}
          </h1>
          <p className="mt-5 text-lg leading-relaxed text-ink-soft">{post.excerpt}</p>
          <div className="mt-6 border-y border-hairline py-4 text-sm text-ink-soft">
            By <span className="font-medium text-ink">{getAuthorName(post.authorId)}</span>
          </div>
          <div className="prose prose-neutral mt-10 max-w-none">
            {post.content.split(/\n{2,}/).map((paragraph) => (
              <p key={paragraph} className="mb-5 text-base leading-8 text-ink">
                {paragraph}
              </p>
            ))}
          </div>
        </article>
      </main>
      <SiteFooter />
    </div>
  );
}
