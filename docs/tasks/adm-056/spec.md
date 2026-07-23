# Specification: ADM-056 Admin Pending Post Queue API

## Task Overview
Task ID: ADM-056  
Task Name: Admin Pending Post Queue API (`GET /api/admin/posts/pending`)  
Assignee: Minh  
Target Branch: `feature/minh-admin-pending-posts`  

API này cung cấp danh sách bài viết đang chờ quản trị viên duyệt (`PENDING_REVIEW`) dành cho Quản trị viên (ADMIN hoặc SUPER_ADMIN) theo phạm vi Lab của người dùng hiện tại.

## Problem Statement
Khi người dùng thuộc Lab gửi bài viết để duyệt (`SUBMIT`), bài viết sẽ chuyển sang trạng thái `PENDING_REVIEW`. Quản trị viên cần một danh sách ưu tiên theo thời gian gửi duyệt (từ cũ nhất đến mới nhất) để kiểm duyệt kịp thời mà không bị lẫn lộn với các bài viết nháp (`DRAFT`), đã xuất bản (`PUBLISHED`), hoặc bài viết bị từ chối (`REJECTED`).

## Goals
- Cung cấp endpoint `GET /api/admin/posts/pending` cho ADMIN/SUPER_ADMIN.
- Chỉ trả về các bài viết thuộc Lab của người dùng đăng nhập, chưa bị xóa mềm (`deleted_at IS NULL`) và có trạng thái `moderation_status = 'PENDING_REVIEW'`.
- Sắp xếp ưu tiên:
  1. Bài viết có thời gian gửi duyệt (`SUBMIT`) gần nhất cũ hơn lên trước (tăng dần theo thời gian gửi `SUBMIT` mới nhất).
  2. Bài viết không có log `SUBMIT` xếp phía sau (xếp theo thời gian tạo hoặc `post.id`).
  3. Sử dụng `post.id ASC` làm tie-breaker để đảm bảo phân trang ổn định.
- Tái sử dụng contract phân trang `AdminPostPageResponse` và `AdminPostSummaryResponse` từ ADM-055 mà không làm thay đổi contract API cũ.

## Non-Goals
- Không thay đổi contract API `GET /api/admin/posts` hiện tại của ADM-055.
- Không thêm API duyệt/từ chối/yêu cầu sửa bài viết trong scope task này.
- Không tạo DB migration mới hay chỉnh sửa bảng DB.
- Không thay đổi frontend trong scope task này.

## Actors
- **ADMIN / SUPER_ADMIN**: Có quyền xem danh sách chờ duyệt của Lab mình.
- **Unauthenticated / Non-Admin Users**: Bị từ chối truy cập (HTTP 401 / 403).

## Current Behavior
- ADM-055 cung cấp `GET /api/admin/posts` cho phép lọc theo `status`, nhưng sắp xếp theo `createdAt DESC, id DESC`. Chưa có API riêng tối ưu việc sắp xếp theo thứ tự gửi duyệt (`SUBMIT` log timestamp ASC).

## Expected Behavior
- Gọi `GET /api/admin/posts/pending` với Bearer Token của Admin/SuperAdmin thuộc Lab.
- Trả về HTTP 200 kèm danh sách phân trang các bài viết `PENDING_REVIEW` thuộc Lab đó.
- Sắp xếp đúng theo thứ tự thời gian nộp duyệt tăng dần (`latest submit timestamp ASC`, sau đó là các bài viết thiếu log submit, rồi `post.id ASC`).

## Functional Requirements
1. **Endpoint Authorization**:
   - `GET /api/admin/posts/pending` yêu cầu người dùng authenticated và có quyền ADMIN/SUPER_ADMIN.
2. **Lab Isolation**:
   - Chỉ truy xuất các bài viết thuộc `Lab` của actor.
3. **Filtering Rules**:
   - Chỉ lấy các bài viết có `deletedAt IS NULL`.
   - Chỉ lấy bài viết có `moderationStatus = PENDING_REVIEW`.
4. **Pagination**:
   - Tham số query `page` (default: 0, min: 0).
   - Tham số query `size` (default: 20, min: 1, max: 100).
5. **Ordering Logic**:
   - Lấy thời gian log `created_at` của hành động `SUBMIT` mới nhất cho từng post `PENDING_REVIEW`.
   - Bài viết nào gửi duyệt trước (timestamp nhỏ hơn) xếp trước.
   - Bài viết nào không có log `SUBMIT` xếp sau.
   - Tie-breaker: `post.id ASC`.

## Business Rules
- ADMIN của Lab A không được thấy bài viết thuộc Lab B.
- Bài viết `DRAFT`, `APPROVED`, `PUBLISHED`, `NEEDS_REVISION`, `REJECTED` bị loại trừ.
- Bài viết đã bị xóa mềm (`deleted_at` không NULL) bị loại trừ.

## Authorization Rules
- Authenticated user thiếu vai trò ADMIN / SUPER_ADMIN -> HTTP 403 Forbidden.
- Unauthenticated user -> HTTP 401 Unauthorized.

## Validation Rules
- `page < 0`: HTTP 400 Bad Request.
- `size < 1` hoặc `size > 100`: HTTP 400 Bad Request.

## Acceptance Criteria
- [ ] Endpoint `GET /api/admin/posts/pending` trả về HTTP 200 cho ADMIN / SUPER_ADMIN.
- [ ] Dữ liệu trả về đúng định dạng `AdminPostPageResponse`.
- [ ] Chỉ chứa bài viết `PENDING_REVIEW` chưa bị xoá mềm thuộc cùng Lab.
- [ ] Thứ tự bài viết đúng theo `latest SUBMIT log timestamp ASC`, bài viết thiếu log nộp xếp sau, tie-breaker `id ASC`.
- [ ] Không lộ entity JPA, thông tin nhạy cảm (email, password hash, v.v.).
- [ ] Phân trang mặc định page=0, size=20. Trả về 400 nếu tham số sai range.
- [ ] Toàn bộ unit test và integration test pass (Spring profiles `nodb` và PostgreSQL runtime).

## Edge Cases
- Bài viết chuyển sang `PENDING_REVIEW` nhiều lần (có nhiều log `SUBMIT`): Lấy log `SUBMIT` có `created_at` lớn nhất của bài viết đó làm căn cứ sắp xếp.
- Bài viết `PENDING_REVIEW` nhưng không có log `SUBMIT` nào (data cũ/mock): Đưa về cuối danh sách, sắp xếp bằng `post.id ASC`.

## Out-of-Scope Items
- Chi tiết bài viết bài viết (`GET /api/admin/posts/{id}`).
- Các thao tác chuyển đổi trạng thái (Approve, Reject, Request Revision).
- Frontend UI.

## Dependencies & Risks
- Phụ thuộc vào DTO và Mapper của ADM-055 (`AdminPostPageResponse`, `AdminPostSummaryResponse`, `AdminPostApiMapper`).
- Nguy cơ: Cần viết câu lệnh JPQL/SQL sắp xếp theo subquery hợp lý để tránh giảm hiệu năng hoặc lỗi đếm trang (countQuery).
