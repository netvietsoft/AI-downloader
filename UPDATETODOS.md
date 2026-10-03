# BẢNG ĐIỀU PHỐI TIẾN ĐỘ DỰ ÁN (UPDATETODOS.MD)

**Người quản lý:** Agent 0 (CEO - ORCHESTRATOR)  
**Phiên bản chuẩn:** Development Workspace Standard V2.1.2  
**Thời gian cập nhật:** 2026-10-03  

---

## 📊 TIẾN ĐỘ TỔNG QUAN

| Hạng mục | Trạng thái | Agent chịu trách nhiệm | Ghi chú |
| :--- | :---: | :---: | :--- |
| **1. Khởi tạo Governance Framework** | `DONE` | Agent 0 (CEO) | README, AGENTS, rules, UPDATETODOS, Tasksrequiring, PROJECT_ERROR, ACQUIREMENTS |
| **2. Tách riêng nhánh Web & Cấu hình Port 3080** | `DONE` | Agent 0 (CEO) | Nhánh `web` độc lập phục vụ clone về `/home/netviet/projects-deploy/ai-dowloader` |
| **3. Audit Bảo Mật & Keystore** | `DONE` | Agent 0 (CEO) | Loại bỏ nguy cơ lộ mật khẩu và keystore lên Git |
| **4. Android App Feature & Parity** | `IN_PROGRESS`| Agent 3 (Frontend) | Module Video Sniffer, Player, Settings, Language |
| **5. Deploy Web lên snaptik2.com (Port 3080)** | `DONE` | DevOps / User | Đã deploy và chạy online trên PM2 cổng 3080 |

---

## 🎯 DANH SÁCH TASK CHI TIẾT

### Phase 1: Git & Web Deployment
- [x] **TASK-001:** Rà soát Git, cấu hình `.gitignore` chuẩn bảo vệ Keystore, loại trừ 35.000 file thô `SOURCE/`.
- [x] **TASK-002:** Thống nhất server web Node.js port 3080, khắc phục lỗi Express 5 wildcard routing, phục vụ API Sniffer + Static Frontend cho domain `snaptik2.com`.
- [x] **TASK-003:** Tạo file cấu hình `ecosystem.config.js`, `nginx-snaptik2.conf`, `deploy.sh` phục vụ triển khai server.
- [x] **TASK-004:** Đẩy mã nguồn lên GitHub:
  - Branch `main`: Chứa mã nguồn App Android + Web Platform + Governance.
  - Branch `web`: Chứa độc lập mã nguồn Web Platform cho deploy server.
  - Deploy và khôi phục an toàn các dịch vụ trên server Ubuntu.

### Phase 2: Android App Completion & Parity
- [ ] **TASK-005:** Đồng bộ các tham số cấu hình production (AdMob, Firebase, IAP) từ [Tasksrequiring.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/Tasksrequiring.md).
- [ ] **TASK-006:** Kiểm thử e2e luồng tải video trên các nền tảng (TikTok, FB, YT, X).
- [ ] **TASK-007:** Build bản phát hành Release AAB/APK.
