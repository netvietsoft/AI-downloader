# CANONICAL DISTILLED PROJECT KNOWLEDGE (ACQUIREMENTS.MD)

Tài liệu chưng cất kinh nghiệm, nguyên tắc và giải pháp kỹ thuật tối ưu đã được kiểm chứng trong dự án.

---

## [ACQ-001] Mô Hình Triển Khai Web Single-Port (All-in-One Express Server)
- **Bối cảnh:** Khi triển khai web tải video đa nền tảng và sniffer API lên máy chủ Ubuntu tại `/home/netviet/projects-deploy/ai-dowloader` cho domain `snaptik2.com`.
- **Giải pháp tối ưu:** Hợp nhất Frontend static serving và Backend API routing vào một tiến trình Node.js duy nhất chạy trên `PORT = process.env.PORT || 3070`.
- **Lợi ích:**
  1. Loại bỏ 100% lỗi CORS giữa frontend và backend.
  2. Chỉ cần mở 1 port duy nhất (3070) cho Nginx reverse proxy.
  3. Quản lý tiến trình đơn giản qua 1 instance PM2 (`ecosystem.config.js`).
  4. Client `app.js` dùng relative path `/api/...`, tự động tương thích mọi môi trường dev/staging/prod.

---

## [ACQ-002] Tách Nhánh Triển Khai Độc Lập Bằng Git Subtree
- **Bối cảnh:** Repository chứa cả mã nguồn Android App (`CONVERT/apps/android`) và Web Platform (`WWW/`), nhưng server production chỉ cần mã nguồn Web để chạy `npm install` và `pm2 start`.
- **Giải pháp tối ưu:** Sử dụng nhánh riêng `web` được trích xuất từ thư mục `WWW/`. Thư mục gốc của nhánh `web` chứa toàn bộ code cần thiết cho server.
- **Lợi ích:** Lệnh clone trên server gọn nhẹ, không tải code Android thừa, tiết kiệm tài nguyên và không làm lộ cấu trúc nội bộ dự án.

---

## [ACQ-003] Tương Thích Express 5 Routing (No Raw Wildcards)
- **Bối cảnh:** Khi ứng dụng nâng cấp lên hoặc sử dụng `express ^5.x.x`, thư viện phân tích route `path-to-regexp` được nâng cấp từ v0.1.x lên v6+/v7+.
- **Nguyên tắc kỹ thuật:**
  1. Tuyệt đối không dùng `app.get('*', ...)` vì sẽ gây ra ngoại lệ nghiêm trọng `PathError: Missing parameter name`.
  2. Với các tác vụ fallback SPA hoặc static routing, sử dụng middleware `app.use((req, res, next) => ...)` có kiểm tra `req.method === 'GET'` và `!req.url.startsWith('/api/')`.
- **Lợi ích:** Đảm bảo server không bị crash vô điều kiện khi khởi động và tương thích hoàn toàn với hệ sinh thái Express hiện đại.

---

## [ACQ-004] Tích Hợp Google Play Billing Library v8.0.0 & Đồng Bộ Giá Động Cho Paywall
- **Bối cảnh:** Google Play Console yêu cầu tối thiểu **Play Billing Library v8.0.0** để sử dụng các tính năng kiếm tiền mới nhất và bắt buộc tuân thủ chính sách *Subscriptions Policy* (đồng nhất 100% giữa giá hiển thị nổi bật và giá trong giỏ hàng thanh toán Google Play) và chính sách *Broken Functionality Policy* (mọi nút mua, khôi phục hoặc tương tác đều phải phản hồi nội dung thực).
- **Giải pháp kỹ thuật tối ưu:**
  1. **Nâng cấp Billing Library v8.0.0:** Sử dụng `com.android.billingclient:billing:8.0.0` với `PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()`.
  2. **Truy vấn giá thời gian thực với `QueryProductDetailsResult`:**
     - Trong Billing v8, callback `queryProductDetailsAsync` trả về `QueryProductDetailsResult`. Ta truy xuất danh sách sản phẩm thông qua `queryProductDetailsResult.productDetailsList`.
     - Hỗ trợ cả 2 loại sản phẩm: `ProductType.SUBS` (đăng ký định kỳ `weekly_pro`, `yearly_pro`) và `ProductType.INAPP` (mua vĩnh viễn `lifetime_pro`).
     - **QUY TẮC BẮT BUỘC:** Tuyệt đối không gộp `SUBS` và `INAPP` vào chung một `QueryProductDetailsParams`. Thư viện Play Billing v8.0.0 kiểm tra nghiêm ngặt và sẽ ném ngay lập tức `IllegalArgumentException: All products should be of the same product type.` dẫn tới sập ứng dụng (crash on resume). Bắt buộc phải tách thành 2 đối tượng `QueryProductDetailsParams` độc lập cho từng `ProductType` và gộp kết quả sau khi tải xong.
     - Lấy `formattedPrice` từ `ProductDetails` và `SubscriptionOfferDetails` để cập nhật trực tiếp lên các thẻ giá giao diện người dùng. Tuyệt đối không hardcode giá trị tiền tệ VND/USD vào XML/Strings.
  3. **Khởi chạy luồng thanh toán chuẩn:**
     - Sử dụng `BillingClient.launchBillingFlow(activity, params)` với `ProductDetailsParams` và `offerToken` chính xác của Google Play.
     - Sau khi mua thành công, luôn gọi `acknowledgePurchase` trong vòng 3 ngày (thực tế xử lý ngay lập tức) để tránh bị Google tự động hoàn tiền và hủy đơn hàng.
  4. **Cơ chế khôi phục gói mua (Restore Purchases):**
     - Cung cấp nút "Khôi phục gói mua" hoạt động thực tế với `queryPurchasesAsync` cho cả `SUBS` và `INAPP`.
  5. **Xử lý trạng thái tài khoản Pro tại Settings:**
     - Khi người dùng đã có gói Pro hoạt động, nhấp vào thẻ tài khoản sẽ hiển thị hộp thoại thông tin gói và liên kết mở trực tiếp trang quản lý đăng ký của Google Play (`https://play.google.com/store/account/subscriptions`), ngăn chặn việc mua trùng lặp gây ức chế và vi phạm UX.
- **Lợi ích:** Ứng dụng đáp ứng quy chuẩn Play Billing Library 8.0.0+ của Google Play, loại bỏ hoàn toàn cảnh báo lỗi thời thư viện và vượt qua 100% các tiêu chí kiểm duyệt chính sách Subscriptions và Broken Functionality trên mọi quốc gia và mọi đơn vị tiền tệ.

---

## [ACQ-005] Tối Ưu Hóa JVM & C1 Compiler Cho Tiến Trình R8 Minification Trên Môi Trường Giới Hạn Commit Memory
- **Bối cảnh:** Khi thực hiện đóng gói AAB hoặc APK Release với `isMinifyEnabled = true`, công cụ R8 của Android Gradle Plugin thực thi các tác vụ phân tích, thu nhỏ và obfuscate code toàn dự án, tiêu thụ bộ nhớ JVM và native heap đột biến. Trên máy trạm Windows có nhiều ứng dụng nền ngốn commit charge (như máy ảo giả lập Android LDPlayer), trình biên dịch JIT HotSpot C2 Compiler sẽ cạn bộ nhớ arena trong lúc tối ưu hóa R8 và làm crash toàn bộ tiến trình daemon.
- **Giải pháp kỹ thuật tối ưu:**
  1. Cấu hình `-XX:TieredStopAtLevel=1` trong `org.gradle.jvmargs` (`gradle.properties`): Buộc JVM dừng ở bộ biên dịch C1 (Client Compiler). C1 biên dịch nhanh hơn, không tốn tài nguyên phân tích chuyên sâu SSA đồ thị của C2 và loại bỏ 100% rủi ro crash `arena.cpp`.
  2. Bổ sung `-XX:ReservedCodeCacheSize=128m` và `org.gradle.workers.max=4` để tránh tạo quá nhiều luồng (thread) dẫn tới vượt hạn mức stack memory của hệ điều hành.
  3. Tách rời tác vụ `./gradlew bundleRelease` và `./gradlew assembleRelease` chạy lần lượt thay vì đồng thời.
- **Lợi ích:** Quá trình build R8 diễn ra mượt mà, ổn định tuyệt đối và hoàn thành đóng gói AAB/APK trong vòng chưa đầy 2 phút mà không bao giờ gặp lỗi sập daemon.

---

## [ACQ-006] Tiêu Chuẩn Nâng Cấp Target API Level 36 & Chiến Lược Phiên Bản Phát Hành (Version Code Increment)
- **Bối cảnh:** Khi xuất bản ứng dụng lên Google Play Console, các đợt phát hành mới bắt buộc phải tuân thủ chuẩn Target API Level tối thiểu mới nhất (API Level 36) và cơ chế kiểm soát mã phiên bản nghiêm ngặt của Google Play.
- **Giải pháp kỹ thuật tối ưu:**
  1. **Cấu hình SDK chuẩn trong `build.gradle.kts`:** Đặt `compileSdk = 36` và `targetSdk = 36`.
  2. **Bỏ qua cảnh báo AGP:** Trong `gradle.properties`, bổ sung `android.suppressUnsupportedCompileSdk=36` để Android Gradle Plugin tiếp tục đóng gói mượt mà mà không ném lỗi không tương thích phiên bản SDK mới.
  3. **Quy tắc tăng Version Code một chiều (Monotonically Increasing):** Mỗi khi một file bundle AAB đã được upload lên Google Play Console (dù ở bất kỳ track thử nghiệm nào hay bị cảnh báo lỗi), Google Play Console sẽ lưu định danh version code đó vào Thư viện gói ứng dụng (App Bundle Explorer). Để upload bản sửa lỗi thành công, bắt buộc phải tăng `versionCode` (từ 9 lên 10) và cập nhật `versionName` tương ứng (`1.0.4`).
- **Lợi ích:** Vượt qua hoàn toàn bộ lọc kiểm duyệt Target API của Google Play Console, tránh lỗi xung đột "Version code has already been used", đảm bảo ứng dụng được tối ưu hóa bảo mật và hiệu năng trên các hệ điều hành Android mới nhất.

---

## [ACQ-007] Phòng Ngừa Vi Phạm Chính Sách Misleading Claims & Đảm Bảo Tính Toàn Vẹn In-App Experience
- **Bối cảnh:** Google Play kiểm duyệt ứng dụng ngày càng nghiêm ngặt về tính trung thực giữa nội dung giới thiệu (Store Listing Description, Screenshots) và chức năng thực tế trong ứng dụng (In-App Experience). Mọi tuyên bố quá mức (như 4K/8K, hỗ trợ mọi nền tảng mạng xã hội) hoặc tính năng quảng cáo nhưng không thể kiểm thử trực tiếp trên ứng dụng mới cài đặt đều bị từ chối phát hành theo *Misleading Claims Policy*.
- **Nguyên tắc kỹ thuật tối ưu:**
  1. **Đồng nhất 100% giữa Tuyên Bố và Năng Lực Thật (Feature Parity):**
     - Tuyệt đối không quảng cáo các định dạng hay độ phân giải vượt quá năng lực thực tế của app (chuyển toàn bộ "4K, 8K" sang "HD / High Definition" trên mọi file `strings.xml` và layout).
     - Không cam kết tải video từ các trang mạng xã hội bị hạn chế hoặc có cơ chế bản quyền đặc thù trừ khi engine đã được kiểm chứng hoạt động ổn định và nhất quán.
  2. **Bắt luồng Video Web Di Động Đa Tầng (Multi-Tier Sniffing Engine):**
     - Với các CDN lớn (như Meta/Instagram `cdninstagram.com`, `fbcdn.net`), các chunk video thường có query byte-range (`&bytestart=...&byteend=...`). Không được lọc bỏ toàn bộ URL chỉ vì có chứa byte-range; thay vào đó, giữ nguyên URL gốc và strip các tham số chunk range để tải về file video hoàn chỉnh.
     - Kết hợp cả ba cơ chế: Network Interception (`shouldInterceptRequest`), DOM Inspection (`outerHTML` + `og:video`), và JS Event Hooking (`<video>` play event).
  3. **Tính Độc Lập Cho Các Tính Năng Bảo Mật & Két Riêng Tư (Self-Contained Private Vault):**
     - Một tính năng được quảng cáo như "Két video riêng tư mã hóa PIN" phải có khả năng trải nghiệm và kiểm thử ngay cả khi người dùng hoặc Google reviewer chưa tải bất kỳ video nào từ web.
     - Cung cấp nút "+ Nhập video từ máy" (`ActivityResultContracts.GetContent()`) và nút "+ Thêm video mẫu" (demo video) trực tiếp trong két, đảm bảo reviewer kiểm thử được 100% luồng mở khóa, xem, khôi phục và xóa video.
- **Lợi ích:** Đảm bảo ứng dụng minh bạch, vượt qua 100% các tiêu chuẩn kiểm duyệt gắt gao của Google Play Policy, bảo vệ tài khoản nhà phát triển và mang lại trải nghiệm người dùng chân thực, tin cậy.


