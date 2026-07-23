# Specification: ADM-027–041 Member & Lab Management API

## Task Overview

Task group: ADM-027 đến ADM-041  
Assignee: Thành  
Branch: `feature/admin-member-lab`  
Sprint: Sprint 1  
Phase: MVP  

Nhóm task này triển khai Admin REST API cho các module:
1. **Member Management** (ADM-027–033): Quản lý hồ sơ thành viên trong Lab.
2. **Lab Management** (ADM-034–037): Xem và cập nhật thông tin Lab.
3. **Research Field Management** (ADM-038–041): Quản lý danh mục lĩnh vực nghiên cứu.

---

## Problem Statement

Quản trị viên cần có công cụ để:
- Xem, tìm kiếm và cập nhật hồ sơ chi tiết của từng thành viên (profile, lĩnh vực nghiên cứu, trạng thái hoạt động).
- Xem thông tin Lab và cập nhật nội dung cũng như hình ảnh đại diện (logo, cover).
- Quản lý danh mục các lĩnh vực nghiên cứu của Lab (tạo, cập nhật, bật/tắt).

Hiện tại chưa có Controller, Service, DTO hoặc Mapper nào cho các module này.

---

## Goals

- Cung cấp đầy đủ 15 endpoint trong phạm vi phân công.
- Chỉ Admin/SuperAdmin mới được phép truy cập tất cả endpoint.
- Tất cả các thao tác đọc/ghi đều được kiểm soát theo Lab Scope.
- Không lộ `password_hash`, dữ liệu nhạy cảm hoặc entity JPA ra API.

---

## Non-Goals

- Quản lý `users` hoặc `user_roles` trực tiếp (thuộc Tuấn Đạt).
- Upload file thực tế (chỉ nhận `fileId` của file đã tồn tại trong bảng `files`).
- Tạo file upload storage service thực.
- Frontend changes.
- Flyway migrations.

---

## Actors

- **ADMIN / SUPER_ADMIN**: Toàn quyền truy cập tất cả endpoint.
- **LEADER / MEMBER / Unauthenticated**: Bị từ chối – HTTP 401 hoặc 403.

---

## Current Behavior

Không có AdminMemberController, AdminLabController hay service tương ứng.

---

## Expected Behavior

### Member Management

| Task | Method | Endpoint | Chức năng |
|------|--------|----------|-----------|
| ADM-027 | GET | `/api/admin/members` | Danh sách thành viên (có thể filter/page) |
| ADM-028 | GET | `/api/admin/members/{userId}` | Chi tiết hồ sơ thành viên |
| ADM-029 | PUT | `/api/admin/members/{userId}/profile` | Cập nhật hồ sơ thành viên |
| ADM-030 | PUT | `/api/admin/members/{userId}/research-fields` | Cập nhật lĩnh vực nghiên cứu |
| ADM-031 | PATCH | `/api/admin/members/{userId}/activity-status` | Cập nhật trạng thái hoạt động |
| ADM-032 | GET | `/api/admin/members/{userId}/projects` | Danh sách project thành viên tham gia |
| ADM-033 | GET | `/api/admin/members/{userId}/evaluations` | Lịch sử đánh giá thành viên |

### Lab Management

| Task | Method | Endpoint | Chức năng |
|------|--------|----------|-----------|
| ADM-034 | GET | `/api/admin/lab` | Lấy thông tin Lab |
| ADM-035 | PUT | `/api/admin/lab` | Cập nhật thông tin Lab |
| ADM-036 | POST | `/api/admin/lab/logo` | Cập nhật logo Lab (nhận fileId) |
| ADM-037 | POST | `/api/admin/lab/cover` | Cập nhật cover Lab (nhận fileId) |

### Research Field Management

| Task | Method | Endpoint | Chức năng |
|------|--------|----------|-----------|
| ADM-038 | GET | `/api/admin/research-fields` | Danh sách lĩnh vực nghiên cứu |
| ADM-039 | POST | `/api/admin/research-fields` | Tạo lĩnh vực nghiên cứu mới |
| ADM-040 | PUT | `/api/admin/research-fields/{fieldId}` | Cập nhật lĩnh vực nghiên cứu |
| ADM-041 | PATCH | `/api/admin/research-fields/{fieldId}/status` | Bật/tắt trạng thái |

---

## Functional Requirements

### Member Management
- `GET /api/admin/members`: Trả về danh sách user (có `deletedAt IS NULL`) thuộc Lab của actor. Hỗ trợ phân trang (page, size). Trả về các trường cơ bản của User + profile nếu có.
- `GET /api/admin/members/{userId}`: Trả về chi tiết User + MemberProfile + ResearchFields + ProjectMemberships + MemberEvaluations tổng hợp.
- `PUT /api/admin/members/{userId}/profile`: Cập nhật các trường trong `MemberProfile`. Tạo mới profile nếu chưa có. Không sửa bảng `users`.
- `PUT /api/admin/members/{userId}/research-fields`: Thay thế toàn bộ danh sách `MemberResearchField` bằng danh sách `fieldIds` mới.
- `PATCH /api/admin/members/{userId}/activity-status`: Cập nhật `activityStatus` trong `MemberProfile`.
- `GET /api/admin/members/{userId}/projects`: Trả về danh sách `ProjectMember` của user đó.
- `GET /api/admin/members/{userId}/evaluations`: Trả về danh sách `MemberEvaluation` của user đó.

### Lab Management
- `GET /api/admin/lab`: Trả về thông tin Lab của actor.
- `PUT /api/admin/lab`: Cập nhật `name`, `description`, `mission`, `vision`, `contactEmail`, `websiteUrl`.
- `POST /api/admin/lab/logo`: Nhận `fileId`, xác nhận file thuộc Lab, gán vào `lab.logoFile`.
- `POST /api/admin/lab/cover`: Tương tự cho `lab.coverFile`.

### Research Field Management
- `GET /api/admin/research-fields`: Trả về tất cả ResearchField (không phân trang, vì danh sách nhỏ).
- `POST /api/admin/research-fields`: Tạo mới ResearchField với code duy nhất trong Lab.
- `PUT /api/admin/research-fields/{fieldId}`: Cập nhật `name`, `description`. Không sửa `code`.
- `PATCH /api/admin/research-fields/{fieldId}/status`: Chuyển `status` giữa `ACTIVE` và `INACTIVE`.

---

## Business Rules

- Mọi thao tác đọc/ghi đều bị giới hạn trong phạm vi Lab của Actor.
- `User.deletedAt IS NOT NULL` → không hiển thị trong danh sách thành viên.
- Không bao giờ expose `password_hash`.
- `MemberProfile` có quan hệ 1-1 với `User`. Nếu chưa có, endpoint UPDATE tạo mới.
- Khi thay thế ResearchFields của thành viên: xóa toàn bộ cũ, tạo mới theo danh sách mới.
- `ResearchField` khi `INACTIVE` không bị xóa. Mapping với Member/Project cũ vẫn giữ nguyên.
- Code của ResearchField phải duy nhất toàn hệ thống.
- `Lab.code` không được thay đổi qua API.
- File logo/cover phải thuộc cùng Lab mới được gán.

---

## Authorization Rules

- Tất cả endpoint yêu cầu JWT Bearer Token hợp lệ.
- ADMIN / SUPER_ADMIN: Được phép toàn bộ.
- LEADER / MEMBER: HTTP 403 Forbidden.
- Unauthenticated: HTTP 401 Unauthorized.

---

## Validation Rules

- `userId`, `fieldId`: UUID hợp lệ, tồn tại trong Lab của actor.
- `page`: ≥ 0. `size`: 1–100. Default: page=0, size=20.
- `activityStatus`: Phải là giá trị hợp lệ trong enum `MemberProfileActivityStatus`.
- `fieldIds` (ADM-030): List UUID, mỗi phần tử phải tồn tại trong bảng `research_fields`.
- `ResearchField.code`: Not blank, tối đa 50 ký tự, unique.
- `ResearchField.name`: Not blank, tối đa 150 ký tự.
- `Lab.name`: Not blank, tối đa 255 ký tự.
- `Lab.contactEmail`: Format email hợp lệ nếu cung cấp.
- File logo/cover: `fileId` phải tồn tại và thuộc cùng Lab.

---

## Acceptance Criteria

- [ ] ADM-027: `GET /api/admin/members` → 200, danh sách members thuộc Lab, không có deletedAt, phân trang đúng.
- [ ] ADM-028: `GET /api/admin/members/{userId}` → 200 với đầy đủ profile + fields + projects + evaluations.
- [ ] ADM-029: `PUT /api/admin/members/{userId}/profile` → 200, cập nhật MemberProfile.
- [ ] ADM-030: `PUT /api/admin/members/{userId}/research-fields` → 200, thay thế toàn bộ fields.
- [ ] ADM-031: `PATCH /api/admin/members/{userId}/activity-status` → 200, cập nhật activityStatus.
- [ ] ADM-032: `GET /api/admin/members/{userId}/projects` → 200, danh sách project memberships.
- [ ] ADM-033: `GET /api/admin/members/{userId}/evaluations` → 200, danh sách evaluations.
- [ ] ADM-034: `GET /api/admin/lab` → 200, thông tin Lab của actor.
- [ ] ADM-035: `PUT /api/admin/lab` → 200, cập nhật được Lab info.
- [ ] ADM-036: `POST /api/admin/lab/logo` → 200, gán logo.
- [ ] ADM-037: `POST /api/admin/lab/cover` → 200, gán cover.
- [ ] ADM-038: `GET /api/admin/research-fields` → 200, danh sách fields.
- [ ] ADM-039: `POST /api/admin/research-fields` → 201, tạo field mới.
- [ ] ADM-040: `PUT /api/admin/research-fields/{fieldId}` → 200, cập nhật field.
- [ ] ADM-041: `PATCH /api/admin/research-fields/{fieldId}/status` → 200, toggle status.
- [ ] Tất cả endpoint trả về 401 khi không authenticated.
- [ ] Tất cả endpoint trả về 403 khi MEMBER hoặc LEADER gọi.
- [ ] Không expose entity JPA, password_hash, hoặc dữ liệu ngoài Lab.

---

## Edge Cases

- Thành viên chưa có `MemberProfile` → ADM-029 tạo mới profile.
- `fieldIds` rỗng trong ADM-030 → Xóa toàn bộ MemberResearchField, trả về list rỗng.
- `fieldId` không tồn tại → 404 Not Found.
- `userId` không thuộc Lab của actor → 404 Not Found.
- `ResearchField.code` trùng → 409 Conflict.
- File logo/cover không thuộc Lab → 400/422.

---

## Dependencies

- Entities: `User`, `Lab`, `MemberProfile`, `MemberResearchField`, `ResearchField`, `ProjectMember`, `MemberEvaluation`, `File` – đã tồn tại.
- Repositories: `UserRepository`, `MemberProfileRepository`, `MemberResearchFieldRepository`, `ResearchFieldRepository`, `ProjectMemberRepository`, `MemberEvaluationRepository`, `LabRepository`, `FileRepository` – đã tồn tại.
- `AdminRolePolicy.requireAdminActor()` – đã tồn tại.
- `AuthenticatedActorResolver.requireActorUserId()` – đã tồn tại.
