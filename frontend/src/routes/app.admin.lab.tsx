import { createFileRoute } from "@tanstack/react-router";
import {
  Building2,
  BookOpen,
  Plus,
  Pencil,
  ToggleLeft,
  ToggleRight,
  Globe,
  Mail,
  AlignLeft,
  Target,
  Eye,
  X,
} from "lucide-react";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { EmptyState, PageHeader, Panel, StatusPill } from "@/components/app/ui";
import {
  allResearchFields as seedFields,
  formatDate,
  type AdminResearchField,
} from "@/lib/members-data";
import { getMockLabInfo, updateMockLabInfo, type AdminLabInfo } from "@/lib/admin-lab-data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/admin/lab")({
  head: () => ({
    meta: [{ title: "Lab Settings — SmartLab Admin" }, { name: "robots", content: "noindex" }],
  }),
  component: AdminLabPage,
});

// ─── Lab Info Form ────────────────────────────────────────────────────────────
function LabInfoForm({
  lab,
  onSave,
}: {
  lab: AdminLabInfo;
  onSave: (updated: AdminLabInfo) => void;
}) {
  const [name, setName] = useState(lab.name);
  const [description, setDescription] = useState(lab.description ?? "");
  const [mission, setMission] = useState(lab.mission ?? "");
  const [vision, setVision] = useState(lab.vision ?? "");
  const [contactEmail, setContactEmail] = useState(lab.contactEmail ?? "");
  const [websiteUrl, setWebsiteUrl] = useState(lab.websiteUrl ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [logoFileId, setLogoFileId] = useState(lab.logoFileId ?? "");
  const [coverFileId, setCoverFileId] = useState(lab.coverFileId ?? "");
  const [imgSaving, setImgSaving] = useState(false);

  const nameError = !name.trim() ? "Lab name is required." : null;

  const handleSubmit = () => {
    if (nameError) {
      setError(nameError);
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    // TODO: PUT /api/admin/lab
    setTimeout(() => {
      const updated = updateMockLabInfo({
        name: name.trim(),
        description: description.trim() || null,
        mission: mission.trim() || null,
        vision: vision.trim() || null,
        contactEmail: contactEmail.trim() || null,
        websiteUrl: websiteUrl.trim() || null,
      });
      setSaving(false);
      setSuccess("Lab information saved successfully.");
      onSave(updated);
    }, 600);
  };

  const handleImageSave = () => {
    setImgSaving(true);
    // TODO: POST /api/admin/lab/logo and /api/admin/lab/cover
    setTimeout(() => {
      updateMockLabInfo({
        logoFileId: logoFileId.trim() || null,
        coverFileId: coverFileId.trim() || null,
      });
      setImgSaving(false);
      setSuccess("Lab images updated.");
    }, 500);
  };

  return (
    <div className="space-y-8">
      <Panel title="Lab information" description="General information visible to lab members.">
        <div className="space-y-4">
          <div>
            <label htmlFor="lab-name" className="mb-1 block text-xs font-medium text-ink">
              Lab name <span className="text-[color:var(--destructive)]">*</span>
            </label>
            <div className="relative">
              <Building2
                className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-soft"
                aria-hidden
              />
              <input
                id="lab-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className={cn(
                  "w-full rounded-md border bg-background py-2 pl-8 pr-3 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40",
                  nameError && name !== ""
                    ? "border-[color:var(--destructive)]"
                    : "border-hairline",
                )}
                placeholder="e.g. SmartResearch Lab"
              />
            </div>
            {nameError && name !== "" && (
              <p className="mt-1 text-xs text-[color:var(--destructive)]">{nameError}</p>
            )}
          </div>

          <div>
            <label htmlFor="lab-description" className="mb-1 block text-xs font-medium text-ink">
              <AlignLeft className="mr-1 inline h-3.5 w-3.5 text-ink-soft" aria-hidden />
              Description
            </label>
            <textarea
              id="lab-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full resize-none rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="Brief description of the lab and its research focus..."
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="lab-mission" className="mb-1 block text-xs font-medium text-ink">
                <Target className="mr-1 inline h-3.5 w-3.5 text-ink-soft" aria-hidden />
                Mission
              </label>
              <textarea
                id="lab-mission"
                value={mission}
                onChange={(e) => setMission(e.target.value)}
                rows={2}
                className="w-full resize-none rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
                placeholder="Lab mission statement..."
              />
            </div>
            <div>
              <label htmlFor="lab-vision" className="mb-1 block text-xs font-medium text-ink">
                <Eye className="mr-1 inline h-3.5 w-3.5 text-ink-soft" aria-hidden />
                Vision
              </label>
              <textarea
                id="lab-vision"
                value={vision}
                onChange={(e) => setVision(e.target.value)}
                rows={2}
                className="w-full resize-none rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
                placeholder="Lab vision statement..."
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="lab-email" className="mb-1 block text-xs font-medium text-ink">
                <Mail className="mr-1 inline h-3.5 w-3.5 text-ink-soft" aria-hidden />
                Contact email
              </label>
              <input
                id="lab-email"
                type="email"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
                className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
                placeholder="contact@lab.edu.vn"
              />
            </div>
            <div>
              <label htmlFor="lab-website" className="mb-1 block text-xs font-medium text-ink">
                <Globe className="mr-1 inline h-3.5 w-3.5 text-ink-soft" aria-hidden />
                Website URL
              </label>
              <input
                id="lab-website"
                type="url"
                value={websiteUrl}
                onChange={(e) => setWebsiteUrl(e.target.value)}
                className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
                placeholder="https://lab.example.edu"
              />
            </div>
          </div>

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
            {saving ? "Saving…" : "Save lab info"}
          </button>
        </div>
      </Panel>

      <Panel
        title="Lab images"
        description="Logo and cover image file identifiers (file upload coming soon)."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="lab-logo-id" className="mb-1 block text-xs font-medium text-ink">
              Logo file ID
            </label>
            <input
              id="lab-logo-id"
              value={logoFileId}
              onChange={(e) => setLogoFileId(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="UUID of the logo file"
            />
          </div>
          <div>
            <label htmlFor="lab-cover-id" className="mb-1 block text-xs font-medium text-ink">
              Cover image file ID
            </label>
            <input
              id="lab-cover-id"
              value={coverFileId}
              onChange={(e) => setCoverFileId(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="UUID of the cover file"
            />
          </div>
        </div>
        <button
          onClick={handleImageSave}
          disabled={imgSaving}
          className="mt-4 inline-flex items-center gap-2 rounded-md border border-hairline bg-surface-elev px-4 py-2 text-xs font-medium text-ink hover:bg-muted disabled:opacity-50"
        >
          {imgSaving ? "Saving…" : "Save image IDs"}
        </button>
      </Panel>
    </div>
  );
}

// ─── Research Field Dialog ────────────────────────────────────────────────────
function ResearchFieldDialog({
  mode,
  field,
  onClose,
  onSave,
  existingCodes,
}: {
  mode: "create" | "edit";
  field: AdminResearchField | null;
  onClose: () => void;
  onSave: (result: { code: string; name: string; description: string | null }) => void;
  existingCodes: string[];
}) {
  const [code, setCode] = useState(field?.code ?? "");
  const [name, setName] = useState(field?.name ?? "");
  const [description, setDescription] = useState(field?.description ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const codeValue = code.trim().toUpperCase();
  const isDuplicateCode = mode === "create" && existingCodes.includes(codeValue);

  const handleSubmit = () => {
    if (!name.trim()) {
      setError("Field name is required.");
      return;
    }
    if (mode === "create" && !code.trim()) {
      setError("Code is required.");
      return;
    }
    if (isDuplicateCode) {
      setError(`Code "${codeValue}" already exists.`);
      return;
    }

    setSaving(true);
    setError(null);
    // TODO: POST /api/admin/research-fields or PUT /api/admin/research-fields/{id}
    setTimeout(() => {
      setSaving(false);
      onSave({ code: codeValue, name: name.trim(), description: description.trim() || null });
    }, 400);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-xl border border-hairline bg-background shadow-2xl">
        <div className="flex items-center justify-between border-b border-hairline px-5 py-4">
          <h2 className="text-sm font-semibold text-ink">
            {mode === "create" ? "New research field" : "Edit research field"}
          </h2>
          <button
            onClick={onClose}
            aria-label="Close dialog"
            className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="space-y-4 p-5">
          {mode === "create" && (
            <div>
              <label htmlFor="rf-code" className="mb-1 block text-xs font-medium text-ink">
                Code <span className="text-[color:var(--destructive)]">*</span>
              </label>
              <input
                id="rf-code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink uppercase placeholder:normal-case placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
                placeholder="e.g. AI, ROBOTICS, SE"
                maxLength={50}
              />
              {isDuplicateCode && (
                <p className="mt-1 text-xs text-[color:var(--destructive)]">
                  This code already exists.
                </p>
              )}
            </div>
          )}
          <div>
            <label htmlFor="rf-name" className="mb-1 block text-xs font-medium text-ink">
              Name <span className="text-[color:var(--destructive)]">*</span>
            </label>
            <input
              id="rf-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="e.g. Artificial Intelligence"
              maxLength={150}
            />
          </div>
          <div>
            <label htmlFor="rf-desc" className="mb-1 block text-xs font-medium text-ink">
              Description
            </label>
            <textarea
              id="rf-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full resize-none rounded-md border border-hairline bg-background px-3 py-2 text-sm text-ink placeholder:text-ink-soft focus:outline-none focus:ring-2 focus:ring-[color:var(--cyan)]/40"
              placeholder="Short description of this research field..."
            />
          </div>
          {error && (
            <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
              {error}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-1">
            <button
              onClick={onClose}
              className="rounded-md border border-hairline px-3 py-1.5 text-xs font-medium text-ink-soft hover:bg-muted hover:text-ink"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={saving || isDuplicateCode}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {saving ? "Saving…" : mode === "create" ? "Create field" : "Save changes"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Research Fields Tab ──────────────────────────────────────────────────────
function ResearchFieldsTab() {
  const [fields, setFields] = useState<AdminResearchField[]>(seedFields);
  const [dialogMode, setDialogMode] = useState<"create" | "edit" | null>(null);
  const [editTarget, setEditTarget] = useState<AdminResearchField | null>(null);

  const existingCodes = fields.map((f) => f.code);

  const toggleStatus = (id: string) => {
    // Optimistic update
    setFields((prev) =>
      prev.map((f) =>
        f.id === id ? { ...f, status: f.status === "ACTIVE" ? "INACTIVE" : "ACTIVE" } : f,
      ),
    );
    // TODO: PATCH /api/admin/research-fields/{id}/status
  };

  const handleSave = (data: { code: string; name: string; description: string | null }) => {
    if (dialogMode === "create") {
      setFields((prev) => [
        ...prev,
        {
          id: `rf_${Date.now()}`,
          code: data.code,
          name: data.name,
          description: data.description,
          status: "ACTIVE",
          createdAt: new Date().toISOString(),
        },
      ]);
    } else if (editTarget) {
      setFields((prev) =>
        prev.map((f) =>
          f.id === editTarget.id ? { ...f, name: data.name, description: data.description } : f,
        ),
      );
    }
    setDialogMode(null);
    setEditTarget(null);
  };

  return (
    <div>
      <Panel
        title="Research fields"
        description="Active and inactive research areas in the lab catalogue."
        action={
          <button
            onClick={() => {
              setDialogMode("create");
              setEditTarget(null);
            }}
            className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90"
          >
            <Plus className="h-3.5 w-3.5" aria-hidden /> New field
          </button>
        }
        className="overflow-hidden"
      >
        {fields.length === 0 ? (
          <EmptyState title="No research fields" hint='Click "New field" to add the first one.' />
        ) : (
          <div className="-mx-5 -mb-5 overflow-x-auto">
            <table className="w-full min-w-[500px] text-sm" aria-label="Research fields">
              <thead>
                <tr className="border-t border-hairline text-left text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                  <th className="px-5 py-3 font-medium">Field</th>
                  <th className="px-3 py-3 font-medium">Code</th>
                  <th className="px-3 py-3 font-medium">Status</th>
                  <th className="px-3 py-3 font-medium">Created</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {fields.map((field) => (
                  <tr key={field.id} className="border-t border-hairline align-middle">
                    <td className="px-5 py-3">
                      <div className="font-medium text-ink">{field.name}</div>
                      {field.description && (
                        <div className="mt-0.5 max-w-xs truncate text-xs text-ink-soft">
                          {field.description}
                        </div>
                      )}
                    </td>
                    <td className="px-3 py-3">
                      <code className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono text-ink-soft">
                        {field.code}
                      </code>
                    </td>
                    <td className="px-3 py-3">
                      <StatusPill tone={field.status === "ACTIVE" ? "emerald" : "neutral"}>
                        {field.status === "ACTIVE" ? "Active" : "Inactive"}
                      </StatusPill>
                    </td>
                    <td className="px-3 py-3 text-xs text-ink-soft">
                      {formatDate(field.createdAt)}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => {
                            setEditTarget(field);
                            setDialogMode("edit");
                          }}
                          aria-label={`Edit ${field.name}`}
                          className="rounded-md p-1 text-ink-soft hover:bg-muted hover:text-ink transition-colors"
                        >
                          <Pencil className="h-3.5 w-3.5" aria-hidden />
                        </button>
                        <button
                          onClick={() => toggleStatus(field.id)}
                          aria-label={
                            field.status === "ACTIVE"
                              ? `Deactivate ${field.name}`
                              : `Activate ${field.name}`
                          }
                          className={cn(
                            "rounded-md p-1 transition-colors",
                            field.status === "ACTIVE"
                              ? "text-[color:var(--emerald-ink)] hover:bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)]"
                              : "text-ink-soft hover:bg-muted hover:text-ink",
                          )}
                        >
                          {field.status === "ACTIVE" ? (
                            <ToggleRight className="h-4 w-4" aria-hidden />
                          ) : (
                            <ToggleLeft className="h-4 w-4" aria-hidden />
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {dialogMode && (
        <ResearchFieldDialog
          mode={dialogMode}
          field={editTarget}
          existingCodes={existingCodes}
          onClose={() => {
            setDialogMode(null);
            setEditTarget(null);
          }}
          onSave={handleSave}
        />
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
function AdminLabPage() {
  const { user, activeRole } = useAuth();
  const [lab, setLab] = useState<AdminLabInfo>(getMockLabInfo);
  const [tab, setTab] = useState<"info" | "fields">("info");

  const isAdminView = activeRole === "admin" && !!user?.roles.includes("admin");

  if (!isAdminView) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-muted">
          <Building2 className="h-4 w-4 text-ink-soft" />
        </div>
        <h1 className="mt-4 text-sm font-semibold text-ink">Admin workspace required</h1>
        <p className="mt-1 text-xs text-ink-soft">
          Lab settings are available only when viewing as Admin.
        </p>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow="Admin"
        title="Lab Settings"
        description="Manage lab identity, contact information, and research field catalogue."
      />

      {/* Tab bar */}
      <div className="mb-6 flex border-b border-hairline">
        {(["info", "fields"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              "flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium transition-colors",
              tab === t ? "border-b-2 border-primary text-ink" : "text-ink-soft hover:text-ink",
            )}
          >
            {t === "info" && <Building2 className="h-4 w-4" aria-hidden />}
            {t === "fields" && <BookOpen className="h-4 w-4" aria-hidden />}
            {t === "info" ? "Lab Information" : "Research Fields"}
          </button>
        ))}
      </div>

      {tab === "info" ? <LabInfoForm lab={lab} onSave={setLab} /> : <ResearchFieldsTab />}
    </>
  );
}
