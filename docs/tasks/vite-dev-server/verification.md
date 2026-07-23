# Verification report — Vite development server startup

## Summary

- Bật `resolve.tsconfigPaths: true`.
- Loại plugin tsconfig paths cũ khỏi cấu hình cuối sau khi wrapper Lovable hoàn tất.
- Giữ nguyên wrapper và toàn bộ plugin/config còn lại.
- Cho phép Vite tự chọn cổng khả dụng khi `5173` đang bận.

## Changed files

- `frontend/vite.config.ts`
- `docs/TASKS.md`
- `docs/tasks/vite-dev-server/spec.md`
- `docs/tasks/vite-dev-server/plan.md`
- `docs/tasks/vite-dev-server/test-plan.md`
- `docs/tasks/vite-dev-server/verification.md`

## Commands executed

Command:

```powershell
npm --prefix frontend run typecheck
```

Result: pass, exit code `0`.

Relevant output: `tsc --noEmit`.

Command:

```powershell
npm --prefix frontend run build
```

Result: pass, exit code `0`.

Relevant output: Vite client, SSR và Nitro production build hoàn tất.

Command:

```powershell
node node_modules\vite\bin\vite.js --host 127.0.0.1
```

Result: runtime verification pass; command bị test harness dừng theo timeout sau khi
server đã báo ready.

Relevant output khi cổng trống:

```text
VITE v8.1.4 ready
Local: http://127.0.0.1:5173/
```

Relevant output khi cổng `5173` bận:

```text
Port 5173 is in use, trying another one...
VITE v8.1.4 ready
Local: http://127.0.0.1:5174/
```

Không có cảnh báo `The plugin "vite-tsconfig-paths" is detected` trong cả hai lần.

Command:

```powershell
npm --prefix frontend run lint
```

Result: fail do baseline repository.

Relevant output: Prettier báo `Delete ␍` trên nhiều file CRLF có sẵn, bắt đầu từ
`frontend/eslint.config.js`, `frontend/src/components/app/AppShell.tsx` và nhiều
route/component ngoài phạm vi task.

Command:

```powershell
npx eslint vite.config.ts
```

Result: pass, exit code `0`, sau khi format riêng file thay đổi bằng Prettier.

Command:

```powershell
git diff --check
```

Result: pass; chỉ có cảnh báo Git về việc có thể chuyển LF sang CRLF trên Windows,
không có whitespace error.

## Acceptance criteria

- [x] Resolver tsconfig native được bật.
- [x] Plugin paths cũ không còn trong cấu hình plugin cuối.
- [x] Typecheck pass.
- [x] Production build pass.
- [x] Dev server chạy trên `5173` khi cổng trống.
- [x] Dev server fallback sang `5174` khi `5173` bận.
- [x] Proxy backend được giữ nguyên.
- [x] ESLint riêng file thay đổi pass.
- [ ] Lint toàn frontend chưa pass do lỗi line ending có sẵn ngoài phạm vi task.

## Known limitations and risks

- Package `vite-tsconfig-paths` vẫn phải nằm trong dependencies vì wrapper Lovable
  phiên bản hiện tại khai báo peer dependency và import package trong quá trình tạo
  config. Plugin được loại khỏi cấu hình Vite cuối, nên không chạy và không tạo cảnh
  báo.
- Khi cổng fallback được dùng, người phát triển cần đọc URL thực tế do Vite in ra;
  dòng thông báo tĩnh trong `run.bat` vẫn nhắc URL ưu tiên `5173`.
- Không thay đổi hoặc format các file CRLF ngoài phạm vi để tránh ghi đè công việc
  đang tồn tại.
