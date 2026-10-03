# Tasksrequiring.md — CÁC TÀI NGUYÊN & THÔNG TIN CẦN CHỦ TỊCH CUNG CẤP / PHÊ DUYỆT
==================================================================================
> **Quy chuẩn:** Development Workspace Standard V2.1 (Mục 4 - Nhiệm vụ CEO / Agent 0)  
> **Người theo dõi:** CEO - Orchestrator  
> **Cấp phê duyệt:** Chủ tịch Hội đồng Quản trị  
> **Mục đích:** Danh mục các giá trị, khóa API, tài nguyên bên thứ 3 và chính sách kinh doanh đang dùng giá trị mẫu (Demo/Placeholder), cần Chủ tịch hoặc đối tác cung cấp để phát hành Production.

---

## 1. BẢNG DANH MỤC THÔNG TIN CẦN CUNG CẤP

| STT | Hạng mục | Giá trị Demo hiện tại trong mã nguồn | Giá trị Production cần bổ sung | Mức độ ưu tiên | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **01** | **AdMob App ID** | `ca-app-pub-3940256099942544~3347511713` (Google Test App ID) | Cần ID thật từ Google AdMob Console | CAO (Trước khi Release) | ĐANG DÙNG DEMO |
| **02** | **AdMob Banner Unit ID** | `ca-app-pub-3940256099942544/6300978111` (Test Banner) | Cần Ad Unit ID Banner thật | TRUNG BÌNH | ĐANG DÙNG DEMO |
| **03** | **AdMob Interstitial Unit ID** | `ca-app-pub-3940256099942544/1033173712` (Test Interstitial) | Cần Ad Unit ID Interstitial thật | TRUNG BÌNH | ĐANG DÙNG DEMO |
| **04** | **AdMob Rewarded Unit ID** | `ca-app-pub-3940256099942544/5224354917` (Test Rewarded) | Cần Ad Unit ID Rewarded thật | TRUNG BÌNH | ĐANG DÙNG DEMO |
| **05** | **Firebase Config (`google-services.json`)** | Đã cấu hình dependencies Firebase BoM, Analytics, Messaging; chưa có file json | Cần file `google-services.json` từ Firebase Console | CAO (Trước khi Release) | CHỜ FILE THẬT |
| **06** | **Chính sách giá gói PRO (In-app Purchase)** | Tuần: 49.000₫ / Tháng: 149.000₫ / Năm: 699.000₫ | Chủ tịch duyệt mức giá chính thức & Product ID (Google Play Console) | CAO (Kinh doanh) | ĐANG DÙNG DEMO |
| **07** | **URL Chính sách Bảo mật (Privacy Policy)** | Hiển thị Dialog text nội bộ chuẩn GDPR/CCPA | Link Web chính thức (nếu có domain) | TRUNG BÌNH | ĐÃ CÓ NỘI DUNG NỘI BỘ |
| **08** | **URL Điều khoản Sử dụng (Terms of Service)** | Hiển thị Dialog text nội bộ | Link Web chính thức (nếu có domain) | TRUNG BÌNH | ĐÃ CÓ NỘI DUNG NỘI BỘ |
| **09** | **Keystore Ký phát hành (Release Keystore)** | Đang dùng Android Debug Keystore | Cần file `.jks`, alias, password để build Release AAB | CAO (Giai đoạn Release) | CHỜ CẤP KHI RELEASE |

---

## 2. GHI CHÚ ĐIỀU HÀNH TỪ CEO
- Toàn bộ các giá trị trên hiện đã được đội ngũ kỹ thuật bọc an toàn, ứng dụng đã biên dịch, nạp và chạy hoàn hảo ở chế độ Test/Debug trên thiết bị thật Samsung Galaxy A07 (`SM-A075F`).
- Khi Chủ tịch phê duyệt hoặc bàn giao các giá trị Production trên, đội ngũ Kỹ thuật sẽ cập nhật vào hệ thống trong vòng 15 phút mà không làm gián đoạn mã nguồn.
