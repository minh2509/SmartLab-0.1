# Spec: Admin — Member & Lab Management Frontend
## Task: thanh-adm-027-041-frontend

---

## 1. Product Context

**Primary user**: Admin (role `admin`)  
**Goal**: Quản lý thành viên lab (xem hồ sơ, chỉnh sửa, đánh giá), quản lý thông tin Lab và danh mục lĩnh vực nghiên cứu.  
**Hành động quan trọng nhất**:
- Xem danh sách thành viên → click xem chi tiết hồ sơ
- Cập nhật profile thành viên (thông tin cá nhân, lĩnh vực, trạng thái hoạt động)
- Cập nhật thông tin Lab và logo/cover
- Quản lý danh mục lĩnh vực nghiên cứu

**Permission**: Chỉ admin có quyền truy cập. Khi activeRole ≠ admin → hiện unauthorized state.

---

## 2. Tech Stack (hiện tại của project)

| Item | Technology |
|------|-----------|
| Framework | React 19 + TypeScript |
| Routing | TanStack Router (file-based: `app.*.tsx`) |
| Styling | Tailwind CSS v4 |
| UI primitives | Radix UI (Dialog, Tabs, Select, Switch, etc.) |
| Icons | Lucide React |
| Forms | React Hook Form + Zod |
| State | localStorage mock (hiện tại) / sẽ kết nối API thật |
| API client | `apiRequest` từ `@/lib/api-client.ts` |
| Toast | Sonner |
| Auth | `useAuth()` từ `@/lib/auth` |
| Shared UI | `PageHeader`, `Panel`, `EmptyState`, `StatusPill` từ `@/components/app/ui` |

---

## 3. Pages & Routes

### Trang 1: Quản lý Thành viên
- **Route**: `/app/admin/members`
- **File**: `frontend/src/routes/app.admin.members.tsx`
- **Nav label**: "Members"

### Trang 2: Cài đặt Lab & Lĩnh vực Nghiên cứu
- **Route**: `/app/admin/lab`  
- **File**: `frontend/src/routes/app.admin.lab.tsx`
- **Nav label**: "Lab Settings"

---

## 4. Phân tích yêu cầu từng trang

### 4.1 Trang Quản lý Thành viên (`/app/admin/members`)

**Mục tiêu người dùng**:
1. Xem nhanh toàn bộ thành viên (tên, email, trạng thái, vai trò)
2. Click vào thành viên để xem chi tiết đầy đủ (profile, lĩnh vực nghiên cứu, dự án tham gia, lịch sử đánh giá)
3. Chỉnh sửa thông tin profile, lĩnh vực, trạng thái hoạt động

**Layout**:
- Header với tiêu đề, stat tiles nhỏ (tổng số, đang hoạt động, cựu thành viên)
- Filter bar: Search (fullName), bộ lọc Activity Status
- Bảng danh sách thành viên với horizontal scroll trên mobile
- Khi click row → mở Drawer bên phải hiển thị chi tiết đầy đủ

**Drawer chi tiết thành viên**:
- Tab 1: Thông tin hồ sơ (profile form, research fields)
- Tab 2: Dự án tham gia (read-only list)
- Tab 3: Đánh giá (read-only list)

### 4.2 Trang Lab Settings (`/app/admin/lab`)

**Mục tiêu người dùng**:
1. Xem thông tin hiện tại của Lab
2. Chỉnh sửa thông tin (tên, mô tả, sứ mệnh, tầm nhìn, email, website)
3. Cập nhật logo và cover (nhập file ID)
4. Quản lý danh mục lĩnh vực nghiên cứu (CRUD + bật/tắt status)

**Layout**: 2 Tab
- Tab 1: "Thông tin Lab" — form chỉnh sửa + khu vực hình ảnh
- Tab 2: "Lĩnh vực nghiên cứu" — bảng danh mục + nút thêm mới + bật/tắt từng mục

---

## 5. UX Flow

### Flow: Xem & chỉnh sửa thành viên
```
Admin vào /app/admin/members
→ Trang load danh sách (skeleton loading)
→ Hiện bảng thành viên với filter
→ Admin search hoặc filter theo activity status
→ Click một dòng → Drawer mở với thông tin chi tiết (skeleton)
→ Admin chỉnh sửa form → Submit
→ Saving state (button disabled, spinner)
→ Thành công → toast "Cập nhật thành công" → Drawer tự refresh data
→ Thất bại → Hiển thị inline error
```

### Flow: Cập nhật thông tin Lab
```
Admin vào /app/admin/lab → Tab "Thông tin Lab"
→ Form hiện sẵn data hiện tại (pre-filled)
→ Admin chỉnh sửa → Submit
→ Saving state
→ Thành công → toast confirm
→ Thất bại → inline error
```

### Flow: Quản lý lĩnh vực nghiên cứu
```
Admin vào Tab "Lĩnh vực nghiên cứu"
→ Danh sách hiển thị code, tên, status badge, ngày tạo
→ Nút "Thêm mới" → Dialog nhỏ
→ Toggle switch bật/tắt từng mục → optimistic update
→ Nút "Sửa" → Dialog sửa tên/mô tả
```

---

## 6. States cần xử lý

| State | Tất cả screens |
|-------|---------------|
| Loading (skeleton) | ✓ |
| Empty | ✓ |
| Error (inline banner) | ✓ |
| Success (toast) | ✓ |
| Unauthorized (admin only) | ✓ |
| Submitting (disabled button) | ✓ |

---

## 7. Mock Data Strategy

**Hiện tại**: Dùng mock data vì backend ở `!nodb` profile (cần PostgreSQL để chạy thật).  
Mock data sẽ được đặt trong:
- `frontend/src/lib/members-data.ts` — mock thành viên
- `frontend/src/lib/admin-lab-data.ts` — mock thông tin lab & research fields

**Design**: Tách mock rõ ràng, dễ thay bằng API call thật.

---

## 8. Assumptions

1. Backend chưa kết nối DB nên dùng mock data trước; phần kết nối API để comment TODO.
2. Upload file ảnh chưa triển khai trong scope này; dùng text input cho fileId.
3. Không thêm route param `/:userId` cho member detail — dùng Drawer pattern giống projects.
4. Tái sử dụng `StatusPill`, `EmptyState`, `PageHeader`, `Panel` hiện có.
5. Thêm 2 nav items mới trong AppShell cho admin.
