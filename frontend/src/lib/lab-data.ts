export const lab = {
  name: "SmartResearch Lab",
  shortName: "SmartLab",
  tagline: "Intelligent systems for the physical world",
  intro:
    "Smartis a university research lab working at the intersection of artificial intelligence, robotics, and software engineering. We build systems that reason, move, and scale — from perception models running on embedded robots to platforms that support scientific collaboration.",
  location: "Building E3 · Faculty of Information Technology",
  founded: 2017,
};

export const achievements = [
  {
    metric: "42",
    label: "Peer-reviewed publications",
    detail: "Across ICRA, NeurIPS, ICSE and 9 other venues since 2021.",
  },
  {
    metric: "17",
    label: "Active research projects",
    detail: "Spanning three focus areas and four industry partnerships.",
  },
  {
    metric: "$3.4M",
    label: "Grant funding secured",
    detail: "From national science foundations and industry sponsors.",
  },
  {
    metric: "68",
    label: "Members and alumni",
    detail: "Undergraduate researchers, MSc, PhD, and visiting scholars.",
  },
];

export const featuredProjects = [
  {
    slug: "atlas-perception",
    code: "NL-24-07",
    title: "Atlas — Long-horizon perception for mobile robots",
    summary:
      "A perception stack combining event cameras and neural fields for stable localization in unstructured environments.",
    field: "Robotics",
    status: "Active",
    year: "2024 — present",
    lead: "Dr. removed (see note)", // placeholder — will not be displayed
    members: 9,
  },
  {
    slug: "helix-code-reasoning",
    code: "NL-25-02",
    title: "Helix — Program repair with grounded LLM reasoning",
    summary:
      "Studying how retrieval-grounded language models can localize, explain, and repair defects in large Java codebases.",
    field: "Software Engineering",
    status: "Active",
    year: "2025 — present",
    lead: "",
    members: 6,
  },
  {
    slug: "meridian-clinical-nlp",
    code: "NL-23-11",
    title: "Meridian — Clinical NLP for low-resource languages",
    summary:
      "Pretraining and evaluation pipelines for medical text understanding in Vietnamese and Bahasa hospital records.",
    field: "Artificial Intelligence",
    status: "Publishing",
    year: "2023 — 2025",
    lead: "",
    members: 7,
  },
];

export const featuredPeople = [
  {
    name: "Linh Pham",
    role: "PhD Researcher · Robotics",
    focus: "Event-based SLAM, tactile sensing",
    initials: "LP",
  },
  {
    name: "Kenji Alvarado",
    role: "MSc Researcher · AI",
    focus: "Clinical language models, evaluation",
    initials: "KA",
  },
  {
    name: "Amara Osei",
    role: "Research Engineer · SE",
    focus: "Static analysis, program repair",
    initials: "AO",
  },
  {
    name: "Nikolai Weiss",
    role: "Undergraduate Researcher",
    focus: "Simulation infrastructure",
    initials: "NW",
  },
];

export const culture = [
  {
    title: "Weekly reading group",
    body: "Every Thursday we dissect one paper across the three focus areas — chalk, coffee, and honest critique.",
  },
  {
    title: "Open lab Fridays",
    body: "Undergraduates shadow ongoing projects and pair with senior researchers on small, shippable experiments.",
  },
  {
    title: "Field trips & showcase days",
    body: "Twice a semester we take active prototypes into the wild: hospitals, warehouses, and partner campuses.",
  },
];

export const researchFields = [
  {
    key: "ai",
    name: "Artificial Intelligence",
    description:
      "Foundation models, evaluation, and applied machine learning for scientific and clinical domains.",
    projects: 6,
  },
  {
    key: "robotics",
    name: "Robotics",
    description:
      "Perception, control, and human-robot interaction for mobile platforms operating in unstructured spaces.",
    projects: 5,
  },
  {
    key: "se",
    name: "Software Engineering",
    description:
      "Programming languages, static analysis, and developer tooling for large, long-lived codebases.",
    projects: 6,
  },
];

export const activities = [
  {
    title: "Spring showcase day at Building E3",
    date: "Mar 14, 2025",
    kind: "Showcase day",
    blurb:
      "Six teams presented live prototypes — from tactile grippers to a retrieval-grounded code reviewer — to 120+ students and faculty.",
  },
  {
    title: "Field study · Bach Mai Hospital",
    date: "Feb 02, 2025",
    kind: "Field trip",
    blurb:
      "The Meridian team spent a week shadowing clinicians to refine annotation guidelines for Vietnamese discharge notes.",
  },
  {
    title: "Invited talk · ICRA workshop",
    date: "Nov 21, 2024",
    kind: "Talk",
    blurb:
      "Dr. Tran gave a keynote on event-based perception for warehouse robots, drawing from two years of Atlas deployments.",
  },
  {
    title: "Open lab Friday · Undergraduates welcome",
    date: "Every Friday",
    kind: "Workshop",
    blurb:
      "A recurring low-pressure session where new students pair with senior researchers on a small, shippable experiment.",
  },
];

export const publicPosts = [
  {
    slug: "atlas-v2-release",
    title: "Atlas v2: stable localization on 40-minute warehouse runs",
    excerpt:
      "Our second-generation perception stack cut drift by 63% on the internal warehouse benchmark. Here is what changed and what remains open.",
    category: "News",
    date: "Apr 08, 2025",
    readMinutes: 6,
  },
  {
    slug: "helix-icse-2025",
    title: "Helix accepted at ICSE 2025 Research Track",
    excerpt:
      "Our paper on retrieval-grounded program repair for large Java codebases will appear in the main technical program in Lisbon.",
    category: "Publication",
    date: "Feb 27, 2025",
    readMinutes: 3,
  },
  {
    slug: "phd-openings-2025",
    title: "PhD openings for Fall 2025 — Robotics and Clinical NLP",
    excerpt:
      "We are hiring two funded PhD researchers to join the Atlas and Meridian projects. Applications close May 30.",
    category: "Announcement",
    date: "Feb 12, 2025",
    readMinutes: 4,
  },
];
