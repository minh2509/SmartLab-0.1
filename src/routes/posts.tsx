import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { Card, Chip, SectionHeading } from "@/components/site/primitives";
import {
  estimateReadMinutes,
  formatPublicDate,
  getAuthorName,
  postCategoryLabel,
  postCategoryTone,
  usePosts,
} from "@/lib/posts-data";

export const Route = createFileRoute("/posts")({
  head: () => ({
    meta: [{ title: "Posts — SmartResearch Lab" }],
  }),
  component: PublicPostsPage,
});

function PublicPostsPage() {
  const { publicPosts } = usePosts();

  return (
    <div className="min-h-screen bg-background text-ink">
      <SiteHeader />
      <main>
        <section className="border-b border-hairline bg-muted/25">
          <div className="mx-auto max-w-7xl px-6 py-16 md:py-20">
            <SectionHeading
              eyebrow="Public posts"
              title="News, publications, and research stories"
              description="Published updates from SmartLab. Drafts, reviews, and revisions stay inside the member workspace."
            />
          </div>
        </section>

        <section className="border-b border-hairline">
          <div className="mx-auto max-w-7xl px-6 py-14 md:py-18">
            {publicPosts.length === 0 ? (
              <div className="rounded-xl border border-dashed border-hairline p-10 text-center text-sm text-ink-soft">
                No posts have been published yet.
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {publicPosts.map((post) => (
                  <Link key={post.id} to="/posts/$slug" params={{ slug: post.slug }}>
                    <Card className="flex h-full flex-col gap-4 transition-colors hover:border-ink/30">
                      <div className="flex items-center justify-between gap-3 text-xs text-ink-soft">
                        <Chip tone={postCategoryTone(post.category)}>
                          {postCategoryLabel[post.category]}
                        </Chip>
                        <span className="font-mono">{formatPublicDate(post.publishedAt)}</span>
                      </div>
                      <h2 className="font-display text-2xl leading-tight text-ink">{post.title}</h2>
                      <p className="text-sm leading-relaxed text-ink-soft">{post.excerpt}</p>
                      <div className="mt-auto flex items-center justify-between border-t border-hairline pt-4 text-xs text-ink-soft">
                        <span>{getAuthorName(post.authorId)}</span>
                        <span>{estimateReadMinutes(post.content)} min read</span>
                      </div>
                    </Card>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
