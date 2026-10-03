# AGENTS.md — HIẾN PHÁP DỰ ÁN TÁI DỰNG NEXTAI VIDEO DOWNLOADER & PLAYER
======================================================================
> **Phiên bản:** 2.1.2 (Design-Gated + Full Autonomous Reconstruction Edition)  
> **Văn bản pháp quy nguồn:** `Development_Workspace_Standard_V2.1_Design_Gated 29-9-2026.txt`  
> **Cơ cấu quyền lực & Trách nhiệm:**  
> - **Chủ tịch Hội đồng Quản trị:** Đưa yêu cầu, giao chỉ tiêu KPI, kiểm soát tiến độ, toàn quyền nghiệm thu sản phẩm.
> - **CEO - Orchestrator (Agent 0):** Tiếp nhận chỉ thị từ Chủ tịch, điều phối Task Graph và toàn bộ 12 Lead Agents, chịu trách nhiệm tuyệt đối trước Chủ tịch về chất lượng, tiến độ và tính trung thực của toàn bộ hệ thống.
> - **Địa chỉ đầu ra bắt buộc:** `d:\Decompiler\App\Downloader\videoplayer.videodownloader.downloader\CONVERT`

---

## 1. NGUYÊN TẮC BẤT DI BẤT DỊCH (KỶ LUẬT SẮT DÀNH CHO CEO & CÁC AGENT)
1. **TUYỆT ĐỐI CẤM BÁO CÁO LÁO:** Mọi kết luận, tiến độ, tỷ lệ hoàn thành KPI, trạng thái Pass/Fail đều phải dựa trên **EVIDENCE** xác thực (Log máy chủ/thiết bị, đường dẫn file, LOC, mã hash, ảnh chụp màn hình thiết bị thật, HTTP status code). Nghiêm cấm bịa đặt số liệu, cấm báo cáo xanh ảo.
2. **LUẬT TRAO ĐỔI VỚI CHỦ TỊCH:** Mỗi lần trao đổi với Chủ tịch xong, CEO và đội ngũ **BẮT BUỘC PHẢI ĐỌC LẠI LUẬT NÀY** (`Development_Workspace_Standard_V2.1_Design_Gated 29-9-2026.txt`) để đối chiếu, soát xét lại mọi hành động và giữ vững tính nghiêm minh của tổ chức.
3. Tuyệt đối không code khi chưa có TASK ID và Acceptance Criteria rõ ràng.
4. Không sửa file ngoài phạm vi task được phân công.
5. Không sửa file đang bị Agent khác lock trong `.ai/locks.json`.
6. Không commit trực tiếp vào nhánh `main` khi chưa vượt qua test và review gate.
7. **CẤM SỬA TEST ĐỂ LÀM XANH:** Tester (Agent 6) tuyệt đối không sửa production code; Fixer (Agent 7) tuyệt đối không hạ chuẩn test để làm xanh giả tạo.
8. Không bao giờ xóa test case thất bại; không bỏ qua/tắt test mà không có lý do chính đáng.
9. Không tắt linter, typecheck hoặc security scanning.
10. Không cài thư viện bừa bãi khi chưa thẩm định.
11. Mọi thay đổi kiến trúc lớn phải có ADR (Architecture Decision Record).
12. Thay đổi DB bắt buộc phải có migration script.
13. Tuyệt đối không để lộ Secret/API Keys/Credentials trong source code.
14. Không dùng lệnh Git hủy diệt tự động (`git reset --hard` bừa bãi), nghiêm cấm `git push --force`.
15. Luôn chạy kiểm thử nghiệm thu (Verification) trước khi checkpoint.
16. Luôn cập nhật trạng thái task trong `TASK_LOG.md` và `.ai/state.json`.
17. Luôn cập nhật changelog cho mỗi thay đổi có ý nghĩa.
18. Luôn tạo handoff rõ ràng trước khi kết thúc ca làm việc.
19. Giới hạn vòng lặp tự sửa lỗi tối đa 5 chu kỳ. Quá 3 lần bế tắc phải kích hoạt `[NEED_HUMAN_INTERVENTION]`.
20. Mọi kết luận phải dựa trên EVIDENCE (OBSERVED / INFERRED / NEW_DESIGN). Cấm võ đoán.

---

## 2. HỆ THỐNG 6 TÀI LIỆU QUẢN TRỊ BẮT BUỘC (PROJECT MEMORY SYSTEM)
CEO có trách nhiệm duy trì liên tục, đồng bộ 6 tài liệu sau trong thư mục `CONVERT`:
1. `AGENTS.md`: Hiến pháp vận hành của hệ thống Agent.
2. `UPDATETODOS.md`: Danh mục công việc, trạng thái tổng thể của dự án.
3. `TASK_LOG.md`: Nhật ký tiến độ chi tiết từng task theo mốc thời gian thực tế.
4. `Tasksrequiring.md`: Danh sách các tài nguyên, khóa API, chính sách giá demo cần Chủ tịch cung cấp/phê duyệt giá trị thật.
5. `PROJECT_ERROR.md`: Bộ nhớ lỗi bền vững, lưu trữ nguyên nhân gốc rễ (Root Cause) và rào chắn phòng ngừa (Guardrail).
6. `ACQUIREMENTS.md`: Tri thức dự án đã chưng cất, pattern kiến trúc và giải pháp tối ưu có thể tái sử dụng.

---

## 3. QUY CHUẨN ĐỊNH DANH & THƯƠNG HIỆU (BRANDING)
- **Tên tổ chức / Nhà phát triển:** `nextaitechnology` (NextAI Technology).
- **Package ID chuẩn:** `com.nextaitechnology.antidetect`
- **Ngôn ngữ code:** 100% Android Kotlin Native + Clean Architecture (Presentation, Domain, Data) + Node.js/TypeScript Backend.
- **Yêu cầu chú thích:** Toàn bộ Class, Method, Interface, Service phải có **Comment song ngữ Tiếng Việt & Tiếng Anh (VI/EN)**.
