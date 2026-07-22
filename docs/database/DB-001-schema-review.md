# DB-001 Schema Review

## Executive summary

Conclusion: NEEDS_REVISION

`docs/database/lab-source.sql` is a strong PostgreSQL-oriented source schema, but it should not be converted directly into a first Flyway migration. The main blockers are extension permission assumptions, source cleanup around repeated `ALTER TABLE users` statements, circular foreign-key handling when migrations are split, unclear Super Admin representation, future-expansion modules included in the base schema, and missing cross-table consistency guarantees for lab/project-scoped data.

No SQL was executed during this review.

## Schema inventory

- Tables: 34
- Triggers: 11
- Indexes: 50
- ALTER TABLE statements: 7
- Seed groups: 5

## Critical findings

1. `CREATE EXTENSION IF NOT EXISTS pgcrypto` can fail in Flyway if the migration user lacks extension privileges. DB-001 should require an owner decision: pre-install `pgcrypto`, grant the migration role enough permission, or replace `gen_random_uuid()` defaults with application-generated UUIDs.
2. The labs/users/files dependency cycle and documents/document_versions cycle require delayed foreign-key constraints. The source handles this with later `ALTER TABLE` statements, but a naive Flyway split can fail if those constraints are placed before both sides exist.
3. `users.joined_at` is added twice after the table is created, and `users.updated_at` plus `users.last_login_at` are added even though `users` already defines `updated_at` and `last_login_at`. `IF NOT EXISTS` prevents failure, but V1 should not contain contradictory historical patch statements.
4. The source seeds `ADMIN`, `LEADER`, and `MEMBER`, but project instructions define Super Admin/Admin/Leader/Member. Running V1 without a Super Admin decision creates an authorization-model mismatch.

## High-priority findings

1. `roles.updated_at` is added after `roles` is created and after role seed data, but `roles` has no `updated_at` trigger. Either define it in the table with a trigger, or omit it until a role-edit workflow exists.
2. `documents` and `events` are marked `FUTURE EXPANSION` but are included in the source schema. They should probably move to later migrations unless DB-002 explicitly approves them for V1.
3. Many lab/project-scoped relationships can point across labs. Examples: `projects.lab_id` plus `projects.created_by`, `posts.lab_id` plus `posts.project_id`, `documents.lab_id` plus `documents.project_id`, `events.lab_id` plus `events.project_id`, `files.lab_id` plus `files.uploaded_by`, and project/member/task tables that join users to projects. PostgreSQL cannot enforce all of these with simple single-column foreign keys; DB-002 needs a policy for composite foreign keys, triggers, or application-level validation.
4. Soft delete is inconsistent. Some tables have `deleted_at`, some encode deletion in status, and many FK paths still use `ON DELETE CASCADE`. Unique constraints on users, projects, and posts do not account for soft-deleted rows.
5. `ON DELETE CASCADE` on content ownership can remove important records. For example, deleting a user cascades posts through `author_id`; deleting a lab cascades most operational data. This may be acceptable only for hard-delete admin flows, not ordinary account removal.
6. Several temporal and state constraints are missing: project expected end date before start date, task due date before start date, event end before start, project actual end without completed/closed status, and status/date consistency for left/removed/reviewed/published fields.

## Medium and low-priority findings

1. Foreign-key indexes are incomplete. Missing or weak candidates include `user_roles(role_id)`, `role_permissions(permission_id)`, `member_research_fields(research_field_id)`, `project_research_fields(research_field_id)`, attachment `file_id` columns, `document_versions(file_id)`, `document_access_rules` foreign keys, `event_participants(user_id)`, and several `*_by` audit columns.
2. Some explicit indexes may be redundant with unique constraints. For example, `idx_users_lab_id` overlaps with unique indexes beginning with `lab_id`; keep it only if query plans justify it.
3. `member_evaluations` has a unique constraint including nullable `evaluation_period`; PostgreSQL allows multiple rows where `evaluation_period IS NULL`. Make it `NOT NULL`, use a generated period key, or add a partial unique index.
4. `document_access_rules` should define whether exactly one of `user_id` or `project_id` is required. Currently both can be null or both can be set.
5. `roles`, `permissions`, `research_fields`, `post_categories`, and `evaluation_criteria` are global. Confirm whether SmartLab wants global catalogs or lab-specific customization.
6. Seed groups are idempotent via `ON CONFLICT`, but production seed data should be minimal. The sample lab belongs in development seed data unless production always starts with `MAIN_LAB`.
7. `CREATE TABLE` and most `CREATE INDEX` statements are not idempotent. That is normal for Flyway versioned migrations, but the source mixes idempotent and non-idempotent styles; DB-002 should choose one style for migration files.

## Circular dependency analysis

- `labs` <-> `files`: `files.lab_id` references `labs`; `labs.logo_file_id` and `labs.cover_file_id` later reference `files`. In Flyway, create `labs` first with nullable file columns but without those FKs, create `files`, then add the two lab file FKs in a later constraint section or migration.
- `users` <-> `files`: `files.uploaded_by` references `users`; `users.avatar_file_id` later references `files`. In Flyway, create `users` without the avatar FK, create `files`, then add `fk_users_avatar_file`.
- `users` self-reference: `users.deleted_by` references `users`. PostgreSQL supports this inline, but seed/delete behavior should be reviewed.
- `documents` <-> `document_versions`: `document_versions.document_id` references `documents`; `documents.current_version_id` later references `document_versions`. In Flyway, create `documents` with nullable `current_version_id`, create `document_versions`, then add `fk_documents_current_version`.

## Proposed migration split

- `V1__enable_extensions_and_common_functions.sql`
- `V2__create_lab_identity_and_role_tables.sql`
- `V3__create_file_and_profile_tables.sql`
- `V4__add_core_identity_file_foreign_keys.sql`
- `V5__create_research_project_membership_tables.sql`
- `V6__create_task_evaluation_and_content_tables.sql`
- `V7__create_notification_and_audit_tables.sql`
- `V8__create_indexes_and_partial_unique_indexes.sql`
- `V9__seed_required_reference_data.sql`
- `V10__dev_seed_sample_lab.sql` or a non-production seed script outside Flyway

Future migrations, only after owner approval:

- `V11__create_document_tables.sql`
- `V12__add_document_version_cycle_constraints.sql`
- `V13__create_calendar_event_tables.sql`

Do not create these migrations in DB-001.

## Seed-data plan

Migration seed data should include stable reference data that application logic depends on: approved role codes, permission codes if they are used, research fields if they are global, post categories if fixed, and evaluation criteria if fixed by product rules.

Development-only data should include the sample `MAIN_LAB`, demo users, demo projects, and any realistic content needed for local or UI demonstration.

Do not seed production credentials, password hashes, or environment-specific URLs in Flyway migrations.

## Super Admin decision

Viable options:

1. Add explicit `SUPER_ADMIN` as a role code.
2. Keep only `ADMIN` in `roles` and represent the initial Main Admin separately, for example with an immutable bootstrap user flag or system setting.
3. Use `ADMIN` plus application-level ownership metadata for the initial account.

Recommendation: prefer an explicit `SUPER_ADMIN` role if the backend will enforce a distinct system-level role. If Super Admin is only a bootstrap concept with no permanent permission difference, document that and keep `ADMIN` only. Minh or the team must approve this before DB-002.

## Questions requiring owner approval

- Should `pgcrypto` be installed by Flyway, pre-installed by infrastructure, or avoided?
- Is `SUPER_ADMIN` a persisted role, a bootstrap-only concept, or an application-level flag?
- Are roles, permissions, research fields, post categories, and evaluation criteria global or lab-scoped?
- Should unique slugs be scoped by `lab_id`, globally unique, or routed with a lab identifier?
- Should public routes support multiple labs, given lab-scoped project and post slugs?
- Should documents and events be removed from V1 and deferred to later migrations?
- Which delete actions are hard deletes, and which should use soft delete?
- Should soft-deleted users/projects/posts release unique usernames, emails, codes, and slugs?
- Which cross-lab consistency checks belong in SQL versus service-layer validation?
- Should date/status consistency be enforced with CHECK constraints or application logic?
- Should sample lab seed data be production migration data or development-only data?
- Which foreign-key indexes are mandatory for V1 performance?

## Recommended next task

DB-002: Prepare the V1 migration plan and schema cleanup patch.

Scope for DB-002:

- Decide the approved V1 table/module boundary.
- Remove duplicate historical `ALTER TABLE` column additions from the source-to-migration plan.
- Resolve the `pgcrypto` strategy.
- Decide Super Admin representation.
- Decide global versus lab-scoped reference catalogs.
- Define the first migration files and the exact statements that belong in each.
- Do not implement Java entities, repositories, controllers, DTOs, frontend integration, or production seed users.
