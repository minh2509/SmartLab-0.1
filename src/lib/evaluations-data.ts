import { useCallback, useEffect, useState } from "react";

export type EvaluationScores = {
  taskCompletion: number;
  workQuality: number;
  responsibility: number;
  teamwork: number;
  researchAbility: number;
  proactiveness: number;
};

export type MemberEvaluation = {
  id: string;
  projectId: string;
  memberId: string;
  evaluatorId: string;
  periodLabel: string;
  evaluationDate: string;
  scores: EvaluationScores;
  overallScore: number;
  strengths: string;
  weaknesses: string;
  suggestedImprovements: string;
  technicalSkillsObserved: string;
  nextLearningDirection: string;
  generalFeedback: string;
  createdAt: string;
  updatedAt: string;
};

export type EvaluationDraft = Pick<
  MemberEvaluation,
  | "memberId"
  | "periodLabel"
  | "evaluationDate"
  | "scores"
  | "strengths"
  | "weaknesses"
  | "suggestedImprovements"
  | "technicalSkillsObserved"
  | "nextLearningDirection"
  | "generalFeedback"
>;

type Result<T = MemberEvaluation> = { ok: true; value: T } | { ok: false; error: string };

const STORAGE_KEY = "nova.memberEvaluations.v1";
export const scoreKeys: (keyof EvaluationScores)[] = [
  "taskCompletion",
  "workQuality",
  "responsibility",
  "teamwork",
  "researchAbility",
  "proactiveness",
];

export const scoreLabels: Record<keyof EvaluationScores, string> = {
  taskCompletion: "Task completion",
  workQuality: "Work quality",
  responsibility: "Responsibility",
  teamwork: "Teamwork",
  researchAbility: "Research ability",
  proactiveness: "Proactiveness",
};

export function emptyScores(): EvaluationScores {
  return {
    taskCompletion: 3,
    workQuality: 3,
    responsibility: 3,
    teamwork: 3,
    researchAbility: 3,
    proactiveness: 3,
  };
}

export function calculateOverallScore(scores: EvaluationScores) {
  const total = scoreKeys.reduce((sum, key) => sum + scores[key], 0);
  return Math.round((total / scoreKeys.length) * 10) / 10;
}

export function scoreTone(score: number) {
  if (score >= 4.5) return "emerald";
  if (score >= 3.5) return "cyan";
  if (score >= 2.5) return "amber";
  return "rose";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function readScore(value: unknown) {
  return typeof value === "number" && Number.isInteger(value) && value >= 1 && value <= 5
    ? value
    : 0;
}

function normalizeScores(value: unknown): EvaluationScores | null {
  if (!isRecord(value)) return null;
  const scores = {
    taskCompletion: readScore(value.taskCompletion),
    workQuality: readScore(value.workQuality),
    responsibility: readScore(value.responsibility),
    teamwork: readScore(value.teamwork),
    researchAbility: readScore(value.researchAbility),
    proactiveness: readScore(value.proactiveness),
  };
  return scoreKeys.every((key) => scores[key] >= 1 && scores[key] <= 5) ? scores : null;
}

function normalizeEvaluation(value: unknown): MemberEvaluation | null {
  if (!isRecord(value)) return null;
  const scores = normalizeScores(value.scores);
  if (!scores) return null;
  const id = readString(value.id);
  const projectId = readString(value.projectId);
  const memberId = readString(value.memberId);
  const evaluatorId = readString(value.evaluatorId);
  const periodLabel = readString(value.periodLabel);
  const evaluationDate = readString(value.evaluationDate);
  if (!id || !projectId || !memberId || !evaluatorId || !periodLabel || !evaluationDate) {
    return null;
  }
  return {
    id,
    projectId,
    memberId,
    evaluatorId,
    periodLabel,
    evaluationDate,
    scores,
    overallScore: calculateOverallScore(scores),
    strengths: readString(value.strengths),
    weaknesses: readString(value.weaknesses),
    suggestedImprovements: readString(value.suggestedImprovements),
    technicalSkillsObserved: readString(value.technicalSkillsObserved),
    nextLearningDirection: readString(value.nextLearningDirection),
    generalFeedback: readString(value.generalFeedback),
    createdAt: readString(value.createdAt),
    updatedAt: readString(value.updatedAt),
  };
}

function load(): MemberEvaluation[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => normalizeEvaluation(item))
      .filter((item): item is MemberEvaluation => !!item);
  } catch {
    return [];
  }
}

function save(list: MemberEvaluation[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    return true;
  } catch {
    return false;
  }
}

type Sub = (list: MemberEvaluation[]) => void;
const subs = new Set<Sub>();
let cache: MemberEvaluation[] | null = null;

function getAll() {
  if (!cache) cache = load();
  return cache;
}

function setAll(next: MemberEvaluation[]) {
  const previous = cache;
  cache = next;
  const ok = save(next);
  if (!ok) cache = previous;
  subs.forEach((sub) => sub(cache ?? []));
  return ok;
}

function makeId() {
  return `eval_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function periodKey(value: string) {
  return value.trim().toLowerCase().replace(/\s+/g, " ");
}

function cleanDraft(draft: EvaluationDraft): EvaluationDraft {
  return {
    memberId: draft.memberId,
    periodLabel: draft.periodLabel.trim(),
    evaluationDate: draft.evaluationDate,
    scores: draft.scores,
    strengths: draft.strengths.trim(),
    weaknesses: draft.weaknesses.trim(),
    suggestedImprovements: draft.suggestedImprovements.trim(),
    technicalSkillsObserved: draft.technicalSkillsObserved.trim(),
    nextLearningDirection: draft.nextLearningDirection.trim(),
    generalFeedback: draft.generalFeedback.trim(),
  };
}

export function findPeriodEvaluation(
  evaluations: MemberEvaluation[],
  projectId: string,
  memberId: string,
  periodLabel: string,
) {
  const key = periodKey(periodLabel);
  return evaluations.find(
    (item) =>
      item.projectId === projectId &&
      item.memberId === memberId &&
      periodKey(item.periodLabel) === key,
  );
}

export function useEvaluations() {
  const [list, setList] = useState<MemberEvaluation[]>(() => getAll());

  useEffect(() => {
    const cb: Sub = (next) => setList(next);
    subs.add(cb);
    return () => {
      subs.delete(cb);
    };
  }, []);

  const upsertEvaluation = useCallback(
    (
      projectId: string,
      evaluatorId: string,
      draft: EvaluationDraft,
      explicitId?: string,
    ): Result => {
      const cleaned = cleanDraft(draft);
      const now = new Date().toISOString();
      const existing =
        (explicitId ? getAll().find((item) => item.id === explicitId) : undefined) ??
        findPeriodEvaluation(getAll(), projectId, cleaned.memberId, cleaned.periodLabel);
      const nextEvaluation: MemberEvaluation = {
        id: existing?.id ?? makeId(),
        projectId,
        evaluatorId,
        memberId: cleaned.memberId,
        periodLabel: cleaned.periodLabel,
        evaluationDate: cleaned.evaluationDate,
        scores: cleaned.scores,
        overallScore: calculateOverallScore(cleaned.scores),
        strengths: cleaned.strengths,
        weaknesses: cleaned.weaknesses,
        suggestedImprovements: cleaned.suggestedImprovements,
        technicalSkillsObserved: cleaned.technicalSkillsObserved,
        nextLearningDirection: cleaned.nextLearningDirection,
        generalFeedback: cleaned.generalFeedback,
        createdAt: existing?.createdAt ?? now,
        updatedAt: now,
      };
      const next = existing
        ? getAll().map((item) => (item.id === existing.id ? nextEvaluation : item))
        : [nextEvaluation, ...getAll()];
      const ok = setAll(next);
      return ok
        ? { ok: true, value: nextEvaluation }
        : { ok: false, error: "Evaluation could not be saved." };
    },
    [],
  );

  return { evaluations: list, upsertEvaluation };
}
