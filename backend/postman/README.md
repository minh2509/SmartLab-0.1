# SmartLab Admin Postman contract

`SmartLab-Admin-API.postman_collection.json` is an executable contract for the
Admin API surface that exists in the current
`feature/admin-observability-join-event` worktree.

The collection is intentionally database-backed. Every Admin controller is
annotated with `@Profile("!nodb")`, and the `nodb` security chain denies
`/api/**`, so this collection cannot be run against the `nodb` profile.

## Current branch coverage

The collection covers every mapping currently exposed by the seven controllers
under `backend/src/main/java/com/smartlab/controller/admin/`.

| Controller | Covered mappings |
| --- | --- |
| `AdminDashboardController` | `GET /api/admin/dashboard` |
| `AdminProjectLookupController` | `GET /api/admin/projects/options` |
| `AdminJoinRequestController` | `GET /api/admin/project-join-requests`, `GET /options`, `GET /capabilities`, `GET /{requestId}`, `PATCH /{requestId}/approve`, `PATCH /{requestId}/reject` |
| `AdminNotificationController` | `GET /api/admin/notifications`, `GET /options`, `GET /{notificationId}`, `POST /api/admin/notifications`, `DELETE /{notificationId}` |
| `AdminUserController` | `POST /api/admin/users`, `GET /api/admin/users`, `GET /{userId}`, `GET /by-email`, `PATCH /{userId}`, `PATCH /{userId}/status` |
| `AdminRoleController` | `GET /api/admin/roles` |
| `AdminUserRoleController` | `GET /api/admin/users/{userId}/roles`, `PUT /api/admin/users/{userId}/roles`, `PUT /api/admin/users/{userId}/roles/{roleId}`, `DELETE /api/admin/users/{userId}/roles/{roleId}` |

That is 24 distinct `/api/admin/**` method/path mappings. Authentication setup
and the shared security boundary add these supporting checks:

- `POST /api/auth/login` for an Admin and a non-Admin account.
- `GET /api/auth/me`.
- Anonymous Admin access returns `401` with the shared error schema.
- An authenticated MEMBER/LEADER-only caller receives `403` with the shared
  error schema.

The request tests check the expected status, response shape, important enum and
scope fields, compact option/list DTOs, empty `204` responses, and the common
error object (`timestamp`, `status`, `error`, `message`, `path`).

## Branch-dependent boundary

This collection reflects the checked-out feature branch, not the union of every
remote branch.

- At the time of this snapshot, `origin/main` contains
  `AdminPostController` with `GET /api/admin/posts`,
  `GET /api/admin/posts/pending`, `GET /api/admin/posts/{postId}`, and
  `POST /api/admin/posts/{postId}/approve`. That controller is not present in
  this feature worktree, so those four mappings are deliberately not invented
  in this collection. Add their real main-branch DTO contracts after the
  branches are merged.
- Conversely, the operational dashboard, project lookup, join-request, and
  notification controllers in this collection are feature-branch work until
  they are merged into main.
- ADM-084 through ADM-089 event endpoints are not covered because no Admin
  event controller exists in the current worktree.

Re-run the controller-to-collection inventory after merging main. A successful
import of this file does not prove coverage of endpoints that exist only on
another branch.

## Prerequisites

1. Run the backend with Java 21 and a disposable PostgreSQL database using the
   local database profile described in `backend/README.md`.
2. Apply the Flyway migrations and ensure the database has:
   - one active `ADMIN` or `SUPER_ADMIN` account;
   - one active account with neither `ADMIN` nor `SUPER_ADMIN` (for example,
     MEMBER-only);
   - at least one active lab user so a LAB notification has an eligible
     recipient;
   - at least one project if project option coverage must return a non-empty
     result;
   - at least one project join request if list/detail coverage must run.
3. Keep real passwords outside source control. Set them as Postman environment
   values or Newman `--env-var` arguments.
4. Import `SmartLab-Admin-API.postman_collection.json`.

## Variables

Set these before a complete run:

| Variable | Required | Purpose |
| --- | --- | --- |
| `baseUrl` | yes | Backend origin; defaults to `http://localhost:8080`. |
| `adminEmail`, `adminPassword` | yes | Active ADMIN/SUPER_ADMIN credentials. |
| `nonAdminEmail`, `nonAdminPassword` | yes for security folder | Active MEMBER/LEADER-only credentials used for the `403` check. |
| `joinRequestId` | if no option is available | Join request used by the detail request. The options/list requests capture one automatically when possible. |
| `approveJoinRequestId` | when override is enabled | Existing PENDING request used by the state-changing approve example. |
| `rejectJoinRequestId` | when override is enabled | A different existing PENDING request used by the state-changing reject example. |

The collection derives and stores `accessToken`, `nonAdminAccessToken`,
`adminUserId`, `labId`, `projectId`, `notificationId`, the generated managed
user variables, and MEMBER/LEADER role IDs.

`GET /api/admin/project-join-requests/capabilities` synchronizes
`joinRequestOverrideEnabled` with the backend feature flag. When the capability
is `false`, approve/reject are expected to return `403`. When it is `true`,
provide two distinct PENDING IDs and the requests expect `200`.

## Run order

Run folders in collection order:

1. Authentication.
2. Admin Security Boundary.
3. Admin Dashboard.
4. Admin Project Lookup.
5. Admin Project Join Requests.
6. Admin Notifications.
7. Admin User Management.
8. Admin Role Management.

The user workflow creates a unique MEMBER, reads and updates it, then sets it to
`LOCKED`. The role workflow assigns LEADER, verifies both active assignments,
revokes MEMBER while LEADER remains, and finally replaces the active set with
MEMBER. This is deliberate mutation of a disposable account; there is currently
no Admin delete-user endpoint to clean it up.

The notification workflow creates a LAB notification and then globally hides
its recipient deliveries. It does not hard-delete the notification history.
Approve/reject mutate their supplied join requests permanently.

## Newman example

With `newman` already installed, run from the repository root:

```powershell
newman run backend/postman/SmartLab-Admin-API.postman_collection.json `
  --env-var "baseUrl=http://localhost:8080" `
  --env-var "adminEmail=<admin-email>" `
  --env-var "adminPassword=<admin-password>" `
  --env-var "nonAdminEmail=<member-email>" `
  --env-var "nonAdminPassword=<member-password>" `
  --env-var "approveJoinRequestId=<pending-request-id-1>" `
  --env-var "rejectJoinRequestId=<pending-request-id-2>"
```

Omit the two mutation IDs while the override capability is disabled. If the
database has no join requests, either seed one or skip the join-request detail
and mutation requests.

This repository does not install Newman as a project dependency. Do not add or
upgrade dependencies solely to run the collection without project approval.

## What static validation proves

Parsing the collection JSON and compiling its JavaScript test snippets proves
that the artifact is structurally readable. It does not prove live API behavior.
A live run requires database credentials and appropriate seed records; do not
report a Newman/Postman pass unless that run actually occurred.
