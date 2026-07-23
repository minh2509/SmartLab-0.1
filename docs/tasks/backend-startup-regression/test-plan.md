# Test plan — Backend startup test-compilation regression

## TC-01 — Admin API structure test compiles

- Preconditions: DTO source hiện tại tồn tại.
- Action: Chạy `AdminApiStructureTests`.
- Expected result: Hai DTO được resolve và các boundary assertion pass.

## TC-02 — Admin user controller fixtures

- Preconditions: `ManagedUserSummary` có 11 record components.
- Action: Chạy `AdminUserControllerTests` và `AdminUserRoleControllerTests`.
- Expected result: Fixture compile; mapping response và role assertions pass.

## TC-03 — Successful login audit

- Preconditions: Authentication thành công và `UserRepository` trả user tương ứng.
- Action: POST login với `X-Forwarded-For` và `User-Agent`.
- Expected result: Token response đúng, `lastLoginAt` được đặt và một
  `LoginHistory` success được lưu với IP đầu tiên cùng user agent.

## TC-04 — Login failures

- Preconditions: AuthenticationManager ném bad credentials, user-not-found hoặc
  disabled.
- Action: POST login.
- Expected result: HTTP 401 generic, không lộ email/password hoặc loại exception.

## TC-05 — Current user endpoint

- Preconditions: JWT hợp lệ được truyền trực tiếp.
- Action: Gọi `me`.
- Expected result: Response user map đúng và constructor mới được cung cấp đủ
  dependency.

## TC-06 — Full backend suite

- Preconditions: Profile `nodb`.
- Action: Chạy `mvnw clean test`.
- Expected result: Build success, không failure/error.

## TC-06A — Security context bean graph

- Preconditions: `AuthController` cần `UserRepository` và
  `LoginHistoryRepository`.
- Action: Chạy `AdminApiSecurityTests` và `JwtLocalBeanGraphTests`.
- Expected result: Context khởi động bằng repository mock; security assertions cũ
  tiếp tục pass.

## TC-06B — Generated one-time credential

- Preconditions: Create user không cung cấp password hoặc chỉ có whitespace.
- Action: Gọi `createManagedUser`.
- Expected result: Service sinh password hợp lệ, encode đúng, đánh dấu
  `temporaryPasswordGenerated=true` và không lộ `passwordHash`.

## TC-07 — Runtime startup

- Preconditions: Profile `nodb`; cổng `8080` có thể đang bận.
- Action: Chạy Spring Boot với `server.port=0`.
- Expected result: Application báo started trên cổng ngẫu nhiên.

## TC-08 — Repository hygiene

- Preconditions: Verification hoàn tất.
- Action: Chạy `git diff --check`, `git status --short` và review diff.
- Expected result: Không có whitespace error và không ghi đè thay đổi ngoài phạm
  vi.
