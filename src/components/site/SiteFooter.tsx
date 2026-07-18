import { lab } from "@/lib/lab-data";

export function SiteFooter() {
  return (
    <footer className="border-t border-hairline">
      <div className="mx-auto grid max-w-7xl gap-10 px-6 py-14 md:grid-cols-4">
        <div className="md:col-span-2">
          <div className="flex items-center gap-2.5">
            <span className="grid h-8 w-8 place-items-center rounded-md bg-primary text-primary-foreground">
              <span className="font-display text-lg leading-none">N</span>
            </span>
            <span className="text-sm font-semibold tracking-tight text-ink">{lab.name}</span>
          </div>
          <p className="mt-4 max-w-md text-sm text-ink-soft">{lab.intro}</p>
        </div>
        <div>
          <div className="text-xs uppercase tracking-[0.14em] text-ink-soft">Lab</div>
          <ul className="mt-4 space-y-2 text-sm text-ink">
            <li>{lab.location}</li>
            <li>Founded {lab.founded}</li>
            <li>contact@novaresearch.lab</li>
          </ul>
        </div>
        <div>
          <div className="text-xs uppercase tracking-[0.14em] text-ink-soft">Explore</div>
          <ul className="mt-4 space-y-2 text-sm text-ink">
            <li>
              <a href="#projects" className="hover:underline">
                Projects
              </a>
            </li>
            <li>
              <a href="#people" className="hover:underline">
                People
              </a>
            </li>
            <li>
              <a href="#fields" className="hover:underline">
                Research fields
              </a>
            </li>
          </ul>
        </div>
      </div>
      <div className="border-t border-hairline">
        <div className="mx-auto flex max-w-7xl flex-col items-start justify-between gap-2 px-6 py-5 text-xs text-ink-soft md:flex-row md:items-center">
          <span>
            © {new Date().getFullYear()} {lab.name}. All work licensed for academic use.
          </span>
          <span className="font-mono">v0.1 · public preview</span>
        </div>
      </div>
    </footer>
  );
}
