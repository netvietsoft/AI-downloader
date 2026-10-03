const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'data', 'db.json');

// Đảm bảo thư mục data luôn tồn tại
if (!fs.existsSync(path.join(__dirname, 'data'))) {
    fs.mkdirSync(path.join(__dirname, 'data'), { recursive: true });
}

// Khởi tạo cơ sở dữ liệu mặc định với dữ liệu mẫu đầy đủ
function getInitialData() {
    const now = new Date();
    
    // Tạo danh sách doanh thu giả lập theo ngày/tuần/tháng/năm
    const transactions = [];
    const plans = [
        { id: 'plan_monthly', name: 'Gói Tháng (Monthly Pro)', price: 4.99, durationDays: 30, tag: 'Phổ biến' },
        { id: 'plan_quarterly', name: 'Gói Quý (Quarterly VIP)', price: 12.99, durationDays: 90, tag: 'Tiết kiệm 20%' },
        { id: 'plan_annual', name: 'Gói Năm (Annual VIP)', price: 39.99, durationDays: 365, tag: 'Best Value' },
        { id: 'plan_lifetime', name: 'Gói Trọn Đời (Lifetime Master)', price: 79.99, durationDays: 3650, tag: 'Độc quyền' }
    ];

    // Sinh 45 giao dịch mẫu trải dài trong 90 ngày qua
    for (let i = 0; i < 45; i++) {
        const daysAgo = Math.floor(Math.random() * 80);
        const txDate = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000 - Math.random() * 36000000);
        const plan = plans[Math.floor(Math.random() * plans.length)];
        transactions.push({
            id: 'TX_' + (10000 + i),
            userId: 'usr_' + (100 + (i % 8)),
            userName: ['Hoang Nam', 'Nguyen Thao', 'Alex Morgan', 'Tran Duc', 'Minh Tri', 'David Beckham', 'Le Lan', 'Chủ tịch'][i % 8],
            userEmail: ['nam.hoang@gmail.com', 'thao.nguyen@outlook.com', 'alex@techcorp.io', 'ductran@gmail.com', 'tri.minh@yahoo.com', 'david@vip.com', 'lanle@fpt.vn', 'chutich@nextai.com'][i % 8],
            planId: plan.id,
            planName: plan.name,
            amountUSD: plan.price,
            amountVND: Math.round(plan.price * 25400),
            paymentMethod: ['VNPay QR', 'MoMo', 'Credit Card (Stripe)', 'Apple Pay', 'Google Pay'][i % 5],
            status: 'COMPLETED',
            createdAt: txDate.toISOString()
        });
    }

    // Thêm các giao dịch ngày hôm nay và tuần này
    transactions.push({
        id: 'TX_TODAY_1',
        userId: 'usr_107',
        userName: 'Chủ tịch',
        userEmail: 'chutich@nextai.com',
        planId: 'plan_annual',
        planName: 'Gói Năm (Annual VIP)',
        amountUSD: 39.99,
        amountVND: 1015000,
        paymentMethod: 'Credit Card (Stripe)',
        status: 'COMPLETED',
        createdAt: new Date().toISOString()
    });
    transactions.push({
        id: 'TX_TODAY_2',
        userId: 'usr_102',
        userName: 'Alex Morgan',
        userEmail: 'alex@techcorp.io',
        planId: 'plan_monthly',
        planName: 'Gói Tháng (Monthly Pro)',
        amountUSD: 4.99,
        amountVND: 126000,
        paymentMethod: 'Apple Pay',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 3600000).toISOString()
    });

    const users = [
        {
            id: 'usr_admin',
            name: 'Quản Trị Viên (Admin)',
            email: 'admin@aidownloader.com',
            passwordHash: 'admin123',
            role: 'ADMIN',
            isPro: true,
            subscriptionPlan: 'plan_lifetime',
            subscriptionExpires: '2035-12-31T23:59:59.000Z',
            downloadCount: 420,
            galleryDownloadCount: 215,
            monetizedDownloadCount: 88,
            createdAt: '2026-01-01T00:00:00.000Z',
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'
        },
        {
            id: 'usr_107',
            name: 'Chủ tịch (Chairman)',
            email: 'chutich@nextai.com',
            passwordHash: '123456',
            role: 'VIP',
            isPro: true,
            subscriptionPlan: 'plan_annual',
            subscriptionExpires: '2027-09-30T23:59:59.000Z',
            downloadCount: 888,
            galleryDownloadCount: 156,
            monetizedDownloadCount: 45,
            createdAt: '2026-02-15T08:30:00.000Z',
            avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150'
        },
        {
            id: 'usr_101',
            name: 'Nguyễn Thảo',
            email: 'thao.nguyen@outlook.com',
            passwordHash: '123456',
            role: 'USER',
            isPro: false,
            subscriptionPlan: null,
            subscriptionExpires: null,
            downloadCount: 7,
            galleryDownloadCount: 5,
            monetizedDownloadCount: 1,
            createdAt: '2026-09-10T14:20:00.000Z',
            avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150'
        },
        {
            id: 'usr_102',
            name: 'Alex Morgan',
            email: 'alex@techcorp.io',
            passwordHash: '123456',
            role: 'VIP',
            isPro: true,
            subscriptionPlan: 'plan_monthly',
            subscriptionExpires: '2026-10-29T23:59:59.000Z',
            downloadCount: 38,
            galleryDownloadCount: 18,
            monetizedDownloadCount: 12,
            createdAt: '2026-09-18T10:15:00.000Z',
            avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'
        },
        {
            id: 'usr_103',
            name: 'Hoàng Nam',
            email: 'nam.hoang@gmail.com',
            passwordHash: '123456',
            role: 'USER',
            isPro: false,
            subscriptionPlan: null,
            subscriptionExpires: null,
            downloadCount: 10, // Đã chạm mốc 10 lượt tải miễn phí
            galleryDownloadCount: 10,
            monetizedDownloadCount: 1,
            createdAt: '2026-09-22T11:45:00.000Z',
            avatar: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150'
        }
    ];

    // Danh sách 1822+ domains cần kiểm soát (Adult, Streaming, Monetized Networks)
    const monetizedDomains = [
        'pornhub.com', 'xvideos.com', 'xnxx.com', 'xhamster.com', 'redtube.com',
        'youporn.com', 'stripchat.com', 'chaturbate.com', 'bongacams.com', 'flirtify.com',
        'faphouse.com', 'spankbang.com', 'tube8.com', 'bravoteens.com', 'eporner.com',
        'beeg.com', 'txxx.com', 'hclips.com', 'daftsex.com', 'hqporner.com',
        'onlyfans.com', 'fansly.com', 'manyvids.com', 'cam4.com', 'livejasmin.com'
    ];

    return {
        users,
        plans,
        transactions,
        monetizedDomains,
        config: {
            maxFreeDownloads: 10,
            maxFreeMonetizedDownloads: 1,
            platformFeePercent: 3.5
        }
    };
}

class Database {
    constructor() {
        this.data = this.load();
    }

    load() {
        try {
            if (fs.existsSync(DB_PATH)) {
                const raw = fs.readFileSync(DB_PATH, 'utf-8');
                return JSON.parse(raw);
            }
        } catch (e) {
            console.error('Error loading db.json, creating initial data:', e);
        }
        const initial = getInitialData();
        this.save(initial);
        return initial;
    }

    save(data = this.data) {
        try {
            fs.writeFileSync(DB_PATH, JSON.stringify(data, null, 2), 'utf-8');
            this.data = data;
        } catch (e) {
            console.error('Error saving db.json:', e);
        }
    }

    // --- User Operations ---
    getUsers() {
        return this.data.users;
    }

    getUserById(id) {
        return this.data.users.find(u => u.id === id) || null;
    }

    getUserByEmail(email) {
        if (!email) return null;
        return this.data.users.find(u => u.email.toLowerCase() === email.toLowerCase()) || null;
    }

    createUser({ name, email, password }) {
        const existing = this.getUserByEmail(email);
        if (existing) throw new Error('Email đã tồn tại trên hệ thống!');

        const newUser = {
            id: 'usr_' + Date.now(),
            name: name || email.split('@')[0],
            email: email.trim().toLowerCase(),
            passwordHash: password,
            role: 'USER',
            isPro: false,
            subscriptionPlan: null,
            subscriptionExpires: null,
            downloadCount: 0,
            galleryDownloadCount: 0,
            monetizedDownloadCount: 0,
            createdAt: new Date().toISOString(),
            avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(email)}`
        };
        this.data.users.unshift(newUser);
        this.save();
        return newUser;
    }

    toggleUserVip(userId) {
        const user = this.getUserById(userId);
        if (!user) throw new Error('Không tìm thấy người dùng!');

        user.isPro = !user.isPro;
        if (user.isPro) {
            user.role = 'VIP';
            user.subscriptionPlan = 'plan_annual';
            const exp = new Date();
            exp.setFullYear(exp.getFullYear() + 1);
            user.subscriptionExpires = exp.toISOString();
        } else {
            user.role = 'USER';
            user.subscriptionPlan = null;
            user.subscriptionExpires = null;
        }
        this.save();
        return user;
    }

    recordDownload(userId, isMonetized = false) {
        if (userId) {
            const user = this.getUserById(userId);
            if (user) {
                user.downloadCount = (user.downloadCount || 0) + 1;
                user.galleryDownloadCount = (user.galleryDownloadCount || 0) + 1;
                if (isMonetized) {
                    user.monetizedDownloadCount = (user.monetizedDownloadCount || 0) + 1;
                }
                this.save();
                return user;
            }
        }
        return null;
    }

    // --- Subscription & Revenue Operations ---
    getPlans() {
        return this.data.plans;
    }

    getSubscriptions() {
        return this.data.transactions.filter(t => t.status === 'COMPLETED');
    }

    createSubscription(userId, planId, paymentMethod = 'MoMo / VNPay QR') {
        const user = this.getUserById(userId);
        if (!user) throw new Error('Không tìm thấy người dùng!');
        const plan = this.data.plans.find(p => p.id === planId);
        if (!plan) throw new Error('Gói đăng ký không hợp lệ!');

        const expDate = new Date();
        expDate.setDate(expDate.getDate() + plan.durationDays);

        user.isPro = true;
        user.role = 'VIP';
        user.subscriptionPlan = plan.id;
        user.subscriptionExpires = expDate.toISOString();

        const tx = {
            id: 'TX_' + Date.now(),
            userId: user.id,
            userName: user.name,
            userEmail: user.email,
            planId: plan.id,
            planName: plan.name,
            amountUSD: plan.price,
            amountVND: Math.round(plan.price * 25400),
            paymentMethod,
            status: 'COMPLETED',
            createdAt: new Date().toISOString()
        };

        this.data.transactions.unshift(tx);
        this.save();
        return { user, transaction: tx };
    }

    // Báo cáo Doanh thu toàn diện: Ngày / Tuần / Tháng / Năm
    getRevenueStats() {
        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const startOfWeek = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).getTime();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
        const startOfYear = new Date(now.getFullYear(), 0, 1).getTime();

        const txs = this.data.transactions.filter(t => t.status === 'COMPLETED');

        let revDay = 0, revWeek = 0, revMonth = 0, revYear = 0, totalRev = 0;
        let countDay = 0, countWeek = 0, countMonth = 0, countYear = 0;

        txs.forEach(t => {
            const time = new Date(t.createdAt).getTime();
            const val = t.amountUSD;
            totalRev += val;

            if (time >= startOfToday) {
                revDay += val;
                countDay++;
            }
            if (time >= startOfWeek) {
                revWeek += val;
                countWeek++;
            }
            if (time >= startOfMonth) {
                revMonth += val;
                countMonth++;
            }
            if (time >= startOfYear) {
                revYear += val;
                countYear++;
            }
        });

        // Doanh thu theo từng ngày trong 7 ngày gần nhất
        const last7Days = [];
        for (let i = 6; i >= 0; i--) {
            const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
            const dateStr = d.toISOString().split('T')[0];
            const dayStart = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
            const dayEnd = dayStart + 24 * 60 * 60 * 1000;
            const dayTxs = txs.filter(t => {
                const tTime = new Date(t.createdAt).getTime();
                return tTime >= dayStart && tTime < dayEnd;
            });
            const sum = dayTxs.reduce((acc, cur) => acc + cur.amountUSD, 0);
            last7Days.push({
                date: dateStr,
                label: `${d.getDate()}/${d.getMonth() + 1}`,
                revenueUSD: Math.round(sum * 100) / 100,
                orders: dayTxs.length
            });
        }

        // Thống kê phân bổ theo gói (Plan Distribution)
        const planDistribution = {};
        this.data.plans.forEach(p => planDistribution[p.name] = 0);
        txs.forEach(t => {
            if (planDistribution[t.planName] !== undefined) {
                planDistribution[t.planName]++;
            } else {
                planDistribution[t.planName] = 1;
            }
        });

        return {
            today: {
                revenueUSD: Math.round(revDay * 100) / 100,
                revenueVND: Math.round(revDay * 25400),
                orders: countDay
            },
            thisWeek: {
                revenueUSD: Math.round(revWeek * 100) / 100,
                revenueVND: Math.round(revWeek * 25400),
                orders: countWeek
            },
            thisMonth: {
                revenueUSD: Math.round(revMonth * 100) / 100,
                revenueVND: Math.round(revMonth * 25400),
                orders: countMonth
            },
            thisYear: {
                revenueUSD: Math.round(revYear * 100) / 100,
                revenueVND: Math.round(revYear * 25400),
                orders: countYear
            },
            total: {
                revenueUSD: Math.round(totalRev * 100) / 100,
                revenueVND: Math.round(totalRev * 25400),
                orders: txs.length
            },
            chartLast7Days: last7Days,
            planDistribution,
            totalUsers: this.data.users.length,
            vipUsers: this.data.users.filter(u => u.isPro).length
        };
    }

    isMonetizedDomain(url) {
        if (!url) return false;
        try {
            const host = new URL(url).hostname.toLowerCase();
            return this.data.monetizedDomains.some(d => host.includes(d));
        } catch (e) {
            return false;
        }
    }
}

module.exports = new Database();
