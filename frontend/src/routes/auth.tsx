import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/auth")({
  head: () => ({
    meta: [
      { title: "Sign in — SmartResearch Lab" },
      {
        name: "description",
        content: "Sign in to the SmartResearch Lab workspace.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AuthPage,
});

function AuthPage() {
  const { user, ready, signIn } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (ready && user) {
      navigate({
        to: "/app/dashboard",
        replace: true,
      });
    }
  }, [ready, user, navigate]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setPending(true);

    try {
      await signIn(email.trim(), password);

      navigate({
        to: "/app/dashboard",
        replace: true,
      });
    } catch (err) {
      toast.error(signInErrorTitle(err), {
        id: "sign-in-error",
        description: signInErrorDescription(err),
      });
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
              <div className="text-sm font-semibold">SmartResearch Lab</div>

              <div className="text-[10px] uppercase tracking-[0.16em] opacity-70">
                Member workspace
              </div>
            </div>
          </Link>

          <div className="max-w-md">
            <p className="font-display text-3xl leading-tight md:text-4xl">
              “The lab is not only a place. It is a community willing to be honest about what it
              does not yet know.”
            </p>

            <p className="mt-4 text-sm opacity-75">— SmartLab research culture</p>
          </div>

          <div className="text-[11px] uppercase tracking-[0.14em] opacity-60">
            Building E3 · Faculty of Information Technology
          </div>
        </aside>

        {/* Right: sign-in form */}
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
                  onChange={(event) => {
                    setEmail(event.target.value);
                  }}
                  placeholder="you@smart.lab"
                  className="rounded-md border border-hairline bg-surface-elev px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-ink/40"
                />
              </label>

              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-medium text-ink">Password</span>

                <input
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => {
                    setPassword(event.target.value);
                  }}
                  placeholder="••••••••"
                  className="rounded-md border border-hairline bg-surface-elev px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-ink/40"
                />
              </label>

              <button
                type="submit"
                disabled={disabled}
                className={cn(
                  "mt-1 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-opacity",
                  disabled ? "cursor-not-allowed opacity-50" : "hover:opacity-90",
                )}
              >
                {pending ? "Signing in…" : "Sign in"}
              </button>
            </form>

            <div className="mt-10 rounded-md border border-hairline bg-surface-elev px-3 py-3 text-xs leading-relaxed text-ink-soft">
              Authentication now uses the SmartLab backend. Use an account created in the local
              database by a lab administrator.
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

function signInErrorTitle(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    return "Invalid email or password.";
  }
  if (error instanceof ApiError && error.status === 0) {
    return "Unable to connect to the backend.";
  }
  return "Unable to sign in.";
}

function signInErrorDescription(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    return "Check your email and password, then try again.";
  }
  if (error instanceof ApiError && error.status === 0) {
    return "Make sure the SmartLab backend is running on port 8080.";
  }
  return error instanceof Error ? error.message : "Please try again later.";
}
