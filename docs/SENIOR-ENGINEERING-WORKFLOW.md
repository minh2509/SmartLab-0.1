# Quy trình Senior Software Engineer

Tài liệu này là phần mở rộng bắt buộc của `AGENTS.md`. Mọi AI làm việc trong
repository SmartLab phải đọc tài liệu này trước khi thực hiện một feature, bug
fix hoặc refactor có mức độ đáng kể.

## 1. Phạm vi áp dụng

Quy trình đầy đủ áp dụng cho:

- Feature mới hoặc thay đổi hành vi hiện có.
- Bug fix có chỉnh sửa source code.
- Refactor có thể ảnh hưởng đến contract, dữ liệu hoặc luồng nghiệp vụ.
- Thay đổi liên quan đến authentication, authorization, validation, database,
  API hoặc kiến trúc.

Không bắt buộc tạo bộ tài liệu task đầy đủ cho:

- Câu hỏi chỉ cần đọc và giải thích code.
- Kiểm tra trạng thái hoặc chẩn đoán chỉ đọc.
- Sửa lỗi chính tả hoặc thay đổi tài liệu rất nhỏ.
- Thay đổi cấu hình hướng dẫn AI không tác động đến source code.

Nếu chưa rõ một task có đáng kể hay không, ưu tiên áp dụng quy trình đầy đủ.

## 2. Cách sử dụng skills

Khi Superpowers đã được cài và xuất hiện trong danh sách skills của phiên làm
việc, sử dụng skill phù hợp với từng giai đoạn:

- `brainstorming` để làm rõ yêu cầu còn mơ hồ hoặc có nhiều phương án thiết kế.
- `writing-plans` để tạo kế hoạch dựa trên code thực tế.
- `test-driven-development` để thực hiện Red → Green → Refactor khi phù hợp.
- `systematic-debugging` để tìm nguyên nhân gốc của lỗi.
- `verification-before-completion` trước khi tuyên bố kết quả.
- `requesting-code-review` hoặc workflow review tương ứng trước khi bàn giao.
- `finishing-a-development-branch` chỉ để chuẩn bị phương án bàn giao; không tự
  commit, push, merge, rebase hoặc thay đổi lịch sử Git.

Không được giả vờ đã dùng một skill nếu skill đó không khả dụng. Khi skill
không khả dụng, thực hiện trực tiếp quy trình tương đương trong tài liệu này và
nêu rõ giới hạn nếu nó ảnh hưởng đến kết quả.

Skill `documents` chỉ dùng khi task yêu cầu tạo hoặc chỉnh sửa `.docx`. Các file
Markdown trong `docs/tasks/` phải được chỉnh sửa như source text bình thường.

Mọi skill chỉ bổ trợ workflow. Yêu cầu trực tiếp của người dùng, `AGENTS.md` và
source code thực tế luôn có độ ưu tiên cao hơn.

## 3. Bộ tài liệu bắt buộc cho mỗi task

Mỗi task thuộc phạm vi áp dụng phải có thư mục riêng:

```text
docs/tasks/<task-name>/
├── spec.md
├── plan.md
├── test-plan.md
└── verification.md
```

Tên task dùng chữ thường và dấu gạch ngang, hoặc giữ đúng mã task hiện có, ví
dụ `adm-057-project-members`.

Không bắt đầu chỉnh sửa source code trước khi hoàn thành và tự kiểm tra:

- `spec.md`
- `plan.md`
- `test-plan.md`

`verification.md` được tạo hoặc hoàn thiện sau khi triển khai và chạy kiểm tra.

Không đưa placeholder như `<TASK_NAME>` vào tài liệu đã bàn giao.

## 4. Bước 1 — Đọc và phân tích dự án

Trước khi lập tài liệu hoặc sửa code:

1. Đọc toàn bộ `AGENTS.md`, tài liệu này, `docs/PROJECT-PROFILE.md` và
   `docs/TASKS.md`.
2. Đọc yêu cầu task và mọi file đính kèm.
3. Kiểm tra branch hiện tại, `git status` và `git diff`.
4. Kiểm tra cấu trúc project và scripts thực tế.
5. Tìm các route, entity, controller, service, repository, DTO, component,
   hook, store, API và test liên quan.
6. Phân tích luồng hiện tại từ đầu vào đến đầu ra.
7. Xác định abstraction, component và business rule có thể tái sử dụng.
8. Xác định dữ liệu, quyền truy cập và chức năng cũ có nguy cơ bị ảnh hưởng.
9. Báo ngắn gọn các file dự kiến tạo hoặc chỉnh sửa trước khi triển khai.

Không tự tưởng tượng tên file, class, method, endpoint, database field, script
hoặc permission khi chưa kiểm tra code thực tế. Source code là nguồn sự thật
cuối cùng.

## 5. Bước 2 — Tạo specification

Tạo `docs/tasks/<task-name>/spec.md` với nội dung phù hợp từ các mục:

- Task overview.
- Problem statement.
- Goals.
- Non-goals.
- Actors.
- Current behavior.
- Expected behavior.
- Functional requirements.
- Business rules.
- Authentication và authorization rules.
- Validation rules.
- Acceptance criteria có thể kiểm tra.
- Edge cases.
- Error cases.
- Out-of-scope items.
- Assumptions.
- Dependencies.
- Risks.

Specification mô tả hệ thống cần làm gì, chưa đi sâu vào chi tiết
implementation. Không invent business rule để lấp khoảng trống. Nếu một quyết
định có thể làm thay đổi đáng kể kết quả và không thể suy ra từ code, phải hỏi
người dùng.

## 6. Bước 3 — Tạo implementation plan

Tạo `docs/tasks/<task-name>/plan.md` dựa trên file và kiến trúc đã kiểm tra.
Bao gồm các mục phù hợp:

- Tóm tắt kiến trúc và luồng hiện tại.
- Giải pháp đề xuất và lý do lựa chọn.
- Luồng dữ liệu.
- Files cần tạo.
- Files cần chỉnh sửa.
- Class, method, route, component và store liên quan.
- Database hoặc migration changes.
- Backend layers và `nodb` impact.
- API contract và DTO/request/response changes.
- Frontend và state-management changes.
- Validation.
- Authentication và authorization.
- Error handling và logging.
- Backward compatibility, bao gồm dữ liệu `localStorage` cũ.
- Thứ tự implementation.
- Rollback considerations.
- Verification commands dựa trên scripts thực tế.

Mỗi bước phải nhỏ, rõ ràng, có mục đích và có thể xác minh độc lập. Không dùng
kế hoạch chung chung như “làm backend”, “làm frontend”, “test”.

Nếu code thực tế khác giả định ban đầu, cập nhật `spec.md`, `plan.md` hoặc
`test-plan.md` trước khi tiếp tục.

## 7. Bước 4 — Tạo test plan

Tạo `docs/tasks/<task-name>/test-plan.md` với các nhóm test phù hợp:

- Unit tests.
- Integration tests.
- API tests.
- Frontend tests.
- Authentication và authorization tests.
- Validation tests.
- Error cases.
- Edge cases.
- Regression tests.
- Manual verification.

Mỗi test case phải nêu:

- Preconditions.
- Input hoặc action.
- Expected result.

Ưu tiên Test-Driven Development khi hợp lý:

```text
Red → Green → Refactor
```

1. Viết test thất bại.
2. Chạy test và xác nhận thất bại vì đúng lý do.
3. Viết lượng code tối thiểu để test pass.
4. Chạy lại test.
5. Refactor.
6. Chạy regression tests liên quan.

Không ép TDD cho thay đổi chỉ có tài liệu, generated code hoặc trường hợp không
có test harness phù hợp; phải ghi rõ cách kiểm chứng thay thế.

## 8. Bước 5 — Kiểm tra tài liệu trước khi code

Trước khi chỉnh sửa source code, tự kiểm tra:

- Specification có mâu thuẫn không?
- Acceptance criteria có đo lường hoặc kiểm tra được không?
- Plan có dựa trên file, script và contract thực tế không?
- Permission dựa trên system role, active role hay project scope đã rõ chưa?
- Validation và error handling đã đủ chưa?
- Happy path và failure path đã được bao phủ chưa?
- Có nguy cơ phá vỡ API, dữ liệu cũ, route hoặc chức năng hiện có không?
- Phạm vi có chứa thay đổi không cần thiết không?

Chỉ triển khai sau khi các điểm trên đã được xử lý.

## 9. Bước 6 — Thực thi plan

Thực hiện đúng thứ tự trong `plan.md`:

1. Chỉ thay đổi trong phạm vi task.
2. Không refactor diện rộng nếu chưa được yêu cầu.
3. Tái sử dụng kiến trúc, convention, type, store và component hiện có.
4. Giữ code đơn giản, rõ ràng và dễ bảo trì.
5. Không duplicate business logic hoặc source of truth.
6. Không đặt business logic trong controller.
7. Không bỏ qua validation, authentication hoặc authorization.
8. Không hard-code secrets hoặc dữ liệu nhạy cảm.
9. Không thêm hoặc nâng cấp dependency khi chưa được chấp thuận.
10. Bảo vệ dữ liệu `localStorage` cũ bằng safe defaults khi shape thay đổi.
11. Mỗi thay đổi phải có lý do rõ ràng và có cách kiểm chứng.

Chỉ dùng parallel agents khi người dùng hoặc hướng dẫn đang áp dụng cho phép,
các phần việc thực sự độc lập và contract đã được thống nhất.

## 10. Bước 7 — Systematic debugging

Khi gặp lỗi, không sửa bằng thử nghiệm ngẫu nhiên:

1. Tái hiện lỗi.
2. Ghi nhận preconditions, input, output, log và stack trace.
3. Xác định layer đầu tiên xuất hiện sai lệch.
4. Theo dõi dữ liệu ngược về nguyên nhân gốc.
5. Đưa ra giả thuyết có thể kiểm chứng.
6. Kiểm chứng từng giả thuyết bằng thay đổi nhỏ hoặc kiểm tra chỉ đọc.
7. Sửa nguyên nhân gốc, không chỉ che triệu chứng.
8. Thêm regression test khi có test harness phù hợp.
9. Chạy lại test liên quan và regression tests.

## 11. Bước 8 — Verification before completion

Không tuyên bố task hoàn thành nếu chưa chạy kiểm tra thực tế. Trước tiên phải
đọc scripts trong `package.json`, `frontend/package.json` và `backend/pom.xml`
để chọn command đúng.

Các command chuẩn của SmartLab có thể gồm:

```powershell
npm run build:frontend
npm run build:backend
```

Backend không cần database phải dùng profile `nodb`, ví dụ trên Windows:

```powershell
Set-Location backend
$env:SPRING_PROFILES_ACTIVE = "nodb"
.\mvnw.cmd clean test
```

Chỉ chạy lint, typecheck, test, build hoặc server command thực sự tồn tại và
phù hợp với phạm vi thay đổi.

Ngoài automated checks, xác minh các điểm phù hợp:

- Application khởi động thành công.
- Database migration thành công nếu task có database.
- API contract và status code đúng.
- Authorization đúng với system role và project scope.
- Frontend gọi API hoặc mock store đúng.
- Không có console error nghiêm trọng.
- Responsive behavior đúng ở khoảng 375px, 768px và 1280px khi có UI.
- Accessibility và keyboard flow không bị suy giảm.
- Acceptance criteria đã được đáp ứng.
- Chức năng cũ liên quan không bị phá vỡ.

Luôn chạy:

```powershell
git diff --check
git status --short
git diff
```

Không tuyên bố command pass nếu chưa thực sự chạy thành công.

## 12. Bước 9 — Tạo verification report

Tạo hoặc cập nhật `docs/tasks/<task-name>/verification.md` với:

- Summary of implementation.
- Changed files.
- Created files.
- Commands executed.
- Test results.
- Build results.
- Manual verification results.
- Acceptance criteria checklist.
- Regression checks.
- Known limitations.
- Remaining risks.
- Follow-up work.
- Những phần chưa thể kiểm tra và lý do.

Ghi từng command theo mẫu:

```text
Command:
Result:
Relevant output:
```

Nếu test hoặc build thất bại, ghi trung thực và không đánh dấu task
`READY_FOR_REVIEW`.

## 13. Bước 10 — Code review

Sau implementation và verification, tự review diff theo:

- Correctness.
- Readability.
- Maintainability.
- Security.
- Authentication và authorization.
- Validation.
- Error handling.
- Performance.
- Database consistency.
- Duplicate logic.
- API compatibility.
- Test coverage.
- Regression risks.

Phân loại phát hiện thành `Critical`, `High`, `Medium` hoặc `Low`. Phải xử lý
`Critical` và `High` trước khi bàn giao; báo rõ `Medium` và `Low` còn lại.

## 14. Bước 11 — Bàn giao development branch

Trước khi đề xuất commit:

- Xác nhận không có file ngoài phạm vi bị thay đổi.
- Không đưa secrets, credentials, build artifacts, file tạm hoặc log thừa vào
  commit.
- Đảm bảo tài liệu khớp implementation và kết quả kiểm tra thực tế.
- Không tự commit, push, merge, rebase, force-push hoặc rewrite Git history.
- Không dùng `git add .`.

Sau khi verification đạt yêu cầu, đề xuất:

- Branch name.
- Conventional Commit message.
- Pull Request title và description.
- Summary.
- Testing evidence.
- Risks.
- Screenshot hoặc API example nếu cần.
- Các lệnh Git dùng đúng file path để người dùng tự review và chạy.

Chỉ dùng trạng thái `READY_FOR_REVIEW` sau khi các build và test bắt buộc đã
pass. Chỉ dùng `DONE` sau khi Pull Request đã merge.

## 15. Quy tắc phản hồi

Trong quá trình làm việc:

1. Dùng tiếng Việt trừ khi người dùng yêu cầu ngôn ngữ khác.
2. Trình bày ngắn gọn những gì đã tìm thấy trong codebase.
3. Nêu plan gồm files, data, permissions và edge cases trước khi code.
4. Tạo đủ tài liệu task trước khi chỉnh sửa source code.
5. Sau mỗi phase đáng kể, nêu file đã thay đổi và kết quả kiểm tra.
6. Khi plan khác code thực tế, cập nhật tài liệu.
7. Không nói “hoàn thành”, “đã sửa xong” hoặc “mọi thứ hoạt động” khi chưa có
   bằng chứng verification.
8. Không chỉ đưa hướng dẫn khi người dùng đã yêu cầu thực hiện; trực tiếp sửa,
   test và xác minh trong phạm vi được cấp phép.
9. Nếu không thể chạy command, ghi rõ lý do và không giả định command sẽ pass.
10. Phân biệt rõ giới hạn demo phía frontend với bảo mật production thực sự.

