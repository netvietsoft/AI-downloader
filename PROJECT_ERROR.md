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
- **Giải pháp:** Chuyển `BACKEND_URL = ''` (đường dẫn tương đối) và hợp nhất Express server phục vụ cả static frontend và `/api/*` trên cùng cổng 3070.
- **Ngăn ngừa:** Không bao giờ hardcode host/port trong mã nguồn client-side.
