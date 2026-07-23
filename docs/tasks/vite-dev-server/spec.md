# Specification — Vite development server startup

## Task overview

Loại bỏ cảnh báo plugin `vite-tsconfig-paths` trong cấu hình Vite cuối cùng và cho
frontend khởi động trên cổng khả dụng khi cổng mặc định `5173` đang bị chiếm.

## Problem statement

Frontend đang dùng Vite 8, vốn hỗ trợ `resolve.tsconfigPaths` native. Tuy nhiên
`@lovable.dev/vite-tanstack-config` vẫn tự thêm plugin `vite-tsconfig-paths`, khiến
Vite phát cảnh báo. Cấu hình đồng thời đặt `strictPort: true`, nên dev server dừng
hoàn toàn khi cổng `5173` đang được một process khác sử dụng.

## Goals

- Bật `resolve.tsconfigPaths: true`.
- Loại plugin paths cũ khỏi danh sách plugin cuối cùng mà không thay thế các plugin
  TanStack, React, Tailwind, Nitro và Lovable còn lại.
- Ưu tiên cổng `5173`, nhưng cho phép Vite chọn cổng tiếp theo khi cổng này bận.
- Không ảnh hưởng build production hoặc proxy backend.

## Non-goals

- Không tự động dừng process đang dùng cổng.
- Không thay đổi backend, API, dữ liệu, route, authentication hoặc authorization.
- Không thay thế toàn bộ wrapper Lovable.
- Không nâng cấp dependency.

## Current behavior

- Wrapper Lovable tự thêm plugin có tên `vite-tsconfig-paths`.
- `server.port` là `5173`.
- `server.strictPort` là `true`.
- Khi `5173` bận, Vite thoát với lỗi `Port 5173 is already in use`.

## Expected behavior

- Cấu hình cuối không còn plugin có tên `vite-tsconfig-paths` hoặc
  `vite-plugin-tsconfig-paths`.
- Vite dùng resolver tsconfig native.
- Khi `5173` trống, frontend chạy trên `5173`.
- Khi `5173` bận, frontend chạy trên cổng khả dụng tiếp theo và in URL thực tế.

## Acceptance criteria

- `npm --prefix frontend run typecheck` pass.
- `npm --prefix frontend run build` pass.
- Dev server không in cảnh báo plugin paths cũ.
- Dev server khởi động được khi `5173` trống.
- Một lần khởi động thứ hai không thất bại chỉ vì `5173` đã bận.
- Proxy `/api` và `/actuator` vẫn trỏ tới `http://localhost:8080`.

## Edge and error cases

- Plugin wrapper trả về các giá trị plugin không phải object.
- Plugin cũ dùng một trong hai tên mà Vite nhận diện.
- Cổng `5173` và một hoặc nhiều cổng kế tiếp cùng bị chiếm.
- Wrapper vẫn cần package `vite-tsconfig-paths` khi tạo cấu hình.

## Assumptions, dependencies and risks

- Vite hiện tại là phiên bản 8 và có `resolve.tsconfigPaths`.
- Wrapper Lovable hiện tại vẫn import `vite-tsconfig-paths`; dependency chưa thể gỡ
  khỏi `package.json` nếu không thay hoặc nâng cấp wrapper.
- Khi Vite chọn cổng khác, URL không còn cố định là `http://localhost:5173`; terminal
  Vite là nguồn chính xác cho URL runtime.
