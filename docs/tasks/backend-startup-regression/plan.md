# Implementation plan — Backend startup test-compilation regression

## Current flow

```text
mvnw spring-boot:run
  -> compile
  -> testCompile
  -> Spring Boot startup
```

Lỗi xuất hiện tại `testCompile`; main source không phải layer đầu tiên sai lệch.

## Files to modify

- `backend/src/test/java/com/smartlab/controller/admin/AdminApiStructureTests.java`
- `backend/src/test/java/com/smartlab/controller/admin/AdminUserControllerTests.java`
- `backend/src/test/java/com/smartlab/controller/admin/AdminUserRoleControllerTests.java`
- `backend/src/test/java/com/smartlab/controller/auth/AuthControllerTests.java`
- `backend/src/test/java/com/smartlab/security/AdminApiSecurityTests.java`
- `backend/src/test/java/com/smartlab/security/JwtLocalBeanGraphTests.java`
- `backend/src/test/java/com/smartlab/security/SecurityStructuralTests.java`
- `backend/src/test/java/com/smartlab/service/admin/AdminUserServiceTests.java`
- `docs/TASKS.md`
- `docs/tasks/backend-startup-regression/verification.md`

## Proposed changes

1. Import đúng `AdminPostDetailResponse` và
   `AdminPostModerationActionResponse`.
2. Cấp safe default cho ba trường mới của `ManagedUserSummary`:
   `lastLoginAt=null`, `temporaryPassword=null`,
   `temporaryPasswordGenerated=false`.
3. Tạo mock `UserRepository` và `LoginHistoryRepository` trong auth controller test.
4. Truyền đủ năm dependency vào cả hai điểm tạo `AuthController`.
5. Mở rộng happy-path login test để kiểm tra audit record hiện có.
6. Cung cấp repository mock cho hai Spring security context test.
7. Đồng bộ structural test với login-history/audit implementation hiện tại và dùng
   path-segment filtering đa nền tảng.
8. Đồng bộ Admin user service test với one-time/generated credential behavior đã
   được production source triển khai.

## Data and persistence

- Chỉ dùng Mockito; không ghi database thật.
- Không thay entity, repository interface hoặc migration.

## Permissions

- Không thay đổi rule Admin, JWT authority hoặc profile.
- Test tiếp tục gọi controller độc lập qua MockMvc.

## Verification order

1. Chạy targeted tests cho các class bị ảnh hưởng.
2. Chạy `clean test` toàn backend với profile `nodb`.
3. Khởi động backend với profile `nodb` và `server.port=0`.
4. Review diff, chạy `git diff --check`, ghi verification.

## Rollback

Các thay đổi chỉ nằm trong test và tài liệu; rollback không ảnh hưởng production
runtime hay dữ liệu.
