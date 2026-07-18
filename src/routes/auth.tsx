import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { DEMO_PASSWORD, roleLabel, useAuth } from "@/lib/auth";
import { useUsers } from "@/lib/users-data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/auth")({
  head: () => ({
    meta: [
      { title: "Sign in — Nova Research Lab" },
      { name: "description", content: "Sign in to the Nova Research Lab workspace." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AuthPage,
});

function AuthPage() {
  const { user, ready, signIn } = useAuth();
  const { users } = useUsers();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (ready && user) navigate({ to: "/app/dashboard", replace: true });
  }, [ready, user, navigate]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await signIn(email, password);
      navigate({ to: "/app/dashboard", replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Sign-in failed.");
    } finally {
      setPending(false);
    }
  }

  const disabled = pending || !email.trim() || !password.trim();

  return (
    <div className="min-h-screen bg-background text-ink">
      <div className="mx-auto grid min-h-screen max-w-6xl grid-cols-1 lg:grid-cols-2">
        {/* Left: brand panel */}
        <aside className="relative hidden flex-col justify-between border-r border-hairline bg-primary p-10 text-primary-foreground lg:flex">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="grid h-9 w-9 place-items-center rounded-md bg-primary-foreground text-primary">
              <span className="font-display text-xl leading-none">N</span>
            </span>
            <div className="leading-tight">
              <div className="text-sm font-semibold">Nova Research Lab</div>
              <div className="text-[10px] uppercase tracking-[0.16em] opacity-70">
                Member workspace
              </div>
            </div>
          </Link>

          <div className="max-w-md">
            <p className="font-display text-3xl leading-tight md:text-4xl">
              "The lab isn't a room. It's a group of people willing to be honest about what they
              don't yet know."
            </p>
            <p className="mt-4 text-sm opacity-75">— Dr. Minh Tran, Principal Investigator</p>
          </div>

          <div className="text-[11px] uppercase tracking-[0.14em] opacity-60">
            Building E3 · Faculty of Information Technology
          </div>
        </aside>

        {/* Right: form */}
        <section className="flex flex-col justify-center px-6 py-16 lg:px-16">
          <div className="mx-auto w-full max-w-sm">
            <div className="mb-8">
              <div className="text-[11px] uppercase tracking-[0.18em] text-ink-soft">Sign in</div>
              <h1 className="mt-2 font-display text-3xl text-ink">Enter the workspace</h1>
              <p className="mt-2 text-sm text-ink-soft">
                Use your lab-issued credentials. Access is scoped by role.
              </p>
            </div>

            <form onSubmit={submit} className="flex flex-col gap-4">
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-medium text-ink">Email</span>
                <input
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@nova.lab"
                  className="rounded-md border border-hairline bg-surface-elev px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-ink/40"
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-medium text-ink">Password</span>
                <input
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="rounded-md border border-hairline bg-surface-elev px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-ink/40"
                />
              </label>

              {error ? (
                <div
                  role="alert"
                  className="rounded-md border border-[color:var(--destructive)]/30 bg-[color:var(--destructive)]/8 px-3 py-2 text-xs text-[color:var(--destructive)]"
                >
                  {error}
                </div>
              ) : null}

              <button
                type="submit"
                disabled={disabled}
                className={cn(
                  "mt-1 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-opacity",
                  disabled ? "opacity-50" : "hover:opacity-90",
                )}
              >
                {pending ? "Signing in…" : "Sign in"}
              </button>
            </form>

            <div className="mt-10">
              <div className="mb-3 flex items-center gap-2 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                <span className="h-px flex-1 bg-hairline" />
                Demo accounts
                <span className="h-px flex-1 bg-hairline" />
              </div>
              <div className="flex flex-col gap-2">
                {users.map((u) => (
                  <button
                    key={u.id}
                    type="button"
                    onClick={() => {
                      setEmail(u.email);
                      setPassword(DEMO_PASSWORD);
                    }}
                    className="group flex items-center justify-between gap-3 rounded-md border border-hairline bg-surface-elev px-3 py-2 text-left transition-colors hover:border-ink/20"
                  >
                    <div className="min-w-0">
                      <div className="truncate text-sm text-ink">{u.fullName}</div>
                      <div className="truncate text-[11px] text-ink-soft">{u.email}</div>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      {u.roles.map((r) => (
                        <span
                          key={r}
                          className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-ink-soft"
                        >
                          {roleLabel[r]}
                        </span>
                      ))}
                      {u.status === "locked" ? (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-ink-soft">
                          Locked
                        </span>
                      ) : null}
                    </div>
                  </button>
                ))}
              </div>
              <p className="mt-3 text-[11px] text-ink-soft">
                Frontend demo only. Password for every active demo account:{" "}
                <span className="font-mono">{DEMO_PASSWORD}</span>
              </p>
            </div>

            <div className="mt-8 text-xs text-ink-soft">
              <Link to="/" className="hover:text-ink">
                ← Back to public site
              </Link>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
