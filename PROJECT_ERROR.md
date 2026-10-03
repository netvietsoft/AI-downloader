# CANONICAL DURABLE ERROR MEMORY (PROJECT_ERROR.MD)

Tài liệu ghi nhận các lỗi đã gặp trong dự án, nguyên nhân gốc rễ (root cause), giải pháp đã xác minh (verification) và biện pháp ngăn ngừa (guardrails). Bắt buộc đọc trước khi thực hiện task.

---

## [ERR-001] Nguy cơ rò rỉ Release Keystore và lộ mật khẩu ký số lên Git
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-03
- **Triệu chứng:** File `release-keystore.jks` và `KEYSTORE_INFO.txt` (chứa password thật `AiDownloader2026@Pro`) nằm trong thư mục theo dõi Git.
- **Root Cause:** Cấu hình `.gitignore` ban đầu chưa liệt kê `*.jks`, `*.keystore`, `*KEYSTORE*`.
- **Giải pháp:** Cập nhật `.gitignore` loại trừ triệt để mọi file `.jks`, file mật khẩu và thư mục `File/`. Chuyển cấu hình `signingConfigs` trong Android sang đọc biến môi trường.
- **Ngăn ngừa:** Kiểm tra `git check-ignore` trước khi thực hiện commit.

---

## [ERR-002] Cấu hình .gitignore chặn nhầm toàn bộ Icon và Resource hình ảnh
- **Mức độ:** HIGH
- **Ngày phát hiện:** 2026-10-03
- **Triệu chứng:** Các icon ứng dụng Android `ic_launcher.png`, 9-patch `bg_popmenu_black.9.png` và icon Extension Chrome bị Git bỏ qua không thêm vào repo.
- **Root Cause:** `.gitignore` đặt `*.png`, `*.jpg` một cách bừa bãi không phân biệt giữa screenshot kiểm thử và resource ứng dụng.
- **Giải pháp:** Xóa rule `*.png`, `*.jpg` toàn cục trong `.gitignore`, chỉ chặn các thư mục `screenshots/` và file dạng `*screen*.png`, `test_*.png`.
- **Ngăn ngừa:** Kiểm tra kỹ danh sách untracked files của `src/main/res/`.

---

## [ERR-003] Frontend Web gọi API hardcode localhost:5101 dẫn tới lỗi khi deploy server
- **Mức độ:** MEDIUM
- **Ngày phát hiện:** 2026-10-03
- **Triệu chứng:** `app.js` đặt cứng `BACKEND_URL = 'http://localhost:5101'`, khi deploy lên server domain `snaptik2.com` cổng 3070 sẽ bị lỗi mạng CORS hoặc không gọi được backend.
- **Root Cause:** Cấu hình môi trường dev cục bộ hardcode URL.
- **Giải pháp:** Chuyển `BACKEND_URL = ''` (đường dẫn tương đối) và hợp nhất Express server phục vụ cả static frontend và `/api/*` trên cùng cổng.
- **Ngăn ngừa:** Không bao giờ hardcode host/port trong mã nguồn client-side.

---

## [ERR-004] Xung đột cổng triển khai (Port Conflict) trên VPS
- **Mức độ:** MEDIUM
- **Ngày phát hiện:** 2026-10-03
- **Triệu chứng:** Cổng `3070` đã bị chiếm dụng bởi dịch vụ khác trên server, dẫn đến trả về lỗi 404 (File not found) hoặc tranh chấp tài nguyên.
- **Root Cause:** Cấu hình cổng mặc định trùng với tiến trình khác đang chạy trên hệ thống.
- **Giải pháp:** Kiểm tra danh sách cổng đang lắng nghe bằng `sudo ss -tulpn`, chuyển dịch vụ sang cổng khả dụng `3080` và cập nhật đồng bộ các file `.env.example`, `ecosystem.config.js`, `nginx-snaptik2.conf`, `server.js`.
- **Ngăn ngừa:** Luôn scan cổng trước khi quyết định cổng deploy.

---

## [ERR-005] Express 5 Crash Do Cú Pháp Wildcard Routing 'app.get(*)'
- **Mức độ:** HIGH
- **Ngày phát hiện:** 2026-10-03
- **Triệu chứng:** PM2 restart liên tục bị crash loop (`↺: 15`), lệnh `curl http://localhost:3080/api/health` trả về `curl: (7) Connection refused`.
- **Root Cause:** Ứng dụng dùng `express ^5.2.1`. Trong Express 5 (sử dụng thư viện `path-to-regexp` v6/v7), cú pháp route wildcard `app.get('*', ...)` không còn được hỗ trợ và sẽ ném lỗi `PathError: Missing parameter name at index 1: *`.
- **Giải pháp:** Chuyển toàn bộ SPA fallback handler sang Express middleware chuẩn:
  ```javascript
  app.use((req, res, next) => {
      if (req.method !== 'GET') return next();
      if (req.url.startsWith('/api/')) return next();
      if (fs.existsSync(publicDir)) {
          const indexPath = path.join(publicDir, 'index.html');
          if (fs.existsSync(indexPath)) return res.sendFile(indexPath);
      }
      next();
  });
  ```
- **Ngăn ngừa:** Luôn chạy thử `node server.js` kiểm chứng runtime khi nâng cấp hoặc khởi chạy Express 5; tránh dùng ký tự wildcard thô `*` trong routing pattern.

