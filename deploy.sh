#!/usr/bin/env bash
# ==============================================================================
# Script Triển Khai Tự Động Web snaptik2.com trên Server
# Đường dẫn: /home/netviet/projects-deploy/ai-dowloader
# Port: 3070 | Domain: snaptik2.com
# ==============================================================================

set -e

echo ">>> [1/4] Di chuyển vào thư mục dự án..."
cd /home/netviet/projects-deploy/ai-dowloader

echo ">>> [2/4] Cài đặt dependencies (Production)..."
npm install --omit=dev

echo ">>> [3/4] Cấu hình biến môi trường..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Đã tạo .env từ .env.example (PORT=3070, DOMAIN=snaptik2.com)"
fi

echo ">>> [4/4] Khởi động hoặc Khởi động lại ứng dụng với PM2..."
if command -v pm2 &> /dev/null; then
    pm2 restart ecosystem.config.js || pm2 start ecosystem.config.js
    pm2 save
    echo "======================================================="
    echo "✅ Ứng dụng đã chạy thành công trên PM2 (Cluster Mode)!"
    echo "🌐 Port: 3070 | Domain: http://snaptik2.com"
    echo "======================================================="
else
    echo "⚠️ Chưa cài đặt PM2. Đang chạy trực tiếp với node server.js..."
    echo "Khuyên dùng: sudo npm install -g pm2"
    PORT=3070 node server.js
fi
