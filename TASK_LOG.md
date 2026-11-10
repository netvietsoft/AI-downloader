# NHẬT KÝ THỰC THI TASK (TASK_LOG.MD)

Tài liệu ghi nhận toàn bộ tiến trình và hoạt động thực thi theo dòng thời gian của các Agent trong dự án theo chuẩn **Development Workspace Standard V2.1.2**.

---

## [2026-11-10 14:45] TASK-013: Khắc Phục Triệt Để Vi Phạm Chính Sách Subscriptions (LocaleHelper Country Preservation & Play Price Format Cloning) & Release Build 13 (v1.0.7)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend & Billing) / Agent 7 (Fixer) / Agent 0 (CEO Orchestrator)
- **Bối cảnh:** Google Play Console từ chối bản build Version Code 12 theo chính sách Subscriptions Policy: *"Currency differences with prominent display price. Ensure that currency displayed is consistent through multiple screens such as offers page and payment cart within the purchase flow and is appropriately localized for each country that your app is targeting."* Ảnh chụp từ Reviewer hiển thị cả 3 gói trên Paywall đều hiện giá fallback USD (`$2.99`, `$19.99`, `Just $1.66 / month • Save 70%`, `$39.99`) dù 3 mã `weekly_pro`, `yearly_pro`, `lifetime_pro` đã được Active trên Google Play Console.
- **Nguyên nhân gốc rễ (Root Cause):**
  1. `LocaleHelper.wrapContext()` và `LocaleHelper.applyLanguage()` tạo `Locale(langCode)` (ví dụ `Locale("en")` không có tham số `country`) rồi gán `Locale.setDefault(locale)`, xóa trắng `Locale.getDefault().country` (`""`) trên toàn bộ thiết bị nói tiếng Anh ngoài Mỹ (`en_IN` Ấn Độ, `en_GB` Anh, `en_PH` Philippines, `en_SG` Singapore, `en_AU` Úc, `en_CA` Canada, `en_IE` Ireland...) cũng như 36 ngôn ngữ khác, khiến `CurrencyFormatter.getFallbackPricing()` luôn rơi về USD `$2.99 / $19.99 / $1.66 / $39.99`.
  2. `BillingManager.onBillingSetupFinished()` gọi trùng lặp `queryAvailableProducts()` tại cùng một mili-giây và `PaymentWallActivity.onCreate()` gọi `refreshPricing()` ngay lập tức khi `ProductDetails` đang tải, ghi đè `@string/paywall_price_loading` bằng giá fallback USD; khi bấm `SUBSCRIBE NOW`, `handleSubscribe()` tải lại `ProductDetails` và mở thẳng giỏ hàng nội tệ mà không gọi `refreshPricing()` cập nhật màn hình Offer.
  3. `CurrencyFormatter` dùng `NumberFormat.getCurrencyInstance(Locale("en"))` sinh ký hiệu khác với `formattedPrice` của Google Play (`IDR` thay vì `Rp`, `CA$` thay vì `$`, `US$` thay vì `$`).
- **Hành động đã thực hiện:**
  1. Nâng cấp `LocaleHelper.kt`: bổ sung `getSystemDeviceLocale()`, `getDeviceCountryCode(context)` (kết hợp SIM Country ISO, Network Country ISO, `Resources.getSystem()`, `TIMEZONE_TO_COUNTRY`, và bảng ánh xạ 50 ngôn ngữ `DEFAULT_COUNTRY_FOR_LANGUAGE`) cùng `resolveLocaleWithCountry(langCode, context)` bảo toàn 100% mã quốc gia ISO 2 ký tự.
  2. Nâng cấp `BillingManager.kt`: triển khai Single-Flight Product Query (`isQueryingProducts` + `pendingQueryCallbacks` + `isLoadingProducts: StateFlow<Boolean>`), tự động retry có backoff, và chọn `getSelectedSubscriptionOffer()` (ưu tiên Base Plan `offerId == null`, `priceAmountMicros > 0`) đồng nhất cho cả hiển thị Paywall và `offerToken` mở giỏ hàng Google Play.
  3. Nâng cấp `CurrencyFormatter.kt`: bổ sung `formatAmountMatchingPlayPrice()` sao chép chính xác tiền tố (`prefix`), hậu tố (`suffix`) và dấu phân cách từ `formattedPrice` của Google Play; mở rộng `CURRENCY_SPECS` phủ toàn bộ 50 ngôn ngữ và các quốc gia mục tiêu.
  4. Nâng cấp `PaymentWallActivity.kt` & `SettingsFragment.kt`: giữ trạng thái `@string/paywall_price_loading` khi đang tải lần đầu từ Google Play và bắt buộc gọi `refreshPricing()` ngay trước khi mở giỏ hàng thanh toán.
  5. Tăng `versionCode = 13` và `versionName = "1.0.7"` trong `CONVERT/apps/android/app/build.gradle.kts`.
  6. Bổ sung unit tests trong `CurrencyFormatterTest.kt` (14/14 passed).
  7. Build AAB Release (`com.ai.video.downloader.videodownloader_v1.0.7_vc13.aab`, 11.07 MB) và APK Release (`com.ai.video.downloader.videodownloader_v1.0.7_vc13.apk`, 7.31 MB) ký số hoàn chỉnh.
- **Kết quả kiểm chứng:**
  - `testDebugUnitTest`: 14/14 unit tests passed (`BUILD SUCCESSFUL in 42s`).
  - `bundleRelease` & `assembleRelease`: `BUILD SUCCESSFUL`, `output-metadata.json` xác nhận `versionCode = 13`, `versionName = "1.0.7"`.
- **Trạng thái:** `COMPLETED`
- **Tài liệu cập nhật kèm theo:**
  - `PROJECT_ERROR.md`: Bổ sung `[ERR-014]`.
  - `ACQUIREMENTS.md`: Bổ sung `[ACQ-009]`.
  - `CHANGELOG.md`: Bổ sung phiên bản `[1.0.7]`.
  - `UPDATETODOS.md`: Hoàn thành `TASK-013`.
  - `.ai/state.json`: Chuyển trạng thái `COMPLETED`.

---

## [2026-10-31 12:30] TASK-012: Khắc Phục Vi Phạm Chính Sách Subscriptions (Currency Differences) & Release Build 12 (v1.0.6)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend & Billing) / Agent 0 (CEO Orchestrator)
- **Bối cảnh:** Google Play Console từ chối bản build Version Code 11 theo chính sách Subscriptions Policy: *"Currency differences with prominent display price. Ensure that currency displayed is consistent through multiple screens such as offers page and payment cart within the purchase flow and is appropriately localized for each country that your app is targeting."* (IN_APP_EXPERIENCE-6840.png). Reviewer gặp tình trạng thẻ gói năm hiển thị tiền tệ bất nhất ($1.66 hardcoded dưới giá EUR/VND) và khi bấm Subscribe Now hiện thông báo lỗi tiếng Việt hardcode do race condition BillingClient.
- **Hành động đã thực hiện:**
  1. Loại bỏ toàn bộ giá USD cứng `$2.99`, `$19.99`, `$39.99` và subtext cứng trong `activity_payment_wall.xml`, thay bằng `@string/paywall_price_loading`, `@string/paywall_subtext_weekly`, `@string/paywall_subtext_lifetime`.
  2. Nâng cấp `CurrencyFormatter.kt`: tính toán động phân rã tháng cho gói Năm từ `yearlyPhase.priceAmountMicros / 12` và định dạng bằng `formatAmountWithCurrency` đồng nhất 100% với `priceCurrencyCode` từ Google Play; mở rộng fallback sang CAD, AUD, SGD và tự động nhận diện currency của thiết bị.
  3. Tái cấu trúc `BillingManager.kt`: triển khai hàng đợi kết nối an toàn (Connection Queue với cờ `@Volatile var isConnecting` và `pendingReadyCallbacks` được đồng bộ `synchronized`), loại bỏ rớt callback do race condition khi mở `PaymentWallActivity`.
  4. Bản địa hóa toàn bộ thông báo, dialog lỗi, restore purchase và chi tiết gói đang active trong `SettingsFragment.kt` sang `strings.xml` và `values-vi/strings.xml` (20 chuỗi tài nguyên mới).
  5. Tăng `versionCode = 12` và `versionName = "1.0.6"` trong `CONVERT/apps/android/app/build.gradle.kts`.
  6. Viết 11 Unit Test trong `CurrencyFormatterTest.kt` kiểm chứng 100% currency parity (Passed 11/11).
  7. Build AAB Release (`com.ai.video.downloader.videodownloader_v1.0.6_vc12.aab`, 11.05 MB) và APK Release (`com.ai.video.downloader.videodownloader_v1.0.6_vc12.apk`, 7.30 MB) ký số hoàn chỉnh.
  8. Push nhánh `fix/subscription-currency-v1.0.6` lên GitHub và merge vào nhánh `main`, push lên `origin/main` (Commit `3b28519`).
- **Kết quả kiểm chứng:**
  - `testDebugUnitTest`: 11/11 unit tests passed.
  - `bundleRelease` & `assembleRelease`: BUILD SUCCESSFUL.
  - Mã nguồn đã đồng bộ trên GitHub `origin/main` và `origin/fix/subscription-currency-v1.0.6`.
- **Trạng thái:** `COMPLETED`
- **Tài liệu cập nhật kèm theo:**
  - `PROJECT_ERROR.md`: Bổ sung `[ERR-013]`.
  - `ACQUIREMENTS.md`: Bổ sung `[ACQ-008]`.
  - `CHANGELOG.md`: Bổ sung phiên bản `[1.0.6]`.
  - `UPDATETODOS.md`: Hoàn thành `TASK-012`.
  - `.ai/state.json`: Chuyển trạng thái `COMPLETED`.

---

## [2026-10-21 16:45] TASK-011: Khắc Phục Vi Phạm Chính Sách Misleading Claims & Release Build 11 (v1.0.5)
- **Agent chịu trách nhiệm:** Agent 3 (Android Frontend & Engine) / Agent 0 (CEO Orchestrator)
- **Bối cảnh:** Google Play Console từ chối bản build Version Code 10 theo chính sách Misleading Claims & In-App Experience: mô tả hứa hẹn tự động tải trên mọi mạng xã hội nhưng Instagram Reels không hiện nút tải; hứa hẹn 4K/8K và định dạng không thực tế; Private Vault trống rỗng không thể kiểm thử độc lập trên clean install (IN_APP_EXPERIENCE-6747.png).
- **Hành động đã thực hiện:**
  1. Nâng cấp `SnifferEngine.kt` & `HomeFragment.kt`: bóc tách Instagram Reels/Posts chuyên sâu (`extractInstagramVideo()`, thẻ `og:video`, `meta[property="og:video:secure_url"]`), xử lý luồng CDN Meta `cdninstagram.com` & `fbcdn.net` loại bỏ query byte-range (`bytestart`/`byteend`) mà không drop stream.
  2. Bổ sung tính năng tự kiểm thử cho Private Vault (`dialog_private_vault.xml` & `PlayerFragment.kt`): thêm nút "+ Add Demo Video" (`btn_vault_add_demo`) và "+ Import Video" (`btn_vault_import` qua `ActivityResultContracts.GetContent()`), cho phép kiểm thử toàn bộ luồng mã hóa AES-256 ngay trên clean install.
  3. Rà soát và thanh lọc toàn bộ từ khóa "4K", "8K" thành "HD / High Definition" trên toàn bộ 50 thư mục `res/values-*/strings.xml` và layout `activity_payment_wall.xml`.
  4. Nâng `versionCode = 11` và `versionName = "1.0.5"` trong `app/build.gradle.kts`.
  5. Đóng gói AAB Release (`com.ai.video.downloader.videodownloader_v1.0.5_vc11.aab`, 11.05 MB) và APK Release (7.29 MB) ký số hoàn chỉnh.
  6. Kiểm thử thực tế trên thiết bị Samsung Galaxy A07 thật qua ADB thành công 100%.
- **Trạng thái:** `COMPLETED`
- **Tài liệu cập nhật kèm theo:**
  - `PROJECT_ERROR.md`: Bổ sung `[ERR-012]`.
  - `ACQUIREMENTS.md`: Bổ sung `[ACQ-007]`.
  - `CHANGELOG.md`: Bổ sung phiên bản `[1.0.5]`.
  - `UPDATETODOS.md`: Hoàn thành `TASK-011`.

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
