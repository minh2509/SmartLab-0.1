# Verification report — Backend startup test-compilation regression

## Result

Status: `READY_FOR_REVIEW`

The backend test suite, package step, and `nodb` runtime startup all pass. No
production source was changed by this task.

## Root cause

The current production contracts had moved ahead of several regression tests:

- Admin user responses now wrap a user summary with a one-time credential.
- Managed user summaries now contain 11 fields.
- Successful login records login history and updates `lastLoginAt`.
- Admin APIs use Bearer JWT authorization and allow `ADMIN` or `SUPER_ADMIN`.
- Blank admin-created passwords are generated; supplied passwords are trimmed.
- Structural source checks used path matching that did not work reliably on
  Windows.

These stale tests caused 25 test-compilation errors before Spring Boot could
start.

## Files changed

- `backend/src/test/java/com/smartlab/controller/admin/AdminApiStructureTests.java`
- `backend/src/test/java/com/smartlab/controller/admin/AdminUserControllerTests.java`
- `backend/src/test/java/com/smartlab/controller/admin/AdminUserRoleControllerTests.java`
- `backend/src/test/java/com/smartlab/controller/auth/AuthControllerTests.java`
- `backend/src/test/java/com/smartlab/security/AdminApiSecurityTests.java`
- `backend/src/test/java/com/smartlab/security/JwtLocalBeanGraphTests.java`
- `backend/src/test/java/com/smartlab/security/SecurityStructuralTests.java`
- `backend/src/test/java/com/smartlab/service/admin/AdminUserServiceTests.java`
- `docs/TASKS.md`
- `docs/tasks/backend-startup-regression/*`

## Commands and evidence

1. Initial `spring-boot:run` attempt:
   failed during `testCompile` with 25 compilation errors in stale tests.
2. Targeted controller tests:
   23 tests passed after their fixtures and response assertions were synchronized.
3. Targeted admin security tests:
   12 tests passed using Bearer JWT authorization.
4. Required clean backend test:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'nodb'
   .\mvnw.cmd clean test
   ```

   Result: `Tests run: 249, Failures: 0, Errors: 0, Skipped: 0`;
   `BUILD SUCCESS`.
5. Package:

   ```powershell
   .\mvnw.cmd package -DskipTests
   ```

   Result: `BUILD SUCCESS`; generated
   `target/smartlab-backend-0.0.1-SNAPSHOT.jar`.
6. Runtime smoke test:
   the packaged JAR started with profile `nodb`, reached
   `ReadinessState.ACCEPTING_TRAFFIC`, and
   `GET /actuator/health` returned HTTP 200 with status `UP`.
7. Diff validation:
   `git diff --check` returned exit code `0`.

## Acceptance checklist

- [x] Test sources compile on Windows.
- [x] Full backend tests pass with the `nodb` profile.
- [x] The packaged backend starts on port 8080.
- [x] The health endpoint reports `UP`.
- [x] No production source was changed by this task.
- [x] No unrelated working-tree changes were overwritten.

## Limitations and follow-up

- The `nodb` profile intentionally runs without PostgreSQL. It is suitable for
  compilation, tests, health checks, and database-independent startup; real
  admin APIs require the database-backed profile and its environment variables.
- The managed verification process was automatically stopped when its command
  session ended, so it is not left running after verification.
- Maven reports a Mockito dynamic-agent compatibility warning and SpringDoc
  endpoint warnings. They do not fail the current build.
- Older task notes describe login-history persistence as deferred, while the
  current source already implements it. Source behavior was treated as the
  source of truth for this regression fix.
