# Test plan — Development runner

## TC-01 — Chạy từ repository root

- Preconditions: Node.js, npm, root dependencies và frontend dependencies đã
  được cài.
- Input: Chạy `cmd /c run.bat` tại repository root.
- Expected result: Hai terminal có title `SmartLab Backend` và
  `SmartLab Frontend` được mở.

## TC-02 — Chạy từ working directory khác

- Preconditions: Giống TC-01.
- Input: Chuyển đến thư mục khác và gọi `run.bat` bằng absolute path.
- Expected result: Script tự chuyển về repository root và tìm đúng
  `package.json`.

## TC-03 — Thiếu Node.js

- Preconditions: `node` không có trong `PATH`.
- Input: Chạy `run.bat`.
- Expected result: Script in lỗi rõ ràng và trả exit code `1`.

## TC-04 — Thiếu npm

- Preconditions: `npm` không có trong `PATH`.
- Input: Chạy `run.bat`.
- Expected result: Script in lỗi rõ ràng và trả exit code `1`.

## TC-05 — Thiếu root dependencies

- Preconditions: Không có `node_modules/.bin/concurrently.cmd`.
- Input: Chạy `run.bat`.
- Expected result: Script hướng dẫn chạy `npm install`, không tự tải package và
  trả exit code `1`.

## TC-06 — Thiếu frontend dependencies

- Preconditions: Root dependencies tồn tại nhưng không có
  `frontend/node_modules/.bin/vite.cmd`.
- Input: Chạy `run.bat`.
- Expected result: Script hướng dẫn chạy `npm --prefix frontend install` và trả
  exit code `1`.

## TC-07 — Terminal độc lập

- Preconditions: Hai terminal đã mở.
- Input: Đóng hoặc dừng một terminal.
- Expected result: Terminal còn lại không bị launcher chủ động dừng.

## TC-08 — Shutdown

- Preconditions: Cả hai process đã chạy.
- Input: Nhấn `Ctrl+C`.
- Expected result: Chỉ process trong terminal đang thao tác dừng; terminal còn
  lại tiếp tục độc lập.

## TC-09 — Maven user home không phải symlink

- Preconditions: `$HOME/.m2` hoặc Maven user home là thư mục thường và thuộc
  tính PowerShell `Target` trả về `null`.
- Input: Chạy `backend\mvnw.cmd -v`.
- Expected result: Wrapper không truy cập chỉ mục của giá trị `null`, tìm hoặc
  tải Maven distribution và in Maven version.

## TC-10 — Backend runner trên Windows

- Preconditions: Java 21 và Maven Wrapper distribution khả dụng.
- Input: Chạy `node scripts/backend.mjs build`.
- Expected result: Backend runner gọi Maven Wrapper thành công; không xuất hiện
  `Cannot index into a null array`.

## TC-11 — Development profile mặc định

- Preconditions: Không có biến môi trường `SPRING_PROFILES_ACTIVE`.
- Input: Chạy `node scripts/backend.mjs dev`.
- Expected result: Backend khởi động với profile `nodb`.

## TC-12 — Giữ profile do người dùng cấu hình

- Preconditions: `SPRING_PROFILES_ACTIVE=local`.
- Input: Chạy `node scripts/backend.mjs dev`.
- Expected result: Backend nhận profile `local`; runner không ghi đè bằng
  `nodb`.

## TC-13 — Không dùng shell mode không an toàn

- Preconditions: Windows và Node.js hiện tại.
- Input: Chạy backend runner.
- Expected result: Không xuất hiện Node deprecation warning `DEP0190` về
  `shell: true`.

## Regression checks

- `npm run dev:frontend` không thay đổi.
- `npm run dev:backend` không thay đổi.
- `npm run dev` không thay đổi.
- Không có file frontend/backend bị chỉnh sửa.

## Manual verification

1. Double-click `run.bat`.
2. Xác nhận log có nhãn `FRONTEND` và `BACKEND`.
3. Mở `http://localhost:5173`.
4. Kiểm tra backend tại endpoint health/API phù hợp với cấu hình hiện tại.
5. Nhấn `Ctrl+C` và xác nhận hai process dừng.
