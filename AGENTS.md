# Research Lab Management System — Codex Project Context

## 1. Project objective

This repository contains a polished demo web application for managing a university research lab.

The system supports:

- Public lab introduction.
- Authentication and role-aware dashboards.
- Research project management.
- Project participation requests.
- Project member management.
- Task assignment and task output submission.
- Member evaluation and technical feedback.
- Blog and content moderation.
- Website notifications.
- Lab-wide and project-specific calendars.

This is currently a demo-oriented monorepo application. The frontend still contains mock data and `localStorage` behavior in several areas, while the backend is a Spring Boot foundation that should be expanded only when the active task requires it.

Do not assume that visual functionality equals production-grade security.

### 1.1 Required senior engineering workflow

For every significant feature, bug fix, or refactor, read and follow
`docs/SENIOR-ENGINEERING-WORKFLOW.md` before changing source code.

- Treat that document as a mandatory extension of this file.
- Create the task documentation required there before implementation.
- Use relevant Superpowers skills when they are installed and available.
- Do not claim that a skill was used when it is unavailable.
- Use the documents skill only when the requested artifact is a `.docx`;
  normal Markdown task documentation remains repository-native text.
- Communicate with the user in Vietnamese unless the user requests another
  language.

---

## 2. Technical context

SmartLab is a monorepo, not a frontend-only project.

Project structure:

- `frontend/` contains the React, TypeScript, Vite, and TanStack frontend.
- `backend/` contains the Java 21 and Spring Boot 4.1 backend.
- `docs/` contains the project profile, task tracking, and Codex technology profiles.
- `scripts/` contains local development runners.

The frontend currently uses:

- React.
- TypeScript.
- Vite.
- TanStack Router/Start.
- Existing route-based page structure.
- Existing shared design system and reusable components.
- Mock authentication stored in `localStorage`.
- Mock project data stored in `localStorage`.
- Role-aware frontend navigation and route guards.

The backend currently uses:

- Java 21.
- Spring Boot 4.1.
- Maven Wrapper inside `backend/`.
- Backend package root `com.smartlab`.
- Spring Web MVC, Spring Security, Spring Data JPA, Flyway, PostgreSQL driver, and Actuator.
- `nodb` profile for database-independent development and tests.

PostgreSQL is the target database, but database integration is currently deferred.

Before modifying code, inspect:

- `docs/PROJECT-PROFILE.md`
- `docs/TASKS.md`
- Root `package.json`
- `frontend/package.json`
- `backend/pom.xml`
- `scripts/backend.mjs`
- `frontend/src/routes/`
- `frontend/src/components/`
- `frontend/src/lib/`
- `backend/src/main/java/com/smartlab/`
- Existing route configuration
- Existing authentication context
- Existing project store
- Existing shared UI primitives
- Existing styles and design tokens
- Existing backend packages, services, DTOs, controllers, repositories, and entities when backend work is in scope

Do not assume this document perfectly matches the current implementation.

The source code is the final source of truth.

If this document conflicts with actual working code, preserve the working implementation and report the discrepancy before changing architecture.

---

## 3. Product design direction

The product uses a premium university research lab visual identity.

The interface must feel:

- Editorial.
- Calm.
- Intelligent.
- Modern.
- High-end.
- Designed by a senior product designer.

Visual references include:

- High-end university research websites.
- Linear.
- Vercel.
- Arc.
- Notion.
- Apple-level spacing and typography.

### Approved visual identity

Primary colors:

- Deep navy.
- Soft white or warm ivory.
- Muted cyan and blue accents.

Secondary accents:

- Violet.
- Emerald.
- Amber.

Secondary accents must be used sparingly and meaningfully.

### Design rules

Use:

- Strong typography.
- Generous whitespace.
- Refined cards.
- Subtle borders.
- Clear hierarchy.
- Consistent spacing.
- Compact metadata chips.
- Clear project-status badges.
- Clear role badges.
- Realistic research content.

Avoid:

- Generic SaaS dashboards.
- Random gradients.
- Rainbow gradients.
- Excessive glassmorphism.
- Excessive shadows.
- Neon effects.
- Placeholder-looking content.
- Unnecessary animation.
- Dense screens without hierarchy.
- Redesigning approved screens without an explicit request.

All new screens must reuse the current design language.

Do not introduce a second independent design system.

---

## 4. Core implementation discipline

Work on one module or feature flow at a time.

Before coding, provide a short plan containing:

1. Files and components to create or modify.
2. Data structures or mock data required.
3. Permission rules involved.
4. Edge cases to handle.

Then implement only the requested feature.

Do not:

- Add unrelated functionality.
- Rename existing routes without explicit permission.
- Replace working components unnecessarily.
- Rewrite entire files when a focused change is sufficient.
- Change the global design system unless required.
- Modify unrelated flows.
- Add unrelated backend, database, or external-service work. Backend work is allowed only when required by the active task.
- Replace `localStorage` with another persistence mechanism unless requested.
- Add full CRUD for entities when only a shell or partial flow was requested.
- Invent new business rules.
- Duplicate an existing component, hook, type, or store.

Prefer extending existing abstractions over creating parallel implementations.

---

## 4.1 Backend implementation rules

Backend package root:

```text
com.smartlab
```

Backend architecture direction:

```text
Controller -> Service -> Repository -> Entity
```

Backend rules:

- Business logic belongs in services.
- Controllers handle HTTP request and response concerns.
- Repositories handle persistence.
- Entities must not be returned directly as API contracts.
- Use request DTOs and response DTOs for APIs.
- Do not bypass the service layer.
- Do not add or upgrade dependencies without approval.
- Do not change Java or Spring Boot versions without approval.
- Do not invent permissions, workflow rules, persistence models, or API contracts.
- Preserve the package structure under `com.smartlab` unless the active task explicitly changes it.

Current database state:

- PostgreSQL is the target database.
- Database integration is currently deferred.
- Database-independent tasks must use the `nodb` profile.
- Do not create entities, repositories, migrations, or datasource configuration unless the active task requires them.
- Do not commit real database credentials.

Required backend test command:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```

Required backend development command before database integration:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw spring-boot:run
```

---

## 4.2 Frontend implementation rules

- Preserve the existing React, TypeScript, Vite, and TanStack structure.
- Preserve the existing design system.
- Do not replace working mock or `localStorage` behavior unless the active task requires backend integration.
- Do not redesign approved screens without explicit instruction.
- Frontend role checks are for demo UX only and are not production authorization.
- Do not introduce a second router, state-management library, styling framework, form library, icon library, or notification library unless explicitly requested and justified.

---

## 5. Roles and permissions

The system distinguishes between:

1. System roles.
2. Project-scoped permissions.

These are not the same concept.

A user may have multiple system roles.

Example:

```ts
roles: ["admin", "leader"];
```

A project may have multiple leaders.

A Leader can manage only projects to which they are assigned.

### Super Admin / Main Admin

There is one initial main Admin account concept.

The Main Admin:

- Is the initial system administrator.
- Can later create additional Admin accounts.
- Has full system-level management access.
- Can manage users, roles, projects, posts, notifications, and lab calendar.
- Can view, edit, and delete system-level data.
- Can permanently delete posts.

Do not implement special Main Admin-only behavior unless explicitly requested.

### Admin

Multiple Admins are allowed.

Admin permissions:

- Manage users and system roles.
- Manage lab-level content.
- Create lab-wide calendar events.
- Review and moderate member posts.
- View all projects.
- Edit all projects.
- Delete projects.
- Permanently delete posts.

Admin should not replace Leader-specific project workflows unless necessary.

### Leader

Multiple Leaders are allowed.

A Leader:

- Manages only assigned projects.
- Can approve or reject join requests for assigned projects.
- Can manage project members.
- Can create and manage project tasks.
- Can create project-specific calendar events.
- Can publish project announcements according to the content workflow.
- Can evaluate project members.
- Can provide technical feedback to members in assigned projects.

A Leader must not edit or manage an unassigned project.

### Member

A Member:

- Can view public lab content.
- Can manage their own profile.
- Can view public projects.
- Can request to join projects.
- Can access internal project content only after approval.
- Can view assigned tasks.
- Can submit task outputs.
- Can create blog posts as drafts.
- Can submit drafts for review.
- Can view their own evaluations and technical feedback.

A Member:

- Cannot change their own system role.
- Cannot access internal project data without membership or approval.
- Cannot permanently delete posts.
- Cannot manage other users.

---

## 6. Global business rules

These rules must be preserved unless the user explicitly changes them.

### Roles

- Multiple Admins are allowed.
- Multiple Leaders are allowed.
- One user can have multiple roles.
- Project roles and system roles must not be treated as identical.
- Leader permissions must be scoped to assigned projects.
- A project can have multiple Leaders.

### Projects

Project data includes the following concept:

```ts
type Project = {
  id: string;
  slug: string;
  name: string;
  code: string;
  description: string;
  objective: string;
  type: "production" | "research";
  researchFields: string[];
  leaderIds: string[];
  memberIds: string[];
  startDate: string;
  expectedEndDate: string;
  status: string;
  progress: number;
  visibility: "public" | "internal";
};
```

Preserve the existing project type if it differs slightly.

Do not create a duplicate project model.

Project rules:

- Public users can see only public project information.
- Internal projects must not appear in the public project list.
- Internal project content requires authentication and project membership.
- Admin can view, edit, and delete all projects.
- Assigned Leaders can edit their projects.
- Unassigned Leaders must not manage other projects.
- Members have read-only access to projects they belong to.
- Progress must remain between `0` and `100`.
- Expected end date must not be before the start date.
- A project should have at least one Leader unless the current implementation explicitly allows otherwise.
- Project status must always be visible.
- Research fields must be connected to projects.

### Posts

Post statuses:

```ts
type PostStatus =
  "draft" | "pending_review" | "needs_revision" | "approved" | "published" | "rejected";
```

Rules:

- Member posts must start as Draft.
- A Draft must be submitted before review.
- Only reviewed and approved content may be published.
- Only Admin can permanently delete a post.
- Do not skip the Draft state.
- Do not merge Draft and Pending Review into one state.

### Calendars

- Admin creates lab-wide events.
- Leader creates project-specific events only for assigned projects.
- Member cannot create lab-wide events.
- Project events must remain scoped to their project.

### Notifications

- Users may delete their own notifications.
- Notifications older than 30 days are expired.
- Expired notifications should be removable.
- Do not implement unrelated real-time notification infrastructure unless requested.

---

## 7. Implemented modules

The following modules were previously generated by Lovable.

Codex must verify all files and routes before modifying them.

### 7.1 Design system and public intro page

Expected public route:

```text
/
```

Expected content:

- Public hero section.
- General lab introduction.
- Achievements.
- Featured projects.
- Featured people or members.
- Activity highlights.
- Research fields.
- Latest public posts or news.
- Login or projects call-to-action.
- Public footer.

Research fields:

- Artificial Intelligence.
- Robotics.
- Software Engineering.

Restrictions:

- No lecturer or teacher information.
- No internal lab information for public users.
- Internal content must require authentication.

Expected files may include:

```text
frontend/src/styles.css
frontend/src/routes/index.tsx
frontend/src/components/site/
frontend/src/lib/lab-data.ts
```

Expected reusable components may include:

```text
SiteHeader
SiteFooter
SectionHeading
Chip
StatCard
ProjectCard
PersonCard
FieldCard
Card
```

Do not redesign the public homepage unless explicitly requested.

### 7.2 Authentication and role-aware dashboards

Expected routes:

```text
/auth
/app
/app/dashboard
```

Expected behavior:

- `/auth` is public.
- `/app/*` requires authentication.
- Unauthenticated access to `/app/*` redirects to `/auth`.
- Authenticated users visiting `/auth` redirect to `/app/dashboard`.
- Dashboard content changes according to the active role.
- Multi-role users can switch active roles.
- Active role is persisted.
- Sign-out clears the authentication session.

Expected demo accounts:

```text
admin@smart.lab
roles: Admin

amara@smart.lab
roles: Admin, Leader

tran@smart.lab
roles: Leader, Member

linh@smart.lab
roles: Member
```

Expected demo password:

```text
smart2026
```

These credentials are demo-only.

Do not treat them as production credentials.

Expected files may include:

```text
frontend/src/lib/auth.tsx
frontend/src/lib/dashboard-data.ts
frontend/src/components/app/AppShell.tsx
frontend/src/components/app/RoleSwitcher.tsx
frontend/src/components/app/StatTile.tsx
frontend/src/components/app/QueueRow.tsx
frontend/src/components/app/EmptyState.tsx
```

Current authentication is expected to be mock authentication based on `localStorage`.

Frontend role checks are for demo UX only and are not secure backend authorization.

Do not introduce a real authentication provider unless explicitly requested.

### 7.3 Project management

Expected routes:

```text
/projects
/projects/$slug
/app/projects
/app/projects/$slug
```

Expected route behavior:

#### `/projects`

- Public.
- Lists only public projects.
- Supports project filtering if already implemented.
- Must have loading, empty, and error states where applicable.

#### `/projects/$slug`

- Public project details for public projects.
- Public users see only public information.
- Internal projects require authentication and membership.
- Unauthorized users see an access-required state, not internal data.

#### `/app/projects`

- Admin sees all projects.
- Assigned Leader sees assigned projects.
- Member sees projects they belong to.
- Admin can edit and delete.
- Assigned Leader can edit but cannot delete.
- Member is read-only.

#### `/app/projects/$slug`

- Admin can manage any project.
- Assigned Leader can manage the project.
- Member can view their project.
- Other users must not access restricted internal information.

Expected files may include:

```text
frontend/src/lib/projects-data.ts
frontend/src/components/app/projects/ProjectTable.tsx
frontend/src/components/app/projects/ProjectEditDialog.tsx
frontend/src/components/app/projects/DeleteProjectDialog.tsx
frontend/src/components/app/projects/ProjectDetailPanel.tsx
frontend/src/routes/projects.tsx
frontend/src/routes/projects_.$slug.tsx
frontend/src/routes/app.projects.tsx
frontend/src/routes/app.projects_.$slug.tsx
```

The exact route file naming depends on the router.

Inspect the current routing convention before adding files.

Expected project persistence:

- `localStorage`.
- Seeded realistic project data.
- Demo reset functionality may exist for Admin.

Do not replace the existing store unless requested.

---

## 8. Route and navigation rules

Preserve current routes.

Do not create competing routes for the same feature.

Before creating a route:

1. Search the codebase for existing route names.
2. Inspect the router configuration.
3. Inspect existing sidebar and header navigation.
4. Reuse existing route helpers or link components.
5. Confirm route protection rules.

When adding a new protected page:

- Add it inside the existing app shell.
- Reuse the existing authentication guard.
- Filter navigation by the active role.
- Do not rely only on hiding the sidebar item.
- The page itself must validate permissions.

Navigation visibility is not authorization.

---

## 9. Data and persistence rules

Current data may be mock data stored in:

- Static TypeScript arrays.
- React context.
- `localStorage`-backed stores.

Before adding data:

- Search for an existing type.
- Search for an existing store.
- Search for existing helper functions.
- Search for `localStorage` keys.
- Reuse current IDs and user references.
- Avoid duplicating the same entity in multiple files.

Do not silently migrate existing `localStorage` data.

If the data shape must change:

- Add safe defaults.
- Handle old stored records.
- Prevent the page from crashing due to missing fields.
- Explain any compatibility implications.

Do not expose secrets in frontend code.

Do not put database passwords, API secrets, private tokens, or service-role keys in Vite environment variables.

---

## 10. Required UI states

For important screens and actions, handle states when applicable:

- Loading.
- Empty.
- Error.
- Success.
- Disabled.
- Pending.
- Approved.
- Rejected.
- Needs revision.
- Unauthorized.
- Not found.

Do not add every state to every page artificially.

Only add states relevant to the requested feature.

Avoid native `alert()` and `confirm()` unless the current project already standardizes them.

Use existing dialog, toast, status badge, error message, and empty-state components.

Forms must include:

- Clear labels.
- Inline validation.
- Disabled submission while invalid or submitting.
- A visible pending state.
- Clear success or error feedback.
- Keyboard accessibility.
- Reasonable focus management for dialogs.

---

## 11. Responsive design

Every new screen must work at approximately:

```text
375px
768px
1280px
```

Responsive rules:

- Tables may scroll horizontally on small screens.
- Avoid hiding critical information without an alternative.
- Dialogs must fit mobile screens.
- Forms should become single-column when necessary.
- Navigation should follow the existing mobile pattern.
- Do not invent a second mobile navigation implementation.
- Avoid fixed widths that overflow small devices.

---

## 12. Accessibility

Preserve or improve:

- Semantic headings.
- Form labels.
- Keyboard navigation.
- Visible focus states.
- Sufficient color contrast.
- Accessible button names.
- Dialog title and description.
- `aria` attributes where required.
- Reduced-motion compatibility where applicable.

Do not use color alone to communicate status.

---

## 13. Code quality rules

Use TypeScript and Java properly in their respective workspaces.

In frontend TypeScript, avoid:

```ts
any
as any
// @ts-ignore
```

unless there is a documented and unavoidable reason.

Prefer:

- Existing domain types.
- Narrow unions.
- Type guards.
- Reusable helpers.
- Small focused components.
- Existing shared components.
- Clear naming.
- Minimal file changes.

Do not over-engineer.

Do not introduce:

- A new state-management library.
- A new form library.
- A new styling framework.
- A new router.
- A new icon library.
- A new notification library.

unless explicitly requested and justified.

Keep one clear source of truth for:

- Authentication.
- Active role.
- Projects.
- Users.
- Shared status definitions.

In backend Java:

- Keep business rules in services.
- Keep HTTP concerns in controllers.
- Keep persistence concerns in repositories.
- Use explicit request and response DTOs at API boundaries.
- Avoid exposing entities directly to API consumers.
- Avoid broad exception swallowing.
- Avoid adding Lombok usage, mappers, validation patterns, or helper libraries beyond what already exists unless the active task requires it and approval is given.

---

## 14. Mandatory workflow for each task

### Step 1: Inspect

Before editing:

- Read this file.
- Read `docs/PROJECT-PROFILE.md`.
- Read `docs/TASKS.md`.
- Read the feature request and task-related requirements.
- Check the current Git branch.
- Inspect related existing files.
- Search for duplicate concepts.
- Check the current Git diff.
- Identify existing routes, stores, components, and styles.
- Identify existing backend packages, DTOs, services, repositories, entities, and configuration when backend work is in scope.
- Report the expected files to change.

### Step 2: Plan

Provide a short implementation plan containing:

- Files to create or modify.
- Components to reuse.
- Data structures involved.
- Permission checks.
- Edge cases.
- Backend layers, DTOs, persistence, or `nodb` impact when backend work is in scope.

Do not start with a broad rewrite.

### Step 3: Implement

Implement only the requested module.

Preserve unrelated behavior.

Keep changes focused and reviewable.

### Step 4: Verify

Use the scripts that actually exist in `package.json`.

Run required tests or builds for the active task.

For backend work that does not require database integration, use:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```

For frontend work, inspect available scripts and run relevant commands, for example:

```bash
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run build
```

Do not claim a command passed unless it was actually executed successfully.

If no `typecheck` script exists, use the correct project-specific command or report that the script is absent.

Before claiming completion:

1. Run the required tests or build.
2. Review the diff.
3. Run `git diff --check`.
4. Confirm that no unrelated files changed.
5. Update task status using real evidence.

Task statuses:

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `READY_FOR_REVIEW`
- `DONE`

Task status rules:

- Use `READY_FOR_REVIEW` only after build and tests pass.
- Use `DONE` only after the Pull Request is merged.
- Never invent test results, commit hashes, or Pull Request URLs.

### Step 5: Report

After implementation, provide:

1. What changed.
2. Files created or modified.
3. Routes added or updated.
4. Role access and permission behavior.
5. Validation and edge cases handled.
6. Commands executed and their results.
7. Manual test checklist.
8. Assumptions or incomplete backend dependencies.

Git safety:

- Codex must not automatically commit.
- Codex must not automatically push.
- Codex must not automatically merge.
- Codex must not automatically rebase.
- Codex must not automatically force-push.
- Codex must not automatically rewrite Git history.
- Codex must not use `git add .`.
- After completing a task, Codex must provide exact Git commands using specific file paths for the user to review and run manually.

---

## 15. Known risks and facts that must be verified

The following points may be inconsistent with the real code and must be checked before future development.

### Public homepage

The original homepage requirements included achievement metrics.

A later summary described this order:

```text
Hero
Featured projects
Research fields
Featured people
Activity
Latest posts
CTA
Footer
```

This may mean the achievement section was removed or omitted from the summary.

Do not redesign the homepage automatically.

Only report the inconsistency unless the current task explicitly asks to fix it.

### Mock authentication

Authentication currently appears to rely on `localStorage`.

Consequences:

- It is not secure.
- Users may modify role data in browser storage.
- Route guards are frontend-only.
- It is acceptable for the current demo stage.
- It must not be presented as production authentication.

### Public and internal project data

If internal project records are bundled into frontend mock data, hiding them in the UI does not provide real confidentiality.

Client-side filtering only prevents normal UI access.

It does not secure sensitive data.

Do not perform a backend migration unless explicitly requested, but clearly mention this limitation when relevant.

### Role switching

The active-role switcher is a demo UX mechanism.

Switching the active role must not grant a role the user does not already possess.

Validate that:

```ts
user.roles.includes(activeRole);
```

before rendering role-specific access.

### Project authorization

Do not rely on conditions such as:

```ts
activeRole === "leader";
```

alone.

Leader authorization must also verify:

```ts
project.leaderIds.includes(currentUser.id);
```

### Admin and Leader overlap

A user may be both Admin and Leader.

Active-role UI may change the dashboard experience, but permissions must follow the agreed application behavior.

Before implementing a sensitive action, verify whether permission is based on:

- All roles possessed by the user, or
- The currently active role.

Do not invent the answer silently.

Follow the existing implementation and report ambiguity when it materially affects access.

---

## 16. Final rule

The primary objective is not to generate the largest amount of code.

The objective is to produce a focused, correct, polished, demo-ready feature that integrates cleanly with the existing Research Lab Management System.
