# CANONICAL DISTILLED PROJECT KNOWLEDGE (ACQUIREMENTS.MD)

Tài liệu chưng cất kinh nghiệm, nguyên tắc và giải pháp kỹ thuật tối ưu đã được kiểm chứng trong dự án.

---

## [ACQ-001] Mô Hình Triển Khai Web Single-Port (All-in-One Express Server)
- **Bối cảnh:** Khi triển khai web tải video đa nền tảng và sniffer API lên máy chủ Ubuntu tại `/home/netviet/projects-deploy/ai-dowloader` cho domain `snaptik2.com`.
- **Giải pháp tối ưu:** Hợp nhất Frontend static serving và Backend API routing vào một tiến trình Node.js duy nhất chạy trên `PORT = process.env.PORT || 3070`.
- **Lợi ích:**
  1. Loại bỏ 100% lỗi CORS giữa frontend và backend.
  2. Chỉ cần mở 1 port duy nhất (3070) cho Nginx reverse proxy.
  3. Quản lý tiến trình đơn giản qua 1 instance PM2 (`ecosystem.config.js`).
  4. Client `app.js` dùng relative path `/api/...`, tự động tương thích mọi môi trường dev/staging/prod.

---

## [ACQ-002] Tách Nhánh Triển Khai Độc Lập Bằng Git Subtree
- **Bối cảnh:** Repository chứa cả mã nguồn Android App (`CONVERT/apps/android`) và Web Platform (`WWW/`), nhưng server production chỉ cần mã nguồn Web để chạy `npm install` và `pm2 start`.
- **Giải pháp tối ưu:** Sử dụng nhánh riêng `web` được trích xuất từ thư mục `WWW/`. Thư mục gốc của nhánh `web` chứa toàn bộ code cần thiết cho server.
- **Lợi ích:** Lệnh clone trên server gọn nhẹ, không tải code Android thừa, tiết kiệm tài nguyên và không làm lộ cấu trúc nội bộ dự án.
