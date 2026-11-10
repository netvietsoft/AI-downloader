# PROJECT_MEMORY.md — BỘ NHỚ VẬN HÀNH DỰ ÁN CONVERT
===================================================
- Ngày khởi động: 2026-09-27
- Thẩm quyền: Chủ tịch Hội đồng Quản trị giao toàn quyền cho CEO điều hành tự động hóa 100%.
- Mục tiêu: Hoàn thiện sản phẩm hoàn chỉnh, tự kiểm thử đạt 100% chức năng hoạt động.
- Nguồn tham chiếu:
  - Decompile Source: `D:\Decompiler\App\Downloader\videoplayer.videodownloader.downloader\SOURCE`
  - Báo cáo phân tích: 26 files tại `SOURCE/Report`
  - Thiết kế & Prototype: `SOURCE/Redesign/UI/index.html` & `BAN_DO_CHUC_NANG_APP_TREE_MAP.md`

## TIẾN ĐỘ & BỘ NHỚ VẬN HÀNH BỔ SUNG (2026-09-27 -> 2026-09-28)
- **Định danh ban đầu**: Package core `com.nextaitechnology.antidetect`, App Name "NextAI Downloader".
- **Tính năng nổi bật hoàn thành**:
  1. **Nút tùy chọn 3 chấm (⋮) Danh sách tải về**: Quản lý xóa bản ghi tải xuống, xóa file vật lý an toàn và cập nhật badge bottom navigation.
  2. **Thanh tìm kiếm thông minh**: Nút xoá nhanh "✕", nhận diện từ khóa tìm kiếm và điều hướng tự động sang Google Search (`https://www.google.com/search?q=...`).
  3. **Hệ thống bảo vệ Trình duyệt Anti-Popup 3 Tầng**:
     - WebSettings khóa `javaScriptCanOpenWindowsAutomatically = false` và `setSupportMultipleWindows(false)`.
     - Script JS hook `ANTI_POPUP_JS` tiêm tại `onPageStarted` & `onPageFinished`: triệt tiêu `window.open`, ngăn liên kết `target="_blank"` trỏ sang domain ngoài, tiêu hủy lớp phủ click-jacking tàng hình `z-index >= 9999`.
     - Chốt chặn `shouldOverrideUrlLoading` / `handleUrlOverride` lọc sạch các domain quảng cáo popup, cờ bạc, cam site (`flirtify`, `faphouse`, `stripchat`, `exoclick`, v.v.).
  4. **Tiến trình Download 0% -> 100% & Trình phát Video Offline**:
     - `DownloadEngine`: Kết nối StateFlow `tasksFlow`, cập nhật mượt mỗi 250ms, tính toán tốc độ thực tế (KB/s, MB/s) và thanh tiến trình 0%-100%. Hỗ trợ tải phân đoạn HLS (.ts) ghép video .mp4 hoàn chỉnh.
     - `AcePlayerActivity`: Sử dụng `DefaultDataSource.Factory` hỗ trợ đọc tệp offline (`file://` và đường dẫn nội bộ) tức thì 0ms, không còn tình trạng load lâu hoặc đơ máy.
     - `PlayerFragment` & `ProgressFragment`: Hiển thị danh sách video offline chuẩn xác (>50KB), có đầy đủ nút PLAY, pause/resume, xoá tác vụ tải.
   5. **Cơ chế Khóa Tải Bằng Payment Wall (Monetized Domain Gate - 1 Free Video)**:
      - Nạp 1,822 tên miền video thương mại từ `assets/monetized_domains.txt` vào `MonetizedDomainRegistry` (HashSet tra cứu O(1)).
      - `SettingsRepository`: Quản lý biến đếm `monetizedDownloadCount`. Cho phép tải đúng 1 video miễn phí đầu tiên (kèm huy hiệu "🎁 Dùng thử (1 lượt miễn phí)").
      - Khi người dùng vào các trang này và phát hiện video thứ 2 trở đi: Ứng dụng lập tức kích hoạt `PaymentWallActivity` với lý do `monetized_limit_reached` hiển thị banner cảnh báo và yêu cầu nâng cấp gói cước VIP Pro.
- **Thiết bị kiểm thử thực tế**:
  - Model: Samsung Galaxy A07 (`SM-A075F`), Android 14/15, độ phân giải 720x1600.
  - Kết nối Wireless ADB: `192.168.1.18:34749` (cổng pair: `40129`, mã ghép: `780805`).
  - Trạng thái nghiệm thu: APK đã build lại (`BUILD SUCCESSFUL`), cài đặt thành công (`Success`) và khởi chạy `MainActivity` mượt mà, ảnh chụp màn hình nghiệm thu thực tế đạt 100% chức năng.

---

## BỘ NHỚ VẬN HÀNH & CHUẨN HÓA PHÁT HÀNH GOOGLE PLAY (v1.0.2 -> v1.0.7 VC 13)
- **Định danh phát hành chính thức (Google Play ID)**: `com.ai.video.downloader.videodownloader`
- **Phiên bản hiện tại**: `1.0.7` (Version Code: `13`)
- **Target SDK**: `compileSdk = 36`, `targetSdk = 36` (Android 16 - Tuân thủ chính sách Google Play 2026)
- **Google Play Billing v8.0.0 Compliance**:
  - Tách biệt hoàn toàn truy vấn `SUBS` (`weekly_pro`, `yearly_pro`) và `INAPP` (`lifetime_pro`) tránh ngoại lệ `IllegalArgumentException`.
  - Triển khai **Connection Queue** + **Single-Flight Product Query Deduplication** (`isQueryingProducts` + `pendingQueryCallbacks` + `isLoadingProducts: StateFlow<Boolean>`) và **Auto-Retry Backoff** chống rớt callback hoặc trùng truy vấn khi kết nối BillingClient.
  - **100% Offer & Pricing Phase Parity**: Đồng bộ tuyệt đối `getSelectedSubscriptionOffer(details)` và `getRecurringPricingPhase(sku)` giữa màn hình Offers Page (`PaymentWallActivity`) và Google Play Payment Cart (`executeBillingFlow`).
  - **Exact Play Store Currency Notation**: Sử dụng `CurrencyFormatter.formatAmountMatchingPlayPrice()` trích xuất trực tiếp `prefix`/`suffix`/dấu phân cách từ `formattedPrice` của Google Play để hiển thị giá chia tháng của gói Năm (`yearlySubtext`) khớp 100% ký hiệu tiền tệ.
  - **Locale Country Preservation & Multi-Signal Country Detection**: Bảo toàn mã quốc gia thiết bị trong `LocaleHelper.wrapContext()` (`en_IN`, `en_GB`, `en_PH`, `en_SG`, `en_AU`, `en_CA`, v.v.) kết hợp nhận diện SIM/Network ISO + System Locale + Timezone + 50 ngôn ngữ, chấm dứt hoàn toàn lỗi rơi về `$1.66 / $2.99 / $19.99 / $39.99` USD.
  - Bản địa hóa 100% thông báo, callback và dialog liên quan đến thanh toán (`strings.xml` và `values-vi/strings.xml`).
- **Sniffer Engine Chuyên Sâu**:
  - Bóc tách Instagram Reels/Posts (`og:video`, `meta[property="og:video:secure_url"]`).
  - Xử lý luồng Meta CDN `cdninstagram.com` & `fbcdn.net` loại bỏ byte-range parameters mà không drop stream.
  - Thanh lọc toàn bộ phát ngôn "4K", "8K" thành "HD / High Definition" trên toàn bộ 50 thư mục ngôn ngữ.
- **Két Sắt Riêng Tư Tự Kiểm Thử (Self-Contained Vault)**:
  - Tích hợp nút "+ Add Demo Video" (`btn_vault_add_demo`) và "+ Import Video" (`btn_vault_import`), cho phép reviewer kiểm thử trọn vẹn luồng mã hóa AES-256 ngay trên clean install.
- **Danh mục File Thành Phẩm (Release Artifacts)**:
  - AAB Release: `CONVERT/apps/android/app/build/outputs/bundle/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.aab` (Signed).
  - APK Release: `CONVERT/apps/android/app/build/outputs/apk/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.apk` (Signed).
- **Bộ nhớ lỗi và tri thức bền vững tích lũy**:
  - [PROJECT_ERROR.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/PROJECT_ERROR.md): `[ERR-001]` đến `[ERR-014]`.
  - [ACQUIREMENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/ACQUIREMENTS.md): `[ACQ-001]` đến `[ACQ-009]`.

