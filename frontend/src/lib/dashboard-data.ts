export const pendingApprovals = [
  {
    id: "p_112",
    title: "Atlas v2 — 40-minute warehouse benchmark results",
    author: "Linh Pham",
    project: "Atlas",
    submitted: "2h ago",
    kind: "Publication",
  },
  {
    id: "p_109",
    title: "Meridian annotation guidelines v3 (VI)",
    author: "Kenji Alvarado",
    project: "Meridian",
    submitted: "1d ago",
    kind: "Internal doc",
  },
  {
    id: "p_104",
    title: "PhD openings — Fall 2026 announcement",
    author: "Amara Osei",
    project: "Lab-wide",
    submitted: "2d ago",
    kind: "Announcement",
  },
];

export const recentActivity = [
  { who: "Amara Osei", what: "approved 2 posts in Helix", when: "12m ago" },
  { who: "Dr. Minh Tran", what: "requested revisions on Atlas v2 draft", when: "1h ago" },
  { who: "Linh Pham", what: "submitted a publication for review", when: "2h ago" },
  { who: "Kenji Alvarado", what: "joined Meridian project", when: "Yesterday" },
];

export const systemStatus = [
  { label: "Auth service", status: "Operational", tone: "emerald" as const },
  { label: "File storage", status: "Operational", tone: "emerald" as const },
  { label: "Notifications", status: "Degraded", tone: "amber" as const },
];

export const leaderProjects = [
  {
    code: "NL-24-07",
    name: "Atlas — Long-horizon perception",
    members: 9,
    openTasks: 14,
    pendingReviews: 3,
    health: "On track",
    tone: "emerald" as const,
  },
  {
    code: "NL-25-02",
    name: "Helix — Program repair with LLMs",
    members: 6,
    openTasks: 21,
    pendingReviews: 5,
    health: "Needs attention",
    tone: "amber" as const,
  },
];

export const leaderReviewQueue = [
  {
    title: "Atlas v2 — 40-minute warehouse benchmark results",
    author: "Linh Pham",
    waiting: "2h",
  },
  {
    title: "Event-camera calibration script (PR #148)",
    author: "Nikolai Weiss",
    waiting: "6h",
  },
  {
    title: "Helix ablation table — LLaMA vs. Mistral",
    author: "Amara Osei",
    waiting: "1d",
  },
];

export const memberTasks = [
  { title: "Rerun Atlas benchmark on hallway-B split", due: "Fri", state: "In progress" },
  { title: "Draft reviewer response for ICSE camera-ready", due: "Mon", state: "Not started" },
  { title: "Update lab wiki — event-camera setup", due: "Next week", state: "In progress" },
];

export type PostStatus =
  "Draft" | "Pending Review" | "Needs Revision" | "Approved" | "Published" | "Rejected";

export const memberPosts: { title: string; status: PostStatus; updated: string }[] = [
  { title: "Atlas v2 — warehouse benchmark results", status: "Pending Review", updated: "2h ago" },
  {
    title: "Field notes: tactile gripper trial 04",
    status: "Needs Revision",
    updated: "Yesterday",
  },
  { title: "ICRA workshop slides draft", status: "Draft", updated: "3d ago" },
  { title: "Atlas v1 retrospective", status: "Published", updated: "Last week" },
];

export const memberNotifications = [
  {
    title: "Dr. Tran requested revisions on your draft",
    detail: "Atlas v2 — warehouse benchmark results",
    when: "1h ago",
    unread: true,
  },
  {
    title: "New comment on your post",
    detail: "Amara Osei · Field notes: tactile gripper trial 04",
    when: "Yesterday",
    unread: true,
  },
  {
    title: "Weekly reading group — Thursday 4pm",
    detail: "Paper: Neural fields for SLAM (Chen et al., 2025)",
    when: "2d ago",
    unread: false,
  },
];
