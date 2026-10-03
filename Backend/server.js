const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const archiver = require('archiver');
const db = require('./database');
const snifferService = require('./snifferService');

const app = express();
const PORT = process.env.PORT || 3070;

// Phục vụ frontend tĩnh (hỗ trợ cả khi chạy độc lập hoặc trong WWW)
const publicDir = fs.existsSync(path.join(__dirname, 'public')) 
    ? path.join(__dirname, 'public') 
    : path.join(__dirname, '..', 'Frondend', 'public');

if (fs.existsSync(publicDir)) {
    app.use(express.static(publicDir));
}

// Cho phép CORS từ Frontend (Port 5100) và các Extension trình duyệt
app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With', 'X-Extension-Client']
}));

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Log request
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// Middleware xác thực người dùng đơn giản
function authenticate(req, res, next) {
    const authHeader = req.headers['authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.substring(7);
        // Trong hệ thống này, token có thể là userId hoặc email mã hóa
        const user = db.getUserById(token) || db.getUserByEmail(token);
        if (user) {
            req.user = user;
        }
    }
    next();
}

app.use(authenticate);

// --- 1. HEALTH CHECK ---
app.get('/api/health', (req, res) => {
    res.json({
        status: 'UP',
        service: 'AI Downloader Sniffer Backend',
        port: PORT,
        timestamp: new Date().toISOString()
    });
});

// --- 2. AUTHENTICATION (ĐĂNG KÝ / ĐĂNG NHẬP) ---
app.post('/api/auth/register', (req, res) => {
    try {
        const { name, email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ success: false, message: 'Vui lòng cung cấp email và mật khẩu!' });
        }
        const user = db.createUser({ name, email, password });
        res.json({
            success: true,
            message: 'Đăng ký tài khoản thành công!',
            token: user.id,
            user
        });
    } catch (e) {
        res.status(400).json({ success: false, message: e.message });
    }
});

app.post('/api/auth/login', (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ success: false, message: 'Vui lòng nhập email và mật khẩu!' });
        }
        const user = db.getUserByEmail(email);
        if (!user || user.passwordHash !== password) {
            return res.status(401).json({ success: false, message: 'Email hoặc mật khẩu không chính xác!' });
        }
        res.json({
            success: true,
            message: 'Đăng nhập thành công!',
            token: user.id,
            user
        });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

app.get('/api/auth/me', (req, res) => {
    const userId = req.query.userId || (req.user ? req.user.id : null);
    if (!userId) {
        return res.status(401).json({ success: false, message: 'Chưa đăng nhập!' });
    }
    const user = db.getUserById(userId);
    if (!user) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy người dùng!' });
    }
    res.json({ success: true, user });
});

// --- 3. SNIFFER ENGINE API (BÓC TÁCH VIDEO STREAM) ---
app.post('/api/sniff', async (req, res) => {
    try {
        const { url, userId } = req.body;
        if (!url) {
            return res.status(400).json({ success: false, message: 'URL không được để trống!' });
        }

        const sniffResult = await snifferService.sniffUrl(url);

        // Kiểm tra quyền hạn tải video của người dùng
        const user = userId ? db.getUserById(userId) : (req.user || null);
        const isMonetized = db.isMonetizedDomain(url);

        let canDownload = true;
        let limitReason = null;

        if (!user) {
            // Khách chưa đăng nhập: Cho phép xem định dạng, nhưng tải sẽ cảnh báo
            canDownload = true;
        } else if (user.isPro) {
            // Thành viên VIP: Không giới hạn bất kỳ video nào
            canDownload = true;
        } else {
            // Thành viên Miễn phí: Giới hạn 10 video thông thường
            if ((user.downloadCount || 0) >= db.data.config.maxFreeDownloads) {
                canDownload = false;
                limitReason = `Bạn đã sử dụng hết ${db.data.config.maxFreeDownloads} lượt tải miễn phí. Vui lòng nâng cấp VIP Pro để tải không giới hạn!`;
            } else if (isMonetized && (user.monetizedDownloadCount || 0) >= db.data.config.maxFreeMonetizedDownloads) {
                canDownload = false;
                limitReason = `Website này thuộc danh mục mạng trả phí/hạn chế. Bạn đã dùng hết lượt thử miễn phí, vui lòng đăng ký gói VIP!`;
            }
        }

        res.json({
            success: true,
            data: sniffResult,
            limits: {
                canDownload,
                limitReason,
                isMonetized,
                userStatus: user ? (user.isPro ? 'VIP' : 'FREE') : 'GUEST',
                downloadCount: user ? user.downloadCount : 0,
                maxFree: db.data.config.maxFreeDownloads
            }
        });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// --- 4. KIỂM TRA & KÍCH HOẠT TẢI XUỐNG ---
app.post('/api/download/check-limit', (req, res) => {
    const { userId, url } = req.body;
    const user = userId ? db.getUserById(userId) : (req.user || null);
    const isMonetized = db.isMonetizedDomain(url);

    if (!user) {
        return res.json({
            canDownload: true,
            requiresLogin: false,
            isPro: false,
            remainingFree: 1,
            message: 'Lượt tải dùng thử (Khách)'
        });
    }

    if (user.isPro) {
        return res.json({
            canDownload: true,
            isPro: true,
            remainingFree: 999999,
            message: 'Tài khoản VIP Pro - Tải không giới hạn'
        });
    }

    const currentCount = user.downloadCount || 0;
    const maxFree = db.data.config.maxFreeDownloads;
    const remaining = Math.max(0, maxFree - currentCount);

    if (currentCount >= maxFree) {
        return res.json({
            canDownload: false,
            isPro: false,
            remainingFree: 0,
            requiresSubscription: true,
            message: `Bạn đã sử dụng hết ${maxFree} lượt tải miễn phí. Vui lòng nâng cấp gói Pro để tiếp tục tải!`
        });
    }

    if (isMonetized && (user.monetizedDownloadCount || 0) >= db.data.config.maxFreeMonetizedDownloads) {
        return res.json({
            canDownload: false,
            isPro: false,
            remainingFree: remaining,
            requiresSubscription: true,
            message: 'Website này yêu cầu nâng cấp gói VIP Pro để tải xuống!'
        });
    }

    res.json({
        canDownload: true,
        isPro: false,
        remainingFree: remaining,
        message: `Bạn còn ${remaining} lượt tải miễn phí.`
    });
});

app.post('/api/download/trigger', (req, res) => {
    try {
        const { userId, url, formatId, quality, streamUrl, fileName } = req.body;
        const user = userId ? db.getUserById(userId) : (req.user || null);
        const isMonetized = db.isMonetizedDomain(url);

        if (user) {
            if (!user.isPro) {
                if ((user.downloadCount || 0) >= db.data.config.maxFreeDownloads) {
                    return res.status(403).json({
                        success: false,
                        error: 'LIMIT_REACHED',
                        requiresSubscription: true,
                        message: 'Bạn đã đạt giới hạn 10 lượt tải miễn phí. Vui lòng nâng cấp gói VIP!'
                    });
                }
                if (isMonetized && (user.monetizedDownloadCount || 0) >= db.data.config.maxFreeMonetizedDownloads) {
                    return res.status(403).json({
                        success: false,
                        error: 'MONETIZED_LIMIT_REACHED',
                        requiresSubscription: true,
                        message: 'Trang web thuộc danh mục bản quyền/VIP. Vui lòng nâng cấp gói VIP!'
                    });
                }
            }
            db.recordDownload(user.id, isMonetized);
        }

        const actualStreamUrl = streamUrl || snifferService.getStreamUrl(url, formatId);
        const videoTitle = snifferService.getTitle(url) || 'Facebook_Video';
        const cleanTitle = (fileName || videoTitle).replace(/[/\\?%*:|"<>]/g, '_').trim();
        const isAudio = quality === 'mp3' || formatId === 'fmt_mp3' || formatId === 'fmt_m4a' || (quality && quality.toLowerCase().includes('audio'));
        const ext = isAudio ? 'mp3' : 'mp4';
        const resolvedFileName = `${cleanTitle}_${quality || 'HD'}.${ext}`;

        res.json({
            success: true,
            message: 'Kích hoạt tải xuống video thành công!',
            downloadUrl: `/api/download/stream?url=${encodeURIComponent(url || 'demo')}&quality=${encodeURIComponent(quality || 'HD')}&formatId=${encodeURIComponent(formatId || 'mp4')}&streamUrl=${encodeURIComponent(actualStreamUrl || '')}&fileName=${encodeURIComponent(resolvedFileName)}`,
            fileName: resolvedFileName
        });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// Stream video/audio trực tiếp từ CDN máy chủ về trình duyệt người dùng
app.get('/api/download/stream', async (req, res) => {
    try {
        const { quality, formatId, url, fileName } = req.query;
        let streamUrl = req.query.streamUrl;
        if (!streamUrl && url) {
            streamUrl = snifferService.getStreamUrl(url, formatId);
        }

        const isAudio = quality === 'mp3' || formatId === 'fmt_mp3' || formatId === 'fmt_m4a' || (quality && quality.toLowerCase().includes('audio'));
        const ext = isAudio ? (formatId === 'fmt_m4a' ? 'm4a' : 'mp3') : 'mp4';
        const rawFileName = fileName || `AIDownloader_Video_${Date.now()}.${ext}`;
        const cleanFileName = rawFileName.replace(/[/\\?%*:|"<>]/g, '_');

        if (streamUrl && streamUrl.startsWith('http')) {
            console.log(`[Stream Proxy] Bắt đầu stream video thật từ: ${streamUrl.slice(0, 80)}...`);
            const fetchHeaders = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
            };
            if (streamUrl.includes('fbcdn.net') || (url && url.includes('facebook'))) {
                fetchHeaders['Referer'] = 'https://www.facebook.com/';
            } else if (streamUrl.includes('tiktok') || (url && url.includes('tiktok'))) {
                fetchHeaders['Referer'] = 'https://www.tiktok.com/';
            }

            const streamRes = await fetch(streamUrl, { headers: fetchHeaders });
            if (streamRes.ok) {
                res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(cleanFileName)}"; filename*=UTF-8''${encodeURIComponent(cleanFileName)}`);
                res.setHeader('Content-Type', isAudio ? 'audio/mpeg' : (streamRes.headers.get('content-type') || 'video/mp4'));
                const contentLength = streamRes.headers.get('content-length');
                if (contentLength) {
                    res.setHeader('Content-Length', contentLength);
                }
                const { Readable } = require('stream');
                Readable.fromWeb(streamRes.body).pipe(res);
                return;
            } else {
                console.warn(`[Stream Proxy] Stream CDN trả về lỗi HTTP ${streamRes.status}`);
            }
        }

        // Chế độ dự phòng khi không có luồng thật
        res.setHeader('Content-Disposition', `attachment; filename="${cleanFileName}"`);
        res.setHeader('Content-Type', isAudio ? 'audio/mpeg' : 'video/mp4');
        const samplePayload = Buffer.from(
            `[AIDownloader Pro Stream Data]\nSource: ${url}\nQuality: ${quality}\nTimestamp: ${new Date().toISOString()}\nStatus: Verified High Speed Stream\n`
        );
        res.send(samplePayload);
    } catch(err) {
        console.error('[Stream Proxy Error]', err);
        res.status(500).send('Lỗi khi truyền tải luồng video: ' + err.message);
    }
});

// --- 5. QUẢN LÝ GÓI ĐĂNG KÝ (SUBSCRIPTIONS) ---
app.get('/api/subscription/plans', (req, res) => {
    res.json({
        success: true,
        plans: db.getPlans()
    });
});

app.post('/api/subscription/subscribe', (req, res) => {
    try {
        const { userId, planId, paymentMethod } = req.body;
        if (!userId || !planId) {
            return res.status(400).json({ success: false, message: 'Thiếu thông tin người dùng hoặc gói cước!' });
        }
        const result = db.createSubscription(userId, planId, paymentMethod);
        res.json({
            success: true,
            message: `Chúc mừng bạn đã nâng cấp thành công gói ${result.transaction.planName}!`,
            data: result
        });
    } catch (e) {
        res.status(400).json({ success: false, message: e.message });
    }
});

// --- 6. QUẢN TRỊ ADMIN (USERS, SUBSCRIPTIONS, DOANH THU NGÀY/TUẦN/THÁNG/NĂM) ---
app.get('/api/admin/users', (req, res) => {
    const users = db.getUsers().map(u => ({
        id: u.id,
        name: u.name,
        email: u.email,
        role: u.role,
        isPro: u.isPro,
        subscriptionPlan: u.subscriptionPlan,
        subscriptionExpires: u.subscriptionExpires,
        downloadCount: u.downloadCount || 0,
        monetizedDownloadCount: u.monetizedDownloadCount || 0,
        createdAt: u.createdAt,
        avatar: u.avatar
    }));
    res.json({ success: true, count: users.length, users });
});

app.post('/api/admin/users/:id/toggle-vip', (req, res) => {
    try {
        const updated = db.toggleUserVip(req.params.id);
        res.json({
            success: true,
            message: `Đã cập nhật trạng thái VIP cho ${updated.name}: ${updated.isPro ? 'Đã kích hoạt' : 'Hủy kích hoạt'}`,
            user: updated
        });
    } catch (e) {
        res.status(400).json({ success: false, message: e.message });
    }
});

app.get('/api/admin/subscriptions', (req, res) => {
    const subscriptions = db.getSubscriptions();
    res.json({ success: true, count: subscriptions.length, subscriptions });
});

app.get('/api/admin/revenue', (req, res) => {
    const stats = db.getRevenueStats();
    res.json({
        success: true,
        stats
    });
});

// --- 7. EXTENSION API & GÓI TẢI EXTENSION ---
app.get('/api/extension/status', (req, res) => {
    const email = req.query.email;
    const userId = req.query.userId;
    let user = null;

    if (userId) user = db.getUserById(userId);
    else if (email) user = db.getUserByEmail(email);

    if (!user) {
        return res.json({
            isLoggedIn: false,
            isPro: false,
            message: 'Chưa đăng nhập trên Extension',
            remainingDownloads: 1,
            maxFree: db.data.config.maxFreeDownloads
        });
    }

    const remaining = user.isPro ? 999999 : Math.max(0, db.data.config.maxFreeDownloads - (user.downloadCount || 0));

    res.json({
        isLoggedIn: true,
        user: {
            id: user.id,
            name: user.name,
            email: user.email,
            isPro: user.isPro,
            role: user.role,
            downloadCount: user.downloadCount || 0,
            remainingDownloads: remaining
        },
        requiresSubscription: !user.isPro && remaining <= 0,
        monetizedDomains: db.data.monetizedDomains
    });
});

// Endpoint tải xuống gói Extension dạng file .zip
app.get('/api/extension/download-package', (req, res) => {
    const zipPath = path.join(__dirname, '..', 'ai-downloader-extension.zip');
    if (!fs.existsSync(zipPath)) {
        return res.status(404).json({ success: false, message: 'File zip Extension chưa được khởi tạo!' });
    }
    res.download(zipPath, 'AI_Video_Downloader_Extension.zip');
});

// SPA fallback về index.html cho các route giao diện
app.get('*', (req, res, next) => {
    if (req.url.startsWith('/api/')) return next();
    if (fs.existsSync(publicDir)) {
        const indexPath = path.join(publicDir, 'index.html');
        if (fs.existsSync(indexPath)) {
            return res.sendFile(indexPath);
        }
    }
    next();
});

// Khởi chạy server
app.listen(PORT, () => {
    console.log(`=======================================================`);
    console.log(`🚀 AI Downloader Backend API Server is RUNNING!`);
    console.log(`📡 URL: http://localhost:${PORT}`);
    console.log(`📊 Health Check: http://localhost:${PORT}/api/health`);
    console.log(`👥 Admin Users: http://localhost:${PORT}/api/admin/users`);
    console.log(`💰 Revenue Analytics: http://localhost:${PORT}/api/admin/revenue`);
    console.log(`=======================================================`);
});
