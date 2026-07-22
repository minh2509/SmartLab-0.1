import { Send, UserRoundCheck } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  isUuid,
  type CreateAdminNotificationInput,
  type NotificationTargetType,
} from "@/lib/admin-api";

type FormState = {
  title: string;
  message: string;
  notificationType: string;
  targetType: NotificationTargetType;
  userIds: string;
  projectId: string;
  relatedType: string;
  relatedId: string;
  linkUrl: string;
};

const EMPTY_FORM: FormState = {
  title: "",
  message: "",
  notificationType: "ADMIN_ANNOUNCEMENT",
  targetType: "LAB",
  userIds: "",
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
  onClose,
  onSubmit,
}: {
  open: boolean;
  pending: boolean;
  error: string | null;
  currentUser: { id: string; fullName: string; email: string };
  onClose: () => void;
  onSubmit: (input: CreateAdminNotificationInput) => Promise<void>;
}) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const errors = useMemo(() => validate(form), [form]);
  const valid = Object.keys(errors).length === 0;

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
                hint={`${form.notificationType.length}/80`}
              >
                {(controlProps) => (
                  <input
                    {...controlProps}
                    maxLength={80}
                    value={form.notificationType}
                    disabled={pending}
                    onBlur={() => setTouched((current) => ({ ...current, notificationType: true }))}
                    onChange={(event) => setForm({ ...form, notificationType: event.target.value })}
                    className="admin-notification-input font-mono"
                    placeholder="ADMIN_ANNOUNCEMENT"
                  />
                )}
              </Field>

              <Field id="admin-notification-target" label="Target" required>
                {(controlProps) => (
                  <select
                    {...controlProps}
                    value={form.targetType}
                    disabled={pending}
                    onChange={(event) =>
                      setForm({ ...form, targetType: event.target.value as NotificationTargetType })
                    }
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
                  label="Recipient UUIDs"
                  required
                  hint="Comma, space, or new line separated"
                  error={touched.userIds ? errors.userIds : undefined}
                >
                  {(controlProps) => (
                    <textarea
                      {...controlProps}
                      rows={3}
                      value={form.userIds}
                      disabled={pending}
                      onBlur={() => setTouched((current) => ({ ...current, userIds: true }))}
                      onChange={(event) => setForm({ ...form, userIds: event.target.value })}
                      className="admin-notification-input min-h-20 resize-y font-mono text-xs"
                      placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                    />
                  )}
                </Field>
                <button
                  type="button"
                  disabled={pending}
                  onClick={() => {
                    const ids = parseUserIds(form.userIds);
                    if (!ids.includes(currentUser.id)) ids.push(currentUser.id);
                    setForm({ ...form, userIds: ids.join("\n") });
                    setTouched((current) => ({ ...current, userIds: true }));
                  }}
                  className="mt-2 inline-flex items-center gap-1.5 rounded-md border border-hairline px-2.5 py-1.5 text-xs text-ink hover:bg-muted disabled:opacity-45"
                >
                  <UserRoundCheck className="h-3.5 w-3.5" /> Add my account · {currentUser.email}
                </button>
              </div>
            ) : null}

            {form.targetType === "PROJECT" ? (
              <Field
                id="admin-notification-project-id"
                label="Project UUID"
                required
                hint="Use the ID shown in Join Request detail"
                error={touched.projectId ? errors.projectId : undefined}
              >
                {(controlProps) => (
                  <>
                    <input
                      {...controlProps}
                      value={form.projectId}
                      disabled={pending}
                      onBlur={() => setTouched((current) => ({ ...current, projectId: true }))}
                      onChange={(event) => setForm({ ...form, projectId: event.target.value })}
                      className="admin-notification-input font-mono"
                      placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                    />
                    <p className="mt-1.5 text-[11px] leading-relaxed text-ink-soft">
                      A project selector is unavailable until a backend project-list API exists.
                    </p>
                  </>
                )}
              </Field>
            ) : null}

            <div className="rounded-lg border border-hairline p-4">
              <div className="text-sm font-medium text-ink">Related record</div>
              <p className="mt-1 text-xs leading-relaxed text-ink-soft">
                Optional. Type and ID must either both be filled or both be empty.
              </p>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Field
                  id="admin-notification-related-type"
                  label="Related type"
                  error={touched.relatedType ? errors.relatedType : undefined}
                >
                  {(controlProps) => (
                    <input
                      {...controlProps}
                      maxLength={80}
                      value={form.relatedType}
                      disabled={pending}
                      onBlur={() => setTouched((current) => ({ ...current, relatedType: true }))}
                      onChange={(event) => setForm({ ...form, relatedType: event.target.value })}
                      className="admin-notification-input font-mono"
                      placeholder="PROJECT"
                    />
                  )}
                </Field>
                <Field
                  id="admin-notification-related-id"
                  label="Related UUID"
                  error={touched.relatedId ? errors.relatedId : undefined}
                >
                  {(controlProps) => (
                    <input
                      {...controlProps}
                      value={form.relatedId}
                      disabled={pending}
                      onBlur={() => setTouched((current) => ({ ...current, relatedId: true }))}
                      onChange={(event) => setForm({ ...form, relatedId: event.target.value })}
                      className="admin-notification-input font-mono"
                      placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                    />
                  )}
                </Field>
              </div>
            </div>

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

function validate(form: FormState) {
  const errors: Record<string, string> = {};
  const title = form.title.trim();
  const type = form.notificationType.trim();
  const projectId = form.projectId.trim();
  const relatedType = form.relatedType.trim();
  const relatedId = form.relatedId.trim();
  const linkUrl = form.linkUrl.trim();
  const userIds = parseUserIds(form.userIds);

  if (!title) errors.title = "Title is required.";
  else if (title.length > 255) errors.title = "Title must not exceed 255 characters.";
  if (!type) errors.notificationType = "Notification type is required.";
  else if (type.length > 80) errors.notificationType = "Type must not exceed 80 characters.";

  if (form.targetType === "USER") {
    if (userIds.length === 0) errors.userIds = "Add at least one user UUID.";
    else if (userIds.some((id) => !isUuid(id)))
      errors.userIds = "Every recipient must be a valid UUID.";
  }
  if (form.targetType === "PROJECT" && !isUuid(projectId)) {
    errors.projectId = "A valid project UUID is required.";
  }

  if (relatedType.length > 80) errors.relatedType = "Related type must not exceed 80 characters.";
  if (Boolean(relatedType) !== Boolean(relatedId)) {
    errors.relatedType = "Related type and UUID must be supplied together.";
    errors.relatedId = "Related type and UUID must be supplied together.";
  } else if (relatedId && !isUuid(relatedId)) {
    errors.relatedId = "Related ID must be a valid UUID.";
  }

  if (linkUrl && !isValidLink(linkUrl)) {
    errors.linkUrl = "Use an application path beginning with / or an HTTP(S) URL.";
  }
  return errors;
}

function toInput(form: FormState): CreateAdminNotificationInput {
  const relatedType = form.relatedType.trim();
  const relatedId = form.relatedId.trim();
  return {
    title: form.title.trim(),
    message: form.message.trim() || null,
    notificationType: form.notificationType.trim(),
    targetType: form.targetType,
    userIds: form.targetType === "USER" ? Array.from(new Set(parseUserIds(form.userIds))) : [],
    projectId: form.targetType === "PROJECT" ? form.projectId.trim() : null,
    relatedType: relatedType || null,
    relatedId: relatedId || null,
    linkUrl: form.linkUrl.trim() || null,
  };
}

function parseUserIds(value: string) {
  return value
    .split(/[\s,;]+/)
    .map((item) => item.trim())
    .filter(Boolean);
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
