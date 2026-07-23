# Test Plan: Admin Member & Lab Management Frontend
## Task: thanh-adm-027-041-frontend

---

## Test Matrix

### 1. `/app/admin/members` — Danh sách thành viên

| # | Test case | Expected |
|---|-----------|----------|
| 1 | Truy cập với `activeRole=admin` | Render bảng danh sách thành viên |
| 2 | Truy cập với `activeRole=member` hoặc `leader` | Hiện Unauthorized state |
| 3 | Danh sách rỗng | Hiện EmptyState với hint |
| 4 | Nhập search query khớp tên | Lọc đúng thành viên |
| 5 | Nhập search không khớp | EmptyState "no results" |
| 6 | Chọn Activity Status filter | Lọc đúng theo status |
| 7 | Xóa filter → reset | Danh sách đầy đủ trở lại |
| 8 | Loading state ban đầu | Hiện skeleton rows |
| 9 | Click một row thành viên | Drawer mở với loading skeleton |
| 10 | Drawer hiện đúng tên, email, roles | Data chính xác |

### 2. MemberDetailDrawer — Profile Tab

| # | Test case | Expected |
|---|-----------|----------|
| 11 | Form pre-filled với data thành viên | Các field có giá trị hiện tại |
| 12 | Submit form rỗng bắt buộc | Validation error inline |
| 13 | Submit với data hợp lệ | Button "Saving..." → toast success |
| 14 | Server error khi submit | Inline error banner |
| 15 | Chọn lĩnh vực nghiên cứu (multi) | Badge xuất hiện, có thể xóa |
| 16 | Thay đổi Activity Status | Phản ánh ngay |
| 17 | Đóng drawer bằng Escape hoặc nút ✕ | Drawer đóng, data reset |

### 3. MemberDetailDrawer — Projects Tab

| # | Test case | Expected |
|---|-----------|----------|
| 18 | Thành viên có dự án | Danh sách hiện tên project, vai trò, status |
| 19 | Thành viên không có dự án | EmptyState |

### 4. MemberDetailDrawer — Evaluations Tab

| # | Test case | Expected |
|---|-----------|----------|
| 20 | Thành viên có đánh giá | Danh sách hiện period, score, comment |
| 21 | Thành viên không có đánh giá | EmptyState |

### 5. `/app/admin/lab` — Lab Info Tab

| # | Test case | Expected |
|---|-----------|----------|
| 22 | Truy cập với `activeRole=admin` | Form pre-filled với thông tin Lab |
| 23 | Truy cập với `activeRole != admin` | Unauthorized state |
| 24 | Submit form thiếu trường bắt buộc (name) | Validation error |
| 25 | Submit form hợp lệ | Toast success, data cập nhật |
| 26 | Server error | Inline error |
| 27 | Tab Lab Images — nhập fileId logo | Save thành công |

### 6. `/app/admin/lab` — Research Fields Tab

| # | Test case | Expected |
|---|-----------|----------|
| 28 | Danh sách hiện ACTIVE/INACTIVE với badge | Đúng màu per status |
| 29 | Toggle switch ACTIVE → INACTIVE | Switch đổi ngay (optimistic) |
| 30 | Toggle thất bại | Revert switch, toast error |
| 31 | Click "Thêm mới" | Dialog tạo mở |
| 32 | Submit tạo mới với code trùng | Inline error "code exists" |
| 33 | Submit tạo mới hợp lệ | Dialog đóng, list cập nhật |
| 34 | Click "Sửa" | Dialog sửa mở với data pre-filled |
| 35 | Submit sửa hợp lệ | Dialog đóng, list cập nhật |
| 36 | Danh sách rỗng | EmptyState |

### 7. Responsive

| # | Test case | Expected |
|---|-----------|----------|
| 37 | Members page @ 360px | Table scroll ngang, drawer full screen |
| 38 | Lab page @ 360px | Form single column |
| 39 | Members page @ 768px | Table đầy đủ cột |
| 40 | Lab page @ 1280px | 2-column form layout |

### 8. Accessibility

| # | Test case | Expected |
|---|-----------|----------|
| 41 | Keyboard Tab qua form | Focus đúng thứ tự |
| 42 | Escape đóng Drawer | Drawer đóng, focus về nút mở |
| 43 | Icon buttons có aria-label | Screen reader đọc được |
| 44 | Status badges không chỉ dùng màu | Có text label |

---

## Build Verification

```bash
npm --prefix frontend run lint        # 0 errors
npm --prefix frontend run typecheck   # 0 errors
npm --prefix frontend run build       # Build thành công
```

Ghi kết quả vào `verification.md`.
