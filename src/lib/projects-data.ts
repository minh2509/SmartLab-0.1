import { useEffect, useState, useCallback } from "react";
import { formatUserInitials, formatUserName } from "@/lib/users-data";

export type ProjectStatus = "Planning" | "Active" | "Publishing" | "Completed" | "On hold";

export type ProjectType = "Research" | "Production";
export type ProjectVisibility = "public" | "internal";
export type FieldKey = "ai" | "robotics" | "se";

export type Project = {
  id: string;
  slug: string;
  code: string;
  name: string;
  description: string;
  objective: string;
  type: ProjectType;
  fields: FieldKey[];
  leaderIds: string[];
  memberIds: string[];
  startDate: string; // ISO
  expectedEnd: string; // ISO
  status: ProjectStatus;
  progress: number; // 0-100
  visibility: ProjectVisibility;
};

const seed: Project[] = [
  {
    id: "prj_atlas",
    slug: "atlas-perception",
    code: "NL-24-07",
    name: "Atlas — Long-horizon perception",
    description:
      "A perception stack combining event cameras and neural fields for stable localization in unstructured warehouse environments.",
    objective:
      "Achieve <1% drift over 60-minute autonomous runs on the internal warehouse benchmark by Q4 2026.",
    type: "Research",
    fields: ["robotics", "ai"],
    leaderIds: ["u_tran", "u_amara"],
    memberIds: ["u_linh"],
    startDate: "2024-03-04",
    expectedEnd: "2026-12-15",
    status: "Active",
    progress: 62,
    visibility: "public",
  },
  {
    id: "prj_helix",
    slug: "helix-code-reasoning",
    code: "NL-25-02",
    name: "Helix — Program repair with grounded LLMs",
    description:
      "Retrieval-grounded language models that localize, explain and repair defects in large Java codebases.",
    objective:
      "Publish end-to-end evaluation on Defects4J-XL and ship an internal reviewer bot for the lab codebase.",
    type: "Research",
    fields: ["se", "ai"],
    leaderIds: ["u_amara"],
    memberIds: ["u_linh"],
    startDate: "2025-01-13",
    expectedEnd: "2026-06-30",
    status: "Active",
    progress: 41,
    visibility: "public",
  },
  {
    id: "prj_meridian",
    slug: "meridian-clinical-nlp",
    code: "NL-23-11",
    name: "Meridian — Clinical NLP for low-resource languages",
    description:
      "Pretraining and evaluation pipelines for medical text understanding in Vietnamese and Bahasa hospital records.",
    objective:
      "Release a peer-reviewed benchmark and a de-identified Vietnamese discharge-note corpus.",
    type: "Research",
    fields: ["ai"],
    leaderIds: ["u_tran"],
    memberIds: [],
    startDate: "2023-09-01",
    expectedEnd: "2025-11-30",
    status: "Publishing",
    progress: 88,
    visibility: "public",
  },
  {
    id: "prj_orbit",
    slug: "orbit-lab-platform",
    code: "NL-25-06",
    name: "Orbit — Internal lab platform",
    description:
      "Production system that hosts the lab website, member portal, and review workflows across all projects.",
    objective:
      "Ship v1.0 with authentication, project management, and post review before the Spring 2026 intake.",
    type: "Production",
    fields: ["se"],
    leaderIds: ["u_amara"],
    memberIds: [],
    startDate: "2025-05-20",
    expectedEnd: "2026-03-01",
    status: "Active",
    progress: 34,
    visibility: "internal",
  },
  {
    id: "prj_tactile",
    slug: "tactile-gripper",
    code: "NL-24-14",
    name: "Tactile gripper for fragile-object handling",
    description:
      "A soft-robotic gripper that combines resistive tactile skin with model-predictive grasp planning.",
    objective: "Demonstrate 95% success on the fruit-picking benchmark with damage-rate under 2%.",
    type: "Research",
    fields: ["robotics"],
    leaderIds: ["u_tran"],
    memberIds: ["u_linh"],
    startDate: "2024-10-01",
    expectedEnd: "2026-02-28",
    status: "On hold",
    progress: 18,
    visibility: "public",
  },
  {
    id: "prj_pulse",
    slug: "pulse-review-bench",
    code: "NL-22-03",
    name: "Pulse — Code review evaluation harness",
    description:
      "Legacy benchmark and dataset for evaluating AI code reviewers against human reviewers.",
    objective: "Retire after final report; hand dataset to Helix.",
    type: "Research",
    fields: ["se"],
    leaderIds: ["u_amara"],
    memberIds: [],
    startDate: "2022-04-11",
    expectedEnd: "2024-05-30",
    status: "Completed",
    progress: 100,
    visibility: "public",
  },
];

const STORAGE_KEY = "smart.projects.v1";

function load(): Project[] {
  if (typeof window === "undefined") return seed;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seed;
    return JSON.parse(raw) as Project[];
  } catch {
    return seed;
  }
}

function save(list: Project[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    return true;
  } catch {
    return false;
  }
}

// simple in-memory pub/sub so hooks stay in sync
type Sub = (list: Project[]) => void;
const subs = new Set<Sub>();
let cache: Project[] | null = null;

function getAll(): Project[] {
  if (!cache) cache = load();
  return cache;
}
function setAll(next: Project[]) {
  cache = next;
  const ok = save(next);
  subs.forEach((s) => s(next));
  return ok;
}

export function useProjects() {
  const [list, setList] = useState<Project[]>(() => getAll());
  useEffect(() => {
    const cb: Sub = (l) => setList(l);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const update = useCallback((id: string, patch: Partial<Project>) => {
    return setAll(getAll().map((p) => (p.id === id ? { ...p, ...patch } : p)));
  }, []);
  const remove = useCallback((id: string) => {
    return setAll(getAll().filter((p) => p.id !== id));
  }, []);
  const reset = useCallback(() => {
    return setAll(seed);
  }, []);
  const addMember = useCallback((projectId: string, userId: string) => {
    const project = getAll().find((p) => p.id === projectId);
    if (!project) return false;
    if (project.leaderIds.includes(userId)) return true;
    if (project.memberIds.includes(userId)) return true;
    return setAll(
      getAll().map((p) => (p.id === projectId ? { ...p, memberIds: [...p.memberIds, userId] } : p)),
    );
  }, []);

  return { projects: list, update, remove, reset, addMember };
}

export const fieldMeta: Record<FieldKey, { name: string; tone: "cyan" | "emerald" | "violet" }> = {
  ai: { name: "Artificial Intelligence", tone: "cyan" },
  robotics: { name: "Robotics", tone: "emerald" },
  se: { name: "Software Engineering", tone: "violet" },
};

export const statusTone: Record<
  ProjectStatus,
  "cyan" | "emerald" | "violet" | "amber" | "neutral"
> = {
  Planning: "violet",
  Active: "cyan",
  Publishing: "emerald",
  Completed: "neutral",
  "On hold": "amber",
};

export function getUserName(id: string) {
  return formatUserName(id);
}
export function getUserInitials(id: string) {
  return formatUserInitials(id);
}

export function formatDate(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
