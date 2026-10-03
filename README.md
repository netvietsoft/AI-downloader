# AI Video Downloader - Web Platform & Sniffer API (snaptik2.com)

Hệ thống Web tải video trực tuyến đa nền tảng và Backend Sniffer API hiệu năng cao cho website **snaptik2.com**, hỗ trợ tải video từ TikTok (không logo/watermark), Facebook Reels/Watch, YouTube, Instagram, Twitter/X, Douyin, v.v.

---

## 🚀 Thông Số Triển Khai Server

- **Thư mục triển khai:** `/home/netviet/projects-deploy/ai-dowloader`
- **Cổng dịch vụ (Port):** `3070`
- **Tên miền (Domain):** `snaptik2.com` (và `www.snaptik2.com`)
- **Runtime:** Node.js (v18+ hoặc v20+ khuyến nghị)
- **Quản lý tiến trình:** PM2 (Cluster Mode)

---

## 🛠 Hướng Dẫn Clone & Dựng Web Trên Server (1-Click)

### 1. Clone nhánh web về server:
```bash
git clone -b web git@github.com:netvietsoft/AI-downloader.git /home/netviet/projects-deploy/ai-dowloader
cd /home/netviet/projects-deploy/ai-dowloader
```

### 2. Cài đặt dependencies:
```bash
npm install --omit=dev
```

### 3. Tạo file cấu hình môi trường (.env):
```bash
cp .env.example .env
```

### 4. Khởi chạy bằng PM2:
```bash
# Khởi chạy qua ecosystem
pm2 start ecosystem.config.js
pm2 save

# Thiết lập tự khởi động cùng hệ điều hành:
pm2 startup
```

*(Hoặc chạy nhanh bằng script đi kèm: `chmod +x deploy.sh && ./deploy.sh`)*

---

## 🌐 Cấu Hình Nginx Reverse Proxy (snaptik2.com)

1. Sao chép file cấu hình vào Nginx:
```bash
sudo cp nginx-snaptik2.conf /etc/nginx/sites-available/snaptik2.com
sudo ln -s /etc/nginx/sites-available/snaptik2.com /etc/nginx/sites-enabled/
```

2. Kiểm tra và reload Nginx:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

3. Kích hoạt chứng chỉ SSL miễn phí (Let's Encrypt Certbot):
```bash
sudo certbot --nginx -d snaptik2.com -d www.snaptik2.com
```

---

## 📡 Danh Sách API Endpoints

- **GET `/api/health`**: Kiểm tra trạng thái máy chủ
- **POST `/api/sniff`**: Bóc tách link tải video (Body: `{ "url": "https://..." }`)
- **GET `/api/history`**: Lịch sử tải video
- **POST `/api/auth/register`**: Đăng ký tài khoản
- **POST `/api/auth/login`**: Đăng nhập
- **GET `/download-extension`**: Tải file zip tiện ích mở rộng Chrome Extension
