# Test Plan: ADM-056 Admin Pending Post Queue API

## Overview
Tài liệu test plan cho API danh sách bài viết chờ duyệt `GET /api/admin/posts/pending`.

## Test Approach
Áp dụng Test-Driven Development (Red -> Green -> Refactor):
1. Đảm bảo cấu trúc test bao phủ Controller, Service, Repository và Authorization boundary.
2. Kiểm tra trên cả môi trường `nodb` (mock/unit) và runtime PostgreSQL nếu có.

## Test Cases

### 1. Authorization Tests
- **TC-AUTH-01: Unauthenticated Access**
  - Input: Request `GET /api/admin/posts/pending` không kèm Token.
  - Expected: HTTP 401 Unauthorized.
- **TC-AUTH-02: Non-Admin Access**
  - Input: Request `GET /api/admin/posts/pending` với Token của `MEMBER`.
  - Expected: HTTP 403 Forbidden.
- **TC-AUTH-03: Admin Access**
  - Input: Request `GET /api/admin/posts/pending` với Token của `ADMIN` hoặc `SUPER_ADMIN`.
  - Expected: HTTP 200 OK.

### 2. Service & Validation Tests
- **TC-VAL-01: Null Query Parameter Validation**
  - Input: `query = null`.
  - Expected: Throws `InvalidAdminServiceInputException`.
- **TC-VAL-02: Invalid Page Number**
  - Input: `page = -1`.
  - Expected: Throws `InvalidAdminServiceInputException` / HTTP 400 Bad Request.
- **TC-VAL-03: Invalid Size Number**
  - Input: `size = 0` hoặc `size = 101`.
  - Expected: Throws `InvalidAdminServiceInputException` / HTTP 400 Bad Request.
- **TC-VAL-04: Default Pagination**
  - Input: Không truyền `page` và `size`.
  - Expected: `page = 0`, `size = 20`.

### 3. Repository & Ordering Tests
- **TC-REPO-01: Lab Isolation**
  - Preconditions: Bài viết `PENDING_REVIEW` thuộc Lab A và Lab B.
  - Input: Actor thuộc Lab A gọi API.
  - Expected: Chỉ nhận về các bài viết thuộc Lab A.
- **TC-REPO-02: Soft-deleted and Non-PENDING Exclusion**
  - Preconditions: Có bài viết `DRAFT`, `APPROVED`, `PUBLISHED`, và 1 bài `PENDING_REVIEW` bị xóa mềm (`deleted_at` set).
  - Input: Admin gọi API pending.
  - Expected: Tất cả các bài viết trên đều bị loại trừ.
- **TC-REPO-03: Ordering by Latest SUBMIT Timestamp ASC**
  - Preconditions:
    - Post 1: Nộp duyệt lúc 10:00 (log SUBMIT).
    - Post 2: Nộp duyệt lúc 09:00 (log SUBMIT).
    - Post 3: Nộp duyệt lần 1 lúc 08:00, bị yêu cầu sửa, nộp lại lần 2 lúc 11:00.
    - Post 4: Trạng thái `PENDING_REVIEW` nhưng không có log SUBMIT.
  - Expected Order:
    1. Post 2 (09:00)
    2. Post 1 (10:00)
    3. Post 3 (11:00 - timestamp nộp mới nhất)
    4. Post 4 (Không có log SUBMIT - xếp cuối)
- **TC-REPO-04: Tie-breaker by Post ID ASC**
  - Preconditions: Post A và Post B có cùng timestamp `SUBMIT`.
  - Expected Order: Post có UUID/ID nhỏ hơn đứng trước.

## Verification Commands
```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```
