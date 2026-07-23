// Mock data for Admin Member Management
// TODO: Replace with real API calls to /api/admin/members when DB is connected

export type MemberActivityStatus = "ACTIVE" | "INACTIVE" | "ALUMNI";
export type MemberAccountStatus = "ACTIVE" | "LOCKED";

export type AdminResearchField = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string | null;
};

export type AdminMemberProject = {
  projectId: string;
  projectName: string;
  projectCode: string;
  projectRole: string;
  memberStatus: string;
  joinedAt: string | null;
};

export type AdminMemberEvaluation = {
  id: string;
  projectId: string;
  projectName: string;
  evaluationPeriod: string;
  overallScore: number | null;
  comment: string | null;
  evaluatedAt: string | null;
};

export type AdminMemberProfile = {
  studentCode: string | null;
  phone: string | null;
  personalEmail: string | null;
  bio: string | null;
  specialization: string | null;
  joinedAt: string | null;
  activityStatus: MemberActivityStatus | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  portfolioUrl: string | null;
};

export type AdminMemberSummary = {
  id: string;
  username: string;
  fullName: string;
  email: string;
  avatarFileId: string | null;
  accountStatus: MemberAccountStatus;
  activityStatus: MemberActivityStatus | null;
  roles: string[];
  joinedAt: string | null;
};

export type AdminMemberDetail = AdminMemberSummary & {
  profile: AdminMemberProfile | null;
  researchFields: AdminResearchField[];
  projectCount: number;
  evaluationCount: number;
};

const seedDate = "2025-09-01T00:00:00.000Z";

export const mockMembers: AdminMemberSummary[] = [
  {
    id: "m_001",
    username: "linh.pham",
    fullName: "Linh Pham",
    email: "linh@smart.lab",
    avatarFileId: null,
    accountStatus: "ACTIVE",
    activityStatus: "ACTIVE",
    roles: ["MEMBER"],
    joinedAt: seedDate,
  },
  {
    id: "m_002",
    username: "kenji.alvarado",
    fullName: "Kenji Alvarado",
    email: "kenji@smart.lab",
    avatarFileId: null,
    accountStatus: "ACTIVE",
    activityStatus: "ACTIVE",
    roles: ["MEMBER", "LEADER"],
    joinedAt: "2024-03-15T00:00:00.000Z",
  },
  {
    id: "m_003",
    username: "nikolai.weiss",
    fullName: "Nikolai Weiss",
    email: "nikolai@smart.lab",
    avatarFileId: null,
    accountStatus: "ACTIVE",
    activityStatus: "INACTIVE",
    roles: ["MEMBER"],
    joinedAt: "2024-08-01T00:00:00.000Z",
  },
  {
    id: "m_004",
    username: "amara.osei",
    fullName: "Amara Osei",
    email: "amara@smart.lab",
    avatarFileId: null,
    accountStatus: "ACTIVE",
    activityStatus: "ACTIVE",
    roles: ["ADMIN", "LEADER"],
    joinedAt: "2023-01-10T00:00:00.000Z",
  },
  {
    id: "m_005",
    username: "minh.tran",
    fullName: "Dr. Minh Tran",
    email: "tran@smart.lab",
    avatarFileId: null,
    accountStatus: "ACTIVE",
    activityStatus: "ACTIVE",
    roles: ["LEADER", "MEMBER"],
    joinedAt: "2022-06-01T00:00:00.000Z",
  },
  {
    id: "m_006",
    username: "sofia.reyes",
    fullName: "Sofia Reyes",
    email: "sofia@smart.lab",
    avatarFileId: null,
    accountStatus: "LOCKED",
    activityStatus: "ALUMNI",
    roles: ["MEMBER"],
    joinedAt: "2023-09-01T00:00:00.000Z",
  },
];

export const mockMemberDetails: Record<string, AdminMemberDetail> = {
  m_001: {
    ...mockMembers[0],
    profile: {
      studentCode: "SE2021001",
      phone: "0901234567",
      personalEmail: "linh.pham.personal@gmail.com",
      bio: "PhD researcher focusing on event-based SLAM and tactile sensing for mobile robots.",
      specialization: "Robotics & Perception",
      joinedAt: seedDate,
      activityStatus: "ACTIVE",
      githubUrl: "https://github.com/linhpham",
      linkedinUrl: "https://linkedin.com/in/linhpham",
      portfolioUrl: null,
    },
    researchFields: [
      {
        id: "rf_001",
        code: "ROBOTICS",
        name: "Robotics",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
      {
        id: "rf_003",
        code: "AI",
        name: "Artificial Intelligence",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
    ],
    projectCount: 2,
    evaluationCount: 3,
  },
  m_002: {
    ...mockMembers[1],
    profile: {
      studentCode: "SE2022004",
      phone: "0912345678",
      personalEmail: null,
      bio: "MSc researcher working on clinical language models and evaluation frameworks.",
      specialization: "AI & NLP",
      joinedAt: "2024-03-15T00:00:00.000Z",
      activityStatus: "ACTIVE",
      githubUrl: null,
      linkedinUrl: "https://linkedin.com/in/kenjialvarado",
      portfolioUrl: null,
    },
    researchFields: [
      {
        id: "rf_003",
        code: "AI",
        name: "Artificial Intelligence",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
    ],
    projectCount: 1,
    evaluationCount: 1,
  },
  m_003: {
    ...mockMembers[2],
    profile: null,
    researchFields: [],
    projectCount: 0,
    evaluationCount: 0,
  },
  m_004: {
    ...mockMembers[3],
    profile: {
      studentCode: null,
      phone: "0923456789",
      personalEmail: null,
      bio: "Research Engineer specializing in static analysis and program repair.",
      specialization: "Software Engineering",
      joinedAt: "2023-01-10T00:00:00.000Z",
      activityStatus: "ACTIVE",
      githubUrl: "https://github.com/amaraosei",
      linkedinUrl: null,
      portfolioUrl: null,
    },
    researchFields: [
      {
        id: "rf_002",
        code: "SE",
        name: "Software Engineering",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
    ],
    projectCount: 3,
    evaluationCount: 0,
  },
  m_005: {
    ...mockMembers[4],
    profile: {
      studentCode: null,
      phone: "0934567890",
      personalEmail: null,
      bio: "PhD lead researcher on event-based perception for warehouse robots. Atlas project lead.",
      specialization: "Robotics",
      joinedAt: "2022-06-01T00:00:00.000Z",
      activityStatus: "ACTIVE",
      githubUrl: null,
      linkedinUrl: null,
      portfolioUrl: null,
    },
    researchFields: [
      {
        id: "rf_001",
        code: "ROBOTICS",
        name: "Robotics",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
    ],
    projectCount: 2,
    evaluationCount: 2,
  },
  m_006: {
    ...mockMembers[5],
    profile: {
      studentCode: "SE2023007",
      phone: null,
      personalEmail: "sofia.reyes@gmail.com",
      bio: "Alumni member, graduated Spring 2025.",
      specialization: "AI",
      joinedAt: "2023-09-01T00:00:00.000Z",
      activityStatus: "ALUMNI",
      githubUrl: null,
      linkedinUrl: null,
      portfolioUrl: null,
    },
    researchFields: [
      {
        id: "rf_003",
        code: "AI",
        name: "Artificial Intelligence",
        description: null,
        status: "ACTIVE",
        createdAt: "2022-01-01T00:00:00.000Z",
      },
    ],
    projectCount: 1,
    evaluationCount: 2,
  },
};

export const mockMemberProjects: Record<string, AdminMemberProject[]> = {
  m_001: [
    {
      projectId: "p_001",
      projectName: "Atlas — Long-horizon perception",
      projectCode: "NL-24-07",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "ACTIVE",
      joinedAt: "2024-01-15T00:00:00.000Z",
    },
    {
      projectId: "p_003",
      projectName: "Meridian — Clinical NLP",
      projectCode: "NL-23-11",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "ACTIVE",
      joinedAt: "2024-05-01T00:00:00.000Z",
    },
  ],
  m_002: [
    {
      projectId: "p_003",
      projectName: "Meridian — Clinical NLP",
      projectCode: "NL-23-11",
      projectRole: "PROJECT_LEADER",
      memberStatus: "ACTIVE",
      joinedAt: "2024-03-15T00:00:00.000Z",
    },
  ],
  m_003: [],
  m_004: [
    {
      projectId: "p_002",
      projectName: "Helix — Program repair",
      projectCode: "NL-25-02",
      projectRole: "PROJECT_LEADER",
      memberStatus: "ACTIVE",
      joinedAt: "2023-01-10T00:00:00.000Z",
    },
    {
      projectId: "p_003",
      projectName: "Meridian — Clinical NLP",
      projectCode: "NL-23-11",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "ACTIVE",
      joinedAt: "2023-05-01T00:00:00.000Z",
    },
    {
      projectId: "p_001",
      projectName: "Atlas — Long-horizon perception",
      projectCode: "NL-24-07",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "ACTIVE",
      joinedAt: "2023-09-01T00:00:00.000Z",
    },
  ],
  m_005: [
    {
      projectId: "p_001",
      projectName: "Atlas — Long-horizon perception",
      projectCode: "NL-24-07",
      projectRole: "PROJECT_LEADER",
      memberStatus: "ACTIVE",
      joinedAt: "2022-06-01T00:00:00.000Z",
    },
    {
      projectId: "p_002",
      projectName: "Helix — Program repair",
      projectCode: "NL-25-02",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "ACTIVE",
      joinedAt: "2023-03-01T00:00:00.000Z",
    },
  ],
  m_006: [
    {
      projectId: "p_003",
      projectName: "Meridian — Clinical NLP",
      projectCode: "NL-23-11",
      projectRole: "PROJECT_MEMBER",
      memberStatus: "INACTIVE",
      joinedAt: "2023-09-01T00:00:00.000Z",
    },
  ],
};

export const mockMemberEvaluations: Record<string, AdminMemberEvaluation[]> = {
  m_001: [
    {
      id: "e_001",
      projectId: "p_001",
      projectName: "Atlas",
      evaluationPeriod: "Q4 2024",
      overallScore: 8.5,
      comment: "Excellent contributions to the perception stack.",
      evaluatedAt: "2025-01-10T00:00:00.000Z",
    },
    {
      id: "e_002",
      projectId: "p_001",
      projectName: "Atlas",
      evaluationPeriod: "Q1 2025",
      overallScore: 9.0,
      comment: "Outstanding research output.",
      evaluatedAt: "2025-04-05T00:00:00.000Z",
    },
    {
      id: "e_003",
      projectId: "p_003",
      projectName: "Meridian",
      evaluationPeriod: "Q1 2025",
      overallScore: 8.0,
      comment: "Good work on annotation pipeline.",
      evaluatedAt: "2025-04-10T00:00:00.000Z",
    },
  ],
  m_002: [
    {
      id: "e_004",
      projectId: "p_003",
      projectName: "Meridian",
      evaluationPeriod: "Q4 2024",
      overallScore: 7.5,
      comment: "Solid progress on the evaluation framework.",
      evaluatedAt: "2025-01-12T00:00:00.000Z",
    },
  ],
  m_003: [],
  m_004: [],
  m_005: [
    {
      id: "e_005",
      projectId: "p_001",
      projectName: "Atlas",
      evaluationPeriod: "Annual 2024",
      overallScore: null,
      comment: "Project lead — peer review in progress.",
      evaluatedAt: null,
    },
    {
      id: "e_006",
      projectId: "p_002",
      projectName: "Helix",
      evaluationPeriod: "Q1 2025",
      overallScore: 9.5,
      comment: "Exceptional mentorship and technical guidance.",
      evaluatedAt: "2025-04-15T00:00:00.000Z",
    },
  ],
  m_006: [
    {
      id: "e_007",
      projectId: "p_003",
      projectName: "Meridian",
      evaluationPeriod: "Q2 2024",
      overallScore: 7.0,
      comment: "Good initial research contributions.",
      evaluatedAt: "2024-07-01T00:00:00.000Z",
    },
    {
      id: "e_008",
      projectId: "p_003",
      projectName: "Meridian",
      evaluationPeriod: "Q3 2024",
      overallScore: 6.5,
      comment: "Activity declined in second half of semester.",
      evaluatedAt: "2024-10-01T00:00:00.000Z",
    },
  ],
};

export const allResearchFields: AdminResearchField[] = [
  {
    id: "rf_001",
    code: "ROBOTICS",
    name: "Robotics",
    description: "Perception, control, and human-robot interaction for mobile platforms.",
    status: "ACTIVE",
    createdAt: "2022-01-01T00:00:00.000Z",
  },
  {
    id: "rf_002",
    code: "SE",
    name: "Software Engineering",
    description: "Programming languages, static analysis, and developer tooling.",
    status: "ACTIVE",
    createdAt: "2022-01-01T00:00:00.000Z",
  },
  {
    id: "rf_003",
    code: "AI",
    name: "Artificial Intelligence",
    description: "Foundation models, evaluation, and applied machine learning.",
    status: "ACTIVE",
    createdAt: "2022-01-01T00:00:00.000Z",
  },
  {
    id: "rf_004",
    code: "HCI",
    name: "Human-Computer Interaction",
    description: "User-centered design and interaction research.",
    status: "INACTIVE",
    createdAt: "2023-06-01T00:00:00.000Z",
  },
];

export function activityStatusLabel(status: MemberActivityStatus | null): string {
  switch (status) {
    case "ACTIVE":
      return "Active";
    case "INACTIVE":
      return "Inactive";
    case "ALUMNI":
      return "Alumni";
    default:
      return "—";
  }
}

export function activityStatusTone(
  status: MemberActivityStatus | null,
): "emerald" | "amber" | "neutral" | "violet" {
  switch (status) {
    case "ACTIVE":
      return "emerald";
    case "INACTIVE":
      return "amber";
    case "ALUMNI":
      return "violet";
    default:
      return "neutral";
  }
}

export function roleLabel(role: string): string {
  switch (role) {
    case "ADMIN":
      return "Admin";
    case "LEADER":
      return "Leader";
    case "MEMBER":
      return "Member";
    default:
      return role;
  }
}

export function roleTone(role: string): "violet" | "cyan" | "neutral" {
  switch (role) {
    case "ADMIN":
      return "violet";
    case "LEADER":
      return "cyan";
    default:
      return "neutral";
  }
}

export function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}
