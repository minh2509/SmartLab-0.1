import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatDate, useProjects } from "@/lib/projects-data";
import {
  assignmentStatusTone,
  getEffectiveTaskStatus,
  latestSubmission,
  priorityTone,
  taskStatusTone,
  useProjectTasks,
  type ProjectTask,
  type TaskAssignment,
} from "@/lib/tasks-data";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { TaskSubmissionDialog } from "@/components/app/projects/TaskSubmissionDialog";
import { ExternalLink, Play, Send } from "lucide-react";

export const Route = createFileRoute("/app/tasks")({
  head: () => ({
    meta: [{ title: "Tasks — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: TasksPage,
});

function TasksPage() {
  const { user, activeRole } = useAuth();
  const { projects } = useProjects();
  const taskStore = useProjectTasks();
  const { tasks, assignments, submissions, startWork, submitOutput } = taskStore;
  const [submittingAssignment, setSubmittingAssignment] = useState<TaskAssignment | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user || !activeRole) return null;

  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const visiblePairs = isAdmin
    ? tasks.flatMap((task) =>
        assignments
          .filter((assignment) => assignment.taskId === task.id)
          .map((assignment) => ({ task, assignment })),
      )
    : assignments
        .filter((assignment) => assignment.memberId === user.id)
        .map((assignment) => ({
          assignment,
          task: tasks.find((task) => task.id === assignment.taskId),
        }))
        .filter((item): item is { task: ProjectTask; assignment: TaskAssignment } => !!item.task);

  const currentTask = submittingAssignment
    ? (tasks.find((task) => task.id === submittingAssignment.taskId) ?? null)
    : null;

  return (
    <>
      <PageHeader
        eyebrow={isAdmin ? "System oversight" : "Member workspace"}
        title="Tasks"
        description={
          isAdmin
            ? "Read-only overview of project tasks and member assignment states."
            : "Your assigned project tasks, submissions, and requested changes."
        }
      />

      <Panel
        title={isAdmin ? "All assignments" : "Your assigned tasks"}
        description={`${visiblePairs.length} assignment${visiblePairs.length === 1 ? "" : "s"}`}
      >
        {error ? (
          <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
            {error}
          </div>
        ) : null}

        {visiblePairs.length === 0 ? (
          <EmptyState
            title={isAdmin ? "No task assignments yet" : "No assigned tasks"}
            hint={
              isAdmin
                ? "Leader-created project tasks will appear here."
                : "When a project leader assigns you work, it will appear here."
            }
          />
        ) : (
          <div className="grid gap-3">
            {visiblePairs.map(({ task, assignment }) => {
              const project = projects.find((project) => project.id === task.projectId);
              const submission = latestSubmission(submissions, task.id, assignment.memberId);
              const status = getEffectiveTaskStatus(task);
              const canStart =
                !isAdmin &&
                (assignment.status === "assigned" || assignment.status === "needs_feedback") &&
                (task.status === "todo" || task.status === "in_progress");
              const canSubmit =
                !isAdmin &&
                (assignment.status === "working" || assignment.status === "needs_feedback") &&
                task.status !== "done" &&
                task.status !== "cancelled";

              return (
                <article key={assignment.id} className="rounded-lg border border-hairline p-4">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="text-sm font-semibold text-ink">{task.title}</h2>
                        <StatusPill tone={taskStatusTone(status)}>
                          {labelTaskStatus(status)}
                        </StatusPill>
                        <StatusPill tone={assignmentStatusTone(assignment.status)}>
                          {labelAssignmentStatus(assignment.status)}
                        </StatusPill>
                        <StatusPill tone={priorityTone(task.priority)}>
                          Priority: {labelPriority(task.priority)}
                        </StatusPill>
                      </div>
                      <p className="mt-2 text-sm leading-relaxed text-ink-soft">
                        {task.description}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-1.5">
                      {project ? (
                        <Link
                          to="/app/projects/$slug"
                          params={{ slug: project.slug }}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Project
                        </Link>
                      ) : null}
                      {canStart ? (
                        <button
                          onClick={() => {
                            const result = startWork(task.id, assignment.memberId);
                            setError(result.ok ? null : result.error);
                          }}
                          className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
                        >
                          <Play className="h-3.5 w-3.5" /> Start
                        </button>
                      ) : null}
                      {canSubmit ? (
                        <button
                          onClick={() => {
                            setSubmittingAssignment(assignment);
                            setError(null);
                          }}
                          className="inline-flex items-center gap-1 rounded-md bg-primary px-2.5 py-1 text-xs font-medium text-primary-foreground hover:opacity-90"
                        >
                          <Send className="h-3.5 w-3.5" />{" "}
                          {assignment.status === "needs_feedback" ? "Resubmit" : "Submit"}
                        </button>
                      ) : null}
                    </div>
                  </div>

                  <div className="mt-4 grid gap-3 text-xs md:grid-cols-3">
                    <Meta label="Project" value={project?.name ?? "Project not found"} />
                    <Meta label="Due" value={task.dueDate ? formatDate(task.dueDate) : "Not set"} />
                    <Meta label="Output criteria" value={task.outputCriteria} />
                  </div>

                  {assignment.reviewNote ? (
                    <div className="mt-3 rounded-lg border border-hairline bg-muted/40 p-3 text-xs">
                      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        Leader feedback
                      </div>
                      <p className="mt-1 text-ink">{assignment.reviewNote}</p>
                    </div>
                  ) : null}

                  {submission ? (
                    <div className="mt-3 rounded-lg border border-hairline p-3 text-xs">
                      <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        Latest submission
                      </div>
                      <p className="mt-1 text-ink">{submission.summary}</p>
                      {submission.artifactUrl ? (
                        <a
                          href={submission.artifactUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="mt-2 inline-flex text-ink underline-offset-4 hover:underline"
                        >
                          Open artifact
                        </a>
                      ) : null}
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        )}
      </Panel>

      <TaskSubmissionDialog
        task={currentTask}
        assignment={submittingAssignment}
        open={!!submittingAssignment}
        submitting={busy}
        error={error}
        onClose={() => {
          if (!busy) {
            setSubmittingAssignment(null);
            setError(null);
          }
        }}
        onSubmit={(payload) => {
          if (!currentTask || !submittingAssignment || busy) return;
          setBusy(true);
          const result = submitOutput(currentTask.id, submittingAssignment.memberId, payload);
          setBusy(false);
          if (!result.ok) {
            setError(result.error);
            return;
          }
          setSubmittingAssignment(null);
          setError(null);
        }}
      />
    </>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</dt>
      <dd className="mt-1 leading-relaxed text-ink">{value}</dd>
    </div>
  );
}

function labelTaskStatus(status: string) {
  const labels: Record<string, string> = {
    draft: "Draft",
    todo: "To Do",
    in_progress: "In Progress",
    in_review: "In Review",
    done: "Done",
    overdue: "Overdue",
    cancelled: "Cancelled",
  };
  return labels[status] ?? status;
}

function labelAssignmentStatus(status: string) {
  const labels: Record<string, string> = {
    assigned: "Assigned",
    working: "Working",
    submitted: "Submitted",
    needs_feedback: "Needs Feedback",
    completed: "Completed",
  };
  return labels[status] ?? status;
}

function labelPriority(priority: string) {
  const labels: Record<string, string> = {
    low: "Low",
    medium: "Medium",
    high: "High",
    critical: "Critical",
  };
  return labels[priority] ?? priority;
}
