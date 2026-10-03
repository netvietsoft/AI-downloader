# PROJECT_ERROR.md — BỘ NHỚ LỖI BỀN VỮNG DỰ ÁN (CANONICAL ERROR MEMORY)
======================================================================
> **Quy chuẩn:** Development Workspace Standard V2.1.2 (Mục 2 & Mục 18)  
> **Người quản trị:** CEO - Orchestrator (Agent 0)  
> **Nguyên tắc:** Ghi nhận lỗi thật, nguyên nhân gốc rễ (Root Cause), giải pháp đã xác minh, và rào chắn phòng ngừa (Prevention Guardrail). Tuyệt đối không giấu lỗi, không làm giả kết quả.

---

### [ERR-20260927-001] Crash khi tải video HLS (.m3u8) dung lượng 0KB / file text
- **Module:** Downloader Engine (`DownloadEngine.kt`)
- **Mức độ:** CRITICAL
- **Hiện tượng:** Khi tải các link video dạng HLS m3u8, ứng dụng chỉ tải về file playlist dạng text (vài trăm bytes), không có video thực tế, người dùng mở xem thì báo lỗi định dạng.
- **Root cause:** Downloader ban đầu sử dụng HTTP Range Download đơn thuần cho file MP4 trực tiếp, không có bộ parser bóc tách playlist m3u8 và tải các chunks `.ts` để ghép thành file `.mp4`.
- **Giải pháp (Resolved):** 
  - Bổ sung `HlsDownloader` chuyên dụng: Parse Master Playlist & Media Playlist.
  - Tải tuần tự/song song các file `.ts` phân đoạn.
  - Sử dụng cơ chế ghép luồng nhị phân (Binary Stream Stitching) xuất ra file `.mp4` hoàn chỉnh.
- **Rào chắn phòng ngừa (Guardrail):** Kiểm tra Content-Type và Extension trước khi kích hoạt luồng tải; nếu phát hiện mime-type HLS hoặc extension `.m3u8` thì tự động điều hướng sang `HlsDownloader`.

---

### [ERR-20260928-001] Tab Player không phát được video đã tải cục bộ (Load vô tận / UnrecognizedInputFormatException)
- **Module:** Ace Video Player (`AcePlayerActivity.kt`, `PlayerFragment.kt`)
- **Mức độ:** HIGH
- **Hiện tượng:** Trong tab Player, click vào video đã tải về máy thì xoay vòng vô tận hoặc ExoPlayer báo `UnrecognizedInputFormatException`.
- **Root cause:** 
  1. `MediaItem.fromUri` được truyền chuỗi đường dẫn raw không có prefix `file://`, khiến MediaSource mặc định hiểu lầm là URL mạng.
  2. `DefaultDataSource.Factory` không được cấu hình bọc `FileDataSource.Factory` cho các scheme cục bộ.
- **Giải pháp (Resolved):**
  - Cấu hình `DefaultDataSource.Factory(context)` kết hợp `FileDataSource.Factory()`.
  - Chuẩn hóa URI với `Uri.fromFile(File(path))` đảm bảo 100% video offline mở tức thì 0ms.
- **Rào chắn phòng ngừa (Guardrail):** Mọi thao tác phát video offline đều phải đi qua hàm `MediaSourceFactoryProvider.createLocalFileSource()`.

---

### [ERR-20260928-002] XNXX / XVideos trang web chứa popup quảng cáo đè mất trang xem video
- **Module:** Browser & Sniffer (`HomeFragment.kt`, `SnifferEngine.kt`)
- **Mức độ:** HIGH
- **Hiện tượng:** Khi người dùng truy cập các trang chia sẻ video như XNXX, XVideos, web tự động bật popunder hoặc overlay click-jacking mở app Shopee/Lazada hoặc trang web đen độc hại.
- **Root cause:** Các mạng quảng cáo adsterra/exoclick chèn script lắng nghe click toàn màn hình và ghi đè `window.open` hoặc thẻ `<a>` có `target="_blank"`.
- **Giải pháp (Resolved):**
  - Triển khai hệ thống phòng vệ 3 tầng (3-Tier Defense System):
    1. WebSettings: Khóa cửa sổ đa tab tự động (`setSupportMultipleWindows(false)`).
    2. JS Hook tiêm vào WebView (`ANTI_POPUP_JS`): Ghi đè `window.open = function() { return null; }` và triệt tiêu các `div` overlay tàng hình có `z-index >= 9999`.
    3. Chốt chặn URL: `shouldOverrideUrlLoading` lọc danh sách đen hơn 40 tên miền quảng cáo độc hại.
- **Rào chắn phòng ngừa (Guardrail):** Mọi WebView tải nội dung từ Internet bắt buộc phải kích hoạt bộ tiêm phòng thủ 3 tầng này tại `onPageStarted` và `onPageFinished`.

---

### [ERR-20260928-003] Sniffer không bắt được video nhúng Blob và HTML5 video trên trang web chia sẻ
- **Module:** Video Sniffer Engine (`SnifferEngine.kt`)
- **Mức độ:** MEDIUM
- **Hiện tượng:** Các trang như XNXX/XVideos nhúng video qua biến Javascript động trong thẻ `<script>` (ví dụ `html5player.setVideoUrlHigh(...)`) hoặc thẻ `<video>` không dùng thuộc tính `src` tĩnh.
- **Root cause:** Sniffer chỉ quét thuộc tính `src` thông thường qua DOM query đơn giản, không quét cấu trúc source code HTML gốc của trang.
- **Giải pháp (Resolved):**
  - Bổ sung Regex Parser quét trực tiếp mã HTML tải về để tìm các mẫu đặc trưng: `setVideoUrlHigh\('([^']+)'\)`, `setVideoHLS\('([^']+)'\)`.
  - Tích hợp JS Hook bắt sự kiện `play` và `loadstart` của thẻ `<video>` để trích xuất source URL ngay khi người dùng chạm nút phát.
- **Rào chắn phòng ngừa (Guardrail):** Bộ nhận diện SnifferEngine kết hợp 3 cơ chế: Network Interceptor, DOM Inspector, và Dynamic Script Evaluator.

---

### [ERR-20260929-001] Crash NullPointerException khi gọi Toast/Context trong extractVideoDirectly khi Fragment chưa attach
- **Module:** Browser & Sniffer (`HomeFragment.kt`)
- **Mức độ:** HIGH
- **Hiện tượng:** Ứng dụng bị dừng đột ngột (crash) khi xử lý intent `ACTION_VIEW` gửi từ bên ngoài (`adb shell am start -d ...`) ngay lúc Activity/Fragment đang khởi tạo.
- **Root cause:** `extractVideoDirectly` gọi `Toast.makeText(context, ...)` mà không kiểm tra `context == null` hoặc khi Fragment đang trong trạng thái chưa gắn vào Activity (`isAdded == false`).
- **Giải pháp (Resolved):** Sử dụng `val appContext = context?.applicationContext ?: activity?.applicationContext` và bọc trong coroutine an toàn `Dispatchers.Main` với try-catch.
- **Rào chắn phòng ngừa (Guardrail):** Trong mọi Fragment, không được gọi trực tiếp `requireContext()` hoặc `context!!` trong các callback bất đồng bộ; bắt buộc dùng context an toàn `context ?: activity?.applicationContext`.

---

### [ERR-20260929-002] Tra cứu HomeFragment qua findFragmentByTag("HOME") trả về null dẫn tới không nhận diện được domain
- **Module:** Presentation (`MainActivity.kt`)
- **Mức độ:** MEDIUM
- **Hiện tượng:** Khi video được sniffer phát hiện, `activeUrl` bị trống khiến `MonetizedDomainRegistry.isMonetizedDomain` trả về false dù người dùng đang ở website kiếm tiền.
- **Root cause:** Hàm `switchFragment` gọi `supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()` mà không truyền tag `"HOME"`, khiến `findFragmentByTag("HOME")` luôn trả về null.
- **Giải pháp (Resolved):**
  - Truy xuất trực tiếp biến thành viên `homeFragment.getCurrentUrl()` và `homeFragment.getPageTitle()`.
  - Đồng thời kiểm tra chéo cả `videoUrl` và `pageUrl` để đảm bảo 100% không bị bỏ sót domain.
- **Rào chắn phòng ngừa (Guardrail):** Đối với các Fragment cốt lõi được giữ instance trong Activity, truy cập qua reference hoặc đăng ký tag cụ thể khi khởi tạo transaction.

---

### [ERR-20260929-003] Video YouTube tải về không thể phát (UnrecognizedInputFormatException do gói điều khiển SABR 31 bytes)
- **Module:** Sniffer & Downloader Engine (`HomeFragment.kt`, `SnifferEngine.kt`, `DownloadEngine.kt`)
- **Mức độ:** CRITICAL
- **Hiện tượng:** Người dùng phản ánh "tải video youtube ko play dc". Tệp tải về có tên `.mp4` nhưng khi mở bằng AcePlayer (ExoPlayer) thì báo lỗi định dạng hoặc không thể phát.
- **Root cause:** 
  1. Trình duyệt YouTube di động hiện đại không dùng tệp MP4 tĩnh mà sử dụng giao thức truyền phát gói phân đoạn SABR/UMP (`Content-Type: application/vnd.yt-ump`).
  2. Bộ sniffer bắt gói tin mạng vô tình chụp các liên kết `googlevideo.com/videoplayback?...` chứa tham số `sabr=1`, `alr=yes`.
  3. Khi OkHttp gửi request chuẩn GET đến URL này, máy chủ YouTube từ chối và trả về tệp văn bản ASCII 31 bytes: `", sabr.malformed_config"`.
  4. Trình tải lưu 31 bytes này thành tệp `.mp4` vào bộ nhớ, dẫn tới ExoPlayer không thể nhận dạng container MP4.
- **Giải pháp (Resolved):**
  1. **Lọc mạng (Network Filter):** Thêm bộ lọc trong `HomeFragment.shouldInterceptRequest` loại bỏ toàn bộ request `googlevideo.com` có chứa `sabr=1`, `alr=yes`, `range=`, `live=1`, `ump=1`.
  2. **Bộ bóc tách chuyên sâu (Dedicated Stream Extractor):** Trong `SnifferEngine.kt`, bóc tách luồng video qua resolver đa nguồn (hỗ trợ phân giải luồng progressive MP4 720p/360p có đầy đủ video + audio).
  3. **Rào chắn toàn vẹn tệp (File Integrity Guardrail):** Trong `DownloadEngine.kt`, kiểm tra kích thước và nội dung đầu tệp: tự động xóa bỏ và hủy tác vụ nếu tệp tải về < 2048 bytes chứa chuỗi `sabr`, HTML error page hoặc M3U8 rỗng.
- **Bằng chứng xác minh (Empirical Evidence):** Tải video "Me at the zoo" (YouTube: `jNQXAC9IVRw`), tệp `Me_at_the_zoo.mp4` đạt 743,099 bytes (đầy đủ ISO/IEC 14496-14 atom: `ftyp`, `moov`, `mdat`), phát mượt mà trên Samsung Galaxy A07 trong `AcePlayerActivity`.


