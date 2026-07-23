# Specification — Backend startup test-compilation regression

## Task overview

Khôi phục khả năng compile test và khởi động một backend build mới sau khi các
contract DTO/controller/service hiện tại đã thay đổi nhưng một số test chưa được
đồng bộ.

## Problem statement

`mvnw spring-boot:run` chạy phase `test-compile` trước khi khởi động. Phase này hiện
thất bại với 25 lỗi:

- `AdminApiStructureTests` sử dụng hai response DTO nhưng thiếu import.
- Hai controller test tạo `ManagedUserSummary` theo constructor cũ 8 trường, trong
  khi record hiện có 11 trường.
- `AuthControllerTests` tạo `AuthController` theo constructor cũ 3 dependency,
  trong khi controller hiện có thêm `UserRepository` và `LoginHistoryRepository`.

Sau khi hết lỗi compile, full suite còn cho thấy các test context/structural/service
cũ chưa được cập nhật theo commit production hiện tại:

- Security bean graph chưa cung cấp hai repository mới cho `AuthController`.
- Structural test vẫn coi login history và audit repository là tính năng deferred.
- Admin user service test vẫn yêu cầu password không được trim, password trống phải
  bị từ chối và summary không có one-time credential.

Backend cũ đang lắng nghe cổng `8080` và health vẫn trả HTTP 200, nhưng source hiện
tại không thể tạo một runtime mới qua Maven.

## Goals

- Đồng bộ test với contract source hiện tại.
- Giữ hoặc bổ sung coverage cho việc ghi nhận đăng nhập thành công.
- Giữ coverage cho one-time credential và password tự sinh của Admin user.
- Cho phép toàn bộ backend test suite compile và chạy với profile `nodb`.
- Cho phép backend khởi động mới trên một cổng kiểm chứng không xung đột.

## Non-goals

- Không thay đổi API contract, entity, service hoặc production business logic.
- Không dừng backend hiện đang chạy trên `8080`.
- Không thay đổi database, migration, dependency hoặc Spring profile.
- Không sửa các frontend file.

## Expected behavior

- `mvnw clean test` với profile `nodb` pass.
- `mvnw spring-boot:run` với profile `nodb` và `server.port=0` báo application
  started.
- Test login xác nhận user được cập nhật `lastLoginAt` và một `LoginHistory` thành
  công được lưu khi user tồn tại.
- Test Admin user xác nhận password nhập vào được trim, password trống tạo credential
  mới và `passwordHash` không xuất hiện trong summary.

## Authentication and authorization

- Không thay đổi security configuration.
- Profile `nodb` tiếp tục chỉ cho phép health/Swagger và từ chối `/api/**`.
- Các controller Admin tiếp tục chỉ active ngoài profile `nodb`.

## Acceptance criteria

- Không còn 25 lỗi `testCompile` đã tái hiện.
- Không dùng mock hoặc fixture làm thay đổi contract production.
- Login failure và validation tests tiếp tục pass.
- Toàn bộ backend tests pass.
- Health endpoint của runtime kiểm chứng trả HTTP 200 nếu có thể gọi trong thời
  gian process chạy.

## Edge cases and risks

- `UserRepository.findById` trả empty: controller không ghi login history.
- Header `X-Forwarded-For` có nhiều địa chỉ: controller dùng địa chỉ đầu tiên.
- Cổng `8080` đã bận: runtime kiểm chứng dùng cổng ngẫu nhiên.
- Maven có thể cần tải dependency từ Central trong lần chạy đầu tiên.
