# BỘ NHỚ VẬN HÀNH DỰ ÁN (PROJECT_MEMORY.MD)

Tài liệu lưu giữ toàn bộ bối cảnh vận hành, kiến trúc hiện hành và trạng thái hệ thống theo chuẩn **Development Workspace Standard V2.1.2**.

---

## 1. 📌 Bối Cảnh & Thông Tin Dự Án

- **Tên dự án:** AI Video Downloader & Player Platform
- **Mã định danh ứng dụng (Application ID):** `com.ai.video.downloader.videodownloader`
- **Phiên bản hiện tại:** `1.0.7` (Version Code: `13`)
- **Chuẩn quản trị:** Development Workspace Standard V2.1.2 (Design-Gated)
- **Kho lưu trữ:** `git@github.com:netvietsoft/AI-downloader.git`
- **Các nhánh hoạt động:**
  - `main`: Mã nguồn toàn thể (Android Kotlin + Web Platform + Governance)
  - `web`: Nhánh trích xuất độc lập phục vụ server production `snaptik2.com`
  - `fix/subscription-currency-v1.0.7`: Nhánh xử lý Subscriptions Currency Localization, LocaleHelper Country Preservation & Billing Single-Flight Query (VC 13)
  - `fix/subscription-currency-v1.0.6`: Nhánh xử lý Subscriptions Currency Parity & Connection Queue (VC 12)
  - `fix/misleading-claims-v1.0.5`: Nhánh xử lý Misleading Claims & Private Vault
  - `fix/play-billing-crash-v1.0.3`: Nhánh xử lý sự cố Play Billing và Target SDK 36

---

## 2. 📱 Cấu Hình Ứng Dụng Android (Android App Profile)

- **Đường dẫn mã nguồn:** `CONVERT/apps/android`
- **Ngôn ngữ:** Kotlin 1.9.23, Java 17/21
- **Hệ thống build:** Gradle 8.9 (Android Gradle Plugin 8.5.2)
- **SDK Targets:**
  - `minSdk`: `26` (Android 8.0 Oreo)
  - `targetSdk`: `36` (Android 16 - Tuân thủ quy định Google Play Console 2026)
  - `compileSdk`: `36`
- **Cấu hình Compiler & JVM (`gradle.properties`):**
  - `android.suppressUnsupportedCompileSdk=36`
  - `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=128m -XX:+UseParallelGC -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8`
  - `org.gradle.workers.max=4`
  - `kotlin.daemon.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=192m`
- **Kiến trúc cốt lõi:**
  - MVVM Architecture + ViewBinding + Flow/Coroutines
  - Media3 ExoPlayer (Playback Engine)
  - AES-256 Vault Encryption (Bảo vệ video riêng tư tích hợp Demo Video Generator & Local Video Importer)
  - Sniffer Engine (Bóc tách sâu Instagram Reels, Meta CDN, TikTok, Facebook, X/Twitter, Video Web)
  - Google Play Billing Library v8.0.0 (Quản lý Pro Subscriptions & Lifetime Purchase với Connection Queue, dynamic recurring price breakdown, 100% localized)
  - Google Mobile Ads (AdMob SDK 23.0.0)
  - Firebase Analytics & Crashlytics (BoM 33.1.0)
- **Chứng chỉ ký số (Signing Config):**
  - Keystore: `release-keystore.jks` (Được bảo vệ bằng `.gitignore`)
  - Key Alias: `aivideodownloader`

---

## 3. 🌐 Web Platform & Sniffer API (snaptik2.com)

- **Đường dẫn mã nguồn:** `WWW/`
- **Máy chủ triển khai:** Ubuntu VPS tại `/home/netviet/projects-deploy/ai-dowloader`
- **Cổng dịch vụ nội bộ (Internal Port):** `3080` (Tránh xung đột port 3070 đã có trên VPS)
- **Domain công khai:** `snaptik2.com` (Định tuyến qua Nginx Reverse Proxy SSL)
- **Cấu hình PM2:** `ecosystem.config.js` (Cluster mode, port 3080)
- **Kiến trúc Single-Port All-in-One:**
  - Express 5 phục vụ API `/api/sniffer`, `/api/health`, `/api/download`
  - Phục vụ Static HTML/CSS/JS frontend trực tiếp từ thư mục `public/`
  - Sử dụng middleware SPA fallback chuẩn thay cho raw wildcard `app.get('*')` để tránh lỗi Express 5 crash.

---

## 4. 🗄️ Danh Mục File Thành Phẩm Hiện Hành (Release Artifacts)

| File | Phiên bản | Dung lượng | Vị trí lưu trữ |
| :--- | :---: | :---: | :--- |
| **AAB (Play Store Release)** | **VC 13 (v1.0.7)** | **11.07 MB** | [`CONVERT/apps/android/app/build/outputs/bundle/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.aab`](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/build/outputs/bundle/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.aab) |
| **APK (Thiết bị thật Release)** | **VC 13 (v1.0.7)** | **7.31 MB** | [`CONVERT/apps/android/app/build/outputs/apk/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.apk`](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/build/outputs/apk/release/com.ai.video.downloader.videodownloader_v1.0.7_vc13.apk) |
| **AAB (Bản build 12)** | VC 12 (v1.0.6) | 11.05 MB | `CONVERT/apps/android/app/build/outputs/bundle/release/com.ai.video.downloader.videodownloader_v1.0.6_vc12.aab` |
| **APK (Bản build 12)** | VC 12 (v1.0.6) | 7.30 MB | `CONVERT/apps/android/app/build/outputs/apk/release/com.ai.video.downloader.videodownloader_v1.0.6_vc12.apk` |
| **AAB (Bản build 11)** | VC 11 (v1.0.5) | 11.05 MB | `CONVERT/apps/android/app/build/outputs/bundle/release/com.ai.video.downloader.videodownloader_v1.0.5_vc11.aab` |
| **APK (Bản build 11)** | VC 11 (v1.0.5) | 7.29 MB | `CONVERT/apps/android/app/build/outputs/apk/release/app-release.apk` |
| **AAB (Bản build 10)** | VC 10 (v1.0.4) | 10.52 MB | `File/com.ai.video.downloader.videodownloader_v1.0.4_vc10.aab` |
| **APK (Bản build 10)** | VC 10 (v1.0.4) | 6.94 MB | `File/com.ai.video.downloader.videodownloader_v1.0.4_vc10.apk` |
| **AAB (Bản build 9)** | VC 9 (v1.0.3) | 10.52 MB | `File/com.ai.video.downloader.videodownloader_v1.0.3_vc9.aab` |
| **APK (Bản build 9)** | VC 9 (v1.0.3) | 6.94 MB | `File/com.ai.video.downloader.videodownloader_v1.0.3_vc9.apk` |

---

## 5. 📑 Bản Đồ Tài Liệu Quản Trị Hệ Thống

1. **Hiến pháp & Quy tắc:**
   - [AGENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/AGENTS.md): Hiến pháp tối cao dành cho AI Agent (24 Hard Rules).
   - [Docs/rules.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/Docs/rules.md): Phân định quyền hạn từng Agent và quy tắc bảo mật.
2. **Bộ nhớ vận hành (Operational Memory):**
   - [PROJECT_MEMORY.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/PROJECT_MEMORY.md): Bối cảnh và hiện trạng vận hành của dự án (Tài liệu này).
   - [TASK_LOG.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/TASK_LOG.md): Nhật ký hoạt động chi tiết theo thời gian thực (đã ghi nhận đến TASK-013).
   - [UPDATETODOS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/UPDATETODOS.md): Bảng điều phối tiến độ của CEO Orchestrator (13 tasks hoàn tất).
   - [CHANGELOG.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CHANGELOG.md): Lịch sử thay đổi sản phẩm theo chuẩn SemVer (đã ghi nhận đến v1.0.7).
   - [.ai/state.json](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/.ai/state.json): Trạng thái máy đọc (Machine-readable state, status: COMPLETED).
3. **Bộ nhớ bền vững (Durable Memory):**
   - [PROJECT_ERROR.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/PROJECT_ERROR.md): Danh mục 14 lỗi quan trọng đã gặp, nguyên nhân gốc rễ và biện pháp phòng ngừa (`[ERR-001]` đến `[ERR-014]`).
   - [ACQUIREMENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/ACQUIREMENTS.md): Danh mục 9 kiến thức và giải pháp kỹ thuật đã chưng cất kiểm chứng (`[ACQ-001]` đến `[ACQ-009]`).
