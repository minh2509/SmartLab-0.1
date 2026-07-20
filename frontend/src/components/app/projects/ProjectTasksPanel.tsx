import { useState } from "react";
import { type Role } from "@/lib/auth";
import { formatDate, getUserName, type Project } from "@/lib/projects-data";
import {
  assignmentStatusTone,
  getEffectiveTaskStatus,
  latestSubmission,
  priorityTone,
  taskStatusTone,
  useProjectTasks,
  type ProjectTask,
  type TaskAssignment,
  type TaskDraft,
  type TaskSubmission,
} from "@/lib/tasks-data";
import { EmptyState, Panel, StatusPill } from "@/components/app/ui";
import { TaskFormDialog } from "./TaskFormDialog";
import { TaskSubmissionDialog } from "./TaskSubmissionDialog";
import { TaskReviewDialog } from "./TaskReviewDialog";
import { cn } from "@/lib/utils";
import { Check, Pencil, Play, Send, XCircle } from "lucide-react";
import { notifyManyOnce, notifyOnce } from "@/lib/notifications-data";
import { getUserById, type UserAccount } from "@/lib/users-data";

export function ProjectTasksPanel({
  project,
  user,
  activeRole,
}: {
  project: Project;
  user: UserAccount;
  activeRole: Role;
}) {
  const taskStore = useProjectTasks();
  const {
    tasks,
    assignments,
    submissions,
    createTask,
    updateTask,
    publishTask,
    cancelTask,
    startWork,
    submitOutput,
    acceptSubmission,
    requestChanges,
  } = taskStore;

  const [editingTask, setEditingTask] = useState<ProjectTask | null>(null);
  const [creating, setCreating] = useState(false);
  const [submittingAssignment, setSubmittingAssignment] = useState<TaskAssignment | null>(null);
  const [reviewing, setReviewing] = useState<{
    task: ProjectTask;
    assignment: TaskAssignment;
    submission: TaskSubmission;
    mode: "accept" | "changes";
  } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAssignedLeader =
    activeRole === "leader" && user.roles.includes("leader") && project.leaderIds.includes(user.id);
  const isAdmin = activeRole === "admin" && user.roles.includes("admin");
  const isMember = project.memberIds.includes(user.id);
  const projectTasks = tasks.filter((task) => task.projectId === project.id);
  const visibleTasks =
    isAssignedLeader || isAdmin
      ? projectTasks
      : projectTasks.filter((task) => task.assigneeIds.includes(user.id));

  const saveTask = (draft: TaskDraft) => {
    if (!isAssignedLeader) {
      setError("Only assigned project leaders can manage tasks.");
      return;
    }
    const invalidAssignees = draft.assigneeIds.filter((id) => {
      const account = getUserById(id);
      const alreadyAssigned = editingTask?.assigneeIds.includes(id);
      return (
        !project.memberIds.includes(id) ||
        !account ||
        (account.status === "locked" && !alreadyAssigned)
      );
    });
    if (invalidAssignees.length > 0) {
      setError("Assignees must be known members of this project.");
      return;
    }
    const result = editingTask
      ? updateTask(editingTask.id, draft)
      : createTask(project.id, user.id, draft);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    if (editingTask && editingTask.status !== "draft") {
      const addedAssignees = result.value.assigneeIds.filter(
        (memberId) => !editingTask.assigneeIds.includes(memberId),
      );
      notifyManyOnce(
        addedAssignees.map((memberId) => ({
          userId: memberId,
          type: "task_assigned",
          title: "New task assigned",
          message: `You were assigned to ${result.value.title} in ${project.name}.`,
          link: `/app/projects/${project.slug}`,
          eventKey: `task:${result.value.id}:assigned:${memberId}`,
        })),
      );
      notifyManyOnce(
        result.value.assigneeIds.map((memberId) => ({
          userId: memberId,
          type: "task_updated",
          title: "Task updated",
          message: `${result.value.title} was updated in ${project.name}.`,
          link: `/app/projects/${project.slug}`,
          eventKey: `task:${result.value.id}:updated:${result.value.updatedAt}:${memberId}`,
        })),
      );
    }
    setCreating(false);
    setEditingTask(null);
    setError(null);
  };

  const currentSubmissionTask = submittingAssignment
    ? (tasks.find((task) => task.id === submittingAssignment.taskId) ?? null)
    : null;

  return (
    <>
      <Panel
        title="Tasks"
        description={
          isAssignedLeader
            ? `${projectTasks.length} task${projectTasks.length === 1 ? "" : "s"} in this project`
            : isAdmin
              ? "Read-only task oversight"
              : `${visibleTasks.length} assigned to you`
        }
        action={
          isAssignedLeader ? (
            <button
              onClick={() => {
                setCreating(true);
                setEditingTask(null);
                setError(null);
              }}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
            >
              New task
            </button>
          ) : undefined
        }
      >
        {error ? (
          <div className="mb-4 rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
            {error}
          </div>
        ) : null}

        {visibleTasks.length === 0 ? (
          <EmptyState
            title={isAssignedLeader ? "No project tasks yet" : "No assigned tasks"}
            hint={
              isAssignedLeader
                ? "Create a draft task, assign project members, then publish it."
                : "Assigned tasks for this project will appear here."
            }
          />
        ) : (
          <div className="grid gap-3">
            {visibleTasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                assignments={assignments.filter((assignment) => assignment.taskId === task.id)}
                submissions={submissions}
                currentUserId={user.id}
                canManage={isAssignedLeader}
                canSubmit={isMember && task.assigneeIds.includes(user.id)}
                readOnly={isAdmin && !isAssignedLeader}
                onEdit={() => {
                  setEditingTask(task);
                  setCreating(false);
                  setError(null);
                }}
                onPublish={() => {
                  const result = publishTask(task.id);
                  if (result.ok) {
                    notifyManyOnce(
                      result.value.assigneeIds.map((memberId) => ({
                        userId: memberId,
                        type: "task_assigned",
                        title: "New task assigned",
                        message: `You were assigned to ${result.value.title} in ${project.name}.`,
                        link: `/app/projects/${project.slug}`,
                        eventKey: `task:${result.value.id}:assigned:${memberId}`,
                      })),
                    );
                  }
                  setError(result.ok ? null : result.error);
                }}
                onCancel={() => {
                  const result = cancelTask(task.id);
                  setError(result.ok ? null : result.error);
                }}
                onStart={(assignment) => {
                  const result = startWork(task.id, assignment.memberId);
                  setError(result.ok ? null : result.error);
                }}
                onSubmit={(assignment) => {
                  setSubmittingAssignment(assignment);
                  setError(null);
                }}
                onReview={(assignment, submission, mode) => {
                  setReviewing({ task, assignment, submission, mode });
                  setError(null);
                }}
              />
            ))}
          </div>
        )}
      </Panel>

      <TaskFormDialog
        project={project}
        task={editingTask}
        open={creating || !!editingTask}
        error={error}
        onClose={() => {
          setCreating(false);
          setEditingTask(null);
          setError(null);
        }}
        onSave={saveTask}
      />

      <TaskSubmissionDialog
        task={currentSubmissionTask}
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
          if (!submittingAssignment || !currentSubmissionTask || busy) return;
          setBusy(true);
          const result = submitOutput(
            currentSubmissionTask.id,
            submittingAssignment.memberId,
            payload,
          );
          setBusy(false);
          if (!result.ok) {
            setError(result.error);
            return;
          }
          notifyManyOnce(
            project.leaderIds.map((leaderId) => ({
              userId: leaderId,
              type: "task_submitted",
              title: "Task output submitted",
              message: `${getUserName(submittingAssignment.memberId)} submitted output for ${currentSubmissionTask.title}.`,
              link: `/app/projects/${project.slug}`,
              eventKey: `task:${currentSubmissionTask.id}:submitted:${result.value.id}:${leaderId}`,
            })),
          );
          setSubmittingAssignment(null);
          setError(null);
        }}
      />

      <TaskReviewDialog
        task={reviewing?.task ?? null}
        assignment={reviewing?.assignment ?? null}
        submission={reviewing?.submission ?? null}
        mode={reviewing?.mode ?? "accept"}
        error={error}
        onClose={() => {
          setReviewing(null);
          setError(null);
        }}
        onSubmit={(note) => {
          if (!reviewing) return;
          if (!isAssignedLeader || reviewing.assignment.memberId === user.id) {
            setError("Only assigned leaders can review another member's submission.");
            return;
          }
          const result =
            reviewing.mode === "accept"
              ? acceptSubmission(reviewing.task.id, reviewing.assignment.memberId, user.id, note)
              : requestChanges(reviewing.task.id, reviewing.assignment.memberId, user.id, note);
          if (!result.ok) {
            setError(result.error);
            return;
          }
          notifyOnce({
            userId: reviewing.assignment.memberId,
            type: "task_reviewed",
            title: reviewing.mode === "accept" ? "Task output accepted" : "Task changes requested",
            message:
              reviewing.mode === "accept"
                ? `${reviewing.task.title} was accepted in ${project.name}.`
                : `Changes were requested for ${reviewing.task.title} in ${project.name}.`,
            link: `/app/projects/${project.slug}`,
            eventKey: `task:${reviewing.task.id}:reviewed:${reviewing.assignment.memberId}:${reviewing.mode}:${new Date().toISOString()}`,
          });
          setReviewing(null);
          setError(null);
        }}
      />
    </>
  );
}

function TaskCard({
  task,
  assignments,
  submissions,
  currentUserId,
  canManage,
  canSubmit,
  readOnly,
  onEdit,
  onPublish,
  onCancel,
  onStart,
  onSubmit,
  onReview,
}: {
  task: ProjectTask;
  assignments: TaskAssignment[];
  submissions: TaskSubmission[];
  currentUserId: string;
  canManage: boolean;
  canSubmit: boolean;
  readOnly: boolean;
  onEdit: () => void;
  onPublish: () => void;
  onCancel: () => void;
  onStart: (assignment: TaskAssignment) => void;
  onSubmit: (assignment: TaskAssignment) => void;
  onReview: (
    assignment: TaskAssignment,
    submission: TaskSubmission,
    mode: "accept" | "changes",
  ) => void;
}) {
  const effectiveStatus = getEffectiveTaskStatus(task);
  const currentAssignment = assignments.find((assignment) => assignment.memberId === currentUserId);
  const canEditTask = canManage && task.status !== "done" && task.status !== "cancelled";
  const canCancelTask =
    canManage &&
    (task.status === "draft" || task.status === "todo" || task.status === "in_progress");
  const canPublishTask = canManage && task.status === "draft";

  return (
    <article className="rounded-lg border border-hairline p-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm font-semibold text-ink">{task.title}</h3>
            <StatusPill tone={taskStatusTone(effectiveStatus)}>
              {labelTaskStatus(effectiveStatus)}
            </StatusPill>
            <StatusPill tone={priorityTone(task.priority)}>
              Priority: {labelPriority(task.priority)}
            </StatusPill>
          </div>
          <p className="mt-2 text-sm leading-relaxed text-ink-soft">{task.description}</p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-1.5">
          {canPublishTask ? <IconButton label="Publish" icon={Send} onClick={onPublish} /> : null}
          {canEditTask ? <IconButton label="Edit" icon={Pencil} onClick={onEdit} /> : null}
          {canCancelTask ? <IconButton label="Cancel" icon={XCircle} onClick={onCancel} /> : null}
          {readOnly ? (
            <span className="rounded-md border border-hairline px-2.5 py-1 text-[11px] text-ink-soft">
              Read only
            </span>
          ) : null}
        </div>
      </div>

      <div className="mt-4 grid gap-3 text-xs md:grid-cols-3">
        <Meta label="Start" value={task.startDate ? formatDate(task.startDate) : "Not set"} />
        <Meta label="Due" value={task.dueDate ? formatDate(task.dueDate) : "Not set"} />
        <Meta label="Output criteria" value={task.outputCriteria} />
      </div>

      <div className="mt-4 border-t border-hairline pt-4">
        <div className="mb-2 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
          Assignments
        </div>
        <div className="grid gap-2">
          {assignments.map((assignment) => {
            const submission = latestSubmission(submissions, task.id, assignment.memberId);
            const ownsAssignment = canSubmit && assignment.memberId === currentUserId;
            const startable =
              ownsAssignment &&
              (assignment.status === "assigned" || assignment.status === "needs_feedback") &&
              (task.status === "todo" || task.status === "in_progress");
            const submittable =
              ownsAssignment &&
              (assignment.status === "working" || assignment.status === "needs_feedback") &&
              task.status !== "done" &&
              task.status !== "cancelled";
            const reviewable = canManage && assignment.status === "submitted" && submission;

            return (
              <div
                key={assignment.id}
                className={cn(
                  "rounded-md border border-hairline p-3",
                  assignment.status === "needs_feedback" && "bg-muted/30",
                )}
              >
                <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-medium text-ink">
                        {getUserName(assignment.memberId)}
                      </span>
                      <StatusPill tone={assignmentStatusTone(assignment.status)}>
                        {labelAssignmentStatus(assignment.status)}
                      </StatusPill>
                    </div>
                    {assignment.reviewNote ? (
                      <p className="mt-2 max-w-2xl text-xs leading-relaxed text-ink-soft">
                        Feedback: <span className="text-ink">{assignment.reviewNote}</span>
                      </p>
                    ) : null}
                    {submission ? (
                      <p className="mt-2 max-w-2xl text-xs leading-relaxed text-ink-soft">
                        Latest submission: <span className="text-ink">{submission.summary}</span>
                      </p>
                    ) : null}
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-1.5">
                    {startable ? (
                      <IconButton label="Start" icon={Play} onClick={() => onStart(assignment)} />
                    ) : null}
                    {submittable ? (
                      <IconButton
                        label={assignment.status === "needs_feedback" ? "Resubmit" : "Submit"}
                        icon={Send}
                        onClick={() => onSubmit(assignment)}
                      />
                    ) : null}
                    {reviewable ? (
                      <>
                        <IconButton
                          label="Accept"
                          icon={Check}
                          onClick={() => onReview(assignment, submission, "accept")}
                        />
                        <IconButton
                          label="Changes"
                          icon={XCircle}
                          onClick={() => onReview(assignment, submission, "changes")}
                        />
                      </>
                    ) : null}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </article>
  );
}

function IconButton({
  label,
  icon: Icon,
  onClick,
}: {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="inline-flex items-center gap-1 rounded-md border border-hairline px-2 py-1 text-xs text-ink-soft hover:bg-muted hover:text-ink"
    >
      <Icon className="h-3.5 w-3.5" />
      {label}
    </button>
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
