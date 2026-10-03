# TÀI LIỆU KIẾN TRÚC HỆ THỐNG TOÀN DIỆN (SYSTEM ARCHITECTURE SPECIFICATION)
===========================================================================
> **Dự án:** NextAI Video Downloader & AcePlayer  
> **Package ID:** `com.nextaitechnology.antidetect`  
> **Quy chuẩn:** Development Workspace Standard V2.1 Design-Gated  

---

## 1. TỔNG QUAN KIẾN TRÚC (HIGH-LEVEL ARCHITECTURE)
Hệ thống được thiết kế phân tầng theo nguyên lý **Clean Architecture**, tách biệt hoàn toàn giữa:
1. **Presentation Layer (Tầng hiển thị)**:
   - Các Fragment/Activity (`MainActivity`, `AcePlayerActivity`, `LanguageActivity`, `SettingsActivity`).
   - Tương thích 100% với bản mẫu thiết kế tương tác [SOURCE/Redesign/UI/index.html](../../SOURCE/Redesign/UI/index.html).
2. **Domain Layer (Tầng nghiệp vụ cốt lõi)**:
   - `SnifferEngine`: Bóc tách và phân loại luồng media (mp4, m3u8, mp3) qua Regex và JavaScript Injection.
   - `DownloadEngine`: Quản lý tác vụ tải ngầm đa luồng (Coroutines + Flow), tính toán tốc độ KB/s và thanh tiến trình.
   - `AesVaultManager`: Đảm bảo an toàn tuyệt đối với chuẩn mã hóa phần cứng AES-256-GCM.
3. **Data Layer (Tầng dữ liệu)**:
   - `SettingsRepository`: Lưu trữ cấu hình người dùng qua SharedPreferences.
   - `VaultController`: Quản lý kho tệp tin được mã hóa giấu kín `.nomedia`.
4. **Backend & Remote Config Services**:
   - Máy chủ API cung cấp Dynamic Rules cho Sniffer Engine, danh mục Shortcuts mạng xã hội, và Web CMS Dashboard.

---

## 2. BẢNG PHÂN BỔ THÀNH PHẦN MÃ NGUỒN
| Tên Module / File | Vai trò kỹ thuật | Trạng thái kiểm thử |
| :--- | :--- | :--- |
| `MainActivity.kt` | Điều phối luồng giao diện 3 Tabs chính | Passed Verification |
| `SnifferEngine.kt` | Bóc tách luồng TikTok, Facebook, IG, X, HLS | Passed Verification |
| `DownloadEngine.kt` | Tải xuống đa luồng, hỗ trợ Pause/Resume | Passed Verification |
| `AcePlayerController.kt` | Media3 ExoPlayer, 200% Audio Boost, Equalizer | Passed Verification |
| `AesVaultManager.kt` | Mã hóa Két sắt AES-256-GCM với PBKDF2 salt | Passed Verification |
| `TabManager.kt` | Quản lý đa thẻ duyệt web | Passed Verification |
| `Backend/RuleEngine.ts` | Máy chủ API cấu hình quy tắc động từ xa | Passed Verification |
| `Backend/adminView.ts` | Giao diện điều hành Web CMS | Passed Verification |
