# Specification — Two-terminal development runner

## Task overview

Tạo một file chạy tại repository root để mở frontend và backend của SmartLab
trong hai terminal Windows độc lập.

## Problem statement

Repository đã có `npm run dev` nhưng người dùng cần một file có thể chạy trực
tiếp mà không phải nhớ command hoặc tự chuyển đến repository root.

## Goals

- Cung cấp một file `run.bat` tại repository root.
- Mở một terminal riêng cho frontend và một terminal riêng cho backend.
- Hoạt động khi được gọi từ một working directory khác.
- Báo lỗi dễ hiểu khi thiếu công cụ hoặc dependencies.
- Giữ terminal mở khi process lỗi để người dùng đọc log.

## Non-goals

- Không dùng `concurrently` hoặc terminal dùng chung.
- Không tự động cài dependencies.
- Không ghi đè Spring profile do người dùng cung cấp.
- Không thêm dependency.
- Không tạo background service hoặc tự mở trình duyệt.

## Current behavior

Root `package.json` có:

- `dev:frontend`: chạy Vite trên port `5173`.
- `dev:backend`: chạy `scripts/backend.mjs dev`.
- `dev`: dùng `concurrently` để chạy cả hai process.

Người dùng phải tự mở terminal tại repository root và chạy `npm run dev`.

Trên Windows hiện tại, frontend khởi động thành công nhưng backend thất bại
trong `backend/mvnw.cmd` vì Maven Wrapper truy cập phần tử đầu tiên của thuộc
tính `Target` khi thuộc tính này là `null` đối với thư mục home không phải
symlink. `concurrently --kill-others-on-fail` sau đó dừng cả frontend.

Sau khi sửa Maven Wrapper, backend tiếp tục thất bại khi không có profile đang
active vì datasource chỉ được cấu hình trong `application-local.yml`. Dự án đã
có `application-nodb.yml` cho development không phụ thuộc database.

## Expected behavior

Khi chạy `run.bat`:

1. Script chuyển working directory về thư mục chứa chính nó.
2. Kiểm tra Node.js, npm, Java, Maven Wrapper và frontend dependencies.
3. Nếu chưa có `SPRING_PROFILES_ACTIVE`, đặt mặc định `nodb`.
4. Mở terminal `SmartLab Backend` tại `backend/` và chạy
   `mvnw.cmd spring-boot:run`.
5. Mở terminal `SmartLab Frontend` tại `frontend/` và chạy
   `npm run dev -- --port 5173`.
6. Hai terminal hoạt động độc lập và dùng `cmd /k` để giữ log trên màn hình.

## Acceptance criteria

- `run.bat` tồn tại tại repository root.
- Có thể gọi `run.bat` từ một thư mục khác.
- Backend và frontend mở trong hai terminal riêng.
- Thiếu Node/npm hoặc dependencies tạo thông báo có hướng dẫn.
- Không sửa source code frontend/backend.
- Không tự cài dependency hoặc thay đổi môi trường.
- Maven Wrapper chạy được khi Maven user home là thư mục thường, không phải
  symlink.

## Edge and error cases

- `node` không có trong `PATH`.
- `npm` không có trong `PATH`.
- Frontend dependencies chưa được cài.
- Một terminal hoặc process thoát với lỗi mà terminal còn lại vẫn độc lập.
- Đường dẫn repository có khoảng trắng.
- Thuộc tính PowerShell `FileSystemInfo.Target` là `null`.
- Không có `SPRING_PROFILES_ACTIVE`.
- Người dùng đã đặt profile riêng và runner không được ghi đè.

## Assumptions and dependencies

- Môi trường mục tiêu là Windows.
- Node.js và npm là công cụ bắt buộc của monorepo.
- `npm --prefix frontend install` được người dùng thực hiện trước lần chạy đầu
  tiên.
- Maven Wrapper là source of truth cho backend; frontend `package.json` là
  source of truth cho frontend.

## Risks

- Backend dùng `nodb` để chạy không cần database nếu người dùng chưa chọn
  profile khác. Profile `local` vẫn cần database và secrets tương ứng.
- Port `5173` hoặc `8080` đang được sử dụng sẽ khiến process tương ứng thất bại.
