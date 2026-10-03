/**
 * Popup Script: AI Video Downloader Extension
 */

const BACKEND_URL = 'http://localhost:5101';
const WEB_URL = 'http://localhost:5100';

document.addEventListener('DOMContentLoaded', async () => {
    const userSection = document.getElementById('user-section');
    const tabVideoTitle = document.getElementById('tab-video-title');
    const tabVideoDomain = document.getElementById('tab-video-domain');
    const tabVideoThumb = document.getElementById('tab-video-thumb');
    const btnOpenWeb = document.getElementById('btn-open-web');
    const btnUpgradeVip = document.getElementById('btn-upgrade-vip');
    const btnQuickDl1080 = document.getElementById('btn-quick-dl-1080');
    const btnQuickDlMp3 = document.getElementById('btn-quick-dl-mp3');

    let currentTabUrl = '';
    let currentTabTitle = '';

    // 1. Lấy thông tin tab đang hoạt động
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab) {
            currentTabUrl = tab.url || '';
            currentTabTitle = tab.title || 'Video trên trang web hiện tại';
            tabVideoTitle.innerText = currentTabTitle;

            try {
                const host = new URL(currentTabUrl).hostname;
                tabVideoDomain.innerText = `Nguồn: ${host} (Đã bóc tách stream)`;
            } catch (e) {
                tabVideoDomain.innerText = 'Trang web hỗ trợ trực tiếp';
            }
        }
    } catch (err) {
        console.error('Error querying active tab:', err);
    }

    // 2. Kiểm tra trạng thái đăng nhập từ Chrome Storage & Backend
    chrome.storage.local.get(['userToken', 'userEmail', 'userName'], async (stored) => {
        const token = stored.userToken;

        try {
            const res = await fetch(`${BACKEND_URL}/api/extension/status?userId=${token || ''}&email=${stored.userEmail || ''}`);
            const data = await res.json();

            if (data.isLoggedIn && data.user) {
                renderLoggedInUser(data.user);
            } else {
                renderUnauthenticated();
            }
        } catch (e) {
            console.warn('Backend offline or unreachable, rendering local status');
            if (token) {
                renderLoggedInUser({
                    name: stored.userName || 'Người dùng',
                    email: stored.userEmail || '',
                    isPro: false,
                    remainingDownloads: 5
                });
            } else {
                renderUnauthenticated();
            }
        }
    });

    // Render giao diện người dùng ĐÃ ĐĂNG NHẬP
    function renderLoggedInUser(user) {
        userSection.innerHTML = `
            <div class="user-profile-header">
                <img class="user-avatar" src="https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(user.email || 'user')}" alt="Avatar">
                <div class="user-details">
                    <h3>
                        ${user.name}
                        ${user.isPro ? '<span class="vip-badge">👑 VIP PRO</span>' : '<span class="free-badge">FREE TIER</span>'}
                    </h3>
                    <div class="user-email">${user.email}</div>
                </div>
            </div>
            <div class="user-stats">
                <span>Số lượt tải còn lại:</span>
                <strong>${user.isPro ? 'Không Giới Hạn ⚡' : `${user.remainingDownloads} / 10 lượt`}</strong>
            </div>
            <div style="display: flex; gap: 6px; margin-top: 4px;">
                <button class="btn-auth secondary" id="btn-logout" style="font-size: 11px; padding: 4px 8px;">Đăng Xuất</button>
                ${!user.isPro ? '<button class="btn-auth primary" id="btn-quick-vip" style="font-size: 11px; padding: 4px 8px;">Nâng VIP Ngay</button>' : ''}
            </div>
        `;

        document.getElementById('btn-logout').onclick = () => {
            chrome.storage.local.clear(() => {
                location.reload();
            });
        };

        const btnQuickVip = document.getElementById('btn-quick-vip');
        if (btnQuickVip) {
            btnQuickVip.onclick = () => {
                window.open(`${WEB_URL}?action=paywall`, '_blank');
            };
        }
    }

    // Render giao diện người dùng CHƯA ĐĂNG NHẬP
    function renderUnauthenticated() {
        userSection.innerHTML = `
            <div class="unauth-box">
                <h3>Chưa Đăng Nhập Tài Khoản</h3>
                <p>Đăng nhập bằng tài khoản Web AI Downloader để mở khóa tính năng tải không giới hạn và nhận diện chất lượng 4K.</p>
                <div class="auth-button-group">
                    <button class="btn-auth primary" id="btn-login-redirect">Đăng Nhập</button>
                    <button class="btn-auth secondary" id="btn-register-redirect">Đăng Ký</button>
                </div>
            </div>
        `;

        document.getElementById('btn-login-redirect').onclick = () => {
            window.open(`${WEB_URL}?action=login&ref=extension`, '_blank');
        };
        document.getElementById('btn-register-redirect').onclick = () => {
            window.open(`${WEB_URL}?action=register&ref=extension`, '_blank');
        };
    }

    // Nút mở trang web chính
    btnOpenWeb.onclick = () => {
        const target = currentTabUrl ? `${WEB_URL}?url=${encodeURIComponent(currentTabUrl)}` : WEB_URL;
        window.open(target, '_blank');
    };

    // Nút nâng cấp VIP
    btnUpgradeVip.onclick = () => {
        window.open(`${WEB_URL}?action=paywall`, '_blank');
    };

    // Tải nhanh từ popup
    btnQuickDl1080.onclick = () => {
        if (!currentTabUrl) return;
        window.open(`${WEB_URL}?url=${encodeURIComponent(currentTabUrl)}&autodownload=1080p`, '_blank');
    };

    btnQuickDlMp3.onclick = () => {
        if (!currentTabUrl) return;
        window.open(`${WEB_URL}?url=${encodeURIComponent(currentTabUrl)}&autodownload=mp3`, '_blank');
    };
});
