# Implementation plan — Vite development server startup

## Current architecture

`frontend/vite.config.ts` gọi `defineConfig` từ
`@lovable.dev/vite-tanstack-config`. Wrapper tạo cấu hình bất đồng bộ, thêm các
plugin nội bộ rồi merge phần `vite` do dự án cung cấp.

## Proposed solution

Giữ wrapper làm nguồn cấu hình chính. Sau khi wrapper trả về `UserConfig`, lọc đúng
hai tên plugin tsconfig paths cũ và trả lại toàn bộ cấu hình còn lại. Bật resolver
native trong phần `resolve`, đồng thời đặt `strictPort: false`.

## Files to create

- `docs/tasks/vite-dev-server/spec.md`
- `docs/tasks/vite-dev-server/plan.md`
- `docs/tasks/vite-dev-server/test-plan.md`
- `docs/tasks/vite-dev-server/verification.md`

## Files to modify

- `frontend/vite.config.ts`
- `docs/TASKS.md`

## Reused components and configuration

- `@lovable.dev/vite-tanstack-config`
- Cấu hình proxy `/api` và `/actuator` hiện có
- Vite `ConfigEnv`, `PluginOption` và `UserConfig`

## Data, permissions and backend impact

- Không thay đổi data structure hoặc `localStorage`.
- Không thay đổi system role, active role hoặc project permission.
- Không thay đổi API, DTO, persistence, database hoặc profile `nodb`.

## Implementation order

1. Tạo bộ tài liệu task và ghi trạng thái `IN_PROGRESS`.
2. Đổi tên import wrapper để có thể tạo cấu hình trung gian.
3. Thêm type guard nhỏ nhận diện plugin paths cũ theo tên.
4. Bật `resolve.tsconfigPaths: true`.
5. Đặt `server.strictPort: false`.
6. Lọc plugin cũ khỏi cấu hình cuối và giữ nguyên các plugin khác.
7. Chạy typecheck, build và kiểm tra dev server.
8. Review diff, chạy `git diff --check`, cập nhật verification và trạng thái task.

## Error handling and backward compatibility

- Nếu wrapper không trả về `plugins`, cấu hình vẫn hợp lệ.
- Không mutate trực tiếp mảng plugin do wrapper trả về.
- Cổng ưu tiên, proxy, TanStack server entry và các plugin còn lại được giữ nguyên.
- Rollback bằng cách khôi phục export wrapper trực tiếp và `strictPort: true`.

## Verification commands

```powershell
npm --prefix frontend run typecheck
npm --prefix frontend run build
npm --prefix frontend run dev -- --host 127.0.0.1
git diff --check
git status --short
git diff -- frontend/vite.config.ts docs/TASKS.md docs/tasks/vite-dev-server
```
