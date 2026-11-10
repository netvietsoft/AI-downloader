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

---

## [ERR-006] Vi Phạm Chính Sách Subscriptions Do Không Dùng BillingClient & Hardcode Tiền Tệ VND
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-07 (Google Play Policy Rejection Build 6)
- **Triệu chứng:**
  - Google Play từ chối bản build versionCode 6: *"Violation of Subscriptions policy - Currency differences with prominent display price. Ensure that currency displayed is consistent through multiple screens such as offers page and payment cart within the purchase flow and is appropriately localized for each country that your app is targeting."* (IN_APP_EXPERIENCE-6902.png).
  - Tester / Reviewer ở Mỹ hoặc quốc tế mở Paywall vẫn thấy giá VND (`69.000 ₫`, `499.000 ₫`, `899.000 ₫`).
  - Nút "NÂNG CẤP PRO NGAY" chỉ hiển thị Toast giả lập, không mở Google Play Billing payment cart.
- **Root Cause:**
  1. `SettingsRepository.languageCode` đặt mặc định cứng là `"vi"` thay vì đọc `Locale.getDefault().language`.
  2. `CurrencyFormatter` định dạng tiền tệ dựa trên ngôn ngữ chọn thay vì Country/Locale của thiết bị và không có cơ chế truy vấn giá từ Play Store.
  3. Dự án hoàn toàn thiếu thư viện `BillingClient` của Google Play, dẫn tới không có luồng thanh toán thực tế và không có cart đồng bộ giá.
- **Giải pháp:**
  1. Tích hợp Google Play Billing Library tối thiểu `com.android.billingclient:billing:8.0.0` (theo yêu cầu mới nhất của Google Play Console) và khai báo quyền `com.android.vending.BILLING`.
  2. Xây dựng `BillingManager` quản lý kết nối BillingClient v8, truy vấn ProductDetails động (`weekly_pro`, `yearly_pro`, `lifetime_pro`) qua `QueryProductDetailsResult`, khởi chạy `launchBillingFlow`, xử lý `acknowledgePurchase` và khôi phục giao dịch (`restorePurchases`).
  3. Cập nhật `CurrencyFormatter` ưu tiên định dạng giá thực tế từ `ProductDetails.formattedPrice` của Google Play (đồng nhất 100% giữa offer card và payment cart) kèm fallback theo mã quốc gia ISO thiết bị (`Locale.getDefault().country`).
  4. Cập nhật `SettingsRepository` lấy ngôn ngữ mặc định từ hệ điều hành `Locale.getDefault().language`.
- **Ngăn ngừa:**
  - Tuyệt đối không hardcode tiền tệ hoặc giá trong ứng dụng phát hành toàn cầu.
  - Mọi màn hình Paywall phải lấy giá trực tiếp từ Google Play Billing API.

---

## [ERR-007] Vi Phạm Chính Sách Broken Functionality Do Nút Bấm Không Phản Hồi & Trùng Lặp Top Bar
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-07 (Google Play Policy Rejection Build 6)
- **Triệu chứng:**
  - Google Play từ chối bản build versionCode 6: *"Violation of Broken Functionality policy - App contains icon(s) or button(s) that are not responsive or do not return content."* (IN_APP_EXPERIENCE-3854.png).
  - Người dùng ấn vào nút "NÂNG CẤP PRO NGAY" nhưng không có màn hình mua hàng hay phản hồi chức năng nào được kích hoạt.
  - Sau khi mở khóa Pro, ấn vào thẻ gói thành viên trong Cài đặt lại tiếp tục mở Paywall yêu cầu mua lại.
  - Nút huy hiệu "PRO" trên thanh header của Settings (`tv_pro_badge_top`) không có `OnClickListener`.
  - Màn hình Cài đặt (Settings), Trình phát (Player) và Tiến trình (Progress) bị đè một thanh top bar trùng lặp (`layout_top_bar.xml`) chứa biểu tượng cài đặt (bánh răng) và bộ đếm tab không phản hồi, gây ra hiện tượng 2 thanh tiêu đề và nút chết.
- **Root Cause:**
  1. Các nút điều hướng và hành động trong `PaymentWallActivity` và `SettingsFragment` thiếu `setOnClickListener` hoặc trả về mock Toast thay vì chức năng thật.
  2. `MainActivity.kt` không ẩn `layout_top_bar` khi chuyển tab sang Settings/Player/Progress, dẫn đến layout lồng nhau và icon bánh răng trên top bar khi bấm tại tab Settings không phản hồi.
- **Giải pháp:**
  1. Gắn sự kiện `setOnClickListener` cho toàn bộ các phần tử tương tác: radio button chọn gói, toàn bộ card gói, nút Khôi phục gói mua (`restorePurchases`), nút huy hiệu PRO, nút trạng thái gói.
  2. Bổ sung `showProActiveDialog` trong `SettingsFragment` khi người dùng đã là PRO: hiển thị chi tiết gói đang kích hoạt kèm nút "Quản lý gói trên Google Play" dẫn trực tiếp đến `https://play.google.com/store/account/subscriptions`.
  3. Cập nhật `MainActivity.kt` tự động ẩn `layout_top_bar` khi chọn tab Settings, Player, Progress và chỉ hiển thị ở tab Home/Browser.
- **Ngăn ngừa:**
  - Mọi phần tử có thuộc tính trực quan giống nút (button, card, badge, icon) đều phải có visual ripple feedback và xử lý sự kiện đầy đủ, không để lại dead clicks.

---

## [ERR-008] Xung Đột Kotlin Metadata 2.1.0 Khi Dùng billing-ktx & Tràn Bộ Nhớ Heap Gradle Daemon 2GB
- **Mức độ:** HIGH
- **Ngày phát hiện:** 2026-10-07
- **Triệu chứng:**
  1. Khi cấu hình `com.android.billingclient:billing-ktx:8.0.0`, tác vụ `compileDebugKotlin` báo lỗi: `Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.1.0, expected version is 1.9.0`.
  2. Khi chạy đóng gói đồng thời `assembleRelease bundleRelease`, tiến trình Gradle Daemon bị sập đột ngột: `Gradle build daemon disappeared unexpectedly (JVM crash hs_err_pid)`.
- **Root Cause:**
  1. Bản phân phối `billing-ktx:8.0.0` được build kèm Kotlin metadata 2.1.0, gây xung đột nhị phân với Kotlin compiler `1.9.23` của ứng dụng. Trong khi đó, project chỉ cần các API BillingClient gốc vốn viết bằng Java thuần.
  2. Thông số `gradle.properties` cấu hình bộ nhớ `-Xmx2048m` là không đủ cho tác vụ R8 minification nặng cùng lúc đóng gói AAB.
- **Giải pháp:**
  1. Chuyển sang dùng dependency Java gốc `com.android.billingclient:billing:8.0.0` thay vì `-ktx`. Điều này giải quyết triệt để xung đột metadata với Kotlin 1.9.23, mã nguồn Kotlin gọi trực tiếp trơn tru và Google Play Store xác thực hoàn hảo phiên bản 8.0.0.
  2. Cập nhật cấu hình bộ nhớ trong `gradle.properties`:
     ```properties
     android.suppressUnsupportedCompileSdk=36
     org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
     ```
- **Ngăn ngừa:**
  - Với các SDK Google Play khi project đang dùng Kotlin 1.9.x, ưu tiên dùng artifact core (Java) nếu không thực sự cần coroutines adapter riêng;
  - Cấu hình mức cấp phát RAM tối thiểu 4GB cho Gradle daemon khi có bật R8 minification.

---

## [ERR-009] Play Billing Library v8.0.0 Ném IllegalArgumentException Khi Truy Vấn Đồng Thời Gói SUBS và INAPP
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-12 (Google Play Review Rejection Version Code 8 - Broken Functionality - Crashes: Your app crashes after opening / IN_APP_EXPERIENCE-7161.png)
- **Triệu chứng:**
  - Reviewer/người dùng mở màn hình nâng cấp (Paywall) hoặc mở app thì nhận hộp thoại crash của hệ điều hành Android: *"Clear cache for AI Downloader? AI Downloader closed because this app has a bug. Try clearing the app's cache first, and then reopen the app."*
  - Fatal crash trong logcat:
    ```
    FATAL EXCEPTION: main
    Process: com.ai.video.downloader.videodownloader, PID: 28414
    java.lang.RuntimeException: Unable to resume activity {com.ai.video.downloader.videodownloader/com.nextaitechnology.antidetect.feature.settings.PaymentWallActivity}: java.lang.IllegalArgumentException: All products should be of the same product type.
    at com.android.billingclient.api.QueryProductDetailsParams$Builder.setProductList(SourceFile:67)
    at com.nextaitechnology.antidetect.core.billing.BillingManager.queryAvailableProducts(BillingManager.kt)
    at com.nextaitechnology.antidetect.feature.settings.PaymentWallActivity.onResume(PaymentWallActivity.kt)
    ```
- **Root Cause:**
  - `BillingManager.queryAvailableProducts()` gộp chung cả `ProductType.SUBS` (`weekly_pro`, `yearly_pro`) và `ProductType.INAPP` (`lifetime_pro`) vào trong cùng một `QueryProductDetailsParams.Builder.setProductList(...)`.
  - Trong Google Play Billing Library v8.0.0, phương thức `setProductList` kiểm tra:
    `if (productTypes.size() > 1) { throw new IllegalArgumentException("All products should be of the same product type."); }`
  - Khi `PaymentWallActivity.onResume()` gọi `billingManager.queryAvailableProducts()`, ngoại lệ unchecked này bị ném ngay trên luồng UI chính (Main Thread), làm ứng dụng sập ngay lập tức khi mở màn hình thanh toán/VIP.
- **Giải pháp:**
  1. Tách `QueryProductDetailsParams` thành 2 truy vấn độc lập:
     - 1 truy vấn riêng cho `ProductType.SUBS` (`SKU_WEEKLY`, `SKU_YEARLY`).
     - 1 truy vấn riêng cho `ProductType.INAPP` (`SKU_LIFETIME`).
  2. Bổ sung `try-catch` bao bọc mọi lời gọi BillingClient và kiểm tra null-safety danh sách `productDetailsList`.
  3. Cập nhật `queryActivePurchases()` và `restorePurchases()` kiểm tra cả `SUBS` và `INAPP`.
  4. Đổi tên các sự kiện Firebase reserved (`app_exception` -> `app_error_event`, `first_open` -> `custom_first_open`, ...) để tránh cảnh báo reserved name.
  5. Đưa `compileSdk` và `targetSdk` về chuẩn ổn định 35 (Android 15), tăng `versionCode` lên 9 (`v1.0.3`).
- **Ngăn ngừa:**
  - Tuyệt đối không truyền nhiều `ProductType` khác nhau vào cùng một `QueryProductDetailsParams`.
  - Luôn kiểm thử paywall và luồng billing trên thiết bị vật lý thật với logcat trước khi đóng gói release.

---

## [ERR-010] Sập Tiến Trình Gradle Daemon Do Tràn Bộ Nhớ Compiler Arena (C2 JIT Compiler) Khi Chạy R8 Minification
- **Mức độ:** HIGH
- **Ngày phát hiện:** 2026-10-12
- **Triệu chứng:**
  - Khi thực hiện task `minifyReleaseWithR8`, Gradle Daemon đột ngột bị kill / biến mất (`Gradle build daemon disappeared unexpectedly (it may have been killed or may have crashed)`).
  - File `hs_err_pid*.log` ghi nhận lỗi native Out of Memory:
    ```
    Native memory allocation (malloc) failed to allocate bytes. Error detail: Chunk::new
    Out of Memory Error (arena.cpp:168)
    Current thread: JavaThread "C2 CompilerThread" daemon
    Current CompileTask: C2: com.android.tools.r8.ir.optimize...
    ```
  - Kiểm tra tài nguyên hệ thống cho thấy `AvailPageFile` bị cạn kiệt (dưới 100MB) dù RAM vật lý vẫn còn > 8GB do các tiến trình nền (giả lập Android LDPlayer, IDE...) chiếm giữ commit charge lớn.
- **Root Cause:**
  - Bộ biên dịch JIT tầng C2 (C2 Compiler) của HotSpot JVM tự động phân tích và tối ưu hóa chuyên sâu các hàm rất lớn của thư viện R8 (`com.android.tools.r8...`).
  - Quá trình xây dựng đồ thị SSA và IR của C2 tiêu thụ lượng lớn bộ nhớ native heap trong `arena.cpp`. Khi Windows commit limit đạt ngưỡng tối đa, việc phân bổ native chunk thất bại khiến toàn bộ tiến trình JVM sập đột ngột.
- **Giải pháp:**
  1. Thêm cờ `-XX:TieredStopAtLevel=1` vào `org.gradle.jvmargs` trong `gradle.properties`: Giới hạn HotSpot dừng ở cấp độ C1 JIT (Client Compiler), vô hiệu hóa hoàn toàn C2 JIT optimization.
  2. Bổ sung `-XX:ReservedCodeCacheSize=128m` và `org.gradle.workers.max=4` để giảm thiểu lượng thread và native code cache.
  3. Tách rời tác vụ đóng gói `bundleRelease` và `assembleRelease` thành 2 lệnh độc lập thay vì gom chung trong một lần build.
- **Ngăn ngừa:**
  - Với môi trường Windows có dung lượng Virtual Memory commit bị giới hạn, luôn cấu hình `-XX:TieredStopAtLevel=1` cho Gradle daemon phục vụ đóng gói Android R8 Release.

---

## [ERR-011] Google Play Console Từ Chối Bản Build Do Target API Level 35 Thấp Hơn Yêu Cầu Tối Thiểu API Level 36
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-12 (Google Play Console Release Error - Version Code 9)
- **Triệu chứng:**
  - Khi tải lên bản build Version Code 9 (AAB), Google Play Console chặn phát hành với lỗi:
    *"1 message for version code 9 - Error: Your app currently targets API level 35 and must target at least API level 36 to ensure that it is built on the latest APIs optimised for security and performance. Learn more"*
- **Root Cause:**
  - Bản build Version Code 9 cấu hình `targetSdk = 35` và `compileSdk = 35`.
  - Quy định bắt buộc mới nhất của Google Play Console yêu cầu mọi ứng dụng phát hành mới hoặc cập nhật phải target tối thiểu Android API level 36.
  - Khi Version Code 9 đã được tải lên và lưu vào Thư viện gói ứng dụng (App Bundle Explorer) của Google Play Console, không thể upload đè lại cùng số version code đó.
- **Giải pháp:**
  1. Nâng cấp `compileSdk = 36` và `targetSdk = 36` trong `app/build.gradle.kts`.
  2. Bổ sung `android.suppressUnsupportedCompileSdk=36` trong `gradle.properties` để ngăn chặn cảnh báo từ Android Gradle Plugin.
  3. Tăng `versionCode = 10` và `versionName = "1.0.4"` để Google Play Console chấp nhận gói phát hành mới.
  4. Đóng gói và ký số lại toàn bộ bundle phát hành `com.ai.video.downloader.videodownloader_v1.0.4_vc10.aab`.
- **Ngăn ngừa:**
  - Luôn kiểm tra yêu cầu Target API tối thiểu hiện hành của Google Play Console trước khi cấu hình `targetSdk` cho các bản build release;
  - Khi một bản build gặp lỗi sau khi đã upload lên Play Console, luôn tăng `versionCode` lên ít nhất +1 trước khi đóng gói lại.

---

## [ERR-012] Google Play Console Từ Chối Do Vi Phạm Chính Sách Misleading Claims & In-App Experience
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-21 (Google Play Rejection - Version Code 10)
- **Triệu chứng:**
  - Google Play gửi thông báo từ chối duyệt: *"Issue found: Violation of Misleading Claims policy"*
  - 1. Missing features in description: (a) App description hứa hẹn tự động nhận diện và tải video trên mọi trang web mạng xã hội, nhưng reviewer kiểm tra link Instagram (`https://www.instagram.com/reel/DCync3DTMtI`) trong WebView thì nút Download FAB không xuất hiện; (b) Hứa hẹn tải video độ phân giải "4K, 8K, 2K" và các định dạng hiếm "MKV, AVI, FLV, MOV" trong khi engine chỉ phục vụ stream web di động; (c) Quảng cáo tính năng "Private Vault" (Két video riêng tư) nhưng khi mở app mới tinh (clean install), kho trống rỗng và không có nút thêm/nhập video trực tiếp từ máy để reviewer kiểm thử.
  - 2. Missing features in screenshots: Ảnh chụp màn hình mock-up trên Store Listing chứa các nút chức năng và badge "4K / All Social Media Download" không khớp với giao diện thực tế.
  - 3. Reviewer đính kèm ảnh chụp màn hình paywall tiếng Tây Ban Nha `IN_APP_EXPERIENCE-6747.png` hiển thị: *"Soporte para resolución 4K y 8K"*.
- **Root Cause:**
  - 1. Instagram Web Sniffing: Trong `HomeFragment.kt`, hàm `inspectUrlForVideo` tự động loại bỏ mọi URL có chứa `bytestart` hoặc `byteend`, mà các luồng video CDN của Instagram (`cdninstagram.com`, `fbcdn.net`) luôn kèm query byte-range này; đồng thời danh sách đuôi mở rộng chưa nhận diện các đường dẫn CDN Meta không có đuôi `.mp4`.
  - 2. Private Vault: `dialog_private_vault.xml` và `PlayerFragment.kt` chỉ hiển thị video đã tải từ trước qua browser, không có nút nhập file từ bộ nhớ máy (`+ Import Video`) hoặc nút tạo video mẫu (`+ Add Demo Video`), khiến tính năng này không thể kiểm thử độc lập trên môi trường máy test của Google.
  - 3. Khai báo quá mức (Over-promising Claims): Có hơn 50 file `strings.xml` và file layout `activity_payment_wall.xml` chứa các từ khóa "4K", "8K" vi phạm nghiêm ngặt chính sách Misleading Claims của Google Play.
- **Giải pháp:**
  1. Nâng cấp `SnifferEngine.kt` và `HomeFragment.kt`:
     - Tích hợp bóc tách dữ liệu Instagram Reels/Posts chuyên sâu (`extractInstagramVideo()`, thẻ `og:video`, `meta[property="og:video:secure_url"]`, DOM HTML inspection).
     - Điều chỉnh `inspectUrlForVideo()` xử lý riêng cho `cdninstagram.com` và `fbcdn.net`: giữ nguyên stream, tự động loại bỏ các tham số `bytestart`/`byteend` để tải trọn vẹn video file.
     - Bổ sung hook Javascript cho `<video>` event `play` và `loadeddata` tự động thông báo stream video ngay khi video chạy.
  2. Bổ sung tính năng độc lập cho Private Vault:
     - Thêm 2 nút bấm thao tác nhanh: `btn_vault_add_sample` ("+ Add Demo Video") và `btn_vault_import_file` ("+ Import Video") ngay trong giao diện kho đã mở khóa.
     - Tích hợp `ActivityResultContracts.GetContent()` cho phép người dùng nhập trực tiếp bất kỳ video nào từ máy vào két mã hóa AES-256.
     - Hỗ trợ tạo video mẫu demo được bảo vệ an toàn để kiểm thử ngay lập tức 100% flow mà không cần tải từ web trước.
  3. Thanh lọc triệt để từ khóa 4K / 8K:
     - Tự động quét và chuẩn hóa toàn bộ 50 thư mục `res/values-*/strings.xml`, chuyển "4K / 8K" thành "HD / High Definition" trung thực và chính xác.
     - Sửa ghi chú và mô tả trong layout `activity_payment_wall.xml`.
  4. Nâng version code lên 11 (`versionName = "1.0.5"`), build thành công bản phát hành signed AAB (11.05MB) và APK (7.29MB), cài đặt và kiểm thử thực tế trên thiết bị Samsung Galaxy A07 qua ADB.
  5. Cung cấp bộ văn bản mô tả Store Listing tiếng Anh và tiếng Tây Ban Nha chuẩn mực không vi phạm chính sách.
- **Ngăn ngừa:**
  - Tuyệt đối không đưa các tuyên bố tính năng 4K/8K, tải YouTube/mạng xã hội không được hỗ trợ vào text mô tả Store Listing hoặc strings/paywall trong app.
  - Mọi tính năng bảo mật, két riêng tư hoặc player phải có khả năng kiểm thử độc lập (self-contained / testable on clean install).

---

## [ERR-013] Vi Phạm Chính Sách Subscriptions Do Sai Lệch Tiền Tệ Trên Offer Page & Race Condition BillingClient
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-10-28 (Google Play Review Rejection Version Code 11 - Subscriptions Policy Rejection / IN_APP_EXPERIENCE-6840.png)
- **Triệu chứng:**
  - Google Play gửi thông báo từ chối duyệt: *"Issue found: Violation of Subscriptions policy - Currency differences with prominent display price. Ensure that currency displayed is consistent through multiple screens such as offers page and payment cart within the purchase flow and is appropriately localized for each country that your app is targeting."*
  - Reviewer đính kèm ảnh 6 khung hình `IN_APP_EXPERIENCE-6840.png` thể hiện:
    1. Khi mở Paywall, các gói cước hiển thị giá tĩnh hoặc giá USD (`$2.99`, `$19.99`, `$39.99`, kèm subtext `"Just $1.66 / month • Save 70%"`), trong khi thiết bị test tại các quốc gia khác nhau.
    2. Khi ấn vào nút "SUBSCRIBE NOW" cho cả 3 gói Weekly, Yearly, Lifetime, giỏ hàng Google Play không mở ra mà hiện AlertDialog unlocalized tiếng Việt: *"AI Downloader Pro Subscription: Sản phẩm đang được đồng bộ từ Google Play. Vui lòng thử lại trong giây lát."*
- **Root Cause:**
  1. Sai lệch đơn vị tiền tệ trên thẻ gói Năm (`yearlySubtext`): Trong `CurrencyFormatter.kt`, khi Google Play trả về giá thực tế (ví dụ: `19,99 €` hoặc `499.000 ₫`), trường `yearlySubtext` vẫn lấy từ `fallback.yearlySubtext` vốn bị hardcode ký tự USD `$` (`"Just $1.66 / month • Save 70%"`), gây ra tình trạng giá chính là EUR/VND còn dòng chữ phụ bên dưới là USD.
  2. Bảng giá dự phòng (Fallback) chỉ hỗ trợ 11 quốc gia cứng, các quốc gia còn lại (Canada, Úc, Singapore...) đều bị fallback sang USD `$`.
  3. Layout XML `activity_payment_wall.xml` chứa text mặc định hardcode `$2.99`, `$19.99`, `$39.99` gây chớp giá USD khi màn hình vừa mở.
  4. Lỗi Race Condition trong `BillingManager.kt`: `startConnection()` được gọi trong `init`. Khi `PaymentWallActivity.onResume()` chạy ngay sau đó, `billingClient.isReady` vẫn đang `false`, kích hoạt cuộc gọi `startConnection()` thứ hai. Google Play BillingClient từ chối/bỏ qua các cuộc gọi kết nối đồng thời, khiến callback không được gọi và `_productDetailsMap` bị rỗng. Khi người dùng bấm nút mua, hàm kiểm tra `details == null` và hiển thị thông báo lỗi tiếng Việt hardcode.
  5. Các chuỗi thông báo lỗi thanh toán và hội thoại quản lý thuê bao trong `BillingManager.kt`, `PaymentWallActivity.kt` và `SettingsFragment.kt` chưa được quốc tế hóa (hardcoded tiếng Việt), hiển thị unlocalized trên thiết bị của reviewer.
- **Giải pháp:**
  1. Cập nhật `BillingManager.kt`:
     - Xây dựng hàng đợi kết nối an toàn thread-safe với cờ `@Volatile var isConnecting` và `pendingReadyCallbacks`.
     - Bổ sung helper `getProductDetails(sku)` và `getRecurringPricingPhase(sku)`.
     - Thay thế toàn bộ chuỗi hardcoded bằng chuỗi tài nguyên `appContext.getString(R.string.*)` đã được đa ngữ hóa.
  2. Cập nhật `CurrencyFormatter.kt`:
     - Tự động trích xuất `pricingPhase.priceAmountMicros` và `priceCurrencyCode` từ Google Play Billing để tính toán `monthlyAmount = (priceAmountMicros / 1_000_000.0) / 12.0` và định dạng bằng `formatAmountWithCurrency` chuẩn theo Locale và Currency Code thực tế, ghép vào template string `paywall_subtext_yearly_format`.
     - Mở rộng bảng giá fallback hỗ trợ Canada (CAD), Úc (AUD), Singapore (SGD) và tự động nhận diện currency của thiết bị để định dạng nếu không phải USD.
  3. Cập nhật `activity_payment_wall.xml`: Chuyển các giá trị tĩnh thành `@string/paywall_price_loading`, `@string/paywall_subtext_weekly`, `@string/paywall_subtext_lifetime`.
  4. Cập nhật `PaymentWallActivity.kt`: Thêm luồng kiểm tra cache ProductDetails và hiển thị dialog kết nối nếu đang đồng bộ, loại bỏ hoàn toàn dead click và chớp giá sai.
  5. Cập nhật `SettingsFragment.kt`: Quốc tế hóa toàn bộ dialog thông tin gói thuê bao đang kích hoạt.
  6. Bổ sung 20 resource strings trong `strings.xml` và `values-vi/strings.xml`.
  7. Nâng `versionCode = 12` và `versionName = "1.0.6"`.
  8. Viết 11 unit test trong `CurrencyFormatterTest.kt` kiểm chứng 100% currency parity.
- **Ngăn ngừa:**
  - Mọi dòng chữ liên quan đến giá chia nhỏ (subtext, monthly breakdown, daily cost) đều phải được tính toán động từ `ProductDetails` của Google Play với cùng mã tiền tệ `priceCurrencyCode`, tuyệt đối không hardcode ký hiệu tiền tệ hoặc con số tĩnh.
  - Luôn sử dụng hàng đợi kết nối an toàn cho `BillingClient.startConnection()` để tránh race condition khi mở Activity.

---

## [ERR-014] Mất Mã Quốc Gia ISO Khi Khởi Tạo Locale(langCode) & Xung Đột Ký Hiệu Tiền Tệ Giữa Play Store Và JVM NumberFormat
- **Mức độ:** CRITICAL
- **Ngày phát hiện:** 2026-11-10 (Google Play Review Rejection Version Code 12 - Subscriptions Policy Rejection: Currency differences with prominent display price)
- **Triệu chứng:**
  - Google Play tiếp tục từ chối bản build Version Code 12 với thông báo: *"Violation of Subscriptions policy - Currency differences with prominent display price. Ensure that currency displayed is consistent through multiple screens such as offers page and payment cart within the purchase flow and is appropriately localized for each country that your app is targeting."*
  - Ảnh chụp màn hình của Reviewer hiển thị cả 3 gói Weekly, Yearly, Lifetime trên Paywall đều mang giá fallback USD (`$2.99`, `$19.99` kèm `Just $1.66 / month • Save 70%`, `$39.99`) dù thiết bị kiểm thử đặt tại quốc gia mục tiêu ngoài Mỹ và các mã `weekly_pro`, `yearly_pro`, `lifetime_pro` đã được Active trên Google Play Console.
- **Root Cause:**
  1. **Mất mã quốc gia ISO 3166-1 trong `LocaleHelper.kt`:** Ngay khi khởi động ứng dụng tại `NextAIApplication.attachBaseContext()`, hàm `LocaleHelper.wrapContext()` và `LocaleHelper.applyLanguage()` khởi tạo `val locale = Locale(langCode)` (ví dụ `Locale("en")` chỉ có mã ngôn ngữ, `country = ""`) rồi gọi `Locale.setDefault(locale)`. Hành động này xóa trắng thuộc tính `Locale.getDefault().country` (`""`) trên toàn bộ thiết bị toàn cầu (như Ấn Độ `en_IN`, Anh `en_GB`, Philippines `en_PH`, Singapore `en_SG`, Úc `en_AU`, Canada `en_CA`, Ireland `en_IE`, Ba Lan `pl_PL`, Thổ Nhĩ Kỳ `tr_TR`...). Khi `CurrencyFormatter.getFallbackPricing()` chạy, `country` luôn bằng `""` và `Currency.getInstance(Locale("en"))` ném `IllegalArgumentException`, dẫn đến 100% thiết bị rơi về nhánh mặc định cuối cùng là USD (`$2.99`, `$19.99`, `Just $1.66 / month • Save 70%`, `$39.99`).
  2. **Gọi trùng lặp IPC `queryProductDetailsAsync` & hiển thị sớm giá fallback:** Trong `BillingManager.onBillingSetupFinished()`, hệ thống vừa gọi `queryAvailableProducts()` vừa kích hoạt `callbacks` gọi tiếp `queryAvailableProducts()` tại cùng một mili-giây, đồng thời `PaymentWallActivity.onCreate()` gọi `refreshPricing()` ngay lập tức ở frame 0 khi `ProductDetails` chưa tải xong, làm ghi đè `@string/paywall_price_loading` bằng giá fallback USD `$2.99 / $19.99 / $39.99`. Sau đó khi bấm `SUBSCRIBE NOW`, `handleSubscribe()` tải lại `ProductDetails` và mở thẳng Google Play Payment Cart bằng nội tệ mà không gọi `refreshPricing()` cập nhật lại màn hình Offer phía dưới.
  3. **Sai lệch ký hiệu tiền tệ khi tính giá chia theo tháng (`NumberFormat` vs `formattedPrice`):** Khi định dạng `monthlyAmount` bằng `NumberFormat.getCurrencyInstance(Locale("en"))`, JVM sinh ra mã ISO hoặc ký hiệu khác với chuỗi `formattedPrice` của Google Play (ví dụ `IDR 24,917` dưới `Rp 299.000`, `CA$2.25` dưới `$26.99`, hoặc `US$1.67` dưới `$19.99`), và `subscriptionOfferDetails.firstOrNull()` có nguy cơ lấy nhầm phase Free Trial thay vì Base Plan trả phí định kỳ.
- **Giải pháp:**
  1. **Bảo toàn tuyệt đối mã quốc gia ISO trong `LocaleHelper.kt`:**
     - Xây dựng `getSystemDeviceLocale()`, `getDeviceCountryCode(context)` (ưu tiên SIM Country ISO -> Network Country ISO -> `Resources.getSystem().configuration.locales[0].country` -> `TIMEZONE_TO_COUNTRY` -> `DEFAULT_COUNTRY_FOR_LANGUAGE` cho đủ 50 ngôn ngữ) và `resolveLocaleWithCountry(langCode, context)` luôn tạo `Locale(lang, country)` có đủ mã quốc gia 2 ký tự.
  2. **Single-Flight Query & Canonical Offer Selection trong `BillingManager.kt`:**
     - Cơ chế hàng đợi truy vấn đơn luồng (`isQueryingProducts` + `pendingQueryCallbacks` + `isLoadingProducts: StateFlow<Boolean>`) kèm tự động retry có backoff khi dịch vụ ngắt kết nối hoặc chưa tải đủ 3 SKU.
     - Hàm `getSelectedSubscriptionOffer()` ưu tiên Base Plan (`offerId == null` có `priceAmountMicros > 0`) và `getRecurringPricingPhase()` lấy phase trả phí định kỳ (`lastOrNull { it.priceAmountMicros > 0 }`), dùng chung cho cả hiển thị giá trên Paywall và truyền `offerToken` vào `launchBillingFlow`.
  3. **Sao chép chính xác định dạng Google Play (`formatAmountMatchingPlayPrice`) trong `CurrencyFormatter.kt`:**
     - Tách trực tiếp `prefix`, `suffix`, dấu phân cách thập phân và phân nhóm hàng nghìn từ chuỗi `formattedPrice` của Google Play để định dạng chi phí chia theo tháng, đảm bảo khớp 100% ký hiệu tiền tệ trên mọi quốc gia.
     - Mở rộng bảng ánh xạ tiền tệ phủ kín 50 ngôn ngữ và các quốc gia/vùng lãnh thổ mục tiêu.
  4. **Đồng bộ hiển thị trên `PaymentWallActivity.kt`:**
     - Giữ trạng thái `@string/paywall_price_loading` trong khi lần truy vấn đầu tiên từ Google Play đang diễn ra (`isLoadingProducts == true`) để không chớp giá giả lập.
     - Bắt buộc gọi `refreshPricing()` ngay lập tức trước khi khởi chạy `startBillingFlowInternal()` mở giỏ hàng Google Play.
  5. Nâng `versionCode = 13` và `versionName = "1.0.7"`, bổ sung 14 bài kiểm thử đơn vị trong `CurrencyFormatterTest.kt`.
- **Ngăn ngừa:**
  - Tuyệt đối không bao giờ gọi `Locale.setDefault(Locale(langCode))` thiếu tham số `country` vì sẽ làm hỏng toàn bộ hệ thống nhận diện vùng/tiền tệ của JVM và Android.
  - Khi hiển thị giá chia nhỏ (theo tháng/tuần) từ gói thuê bao Google Play, luôn sao chép tiền tố/hậu tố tiền tệ trực tiếp từ `ProductDetails.PricingPhase.formattedPrice`.


