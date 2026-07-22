# DB-002 Migration Plan

## Purpose

Prepare the PostgreSQL migration plan and schema cleanup specification for the first SmartLab database migrations.

This document is a planning artifact only. Do not copy `docs/database/lab-source.sql` directly into migration files. DB-003 must write curated Flyway migrations from this plan and re-check every statement.

## Approved Owner Decisions

- Keep `pgcrypto` and `gen_random_uuid()`.
- Persist four roles: `SUPER_ADMIN`, `ADMIN`, `LEADER`, `MEMBER`.
- Roles, permissions, research fields, post categories, and evaluation criteria are global catalogs in V1.
- Project and post slugs are unique within each lab using `UNIQUE (lab_id, slug)`.
- Documents and calendar/events are excluded from V1 and deferred.
- Users, projects, and posts use soft deletion.
- Labs cannot be hard deleted through normal application APIs.
- Soft-deleted usernames, emails, project codes, and slugs remain reserved.
- Cross-lab consistency is enforced in the service layer for V1 unless a simple SQL constraint is sufficient.
- Simple date ordering belongs in SQL `CHECK` constraints.
- Workflow-dependent timestamp rules belong in application services.
- Production migrations seed only stable reference data.
- Sample lab, demo users, demo projects, and demo content belong in a development-only seed script.
- Add useful foreign-key indexes, but avoid indexes already covered by primary or unique constraints.

## Exact V1 Module Boundary

V1 includes the core modules needed for SmartLab identity, catalogs, project management, task/evaluation flows, content moderation, notifications, and audit history.

V1 excludes document management and calendar/events even though they exist in `lab-source.sql`, because they are explicitly marked future expansion and owner-approved as deferred.

## Tables Included In V1

1. `labs`
2. `users`
3. `roles`
4. `permissions`
5. `user_roles`
6. `role_permissions`
7. `files`
8. `member_profiles`
9. `research_fields`
10. `member_research_fields`
11. `projects`
12. `project_research_fields`
13. `project_members`
14. `project_join_requests`
15. `tasks`
16. `task_assignees`
17. `task_reports`
18. `task_attachments`
19. `evaluation_criteria`
20. `member_evaluations`
21. `member_evaluation_details`
22. `post_categories`
23. `posts`
24. `post_moderation_logs`
25. `post_attachments`
26. `notifications`
27. `notification_recipients`
28. `audit_logs`
29. `login_histories`

## Tables Deferred

- `documents`
- `document_versions`
- `document_access_rules`
- `events`
- `event_participants`

Deferred document migrations must separately handle the `documents.current_version_id` and `document_versions.document_id` cycle.

## Cleanup Changes From lab-source.sql

- Remove duplicate late additions of `users.joined_at`.
- Remove late additions of `users.updated_at` and `users.last_login_at`; both should be defined directly in `CREATE TABLE users` if kept.
- Decide whether `users.joined_at` belongs on `users` or only on `member_profiles` / `project_members`; recommended V1 keeps lab account timestamps on `users` only if product needs them.
- Define `roles.updated_at` directly in `roles` only if role edits are supported in V1; otherwise omit it. If included, add `trg_roles_updated_at`.
- Exclude document and event tables, their triggers, indexes, and cyclic constraints from V1.
- Remove document/event indexes from the V1 index migration.
- Add missing `SUPER_ADMIN` role seed.
- Remove production sample lab seed from Flyway migrations.
- Keep `UNIQUE (lab_id, username)`, `UNIQUE (lab_id, email)`, `UNIQUE (lab_id, code)`, and `UNIQUE (lab_id, slug)` so soft-deleted values remain reserved.
- Add simple date-ordering `CHECK` constraints listed below.
- Keep workflow-dependent timestamp validation, such as `published_at` rules, in services.
- Normalize trigger formatting and attach `set_updated_at()` only to tables that actually have `updated_at`.

## Exact Migration Files And Responsibilities

- `V1__enable_extensions_and_common_functions.sql`
  - Create `pgcrypto`.
  - Create `set_updated_at()` trigger function.

- `V2__create_core_identity_tables.sql`
  - Create `labs`, `roles`, `permissions`, `users`, `user_roles`, and `role_permissions`.
  - Include status `CHECK` constraints.
  - Include unique constraints that reserve soft-deleted user identifiers.
  - Create updated-at triggers for tables with `updated_at`.
  - Do not add file/avatar FKs yet.

- `V3__create_file_profile_and_catalog_tables.sql`
  - Create `files`, `member_profiles`, `research_fields`, `member_research_fields`, `post_categories`, and `evaluation_criteria`.
  - Include global catalog uniqueness on `code`.

- `V4__add_core_file_foreign_keys.sql`
  - Add delayed FKs for `users.avatar_file_id`, `labs.logo_file_id`, and `labs.cover_file_id`.

- `V5__create_project_membership_tables.sql`
  - Create `projects`, `project_research_fields`, `project_members`, and `project_join_requests`.
  - Include lab-scoped project code/slug uniqueness and project/task date checks.
  - Include pending join-request partial uniqueness.

- `V6__create_task_evaluation_and_content_tables.sql`
  - Create `tasks`, `task_assignees`, `task_reports`, `task_attachments`, `member_evaluations`, `member_evaluation_details`, `posts`, `post_moderation_logs`, and `post_attachments`.
  - Include post slug uniqueness by lab.
  - Include task date checks and score checks.

- `V7__create_notification_and_audit_tables.sql`
  - Create `notifications`, `notification_recipients`, `audit_logs`, and `login_histories`.

- `V8__create_indexes.sql`
  - Add selected non-redundant foreign-key and query indexes.
  - Exclude indexes already covered by primary keys or unique constraints unless a separate access pattern requires them.

- `V9__seed_stable_reference_data.sql`
  - Seed `roles`, stable `permissions` if approved, `research_fields`, `post_categories`, and `evaluation_criteria`.
  - Include `SUPER_ADMIN`, `ADMIN`, `LEADER`, and `MEMBER`.
  - Do not seed sample labs, users, demo projects, or demo content.

- `R__dev_seed_sample_data.sql` or `dev/V1__seed_demo_data.sql`
  - Development-only, not production Flyway baseline.
  - Seed sample lab, demo users, demo projects, and demo content.

## Table Creation Order

1. Enable extension and common function.
2. `labs`
3. `roles`
4. `permissions`
5. `users`
6. `user_roles`
7. `role_permissions`
8. `files`
9. `member_profiles`
10. `research_fields`
11. `member_research_fields`
12. `post_categories`
13. `evaluation_criteria`
14. Add delayed core file foreign keys.
15. `projects`
16. `project_research_fields`
17. `project_members`
18. `project_join_requests`
19. `tasks`
20. `task_assignees`
21. `task_reports`
22. `task_attachments`
23. `member_evaluations`
24. `member_evaluation_details`
25. `posts`
26. `post_moderation_logs`
27. `post_attachments`
28. `notifications`
29. `notification_recipients`
30. `audit_logs`
31. `login_histories`
32. Indexes
33. Stable reference seeds

## Delayed Foreign-Key Order

1. Create `labs` with nullable `logo_file_id` and `cover_file_id`, but without FKs to `files`.
2. Create `users` with nullable `avatar_file_id`, but without FK to `files`.
3. Create `files` after `labs` and `users`.
4. Add `fk_users_avatar_file`.
5. Add `fk_labs_logo_file`.
6. Add `fk_labs_cover_file`.

The document/version cycle is deferred with the document module and must not appear in V1.

## CHECK Constraints To Add

- `projects.progress_percent >= 0 AND projects.progress_percent <= 100`
- `projects.expected_end_date IS NULL OR projects.start_date IS NULL OR projects.expected_end_date >= projects.start_date`
- `projects.actual_end_date IS NULL OR projects.start_date IS NULL OR projects.actual_end_date >= projects.start_date`
- `tasks.due_date IS NULL OR tasks.start_date IS NULL OR tasks.due_date >= tasks.start_date`
- `member_evaluations.overall_score IS NULL OR member_evaluations.overall_score >= 0`
- `member_evaluation_details.score >= 0`
- `evaluation_criteria.max_score > 0`
- `files.file_size >= 0`
- All enum-like status, role, visibility, scan, content, moderation, priority, and action checks from the approved source schema.

Do not add workflow timestamp checks in SQL for V1. Examples that stay in services: `published_at` required only when published, `reviewed_at` required only after review, `left_at` required only after leaving/removal, and `completed_at` required only after completion.

## ON DELETE Policy

| Table | Foreign key policy |
| --- | --- |
| `labs` | No normal hard delete through APIs. Keep DB FKs mostly `ON DELETE CASCADE` for rare admin/test teardown only, or use `RESTRICT` if operations wants stronger protection. |
| `users.lab_id` | `ON DELETE CASCADE` acceptable only because labs are not normally hard deleted. |
| `users.deleted_by` | `ON DELETE SET NULL`. |
| `roles` | Referenced by joins; no parent FK. |
| `permissions` | Referenced by joins; no parent FK. |
| `user_roles.user_id` | `ON DELETE CASCADE` for hard-delete cleanup. Normal user deletion is soft. |
| `user_roles.role_id` | `ON DELETE CASCADE` only if role cleanup is allowed; otherwise consider `RESTRICT`. |
| `user_roles.assigned_by` | `ON DELETE SET NULL`. |
| `role_permissions.role_id` | `ON DELETE CASCADE`. |
| `role_permissions.permission_id` | `ON DELETE CASCADE`. |
| `files.lab_id` | `ON DELETE CASCADE` only for lab teardown. |
| `files.uploaded_by` | `ON DELETE SET NULL`. |
| `member_profiles.user_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `member_research_fields.*` | `ON DELETE CASCADE` for join cleanup. |
| `projects.lab_id` | `ON DELETE CASCADE` only for lab teardown. |
| `projects.cover_file_id` | `ON DELETE SET NULL`. |
| `projects.created_by` | `ON DELETE SET NULL`. |
| `project_research_fields.*` | `ON DELETE CASCADE` for join cleanup. |
| `project_members.project_id` | `ON DELETE CASCADE`; normal project deletion is soft. |
| `project_members.user_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `project_members.added_by` | `ON DELETE SET NULL`. |
| `project_join_requests.project_id` | `ON DELETE CASCADE`; normal project deletion is soft. |
| `project_join_requests.requester_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `project_join_requests.cv_file_id` | `ON DELETE SET NULL`. |
| `project_join_requests.reviewed_by` | `ON DELETE SET NULL`. |
| `tasks.project_id` | `ON DELETE CASCADE`; normal project deletion is soft. |
| `tasks.created_by` | `ON DELETE SET NULL`. |
| `task_assignees.task_id` | `ON DELETE CASCADE`. |
| `task_assignees.user_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `task_assignees.assigned_by` | `ON DELETE SET NULL`. |
| `task_reports.task_id` | `ON DELETE CASCADE`. |
| `task_reports.reporter_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `task_reports.reviewed_by` | `ON DELETE SET NULL`. |
| `task_attachments.task_id` | `ON DELETE CASCADE`. |
| `task_attachments.file_id` | `ON DELETE CASCADE` only if deleting a file should remove attachment links; otherwise consider `RESTRICT`. |
| `task_attachments.uploaded_by` | `ON DELETE SET NULL`. |
| `member_evaluations.project_id` | `ON DELETE CASCADE`; normal project deletion is soft. |
| `member_evaluations.member_id` | `ON DELETE CASCADE`; normal user deletion is soft. |
| `member_evaluations.evaluator_id` | `ON DELETE CASCADE`; consider `SET NULL` if evaluator history must survive hard delete. |
| `member_evaluation_details.*` | `ON DELETE CASCADE`. |
| `posts.lab_id` | `ON DELETE CASCADE` only for lab teardown. |
| `posts.project_id` | `ON DELETE SET NULL` to preserve post history. |
| `posts.category_id` | `ON DELETE SET NULL`. |
| `posts.author_id` | Prefer `ON DELETE SET NULL` with nullable `author_id`, or keep `CASCADE` only if hard-deleted users may erase posts. Recommended V1: `SET NULL` and normal user deletion soft. |
| `posts.cover_file_id` | `ON DELETE SET NULL`. |
| `posts.reviewed_by` | `ON DELETE SET NULL`. |
| `post_moderation_logs.post_id` | `ON DELETE CASCADE`; normal post deletion is soft. |
| `post_moderation_logs.actor_id` | `ON DELETE SET NULL`. |
| `post_attachments.post_id` | `ON DELETE CASCADE`. |
| `post_attachments.file_id` | Same decision as task attachments. |
| `post_attachments.uploaded_by` | `ON DELETE SET NULL`. |
| `notifications.lab_id` | `ON DELETE CASCADE` only for lab teardown. |
| `notifications.created_by` | `ON DELETE SET NULL`. |
| `notification_recipients.*` | `ON DELETE CASCADE`. |
| `audit_logs.lab_id` | Consider `ON DELETE SET NULL` or `RESTRICT` if audit history must outlive lab teardown; source uses `CASCADE`. |
| `audit_logs.actor_id` | `ON DELETE SET NULL`. |
| `login_histories.user_id` | `ON DELETE SET NULL`. |

## Soft-Delete Policy

- `users`, `projects`, and `posts` include `deleted_at`.
- User delete operation sets `users.account_status = 'DELETED'`, `deleted_at`, and `deleted_by`.
- Project delete operation sets `projects.deleted_at`; status may move to `CLOSED` only through service workflow if approved.
- Post delete operation sets `posts.deleted_at`; permanent deletion is an Admin-only service decision, not automatic DB behavior.
- Unique constraints remain full unique constraints, not partial unique indexes, so soft-deleted usernames, emails, project codes, project slugs, and post slugs remain reserved.
- Normal queries must filter out `deleted_at IS NOT NULL` unless explicitly requesting deleted records.

## Unique Constraint Policy

- `labs.code` remains globally unique.
- `roles.code`, `permissions.code`, `research_fields.code`, `post_categories.code`, and `evaluation_criteria.code` remain globally unique because they are global catalogs.
- `users`: keep `UNIQUE (lab_id, username)` and `UNIQUE (lab_id, email)`.
- `projects`: keep `UNIQUE (lab_id, code)` and `UNIQUE (lab_id, slug)`.
- `posts`: keep `UNIQUE (lab_id, slug)`.
- Join tables keep pair uniqueness:
  - `UNIQUE (user_id, role_id)`
  - `UNIQUE (role_id, permission_id)`
  - `UNIQUE (member_profile_id, research_field_id)`
  - `UNIQUE (project_id, research_field_id)`
  - `UNIQUE (project_id, user_id)`
  - `UNIQUE (task_id, user_id)`
  - `UNIQUE (task_id, file_id)`
  - `UNIQUE (evaluation_id, criteria_id)`
  - `UNIQUE (post_id, file_id)`
  - `UNIQUE (notification_id, recipient_id)`
- `project_join_requests`: keep partial unique index on `(project_id, requester_id) WHERE status = 'PENDING'`.
- `member_evaluations`: make `evaluation_period` `NOT NULL` if it remains part of the uniqueness key, or replace with a reviewed period model in a later task.

## Index Plan

Add indexes for foreign keys and frequent query filters that are not already covered by primary or unique indexes.

Recommended V1 indexes:

- `users(lab_id)`
- `users(account_status)`
- `files(lab_id)`
- `files(uploaded_by)`
- `user_roles(role_id)`
- `role_permissions(permission_id)`
- `member_research_fields(research_field_id)`
- `projects(lab_id)`
- `projects(status)`
- `projects(visibility)`
- `projects(created_by)`
- `project_research_fields(research_field_id)`
- `project_members(user_id)`
- `project_members(project_role)`
- `project_join_requests(project_id)`
- `project_join_requests(requester_id)`
- `project_join_requests(status)`
- `tasks(project_id)`
- `tasks(status)`
- `tasks(due_date)`
- `task_assignees(user_id)`
- `task_reports(task_id)`
- `task_reports(reporter_id)`
- `task_attachments(file_id)`
- `member_evaluations(project_id)`
- `member_evaluations(member_id)`
- `member_evaluations(evaluator_id)`
- `member_evaluation_details(criteria_id)`
- `posts(lab_id)`
- `posts(project_id)`
- `posts(author_id)`
- `posts(moderation_status)`
- `posts(visibility)`
- `posts(published_at)`
- `post_moderation_logs(post_id)`
- `post_moderation_logs(actor_id)`
- `post_attachments(file_id)`
- `notifications(lab_id)`
- `notifications(related_type, related_id)`
- `notification_recipients(recipient_id)`
- `notification_recipients(read_at)`
- `audit_logs(actor_id)`
- `audit_logs(entity_type, entity_id)`
- `audit_logs(created_at)`
- `login_histories(user_id)`
- `login_histories(login_at)`

Avoid or justify:

- Separate indexes on columns already leading a unique constraint unless query plans need them.
- Document/event indexes in V1.
- Indexes on low-cardinality status columns unless paired with common filters or needed for admin lists.

## Seed Plan

Production Flyway seeds:

- `roles`: `SUPER_ADMIN`, `ADMIN`, `LEADER`, `MEMBER`.
- `permissions`: only stable permission codes approved before DB-003. If no stable permission matrix is approved, seed no permissions in V1 and leave `role_permissions` empty.
- `research_fields`: `AI`, `ROBOTICS`, `SE` if these remain approved global catalogs.
- `post_categories`: stable content categories from the source if approved.
- `evaluation_criteria`: stable criteria from the source if approved.

Development-only seed script:

- Sample `MAIN_LAB`.
- Demo users and password hashes.
- Demo projects, tasks, posts, notifications, evaluations, and files.
- Any data needed only for screenshots or local demos.

Seed safety:

- No production password hashes.
- No secrets.
- No environment-specific URLs.
- Use idempotent inserts for reference data.

## pgcrypto Operational Requirement

Because V1 keeps `pgcrypto` and `gen_random_uuid()`, the deployment environment must satisfy one of these before Flyway runs:

1. The Flyway migration database user can run `CREATE EXTENSION IF NOT EXISTS pgcrypto`.
2. A database administrator pre-installs `pgcrypto` in the target database/schema before migrations.

DB-003 must document which option is used for local development, test, and production.

## Super Admin Bootstrap Strategy

V1 persists `SUPER_ADMIN` as a global role code. Production migrations seed the role only, not a user.

Bootstrap user creation should be handled outside production schema migrations through one of:

- an operator-run bootstrap command,
- a controlled admin setup flow,
- a one-time environment-specific seed script,
- or a development-only seed for local demos.

The bootstrap mechanism must never commit real credentials.

## Service-Layer Consistency Rules

Enforce these in services for V1:

- A project member's `user_id` must belong to the same lab as `project.lab_id`.
- A project join request requester must belong to the project lab.
- A task assignee must be a member of the task's project, unless an approved admin override exists.
- A task report reporter must be assigned to the task or approved by project rules.
- A member evaluation member/evaluator must belong to the project lab, and evaluator authority must be checked.
- A post's `project_id`, if present, must belong to the same `lab_id`.
- A post author and reviewer must belong to the post lab.
- A file used as avatar, cover, attachment, CV, or upload must belong to the same lab as the owning record.
- Notifications and recipients must not cross labs.
- Role assignment must not grant a role outside approved system rules.
- Project-scoped leader permissions must verify project membership/leadership, not only system role.

## Validation Checklist For The Future Migration

Before DB-003 is marked review-ready:

- Verify every migration runs from an empty PostgreSQL database.
- Verify Flyway runs with the agreed `pgcrypto` setup.
- Verify `application-nodb.yml` continues to exclude datasource and Flyway auto-configuration.
- Verify default profile database settings are supplied through environment variables, not committed secrets.
- Verify no document/event tables appear in V1.
- Verify no duplicate `ALTER TABLE users` column additions remain.
- Verify `SUPER_ADMIN`, `ADMIN`, `LEADER`, and `MEMBER` are seeded.
- Verify production migrations do not seed sample lab or demo users.
- Verify all delayed FKs are added after both sides exist.
- Verify simple date-ordering `CHECK` constraints exist.
- Verify workflow timestamp rules are not over-constrained in SQL.
- Verify foreign-key indexes are present where useful and not redundant with PK/unique constraints.
- Verify soft-delete uniqueness remains reserved.
- Verify table and column names align with future Java entity naming.
- Verify all V1 migrations are deterministic and do not depend on local database state.

## Rollback And Recovery Approach For Development

- During early local development, use disposable databases and rerun Flyway from scratch after migration edits.
- If a local migration fails before merge, drop and recreate only the local development database; never reset shared or production databases.
- After a migration is merged, do not edit it in place. Add a new versioned migration for changes.
- Use `flyway clean` only in explicitly disposable local environments and only if the project enables it intentionally.
- Keep development seed scripts separate so schema recovery does not require demo data.

## Definition Of Done For DB-003

DB-003 is done only when:

- Versioned Flyway migration files are created from this plan.
- V1 excludes documents and events.
- Stable production seeds are separated from development-only seed data.
- The migrations run successfully against a fresh PostgreSQL database.
- The agreed `pgcrypto` setup is documented.
- The backend can start with the database profile using environment-supplied database configuration.
- The backend still passes `SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test`.
- `docs/TASKS.md` is updated with real verification evidence.
- No frontend files, unrelated backend Java files, dependency files, or generated secrets are changed.
