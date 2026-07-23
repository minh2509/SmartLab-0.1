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

## ADM-055: Admin Post List and Search API

- Name: List and search posts for administrators
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-admin-post-query`
- Endpoint: `GET /api/admin/posts`
- Test result: 191 tests run, 0 failures, 0 errors; BUILD SUCCESS
- Runtime result: PostgreSQL 18.4 contract verification passed
- Notes: Implemented an ADMIN/SUPER_ADMIN-only, lab-scoped post listing API with pagination and optional keyword, status, content type, author, project, and visibility filters.
- Notes: Soft-deleted posts are excluded; ordering is `createdAt DESC, id DESC`.
- Notes: Fixed the nullable keyword PostgreSQL failure by normalizing a lowercase LIKE pattern in the service instead of applying `lower(...)` to a nullable query parameter.
- Notes: Runtime fixture was removed successfully; remaining fixture labs, users, and posts are all zero.
- Notes: PR #26 was merged into `main` with merge commit `b052b151ad5366bb42471ba3078bc34d2a105a1c`.

### Scope

- Admin post list controller
- Paginated response and post summary DTOs
- Admin post service and mapper
- Lab-scoped repository query and explicit count query
- Keyword and optional filters
- Soft-delete exclusion
- Stable pagination ordering
- Controller, service, repository, structure, and security tests

### Verification

- Targeted ADM-055 tests: PASS
- Full test suite: 191 passed, 0 failures
- `./mvnw compile`: PASS
- `git diff --check`: PASS
- PostgreSQL runtime API assertions: PASS
- Unauthorized request: HTTP 401
- ADMIN request: HTTP 200
- Invalid pagination and enum input: HTTP 400
- Cross-lab and soft-deleted posts excluded
- Runtime fixture cleanup: PASS

## ADM-056: Admin Pending Post Queue API

- Name: List posts waiting for administrator review
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-admin-pending-posts`
- Endpoint: `GET /api/admin/posts/pending`
- Dependencies: ADM-055
- Test result: 204 tests run, 0 failures, 0 errors; BUILD SUCCESS
- Runtime result: PostgreSQL 18.4 API and queue-order contract verification passed
- Scope: ADMIN/SUPER_ADMIN-only, lab-scoped, paginated pending-review queue.
- Notes: Only return non-deleted `PENDING_REVIEW` posts.
- Notes: Order by each post's latest `SUBMIT` moderation-log timestamp ascending; posts without a `SUBMIT` log remain visible and are placed last; use `post.id ASC` as the stable tie-breaker.
- Notes: Reuse the ADM-055 page and summary response contracts; do not add a migration or change the existing list endpoint contract.
- Notes: Targeted ADM-056 tests passed, including controller, service, repository-structure, and admin security coverage.
- Notes: PostgreSQL runtime verified HTTP 401, 403, and 200 behavior; latest-SUBMIT ordering, UUID tie-breaks, null submission timestamps, stable pagination, lab isolation, soft-delete exclusion, and sensitive-field omission all passed.
- Notes: Runtime fixtures and backend process were removed successfully; remaining fixture labs, users, user roles, posts, and moderation logs are all zero.
- Notes: Implementation merged through PR #30 with merge commit `15d56023e2f9a412c6f8e6f86d442dbb085df4b4`.

### Acceptance Criteria

- `GET /api/admin/posts/pending` returns HTTP 200 for an authorized ADMIN or SUPER_ADMIN.
- Results are restricted to the authenticated actor's lab.
- Cross-lab, soft-deleted, and non-`PENDING_REVIEW` posts are excluded.
- Pagination defaults to page 0 and size 20; size must be between 1 and 100.
- The latest `SUBMIT` log determines the queue timestamp when a post has been submitted more than once.
- Posts are ordered from oldest latest-submission timestamp to newest.
- Pending posts without a `SUBMIT` log remain visible after posts with valid submission timestamps.
- Equal or missing submission timestamps use `post.id ASC` for deterministic pagination.
- No JPA entity, private post content, author email, password data, or review note is exposed.
- Controller, service, repository, security, pagination, ordering, and PostgreSQL runtime behavior are verified.

### Out of Scope

- Post detail
- Approve, reject, request-revision, publish, or delete operations
- Frontend changes
- Flyway migrations
- Changes to the ADM-055 response contract

## DB-001: Review PostgreSQL Source Schema

- Name: Review PostgreSQL source schema
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `chore/db-schema-review`
- Test result: Not applicable; documentation review only
- Scope: Schema review and migration planning only
- Notes: Completed immutable-source review of `docs/database/lab-source.sql`; conclusion is `NEEDS_REVISION` before migration preparation.
- Notes: PR #8 merged into `main` with merge commit `0ebd072a691eb4929cf0ab3ec29f392cede1254e`.

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
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-profile-catalog-persistence`
- Test result: `48 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twelve JPA repositories, application startup, and actuator health verified`
- Scope: Member profile, research field, post category, evaluation criteria entities, repositories, enums, and mapping tests
- Notes: PR #14 merged into `main`; PostgreSQL persistence verification completed successfully.

## BE-003: Implement project and membership persistence

- Name: Implement project and membership persistence
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-project-membership-persistence`
- Test result: `59 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, sixteen JPA repositories, application startup, and actuator health verified`
- Scope: Project, research-field association, project member, join-request entities, enums, repositories, and mapping tests
- Notes: PR #15 merged into `main`; PostgreSQL persistence verification completed successfully.

## BE-004: Implement task and member evaluation persistence

- Name: Implement task and member evaluation persistence
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-task-evaluation-persistence`
- Test result: `73 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twenty-two JPA repositories, application startup, and actuator health verified`
- Scope: Task, assignee, report, attachment, member evaluation, evaluation detail entities, enums, repositories, and mapping tests
- Notes: PR #16 merged into `main`; PostgreSQL persistence verification completed successfully.

## BE-005: Implement content and moderation persistence

- Name: Implement content and moderation persistence
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-content-persistence`
- Test result: `85 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twenty-five JPA repositories, application startup, and actuator health verified`
- Scope: Post, moderation log, post attachment entities, enums, repositories, and mapping tests
- Notes: PR #17 merged into `main`; PostgreSQL persistence verification completed successfully.

## BE-006: Implement notification, audit, and login-history persistence

- Name: Implement notification, audit, and login-history persistence
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-notification-audit-persistence`
- Test result: `98 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twenty-nine JPA repositories, application startup, and actuator health verified`
- Scope: Notification, recipient, audit-log, and login-history entities, repositories, PostgreSQL JSON mappings, and mapping tests
- Notes: PR #18 merged into `main`; PostgreSQL persistence verification, JSONB mapping validation, application startup, and actuator health checks completed successfully.

## BE-007: Implement Admin user and role management service

- Name: Implement Admin user and role management service
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-admin-user-role-service`
- Test result: `114 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, twenty-nine JPA repositories, service bean initialization, application startup, and actuator health verified`
- Scope: Admin user lifecycle, account status, role assignment/revocation, lab-scope rules, protected administrator rules, repository support, and unit tests
- Notes: PR #19 merged into `main`; PostgreSQL and application-context verification completed successfully.

## BE-008: Implement Admin user and role management REST API

- Name: Implement Admin user and role management REST API
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-admin-user-role-api`
- Test result: `126 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL schema validation, controller bean initialization, application startup, actuator health, authentication boundary, and JSON exception handling verified`
- Scope: Admin user and role HTTP controllers, request/response DTOs, API mapper, exception handling, validation, and controller tests
- Notes: PR #20 merged into `main`; 126 tests and PostgreSQL/API verification completed successfully.

## BE-009: Implement database-backed Spring Security foundation

- Name: Implement database-backed Spring Security foundation
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-security-foundation`
- Test result: `144 tests run, 0 failures, 0 errors, 0 skipped; PostgreSQL 18.4 authentication, BCrypt compatibility, active role authorization, JSON 401/403, stateless behavior, REST CSRF policy, application startup, and actuator health verified`
- Scope: Database-backed user authentication, BCrypt password verification, SmartLab principal, active role mapping, stateless HTTP Basic, REST CSRF policy, JSON 401/403, Admin authorization rules, nodb compatibility, and security tests
- Notes: Manual verification completed with temporary ADMIN and MEMBER accounts. ADMIN access returned HTTP 200, MEMBER access returned JSON HTTP 403, incorrect credentials returned JSON HTTP 401, authenticated invalid POST returned JSON HTTP 400, no session cookie was created, and Spring Boot no longer generated a development password. JWT, login endpoints, refresh tokens, logout, CORS, login history, and frontend integration remain deferred.
- Notes: PR #21 was merged into `main`.

## BE-010: Implement JWT access-token authentication API

- Name: Implement JWT access-token authentication API
- Assignee: Minh
- Status: `DONE`
- Progress: 100%
- Branch: `feature/minh-jwt-auth-api`
- Test result: `158 tests run, 0 failures, 0 errors, 0 skipped; local PostgreSQL 18.4 application context, JWT bean graph, login API, current-user API, Bearer authentication, Admin role authorization, JSON 400/401/403 responses, HTTP Basic removal, stateless behavior, REST CSRF policy, and actuator health verified`
- Scope: Login API, current-user API, HS256 JWT issuing and validation, Bearer authentication, role claims, JSON authentication failures, stateless security, `nodb` compatibility, and tests
- Notes: PR #22 was merged into `main`.
- Notes: Manual verification completed with temporary ADMIN and MEMBER accounts. Health returned HTTP 200; ADMIN and MEMBER login returned HTTP 200 with Bearer access tokens; ADMIN access to the Admin API returned HTTP 200; MEMBER access returned JSON HTTP 403; `/api/auth/me` returned the authenticated user; incorrect password, tampered token, and HTTP Basic authentication returned JSON HTTP 401; authenticated invalid POST returned JSON HTTP 400; no session cookie was created. Temporary users and lab were removed successfully. Refresh tokens, logout persistence, token revocation, CORS, login history, password reset, and frontend integration remain deferred.
