# SmartLab Project Profile

## Basic Information

Project name: SmartLab

Project type: Monorepo for a university research lab management system.

Purpose: Provide a demo-oriented frontend and Spring Boot backend foundation for managing lab content, projects, roles, members, tasks, evaluations, notifications, and calendars.

Repository root: SmartLab-0.1

## Repository Structure

```text
.
├── AGENTS.md
├── backend/
│   ├── README.md
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
├── docs/
│   ├── PROJECT-PROFILE.md
│   ├── TASKS.md
│   └── codex/profiles/
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── package.json
└── scripts/
    └── backend.mjs
```

## Technology

SmartLab is a monorepo with separate frontend and backend workspaces.

- `frontend/` uses React, TypeScript, Vite, TanStack Router/Start, Tailwind CSS, and the existing shared UI components.
- `backend/` uses Java 21 and Spring Boot 4.1.
- The Maven Wrapper is inside `backend/`.
- PostgreSQL is the target database.
- Database integration is currently deferred.
- Database-independent backend work uses the `nodb` Spring profile.
- Frontend runs on port `5173`.
- Backend runs on port `8080`.
- Backend package root is `com.smartlab`.

## Backend Architecture Direction

Backend work should follow this direction unless a later approved task changes it:

```text
Controller -> Service -> Repository -> Entity
```

APIs use request DTOs and response DTOs. Do not expose entities directly as API contracts.

Current backend package scaffold includes:

- `com.smartlab.controller`
- `com.smartlab.service`
- `com.smartlab.repository`
- `com.smartlab.entity`
- `com.smartlab.dto.request`
- `com.smartlab.dto.response`
- `com.smartlab.enums`
- `com.smartlab.exception`
- `com.smartlab.mapper`
- `com.smartlab.config`
- `com.smartlab.util`

## Roles

SmartLab roles are:

- Super Admin
- Admin
- Leader
- Member

Do not invent permissions, role transitions, authorization rules, or role hierarchy changes. Follow the active task, `AGENTS.md`, and existing implementation.

## Constraints

- Do not change architecture without approval.
- Do not add, remove, replace, or upgrade dependencies without approval.
- Do not add database integration unless the task explicitly requires it.
- Do not commit real database credentials.
- Codex must not commit, push, merge, rebase, or force-push unless explicitly instructed.
- Preserve useful existing rules in `AGENTS.md`.

## Commands

Install root dependencies:

```bash
npm install
```

Install frontend dependencies:

```bash
npm --prefix frontend install
```

Run frontend locally:

```bash
npm run dev:frontend
```

Run backend without database:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw spring-boot:run
```

Run both apps from the repository root:

```bash
npm run dev
```

Build frontend:

```bash
npm run build:frontend
```

Build backend:

```bash
npm run build:backend
```

Run backend tests without database:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```

## Important Notes

- The frontend currently contains demo behavior and mock/localStorage-backed data.
- Frontend role checks are demo UX and are not production authorization.
- Public or internal data bundled into frontend code is not confidential from a security perspective.
- For backend content-core work that does not need persistence, use `SPRING_PROFILES_ACTIVE=nodb`.
- Verify actual scripts and package files before running commands.
