/**
 * Content Script: AI Video Downloader Stream Sniffer
 * Tự động phát hiện video và nhúng nút Tải Xuống (Download Button) nổi trực tiếp trên video
 */

(function () {
    console.log('[AIDownloader Extension] Content script loaded on:', window.location.href);

    const BACKEND_URL = 'http://localhost:5101';
    const WEB_URL = 'http://localhost:5100';

    // Danh sách video đã được gắn nút download
    const attachedVideos = new WeakSet();

    // Lắng nghe và quét các thẻ <video> trên trang web
    function scanAndAttachVideoButtons() {
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (attachedVideos.has(video)) return;

            // Bỏ qua các video quá nhỏ (quảng cáo pixel, avatar...)
            const rect = video.getBoundingClientRect();
            if (rect.width < 120 || rect.height < 80) return;

            attachDownloadButtonToVideo(video);
        });
    }

    // Gắn widget nút tải xuống nổi trên video
    function attachDownloadButtonToVideo(video) {
        attachedVideos.add(video);

        const container = document.createElement('div');
        container.className = 'ai-downloader-widget-container';

        // Lấy nguồn video khả dụng (src hoặc currentSrc)
        const videoSrc = video.currentSrc || video.src || window.location.href;

        container.innerHTML = `
            <div class="ai-dl-badge" title="Tải video với AI Downloader Pro">
                <div class="ai-dl-icon">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                </div>
                <span class="ai-dl-text">TẢI VIDEO</span>
                <span class="ai-dl-arrow">▾</span>
            </div>
            <div class="ai-dl-menu">
                <div class="ai-dl-menu-header">AI Downloader Pro (Chọn chất lượng)</div>
                <button class="ai-dl-option" data-quality="1080p" data-format="mp4">
                    <span class="ai-dl-tag">Full HD</span> 1080p MP4 (60fps) <span class="ai-dl-size">~120MB</span>
                </button>
                <button class="ai-dl-option" data-quality="720p" data-format="mp4">
                    <span class="ai-dl-tag-sub">HD</span> 720p MP4 (Chuẩn) <span class="ai-dl-size">~55MB</span>
                </button>
                <button class="ai-dl-option" data-quality="480p" data-format="mp4">
                    <span class="ai-dl-tag-sd">SD</span> 480p MP4 (Tiết kiệm) <span class="ai-dl-size">~24MB</span>
                </button>
                <button class="ai-dl-option" data-quality="mp3" data-format="mp3">
                    <span class="ai-dl-tag-audio">MP3</span> Tách Âm Thanh 320k <span class="ai-dl-size">~7MB</span>
                </button>
                <div class="ai-dl-footer-link" id="ai-dl-open-web">Mở Trang Web AI Downloader ↗</div>
            </div>
        `;

        // Định vị container bao quanh hoặc gần video
        const parent = video.parentElement || document.body;
        const computedStyle = window.getComputedStyle(parent);
        if (computedStyle.position === 'static') {
            parent.style.position = 'relative';
        }
        parent.appendChild(container);

        // Sự kiện click nút tải
        const badge = container.querySelector('.ai-dl-badge');
        const menu = container.querySelector('.ai-dl-menu');

        badge.addEventListener('click', (e) => {
            e.stopPropagation();
            menu.classList.toggle('show');
        });

        // Click ngoài đóng menu
        document.addEventListener('click', () => {
            menu.classList.remove('show');
        });

        // Click mở trang web
        container.querySelector('#ai-dl-open-web').addEventListener('click', (e) => {
            e.stopPropagation();
            window.open(`${WEB_URL}?url=${encodeURIComponent(window.location.href)}`, '_blank');
        });

        // Xử lý khi chọn chất lượng tải
        container.querySelectorAll('.ai-dl-option').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                menu.classList.remove('show');
                const quality = btn.getAttribute('data-quality');
                const format = btn.getAttribute('data-format');
                await handleDownloadRequest(videoSrc, quality, format);
            });
        });
    }

    // Xử lý kiểm tra tài khoản và kích hoạt tải video
    async function handleDownloadRequest(mediaUrl, quality, format) {
        showToast('Đang kiểm tra tài khoản & quyền tải video...', 'info');

        // Lấy thông tin user đã lưu trong chrome.storage
        chrome.storage.local.get(['userToken', 'userEmail', 'userName'], async (res) => {
            const token = res.userToken;

            if (!token) {
                // Người dùng chưa đăng nhập: Hiển thị hộp thoại nhắc đăng nhập
                showActionDialog({
                    title: 'Yêu cầu đăng nhập',
                    message: 'Bạn cần đăng nhập tài khoản từ web AI Downloader để sử dụng Extension và đồng bộ số lượt tải.',
                    buttonText: 'Đăng Nhập / Đăng Ký Ngay',
                    buttonAction: () => {
                        window.open(`${WEB_URL}?action=login&ref=extension&url=${encodeURIComponent(window.location.href)}`, '_blank');
                    }
                });
                return;
            }

            try {
                // Gọi Backend kiểm tra giới hạn lượt tải (10 video free / monetized domains)
                const checkRes = await fetch(`${BACKEND_URL}/api/download/check-limit`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        userId: token,
                        url: window.location.href
                    })
                });

                const checkData = await checkRes.json();

                if (!checkData.canDownload) {
                    // Quá giới hạn 10 lượt tải hoặc trang web monetized: Bật Paywall
                    showActionDialog({
                        title: 'Đã đạt giới hạn tải miễn phí!',
                        message: checkData.message || 'Bạn đã sử dụng hết số lượt tải video miễn phí. Nâng cấp gói VIP để tải không giới hạn tốc độ cao!',
                        buttonText: 'Nâng Cấp Gói VIP Pro',
                        isVip: true,
                        buttonAction: () => {
                            window.open(`${WEB_URL}?action=paywall&ref=extension`, '_blank');
                        }
                    });
                    return;
                }

                // Thực hiện tải xuống
                showToast(`Đang khởi tạo luồng tải ${quality.toUpperCase()}...`, 'success');

                const triggerRes = await fetch(`${BACKEND_URL}/api/download/trigger`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        userId: token,
                        url: window.location.href,
                        quality,
                        formatId: format
                    })
                });

                const triggerData = await triggerRes.json();

                if (triggerData.success) {
                    // Tải file trực tiếp về máy
                    const downloadLink = document.createElement('a');
                    downloadLink.href = `${BACKEND_URL}${triggerData.downloadUrl}`;
                    downloadLink.download = triggerData.fileName || `Video_${Date.now()}.${format}`;
                    document.body.appendChild(downloadLink);
                    downloadLink.click();
                    downloadLink.remove();

                    showToast(`Bắt đầu tải xuống thành công (${quality})!`, 'success');
                } else {
                    showToast(triggerData.message || 'Có lỗi xảy ra khi tải video.', 'error');
                }

            } catch (err) {
                console.error('[AIDownloader Extension] Error checking limits:', err);
                showToast('Không thể kết nối đến Backend AI Downloader (Port 5101). Hãy chắc chắn Backend đang chạy!', 'error');
            }
        });
    }

    // Hiển thị thông báo Toast nhanh
    function showToast(message, type = 'info') {
        const existing = document.getElementById('ai-dl-toast');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.id = 'ai-dl-toast';
        toast.className = `ai-dl-toast ai-dl-toast-${type}`;
        toast.innerText = message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('ai-dl-toast-hide');
            setTimeout(() => toast.remove(), 400);
        }, 3200);
    }

    // Hộp thoại popup hành động (Đăng nhập / Nâng cấp gói VIP)
    function showActionDialog({ title, message, buttonText, buttonAction, isVip = false }) {
        const existing = document.getElementById('ai-dl-dialog-overlay');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.id = 'ai-dl-dialog-overlay';
        overlay.className = 'ai-dl-dialog-overlay';

        overlay.innerHTML = `
            <div class="ai-dl-dialog-box ${isVip ? 'ai-dl-vip-theme' : ''}">
                <button class="ai-dl-dialog-close" id="ai-dl-close-btn">&times;</button>
                <div class="ai-dl-dialog-badge">${isVip ? '👑 VIP PRO UPGRADE' : '⚡ AI DOWNLOADER PRO'}</div>
                <h3 class="ai-dl-dialog-title">${title}</h3>
                <p class="ai-dl-dialog-message">${message}</p>
                <div class="ai-dl-dialog-actions">
                    <button class="ai-dl-btn-primary" id="ai-dl-action-btn">${buttonText}</button>
                    <button class="ai-dl-btn-secondary" id="ai-dl-cancel-btn">Để sau</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        overlay.querySelector('#ai-dl-close-btn').onclick = () => overlay.remove();
        overlay.querySelector('#ai-dl-cancel-btn').onclick = () => overlay.remove();
        overlay.querySelector('#ai-dl-action-btn').onclick = () => {
            overlay.remove();
            if (buttonAction) buttonAction();
        };
    }

    // Quét định kỳ và MutationObserver theo dõi video tải động (AJAX / Single Page Apps)
    setInterval(scanAndAttachVideoButtons, 1500);

    const observer = new MutationObserver(() => {
        scanAndAttachVideoButtons();
    });
    observer.observe(document.body, { childList: true, subtree: true });

})();
