import { Link } from "@tanstack/react-router";
import { lab } from "@/lib/lab-data";

const nav = [
  { label: "About", href: "/#about" },
  { label: "People", href: "/#people" },
  { label: "Fields", href: "/#fields" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-hairline bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
        <Link to="/" className="flex items-center gap-2.5">
          <span className="grid h-8 w-8 place-items-center rounded-md bg-primary text-primary-foreground">
            <span className="font-display text-lg leading-none">N</span>
          </span>
          <div className="flex flex-col leading-tight">
            <span className="text-sm font-semibold tracking-tight text-ink">{lab.shortName}</span>
            <span className="text-[10px] uppercase tracking-[0.14em] text-ink-soft">
              Research Lab
            </span>
          </div>
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          <Link
            to="/projects"
            className="text-sm text-ink-soft transition-colors hover:text-ink"
            activeProps={{ className: "text-sm text-ink font-medium" }}
          >
            Projects
          </Link>
          <Link
            to="/posts"
            className="text-sm text-ink-soft transition-colors hover:text-ink"
            activeProps={{ className: "text-sm text-ink font-medium" }}
          >
            Posts
          </Link>
          {nav.map((n) => (
            <a
              key={n.href}
              href={n.href}
              className="text-sm text-ink-soft transition-colors hover:text-ink"
            >
              {n.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <Link
            to="/auth"
            className="hidden rounded-md px-3 py-1.5 text-sm text-ink-soft transition-colors hover:text-ink sm:inline-flex"
          >
            Sign in
          </Link>
          <Link
            to="/auth"
            className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90"
          >
            Member portal
            <span aria-hidden>→</span>
          </Link>
        </div>
      </div>
    </header>
  );
}
