// Mock data for Admin Lab & Research Field Management
// TODO: Replace with real API calls to /api/admin/lab and /api/admin/research-fields

export type AdminLabInfo = {
  id: string;
  name: string;
  code: string;
  description: string | null;
  mission: string | null;
  vision: string | null;
  contactEmail: string | null;
  websiteUrl: string | null;
  logoFileId: string | null;
  coverFileId: string | null;
  status: string;
};

let mockLabInfo: AdminLabInfo = {
  id: "lab_001",
  name: "SmartResearch Lab",
  code: "SMARTLAB",
  description:
    "A university research lab working at the intersection of artificial intelligence, robotics, and software engineering. We build systems that reason, move, and scale.",
  mission:
    "To advance the state of the art in intelligent, physical, and software systems through rigorous collaborative research.",
  vision:
    "A world where intelligent systems reliably amplify human capability in science, healthcare, and industry.",
  contactEmail: "contact@smart.lab",
  websiteUrl: "https://smart.lab",
  logoFileId: null,
  coverFileId: null,
  status: "ACTIVE",
};

export function getMockLabInfo(): AdminLabInfo {
  return { ...mockLabInfo };
}

export function updateMockLabInfo(updates: Partial<AdminLabInfo>): AdminLabInfo {
  mockLabInfo = { ...mockLabInfo, ...updates };
  return { ...mockLabInfo };
}
