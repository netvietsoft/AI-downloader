# AI Video Downloader & Player Platform

Hệ thống tải và phát video đa nền tảng chất lượng cao (Hỗ trợ TikTok No-Watermark, Facebook Reels, YouTube, Instagram, X/Twitter, Douyin...), bao gồm:
- **Android App:** Native Android Kotlin Studio project (`CONVERT/apps/android`)
- **Web Platform & Sniffer API:** Node.js Express server (`WWW/`) phục vụ website **snaptik2.com** trên Port 3070
- **Chrome Extension:** Tiện ích mở rộng bóc tách link tải video (`WWW/Extension`)
- **Quản trị dự án:** Tuân thủ tiêu chuẩn [Development_Workspace_Standard_V2.1_Design_Gated 29-9-2026.txt](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/Development_Workspace_Standard_V2.1_Design_Gated%2029-9-2026.txt)

---

## 📁 Cấu Trúc Dự Án

```
PROJECT_ROOT/
├── CONVERT/                      # Toàn bộ mã nguồn Android Kotlin & Assets
│   └── apps/android/             # Android Studio Kotlin Project
├── WWW/                          # Web Platform, API & Extension (snaptik2.com)
│   ├── server.js                 # Unified Production Server (Port 3070)
│   ├── snifferService.js         # Multi-platform video parser
│   ├── database.js               # User, history & subscription db
│   ├── public/                   # Web Frontend UI
│   ├── Extension/                # Chrome Extension
│   ├── ecosystem.config.js       # PM2 Cluster config
│   └── nginx-snaptik2.conf       # Nginx reverse proxy template
├── Docs/
│   └── rules.md                  # Quy tắc phát triển
├── .ai/                          # Trạng thái máy đọc (State, Locks, Project)
├── AGENTS.md                     # Hiến pháp AI Agent
├── UPDATETODOS.md                # Bảng điều phối tiến độ của CEO Orchestrator
├── Tasksrequiring.md             # Danh mục tham số cần User bổ sung
├── PROJECT_ERROR.md              # Bộ nhớ lỗi bền vững (Error Memory)
└── ACQUIREMENTS.md               # Kiến thức & bài học chưng cất (Knowledge Loop)
```

---

## 🚀 Triển Khai Web trên Server (`snaptik2.com`)

Dự án đã tách riêng nhánh `web` để triển khai trực tiếp lên server tại thư mục `/home/netviet/projects-deploy/ai-dowloader`:

```bash
# 1. Clone nhánh web độc lập về server
git clone -b web git@github.com:netvietsoft/AI-downloader.git /home/netviet/projects-deploy/ai-dowloader
cd /home/netviet/projects-deploy/ai-dowloader

# 2. Cài đặt dependencies
npm install --omit=dev

# 3. Khởi chạy với PM2
pm2 start ecosystem.config.js
pm2 save
```

---

## 📱 Build Android App

Mở thư mục `CONVERT/apps/android` bằng **Android Studio**, đồng bộ Gradle và build APK:
```bash
cd CONVERT/apps/android
./gradlew assembleDebug
```
