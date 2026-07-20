import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { SectionHeading, Chip, Card } from "@/components/site/primitives";
import {
  lab,
  achievements,
  featuredProjects,
  featuredPeople,
  researchFields,
  activities,
} from "@/lib/lab-data";
import {
  estimateReadMinutes,
  formatPublicDate,
  postCategoryLabel,
  postCategoryTone,
  usePosts,
} from "@/lib/posts-data";

const activityTone: Record<string, "cyan" | "emerald" | "violet" | "amber"> = {
  "Demo day": "cyan",
  "Field trip": "emerald",
  Talk: "violet",
  Workshop: "amber",
};

export const Route = createFileRoute("/")({
  component: Index,
});

const fieldTone: Record<string, "cyan" | "emerald" | "violet"> = {
  Robotics: "emerald",
  "Software Engineering": "violet",
  "Artificial Intelligence": "cyan",
};

function Index() {
  const { publicPosts } = usePosts();

  return (
    <div className="min-h-screen bg-background text-ink">
      <SiteHeader />

      {/* Hero */}
      <section className="relative overflow-hidden border-b border-hairline">
        <div className="pointer-events-none absolute inset-0 grid-lines opacity-40" />
        <div className="pointer-events-none absolute -right-40 -top-40 h-[420px] w-[420px] rounded-full bg-[color-mix(in_oklab,var(--cyan)_18%,transparent)] blur-3xl" />
        <div className="relative mx-auto max-w-7xl px-6 pb-24 pt-20 md:pt-28">
          <div className="grid gap-12 lg:grid-cols-[1.4fr_1fr] lg:items-end">
            <div>
              <div className="flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-ink-soft">
                <span className="inline-block h-1.5 w-1.5 rounded-full bg-[color:var(--cyan)]" />
                Est. {lab.founded} · {lab.location}
              </div>
              <h1 className="mt-6 font-display text-5xl leading-[1.02] text-ink md:text-6xl lg:text-7xl">
                {lab.tagline}.
              </h1>
              <p className="mt-6 max-w-xl text-lg leading-relaxed text-ink-soft">{lab.intro}</p>
              <div className="mt-8 flex flex-wrap items-center gap-3">
                <a
                  href="#projects"
                  className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90"
                >
                  Explore projects
                </a>
                <a
                  href="#fields"
                  className="inline-flex items-center gap-2 rounded-md border border-hairline px-4 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-muted"
                >
                  Research fields
                </a>
              </div>
            </div>

            <div id="about" className="rounded-2xl border border-hairline bg-surface-elev p-6">
              <div className="flex items-center justify-between border-b border-hairline pb-4">
                <span className="text-xs uppercase tracking-[0.18em] text-ink-soft">
                  Lab snapshot
                </span>
                <span className="font-mono text-xs text-ink-soft">
                  Q{Math.floor(new Date().getMonth() / 3) + 1} · {new Date().getFullYear()}
                </span>
              </div>
              <dl className="mt-4 grid grid-cols-2 gap-4">
                {achievements.map((a) => (
                  <div key={a.label} className="rounded-lg bg-muted/50 p-4">
                    <dt className="text-[11px] uppercase tracking-wider text-ink-soft">
                      {a.label}
                    </dt>
                    <dd className="mt-1 font-display text-3xl text-ink">{a.metric}</dd>
                    <p className="mt-1 text-xs leading-snug text-ink-soft">{a.detail}</p>
                  </div>
                ))}
              </dl>
            </div>
          </div>
        </div>
      </section>

      {/* Featured projects */}
      <section id="projects" className="border-b border-hairline">
        <div className="mx-auto max-w-7xl px-6 py-20 md:py-28">
          <SectionHeading
            eyebrow="Featured projects"
            title="What we are building right now"
            description="A selection of live projects. Public summaries are shown here; internal artifacts, tasks, and evaluations live in the member portal."
          />
          <div className="mt-12 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {featuredProjects.map((p) => (
              <Card key={p.slug} className="flex h-full flex-col justify-between gap-6">
                <div>
                  <div className="flex items-center justify-between text-xs text-ink-soft">
                    <span className="font-mono">{p.code}</span>
                    <Chip tone={p.status === "Active" ? "cyan" : "amber"}>{p.status}</Chip>
                  </div>
                  <h3 className="mt-4 font-display text-2xl leading-tight text-ink">{p.title}</h3>
                  <p className="mt-3 text-sm leading-relaxed text-ink-soft">{p.summary}</p>
                </div>
                <div className="flex items-center justify-between border-t border-hairline pt-4 text-xs text-ink-soft">
                  <Chip tone={fieldTone[p.field] ?? "neutral"}>{p.field}</Chip>
                  <span>
                    {p.members} members · {p.year}
                  </span>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Research fields */}
      <section id="fields" className="border-b border-hairline bg-muted/30">
        <div className="mx-auto max-w-7xl px-6 py-20 md:py-28">
          <SectionHeading
            eyebrow="Research fields"
            title="Three focus areas, one lab"
            description="Our work sits at the intersection of these disciplines. Most projects touch at least two."
          />
          <div className="mt-12 grid gap-px overflow-hidden rounded-2xl border border-hairline bg-hairline md:grid-cols-3">
            {researchFields.map((f, i) => (
              <div key={f.key} className="flex flex-col justify-between gap-8 bg-surface-elev p-8">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs text-ink-soft">0{i + 1}</span>
                    <Chip
                      tone={f.key === "ai" ? "cyan" : f.key === "robotics" ? "emerald" : "violet"}
                    >
                      {f.projects} projects
                    </Chip>
                  </div>
                  <h3 className="mt-6 font-display text-3xl leading-tight text-ink">{f.name}</h3>
                  <p className="mt-3 text-sm leading-relaxed text-ink-soft">{f.description}</p>
                </div>
                <a
                  href="#projects"
                  className="inline-flex items-center gap-1.5 text-sm text-ink transition-opacity hover:opacity-70"
                >
                  See related projects <span aria-hidden>→</span>
                </a>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* People */}
      <section id="people" className="border-b border-hairline">
        <div className="mx-auto max-w-7xl px-6 py-20 md:py-28">
          <SectionHeading
            eyebrow="People"
            title="Researchers to know"
            description="Students, engineers, and scholars who currently drive the lab's work forward."
          />
          <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {featuredPeople.map((p) => (
              <Card key={p.name} className="flex flex-col gap-5">
                <div className="flex h-32 items-center justify-center rounded-lg bg-gradient-to-br from-[color-mix(in_oklab,var(--cyan)_18%,transparent)] to-transparent">
                  <span className="font-display text-4xl text-ink">{p.initials}</span>
                </div>
                <div>
                  <div className="font-medium text-ink">{p.name}</div>
                  <div className="mt-0.5 text-xs text-ink-soft">{p.role}</div>
                  <p className="mt-3 text-sm leading-snug text-ink-soft">{p.focus}</p>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Activity highlights */}
      <section id="activity" className="border-b border-hairline bg-muted/30">
        <div className="mx-auto max-w-7xl px-6 py-20 md:py-28">
          <SectionHeading
            eyebrow="Inside the lab"
            title="Recent activity & highlights"
            description="Demo days, field studies, invited talks, and the weekly rituals that keep the lab curious across cohorts."
          />
          {activities.length === 0 ? (
            <div className="mt-12 rounded-xl border border-dashed border-hairline p-10 text-center text-sm text-ink-soft">
              No public activity has been posted yet. Check back after the next demo day.
            </div>
          ) : (
            <div className="mt-12 grid gap-4 md:grid-cols-2">
              {activities.map((a) => (
                <Card key={a.title} className="flex flex-col gap-4">
                  <div className="flex items-center justify-between">
                    <Chip tone={activityTone[a.kind] ?? "neutral"}>{a.kind}</Chip>
                    <span className="font-mono text-xs text-ink-soft">{a.date}</span>
                  </div>
                  <h3 className="font-display text-2xl leading-tight text-ink">{a.title}</h3>
                  <p className="text-sm leading-relaxed text-ink-soft">{a.blurb}</p>
                </Card>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Latest public posts */}
      <section id="posts" className="border-b border-hairline">
        <div className="mx-auto max-w-7xl px-6 py-20 md:py-28">
          <SectionHeading
            eyebrow="Latest posts"
            title="News, publications, and announcements"
            description="Only public posts appear here. Internal drafts, reviews, and revisions live behind the member portal."
            action={
              <Link
                to="/posts"
                className="hidden text-sm text-ink underline-offset-4 hover:underline md:inline"
              >
                View all posts →
              </Link>
            }
          />
          {publicPosts.length === 0 ? (
            <div className="mt-12 rounded-xl border border-dashed border-hairline p-10 text-center text-sm text-ink-soft">
              No posts have been published yet.
            </div>
          ) : (
            <div className="mt-12 grid gap-4 md:grid-cols-3">
              {publicPosts.slice(0, 3).map((post) => (
                <Link key={post.id} to="/posts/$slug" params={{ slug: post.slug }}>
                  <Card className="flex h-full flex-col gap-4 transition-colors hover:border-ink/30">
                    <div className="flex items-center justify-between text-xs text-ink-soft">
                      <Chip tone={postCategoryTone(post.category)}>
                        {postCategoryLabel[post.category]}
                      </Chip>
                      <span className="font-mono">{formatPublicDate(post.publishedAt)}</span>
                    </div>
                    <h3 className="font-display text-xl leading-snug text-ink">{post.title}</h3>
                    <p className="text-sm leading-relaxed text-ink-soft">{post.excerpt}</p>
                    <div className="mt-auto flex items-center justify-between border-t border-hairline pt-4 text-xs text-ink-soft">
                      <span>{estimateReadMinutes(post.content)} min read</span>
                      <span className="text-ink">Read →</span>
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* CTA */}
      <section className="relative overflow-hidden border-b border-hairline bg-primary text-primary-foreground">
        <div className="pointer-events-none absolute inset-0 grid-lines opacity-[0.08]" />
        <div className="relative mx-auto flex max-w-7xl flex-col gap-8 px-6 py-20 md:flex-row md:items-end md:justify-between md:py-24">
          <div className="max-w-xl">
            <div className="flex items-center gap-2 text-xs uppercase tracking-[0.2em] opacity-70">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-[color:var(--cyan)]" />
              For members & collaborators
            </div>
            <h2 className="mt-5 font-display text-4xl leading-[1.05] md:text-5xl">
              Sign in to access projects, tasks, and reviews.
            </h2>
            <p className="mt-4 text-base leading-relaxed opacity-80">
              Public visitors see this page. Lab members, project leads, and administrators sign in
              to manage internal work.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <a
              href="/auth"
              className="inline-flex items-center gap-2 rounded-md bg-primary-foreground px-5 py-3 text-sm font-medium text-primary transition-opacity hover:opacity-90"
            >
              Sign in to portal
            </a>
            <a
              href="#projects"
              className="inline-flex items-center gap-2 rounded-md border border-primary-foreground/30 px-5 py-3 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-foreground/10"
            >
              View public projects
            </a>
          </div>
        </div>
      </section>

      <SiteFooter />
    </div>
  );
}
