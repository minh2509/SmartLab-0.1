import { Send, UserRoundCheck } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import {
  AdminEntityMultiPicker,
  AdminEntityPicker,
  type AdminPickerOption,
} from "@/components/app/AdminEntityPicker";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  type AdminProjectOption,
  type AdminUserOption,
  type CreateAdminNotificationInput,
  type NotificationTargetType,
} from "@/lib/admin-api";

type FormState = {
  title: string;
  message: string;
  notificationType: string;
  targetType: NotificationTargetType;
  userIds: string[];
  projectId: string;
  relatedType: "" | "PROJECT";
  relatedId: string;
  linkUrl: string;
};

const EMPTY_FORM: FormState = {
  title: "",
  message: "",
  notificationType: "ADMIN_ANNOUNCEMENT",
  targetType: "LAB",
  userIds: [],
  projectId: "",
  relatedType: "",
  relatedId: "",
  linkUrl: "/app/notifications",
};

export function AdminNotificationCreateDialog({
  open,
  pending,
  error,
  currentUser,
  users,
  projects,
  creatableNotificationTypes,
  userLookupLoading,
  userLookupError,
  projectLookupLoading,
  projectLookupError,
  notificationTypeLookupLoading,
  notificationTypeLookupError,
  onClose,
  onEdit,
  onSubmit,
}: {
  open: boolean;
  pending: boolean;
  error: string | null;
  currentUser: { id: string; fullName: string; email: string };
  users: AdminUserOption[];
  projects: AdminProjectOption[];
  creatableNotificationTypes: string[];
  userLookupLoading: boolean;
  userLookupError: string | null;
  projectLookupLoading: boolean;
  projectLookupError: string | null;
  notificationTypeLookupLoading: boolean;
  notificationTypeLookupError: string | null;
  onClose: () => void;
  onEdit: () => void;
  onSubmit: (input: CreateAdminNotificationInput) => Promise<void>;
}) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const errors = useMemo(
    () => validate(form, users, projects, currentUser.id, creatableNotificationTypes),
    [creatableNotificationTypes, currentUser.id, form, projects, users],
  );
  const valid = Object.keys(errors).length === 0;
  const userOptions = useMemo<AdminPickerOption[]>(() => {
    const options = users
      .filter((user) => user.accountStatus !== "DELETED")
      .map((user) => ({
        value: user.id,
        label: user.fullName,
        description: `${user.email} · ${humanize(user.accountStatus)}`,
        keywords: `${user.email} ${user.roleCodes.join(" ")}`,
      }));
    if (!options.some((option) => option.value === currentUser.id)) {
      options.unshift({
        value: currentUser.id,
        label: currentUser.fullName,
        description: `${currentUser.email} · Current account`,
        keywords: currentUser.email,
      });
    }
    return options;
  }, [currentUser, users]);
  const projectOptions = useMemo<AdminPickerOption[]>(
    () =>
      projects.map((project) => ({
        value: project.id,
        label: `${project.code} — ${project.name}`,
        description: `${humanize(project.status)} · ${project.activeRecipientCount} active recipient${
          project.activeRecipientCount === 1 ? "" : "s"
        }`,
        keywords: `${project.name} ${project.code} ${project.slug} ${project.status}`,
      })),
    [projects],
  );
  const targetProjectOptions = useMemo(
    () =>
      projectOptions.map((option) => ({
        ...option,
        disabled:
          projects.find((project) => project.id === option.value)?.activeRecipientCount === 0,
      })),
    [projectOptions, projects],
  );
  const notificationTypeOptions = useMemo<AdminPickerOption[]>(
    () =>
      creatableNotificationTypes.map((value) => ({
        value,
        label: humanize(value),
        description: value,
        keywords: value,
      })),
    [creatableNotificationTypes],
  );
  const selectedProject = projects.find((project) => project.id === form.projectId) || null;
  const selectedRelatedProject = projects.find((project) => project.id === form.relatedId) || null;

  const updateForm = (nextForm: FormState) => {
    setForm(nextForm);
    if (error) onEdit();
  };

  useEffect(() => {
    if (open) {
      setForm(EMPTY_FORM);
      setTouched({});
    }
  }, [open]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched({
      title: true,
      notificationType: true,
      userIds: true,
      projectId: true,
      relatedType: true,
      relatedId: true,
      linkUrl: true,
    });
    if (!valid || pending) return;
    void onSubmit(toInput(form));
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !pending) onClose();
      }}
    >
      <DialogContent className="max-h-[92vh] max-w-2xl overflow-y-auto border-hairline bg-surface-elev p-0">
        <DialogHeader className="border-b border-hairline px-5 py-4 pr-12">
          <DialogTitle className="text-base text-ink">Create system notification</DialogTitle>
          <DialogDescription className="text-xs text-ink-soft">
            Send a persisted notification to selected users, a project team, or the current lab.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={submit}>
          <div className="space-y-5 p-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                id="admin-notification-title"
                label="Title"
                required
                error={touched.title ? errors.title : undefined}
                className="sm:col-span-2"
              >
                {(controlProps) => (
                  <input
                    {...controlProps}
                    autoFocus
                    maxLength={255}
                    value={form.title}
                    disabled={pending}
                    onBlur={() => setTouched((current) => ({ ...current, title: true }))}
                    onChange={(event) => setForm({ ...form, title: event.target.value })}
                    className="admin-notification-input"
                    placeholder="Lab maintenance window"
                  />
                )}
              </Field>

              <Field
                id="admin-notification-type"
                label="Notification type"
                required
                error={touched.notificationType ? errors.notificationType : undefined}
              >
                {(controlProps) => (
                  <AdminEntityPicker
                    id={controlProps.id}
                    required={controlProps.required}
                    aria-required={controlProps["aria-required"]}
                    aria-invalid={controlProps["aria-invalid"]}
                    aria-describedby={controlProps["aria-describedby"]}
                    options={notificationTypeOptions}
                    value={form.notificationType}
                    disabled={pending}
                    onChange={(notificationType) => {
                      updateForm({ ...form, notificationType });
                      setTouched((current) => ({ ...current, notificationType: true }));
                    }}
                    placeholder="Choose a notification type"
                    searchPlaceholder="Search notification types…"
                    emptyMessage="No notification types are available for manual creation."
                    loading={notificationTypeLookupLoading}
                    error={notificationTypeLookupError}
                  />
                )}
              </Field>

              <Field id="admin-notification-target" label="Target" required>
                {(controlProps) => (
                  <select
                    {...controlProps}
                    value={form.targetType}
                    disabled={pending}
                    onChange={(event) => {
                      const targetType = event.target.value as NotificationTargetType;
                      updateForm({
                        ...form,
                        targetType,
                        userIds: targetType === "USER" ? form.userIds : [],
                        projectId: targetType === "PROJECT" ? form.projectId : "",
                      });
                    }}
                    className="admin-notification-input"
                  >
                    <option value="LAB">Entire lab</option>
                    <option value="USER">Selected users</option>
                    <option value="PROJECT">Active project members</option>
                  </select>
                )}
              </Field>
            </div>

            <Field id="admin-notification-message" label="Message" hint="Optional">
              {(controlProps) => (
                <textarea
                  {...controlProps}
                  rows={4}
                  value={form.message}
                  disabled={pending}
                  onChange={(event) => setForm({ ...form, message: event.target.value })}
                  className="admin-notification-input min-h-24 resize-y"
                  placeholder="Add enough context for recipients to understand the notification."
                />
              )}
            </Field>

            {form.targetType === "USER" ? (
              <div>
                <Field
                  id="admin-notification-user-ids"
                  label="Recipients"
                  required
                  hint="Search by name or email"
                  error={touched.userIds ? errors.userIds : undefined}
                >
                  {(controlProps) => (
                    <AdminEntityMultiPicker
                      id={controlProps.id}
                      required={controlProps.required}
                      aria-required={controlProps["aria-required"]}
                      aria-invalid={controlProps["aria-invalid"]}
                      aria-describedby={controlProps["aria-describedby"]}
                      options={userOptions}
                      values={form.userIds}
                      disabled={pending}
                      onChange={(userIds) => {
                        updateForm({ ...form, userIds });
                        setTouched((current) => ({ ...current, userIds: true }));
                      }}
                      placeholder="Choose one or more recipients"
                      searchPlaceholder="Search name, email, role, or status…"
                      emptyMessage="No matching users in this lab."
                      loading={userLookupLoading}
                      error={userLookupError}
                    />
                  )}
                </Field>
                <button
                  type="button"
                  disabled={pending}
                  onClick={() => {
                    const userIds = form.userIds.includes(currentUser.id)
                      ? form.userIds
                      : [...form.userIds, currentUser.id];
                    updateForm({ ...form, userIds });
                    setTouched((current) => ({ ...current, userIds: true }));
                  }}
                  className="mt-2 inline-flex items-center gap-1.5 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted disabled:opacity-45"
                >
                  <UserRoundCheck className="h-3.5 w-3.5" />
                  {form.userIds.includes(currentUser.id)
                    ? "My account selected"
                    : "Add my account"}{" "}
                  · {currentUser.email}
                </button>
              </div>
            ) : null}

            {form.targetType === "PROJECT" ? (
              <Field
                id="admin-notification-project-id"
                label="Project"
                required
                hint="Search by name or code"
                error={touched.projectId ? errors.projectId : undefined}
              >
                {(controlProps) => (
                  <AdminEntityPicker
                    id={controlProps.id}
                    required={controlProps.required}
                    aria-required={controlProps["aria-required"]}
                    aria-invalid={controlProps["aria-invalid"]}
                    aria-describedby={controlProps["aria-describedby"]}
                    options={targetProjectOptions}
                    value={form.projectId}
                    onChange={(projectId) => {
                      updateForm({ ...form, projectId });
                      setTouched((current) => ({ ...current, projectId: true }));
                    }}
                    placeholder="Choose a project"
                    searchPlaceholder="Search project name, code, slug, or status…"
                    emptyMessage="No projects are available in this lab."
                    loading={projectLookupLoading}
                    error={projectLookupError}
                    disabled={pending}
                  />
                )}
              </Field>
            ) : null}

            {form.targetType !== "PROJECT" ? (
              <div className="rounded-lg border border-hairline p-4">
                <div className="text-sm font-medium text-ink">Related record</div>
                <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                  Optional. Choose Project to attach context without entering an internal ID.
                </p>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <Field
                    id="admin-notification-related-type"
                    label="Related type"
                    error={touched.relatedType ? errors.relatedType : undefined}
                  >
                    {(controlProps) => (
                      <select
                        {...controlProps}
                        value={form.relatedType}
                        disabled={pending}
                        onBlur={() => setTouched((current) => ({ ...current, relatedType: true }))}
                        onChange={(event) =>
                          updateForm({
                            ...form,
                            relatedType: event.target.value as FormState["relatedType"],
                            relatedId: "",
                          })
                        }
                        className="admin-notification-input"
                      >
                        <option value="">No related record</option>
                        <option value="PROJECT">Project</option>
                      </select>
                    )}
                  </Field>
                  {form.relatedType === "PROJECT" ? (
                    <Field
                      id="admin-notification-related-id"
                      label="Related project"
                      required
                      error={touched.relatedId ? errors.relatedId : undefined}
                    >
                      {(controlProps) => (
                        <AdminEntityPicker
                          id={controlProps.id}
                          required={controlProps.required}
                          aria-required={controlProps["aria-required"]}
                          aria-invalid={controlProps["aria-invalid"]}
                          aria-describedby={controlProps["aria-describedby"]}
                          options={projectOptions}
                          value={form.relatedId}
                          onChange={(relatedId) => {
                            updateForm({ ...form, relatedId });
                            setTouched((current) => ({ ...current, relatedId: true }));
                          }}
                          placeholder="Choose a related project"
                          searchPlaceholder="Search project name, code, slug, or status…"
                          emptyMessage="No projects are available in this lab."
                          loading={projectLookupLoading}
                          error={projectLookupError}
                          disabled={pending}
                        />
                      )}
                    </Field>
                  ) : null}
                </div>
                {selectedRelatedProject ? (
                  <p className="mt-2 text-[11px] text-ink-soft">
                    Linked to {selectedRelatedProject.code} — {selectedRelatedProject.name}.
                  </p>
                ) : null}
              </div>
            ) : selectedProject ? (
              <div className="rounded-lg border border-hairline bg-muted/25 px-4 py-3">
                <div className="text-xs font-medium text-ink">
                  Project context attached automatically
                </div>
                <p className="mt-1 text-[11px] leading-relaxed text-ink-soft">
                  Recipients and the related record use {selectedProject.code} —{" "}
                  {selectedProject.name}.
                </p>
              </div>
            ) : null}

            <Field
              id="admin-notification-link"
              label="Destination link"
              hint="Optional app path or HTTP(S) URL"
              error={touched.linkUrl ? errors.linkUrl : undefined}
            >
              {(controlProps) => (
                <input
                  {...controlProps}
                  maxLength={500}
                  value={form.linkUrl}
                  disabled={pending}
                  onBlur={() => setTouched((current) => ({ ...current, linkUrl: true }))}
                  onChange={(event) => setForm({ ...form, linkUrl: event.target.value })}
                  className="admin-notification-input"
                  placeholder="/app/notifications"
                />
              )}
            </Field>

            {error ? (
              <div
                role="alert"
                className="rounded-md border border-[color:var(--destructive)]/35 bg-[color-mix(in_oklab,var(--destructive)_8%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]"
              >
                {error}
              </div>
            ) : null}
          </div>

          <footer className="flex flex-col-reverse gap-2 border-t border-hairline px-5 py-4 sm:flex-row sm:justify-end">
            <button
              type="button"
              disabled={pending}
              onClick={onClose}
              className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted disabled:opacity-45"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!valid || pending}
              title={!valid ? "Complete all required fields with valid values." : undefined}
              className="inline-flex items-center justify-center gap-1.5 rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-45"
            >
              <Send className="h-3.5 w-3.5" /> {pending ? "Sending…" : "Create notification"}
            </button>
          </footer>
        </form>

        <style>{`
          .admin-notification-input {
            width: 100%;
            border: 1px solid var(--hairline, hsl(0 0% 90%));
            background: var(--background);
            border-radius: 6px;
            padding: 8px 10px;
            font-size: 13px;
            color: inherit;
          }
          .admin-notification-input:focus { outline: 2px solid color-mix(in oklab, var(--cyan) 40%, transparent); }
          .admin-notification-input:disabled { cursor: not-allowed; opacity: .55; }
        `}</style>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  id,
  label,
  hint,
  required,
  error,
  className,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  required?: boolean;
  error?: string;
  className?: string;
  children: (controlProps: {
    id: string;
    required: boolean | undefined;
    "aria-required": boolean | undefined;
    "aria-invalid": boolean;
    "aria-describedby": string | undefined;
  }) => ReactNode;
}) {
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(" ");

  return (
    <div className={`block ${className || ""}`}>
      <span className="mb-1.5 flex items-center justify-between gap-3 text-xs font-medium text-ink">
        <label htmlFor={id}>
          {label} {required ? <span className="text-[color:var(--destructive)]">*</span> : null}
        </label>
        {hint ? (
          <span id={hintId} className="text-right font-normal text-ink-soft">
            {hint}
          </span>
        ) : null}
      </span>
      {children({
        id,
        required,
        "aria-required": required || undefined,
        "aria-invalid": Boolean(error),
        "aria-describedby": describedBy || undefined,
      })}
      {error ? (
        <span
          id={errorId}
          role="alert"
          className="mt-1 block text-[11px] text-[color:var(--destructive)]"
        >
          {error}
        </span>
      ) : null}
    </div>
  );
}

function validate(
  form: FormState,
  users: AdminUserOption[],
  projects: AdminProjectOption[],
  currentUserId: string,
  creatableNotificationTypes: string[],
) {
  const errors: Record<string, string> = {};
  const title = form.title.trim();
  const type = form.notificationType.trim();
  const projectId = form.projectId.trim();
  const relatedId = form.relatedId.trim();
  const linkUrl = form.linkUrl.trim();
  const availableUserIds = new Set([...users.map((user) => user.id), currentUserId]);
  const availableProjects = new Map(projects.map((project) => [project.id, project]));

  if (!title) errors.title = "Title is required.";
  else if (title.length > 255) errors.title = "Title must not exceed 255 characters.";
  if (!type) errors.notificationType = "Notification type is required.";
  else if (!creatableNotificationTypes.includes(type)) {
    errors.notificationType = "Choose an available notification type.";
  }

  if (form.targetType === "USER") {
    if (form.userIds.length === 0) errors.userIds = "Choose at least one recipient.";
    else if (form.userIds.some((id) => !availableUserIds.has(id))) {
      errors.userIds = "One or more selected recipients are no longer available.";
    }
  }
  if (form.targetType === "PROJECT") {
    const project = availableProjects.get(projectId);
    if (!project) errors.projectId = "Choose an available project.";
    else if (project.activeRecipientCount === 0) {
      errors.projectId = "Choose a project with at least one active recipient.";
    }
  }

  if (
    form.targetType !== "PROJECT" &&
    form.relatedType === "PROJECT" &&
    !availableProjects.has(relatedId)
  ) {
    errors.relatedId = "Choose an available related project.";
  }

  if (linkUrl && !isValidLink(linkUrl)) {
    errors.linkUrl = "Use an application path beginning with / or an HTTP(S) URL.";
  }
  return errors;
}

function toInput(form: FormState): CreateAdminNotificationInput {
  const projectId = form.targetType === "PROJECT" ? form.projectId.trim() : null;
  const relatedType = form.targetType === "PROJECT" ? "PROJECT" : form.relatedType || null;
  const relatedId =
    form.targetType === "PROJECT" ? projectId : form.relatedType ? form.relatedId : null;
  return {
    title: form.title.trim(),
    message: form.message.trim() || null,
    notificationType: form.notificationType.trim(),
    targetType: form.targetType,
    userIds: form.targetType === "USER" ? Array.from(new Set(form.userIds)) : [],
    projectId,
    relatedType,
    relatedId,
    linkUrl: form.linkUrl.trim() || null,
  };
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
}

function isValidLink(value: string) {
  if (value.length > 500 || value.startsWith("//")) return false;
  const hasControlOrWhitespace = Array.from(value).some(
    (character) => character.charCodeAt(0) <= 0x20,
  );
  if (hasControlOrWhitespace || /[\\<>"{}|^`]/.test(value) || /%(?![0-9a-f]{2})/i.test(value)) {
    return false;
  }
  if (value.startsWith("/")) return true;
  try {
    const url = new URL(value);
    return (url.protocol === "http:" || url.protocol === "https:") && Boolean(url.hostname);
  } catch {
    return false;
  }
}
