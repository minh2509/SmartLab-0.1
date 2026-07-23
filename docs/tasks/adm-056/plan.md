# Implementation Plan: ADM-056 Admin Pending Post Queue API

## Architecture & Existing Codebase Context
- **Controller**: `AdminPostController` (`@RequestMapping("/api/admin/posts")`, `@Profile("!nodb")`).
- **Service**: `AdminPostService` (`@Service`, `@Profile("!nodb")`), phụ thuộc vào `PostRepository`, `AdminRolePolicy`, `AdminPostApiMapper`.
- **Repository**: `PostRepository` (`JpaRepository<Post, UUID>`), sử dụng JPQL `@Query` để truy vấn danh sách bài viết.
- **Entity & Enums**: `Post`, `PostModerationLog`, `PostStatus.PENDING_REVIEW`, `PostModerationAction.SUBMIT`.
- **Response Contracts**: Reuses `AdminPostPageResponse` and `AdminPostSummaryResponse`.

## Proposed Solution
Thêm endpoint `GET /api/admin/posts/pending` để phục vụ danh sách hàng chờ kiểm duyệt bài viết cho quản trị viên.

### Flow dữ liệu:
1. Client gửi `GET /api/admin/posts/pending?page=0&size=20` kèm Bearer Token Admin.
2. `AdminPostController.listPendingPosts(...)` tiếp nhận request, validate tham số `page` và `size`.
3. `AdminPostService.listPendingPosts(...)` gọi `rolePolicy.requireAdminActor(...)` để xác thực người dùng và lấy `Lab`.
4. `PostRepository.findAdminPendingPosts(...)` thực thi câu lệnh JPQL lọc bài viết `PENDING_REVIEW` chưa bị xóa mềm của `Lab`, sắp xếp theo `latest SUBMIT timestamp ASC NULLS LAST, post.id ASC`.
5. `AdminPostApiMapper` ánh xạ `Page<Post>` thành `AdminPostPageResponse`.

## Files to Create / Modify

### 1. [MODIFY] [PostRepository.java](file:///c:/Users/ADMIN88/Downloads/SBA/SmartLab-0.1/backend/src/main/java/com/smartlab/repository/PostRepository.java)
- Thêm query `findAdminPendingPosts`:
```java
@EntityGraph(attributePaths = {"author", "project", "category", "coverFile"})
@Query(
    value = """
        select post
        from Post post
        where post.lab = :lab
          and post.deletedAt is null
          and post.moderationStatus = com.smartlab.enums.PostStatus.PENDING_REVIEW
        order by (
            select max(log.createdAt)
            from PostModerationLog log
            where log.post = post
              and log.action = com.smartlab.enums.PostModerationAction.SUBMIT
        ) asc nulls last, post.id asc
        """,
    countQuery = """
        select count(post)
        from Post post
        where post.lab = :lab
          and post.deletedAt is null
          and post.moderationStatus = com.smartlab.enums.PostStatus.PENDING_REVIEW
        """
)
Page<Post> findAdminPendingPosts(@Param("lab") Lab lab, Pageable pageable);
```

### 2. [MODIFY] [AdminPostService.java](file:///c:/Users/ADMIN88/Downloads/SBA/SmartLab-0.1/backend/src/main/java/com/smartlab/service/admin/AdminPostService.java)
- Thêm query record: `ListPendingAdminPostsQuery(UUID actorUserId, Integer page, Integer size)`.
- Thêm method `listPendingPosts(ListPendingAdminPostsQuery query)` với `@Transactional(readOnly = true)`.
- Re-use `normalizedPage` và `normalizedSize`.

### 3. [MODIFY] [AdminPostController.java](file:///c:/Users/ADMIN88/Downloads/SBA/SmartLab-0.1/backend/src/main/java/com/smartlab/controller/admin/AdminPostController.java)
- Thêm method `@GetMapping("/pending")`:
```java
@GetMapping("/pending")
public AdminPostPageResponse listPendingPosts(
        @RequestParam(required = false) @Min(0) Integer page,
        @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
    return adminPostService.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
            actorResolver.requireActorUserId(),
            page,
            size));
}
```

### 4. [MODIFY / NEW] Unit and Integration Tests
- Thêm unit test trong `AdminPostServiceTest.java`.
- Thêm controller & security test trong `AdminPostControllerTest.java`.
- Thêm repository test trong `PostRepositoryTest.java`.

## Implementation Order
1. Cập nhật `PostRepository.java` với `findAdminPendingPosts`.
2. Cập nhật `AdminPostService.java` với `listPendingPosts`.
3. Cập nhật `AdminPostController.java` với `@GetMapping("/pending")`.
4. Viết và cập nhật unit/integration tests cho Service, Controller, Repository.
5. Kiểm tra toàn bộ test suite backend với `SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test`.

## Rollback Considerations
- Các thay đổi hoàn toàn độc lập dưới endpoint `/api/admin/posts/pending`. Nếu có lỗi, có thể revert commit mà không ảnh hưởng tới API ADM-055 hay schema database.

## Verification Commands
```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```
