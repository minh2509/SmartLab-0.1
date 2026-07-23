# Plan: Admin Member & Lab Management Frontend
## Task: thanh-adm-027-041-frontend

---

## Component Architecture

### Trang 1: `/app/admin/members`

```
AdminMembersPage (app.admin.members.tsx)
├── Unauthorized Guard
├── PageHeader (eyebrow, title, description)
├── MiniStat x3 (Total, Active, Alumni)
├── Panel "Danh sách thành viên"
│   ├── Filter Bar
│   │   ├── SearchInput
│   │   └── ActivityStatusFilter (select)
│   ├── LoadingState (skeleton rows)
│   ├── EmptyState
│   └── MemberTable (overflow-x-auto)
│       └── MemberRow (click → open drawer)
└── MemberDetailDrawer (vaul Drawer)
    ├── DrawerHeader (avatar, name, email, status badges)
    ├── Tabs (Profile | Projects | Evaluations)
    ├── Tab: ProfileTab
    │   ├── MemberProfileForm (react-hook-form + zod)
    │   ├── ResearchFieldsEditor (multi-select chips)
    │   └── ActivityStatusSelector
    ├── Tab: ProjectsTab (read-only list)
    └── Tab: EvaluationsTab (read-only list)
```

### Trang 2: `/app/admin/lab`

```
AdminLabPage (app.admin.lab.tsx)
├── Unauthorized Guard
├── PageHeader
└── Tabs (Lab Info | Research Fields)
    ├── Tab: LabInfoTab
    │   ├── LabInfoForm (react-hook-form + zod)
    │   │   ├── name*, description, mission, vision
    │   │   ├── contactEmail, websiteUrl
    │   │   └── Submit button
    │   └── LabImagesSection
    │       ├── LogoInput (fileId text field)
    │       └── CoverInput (fileId text field)
    └── Tab: ResearchFieldsTab
        ├── Panel header + "Thêm mới" button
        ├── EmptyState / LoadingState
        ├── ResearchFieldTable
        │   └── ResearchFieldRow (code, name, status toggle, edit button)
        ├── CreateResearchFieldDialog
        └── EditResearchFieldDialog
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `src/routes/app.admin.members.tsx` | Trang quản lý thành viên |
| `src/routes/app.admin.lab.tsx` | Trang cài đặt lab & lĩnh vực |
| `src/components/app/members/MemberDetailDrawer.tsx` | Drawer chi tiết thành viên |
| `src/components/app/members/MemberProfileForm.tsx` | Form chỉnh sửa hồ sơ |
| `src/components/app/members/MemberTable.tsx` | Bảng danh sách thành viên |
| `src/components/app/lab/LabInfoForm.tsx` | Form thông tin Lab |
| `src/components/app/lab/ResearchFieldsTab.tsx` | Tab quản lý lĩnh vực nghiên cứu |
| `src/components/app/lab/ResearchFieldDialog.tsx` | Dialog tạo/sửa lĩnh vực |
| `src/lib/members-data.ts` | Mock data + types cho members |
| `src/lib/admin-lab-data.ts` | Mock data + types cho lab & research fields |

## Files to Modify

| File | Change |
|------|--------|
| `src/components/app/AppShell.tsx` | Thêm 2 nav items: "Members" và "Lab Settings" cho role admin |

---

## Type Contracts (từ backend DTOs)

### AdminMemberSummary
```typescript
type AdminMemberSummary = {
  id: string;
  username: string;
  fullName: string;
  email: string;
  avatarFileId: string | null;
  accountStatus: 'ACTIVE' | 'LOCKED';
  activityStatus: 'ACTIVE' | 'INACTIVE' | 'ALUMNI' | null;
  roles: string[];
  joinedAt: string | null;
};
```

### AdminMemberDetail
```typescript
type AdminMemberDetail = AdminMemberSummary & {
  profile: {
    studentCode: string | null;
    phone: string | null;
    personalEmail: string | null;
    bio: string | null;
    specialization: string | null;
    joinedAt: string | null;
    activityStatus: 'ACTIVE' | 'INACTIVE' | 'ALUMNI' | null;
    githubUrl: string | null;
    linkedinUrl: string | null;
    portfolioUrl: string | null;
  } | null;
  researchFields: AdminResearchField[];
  projectCount: number;
  evaluationCount: number;
};
```

### AdminResearchField
```typescript
type AdminResearchField = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string | null;
};
```

### AdminLabInfo
```typescript
type AdminLabInfo = {
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
```

---

## API Endpoints (kết nối sau khi có DB)

```
GET    /api/admin/members?page=0&size=20&keyword=
GET    /api/admin/members/{userId}
PUT    /api/admin/members/{userId}/profile
PUT    /api/admin/members/{userId}/research-fields
PATCH  /api/admin/members/{userId}/activity-status
GET    /api/admin/members/{userId}/projects
GET    /api/admin/members/{userId}/evaluations

GET    /api/admin/lab
PUT    /api/admin/lab
POST   /api/admin/lab/logo
POST   /api/admin/lab/cover

GET    /api/admin/research-fields
POST   /api/admin/research-fields
PUT    /api/admin/research-fields/{fieldId}
PATCH  /api/admin/research-fields/{fieldId}/status
```

---

## Navigation Update

Thêm vào AppShell NAV array:
```typescript
{ label: "Members", to: "/app/admin/members", icon: Users2, roles: ["admin"] },
{ label: "Lab Settings", to: "/app/admin/lab", icon: Building2, roles: ["admin"] },
```

Cập nhật type `AppNavPath` thêm 2 path mới.

---

## Responsive Strategy

| Screen | Members page | Lab page |
|--------|-------------|---------|
| Mobile (360px) | Card layout thay table | Single column form |
| Tablet (768px) | Table với ít cột | 2-col form |
| Desktop (1280px) | Full table | Full form |
| Drawer | Full screen mobile, slide-right desktop |

---

## Verification Plan

```bash
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run build
```
