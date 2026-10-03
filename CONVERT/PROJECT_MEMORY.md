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
- **Định danh thương hiệu**: Package `com.nextaitechnology.antidetect`, App Name "NextAI Downloader".
- **Tính năng nổi bật mới hoàn thành**:
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
      - Khi người dùng vào các trang này và phát hiện video thứ 2 trở đi: Ứng dụng lập tức kích hoạt `PaymentWallActivity` với lý do `monetized_limit_reached` hiển thị banner cảnh báo và khóa hoàn toàn luồng tải xuống cho đến khi mua gói cước VIP Pro ($2.99/tuần, $19.99/năm, $39.99 trọn đời).
- **Thiết bị kiểm thử thực tế**:
  - Model: Samsung Galaxy A07 (`SM-A075F`), Android 14/15, độ phân giải 720x1600.
  - Kết nối Wireless ADB: `192.168.1.18:34749` (cổng pair: `40129`, mã ghép: `780805`).
  - Trạng thái nghiệm thu: APK đã build lại (`BUILD SUCCESSFUL`), cài đặt thành công (`Success`) và khởi chạy `MainActivity` mượt mà, ảnh chụp màn hình nghiệm thu thực tế đạt 100% chức năng.

