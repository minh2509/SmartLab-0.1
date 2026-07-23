# Implementation plan — Development runner

## Current architecture

Runner mới điều phối hai terminal trực tiếp:

```text
run.bat
  ├── terminal "SmartLab Backend"
  │   └── backend/mvnw.cmd spring-boot:run
  └── terminal "SmartLab Frontend"
      └── npm run dev -- --port 5173
```

## Proposed solution

Tạo Windows batch launcher dùng `start` và `cmd /k` để mở hai terminal độc
lập. Mỗi terminal có working directory và command riêng.

## Files to create

- `run.bat`: entry point chạy development stack.
- `docs/tasks/dev-runner/verification.md`: bằng chứng kiểm tra sau triển khai.

## Files to modify

- `backend/mvnw.cmd`: xử lý an toàn thuộc tính `Target` rỗng trước khi xác định
  Maven Wrapper distribution directory.

## Data, API, authorization and persistence

- Không thay đổi data structure.
- Không thay đổi API contract.
- Không thay đổi authentication hoặc authorization.
- Không thay đổi database, migration, DTO hoặc persistence.

## Implementation order

1. Tạo `run.bat` với `@echo off` và `setlocal`.
2. Dùng `%~dp0` để chuyển về repository root.
3. Kiểm tra Node.js, npm, Java, Maven Wrapper và Vite.
4. Đặt `SPRING_PROFILES_ACTIVE=nodb` khi chưa có profile.
5. Dùng `start` mở terminal backend với working directory `backend/`.
6. Dùng `start` mở terminal frontend với working directory `frontend/`.
7. Dùng `cmd /k` để giữ từng terminal mở sau lỗi.
8. Xác minh Maven Wrapper và kiểm tra tĩnh hai command terminal.
9. Review diff và chạy `git diff --check`.
10. Ghi kết quả vào `verification.md`.

## Error handling

- Mỗi prerequisite thiếu sẽ in thông báo `[ERROR]` cùng command cài đặt phù
  hợp và trả exit code `1`.
- Sau khi mở terminal thành công, launcher trả exit code `0`; mỗi terminal tự
  hiển thị trạng thái process của mình.

## Backward compatibility and rollback

- Không thay đổi script hiện tại nên mọi command cũ tiếp tục hoạt động.
- Rollback chỉ cần xóa `run.bat` và tài liệu task này.

## Verification commands

```powershell
backend\mvnw.cmd -v
git diff --check -- run.bat backend/mvnw.cmd docs/tasks/dev-runner
git status --short
git diff -- run.bat backend/mvnw.cmd docs/tasks/dev-runner
```

Kiểm tra mở hai cửa sổ terminal là manual verification vì đây là hành vi GUI.
