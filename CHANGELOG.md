# CHANGELOG

Tất cả các thay đổi quan trọng trong dự án **AI Video Downloader** (Bao gồm Android App và Web Platform) được ghi nhận chi tiết tại đây theo tiêu chuẩn [Keep a Changelog](https://keepachangelog.com/vi/1.0.0/) và [Semantic Versioning](https://semver.org/).

---

## [1.0.6] - 2026-10-31 (Build 12)

### Đã khắc phục & Nâng cấp (Fixed & Changed)
- **Khắc phục triệt để vi phạm chính sách Subscriptions (Currency differences with prominent display price):**
  - **Dynamic Recurring Price Breakdown:** Lấy trực tiếp `priceAmountMicros` và `priceCurrencyCode` từ Google Play Billing để tính toán chi phí tháng của gói năm (`paywall_subtext_yearly_format`), đảm bảo 100% khớp đơn vị tiền tệ thực tế của người dùng, loại bỏ hoàn toàn việc hiển thị USD `$1.66` dưới giá VND/EUR/GBP.
  - **Loại bỏ giá cứng USD:** Xóa bỏ giá mặc định `$2.99`, `$19.99`, `$39.99` trong layout `activity_payment_wall.xml`, thay bằng placeholder loading và chuỗi tài nguyên động.
  - **Mở rộng Currency Fallback:** Thêm hỗ trợ CAD, AUD, SGD và tự động phát hiện tiền tệ thiết bị nếu người dùng offline.
  - **Connection Queue trong BillingManager:** Thiết lập hàng đợi kết nối thread-safe chống race condition gọi `startConnection()` đồng thời khi mở `PaymentWallActivity`, đảm bảo không bao giờ drop callback mở Google Play Cart.
  - **Quốc tế hóa 100%:** Bản địa hóa 20 chuỗi thông báo, dialog lỗi, khôi phục giao dịch và quản lý thuê bao trong `SettingsFragment` sang `strings.xml` và `values-vi/strings.xml`.
  - **Nâng cấp phiên bản:** Tăng `versionCode` lên `12` và `versionName` lên `1.0.6`.
  - **Kiểm thử tự động:** Bổ sung 11 unit test trong `CurrencyFormatterTest.kt` kiểm chứng currency parity trên toàn cầu (100% Passed).
  - **Đóng gói phát hành:** Đóng gói thành công AAB Release (`com.ai.video.downloader.videodownloader_v1.0.6_vc12.aab`, 11.05 MB) và APK (`com.ai.video.downloader.videodownloader_v1.0.6_vc12.apk`, 7.30 MB) ký số hoàn chỉnh.

---

## [1.0.5] - 2026-10-21 (Build 11)

### Đã khắc phục & Nâng cấp (Fixed & Changed)
- **Khắc phục vi phạm chính sách Misleading Claims & In-App Experience:**
  - **Bóc tách sâu luồng video Instagram Reels & Meta CDN:** Nâng cấp `SnifferEngine.kt` và `HomeFragment.kt`, nhận diện các thẻ `og:video`, `meta[property="og:video:secure_url"]`, xử lý stream CDN `cdninstagram.com` & `fbcdn.net` loại bỏ query byte-range (`bytestart`/`byteend`) mà không drop stream.
  - **Self-Contained Private Vault:** Thêm nút "+ Add Demo Video" (`btn_vault_add_demo`) và "+ Import Video" (`btn_vault_import`), cho phép reviewer và người dùng kiểm thử toàn bộ luồng mã hóa AES-256 ngay trên clean install.
  - **Thanh lọc phát ngôn 4K / 8K:** Rà soát và chuẩn hóa toàn bộ 50 thư mục `res/values-*/strings.xml` và layout `activity_payment_wall.xml`, chuyển "4K / 8K" thành "HD / High Definition" trung thực với năng lực thực tế.
  - **Nâng cấp phiên bản:** Tăng `versionCode` lên `11` và `versionName` lên `1.0.5`.
  - **Đóng gói phát hành:** Build thành công AAB Release (`com.ai.video.downloader.videodownloader_v1.0.5_vc11.aab`, 11.05 MB) và APK (7.29 MB) ký số hoàn chỉnh.

---

## [1.0.4] - 2026-10-12 (Build 10)

### Đã thay đổi & Nâng cấp (Changed)
- **Nâng cấp Target API Level 36 theo chuẩn Google Play Console:**
  - Nâng cấp `compileSdk = 36` và `targetSdk = 36` đáp ứng yêu cầu bắt buộc mới nhất của Google Play (*"must target at least API level 36"*).
  - Cập nhật cờ `android.suppressUnsupportedCompileSdk=36` trong `gradle.properties`.
  - Tăng `versionCode` lên `10` và `versionName` lên `1.0.4` để tải lên Google Play Console thay thế bản build 9.
  - Đóng gói thành công AAB Release (`com.ai.video.downloader.videodownloader_v1.0.4_vc10.aab`, 10.52 MB) và APK (`com.ai.video.downloader.videodownloader_v1.0.4_vc10.apk`, 6.94 MB) ký số hoàn chỉnh.

---

## [1.0.3] - 2026-10-12 (Build 9)

### Đã khắc phục (Fixed)
- **Khắc phục lỗi Crash mở ứng dụng / Paywall (Broken Functionality policy):**
  - Khắc phục ngoại lệ `IllegalArgumentException: All products should be of the same product type.` trong Play Billing Library v8.0.0 khi mở `PaymentWallActivity` trên main thread.
  - Tách truy vấn `QueryProductDetailsParams` thành 2 luồng độc lập: một truy vấn cho các gói đăng ký định kỳ `ProductType.SUBS` (`weekly_pro`, `yearly_pro`) và một truy vấn cho gói mua vĩnh viễn `ProductType.INAPP` (`lifetime_pro`), sau đó hợp nhất vào `productDetailsMap`.
  - Bổ sung `try-catch` bao bọc toàn bộ các lời gọi API và callback của `BillingClient` cùng `PaymentWallActivity.onResume()`, đảm bảo an toàn tuyệt đối, không làm crash app ngay cả khi mất mạng hoặc Play Services phản hồi chậm.
  - Mở rộng cơ chế `queryActivePurchases()` và `restorePurchases()` đồng bộ kiểm tra cả `SUBS` và `INAPP`.
- **Chuẩn hóa Firebase Analytics Events:**
  - Thay thế các tên sự kiện dành riêng của Firebase (`app_exception` -> `app_error_event`, `first_open` -> `custom_first_open`, ...) loại bỏ hoàn toàn cảnh báo reserved name.
- **Chuẩn hóa SDK & Phiên bản:**
  - Chuẩn hóa `compileSdk = 35` và `targetSdk = 35` (Android 15 ổn định).
  - Tăng `versionCode` lên `9` và `versionName` lên `1.0.3`.
- **Kiểm thử tự động & Xác minh:**
  - Bổ sung bộ Unit Test `CurrencyFormatterTest` kiểm chứng định dạng đa tiền tệ quốc tế.
  - Chạy `testDebugUnitTest` thành công 100% và đóng gói AAB phát hành Release (`app-release.aab` ~11.03 MB) đã ký số thành công.

---

## [1.0.2] - 2026-10-07 (Build 8)

### Đã thay đổi & Nâng cấp (Changed)
- **Nâng cấp Play Billing Library v8.0.0:** Cập nhật dependency lên `com.android.billingclient:billing:8.0.0` để đáp ứng yêu cầu mới nhất của Google Play Console.
- **Tương thích API v8:** Chuyển đổi callback `queryProductDetailsAsync` để nhận đối tượng `QueryProductDetailsResult` và trích xuất danh sách qua `queryProductDetailsResult.productDetailsList`.
- **Phiên bản phát hành:** Tăng `versionCode` lên `8` và `versionName` lên `1.0.2` trong `build.gradle.kts`.
- **Tối ưu hóa bộ nhớ Gradle:** Cập nhật `gradle.properties` tăng dung lượng heap lên 4GB (`-Xmx4096m -XX:MaxMetaspaceSize=1024m`) và bật cờ `android.suppressUnsupportedCompileSdk=36`.

---

## [1.0.1] - 2026-10-07 (Build 7)

### Đã khắc phục (Fixed)
- **Khắc phục lỗi Subscriptions Policy (Google Play Policy Rejection):**
  - Tích hợp Google Play Billing Client thực tế, thay thế mock Toast kích hoạt Pro.
  - Tự động truy vấn và đồng bộ giá thời gian thực (`formattedPrice`) giữa trang chào giá (offers page) và giỏ hàng thanh toán Google Play (payment cart).
  - Khắc phục lỗi hardcode tiền tệ VNĐ trong `SettingsRepository` bằng cách đọc mã quốc gia/ngôn ngữ theo `Locale.getDefault()`.
  - Cập nhật `CurrencyFormatter` ưu tiên giá từ Google Play Store, fallback theo Locale quốc gia của thiết bị.
- **Khắc phục lỗi Broken Functionality Policy (Google Play Policy Rejection):**
  - Kết nối nút "NÂNG CẤP PRO NGAY" vào phương thức `BillingManager.launchBillingFlow`.
  - Kết nối nút "Khôi phục gói mua" vào `BillingManager.restorePurchases`.
  - Thêm hộp thoại thông tin gói và liên kết mở quản lý thuê bao Google Play (`showProActiveDialog`) khi tài khoản đã kích hoạt PRO, ngăn chặn việc ép mua lại gây ức chế.
  - Gắn sự kiện `OnClickListener` cho `tv_pro_badge_top` và `tv_sub_btn_status`.
  - Tự động ẩn thanh `layout_top_bar` khi ở các tab Cài đặt, Trình phát, Tiến trình, loại bỏ hiện tượng trùng lặp 2 thanh tiêu đề và nút cài đặt (bánh răng) bị vô hiệu hóa.
- **Bổ sung ProGuard Rules:** Thêm quy tắc giữ mã nguồn `-keep class com.android.billingclient.** { *; }` trong `proguard-rules.pro`.

---

## [1.0.0] - 2026-10-03 (Build 6)

### Khởi tạo & Phát hành ban đầu (Added)
- Xây dựng ứng dụng Android AI Video Downloader với Kotlin, MVVM architecture, Media3 ExoPlayer, AES-256 Vault, Web Sniffer Engine và AdMob.
- Xây dựng Web Platform chạy Node.js / Express 5 cổng 3080 tại domain `snaptik2.com`.
- Thiết lập Governance Framework V2.1.2: `README.md`, `AGENTS.md`, `PROJECT_ERROR.md`, `ACQUIREMENTS.md`, `UPDATETODOS.md`.
- Bảo vệ an toàn Keystore và thông tin mật khẩu phát hành, loại trừ 35.000 file rác.
