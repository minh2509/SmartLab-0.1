import { Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useAuth, roleLabel, type Role } from "@/lib/auth";
import { cn } from "@/lib/utils";
import { NotificationButton } from "./NotificationButton";
import {
  LayoutDashboard,
  FolderKanban,
  FileText,
  Bell,
  LogOut,
  ChevronDown,
  ShieldCheck,
  ClipboardList,
  ListChecks,
  ClipboardCheck,
  CalendarDays,
  FileClock,
  History,
} from "lucide-react";

type AppNavPath =
  | "/app/dashboard"
  | "/app/projects"
  | "/app/join-requests"
  | "/app/tasks"
  | "/app/evaluations"
  | "/app/calendar"
  | "/app/posts"
  | "/app/moderation/posts"
  | "/app/admin/users"
  | "/app/admin/audit-logs"
  | "/app/admin/login-histories"
  | "/app/notifications";

type NavItem = {
  label: string;
  to: AppNavPath;
  icon: React.ComponentType<{ className?: string }>;
  roles: Role[];
};

const NAV: NavItem[] = [
  {
    label: "Dashboard",
    to: "/app/dashboard",
    icon: LayoutDashboard,
    roles: ["admin", "leader", "member"],
  },
  {
    label: "Projects",
    to: "/app/projects",
    icon: FolderKanban,
    roles: ["admin", "leader", "member"],
  },
  {
    label: "Join Requests",
    to: "/app/join-requests",
    icon: ClipboardList,
    roles: ["admin", "member"],
  },
  {
    label: "Tasks",
    to: "/app/tasks",
    icon: ListChecks,
    roles: ["admin", "member"],
  },
  {
    label: "Evaluations",
    to: "/app/evaluations",
    icon: ClipboardCheck,
    roles: ["admin", "member"],
  },
  {
    label: "Calendar",
    to: "/app/calendar",
    icon: CalendarDays,
    roles: ["admin", "leader", "member"],
  },
  {
    label: "My Posts",
    to: "/app/posts",
    icon: FileText,
    roles: ["member"],
  },
  {
    label: "Moderation",
    to: "/app/moderation/posts",
    icon: ShieldCheck,
    roles: ["admin"],
  },
  {
    label: "Notifications",
    to: "/app/notifications",
    icon: Bell,
    roles: ["admin", "leader", "member"],
  },
  {
    label: "Administration",
    to: "/app/admin/users",
    icon: ShieldCheck,
    roles: ["admin"],
  },
  {
    label: "Audit Logs",
    to: "/app/admin/audit-logs",
    icon: FileClock,
    roles: ["admin"],
  },
  {
    label: "Login History",
    to: "/app/admin/login-histories",
    icon: History,
    roles: ["admin"],
  },
];

function isNavItemActive(pathname: string, item: NavItem): boolean {
  return pathname === item.to || pathname.startsWith(`${item.to}/`);
}

export function AppShell() {
  const { user, activeRole, ready, signOut, setActiveRole } = useAuth();

  const navigate = useNavigate();

  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  });

  useEffect(() => {
    if (ready && !user) {
      navigate({
        to: "/auth",
        replace: true,
      });
    }
  }, [ready, user, navigate]);

  if (!ready || !user || !activeRole) {
    return (
      <div className="grid min-h-screen place-items-center bg-background">
        <div className="text-sm text-ink-soft">Loading workspace…</div>
      </div>
    );
  }

  const items = NAV.filter((item) => item.roles.includes(activeRole));

  const handleSignOut = () => {
    signOut();

    navigate({
      to: "/auth",
      replace: true,
    });
  };

  return (
    <div className="min-h-screen bg-background text-ink">
      <div className="mx-auto flex min-h-screen max-w-[1400px] md:pl-64">
        {/* Desktop sidebar */}
        <aside className="fixed left-[max(0px,calc((100vw-1400px)/2))] top-0 z-40 hidden h-dvh w-64 shrink-0 overflow-y-auto border-r border-hairline bg-background px-4 py-6 md:flex md:flex-col">
          <Link to="/" className="mb-8 flex items-center gap-2.5 px-2">
            <span className="grid h-8 w-8 place-items-center rounded-md bg-primary text-primary-foreground">
              <span className="font-display text-lg leading-none">N</span>
            </span>

            <div className="leading-tight">
              <div className="text-sm font-semibold tracking-tight">SmartLab</div>

              <div className="text-[10px] uppercase tracking-[0.14em] text-ink-soft">Workspace</div>
            </div>
          </Link>

          <nav aria-label="Workspace navigation" className="flex flex-1 flex-col gap-0.5">
            {items.map((item) => {
              const Icon = item.icon;
              const active = isNavItemActive(pathname, item);

              return (
                <Link
                  key={item.to}
                  to={item.to}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm transition-colors",
                    active
                      ? "bg-muted font-medium text-ink"
                      : "text-ink-soft hover:bg-muted/60 hover:text-ink",
                  )}
                >
                  <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />

                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </aside>

        {/* Main content area */}
        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-3 border-b border-hairline bg-background/85 px-4 backdrop-blur-md md:px-6">
            <div className="min-w-0">
              <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                {roleLabel[activeRole]} workspace
              </div>

              <div className="truncate text-sm font-medium text-ink">
                Welcome back, {user.fullName.split(" ")[0]}
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
              <NotificationButton userId={user.id} />

              <RoleSwitcher roles={user.roles} active={activeRole} onChange={setActiveRole} />

              <button
                type="button"
                onClick={handleSignOut}
                className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-2.5 py-1.5 text-xs text-ink-soft transition-colors hover:text-ink"
                aria-label="Sign out"
              >
                <LogOut className="h-3.5 w-3.5" aria-hidden="true" />

                <span className="hidden sm:inline">Sign out</span>
              </button>

              <div
                className="ml-0.5 grid h-8 w-8 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground sm:ml-1 sm:h-9 sm:w-9"
                aria-label={`${user.fullName} avatar`}
              >
                {user.initials}
              </div>
            </div>
          </header>

          {/* Mobile navigation */}
          <nav
            aria-label="Mobile workspace navigation"
            className="sticky top-16 z-20 flex gap-2 overflow-x-auto border-b border-hairline bg-background/95 px-4 py-2 backdrop-blur-md md:hidden"
          >
            {items.map((item) => {
              const Icon = item.icon;
              const active = isNavItemActive(pathname, item);

              return (
                <Link
                  key={item.to}
                  to={item.to}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "inline-flex shrink-0 items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-xs transition-colors",
                    active
                      ? "border-ink bg-ink text-background"
                      : "border-hairline bg-surface-elev text-ink-soft hover:text-ink",
                  )}
                >
                  <Icon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />

                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          <main className="flex-1 px-4 py-6 md:px-6 md:py-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}

function RoleSwitcher({
  roles,
  active,
  onChange,
}: {
  roles: Role[];
  active: Role;
  onChange: (role: Role) => void;
}) {
  const [open, setOpen] = useState(false);

  if (roles.length === 1) {
    return (
      <span className="inline-flex items-center rounded-full border border-hairline bg-surface-elev px-2.5 py-1 text-[11px] font-medium text-ink-soft">
        {roleLabel[active]}
      </span>
    );
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        onBlur={() => {
          window.setTimeout(() => setOpen(false), 120);
        }}
        className="inline-flex items-center gap-1.5 rounded-md border border-hairline bg-surface-elev px-2.5 py-1.5 text-xs font-medium text-ink"
        aria-label={`Viewing as ${roleLabel[active]}`}
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <span className="hidden sm:inline">Viewing as</span>

        <span>{roleLabel[active]}</span>

        <ChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
      </button>

      {open ? (
        <div
          role="menu"
          className="absolute right-0 top-full z-40 mt-1 w-52 overflow-hidden rounded-md border border-hairline bg-surface-elev shadow-lg"
        >
          {roles.map((role) => {
            const isActiveRole = role === active;

            return (
              <button
                key={role}
                type="button"
                role="menuitem"
                onMouseDown={(event) => {
                  event.preventDefault();
                  onChange(role);
                  setOpen(false);
                }}
                className={cn(
                  "flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-muted",
                  isActiveRole ? "text-ink" : "text-ink-soft",
                )}
              >
                <span>{roleLabel[role]}</span>

                {isActiveRole ? (
                  <span className="text-[10px] uppercase tracking-wider text-ink-soft">Active</span>
                ) : null}
              </button>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
