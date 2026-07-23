# Test Plan: ADM-027–041 Member & Lab Management API

## Phương Pháp

- Unit tests cho Service (mock Repository và Policy).
- Controller/Security tests với `@WebMvcTest` và MockMvc.
- Không test runtime PostgreSQL (nodb profile là đủ cho Sprint 1).

---

## Authorization Tests (áp dụng cho mọi endpoint)

| TC | Endpoint mẫu | Input | Expected |
|----|-------------|-------|----------|
| AUTH-01 | GET /api/admin/members | Không có token | HTTP 401 |
| AUTH-02 | GET /api/admin/members | Token MEMBER role | HTTP 403 |
| AUTH-03 | GET /api/admin/members | Token LEADER role | HTTP 403 |
| AUTH-04 | GET /api/admin/members | Token ADMIN role | HTTP 200 |
| AUTH-05 | GET /api/admin/lab | Token SUPER_ADMIN | HTTP 200 |

---

## ADM-027: GET /api/admin/members

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 027-01 | 3 users thuộc Lab, 1 bị xóa mềm | page=0, size=20 | 200, chỉ 3 users hợp lệ |
| 027-02 | Nhiều users | page=0, size=2 | 200, totalElements đúng, 2 items |
| 027-03 | - | page=-1 | 400 Bad Request |
| 027-04 | - | size=0 | 400 Bad Request |
| 027-05 | - | size=101 | 400 Bad Request |
| 027-06 | Actor Lab A | Users của Lab B | Không xuất hiện trong result |

---

## ADM-028: GET /api/admin/members/{userId}

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 028-01 | User có profile, 2 fields, 1 project | userId hợp lệ | 200, đầy đủ fields |
| 028-02 | User chưa có profile | userId hợp lệ | 200, profile null hoặc rỗng |
| 028-03 | - | userId không tồn tại | 404 Not Found |
| 028-04 | User thuộc Lab B | userId của Lab B | 404 Not Found |

---

## ADM-029: PUT /api/admin/members/{userId}/profile

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 029-01 | User có profile | body hợp lệ | 200, profile được cập nhật |
| 029-02 | User chưa có profile | body hợp lệ | 200, profile được tạo mới |
| 029-03 | - | userId không tồn tại | 404 |
| 029-04 | - | personalEmail không hợp lệ | 400 |

---

## ADM-030: PUT /api/admin/members/{userId}/research-fields

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 030-01 | User có 2 fields cũ | fieldIds = [field3] | 200, chỉ còn field3 |
| 030-02 | User có 2 fields cũ | fieldIds = [] | 200, list rỗng |
| 030-03 | - | fieldIds chứa UUID không tồn tại | 404 |
| 030-04 | - | userId không tồn tại | 404 |

---

## ADM-031: PATCH /api/admin/members/{userId}/activity-status

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 031-01 | User có profile ACTIVE | activityStatus=INACTIVE | 200, status=INACTIVE |
| 031-02 | User chưa có profile | activityStatus=ACTIVE | 200, profile được tạo với status=ACTIVE |
| 031-03 | - | activityStatus=INVALID_VALUE | 400 |
| 031-04 | - | userId không tồn tại | 404 |

---

## ADM-032: GET /api/admin/members/{userId}/projects

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 032-01 | User tham gia 2 projects | userId hợp lệ | 200, 2 entries |
| 032-02 | User không tham gia project nào | userId hợp lệ | 200, list rỗng |
| 032-03 | - | userId không tồn tại | 404 |

---

## ADM-033: GET /api/admin/members/{userId}/evaluations

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 033-01 | User có 3 evaluations | userId hợp lệ | 200, 3 entries |
| 033-02 | User không có evaluation nào | userId hợp lệ | 200, list rỗng |
| 033-03 | - | userId không tồn tại | 404 |

---

## ADM-034: GET /api/admin/lab

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 034-01 | Lab tồn tại | Token Admin hợp lệ | 200, đúng Lab của actor |
| 034-02 | - | Token Admin Lab A | Không trả về Lab B |

---

## ADM-035: PUT /api/admin/lab

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 035-01 | Lab tồn tại | body hợp lệ | 200, Lab được cập nhật |
| 035-02 | - | name blank | 400 |
| 035-03 | - | contactEmail không hợp lệ | 400 |

---

## ADM-036/037: POST /api/admin/lab/logo và /cover

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 036-01 | File thuộc Lab | fileId hợp lệ | 200, logoFile được cập nhật |
| 036-02 | File thuộc Lab khác | fileId | 400/422 |
| 036-03 | - | fileId không tồn tại | 404 |

---

## ADM-038: GET /api/admin/research-fields

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 038-01 | 3 fields (2 ACTIVE, 1 INACTIVE) | Token Admin | 200, cả 3 xuất hiện |
| 038-02 | Không có field nào | Token Admin | 200, list rỗng |

---

## ADM-039: POST /api/admin/research-fields

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 039-01 | code chưa tồn tại | body hợp lệ | 201, field mới tạo |
| 039-02 | code đã tồn tại | body trùng code | 409 Conflict |
| 039-03 | - | code blank | 400 |
| 039-04 | - | name blank | 400 |
| 039-05 | - | code > 50 ký tự | 400 |

---

## ADM-040: PUT /api/admin/research-fields/{fieldId}

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 040-01 | Field tồn tại | body hợp lệ | 200, field được cập nhật |
| 040-02 | - | fieldId không tồn tại | 404 |
| 040-03 | - | name blank | 400 |

---

## ADM-041: PATCH /api/admin/research-fields/{fieldId}/status

| TC | Preconditions | Input | Expected |
|----|--------------|-------|----------|
| 041-01 | Field ACTIVE | status=INACTIVE | 200, status=INACTIVE |
| 041-02 | Field INACTIVE | status=ACTIVE | 200, status=ACTIVE |
| 041-03 | - | status=INVALID | 400 |
| 041-04 | - | fieldId không tồn tại | 404 |

---

## Verification Command

```powershell
Set-Location backend
$env:SPRING_PROFILES_ACTIVE = "nodb"
.\mvnw.cmd clean test
```
