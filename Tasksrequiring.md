# DANH MỤC THAM SỐ CẦN USER BỔ SUNG & HOÀN THIỆN (TASKSREQUIRING.MD)

Tài liệu này do **Agent 0 (CEO - ORCHESTRATOR)** quản lý, tổng hợp các tham số bảo mật, API key, App ID hoặc cấu hình kinh doanh còn thiếu hoặc đang sử dụng giá trị Demo/Mặc định mà User cần cung cấp/cập nhật khi phát hành chính thức:

---

## 1. Cấu Hình Android Application
- [ ] **Package Name:** `com.ai.video.downloader.videodownloader` *(Xác nhận giữ nguyên hay đổi mới)*
- [ ] **Application Name:** `AI Video Downloader`
- [ ] **Release Keystore:** Đã tạo tại máy cục bộ (File `release-keystore.jks`). Không upload lên Git.

---

## 2. Dịch Vụ Quảng Cáo (Google AdMob)
- [ ] **AdMob App ID:** Đang sử dụng ID test / placeholder `ca-app-pub-3940256099942544~3347511713` -> Cần thay bằng AdMob App ID chính thức từ Google AdMob Console.
- [ ] **Banner Ad Unit ID:** Cần cung cấp ID chính thức.
- [ ] **Interstitial Ad Unit ID:** Cần cung cấp ID chính thức.
- [ ] **Rewarded Ad Unit ID:** Cần cung cấp ID chính thức.
- [ ] **Native Advanced Ad Unit ID:** Cần cung cấp ID chính thức.

---

## 3. Firebase & Cloud Services
- [ ] **google-services.json:** Dự án `ai-video-downloader-e9bc4` (Đã có sẵn trong `CONVERT/apps/android/app/google-services.json`).
- [ ] **Firebase Cloud Messaging (FCM) Server Key:** Cần nếu sử dụng Push Notification từ server.

---

## 4. Gói Cước & Đăng Ký (In-App Purchase / Subscription)
- [ ] **Gói Tháng (Monthly Pro):** Giá demo: `$4.99`
- [ ] **Gói Quý (Quarterly VIP):** Giá demo: `$12.99`
- [ ] **Gói Năm (Annual VIP):** Giá demo: `$39.99`
- [ ] **Gói Trọn Đời (Lifetime Master):** Giá demo: `$79.99`
- [ ] **Google Play Console Product IDs:** Cần tạo các SKU tương ứng trên Google Play Console.

---

## 5. Cấu Hình Web Platform (snaptik2.com)
- [x] **Cổng triển khai (Port):** `3070`
- [x] **Tên miền:** `snaptik2.com`
- [ ] **Cấu hình DNS:** Trỏ bản ghi A của domain `snaptik2.com` và `www.snaptik2.com` về IP máy chủ server.
- [ ] **Chứng chỉ SSL:** Chạy `sudo certbot --nginx -d snaptik2.com -d www.snaptik2.com` sau khi cấu hình Nginx.
