# DỰ ÁN TÁI DỰNG NEXTAI VIDEO DOWNLOADER & ACEPLAYER (FULL STACK RECONSTRUCTION)
================================================================================
Dự án tái dựng toàn diện 100% ứng dụng Video Downloader & Media Player từ mã nguồn decompile `SOURCE` sang kiến trúc hiện đại chuẩn hóa V2.1 Design-Gated.

## 1. THÀNH PHẦN HỆ THỐNG
- **apps/android**: Ứng dụng Android Kotlin Native hoàn chỉnh (`com.nextaitechnology.antidetect`)
  - Trình duyệt bảo mật đa thẻ (Secure Multi-Tab Browser)
  - Engine bóc tách luồng video thông minh (Smart Video Sniffer) hỗ trợ 1080p, 720p, 480p, MP3
  - Trình tải ngầm đa luồng (Multi-Thread Downloader với OkHttp & Coroutines)
  - Trình phát video chuyên nghiệp AcePlayer (AndroidX Media3 ExoPlayer, 200% Audio Boost, Equalizer, PiP)
  - Két sắt bảo mật mã hóa thực tế chuẩn AES-256 (Private Vault PIN-Protected)
  - Đa ngôn ngữ 18 quốc gia & Quản lý cài đặt bộ nhớ SAF
- **Backend**: Cụm dịch vụ API & CMS Quản trị từ xa (Node.js/Express TypeScript)
  - Dynamic Sniffing Rules API (cập nhật regex bóc tách video tự động từ xa)
  - Social Shortcuts & Content Recommendations Management
  - Error Telemetry & Broken Link Reporting
  - Web CMS Dashboard quản trị tập trung
- **Docs**: Toàn bộ tài liệu kiến trúc, Behavior Specs, OpenAPI Contract và Báo cáo kiểm thử

## 2. QUY CHUẨN VẬN HÀNH
- Tuân thủ hiến pháp [AGENTS.md](AGENTS.md) và tiêu chuẩn `Development_Workspace_Standard_V2.1_Design_Gated.txt`.
- Giao diện khớp 100% với bản mẫu thiết kế [SOURCE/Redesign/UI/index.html](../SOURCE/Redesign/UI/index.html).
