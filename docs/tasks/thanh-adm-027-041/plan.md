# Implementation Plan: ADM-027–041 Member & Lab Management API

## Architecture Hiện Tại

Tuân thủ pattern: `Controller → Service → Repository → Entity`

Các thành phần đã có:
- `AdminRolePolicy.requireAdminActor(UUID)` → xác thực & lấy `ActorContext` (có `lab()`)
- `AuthenticatedActorResolver.requireActorUserId()` → lấy UUID từ JWT
- Toàn bộ Entity và Repository cần thiết đã tồn tại
- Pattern DTO dùng `record` cho response, `final class` với setters cho request có validation
- Pattern Service dùng inner `record` cho Command/Query

---

## Giải Pháp Đề Xuất

Tạo 2 Controller, 3 Service, 1 Mapper, các DTO request/response tương ứng.

Không tạo thêm Entity hay Repository mới. Không thay đổi Entity hiện có.

---

## Implementation Order

### Phase 1: DTOs & Mapper (không phụ thuộc gì)

### Phase 2: Services (phụ thuộc DTO)

### Phase 3: Controllers (phụ thuộc Service)

### Phase 4: Tests

---

## Files Cần Tạo

### Phase 1 – DTOs & Mapper

#### `com.smartlab.dto.response.admin`
- `AdminMemberSummaryResponse.java` – dùng cho ADM-027 (list)
- `AdminMemberDetailResponse.java` – dùng cho ADM-028 (detail)
- `AdminMemberProjectResponse.java` – dùng cho ADM-032
- `AdminMemberEvaluationResponse.java` – dùng cho ADM-033
- `AdminLabResponse.java` – dùng cho ADM-034, 035, 036, 037
- `AdminResearchFieldResponse.java` – dùng cho ADM-038, 039, 040, 041

#### `com.smartlab.dto.request.admin`
- `UpdateMemberProfileRequest.java` – ADM-029
- `UpdateMemberResearchFieldsRequest.java` – ADM-030
- `UpdateMemberActivityStatusRequest.java` – ADM-031
- `UpdateLabInfoRequest.java` – ADM-035
- `UpdateLabImageRequest.java` – ADM-036, 037 (body: `{ "fileId": "..." }`)
- `CreateResearchFieldRequest.java` – ADM-039
- `UpdateResearchFieldRequest.java` – ADM-040
- `UpdateResearchFieldStatusRequest.java` – ADM-041

#### `com.smartlab.mapper`
- `AdminMemberApiMapper.java` – map User+MemberProfile → summary/detail responses
- `AdminLabApiMapper.java` – map Lab → AdminLabResponse
- `AdminResearchFieldApiMapper.java` – map ResearchField → response

### Phase 2 – Services

#### `com.smartlab.service.admin`
- `AdminMemberService.java` – ADM-027 đến 033
- `AdminLabService.java` – ADM-034 đến 037
- `AdminResearchFieldService.java` – ADM-038 đến 041

### Phase 3 – Controllers

#### `com.smartlab.controller.admin`
- `AdminMemberController.java` – `@RequestMapping("/api/admin/members")`
- `AdminLabController.java` – `@RequestMapping("/api/admin/lab")` và `"/api/admin/research-fields"`

---

## Files Cần Chỉnh Sửa

- Không có file nào cần chỉnh sửa ngoài phạm vi task (không sửa entity, repository hiện có).
- Trường hợp ngoại lệ: Cần bổ sung phương thức query vào `UserRepository` nếu cần filter user theo Lab có phân trang.

---

## API Contract

### ADM-027 GET /api/admin/members
**Query params**: `page` (int, default 0), `size` (int, default 20, max 100), `keyword` (string, optional)

**Response 200**:
```json
{
  "content": [
    {
      "userId": "uuid",
      "username": "string",
      "fullName": "string",
      "email": "string",
      "avatarFileId": "uuid|null",
      "accountStatus": "ACTIVE|SUSPENDED|INACTIVE",
      "activityStatus": "ACTIVE|INACTIVE|ON_LEAVE|null",
      "roleCodes": ["MEMBER","LEADER"],
      "joinedAt": "date|null"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### ADM-028 GET /api/admin/members/{userId}
**Response 200**:
```json
{
  "userId": "uuid",
  "username": "string",
  "fullName": "string",
  "email": "string",
  "avatarFileId": "uuid|null",
  "accountStatus": "ACTIVE",
  "roleCodes": ["MEMBER"],
  "profile": {
    "studentCode": "string|null",
    "phone": "string|null",
    "personalEmail": "string|null",
    "bio": "string|null",
    "specialization": "string|null",
    "joinedAt": "date|null",
    "activityStatus": "ACTIVE",
    "githubUrl": "string|null",
    "linkedinUrl": "string|null",
    "portfolioUrl": "string|null"
  },
  "researchFields": [
    { "id": "uuid", "code": "AI", "name": "Artificial Intelligence" }
  ],
  "projectCount": 2,
  "evaluationCount": 3
}
```

### ADM-029 PUT /api/admin/members/{userId}/profile
**Request body**:
```json
{
  "studentCode": "string|null",
  "phone": "string|null",
  "personalEmail": "string|null",
  "bio": "string|null",
  "specialization": "string|null",
  "joinedAt": "date|null",
  "githubUrl": "string|null",
  "linkedinUrl": "string|null",
  "portfolioUrl": "string|null"
}
```
**Response 200**: AdminMemberDetailResponse

### ADM-030 PUT /api/admin/members/{userId}/research-fields
**Request body**: `{ "fieldIds": ["uuid1", "uuid2"] }`
**Response 200**: `{ "researchFields": [...] }`

### ADM-031 PATCH /api/admin/members/{userId}/activity-status
**Request body**: `{ "activityStatus": "ACTIVE|INACTIVE|ON_LEAVE" }`
**Response 200**: `{ "activityStatus": "ACTIVE" }`

### ADM-032 GET /api/admin/members/{userId}/projects
**Response 200**: `{ "projects": [ { "projectId", "projectName", "projectCode", "role", "memberStatus", "joinedAt" } ] }`

### ADM-033 GET /api/admin/members/{userId}/evaluations
**Response 200**: `{ "evaluations": [ { "id", "projectId", "projectName", "evaluationPeriod", "overallScore", "comment", "evaluatedAt" } ] }`

### ADM-034 GET /api/admin/lab
**Response 200**:
```json
{
  "id": "uuid",
  "name": "string",
  "code": "string",
  "description": "string|null",
  "mission": "string|null",
  "vision": "string|null",
  "contactEmail": "string|null",
  "websiteUrl": "string|null",
  "logoFileId": "uuid|null",
  "coverFileId": "uuid|null",
  "status": "ACTIVE"
}
```

### ADM-035 PUT /api/admin/lab
**Request body**:
```json
{
  "name": "string",
  "description": "string|null",
  "mission": "string|null",
  "vision": "string|null",
  "contactEmail": "string|null",
  "websiteUrl": "string|null"
}
```
**Response 200**: AdminLabResponse

### ADM-036 POST /api/admin/lab/logo
**Request body**: `{ "fileId": "uuid" }`
**Response 200**: AdminLabResponse

### ADM-037 POST /api/admin/lab/cover
**Request body**: `{ "fileId": "uuid" }`
**Response 200**: AdminLabResponse

### ADM-038 GET /api/admin/research-fields
**Response 200**: `{ "fields": [ { "id", "code", "name", "description", "status", "createdAt" } ] }`

### ADM-039 POST /api/admin/research-fields
**Request body**:
```json
{ "code": "AI", "name": "Artificial Intelligence", "description": "string|null" }
```
**Response 201**: AdminResearchFieldResponse

### ADM-040 PUT /api/admin/research-fields/{fieldId}
**Request body**: `{ "name": "...", "description": "..." }`
**Response 200**: AdminResearchFieldResponse

### ADM-041 PATCH /api/admin/research-fields/{fieldId}/status
**Request body**: `{ "status": "ACTIVE|INACTIVE" }`
**Response 200**: AdminResearchFieldResponse

---

## Repository Bổ Sung Cần Thiết

`UserRepository` hiện có `findByLab(Lab)` trả về `List<User>`. Cần thêm query phân trang:
```java
Page<User> findByLabAndDeletedAtIsNull(Lab lab, Pageable pageable);
Page<User> findByLabAndDeletedAtIsNullAndFullNameContainingIgnoreCase(Lab lab, String keyword, Pageable pageable);
```

Hoặc dùng Spring Data Method Name Convention để phân trang, không cần JPQL thủ công.

---

## Validation

- `@Valid` trên request body ở Controller.
- `@NotBlank`, `@Size`, `@Email` trên các trường request DTO.
- Service tự validate UUID và kiểm tra Lab scope.
- Throw `ResourceNotFoundException` khi userId/fieldId không tìm thấy trong Lab.
- Throw `InvalidAdminServiceInputException` khi input null/không hợp lệ ở mức service.

---

## Authentication & Authorization

- Tất cả Controller đánh dấu `@Profile("!nodb")`.
- Spring Security đã cấu hình từ trước – các endpoint `/api/admin/**` yêu cầu role ADMIN.
- `AdminRolePolicy.requireAdminActor()` được gọi trong mỗi Service method để enforce.

---

## Error Handling

- `ResourceNotFoundException` → HTTP 404.
- `InvalidAdminServiceInputException` → HTTP 400.
- `ForbiddenAdminOperationException` → HTTP 403.
- `@Valid` constraint violation → HTTP 400 (đã có global handler).

---

## Thứ Tự Implementation Cụ Thể

#### Bước 1: Response DTOs
1. `AdminResearchFieldResponse`
2. `AdminLabResponse`
3. `AdminMemberSummaryResponse`
4. `AdminMemberDetailResponse`
5. `AdminMemberProjectResponse`
6. `AdminMemberEvaluationResponse`

#### Bước 2: Request DTOs
7. `CreateResearchFieldRequest`
8. `UpdateResearchFieldRequest`
9. `UpdateResearchFieldStatusRequest`
10. `UpdateLabInfoRequest`
11. `UpdateLabImageRequest`
12. `UpdateMemberProfileRequest`
13. `UpdateMemberResearchFieldsRequest`
14. `UpdateMemberActivityStatusRequest`

#### Bước 3: Mappers
15. `AdminResearchFieldApiMapper`
16. `AdminLabApiMapper`
17. `AdminMemberApiMapper`

#### Bước 4: Bổ sung UserRepository queries

#### Bước 5: Services
18. `AdminResearchFieldService`
19. `AdminLabService`
20. `AdminMemberService`

#### Bước 6: Controllers
21. `AdminLabController` (gộp Lab + Research Fields)
22. `AdminMemberController`

#### Bước 7: Tests

---

## Verification Commands

```powershell
Set-Location backend
$env:SPRING_PROFILES_ACTIVE = "nodb"
.\mvnw.cmd clean test
```
