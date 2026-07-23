# Test plan — Vite development server startup

## TC-01 — Type safety

- Preconditions: Frontend dependencies đã được cài.
- Action: Chạy `npm --prefix frontend run typecheck`.
- Expected result: Không có lỗi TypeScript.

## TC-02 — Production build

- Preconditions: Frontend dependencies đã được cài.
- Action: Chạy `npm --prefix frontend run build`.
- Expected result: Build thành công và resolver native xử lý được alias hiện có.

## TC-03 — Cổng mặc định khả dụng

- Preconditions: Không có process lắng nghe cổng `5173`.
- Action: Khởi động frontend dev server.
- Expected result: Server chạy trên `5173` và không có cảnh báo plugin paths cũ.

## TC-04 — Cổng mặc định đang bận

- Preconditions: Một frontend dev server đang lắng nghe cổng `5173`.
- Action: Khởi động frontend dev server thứ hai.
- Expected result: Server thứ hai chọn cổng khả dụng tiếp theo thay vì thoát với lỗi
  `Port 5173 is already in use`.

## TC-05 — Plugin wrapper được bảo toàn

- Preconditions: Cấu hình được wrapper Lovable tạo thành công.
- Action: Kiểm tra cấu hình đã resolve hoặc log startup/build.
- Expected result: Chỉ plugin `vite-tsconfig-paths` cũ bị loại; TanStack, React,
  Tailwind, Nitro và plugin Lovable liên quan vẫn hoạt động.

## TC-06 — Proxy regression

- Preconditions: Dev server đã khởi động.
- Action: Kiểm tra cấu hình proxy.
- Expected result: `/api` và `/actuator` vẫn dùng target
  `http://localhost:8080`.

## TC-07 — Git hygiene

- Preconditions: Implementation và verification đã hoàn tất.
- Action: Chạy `git diff --check`, `git status --short` và review diff giới hạn task.
- Expected result: Không có whitespace error và không ghi đè thay đổi không liên quan.

## Manual verification

1. Chạy dev server và đọc URL được Vite in ra.
2. Mở URL đó, xác nhận trang frontend render.
3. Giữ server đầu tiên chạy và khởi động server thứ hai.
4. Xác nhận server thứ hai dùng cổng khác và không in cảnh báo paths cũ.
