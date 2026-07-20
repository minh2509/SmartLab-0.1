import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { PostForm } from "@/components/app/posts/PostForm";
import { PageHeader, Panel } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import { usePosts, type PostDraft } from "@/lib/posts-data";

export const Route = createFileRoute("/app/posts/new")({
  head: () => ({
    meta: [{ title: "New Post — Smartworkspace" }, { name: "robots", content: "noindex" }],
  }),
  component: NewPostPage,
});

function NewPostPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { createDraft } = usePosts();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;
  if (!user.roles.includes("member")) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <h1 className="text-sm font-semibold text-ink">Unauthorized post creation</h1>
        <p className="mt-1 text-xs text-ink-soft">Only Members can create post drafts.</p>
      </div>
    );
  }

  const save = (draft: PostDraft) => {
    if (saving) return;
    setSaving(true);
    setError(null);
    const result = createDraft(user.id, draft);
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    navigate({ to: "/app/posts/$postId/edit", params: { postId: result.post.id } });
  };

  return (
    <>
      <PageHeader
        eyebrow="Member publishing"
        title="New post draft"
        description="All posts start as Draft. Submit from the edit screen when the content is ready for admin review."
      />
      <Panel
        title="Draft editor"
        description="Use a structured textarea; no rich-text editor is installed."
      >
        <PostForm
          saving={saving}
          error={error}
          onCancel={() => navigate({ to: "/app/posts" })}
          onSave={save}
        />
      </Panel>
    </>
  );
}
