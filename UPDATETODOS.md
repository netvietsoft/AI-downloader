# BẢNG ĐIỀU PHỐI TIẾN ĐỘ DỰ ÁN (UPDATETODOS.MD)

**Người quản lý:** Agent 0 (CEO - ORCHESTRATOR)  
**Phiên bản chuẩn:** Development Workspace Standard V2.1.2  
**Thời gian cập nhật:** 2026-10-12  

---

## 📊 TIẾN ĐỘ TỔNG QUAN

| Hạng mục | Trạng thái | Agent chịu trách nhiệm | Ghi chú |
| :--- | :---: | :---: | :--- |
| **1. Khởi tạo Governance Framework** | `DONE` | Agent 0 (CEO) | README, AGENTS, rules, UPDATETODOS, Tasksrequiring, PROJECT_ERROR, ACQUIREMENTS |
| **2. Tách riêng nhánh Web & Cấu hình Port 3080** | `DONE` | Agent 0 (CEO) | Nhánh `web` độc lập phục vụ clone về `/home/netviet/projects-deploy/ai-dowloader` |
| **3. Audit Bảo Mật & Keystore** | `DONE` | Agent 0 (CEO) | Loại bỏ nguy cơ lộ mật khẩu và keystore lên Git |
| **4. Android App Feature & Parity** | `DONE` | Agent 3 (Frontend) | Module Video Sniffer, Player, Settings, Language, Billing |
| **5. Deploy Web lên snaptik2.com (Port 3080)** | `DONE` | DevOps / User | Đã deploy và chạy online trên PM2 cổng 3080 |
| **6. Google Play Policy & Billing v8 Compliance** | `DONE` | Agent 3 (Android) | Xử lý Subscriptions & Broken Functionality, Billing v8.0.0, Build 8 |
| **7. Khắc phục Crash Play Billing v8 (VC 9)** | `DONE` | Agent 3 (Android) | Tách truy vấn SUBS và INAPP, chống crash Main Thread, Build 9 |
| **8. Tuân Thủ Target API Level 36 & Release Build 10** | `DONE` | Agent 3 (Android) | Nâng cấp compileSdk/targetSdk 36, versionCode 10, v1.0.4 AAB/APK ký số |
| **9. Khắc Phục Misleading Claims & Release Build 11** | `DONE` | Agent 3 (Android) | Bóc tách Instagram/Meta CDN, Private Vault độc lập, Purge 4K/8K, Build 11 |

---

## 🎯 DANH SÁCH TASK CHI TIẾT

### Phase 1: Git & Web Deployment
- [x] **TASK-001:** Rà soát Git, cấu hình `.gitignore` chuẩn bảo vệ Keystore, loại trừ 35.000 file thô `SOURCE/`.
- [x] **TASK-002:** Thống nhất server web Node.js port 3080, khắc phục lỗi Express 5 wildcard routing, phục vụ API Sniffer + Static Frontend cho domain `snaptik2.com`.
- [x] **TASK-003:** Tạo file cấu hình `ecosystem.config.js`, `nginx-snaptik2.conf`, `deploy.sh` phục vụ triển khai server.
- [x] **TASK-004:** Đẩy mã nguồn lên GitHub:
  - Branch `main`: Chứa mã nguồn App Android + Web Platform + Governance.
  - Branch `web`: Chứa độc lập mã nguồn Web Platform cho deploy server.
  - Deploy và khôi phục an toàn các dịch vụ trên server Ubuntu.

### Phase 2: Android App Completion & Parity
- [x] **TASK-005:** Đồng bộ các tham số cấu hình production (AdMob, Firebase, IAP) từ Tasksrequiring.md.
- [x] **TASK-006:** Kiểm thử e2e luồng tải video trên các nền tảng (TikTok, FB, YT, X).
- [x] **TASK-007:** Build bản phát hành Release AAB/APK (AAB 11.03MB, APK 7.28MB ký số thành công).
- [x] **TASK-008:** Khắc phục lỗi từ chối duyệt của Google Play Console & Nâng cấp Version Code 8 (Play Billing Library v8.0.0):
  - **Subscriptions policy & Play Billing Library v8.0.0:** Tích hợp `com.android.billingclient:billing:8.0.0`, tương thích `QueryProductDetailsResult`, truy vấn giá và tiền tệ SKU động theo tài khoản Google Play, chuẩn hóa hiển thị tiền tệ theo Locale thiết bị (không mặc định cứng VND cho người dùng quốc tế), đồng bộ 100% giữa trang chào giá (offers page) và giỏ hàng thanh toán (payment cart).
  - **Broken Functionality policy:** Kết nối nút "NÂNG CẤP PRO NGAY" vào flow thanh toán BillingClient thật, xử lý Khôi phục gói mua (restore purchases), khắc phục các thành phần không phản hồi (tv_pro_badge_top, tv_sub_btn_status, card_subscription khi đã Active, link Điều khoản & Chính sách trên Paywall, nút kiểm tra cập nhật), ẩn Top Bar thừa và nút cài đặt bị trùng lặp trên màn hình Cài đặt.
  - **Version Code 8:** Cập nhật `versionCode = 8`, `versionName = "1.0.2"`, build thành công cả AAB và APK release.
- [x] **TASK-009:** Khắc phục lỗi Crash mở ứng dụng (Broken Functionality policy - Rejection Version Code 8) & Nâng cấp Version Code 9 (v1.0.3):
  - **Khắc phục lỗi Crash BillingClient 8.0.0:** Tách truy vấn `QueryProductDetailsParams` độc lập cho `ProductType.SUBS` (`weekly_pro`, `yearly_pro`) và `ProductType.INAPP` (`lifetime_pro`), khắc phục triệt để ngoại lệ `IllegalArgumentException: All products should be of the same product type.` gây crash khi mở `PaymentWallActivity` trên main thread.
  - **Defensive Error Handling:** Bổ sung `try-catch` bao bọc mọi lời gọi và callback của `BillingClient`, `PaymentWallActivity.onResume()`, xử lý null-safety cho `productDetailsList`.
  - **Đồng bộ Restore & Background Check:** Mở rộng `queryActivePurchases()` và `restorePurchases()` hỗ trợ cả đăng ký định kỳ và mua vĩnh viễn trọn đời.
  - **Firebase Analytics Reserved Event Names:** Đổi tên các sự kiện bị Firebase cảnh báo reserved name (`app_exception` -> `app_error_event`, `first_open` -> `custom_first_open`, ...) ngăn ngừa cảnh báo và bảo toàn log crash.
  - **Chuẩn hóa SDK & Version Code 9:** Đưa `compileSdk` và `targetSdk` về chuẩn ổn định 35 (Android 15), cập nhật `versionCode = 9`, `versionName = "1.0.3"`.
  - **Kiểm thử tự động & thực tế:** Bổ sung unit test `CurrencyFormatterTest` kiểm chứng đa ngôn ngữ/quốc gia, xác minh thực tế trên thiết bị Samsung Galaxy thật qua ADB, đóng gói Release AAB 11.03MB.
- [x] **TASK-010:** Nâng cấp Target API Level 36 & Version Code 10 (v1.0.4) đáp ứng quy định mới của Google Play:
  - **Khắc phục lỗi Google Play Target API:** Google Play Console bắt buộc ứng dụng phải target tối thiểu API level 36 (*"must target at least API level 36"*).
  - **Cập nhật SDK:** Nâng cấp `compileSdk = 36` và `targetSdk = 36` trong `app/build.gradle.kts`.
  - **Cấu hình Compiler:** Cập nhật `android.suppressUnsupportedCompileSdk=36` trong `gradle.properties`.
  - **Version Code 10:** Tăng `versionCode = 10`, `versionName = "1.0.4"` tránh xung đột version code đã tải lên Google Play Console.
  - **Đóng gói phát hành:** Build thành công AAB Release (`com.ai.video.downloader.videodownloader_v1.0.4_vc10.aab`, 10.52 MB) và APK (`com.ai.video.downloader.videodownloader_v1.0.4_vc10.apk`, 6.94 MB) ký số hoàn chỉnh.
- [x] **TASK-011:** Khắc phục vi phạm chính sách Misleading Claims & In-App Experience (Rejection VC 10) & Nâng cấp Version Code 11 (v1.0.5):
  - **Khắc phục nhận diện Instagram Reels & Meta CDN:** Tích hợp bộ giải mã `extractInstagramVideo()`, thẻ `og:video`, xử lý stream CDN `cdninstagram.com` & `fbcdn.net` loại bỏ byte-range chunk parameter mà không bị drop stream, bổ sung DOM OuterHTML & `<video>` Play event listener đảm bảo nút Download FAB xuất hiện ngay lập tức khi phát video.
  - **Private Vault độc lập & Tự kiểm thử (Self-Contained Vault):** Bổ sung nút "+ Add Demo Video" (`btn_vault_add_sample`) và "+ Import Video" (`btn_vault_import_file` qua `ActivityResultContracts.GetContent()`), cho phép người dùng và reviewer kiểm thử toàn bộ luồng mã hóa AES-256, xem, xóa, khôi phục ngay trên bản cài mới tinh (clean install) mà không cần tải từ web trước.
  - **Thanh lọc toàn bộ phát ngôn 4K / 8K & Unrealistic Formats:** Rà soát và thay thế toàn bộ từ khóa "4K", "8K" thành "HD / High Definition" trên toàn bộ 50 thư mục `res/values-*/strings.xml` và layout `activity_payment_wall.xml`.
  - **Version Code 11:** Nâng `versionCode = 11`, `versionName = "1.0.5"` trong `app/build.gradle.kts`.
  - **Đóng gói & Kiểm thử thực tế:** Build thành công AAB Release (`app-release.aab`, 11.05 MB) và APK Release (`app-release.apk`, 7.29 MB) ký số hoàn chỉnh. Cài đặt và kiểm thử trực tiếp trên điện thoại Samsung Galaxy thật qua ADB không lỗi.

