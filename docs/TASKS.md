# SmartLab Tasks

## Status Values

- `NOT_STARTED`: Work has not started.
- `IN_PROGRESS`: Work is currently being implemented.
- `BLOCKED`: Work cannot continue because of a specific blocker.
- `READY_FOR_REVIEW`: Implementation is complete and build/tests pass.
- `DONE`: The Pull Request has been merged.

## Task Rules

- Use `READY_FOR_REVIEW` only after build and tests pass.
- Use `DONE` only after the Pull Request is merged.
- Never invent test counts, commit hashes, or Pull Request URLs.
- Keep scope, verification, and evidence aligned with actual work.
- Do not mark a task complete based only on code generation.

## ADM-000: Bootstrap Spring Boot Backend

- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/backend-bootstrap`
- Test result: 1 passed, 0 failures, 0 errors
- Notes: Backend merged into main; `nodb` profile and actuator health verified.

## ADM-007: Content Core Services

- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-content-core`
- Test result: 30 tests run, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS
- Notes: Implemented `PostStatus`, slug generation/unique suffix logic, post workflow transition validation, and unit tests; verified with `SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test`.
- Notes: Merges into main via PR #6; 30 tests passed, 0 failures, 0 errors, 0 skipped.
### Scope

- `PostStatus`
- `SlugService`
- `PostWorkflowService`
- `InvalidPostTransitionException`
- Unit tests

### Out Of Scope

- Entity
- Repository
- Controller
- API endpoints
- Database migrations
- Authentication integration
- Frontend changes

### Slug Acceptance Criteria

- Reject null and blank input.
- Return lowercase output.
- Remove Vietnamese diacritics.
- Convert `đ` and `Đ` to `d`.
- Replace groups of spaces and special characters with one hyphen.
- Remove leading and trailing hyphens.
- Reject a result that becomes empty.
- Support duplicate suffixes beginning with `-2`.

### Workflow Statuses

- `DRAFT`
- `PENDING_REVIEW`
- `NEEDS_REVISION`
- `APPROVED`
- `PUBLISHED`
- `REJECTED`

### Allowed Transitions

- `DRAFT` -> `PENDING_REVIEW`
- `PENDING_REVIEW` -> `APPROVED`
- `PENDING_REVIEW` -> `NEEDS_REVISION`
- `PENDING_REVIEW` -> `REJECTED`
- `NEEDS_REVISION` -> `PENDING_REVIEW`
- `APPROVED` -> `PUBLISHED`
- `PUBLISHED` -> `APPROVED`

### Workflow Rules

- `PUBLISHED` -> `APPROVED` represents unpublishing.
- `REJECTED` is terminal.
- Null statuses are invalid.
- Same-status transitions are invalid.
- Invalid transitions throw `InvalidPostTransitionException`.

### Required Test Command

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```

## DB-001: Review PostgreSQL Source Schema

- Name: Review PostgreSQL source schema
- Assignee: Minh
- Status: `READY_FOR_REVIEW`
- Progress: 100%
- Branch: `chore/db-schema-review`
- Test result: Not applicable; documentation review only
- Scope: Schema review and migration planning only
- Notes: Completed immutable-source review of `docs/database/lab-source.sql`; conclusion is `NEEDS_REVISION` before migration preparation.

## DB-002: Prepare PostgreSQL Migration Plan

- Name: Prepare the PostgreSQL migration plan and schema cleanup specification
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `chore/db-migration-plan`
- Test result: Not applicable; documentation planning only
- Scope: Migration planning and schema cleanup specification only
- Notes: Created `docs/database/DB-002-migration-plan.md` using approved owner decisions; no migration SQL files were created.
- Notes: PR was merged into `main`.

## DB-003A: Configure Local PostgreSQL Datasource

- Name: Configure local PostgreSQL datasource
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `chore/db-local-config`
- Test result: `30 tests run, 0 failures, 0 errors, 0 skipped; local PostgreSQL connection and actuator health verified`
- Scope: Spring datasource profile, environment example, and run documentation only
- Notes: PR #10 merged into `main`; local datasource, Hikari, Flyway, JPA, and actuator health verified.

## DB-003B: Implement Flyway V1-V4 Core Migrations

- Name: Implement Flyway V1-V4 core migrations
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/db-core-migrations`
- Test result: `30 tests run, 0 failures, 0 errors, 0 skipped; Flyway V1-V4 applied successfully against an empty PostgreSQL 18.4 database`
- Scope: PostgreSQL extension, core identity, file/profile/catalog tables, and delayed file foreign keys
- Notes: PR #11 merged into `main`; verified four successful Flyway migrations, 13 tables including `flyway_schema_history`, `pgcrypto`, `set_updated_at()`, all three delayed file foreign keys, and actuator health.

## DB-003C: Implement Flyway V5-V9 Feature Migrations

- Name: Implement Flyway V5-V9 feature migrations
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/db-feature-migrations`
- Test result: `30 tests run, 0 failures, 0 errors, 0 skipped; Flyway V1-V9 applied successfully against an empty PostgreSQL 18.4 database`
- Scope: Projects, membership, tasks, evaluations, content, notifications, audit, indexes, and stable reference seeds
- Notes: Verified nine successful migrations, schema version v9, 30 tables, stable reference seeds, post moderation constraints, selected indexes, no deferred document/event tables, and `/actuator/health` returning HTTP 200.
- Notes: PR #12 was merged into `main`.

## BE-001: Implement core identity JPA persistence

- Name: Implement core identity JPA persistence
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-core-identity-persistence`
- Test result: `39 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, seven JPA repositories, application startup, and actuator health verified`
- Notes: PR #13 merged into `main`; PostgreSQL persistence verification completed successfully.

## BE-002: Implement member profile and catalog persistence

- Name: Implement member profile and catalog persistence
- Assignee: Minh
- Status: `READY_FOR_REVIEW`
- Progress: 100%
- Branch: `feature/minh-profile-catalog-persistence`
- Test result: `48 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twelve JPA repositories, application startup, and actuator health verified`
- Scope: Member profile, research field, post category, evaluation criteria entities, repositories, enums, and mapping tests
- Notes: Verified against PostgreSQL 18.4 with Flyway schema version 9; Hibernate mappings initialized successfully, twelve repository interfaces were detected, and `/actuator/health` returned HTTP 200.
