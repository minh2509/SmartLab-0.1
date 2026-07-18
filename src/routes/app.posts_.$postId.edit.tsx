import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { PostForm } from "@/components/app/posts/PostForm";
import { PageHeader, Panel, StatusPill } from "@/components/app/ui";
import { useAuth } from "@/lib/auth";
import {
  canMemberEditPost,
  postStatusLabel,
  postStatusTone,
  usePosts,
  type PostDraft,
} from "@/lib/posts-data";

export const Route = createFileRoute("/app/posts_/$postId/edit")({
  head: () => ({
    meta: [{ title: "Edit Post — Nova workspace" }, { name: "robots", content: "noindex" }],
  }),
  component: EditPostPage,
});

function EditPostPage() {
  const { postId } = Route.useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { posts, updateDraft, submitForReview } = usePosts();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!user) return null;
  const post = posts.find((item) => item.id === postId);

  if (!post) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <h1 className="text-sm font-semibold text-ink">Post not found</h1>
        <p className="mt-1 text-xs text-ink-soft">This draft may have been deleted.</p>
        <Link to="/app/posts" className="mt-4 inline-flex text-sm text-ink hover:opacity-70">
          Back to my posts
        </Link>
      </div>
    );
  }

  if (!user.roles.includes("member") || !canMemberEditPost(post, user.id)) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-hairline bg-surface-elev p-8 text-center">
        <StatusPill tone={postStatusTone(post.status)}>{postStatusLabel[post.status]}</StatusPill>
        <h1 className="mt-4 text-sm font-semibold text-ink">Unauthorized edit</h1>
        <p className="mt-1 text-xs text-ink-soft">
          You can only edit your own Draft or Needs Revision posts.
        </p>
        <Link to="/app/posts" className="mt-4 inline-flex text-sm text-ink hover:opacity-70">
          Back to my posts
        </Link>
      </div>
    );
  }

  const save = (draft: PostDraft) => {
    if (saving) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    const result = updateDraft(post.id, user.id, draft);
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setSuccess("Draft saved.");
  };

  const submit = (draft: PostDraft) => {
    if (saving) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    const saved = updateDraft(post.id, user.id, draft);
    if (!saved.ok) {
      setSaving(false);
      setError(saved.error);
      return;
    }
    const submitted = submitForReview(post.id, user.id);
    setSaving(false);
    if (!submitted.ok) {
      setError(submitted.error);
      return;
    }
    navigate({ to: "/app/posts" });
  };

  return (
    <>
      <PageHeader
        eyebrow="Member publishing"
        title={post.title}
        description="Edit your authored content before sending it back to the admin moderation queue."
      />
      <Panel
        title="Post editor"
        description="Draft and revision posts are editable by the author only."
      >
        <PostForm
          post={post}
          saving={saving}
          error={error}
          success={success}
          onCancel={() => navigate({ to: "/app/posts" })}
          onSave={save}
          onSubmitForReview={submit}
        />
      </Panel>
    </>
  );
}
