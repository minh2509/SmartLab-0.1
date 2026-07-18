import { createFileRoute } from "@tanstack/react-router";
import { CalendarDays, MapPin, Pencil, Plus, Trash2, X } from "lucide-react";
import { useMemo, useState } from "react";
import { CalendarEventDialog } from "@/components/app/calendar/CalendarEventDialog";
import { DeleteCalendarEventDialog } from "@/components/app/calendar/DeleteCalendarEventDialog";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  calendarScopeTone,
  formatCalendarDateGroup,
  formatCalendarDateTime,
  isPastEvent,
  useCalendarEvents,
  type CalendarEventDraft,
  type LabCalendarEvent,
} from "@/lib/calendar-data";
import { notifyManyOnce } from "@/lib/notifications-data";
import { getUserName, useProjects, type Project } from "@/lib/projects-data";
import { getUserById, getUsers } from "@/lib/users-data";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/app/calendar")({
  head: () => ({
    meta: [{ title: "Calendar — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: CalendarPage,
});

type ScopeFilter = "all" | "lab" | "projects";

function CalendarPage() {
  const { user, activeRole } = useAuth();
  const { projects } = useProjects();
  const { events, createEvent, updateEvent, removeEvent } = useCalendarEvents();
  const [scopeFilter, setScopeFilter] = useState<ScopeFilter>("all");
  const [projectFilter, setProjectFilter] = useState("all");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<LabCalendarEvent | null>(null);
  const [viewing, setViewing] = useState<LabCalendarEvent | null>(null);
  const [deleting, setDeleting] = useState<LabCalendarEvent | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletePending, setDeletePending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const userId = user?.id ?? "";
  const isAdminMode = activeRole === "admin" && !!user?.roles.includes("admin");
  const isLeaderMode = activeRole === "leader" && !!user?.roles.includes("leader");

  const assignedLeaderProjects = useMemo(
    () => projects.filter((project) => project.leaderIds.includes(userId)),
    [projects, userId],
  );
  const memberProjects = useMemo(
    () =>
      projects.filter(
        (project) => project.memberIds.includes(userId) || project.leaderIds.includes(userId),
      ),
    [projects, userId],
  );
  const relevantProjects = isAdminMode
    ? projects
    : isLeaderMode
      ? assignedLeaderProjects
      : memberProjects;
  const projectFilterOptions = relevantProjects;
  const manageableProjects = isLeaderMode ? assignedLeaderProjects : [];
  const canCreateLab = isAdminMode;
  const canCreateProject = isLeaderMode && manageableProjects.length > 0;

  const visibleEvents = useMemo(
    () =>
      events.filter((event) => {
        if (event.scope === "lab") {
          return scopeFilter === "all" || scopeFilter === "lab";
        }
        const project = projects.find((item) => item.id === event.projectId);
        if (!project) return false;
        const canViewProjectEvent =
          isAdminMode || project.leaderIds.includes(userId) || project.memberIds.includes(userId);
        if (!canViewProjectEvent) return false;
        if (scopeFilter === "lab") return false;
        if (projectFilter !== "all" && event.projectId !== projectFilter) return false;
        return true;
      }),
    [events, isAdminMode, projectFilter, projects, scopeFilter, userId],
  );

  if (!user || !activeRole) return null;

  const upcomingEvents = visibleEvents.filter((event) => !isPastEvent(event));
  const pastEvents = visibleEvents.filter((event) => isPastEvent(event)).reverse();
  const labOnlyCount = visibleEvents.filter((event) => event.scope === "lab").length;
  const projectOnlyCount = visibleEvents.filter((event) => event.scope === "project").length;

  const canManageEvent = (event: LabCalendarEvent) => {
    if (event.scope === "lab") return isAdminMode;
    const project = projects.find((item) => item.id === event.projectId);
    return !!project && isLeaderMode && project.leaderIds.includes(user.id);
  };

  const saveEvent = (draft: CalendarEventDraft) => {
    if (saving) return;
    const permissionError = validatePermission(draft, projects, user.id, isAdminMode, isLeaderMode);
    if (permissionError) {
      setError(permissionError);
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    const result = editing ? updateEvent(editing.id, draft) : createEvent(user.id, draft);
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    if (!editing) notifyCalendarRecipients(result.value, projects, user.id);
    setCreating(false);
    setEditing(null);
    setSuccess(editing ? "Calendar event updated." : "Calendar event created.");
  };

  const deleteEvent = () => {
    if (!deleting || deletePending) return;
    if (!canManageEvent(deleting)) {
      setError("You are not authorized to delete this calendar event.");
      return;
    }
    setDeletePending(true);
    setError(null);
    const result = removeEvent(deleting.id);
    setDeletePending(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setDeleting(null);
    setSuccess("Calendar event deleted.");
  };

  return (
    <>
      <PageHeader
        eyebrow="Lab calendar"
        title="Calendar"
        description="A shared agenda for lab-wide events and project-specific work sessions relevant to your workspace."
        action={
          canCreateLab || canCreateProject ? (
            <button
              onClick={() => {
                setCreating(true);
                setEditing(null);
                setError(null);
                setSuccess(null);
              }}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              <Plus className="h-3.5 w-3.5" /> New event
            </button>
          ) : undefined
        }
      />

      {error ? (
        <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}
      {success ? (
        <div className="mb-4 rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--emerald-ink)]">
          {success}
        </div>
      ) : null}

      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <MiniStat label="Relevant events" value={visibleEvents.length} />
        <MiniStat label="Lab-wide" value={labOnlyCount} tone="violet" />
        <MiniStat label="Project-specific" value={projectOnlyCount} tone="cyan" />
      </div>

      <Panel
        title="Agenda filters"
        description={
          isAdminMode
            ? "Admin view includes lab events and project-event oversight."
            : isLeaderMode
              ? "Leader view includes lab events and projects you lead."
              : "Member view includes lab events and projects you belong to."
        }
      >
        <div className="grid gap-3 md:grid-cols-[1fr_220px]">
          <div className="flex flex-wrap gap-1.5">
            {[
              { key: "all", label: "All relevant" },
              { key: "lab", label: "Lab" },
              { key: "projects", label: "My projects" },
            ].map((item) => (
              <button
                key={item.key}
                onClick={() => setScopeFilter(item.key as ScopeFilter)}
                className={cn(
                  "rounded-full border px-3 py-1.5 text-xs transition-colors",
                  scopeFilter === item.key
                    ? "border-ink bg-ink text-background"
                    : "border-hairline text-ink-soft hover:text-ink",
                )}
              >
                {item.label}
              </button>
            ))}
          </div>
          <select
            className="rounded-md border border-hairline bg-background px-2.5 py-1.5 text-xs text-ink focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
            value={projectFilter}
            disabled={scopeFilter === "lab" || projectFilterOptions.length === 0}
            onChange={(event) => setProjectFilter(event.target.value)}
          >
            <option value="all">All projects</option>
            {projectFilterOptions.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
        </div>
      </Panel>

      <div className="mt-4 grid gap-4 xl:grid-cols-[1fr_340px]">
        <Panel
          title="Upcoming agenda"
          description={`${upcomingEvents.length} upcoming event${upcomingEvents.length === 1 ? "" : "s"}`}
        >
          {upcomingEvents.length === 0 ? (
            <EmptyState
              title="No upcoming events"
              hint={
                isLeaderMode && manageableProjects.length === 0
                  ? "You are not assigned to any project where you can create project events."
                  : "Past events remain available below; new relevant events will appear here."
              }
            />
          ) : (
            <AgendaList
              events={upcomingEvents}
              projects={projects}
              canManageEvent={canManageEvent}
              onView={setViewing}
              onEdit={(event) => {
                setEditing(event);
                setCreating(false);
                setError(null);
              }}
              onDelete={(event) => {
                setDeleting(event);
                setError(null);
              }}
            />
          )}
        </Panel>

        <aside className="space-y-4">
          <Panel title="Create permissions">
            <div className="space-y-3 text-sm">
              <PermissionLine
                label="Lab-wide events"
                value={canCreateLab ? "Allowed as Admin" : "Admin only"}
              />
              <PermissionLine
                label="Project events"
                value={
                  canCreateProject
                    ? `${manageableProjects.length} managed project${manageableProjects.length === 1 ? "" : "s"}`
                    : "Assigned Leader only"
                }
              />
            </div>
          </Panel>

          <Panel title="Timezone">
            <p className="text-sm leading-relaxed text-ink-soft">
              Date-time inputs use the browser's local timezone and are stored as ISO date-time
              values in localStorage.
            </p>
          </Panel>
        </aside>
      </div>

      <div className="mt-4">
        <Panel title="Past events" description="Past events are retained until manually deleted.">
          {pastEvents.length === 0 ? (
            <EmptyState
              title="No past events"
              hint="Completed agenda items will remain visible here."
            />
          ) : (
            <AgendaList
              events={pastEvents}
              projects={projects}
              canManageEvent={canManageEvent}
              onView={setViewing}
              onEdit={(event) => {
                setEditing(event);
                setCreating(false);
                setError(null);
              }}
              onDelete={(event) => {
                setDeleting(event);
                setError(null);
              }}
            />
          )}
        </Panel>
      </div>

      <CalendarEventDialog
        event={editing}
        open={creating || !!editing}
        saving={saving}
        error={error}
        mode={isAdminMode ? "admin" : "leader"}
        manageableProjects={isAdminMode ? [] : manageableProjects}
        canCreateLab={canCreateLab}
        onClose={() => {
          if (!saving) {
            setCreating(false);
            setEditing(null);
            setError(null);
          }
        }}
        onSave={saveEvent}
      />

      <DeleteCalendarEventDialog
        event={deleting}
        open={!!deleting}
        deleting={deletePending}
        error={error}
        onClose={() => {
          if (!deletePending) {
            setDeleting(null);
            setError(null);
          }
        }}
        onConfirm={deleteEvent}
      />

      <EventDetailDialog
        event={viewing}
        project={
          viewing?.projectId ? projects.find((project) => project.id === viewing.projectId) : null
        }
        canManage={viewing ? canManageEvent(viewing) : false}
        onClose={() => setViewing(null)}
        onEdit={(event) => {
          setViewing(null);
          setEditing(event);
        }}
      />
    </>
  );
}

function validatePermission(
  draft: CalendarEventDraft,
  projects: Project[],
  userId: string,
  isAdminMode: boolean,
  isLeaderMode: boolean,
) {
  if (draft.scope === "lab") {
    return isAdminMode ? null : "Only Admin users can save lab-wide events.";
  }
  const project = projects.find((item) => item.id === draft.projectId);
  if (!project) return "Project not found.";
  if (!isLeaderMode || !project.leaderIds.includes(userId)) {
    return "Only assigned project leaders can save project events.";
  }
  return null;
}

function notifyCalendarRecipients(event: LabCalendarEvent, projects: Project[], creatorId: string) {
  const recipientIds =
    event.scope === "lab"
      ? getUsers()
          .filter((user) => user.status === "active")
          .map((user) => user.id)
      : Array.from(
          new Set(
            projects.find((project) => project.id === event.projectId)
              ? [
                  ...(projects.find((project) => project.id === event.projectId)?.leaderIds ?? []),
                  ...(projects.find((project) => project.id === event.projectId)?.memberIds ?? []),
                ]
              : [],
          ),
        );

  notifyManyOnce(
    recipientIds
      .filter(
        (recipientId) => recipientId !== creatorId && getUserById(recipientId)?.status === "active",
      )
      .map((recipientId) => ({
        userId: recipientId,
        type: "calendar_event_created",
        title: event.scope === "lab" ? "New lab calendar event" : "New project calendar event",
        message:
          event.scope === "lab"
            ? `${event.title} was added to the lab calendar.`
            : `${event.title} was added to a project calendar.`,
        link: "/app/calendar",
        eventKey: `calendar:${event.id}:created:${recipientId}`,
      })),
  );
}

function AgendaList({
  events,
  projects,
  canManageEvent,
  onView,
  onEdit,
  onDelete,
}: {
  events: LabCalendarEvent[];
  projects: Project[];
  canManageEvent: (event: LabCalendarEvent) => boolean;
  onView: (event: LabCalendarEvent) => void;
  onEdit: (event: LabCalendarEvent) => void;
  onDelete: (event: LabCalendarEvent) => void;
}) {
  const grouped = events.reduce<Record<string, LabCalendarEvent[]>>((acc, event) => {
    const key = formatCalendarDateGroup(event.startAt);
    acc[key] = [...(acc[key] ?? []), event];
    return acc;
  }, {});

  return (
    <div className="space-y-5">
      {Object.entries(grouped).map(([date, dateEvents]) => (
        <section key={date}>
          <h3 className="mb-2 text-[11px] uppercase tracking-[0.16em] text-ink-soft">{date}</h3>
          <div className="space-y-3">
            {dateEvents.map((event) => {
              const project = event.projectId
                ? projects.find((item) => item.id === event.projectId)
                : null;
              const manageable = canManageEvent(event);
              return (
                <article key={event.id} className="rounded-lg border border-hairline p-4">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <button onClick={() => onView(event)} className="min-w-0 flex-1 text-left">
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusPill tone={calendarScopeTone(event.scope)}>
                          {event.scope === "lab" ? "Lab-wide" : "Project"}
                        </StatusPill>
                        <StatusPill tone={isPastEvent(event) ? "neutral" : "emerald"}>
                          {isPastEvent(event) ? "Past" : "Upcoming"}
                        </StatusPill>
                        {project ? (
                          <span className="text-xs text-ink-soft">{project.code}</span>
                        ) : null}
                      </div>
                      <h4 className="mt-2 text-sm font-semibold text-ink">{event.title}</h4>
                      <p className="mt-1 line-clamp-2 text-sm leading-relaxed text-ink-soft">
                        {event.description}
                      </p>
                      <div className="mt-3 flex flex-wrap gap-3 text-xs text-ink-soft">
                        <span>
                          {formatCalendarDateTime(event.startAt)} →{" "}
                          {formatCalendarDateTime(event.endAt)}
                        </span>
                        {event.location ? (
                          <span className="inline-flex items-center gap-1">
                            <MapPin className="h-3.5 w-3.5" /> {event.location}
                          </span>
                        ) : null}
                      </div>
                    </button>
                    {manageable ? (
                      <div className="flex shrink-0 gap-1.5">
                        <button
                          onClick={() => onEdit(event)}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted"
                        >
                          <Pencil className="h-3.5 w-3.5" /> Edit
                        </button>
                        <button
                          onClick={() => onDelete(event)}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink-soft hover:bg-muted hover:text-[color:var(--destructive)]"
                        >
                          <Trash2 className="h-3.5 w-3.5" /> Delete
                        </button>
                      </div>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}

function EventDetailDialog({
  event,
  project,
  canManage,
  onClose,
  onEdit,
}: {
  event: LabCalendarEvent | null;
  project: Project | null | undefined;
  canManage: boolean;
  onClose: () => void;
  onEdit: (event: LabCalendarEvent) => void;
}) {
  if (!event) return null;
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        onMouseDown={(mouseEvent) => mouseEvent.stopPropagation()}
        className="w-full max-w-xl rounded-xl border border-hairline bg-surface-elev shadow-xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-hairline px-5 py-4">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <StatusPill tone={calendarScopeTone(event.scope)}>
                {event.scope === "lab" ? "Lab-wide" : "Project"}
              </StatusPill>
              <StatusPill tone={isPastEvent(event) ? "neutral" : "emerald"}>
                {isPastEvent(event) ? "Past" : "Upcoming"}
              </StatusPill>
            </div>
            <h2 className="mt-3 text-base font-semibold text-ink">{event.title}</h2>
            <p className="mt-1 text-xs text-ink-soft">
              {project ? project.name : event.scope === "lab" ? "Nova Lab" : "Project not found"}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="space-y-4 p-5">
          <div className="rounded-lg border border-hairline bg-muted/30 p-3 text-xs">
            <div className="font-medium text-ink">
              {formatCalendarDateTime(event.startAt)} → {formatCalendarDateTime(event.endAt)}
            </div>
            {event.location ? (
              <div className="mt-1 text-ink-soft">Location · {event.location}</div>
            ) : null}
          </div>
          <p className="text-sm leading-relaxed text-ink">{event.description}</p>
          <div className="grid gap-2 text-xs text-ink-soft sm:grid-cols-2">
            <div>Created by {getUserName(event.createdBy)}</div>
            <div>Updated {formatCalendarDateTime(event.updatedAt)}</div>
          </div>
        </div>
        <footer className="flex items-center justify-end gap-2 border-t border-hairline px-5 py-4">
          {canManage ? (
            <button
              onClick={() => onEdit(event)}
              className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90"
            >
              Edit event
            </button>
          ) : null}
          <button
            onClick={onClose}
            className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
          >
            Close
          </button>
        </footer>
      </div>
    </div>
  );
}

function MiniStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "cyan" | "violet";
}) {
  return (
    <div className="rounded-xl border border-hairline bg-surface-elev p-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      <div
        className={cn(
          "mt-2 font-display text-3xl text-ink",
          tone === "cyan" && "text-[color:var(--cyan)]",
          tone === "violet" && "text-[color:var(--violet-ink)]",
        )}
      >
        {value}
      </div>
    </div>
  );
}

function PermissionLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-ink-soft">{label}</span>
      <span className="text-right font-medium text-ink">{value}</span>
    </div>
  );
}
