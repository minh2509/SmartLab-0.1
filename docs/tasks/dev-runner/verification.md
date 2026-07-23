# Verification report — Two-terminal development runner

## Summary

Đã tạo lại `run.bat` để mở backend và frontend trong hai cửa sổ terminal
Windows độc lập.

## Created files

- `run.bat`
- Bộ tài liệu trong `docs/tasks/dev-runner/`

## Modified files

- `backend/mvnw.cmd`: sửa xử lý PowerShell `Target=null` để Maven Wrapper chạy
  được trên Windows hiện tại.

`scripts/backend.mjs` đã được khôi phục đúng nội dung ban đầu và không thuộc
diff của task.

## Behavior verified

- Có terminal title `SmartLab Backend`.
- Có terminal title `SmartLab Frontend`.
- Backend terminal dùng working directory `backend/`.
- Frontend terminal dùng working directory `frontend/`.
- Cả hai terminal dùng `cmd /k`, giữ cửa sổ mở sau khi process lỗi.
- Backend mặc định profile `nodb` khi chưa có profile.
- Profile do người dùng đặt không bị ghi đè.
- Launcher không dùng `concurrently`.

## Commands executed

Command:

```powershell
backend\mvnw.cmd -v
```

Result: pass, exit code `0`.

Relevant output:

```text
Apache Maven 3.9.16
Java version: 21.0.7
```

Command:

```powershell
# Static assertions against run.bat
```

Result: pass.

Command:

```powershell
git diff --check -- backend/mvnw.cmd run.bat docs/tasks/dev-runner
```

Result: pass; Git chỉ cảnh báo quy tắc chuyển LF sang CRLF trên Windows.

## Acceptance criteria

- [x] `run.bat` tồn tại tại repository root.
- [x] Hai terminal dùng title và working directory riêng.
- [x] Backend chạy Maven Wrapper trực tiếp.
- [x] Frontend chạy Vite qua script frontend hiện có.
- [x] Không dùng `concurrently`.
- [x] Có kiểm tra Node.js, npm, Java, Maven Wrapper và Vite.
- [x] Terminal giữ mở để xem log lỗi.
- [ ] Chưa tự động mở hai cửa sổ trong phiên verification vì đây là hành vi
      GUI nhìn thấy được; cần người dùng double-click để xác nhận trực quan.

## Manual verification

1. Double-click `run.bat`.
2. Xác nhận có hai cửa sổ: `SmartLab Backend` và `SmartLab Frontend`.
3. Xác nhận frontend tại `http://localhost:5173`.
4. Xác nhận backend tại `http://localhost:8080`.
5. Đóng từng terminal độc lập.

## Known limitations

- Port `5173` hoặc `8080` đang bị chiếm sẽ được báo trong terminal tương ứng.
- Profile `local` cần các biến database và JWT của backend.
- Profile mặc định `nodb` phù hợp cho development không cần database.

