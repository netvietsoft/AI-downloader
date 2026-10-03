# QUY TẮC PHÁT TRIỂN & VẬN HÀNH (DOCS/RULES.MD)

## 1. Phân Định Quyền Hạn
- **Agent 0 (CEO - Orchestrator):** Điều phối tổng thể, chia task, theo dõi tiến độ qua `UPDATETODOS.md`, quản lý danh sách thông số thiếu qua `Tasksrequiring.md`. Không trực tiếp code feature.
- **Agent 1 (Architect):** Thiết kế kiến trúc, module, database, API contract.
- **Agent 2 (Backend):** Xây dựng server logic, sniffer service, database, worker, security.
- **Agent 3 (Frontend):** Xây dựng UI/UX cho Android Kotlin và Web Frontend, bám sát Design Gate.
- **Agent 6 (Tester):** Viết unit/integration/e2e tests, ghi nhận bug report. Không sửa production code.
- **Agent 7 (Fixer):** Phân tích root cause từ bug report và sửa mã nguồn trong đúng phạm vi.

## 2. Quy Tắc Bảo Mật (Security & Credentials)
- Mọi chứng chỉ ký số (`*.jks`, `*.keystore`) và file thông tin mật khẩu (`*KEYSTORE*`) phải được loại trừ bằng `.gitignore`.
- Mã nguồn trong `build.gradle.kts` hoặc `server.js` không được hardcode mật khẩu, phải đọc qua biến môi trường.

## 3. Quy Tắc Triển Khai Web (snaptik2.com)
- Thư mục web độc lập được duy trì tại nhánh `web`.
- Chạy trên Node.js với cổng mặc định `PORT=3070`.
- Tên miền đích: `snaptik2.com`.
- Frontend tự động gọi API tương đối (`BACKEND_URL = ''`) để tương thích cả reverse proxy Nginx và trực tiếp.
