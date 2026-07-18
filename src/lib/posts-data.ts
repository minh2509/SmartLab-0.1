import { useCallback, useEffect, useState } from "react";
import { publicPosts as legacyPublicPosts } from "@/lib/lab-data";
import { formatUserName } from "@/lib/users-data";

export type PostStatus =
  "draft" | "pending_review" | "needs_revision" | "approved" | "published" | "rejected";

export type PostCategory = "news" | "publication" | "announcement" | "research_story";

export type LabPost = {
  id: string;
  slug: string;
  authorId: string;
  title: string;
  excerpt: string;
  content: string;
  category: PostCategory;
  status: PostStatus;
  createdAt: string;
  updatedAt: string;
  submittedAt?: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewReason?: string;
  publishedAt?: string;
  publishedBy?: string;
};

export type PostDraft = Pick<LabPost, "title" | "excerpt" | "content" | "category">;

type Result<T = LabPost> = { ok: true; post: T } | { ok: false; error: string };

const STORAGE_KEY = "nova.labPosts.v1";
const statuses: PostStatus[] = [
  "draft",
  "pending_review",
  "needs_revision",
  "approved",
  "published",
  "rejected",
];
const categories: PostCategory[] = ["news", "publication", "announcement", "research_story"];

export const postCategoryLabel: Record<PostCategory, string> = {
  news: "News",
  publication: "Publication",
  announcement: "Announcement",
  research_story: "Research story",
};

export const postStatusLabel: Record<PostStatus, string> = {
  draft: "Draft",
  pending_review: "Pending review",
  needs_revision: "Needs revision",
  approved: "Approved",
  published: "Published",
  rejected: "Rejected",
};

export function postStatusTone(status: PostStatus) {
  if (status === "published" || status === "approved") return "emerald";
  if (status === "pending_review") return "amber";
  if (status === "needs_revision") return "violet";
  if (status === "rejected") return "rose";
  return "neutral";
}

export function postCategoryTone(category: PostCategory) {
  if (category === "publication") return "violet";
  if (category === "announcement") return "amber";
  if (category === "research_story") return "emerald";
  return "cyan";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function makeId() {
  return `post_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function parseLegacyDate(date: string) {
  const parsed = new Date(date);
  return Number.isNaN(parsed.getTime()) ? new Date().toISOString() : parsed.toISOString();
}

function legacyCategory(value: string): PostCategory {
  if (value === "Publication") return "publication";
  if (value === "Announcement") return "announcement";
  if (value === "Research story") return "research_story";
  return "news";
}

const legacyContent: Record<string, string> = {
  "atlas-v2-release":
    "Atlas v2 is the first release from the perception group to stay stable through repeated long-horizon warehouse runs. The update combines better event-camera calibration, a smaller neural-field map, and a stricter recovery routine when lighting changes quickly.\n\nIn the latest internal benchmark, drift dropped by 63% compared with the previous stack. The team also reduced restart time after temporary tracking loss, which makes the system easier to demonstrate in crowded lab conditions.\n\nThe next milestone is field validation with dynamic obstacles and longer routes. We are keeping the release public because several partner labs are testing similar sensor configurations this semester.",
  "helix-icse-2025":
    "Helix has been accepted to the ICSE 2025 Research Track. The paper studies retrieval-grounded program repair for large Java systems, with a focus on explaining why a patch is plausible before suggesting the change.\n\nThe accepted version includes a new defect localization benchmark and an ablation showing that project-specific context matters more than larger prompts alone. This is especially relevant for educational and enterprise codebases where style and architecture vary widely.\n\nThe team will release the evaluation harness after camera-ready review so other labs can reproduce the main experiments.",
  "phd-openings-2025":
    "Nova Research Lab is opening two funded PhD positions for Fall 2025. One position will join Atlas, focusing on robust robot perception. The second will join Meridian, focusing on low-resource clinical NLP and evaluation.\n\nApplicants should have strong engineering fundamentals, a clear research interest, and comfort working in collaborative project teams. Prior publication experience is helpful but not required.\n\nApplications close May 30. Shortlisted candidates will be invited to a technical interview and a project-fit discussion with the relevant research group.",
};

function legacySeed(): LabPost[] {
  return legacyPublicPosts.map((post, index) => {
    const date = parseLegacyDate(post.date);
    const authorId = index === 1 ? "u_amara" : index === 0 ? "u_tran" : "u_admin";
    return {
      id: `seed_${post.slug}`,
      slug: post.slug,
      authorId,
      title: post.title,
      excerpt: post.excerpt,
      content:
        legacyContent[post.slug] ??
        `${post.excerpt}\n\nThis public update was migrated from the original Nova Lab homepage seed content.`,
      category: legacyCategory(post.category),
      status: "published",
      createdAt: date,
      updatedAt: date,
      submittedAt: date,
      reviewedAt: date,
      reviewedBy: "u_admin",
      publishedAt: date,
      publishedBy: "u_admin",
    };
  });
}

function normalizeCategory(value: unknown): PostCategory | null {
  const raw = readString(value);
  if (categories.includes(raw as PostCategory)) return raw as PostCategory;
  if (raw) return legacyCategory(raw);
  return null;
}

function normalizePost(value: unknown): LabPost | null {
  if (!isRecord(value)) return null;
  const id = readString(value.id);
  const title = readString(value.title);
  const authorId = readString(value.authorId);
  const status = readString(value.status) as PostStatus;
  const category = normalizeCategory(value.category);
  const createdAt = readString(value.createdAt);
  const updatedAt = readString(value.updatedAt) || createdAt;
  if (!id || !title || !authorId || !statuses.includes(status) || !category || !createdAt) {
    return null;
  }

  return {
    id,
    slug: readString(value.slug) || slugify(title),
    authorId,
    title,
    excerpt: readString(value.excerpt),
    content: readString(value.content),
    category,
    status,
    createdAt,
    updatedAt,
    submittedAt: readString(value.submittedAt) || undefined,
    reviewedAt: readString(value.reviewedAt) || undefined,
    reviewedBy: readString(value.reviewedBy) || undefined,
    reviewReason: readString(value.reviewReason) || undefined,
    publishedAt: readString(value.publishedAt) || undefined,
    publishedBy: readString(value.publishedBy) || undefined,
  };
}

function load(): LabPost[] {
  if (typeof window === "undefined") return legacySeed();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return legacySeed();
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return legacySeed();
    const normalized = parsed.map(normalizePost).filter((item): item is LabPost => !!item);
    return normalized.length > 0 ? normalized : legacySeed();
  } catch {
    return legacySeed();
  }
}

function save(list: LabPost[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    return true;
  } catch {
    return false;
  }
}

type Sub = (list: LabPost[]) => void;
const subs = new Set<Sub>();
let cache: LabPost[] | null = null;

function getAll() {
  if (!cache) cache = load();
  return cache;
}

function setAll(next: LabPost[]) {
  const previous = cache;
  cache = next;
  const ok = save(next);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? []));
  return ok;
}

export function slugify(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 72);
}

export function createUniqueSlug(title: string, posts: LabPost[], excludeId?: string) {
  const base = slugify(title) || "lab-post";
  const used = new Set(posts.filter((post) => post.id !== excludeId).map((post) => post.slug));
  if (!used.has(base)) return base;
  let index = 2;
  let next = `${base}-${index}`;
  while (used.has(next)) {
    index += 1;
    next = `${base}-${index}`;
  }
  return next;
}

function cleanDraft(draft: PostDraft): PostDraft {
  return {
    title: draft.title.trim(),
    excerpt: draft.excerpt.trim(),
    content: draft.content.trim(),
    category: draft.category,
  };
}

export function canMemberEditPost(post: LabPost, userId: string) {
  return post.authorId === userId && (post.status === "draft" || post.status === "needs_revision");
}

export function canSubmitPost(post: LabPost, userId: string) {
  return canMemberEditPost(post, userId);
}

export function getPublishedPosts(posts: LabPost[]) {
  return posts
    .filter((post) => post.status === "published")
    .sort(
      (a, b) =>
        new Date(b.publishedAt ?? b.updatedAt).getTime() -
        new Date(a.publishedAt ?? a.updatedAt).getTime(),
    );
}

export function getAuthorName(id: string) {
  const name = formatUserName(id);
  return name === "Unknown" ? "Unknown author" : name;
}

export function formatPublicDate(iso?: string) {
  if (!iso) return "Unscheduled";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Unscheduled";
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function estimateReadMinutes(content: string) {
  const words = content.trim().split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.ceil(words / 180));
}

export function usePosts() {
  const [list, setList] = useState<LabPost[]>(() => getAll());

  useEffect(() => {
    const cb: Sub = (posts) => setList(posts);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const createDraft = useCallback((authorId: string, draft: PostDraft): Result => {
    const cleaned = cleanDraft(draft);
    const now = new Date().toISOString();
    const current = getAll();
    const post: LabPost = {
      id: makeId(),
      slug: createUniqueSlug(cleaned.title, current),
      authorId,
      ...cleaned,
      status: "draft",
      createdAt: now,
      updatedAt: now,
    };
    const ok = setAll([post, ...current]);
    return ok
      ? { ok: true, post }
      : { ok: false, error: "The post could not be saved in this browser." };
  }, []);

  const updateDraft = useCallback((postId: string, userId: string, draft: PostDraft): Result => {
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (!canMemberEditPost(post, userId)) {
      return { ok: false, error: "You can only edit your own draft or revision post." };
    }
    const cleaned = cleanDraft(draft);
    const next: LabPost = {
      ...post,
      ...cleaned,
      slug: createUniqueSlug(cleaned.title, current, post.id),
      updatedAt: new Date().toISOString(),
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok
      ? { ok: true, post: next }
      : { ok: false, error: "The post update could not be saved." };
  }, []);

  const submitForReview = useCallback((postId: string, userId: string): Result => {
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (!canSubmitPost(post, userId)) {
      return { ok: false, error: "Only your own draft or revision post can be submitted." };
    }
    const now = new Date().toISOString();
    const next: LabPost = {
      ...post,
      status: "pending_review",
      submittedAt: now,
      updatedAt: now,
      reviewedAt: undefined,
      reviewedBy: undefined,
      reviewReason: undefined,
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok
      ? { ok: true, post: next }
      : { ok: false, error: "The post could not be submitted for review." };
  }, []);

  const approve = useCallback((postId: string, adminId: string): Result => {
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (post.status !== "pending_review") {
      return { ok: false, error: "Only pending review posts can be approved." };
    }
    const now = new Date().toISOString();
    const next: LabPost = {
      ...post,
      status: "approved",
      reviewedAt: now,
      reviewedBy: adminId,
      reviewReason: undefined,
      updatedAt: now,
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok ? { ok: true, post: next } : { ok: false, error: "The approval could not be saved." };
  }, []);

  const requestRevision = useCallback((postId: string, adminId: string, reason: string): Result => {
    const reviewReason = reason.trim();
    if (!reviewReason) return { ok: false, error: "Revision reason is required." };
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (post.status !== "pending_review") {
      return { ok: false, error: "Only pending review posts can be sent for revision." };
    }
    const now = new Date().toISOString();
    const next: LabPost = {
      ...post,
      status: "needs_revision",
      reviewedAt: now,
      reviewedBy: adminId,
      reviewReason,
      updatedAt: now,
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok
      ? { ok: true, post: next }
      : { ok: false, error: "The revision request could not be saved." };
  }, []);

  const reject = useCallback((postId: string, adminId: string, reason: string): Result => {
    const reviewReason = reason.trim();
    if (!reviewReason) return { ok: false, error: "Rejection reason is required." };
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (post.status !== "pending_review") {
      return { ok: false, error: "Only pending review posts can be rejected." };
    }
    const now = new Date().toISOString();
    const next: LabPost = {
      ...post,
      status: "rejected",
      reviewedAt: now,
      reviewedBy: adminId,
      reviewReason,
      updatedAt: now,
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok
      ? { ok: true, post: next }
      : { ok: false, error: "The rejection could not be saved." };
  }, []);

  const publish = useCallback((postId: string, adminId: string): Result => {
    const current = getAll();
    const post = current.find((item) => item.id === postId);
    if (!post) return { ok: false, error: "Post not found." };
    if (post.status !== "approved") {
      return { ok: false, error: "Only approved posts can be published." };
    }
    const now = new Date().toISOString();
    const next: LabPost = {
      ...post,
      status: "published",
      publishedAt: now,
      publishedBy: adminId,
      updatedAt: now,
    };
    const ok = setAll(current.map((item) => (item.id === postId ? next : item)));
    return ok
      ? { ok: true, post: next }
      : { ok: false, error: "The publication could not be saved." };
  }, []);

  const remove = useCallback((postId: string) => {
    const current = getAll();
    if (!current.some((post) => post.id === postId)) {
      return { ok: false, error: "Post not found." };
    }
    return setAll(current.filter((post) => post.id !== postId))
      ? { ok: true }
      : { ok: false, error: "The post could not be deleted." };
  }, []);

  return {
    posts: list,
    publicPosts: getPublishedPosts(list),
    createDraft,
    updateDraft,
    submitForReview,
    approve,
    requestRevision,
    reject,
    publish,
    remove,
  };
}
