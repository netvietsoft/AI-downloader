# NHẬT KÝ THỰC THI TASK (TASK_LOG.MD)

Tài liệu ghi nhận toàn bộ tiến trình và hoạt động thực thi theo dòng thời gian của các Agent trong dự án theo chuẩn **Development Workspace Standard V2.1.2**.

---

## [2026-10-12 20:30] TASK-010: Nâng cấp Target API Level 36 & Release Build 10 (v1.0.4)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend & Build) / Agent 0 (CEO Orchestrator)
- **Bối cảnh:** Google Play Console từ chối bản build Version Code 9 với lỗi: *"Your app currently targets API level 35 and must target at least API level 36 to ensure that it is built on the latest APIs optimised for security and performance."*
- **Hành động đã thực hiện:**
  1. Nâng cấp `compileSdk = 36` và `targetSdk = 36` trong `CONVERT/apps/android/app/build.gradle.kts`.
  2. Bổ sung cấu hình `android.suppressUnsupportedCompileSdk=36` trong `CONVERT/apps/android/gradle.properties`.
  3. Cập nhật `versionCode = 10` và `versionName = "1.0.4"` để tạo định danh phiên bản mới, tránh lỗi trùng lặp mã phiên bản đã lưu trong Google Play Console.
  4. Thực thi `./gradlew bundleRelease` hoàn tất trong 2m 42s (AAB: 10.52 MB).
  5. Thực thi `./gradlew assembleRelease` hoàn tất trong 29s (APK: 6.94 MB).
  6. Sao chép và lưu trữ định danh tiện ích vào `File/com.ai.video.downloader.videodownloader_v1.0.4_vc10.aab` và `.apk`.
- **Kết quả kiểm chứng:**
  - `bundleRelease`: BUILD SUCCESSFUL, AAB đã ký số bằng `release-keystore.jks`.
  - `assembleRelease`: BUILD SUCCESSFUL, APK đã ký số bằng `release-keystore.jks`.
  - `output-metadata.json` xác nhận: `versionCode = 10`, `versionName = 1.0.4`, target API 36.
- **Trạng thái:** `COMPLETED`
- **Tài liệu cập nhật kèm theo:**
  - `PROJECT_ERROR.md`: Bổ sung `[ERR-011]`.
  - `ACQUIREMENTS.md`: Bổ sung `[ACQ-006]`.
  - `CHANGELOG.md`: Bổ sung phiên bản `[1.0.4]`.
  - `UPDATETODOS.md`: Bổ sung `TASK-010`.

---

## [2026-10-12 17:25] TASK-009: Khắc phục Crash Play Billing v8 & Build Release Version Code 9 (v1.0.3)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend) / Agent 7 (Fixer)
- **Bối cảnh:** Google Play Console từ chối duyệt bản build 8 do lỗi vi phạm *Broken Functionality - Crashes* khi mở ứng dụng / Paywall.
- **Hành động đã thực hiện:**
  1. Phân tích logcat phát hiện `IllegalArgumentException: All products should be of the same product type.` do Play Billing v8.0.0 cấm gộp `SUBS` và `INAPP` chung một `QueryProductDetailsParams`.
  2. Tách thành 2 truy vấn độc lập cho `ProductType.SUBS` (`weekly_pro`, `yearly_pro`) và `ProductType.INAPP` (`lifetime_pro`) trong `BillingManager.kt`.
  3. Bổ sung `try-catch` phòng vệ bao bọc toàn bộ callback BillingClient và `PaymentWallActivity.onResume()`.
  4. Đổi tên sự kiện Firebase reserved (`app_exception` -> `app_error_event`, `first_open` -> `custom_first_open`, ...).
  5. Tối ưu hóa JVM compiler: thêm `-XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=128m` và `org.gradle.workers.max=4` trong `gradle.properties` để ngăn chặn crash C2 compiler arena OOM (`arena.cpp:168`).
  6. Chạy Unit Test `CurrencyFormatterTest` thành công 100%.
  7. Đóng gói AAB và APK ký số bản Version Code 9 (`v1.0.3`).
- **Kết quả kiểm chứng:**
  - App mở màn hình Paywall trơn tru không crash.
  - Unit Test `testDebugUnitTest` 29/29 tasks passed.
- **Trạng thái:** `COMPLETED`
- **Tài liệu cập nhật kèm theo:**
  - `PROJECT_ERROR.md`: Bổ sung `[ERR-009]`, `[ERR-010]`.
  - `ACQUIREMENTS.md`: Bổ sung `[ACQ-004]`, `[ACQ-005]`.
  - `CHANGELOG.md`: Bổ sung phiên bản `[1.0.3]`.

---

## [2026-10-07 15:30] TASK-008: Tuân Thủ Play Billing Library v8 & Chính Sách Subscriptions/Broken Functionality (Build 8)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend)
- **Bối cảnh:** Google Play từ chối bản build 6 vì lỗi không đồng bộ tiền tệ Paywall (hardcode VND) và nút bấm không phản hồi (dead click / mock Toast).
- **Hành động đã thực hiện:**
  1. Tích hợp Google Play Billing Library v8.0.0 (`com.android.billingclient:billing:8.0.0`).
  2. Xây dựng `BillingManager.kt` truy vấn giá thời gian thực `formattedPrice` từ Google Play Store và gán vào UI Paywall.
  3. Sửa `CurrencyFormatter.kt` và `SettingsRepository.kt` đọc mã quốc gia/ngôn ngữ động theo `Locale.getDefault()`.
  4. Gắn `OnClickListener` cho toàn bộ các nút trên Paywall và Settings (`tv_pro_badge_top`, `tv_sub_btn_status`, nút Quản lý gói Google Play...).
  5. Ẩn `layout_top_bar` khi chuyển tab sang Settings/Player/Progress để loại bỏ nút trùng lặp.
  6. Tăng `versionCode = 8`, `versionName = "1.0.2"`.
- **Trạng thái:** `COMPLETED`

---

## [2026-10-03] TASK-001 đến TASK-007: Khởi Tạo Dự Án, Web Deploy & Android Initial Release
- **Agent chịu trách nhiệm:** Agent 0 (CEO), Agent 1 (Architect), Agent 2 (Backend), Agent 3 (Frontend)
- **Hành động đã thực hiện:**
  - Thiết lập Governance Framework V2.1.2: `README.md`, `AGENTS.md`, `UPDATETODOS.md`, `PROJECT_ERROR.md`, `ACQUIREMENTS.md`.
  - Bảo vệ Keystore: thêm rule chặn `*.jks`, `*.keystore`, `File/` vào `.gitignore` (`[ERR-001]`).
  - Hợp nhất Express Server Web cổng 3080 tại domain `snaptik2.com` (`[ACQ-001]`).
  - Tách nhánh triển khai độc lập `web` (`[ACQ-002]`).
  - Khắc phục lỗi Express 5 wildcard routing (`[ERR-005]`, `[ACQ-003]`).
  - Đóng gói bản phát hành đầu tiên `v1.0.0` (Build 6).
- **Trạng thái:** `COMPLETED`
