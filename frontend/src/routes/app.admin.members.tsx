import { createFileRoute } from "@tanstack/react-router";
import {
  Users2,
  Search,
  ChevronRight,
  BookOpen,
  FolderKanban,
  Star,
  X,
  Github,
  Linkedin,
  Globe,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  mockMembers,
  mockMemberDetails,
  mockMemberProjects,
  mockMemberEvaluations,
  allResearchFields,
  activityStatusLabel,
  activityStatusTone,
  roleLabel,
  roleTone,
  formatDate,
  type AdminMemberSummary,
  type AdminMemberDetail,
  type MemberActivityStatus,
} from "@/lib/members-data";
import { cn } from "@/lib/utils";
import { Drawer } from "vaul";

export const Route = createFileRoute("/app/admin/members")({
  head: () => ({
    meta: [{ title: "Members — SmartLab Admin" }, { name: "robots", content: "noindex" }],
  }),
  component: AdminMembersPage,
});

type ActivityFilter = "ALL" | MemberActivityStatus;

// ─── Skeleton ────────────────────────────────────────────────────────────────
function SkeletonRow() {
  return (
    <tr className="border-t border-hairline">
      {[1, 2, 3, 4].map((i) => (
        <td key={i} className="px-5 py-3">
          <div className="h-4 w-24 animate-pulse rounded bg-muted" />
        </td>
      ))}
    </tr>
  );
}

// ─── Mini stat ───────────────────────────────────────────────────────────────
function MiniStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "emerald" | "violet" | "amber";
}) {
  const bar: Record<string, string> = {
    emerald: "bg-[color:var(--emerald-ink)]",
    violet: "bg-[color:var(--violet-ink)]",
    amber: "bg-[color:var(--amber-ink)]",
  };
  return (
    <div className="relative overflow-hidden rounded-xl border border-hairline bg-surface-elev p-4">
      {tone && <div className={cn("absolute inset-x-0 top-0 h-0.5", bar[tone])} />}
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div className="mt-1.5 font-display text-2xl text-ink">{value}</div>
    </div>
  );
}

// ─── Profile Tab ─────────────────────────────────────────────────────────────
function ProfileTab({
  detail,
  onSave,
}: {
  detail: AdminMemberDetail;
  onSave: (updated: AdminMemberDetail) => void;
}) {
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const profile = detail.profile;
  const [bio, setBio] = useState(profile?.bio ?? "");
  const [phone, setPhone] = useState(profile?.phone ?? "");
  const [studentCode, setStudentCode] = useState(profile?.studentCode ?? "");
  const [specialization, setSpecialization] = useState(profile?.specialization ?? "");
  const [githubUrl, setGithubUrl] = useState(profile?.githubUrl ?? "");
  const [linkedinUrl, setLinkedinUrl] = useState(profile?.linkedinUrl ?? "");
  const [portfolioUrl, setPortfolioUrl] = useState(profile?.portfolioUrl ?? "");
  const [activityStatus, setActivityStatus] = useState<MemberActivityStatus | null>(
    profile?.activityStatus ?? null,
  );
  const [selectedFieldIds, setSelectedFieldIds] = useState<string[]>(
    detail.researchFields.map((f) => f.id),
  );

  const toggleField = (id: string) => {
    setSelectedFieldIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  };

  const handleSubmit = () => {
    setSaving(true);
    setError(null);
    setSuccess(null);
    // TODO: Call PUT /api/admin/members/{id}/profile and PUT /api/admin/members/{id}/research-fields
    setTimeout(() => {
      setSaving(false);
      setSuccess("Member profile updated successfully.");
      onSave({
        ...detail,
        profile: profile
          ? {
              ...profile,
              bio,
              phone,
              studentCode,
              specialization,
              githubUrl,
              linkedinUrl,
              portfolioUrl,
              activityStatus,
            }
          : null,
      });
    }, 600);
  };

  return (
    <div className="space-y-6">
      {/* Activity Status */}
      <div>
        <label className="mb-1.5 block text-xs font-medium text-ink-soft uppercase tracking-wide">
          Activity Status
        </label>
        <div className="flex gap-2 flex-wrap">
          {(["ACTIVE", "INACTIVE", "ALUMNI"] as MemberActivityStatus[]).map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setActivityStatus(s)}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-medium transition-colors border",
                activityStatus === s
                  ? "border-[color:var(--cyan)] bg-[color-mix(in_oklab,var(--cyan)_14%,transparent)] text-[color:var(--cyan)]"
                  : "border-hairline text-ink-soft hover:text-ink",
              )}
            >
              {activityStatusLabel(s)}
            </button>
          ))}
        </div>
      </div>

      {/* Basic Info */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="mb-1 block text-xs font-medium text-ink">Student Code</label>
          <input
            value={studentCode}
            onChange={(e) => setStudentCode(e.target.value)}
            className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            placeholder="e.g. SE2021001"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-ink">Phone</label>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            placeholder="e.g. 0901234567"
          />
        </div>
        <div className="sm:col-span-2">
          <label className="mb-1 block text-xs font-medium text-ink">Specialization</label>
          <input
            value={specialization}
            onChange={(e) => setSpecialization(e.target.value)}
            className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            placeholder="e.g. Robotics & Perception"
          />
        </div>
        <div className="sm:col-span-2">
          <label className="mb-1 block text-xs font-medium text-ink">Bio</label>
          <textarea
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            rows={3}
            className="w-full resize-none rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            placeholder="Short bio or research interests..."
          />
        </div>
      </div>

      {/* Links */}
      <div>
        <div className="mb-2 text-xs font-medium text-ink-soft uppercase tracking-wide">Links</div>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Github className="h-3.5 w-3.5 shrink-0 text-ink-soft" aria-hidden />
            <input
              value={githubUrl}
              onChange={(e) => setGithubUrl(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="https://github.com/username"
            />
          </div>
          <div className="flex items-center gap-2">
            <Linkedin className="h-3.5 w-3.5 shrink-0 text-ink-soft" aria-hidden />
            <input
              value={linkedinUrl}
              onChange={(e) => setLinkedinUrl(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="https://linkedin.com/in/username"
            />
          </div>
          <div className="flex items-center gap-2">
            <Globe className="h-3.5 w-3.5 shrink-0 text-ink-soft" aria-hidden />
            <input
              value={portfolioUrl}
              onChange={(e) => setPortfolioUrl(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-1.5 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="https://portfolio.example.com"
            />
          </div>
        </div>
      </div>

      {/* Research Fields */}
      <div>
        <div className="mb-2 text-xs font-medium text-ink-soft uppercase tracking-wide">
          Research Fields
        </div>
        <div className="flex flex-wrap gap-2">
          {allResearchFields
            .filter((f) => f.status === "ACTIVE")
            .map((field) => {
              const selected = selectedFieldIds.includes(field.id);
              return (
                <button
                  key={field.id}
                  type="button"
                  onClick={() => toggleField(field.id)}
                  className={cn(
                    "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                    selected
                      ? "border-[color:var(--cyan)] bg-[color-mix(in_oklab,var(--cyan)_14%,transparent)] text-[color:var(--cyan)]"
                      : "border-hairline text-ink-soft hover:text-ink",
                  )}
                >
                  {field.name}
                </button>
              );
            })}
        </div>
      </div>

      {/* Feedback */}
      {error && (
        <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      )}
      {success && (
        <div className="rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--emerald-ink)]">
          {success}
        </div>
      )}

      <button
        onClick={handleSubmit}
        disabled={saving}
        className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
      >
        {saving ? "Saving…" : "Save changes"}
      </button>
    </div>
  );
}

// ─── Projects Tab ─────────────────────────────────────────────────────────────
function ProjectsTab({ memberId }: { memberId: string }) {
  const projects = mockMemberProjects[memberId] ?? [];
  if (projects.length === 0) {
    return (
      <EmptyState title="No project memberships" hint="This member has not joined any projects." />
    );
  }
  return (
    <div className="space-y-2">
      {projects.map((p) => (
        <div
          key={p.projectId}
          className="flex items-start justify-between gap-3 rounded-lg border border-hairline bg-surface-elev p-3"
        >
          <div className="min-w-0">
            <div className="text-sm font-medium text-ink truncate">{p.projectName}</div>
            <div className="mt-0.5 text-xs text-ink-soft">
              {p.projectCode} · Joined {formatDate(p.joinedAt)}
            </div>
          </div>
          <div className="flex shrink-0 flex-col items-end gap-1">
            <StatusPill tone={p.projectRole === "PROJECT_LEADER" ? "cyan" : "neutral"}>
              {p.projectRole === "PROJECT_LEADER" ? "Leader" : "Member"}
            </StatusPill>
            <StatusPill tone={p.memberStatus === "ACTIVE" ? "emerald" : "amber"}>
              {p.memberStatus}
            </StatusPill>
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Evaluations Tab ──────────────────────────────────────────────────────────
function EvaluationsTab({ memberId }: { memberId: string }) {
  const evals = mockMemberEvaluations[memberId] ?? [];
  if (evals.length === 0) {
    return <EmptyState title="No evaluations" hint="This member has not been evaluated yet." />;
  }
  return (
    <div className="space-y-2">
      {evals.map((e) => (
        <div key={e.id} className="rounded-lg border border-hairline bg-surface-elev p-3">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="text-sm font-medium text-ink">
                {e.projectName} · {e.evaluationPeriod}
              </div>
              <div className="mt-0.5 text-xs text-ink-soft">{formatDate(e.evaluatedAt)}</div>
            </div>
            {e.overallScore !== null ? (
              <div className="flex shrink-0 items-center gap-1 rounded-full border border-hairline bg-surface-elev px-2 py-0.5">
                <Star className="h-3 w-3 text-[color:var(--amber-ink)]" aria-hidden />
                <span className="text-xs font-semibold text-ink">{e.overallScore.toFixed(1)}</span>
              </div>
            ) : (
              <span className="text-xs text-ink-soft">—</span>
            )}
          </div>
          {e.comment && <p className="mt-2 text-xs text-ink-soft">{e.comment}</p>}
        </div>
      ))}
    </div>
  );
}

// ─── Member Detail Drawer ─────────────────────────────────────────────────────
function MemberDetailDrawer({
  member,
  open,
  onClose,
}: {
  member: AdminMemberSummary | null;
  open: boolean;
  onClose: () => void;
}) {
  const [activeTab, setActiveTab] = useState<"profile" | "projects" | "evaluations">("profile");
  const detail = member ? mockMemberDetails[member.id] : null;

  const initials = member
    ? member.fullName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .slice(0, 2)
        .toUpperCase()
    : "";

  return (
    <Drawer.Root open={open} onOpenChange={(v) => !v && onClose()} direction="right">
      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm" />
        <Drawer.Content
          aria-label="Member detail"
          className="fixed inset-y-0 right-0 z-50 flex w-full max-w-xl flex-col bg-background shadow-2xl outline-none"
        >
          {/* Header */}
          <div className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
            <div className="flex items-center gap-3 min-w-0">
              <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
                {initials}
              </div>
              <div className="min-w-0">
                <div className="font-semibold text-ink truncate">{member?.fullName}</div>
                <div className="text-xs text-ink-soft truncate">{member?.email}</div>
                <div className="mt-1 flex flex-wrap gap-1">
                  {member?.roles.map((r) => (
                    <StatusPill key={r} tone={roleTone(r)}>
                      {roleLabel(r)}
                    </StatusPill>
                  ))}
                  {member?.activityStatus && (
                    <StatusPill tone={activityStatusTone(member.activityStatus)}>
                      {activityStatusLabel(member.activityStatus)}
                    </StatusPill>
                  )}
                </div>
              </div>
            </div>
            <button
              onClick={onClose}
              aria-label="Close member detail"
              className="shrink-0 rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Stats bar */}
          {detail && (
            <div className="flex divide-x divide-hairline border-b border-hairline">
              <div className="flex-1 px-4 py-3 text-center">
                <div className="text-[11px] uppercase tracking-wide text-ink-soft">Projects</div>
                <div className="mt-0.5 font-display text-xl text-ink">{detail.projectCount}</div>
              </div>
              <div className="flex-1 px-4 py-3 text-center">
                <div className="text-[11px] uppercase tracking-wide text-ink-soft">Evaluations</div>
                <div className="mt-0.5 font-display text-xl text-ink">{detail.evaluationCount}</div>
              </div>
              <div className="flex-1 px-4 py-3 text-center">
                <div className="text-[11px] uppercase tracking-wide text-ink-soft">Fields</div>
                <div className="mt-0.5 font-display text-xl text-ink">
                  {detail.researchFields.length}
                </div>
              </div>
            </div>
          )}

          {/* Tabs */}
          <div className="flex border-b border-hairline">
            {(["profile", "projects", "evaluations"] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={cn(
                  "flex items-center gap-1.5 px-4 py-2.5 text-xs font-medium capitalize transition-colors",
                  activeTab === tab
                    ? "border-b-2 border-primary text-ink"
                    : "text-ink-soft hover:text-ink",
                )}
              >
                {tab === "profile" && <Users2 className="h-3.5 w-3.5" aria-hidden />}
                {tab === "projects" && <FolderKanban className="h-3.5 w-3.5" aria-hidden />}
                {tab === "evaluations" && <Star className="h-3.5 w-3.5" aria-hidden />}
                {tab}
              </button>
            ))}
          </div>

          {/* Tab content */}
          <div className="flex-1 overflow-y-auto px-5 py-5">
            {!detail ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-10 animate-pulse rounded-md bg-muted" />
                ))}
              </div>
            ) : activeTab === "profile" ? (
              <ProfileTab detail={detail} onSave={() => {}} />
            ) : activeTab === "projects" ? (
              <ProjectsTab memberId={detail.id} />
            ) : (
              <EvaluationsTab memberId={detail.id} />
            )}
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
function AdminMembersPage() {
  const { user, activeRole } = useAuth();
  const [query, setQuery] = useState("");
  const [activityFilter, setActivityFilter] = useState<ActivityFilter>("ALL");
  const [selectedMember, setSelectedMember] = useState<AdminMemberSummary | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const isAdminView = activeRole === "admin" && !!user?.roles.includes("admin");

  const filtered = useMemo(() => {
    return mockMembers.filter((m) => {
      const text = query.trim().toLowerCase();
      if (
        text &&
        !m.fullName.toLowerCase().includes(text) &&
        !m.email.toLowerCase().includes(text)
      ) {
        return false;
      }
      if (activityFilter !== "ALL" && m.activityStatus !== activityFilter) return false;
      return true;
    });
  }, [query, activityFilter]);

  const totalActive = mockMembers.filter((m) => m.activityStatus === "ACTIVE").length;
  const totalAlumni = mockMembers.filter((m) => m.activityStatus === "ALUMNI").length;

  const openDrawer = (member: AdminMemberSummary) => {
    setSelectedMember(member);
    setDrawerOpen(true);
  };

  if (!isAdminView) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <Users2 className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Admin workspace required</h1>
        <p className="mt-1 text-xs text-ink-soft">
          Member management is available only when viewing as Admin.
        </p>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow="Admin"
        title="Members"
        description="View and manage lab member profiles, research fields, and activity status."
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-3">
        <MiniStat label="Total members" value={mockMembers.length} />
        <MiniStat label="Active" value={totalActive} tone="emerald" />
        <MiniStat label="Alumni" value={totalAlumni} tone="violet" />
      </div>

      <Panel
        title="Member directory"
        description={`${filtered.length} of ${mockMembers.length} members`}
        className="overflow-hidden"
        action={
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search
                className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-soft"
                aria-hidden
              />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search members…"
                aria-label="Search members"
                className="w-full rounded-md border border-hairline bg-background py-1.5 pl-7 pr-2 text-xs text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40 sm:w-52"
              />
            </div>
            <select
              value={activityFilter}
              onChange={(e) => setActivityFilter(e.target.value as ActivityFilter)}
              aria-label="Filter by activity status"
              className="rounded-md border border-hairline bg-background px-2 py-1.5 text-xs text-ink"
            >
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="ALUMNI">Alumni</option>
            </select>
          </div>
        }
      >
        {filtered.length === 0 ? (
          <EmptyState
            title={mockMembers.length === 0 ? "No members yet" : "No members match these filters"}
            hint="Try a different search or activity status filter."
          />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[700px] text-sm" aria-label="Member directory">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Member</th>
                  <th className="px-3 py-3 font-medium">Roles</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Joined</th>
                  <th className="px-5 py-3 font-medium" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((member) => {
                  const initials = member.fullName
                    .split(" ")
                    .map((n) => n[0])
                    .join("")
                    .slice(0, 2)
                    .toUpperCase();
                  return (
                    <tr
                      key={member.id}
                      className="cursor-pointer border-t border-hairline align-middle hover:bg-muted/30 transition-colors"
                      onClick={() => openDrawer(member)}
                    >
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <div className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                            {initials}
                          </div>
                          <div className="min-w-0">
                            <div className="font-medium text-ink truncate">{member.fullName}</div>
                            <div className="text-xs text-ink-soft truncate">{member.email}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <div className="flex flex-wrap gap-1">
                          {member.roles.map((r) => (
                            <StatusPill key={r} tone={roleTone(r)}>
                              {roleLabel(r)}
                            </StatusPill>
                          ))}
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <StatusPill tone={activityStatusTone(member.activityStatus)}>
                          {activityStatusLabel(member.activityStatus)}
                        </StatusPill>
                      </td>
                      <td className="px-3 py-3 text-xs text-ink-soft">
                        {formatDate(member.joinedAt)}
                      </td>
                      <td className="px-5 py-3">
                        <ChevronRight className="ml-auto h-4 w-4 text-ink-soft" aria-hidden />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <MemberDetailDrawer
        member={selectedMember}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setTimeout(() => setSelectedMember(null), 300);
        }}
      />
    </>
  );
}
