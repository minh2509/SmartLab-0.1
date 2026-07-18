import { useMemo, useState } from "react";
import {
  postCategoryLabel,
  postStatusLabel,
  type LabPost,
  type PostCategory,
  type PostDraft,
} from "@/lib/posts-data";

const categories: PostCategory[] = ["news", "publication", "announcement", "research_story"];
const excerptMax = 220;
const contentMin = 80;

export type PostFormAction = "save" | "submit";

function validatePostDraft(draft: PostDraft) {
  const errors: Partial<Record<keyof PostDraft, string>> = {};
  if (!draft.title.trim()) errors.title = "Title is required.";
  if (!draft.excerpt.trim()) errors.excerpt = "Excerpt is required.";
  if (draft.excerpt.trim().length > excerptMax) {
    errors.excerpt = `Excerpt must be ${excerptMax} characters or less.`;
  }
  if (!draft.content.trim()) errors.content = "Main content is required.";
  if (draft.content.trim().length > 0 && draft.content.trim().length < contentMin) {
    errors.content = `Main content should be at least ${contentMin} characters.`;
  }
  if (!categories.includes(draft.category)) errors.category = "Category is required.";
  return errors;
}

function cleanPostDraft(draft: PostDraft): PostDraft {
  return {
    title: draft.title.trim(),
    excerpt: draft.excerpt.trim(),
    content: draft.content.trim(),
    category: draft.category,
  };
}

export function PostForm({
  post,
  saving,
  error,
  success,
  onCancel,
  onSave,
  onSubmitForReview,
}: {
  post?: LabPost;
  saving: boolean;
  error?: string | null;
  success?: string | null;
  onCancel: () => void;
  onSave: (draft: PostDraft) => void;
  onSubmitForReview?: (draft: PostDraft) => void;
}) {
  const [draft, setDraft] = useState<PostDraft>({
    title: post?.title ?? "",
    excerpt: post?.excerpt ?? "",
    content: post?.content ?? "",
    category: post?.category ?? "news",
  });
  const [touched, setTouched] = useState(false);
  const errors = useMemo(() => validatePostDraft(draft), [draft]);
  const invalid = Object.keys(errors).length > 0;

  const submit = (action: PostFormAction) => (event: React.FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (invalid || saving) return;
    const cleaned = cleanPostDraft(draft);
    if (action === "submit" && onSubmitForReview) onSubmitForReview(cleaned);
    else onSave(cleaned);
  };

  return (
    <form className="space-y-5" onSubmit={submit("save")}>
      {post ? (
        <div className="rounded-lg border border-hairline bg-muted/35 p-3 text-xs">
          <div className="text-[11px] uppercase tracking-[0.14em] text-ink-soft">
            Current status
          </div>
          <div className="mt-1 font-medium text-ink">{postStatusLabel[post.status]}</div>
          {post.reviewReason ? (
            <p className="mt-2 leading-relaxed text-ink-soft">
              Review reason: <span className="text-ink">{post.reviewReason}</span>
            </p>
          ) : null}
        </div>
      ) : null}

      <Field label="Title" error={touched ? errors.title : undefined}>
        <input
          className="input"
          value={draft.title}
          onBlur={() => setTouched(true)}
          onChange={(event) => setDraft({ ...draft, title: event.target.value })}
          placeholder="A precise research-lab headline"
        />
      </Field>

      <Field label="Excerpt" error={touched ? errors.excerpt : undefined}>
        <textarea
          className="input min-h-[88px]"
          value={draft.excerpt}
          maxLength={excerptMax + 40}
          onBlur={() => setTouched(true)}
          onChange={(event) => setDraft({ ...draft, excerpt: event.target.value })}
          placeholder="Short public summary for cards and previews."
        />
        <div className="mt-1 text-right text-[11px] text-ink-soft">
          {draft.excerpt.trim().length}/{excerptMax}
        </div>
      </Field>

      <Field label="Category" error={touched ? errors.category : undefined}>
        <select
          className="input"
          value={draft.category}
          onChange={(event) => setDraft({ ...draft, category: event.target.value as PostCategory })}
        >
          {categories.map((category) => (
            <option key={category} value={category}>
              {postCategoryLabel[category]}
            </option>
          ))}
        </select>
      </Field>

      <Field label="Main content" error={touched ? errors.content : undefined}>
        <textarea
          className="input min-h-[300px] leading-relaxed"
          value={draft.content}
          onBlur={() => setTouched(true)}
          onChange={(event) => setDraft({ ...draft, content: event.target.value })}
          placeholder="Write the post body. Use blank lines to separate paragraphs."
        />
      </Field>

      {error ? (
        <div className="rounded-md border border-[color:var(--destructive)]/40 bg-[color-mix(in_oklab,var(--destructive)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--destructive)]">
          {error}
        </div>
      ) : null}
      {success ? (
        <div className="rounded-md border border-[color-mix(in_oklab,var(--emerald-ink)_35%,transparent)] bg-[color-mix(in_oklab,var(--emerald-ink)_10%,transparent)] px-3 py-2 text-xs text-[color:var(--emerald-ink)]">
          {success}
        </div>
      ) : null}

      <div className="flex flex-col-reverse gap-2 border-t border-hairline pt-5 sm:flex-row sm:justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-hairline px-3 py-1.5 text-sm text-ink hover:bg-muted"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={saving || (touched && invalid)}
          className="rounded-md border border-hairline px-3.5 py-1.5 text-sm font-medium text-ink transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          {saving ? "Saving..." : "Save draft"}
        </button>
        {onSubmitForReview ? (
          <button
            type="button"
            disabled={saving || invalid}
            onClick={submit("submit")}
            className="rounded-md bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {saving ? "Submitting..." : "Submit for review"}
          </button>
        ) : null}
      </div>
    </form>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <div className="mb-1 text-[11px] uppercase tracking-[0.14em] text-ink-soft">{label}</div>
      {children}
      {error ? <div className="mt-1 text-xs text-[color:var(--destructive)]">{error}</div> : null}
    </label>
  );
}
