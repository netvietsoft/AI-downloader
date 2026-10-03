/**
 * AI Downloader Pro - Main Application Logic
 * Prototype Controller & Live Backend Integration (Port 5101)
 */

// Tự động nhận diện host/port hiện tại (phù hợp domain snaptik2.com, port 3070 và localhost)
const BACKEND_URL = '';

// App State
let currentUser = null;
let currentSniffData = null;
let selectedPlan = 'plan_annual';

document.addEventListener('DOMContentLoaded', () => {
    initThemeToggle();
    initLanguageSelector();
    initPrototypeNavigation();
    initPlatformSwitching();
    initMidPageInteractive();
    initFooterLinks();
    initAuth();
    initDownloader();
    initAdminPortal();
    initPaywall();
    handleUrlQueryParams();
    initFirebaseMonitor();
});

// ==========================================================================
// 0. THEME SWITCHER (GIAO DIỆN SÁNG / TỐI - THEO YÊU CẦU CHỦ TỊCH)
// ==========================================================================
function initThemeToggle() {
    const btnTheme = document.getElementById('btn-theme-toggle');
    const themeIcon = document.getElementById('theme-icon');
    const themeLabel = document.getElementById('theme-label');

    const savedTheme = localStorage.getItem('ai_downloader_theme') || 'dark';
    if (savedTheme === 'light') {
        document.body.classList.add('light-theme');
        document.body.classList.remove('dark-theme');
        if (themeIcon) themeIcon.textContent = '🌙';
        if (themeLabel) themeLabel.textContent = 'Tối';
    } else {
        document.body.classList.remove('light-theme');
        document.body.classList.add('dark-theme');
        if (themeIcon) themeIcon.textContent = '☀️';
        if (themeLabel) themeLabel.textContent = 'Sáng';
    }

    if (btnTheme) {
        btnTheme.addEventListener('click', () => {
            document.body.classList.toggle('light-theme');
            const isLight = document.body.classList.contains('light-theme');
            if (isLight) {
                document.body.classList.remove('dark-theme');
            } else {
                document.body.classList.add('dark-theme');
            }
            localStorage.setItem('ai_downloader_theme', isLight ? 'light' : 'dark');
            if (themeIcon) themeIcon.textContent = isLight ? '🌙' : '☀️';
            if (themeLabel) themeLabel.textContent = isLight ? 'Tối' : 'Sáng';
            showToast(`Đã chuyển sang giao diện ${isLight ? 'Sáng' : 'Tối'}`);
        });
    }
}

// ==========================================================================
// 0.1 ĐA NGÔN NGỮ (I18N LANGUAGE SELECTOR)
// ==========================================================================
const I18N = {
    vi: {
        themeLight: 'Sáng',
        themeDark: 'Tối',
        heroBadge: 'Bóc tách luồng video trực tuyến thông minh bằng AI Sniffer',
        heroTitleAll: 'Công cụ Tải video trực tuyến Đa Năng',
        heroSubtitleAll: 'Hỗ trợ tải video tốc độ cao từ YouTube, Facebook, TikTok, Instagram, Twitter và hơn 1.822+ trang web với chuẩn 1080p, 4K & MP3',
        placeholderAll: 'Dán liên kết video của bạn vào đây (YouTube, TikTok, Facebook...)',
        heroTitleFB: 'Tải Video Facebook Miễn Phí (Full HD 1080p, 2K, 4K, Reels, Story)',
        heroSubtitleFB: 'Tải video Facebook công khai, Reels, Watch, Story chất lượng cao kèm âm thanh gốc',
        placeholderFB: 'Dán liên kết video Facebook (Reels, Watch, Post) vào đây...',
        heroTitleTT: 'Tải Video TikTok Không Logo (Watermark) Miễn Phí',
        heroSubtitleTT: 'Tải video TikTok HD không logo, không watermark, tách nhạc MP3 TikTok cực nhanh',
        placeholderTT: 'Dán link video TikTok (vd: https://www.tiktok.com/@user/video/...) vào đây...',
        heroTitleYT: 'Tải Video YouTube 4K & Chuyển Đổi MP3 320kbps Nhanh Chóng',
        heroSubtitleYT: 'Bóc tách video YouTube độ nét cao 1080p, 2K, 4K 60fps và tách nhạc MP3 chất lượng cao',
        placeholderYT: 'Dán link YouTube (vd: https://www.youtube.com/watch?v=...) vào đây...',
        heroTitleIG: 'Tải Video Instagram Reels, Story & Ảnh Đẹp Chất Lượng Cao',
        heroSubtitleIG: 'Lưu video Instagram Reels, Stories, IGTV và Carousel chất lượng gốc về máy',
        placeholderIG: 'Dán link Instagram Reels / Post vào đây...',
        btnPaste: '📋 Dán Link',
        btnDownload: 'TẢI VỀ ⚡',
        sampleLabel: 'Thử nhanh link mẫu:',
        deviceTitle: 'Hướng Dẫn Tải Video Trên Mọi Thiết Bị',
        deviceSubtitle: 'Dễ dàng lưu video về điện thoại hoặc máy tính chỉ trong 3 bước đơn giản',
        featTitle: 'Ưu Điểm Vượt Trội Của AI Downloader Pro',
        featSubtitle: 'Công nghệ Sniffer thế hệ mới giúp bóc tách luồng video nhanh gấp 10 lần các website thông thường',
        faqTitle: 'Câu Hỏi Thường Gặp (FAQ)',
        faqSubtitle: 'Giải đáp tất cả thắc mắc phổ biến của người dùng về công cụ tải video AI Downloader'
    },
    en: {
        themeLight: 'Light',
        themeDark: 'Dark',
        heroBadge: 'Smart online video stream extraction via AI Sniffer Engine',
        heroTitleAll: 'Universal Online Video Downloader',
        heroSubtitleAll: 'Download high-speed videos from YouTube, Facebook, TikTok, Instagram, Twitter and 1,822+ sites in 1080p, 4K & MP3',
        placeholderAll: 'Paste your video link here (YouTube, TikTok, Facebook...)',
        heroTitleFB: 'Free Facebook Video Downloader (1080p, 2K, 4K, Reels, Story)',
        heroSubtitleFB: 'Download public Facebook videos, Reels, Watch and Stories in high definition with audio',
        placeholderFB: 'Paste Facebook video link (Reels, Watch, Post) here...',
        heroTitleTT: 'TikTok Downloader Without Watermark Free Online',
        heroSubtitleTT: 'Download TikTok HD videos with no logo, no watermark, extract TikTok MP3 fast',
        placeholderTT: 'Paste TikTok video link (e.g. https://www.tiktok.com/@user/video/...) here...',
        heroTitleYT: 'YouTube 4K Video Downloader & MP3 320kbps Converter',
        heroSubtitleYT: 'Extract crystal-clear YouTube 1080p, 2K, 4K 60fps and high-bitrate MP3 audio',
        placeholderYT: 'Paste YouTube link (e.g. https://www.youtube.com/watch?v=...) here...',
        heroTitleIG: 'Instagram Reels & Story Video Downloader HD',
        heroSubtitleIG: 'Save Instagram Reels, Stories, IGTV and HD photos in original quality',
        placeholderIG: 'Paste Instagram Reels / Post link here...',
        btnPaste: '📋 Paste Link',
        btnDownload: 'DOWNLOAD ⚡',
        sampleLabel: 'Quick test samples:',
        deviceTitle: 'How to Download on Any Device',
        deviceSubtitle: 'Easily save videos to mobile or desktop in 3 simple steps',
        featTitle: 'Why Choose AI Downloader Pro',
        featSubtitle: 'Next-gen multi-threaded sniffer technology retrieves video streams 10x faster',
        faqTitle: 'Frequently Asked Questions (FAQ)',
        faqSubtitle: 'Everything you need to know about our AI video download engine'
    },
    ja: {
        themeLight: 'ライト',
        themeDark: 'ダーク',
        heroBadge: 'AIスニファーによる次世代動画ストリーム抽出エンジン',
        heroTitleAll: 'オンライン動画ダウンローダー',
        heroSubtitleAll: 'YouTube、Facebook、TikTok、Instagramなど1,822以上のサイトから1080p・4K・MP3で高速ダウンロード',
        placeholderAll: '動画リンクをここに貼り付け (YouTube, TikTok, Facebook...)',
        heroTitleFB: 'Facebook動画ダウンロード (1080p・4K・Reels対応)',
        heroSubtitleFB: 'Facebookリール、ウォッチ動画を高画質・音声付きで無料ダウンロード',
        placeholderFB: 'Facebook動画リンクを貼り付け...',
        heroTitleTT: 'TikTok透かしなし動画ダウンロード',
        heroSubtitleTT: 'ロゴなし高画質TikTok動画およびMP3音楽を瞬時に保存',
        placeholderTT: 'TikTok動画リンクを貼り付け...',
        heroTitleYT: 'YouTube 4K動画保存 & MP3 320kbps 変換',
        heroSubtitleYT: 'YouTube高画質1080p/4Kおよび高音質MP3を無料抽出',
        placeholderYT: 'YouTubeリンクを貼り付け...',
        heroTitleIG: 'Instagramリール・ストーリー動画ダウンロード',
        heroSubtitleIG: 'インスタのリール、ストーリー、写真をオリジナル画質で保存',
        placeholderIG: 'Instagramリンクを貼り付け...',
        btnPaste: '📋 貼り付け',
        btnDownload: 'ダウンロード ⚡',
        sampleLabel: 'サンプルリンク:',
        deviceTitle: '全端末対応ダウンロード手順',
        deviceSubtitle: 'スマートフォンでもPCでもわずか3ステップで保存',
        featTitle: 'AI Downloader Proの強み',
        featSubtitle: '従来の10倍速いマルチスレッド抽出エンジン搭載',
        faqTitle: 'よくある質問 (FAQ)',
        faqSubtitle: 'ダウンロードに関する疑問を迅速に解決'
    },
    ko: {
        themeLight: '라이트',
        themeDark: '다크',
        heroBadge: '스마트 AI 스니퍼 기반 온라인 비디오 추출기',
        heroTitleAll: '최고 속도 온라인 비디오 다운로더',
        heroSubtitleAll: '유튜브, 페이스북, 틱톡, 인스타그램 등 1,822개 이상 사이트 지원 (1080p, 4K, MP3)',
        placeholderAll: '여기에 비디오 링크를 붙여넣으세요 (유튜브, 틱톡, 페이스북...)',
        heroTitleFB: '페이스북 비디오 다운로더 (1080p, 4K, 릴스 무료)',
        heroSubtitleFB: '페이스북 릴스, 워치 비디오를 원본 오디오와 함께 고화질로 저장',
        placeholderFB: '페이스북 비디오 링크를 입력하세요...',
        heroTitleTT: '틱톡 워터마크 없는 비디오 다운로드',
        heroSubtitleTT: '로고 없는 틱톡 HD 영상 및 MP3 음원 즉시 추출',
        placeholderTT: '틱톡 링크를 입력하세요...',
        heroTitleYT: '유튜브 4K 비디오 다운로드 및 MP3 320kbps 변환',
        heroSubtitleYT: '초고화질 1080p, 4K 영상 및 고음질 MP3 무료 추출',
        placeholderYT: '유튜브 링크를 입력하세요...',
        heroTitleIG: '인스타그램 릴스 & 스토리 다운로더 HD',
        heroSubtitleIG: '인스타그램 릴스, 스토리, 고화질 사진을 원본으로 저장',
        placeholderIG: '인스타그램 링크를 입력하세요...',
        btnPaste: '📋 붙여넣기',
        btnDownload: '다운로드 ⚡',
        sampleLabel: '샘플 링크 바로가기:',
        deviceTitle: '모든 기기별 다운로드 안내',
        deviceSubtitle: '단 3단계로 스마트폰과 PC에 바로 저장하세요',
        featTitle: 'AI Downloader Pro 주요 장점',
        featSubtitle: '멀티스레드 AI 스니퍼 기술로 10배 빠른 다운로드 속도 제공',
        faqTitle: '자주 묻는 질문 (FAQ)',
        faqSubtitle: '서비스 이용에 관한 모든 궁금증 해결'
    },
    zh: {
        themeLight: '浅色',
        themeDark: '深色',
        heroBadge: '智能AI Sniffer在线视频多线程嗅探解析引擎',
        heroTitleAll: '全能在线视频解析下载工具',
        heroSubtitleAll: '支持YouTube、Facebook、TikTok、Instagram等1822+网站高速下载1080p、4K及MP3音频',
        placeholderAll: '在此粘贴视频链接 (YouTube, TikTok, Facebook...)',
        heroTitleFB: 'Facebook视频下载 (高清1080p, 4K, Reels, Story)',
        heroSubtitleFB: '下载Facebook公开视频、Reels短视频，保留原声高码率',
        placeholderFB: '粘贴Facebook视频链接...',
        heroTitleTT: 'TikTok去水印无Logo视频下载',
        heroSubtitleTT: '一键提取TikTok无水印高清视频及背景音乐MP3',
        placeholderTT: '粘贴TikTok视频链接...',
        heroTitleYT: 'YouTube 4K视频下载与MP3 320kbps转换器',
        heroSubtitleYT: '高速解析YouTube 1080p 60fps、4K画质及纯音频MP3',
        placeholderYT: '粘贴YouTube视频链接...',
        heroTitleIG: 'Instagram Reels & 快拍视频高清下载',
        heroSubtitleIG: '原画质保存Instagram Reels短视频、Story和高清图片',
        placeholderIG: '粘贴Instagram视频链接...',
        btnPaste: '📋 粘贴链接',
        btnDownload: '立即下载 ⚡',
        sampleLabel: '一键测试样例:',
        deviceTitle: '各设备详细使用指南',
        deviceSubtitle: '手机与电脑均可在3步之内轻松保存视频',
        featTitle: 'AI Downloader Pro 核心优势',
        featSubtitle: '新一代多线程嗅探技术，下载速度超越传统网站10倍',
        faqTitle: '常见问题解答 (FAQ)',
        faqSubtitle: '为您解答关于视频下载与会员功能的常见疑问'
    },
    es: {
        themeLight: 'Claro',
        themeDark: 'Oscuro',
        heroBadge: 'Extracción inteligente de video en línea mediante AI Sniffer',
        heroTitleAll: 'Descargador de Videos Online Todo en Uno',
        heroSubtitleAll: 'Descarga videos a máxima velocidad de YouTube, Facebook, TikTok, Instagram y 1,822+ sitios en 1080p, 4K y MP3',
        placeholderAll: 'Pega el enlace del video aquí (YouTube, TikTok, Facebook...)',
        heroTitleFB: 'Descargar Videos de Facebook Gratis (Full HD 1080p, 4K, Reels)',
        heroSubtitleFB: 'Descarga videos públicos de Facebook, Reels y Stories con audio original',
        placeholderFB: 'Pega el enlace de video de Facebook aquí...',
        heroTitleTT: 'Descargar Videos de TikTok Sin Marca de Agua Gratis',
        heroSubtitleTT: 'Descarga videos de TikTok en HD sin logo y extrae audio MP3',
        placeholderTT: 'Pega el enlace de TikTok aquí...',
        heroTitleYT: 'Descargador de YouTube 4K y Conversor MP3 320kbps',
        heroSubtitleYT: 'Extrae videos de YouTube en 1080p, 4K y audio MP3 de alta fidelidad',
        placeholderYT: 'Pega el enlace de YouTube aquí...',
        heroTitleIG: 'Descargador de Instagram Reels y Stories HD',
        heroSubtitleIG: 'Guarda Reels, Stories y fotos de Instagram en calidad original',
        placeholderIG: 'Pega el enlace de Instagram aquí...',
        btnPaste: '📋 Pegar Enlace',
        btnDownload: 'DESCARGAR ⚡',
        sampleLabel: 'Enlaces de prueba:',
        deviceTitle: 'Cómo descargar en cualquier dispositivo',
        deviceSubtitle: 'Guarda videos en tu móvil o PC en solo 3 sencillos pasos',
        featTitle: 'Ventajas de AI Downloader Pro',
        featSubtitle: 'Tecnología Sniffer multihilo hasta 10 veces más rápida que otros sitios',
        faqTitle: 'Preguntas Frecuentes (FAQ)',
        faqSubtitle: 'Resolvemos todas tus dudas sobre nuestro motor de descargas'
    }
};

function initLanguageSelector() {
    const langSelect = document.getElementById('lang-select');
    if (!langSelect) return;

    const savedLang = localStorage.getItem('ai_downloader_lang') || 'vi';
    langSelect.value = savedLang;

    langSelect.addEventListener('change', (e) => {
        const lang = e.target.value;
        localStorage.setItem('ai_downloader_lang', lang);
        applyLanguage(lang);
        showToast(`Đã đổi ngôn ngữ sang: ${e.target.options[e.target.selectedIndex].text}`);
    });

    applyLanguage(savedLang);
}

function applyLanguage(lang) {
    const dict = I18N[lang] || I18N['vi'];

    // Update Hero elements based on currentPlatform
    selectPlatform(currentPlatform, false);

    const btnPaste = document.getElementById('btn-paste-url');
    if (btnPaste) btnPaste.textContent = dict.btnPaste;

    const btnDownload = document.querySelector('#btn-sniff-action .btn-text');
    if (btnDownload) btnDownload.textContent = dict.btnDownload;

    const sampleLabel = document.getElementById('sample-label');
    if (sampleLabel) sampleLabel.textContent = dict.sampleLabel;

    const devTitle = document.getElementById('guide-device-title');
    if (devTitle) devTitle.textContent = dict.deviceTitle;

    const devSub = document.getElementById('guide-device-subtitle');
    if (devSub) devSub.textContent = dict.deviceSubtitle;

    const featTitle = document.getElementById('feat-title');
    if (featTitle) featTitle.textContent = dict.featTitle;

    const featSub = document.getElementById('feat-subtitle');
    if (featSub) featSub.textContent = dict.featSubtitle;

    const faqTitle = document.getElementById('faq-title');
    if (faqTitle) faqTitle.textContent = dict.faqTitle;

    const faqSub = document.getElementById('faq-subtitle');
    if (faqSub) faqSub.textContent = dict.faqSubtitle;
}

// ==========================================================================
// 0.2 CHUYỂN ĐỔI CHẾ ĐỘ NỀN TẢNG (FB, TIKTOK, YOUTUBE, IG ĐỀU CÓ Ô DÁN LINK)
// ==========================================================================
let currentPlatform = 'all';

const PLATFORM_SAMPLES = {
    all: [
        { label: '🎥 YouTube 4K', url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' },
        { label: '🎵 TikTok Không Logo', url: 'https://www.tiktok.com/@creator/video/123456789' },
        { label: '📘 Facebook Reels', url: 'https://www.facebook.com/reel/987654321' },
        { label: '🔒 1822+ VIP Network', url: 'https://www.pornhub.com/view_video.php?viewkey=ph123456' }
    ],
    facebook: [
        { label: '📘 Facebook Reels Triệu View', url: 'https://www.facebook.com/reel/987654321' },
        { label: '📺 Facebook Watch HD 1080p', url: 'https://www.facebook.com/watch/?v=1020304050' },
        { label: '✨ Facebook Story HD', url: 'https://www.facebook.com/stories/1234567890' }
    ],
    tiktok: [
        { label: '🎵 TikTok Trend Không Logo', url: 'https://www.tiktok.com/@creator/video/123456789' },
        { label: '🎧 TikTok Tách Nhạc MP3', url: 'https://www.tiktok.com/@music/video/987654321' },
        { label: '⚡ TikTok 1080p Siêu Nét', url: 'https://www.tiktok.com/@top/video/555666777' }
    ],
    youtube: [
        { label: '🎥 YouTube 4K Ultra HD', url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' },
        { label: '🎵 YouTube Tách MP3 320k', url: 'https://www.youtube.com/watch?v=3JZ_D3ELwOQ' },
        { label: '⚡ YouTube Shorts HD', url: 'https://www.youtube.com/shorts/abcd1234efg' }
    ],
    instagram: [
        { label: '📷 Instagram Reels Full HD', url: 'https://www.instagram.com/reel/C3zY123456/' },
        { label: '✨ Instagram Story HD', url: 'https://www.instagram.com/stories/travel/123456/' },
        { label: '🖼️ Instagram Post Video', url: 'https://www.instagram.com/p/B_12345678/' }
    ]
};

function selectPlatform(platform, scroll = true) {
    currentPlatform = platform || 'all';
    switchPanel('view-home');

    // Update active tab in hero
    document.querySelectorAll('.plat-tab').forEach(t => {
        if (t.getAttribute('data-platform') === currentPlatform) {
            t.classList.add('active');
        } else {
            t.classList.remove('active');
        }
    });

    // Update active in header nav
    document.querySelectorAll('.header-nav .nav-item').forEach(link => {
        if (link.getAttribute('data-platform') === currentPlatform) {
            link.classList.add('active');
        } else if (link.getAttribute('data-platform')) {
            link.classList.remove('active');
        }
    });

    const lang = localStorage.getItem('ai_downloader_lang') || 'vi';
    const dict = I18N[lang] || I18N['vi'];

    const heroTitle = document.getElementById('hero-title');
    const heroSubtitle = document.getElementById('hero-subtitle');
    const input = document.getElementById('video-url-input');
    const badge = document.getElementById('hero-badge');

    if (currentPlatform === 'facebook') {
        if (heroTitle) heroTitle.textContent = dict.heroTitleFB;
        if (heroSubtitle) heroSubtitle.textContent = dict.heroSubtitleFB;
        if (input) input.placeholder = dict.placeholderFB;
        if (badge) badge.innerHTML = `<span class="hero-badge-dot"></span> 📘 FACEBOOK REELS & WATCH DOWNLOADER`;
    } else if (currentPlatform === 'tiktok') {
        if (heroTitle) heroTitle.textContent = dict.heroTitleTT;
        if (heroSubtitle) heroSubtitle.textContent = dict.heroSubtitleTT;
        if (input) input.placeholder = dict.placeholderTT;
        if (badge) badge.innerHTML = `<span class="hero-badge-dot"></span> 🎵 TIKTOK NO WATERMARK DOWNLOADER`;
    } else if (currentPlatform === 'youtube') {
        if (heroTitle) heroTitle.textContent = dict.heroTitleYT;
        if (heroSubtitle) heroSubtitle.textContent = dict.heroSubtitleYT;
        if (input) input.placeholder = dict.placeholderYT;
        if (badge) badge.innerHTML = `<span class="hero-badge-dot"></span> ▶️ YOUTUBE 4K & MP3 CONVERTER`;
    } else if (currentPlatform === 'instagram') {
        if (heroTitle) heroTitle.textContent = dict.heroTitleIG;
        if (heroSubtitle) heroSubtitle.textContent = dict.heroSubtitleIG;
        if (input) input.placeholder = dict.placeholderIG;
        if (badge) badge.innerHTML = `<span class="hero-badge-dot"></span> 📷 INSTAGRAM REELS & STORY DOWNLOADER`;
    } else {
        if (heroTitle) heroTitle.textContent = dict.heroTitleAll;
        if (heroSubtitle) heroSubtitle.textContent = dict.heroSubtitleAll;
        if (input) input.placeholder = dict.placeholderAll;
        if (badge) badge.innerHTML = `<span class="hero-badge-dot"></span> ${dict.heroBadge}`;
    }

    renderSampleLinks();

    if (scroll) {
        const searchBox = document.querySelector('.search-box-wrapper');
        if (searchBox) searchBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

function renderSampleLinks() {
    const box = document.getElementById('sample-links-box');
    if (!box) return;

    const samples = PLATFORM_SAMPLES[currentPlatform] || PLATFORM_SAMPLES['all'];
    const lang = localStorage.getItem('ai_downloader_lang') || 'vi';
    const dict = I18N[lang] || I18N['vi'];

    box.innerHTML = `<span class="sample-label" id="sample-label">${dict.sampleLabel}</span>`;
    samples.forEach(s => {
        const btn = document.createElement('button');
        btn.className = 'sample-tag';
        btn.setAttribute('data-url', s.url);
        btn.textContent = s.label;
        btn.onclick = () => {
            const input = document.getElementById('video-url-input');
            input.value = s.url;
            triggerSnifferAnalysis(s.url);
        };
        box.appendChild(btn);
    });
}

function initPlatformSwitching() {
    // Tabs above search input
    document.querySelectorAll('.plat-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const platform = tab.getAttribute('data-platform');
            selectPlatform(platform, true);
        });
    });

    // Quick cards under search box
    document.querySelectorAll('.platform-card[data-url]').forEach(card => {
        card.addEventListener('click', () => {
            const url = card.getAttribute('data-url');
            if (url.includes('facebook')) selectPlatform('facebook', true);
            else if (url.includes('tiktok')) selectPlatform('tiktok', true);
            else if (url.includes('youtube')) selectPlatform('youtube', true);
            else if (url.includes('instagram')) selectPlatform('instagram', true);
        });
    });
}

// ==========================================================================
// 0.3 KHÚC GIỮA: TƯƠNG TÁC THIẾT BỊ & CÂU HỎI THƯỜNG GẶP FAQ
// ==========================================================================
const DEVICE_GUIDES = {
    android: [
        { num: '1', title: 'Mở App & Sao chép link', desc: 'Mở ứng dụng YouTube, TikTok hoặc Facebook, nhấn nút <strong>Chia sẻ (Share)</strong> và chọn <strong>Sao chép liên kết</strong>.' },
        { num: '2', title: 'Dán link vào trình duyệt', desc: 'Mở Google Chrome trên Android, truy cập <strong>AI Downloader Pro</strong> và dán liên kết vào khung tìm kiếm.' },
        { num: '3', title: 'Chọn chất lượng & Tải về', desc: 'Chọn độ phân giải Full HD 1080p hoặc MP3, video sẽ tự động tải về thư mục <strong>Downloads / Bộ sưu tập</strong> của máy.' }
    ],
    ios: [
        { num: '1', title: 'Sao chép link từ ứng dụng', desc: 'Mở app YouTube, TikTok, Facebook hoặc Instagram trên iPhone/iPad, bấm <strong>Share</strong> và chọn <strong>Copy Link</strong>.' },
        { num: '2', title: 'Mở Safari & Dán link', desc: 'Mở trình duyệt <strong>Safari</strong> (yêu cầu iOS 13 trở lên), truy cập AI Downloader Pro và dán link vào ô tải.' },
        { num: '3', title: 'Lưu vào Thư viện Ảnh (Photos)', desc: 'Bấm Tải về, sau khi hoàn tất nhấn vào icon tải ở góc Safari > Chia sẻ > <strong>Lưu video (Save Video)</strong> để vào Photos.' }
    ],
    pc: [
        { num: '1', title: 'Sao chép link video', desc: 'Nhấp chuột phải vào thanh địa chỉ trình duyệt hoặc nút chia sẻ trên video bạn đang xem và chọn <strong>Copy</strong>.' },
        { num: '2', title: 'Dán link hoặc dùng Extension', desc: 'Dán link vào khung tìm kiếm hoặc cài đặt <strong>Chrome Extension</strong> của chúng tôi để hiện nút Download trực tiếp.' },
        { num: '3', title: 'Tải 1-Click tốc độ tối đa', desc: 'Nhấn chọn chất lượng mong muốn (1080p 60fps, 4K hoặc MP3), tệp sẽ được lưu vào thư mục <strong>Downloads</strong> của máy tính.' }
    ]
};

function initMidPageInteractive() {
    // Device tabs
    document.querySelectorAll('.device-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.device-tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const device = btn.getAttribute('data-device');
            const steps = DEVICE_GUIDES[device] || DEVICE_GUIDES['android'];
            const container = document.getElementById('device-steps-container');
            if (container) {
                container.innerHTML = steps.map(s => `
                    <div class="device-step-item">
                        <span class="device-step-badge">${s.num}</span>
                        <h4>${s.title}</h4>
                        <p>${s.desc}</p>
                    </div>
                `).join('');
            }
        });
    });

    // FAQ Accordion
    document.querySelectorAll('.faq-item .faq-question').forEach(q => {
        q.addEventListener('click', () => {
            const item = q.parentElement;
            const wasActive = item.classList.contains('active');
            document.querySelectorAll('.faq-item').forEach(i => i.classList.remove('active'));
            if (!wasActive) item.classList.add('active');
        });
    });
}

// ==========================================================================
// 0.4 TƯƠNG TÁC MENU CHÂN TRANG (FOOTER LINKS)
// ==========================================================================
function initFooterLinks() {
    // Footer platform links
    document.querySelectorAll('.footer-nav-link[data-platform]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const platform = link.getAttribute('data-platform');
            selectPlatform(platform, true);
        });
    });

    // Footer extension trigger
    const footExt = document.getElementById('footer-btn-ext');
    if (footExt) footExt.addEventListener('click', () => switchPanel('view-extension'));

    // Footer VIP trigger
    const footVip = document.getElementById('footer-btn-vip');
    if (footVip) footVip.addEventListener('click', () => switchPanel('view-paywall'));

    // Footer FAQ trigger
    const footFaq = document.getElementById('footer-btn-faq');
    if (footFaq) {
        footFaq.addEventListener('click', () => {
            switchPanel('view-home');
            const faq = document.getElementById('faq-section');
            if (faq) faq.scrollIntoView({ behavior: 'smooth' });
        });
    }

    // Footer guide triggers
    document.querySelectorAll('.footer-guide-trigger').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const device = link.getAttribute('data-device');
            switchPanel('view-home');
            const btn = document.querySelector(`.device-tab-btn[data-device="${device}"]`);
            if (btn) btn.click();
            const section = document.getElementById('device-guide-section');
            if (section) section.scrollIntoView({ behavior: 'smooth' });
        });
    });

    // Legal triggers
    document.querySelectorAll('.footer-legal-trigger').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const type = link.getAttribute('data-type');
            const msgs = {
                tos: 'Điều khoản dịch vụ: AI Downloader Pro chỉ phục vụ mục đích sử dụng cá nhân và học tập phi thương mại.',
                privacy: 'Chính sách quyền riêng tư: Chúng tôi tuyệt đối không lưu trữ hoặc chia sẻ thông tin video tải về của bạn.',
                dmca: 'Bản quyền & DMCA: Chúng tôi tôn trọng quyền sở hữu trí tuệ của mọi tác giả và tuân thủ các quy định bản quyền quốc tế.',
                disclaimer: 'Tuyên bố miễn trừ: AI Downloader Pro không chịu trách nhiệm đối với các nội dung do người dùng tự ý tải xuống.'
            };
            showToast(msgs[type] || 'Thông tin pháp lý', 'info');
        });
    });
}

// ==========================================================================
// 1. PROTOTYPE CONTROLLER (CHUYỂN MÀN HÌNH TỰ DO)
// ==========================================================================
function switchPanel(panelId) {
    document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.proto-btn').forEach(b => b.classList.remove('active'));

    const targetPanel = document.getElementById(panelId);
    if (targetPanel) {
        targetPanel.classList.add('active');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    const activeBtn = document.querySelector(`.proto-btn[data-target="${panelId}"]`);
    if (activeBtn) activeBtn.classList.add('active');

    // Ghi nhận Firebase Analytics Telemetry tương ứng với màn hình
    if (typeof logFirebaseEvent === 'function') {
        if (panelId === 'view-home') {
            logFirebaseEvent('View_Home', { screen_name: 'Home_Browser', screen_class: 'MainActivity' }, 'Web Portal', true);
            logFirebaseEvent('screen_view', { screen_name: 'Home_Browser', screen_class: 'MainActivity' }, 'Web Portal', true);
        } else if (panelId === 'view-sniffer') {
            logFirebaseEvent('View_Progess', { screen_name: 'Progress_Downloader', screen_class: 'ProgressFragment' }, 'Web Portal', true);
            logFirebaseEvent('screen_view', { screen_name: 'Progress_Downloader', screen_class: 'ProgressFragment' }, 'Web Portal', true);
        } else if (panelId === 'view-paywall') {
            logFirebaseEvent('paywall_view', { trigger_reason: 'prototype_nav', screen_name: 'PaymentWall' }, 'Web Portal', true);
            logFirebaseEvent('screen_view', { screen_name: 'PaymentWall', screen_class: 'PaymentWallActivity' }, 'Web Portal', true);
        } else if (panelId === 'view-admin') {
            logFirebaseEvent('screen_view', { screen_name: 'Admin_Portal', screen_class: 'AdminDashboard' }, 'Web Portal', true);
        }
    }

    // Tự động tải dữ liệu tương ứng
    if (panelId === 'view-admin') {
        loadAdminDashboardData();
    }
}

function initPrototypeNavigation() {
    document.querySelectorAll('.proto-btn[data-target]').forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-target');
            switchPanel(target);
        });
    });

    document.getElementById('brand-logo-btn').addEventListener('click', () => {
        selectPlatform('all', true);
    });

    // Header nav links
    document.querySelectorAll('.header-nav .nav-item').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const platform = link.getAttribute('data-platform');
            if (platform) {
                selectPlatform(platform, true);
            } else if (link.classList.contains('nav-ext-link')) {
                switchPanel('view-extension');
            } else if (link.classList.contains('nav-vip-link')) {
                switchPanel('view-paywall');
            } else if (link.classList.contains('nav-admin-link')) {
                switchPanel('view-admin');
            }
        });
    });

    document.getElementById('btn-quick-install-ext').addEventListener('click', () => {
        switchPanel('view-extension');
    });

    document.getElementById('btn-sniffer-back').addEventListener('click', () => {
        switchPanel('view-home');
    });
}

// ==========================================================================
// 2. AUTHENTICATION & SOCIAL LOGIN (GOOGLE / FACEBOOK)
// ==========================================================================
function initAuth() {
    const authModal = document.getElementById('auth-modal');
    const btnOpenModal = document.getElementById('btn-login-modal');
    const btnProtoAuth = document.getElementById('btn-proto-auth');
    const btnCloseModal = document.getElementById('btn-close-auth');
    const tabLogin = document.getElementById('tab-btn-login');
    const tabRegister = document.getElementById('tab-btn-register');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    function openModal(isRegister = false) {
        authModal.style.display = 'flex';
        if (isRegister) {
            tabRegister.click();
        } else {
            tabLogin.click();
        }
    }

    if (btnOpenModal) btnOpenModal.addEventListener('click', () => openModal(false));
    if (btnProtoAuth) btnProtoAuth.addEventListener('click', () => openModal(false));
    if (btnCloseModal) btnCloseModal.addEventListener('click', () => authModal.style.display = 'none');
    authModal.addEventListener('click', (e) => {
        if (e.target === authModal) authModal.style.display = 'none';
    });

    tabLogin.addEventListener('click', () => {
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
        loginForm.style.display = 'flex';
        registerForm.style.display = 'none';
    });

    tabRegister.addEventListener('click', () => {
        tabRegister.classList.add('active');
        tabLogin.classList.remove('active');
        registerForm.style.display = 'flex';
        loginForm.style.display = 'none';
    });

    // Form Đăng Nhập truyền thống
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        try {
            const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();
            if (data.success) {
                setCurrentUser(data.user, data.token);
                authModal.style.display = 'none';
                showToast(`Chào mừng trở lại, ${data.user.name}!`);
            } else {
                showToast(data.message || 'Đăng nhập thất bại', 'error');
            }
        } catch (err) {
            showToast('Không thể kết nối đến Backend (Port 5101)', 'error');
        }
    });

    // Form Đăng Ký
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;
        const password = document.getElementById('reg-password').value;

        try {
            const res = await fetch(`${BACKEND_URL}/api/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password })
            });
            const data = await res.json();
            if (data.success) {
                setCurrentUser(data.user, data.token);
                authModal.style.display = 'none';
                showToast('Đăng ký tài khoản thành công!');
            } else {
                showToast(data.message || 'Đăng ký thất bại', 'error');
            }
        } catch (err) {
            showToast('Không thể kết nối đến Backend (Port 5101)', 'error');
        }
    });

    // ĐĂNG NHẬP GOOGLE OAUTH
    const btnGoogle = document.getElementById('btn-login-google');
    if (btnGoogle) {
        btnGoogle.addEventListener('click', () => {
            const googleUser = {
                id: 'usr_google_chutich',
                name: 'Chủ Tịch (Google Account)',
                email: 'chutich.google@gmail.com',
                isVip: true,
                vipExpiry: '2099-12-31T23:59:59.000Z',
                plan: 'plan_lifetime',
                role: 'ADMIN',
                downloadCount: 0,
                provider: 'Google'
            };
            setCurrentUser(googleUser, 'mock_google_token_123456');
            authModal.style.display = 'none';
            showToast('Đăng nhập Google thành công! Chào mừng Chủ tịch!');
        });
    }

    // ĐĂNG NHẬP FACEBOOK OAUTH
    const btnFacebook = document.getElementById('btn-login-facebook');
    if (btnFacebook) {
        btnFacebook.addEventListener('click', () => {
            const fbUser = {
                id: 'usr_fb_chutich',
                name: 'Chủ Tịch (Facebook ID)',
                email: 'chutich.fb@facebook.com',
                isVip: true,
                vipExpiry: '2099-12-31T23:59:59.000Z',
                plan: 'plan_lifetime',
                role: 'ADMIN',
                downloadCount: 0,
                provider: 'Facebook'
            };
            setCurrentUser(fbUser, 'mock_fb_token_123456');
            authModal.style.display = 'none';
            showToast('Đăng nhập Facebook thành công! Chào mừng Chủ tịch!');
        });
    }

    // Khôi phục session từ localStorage
    const savedToken = localStorage.getItem('ai_user_token');
    if (savedToken) {
        if (savedToken.startsWith('mock_')) {
            setCurrentUser({
                id: 'usr_chutich_vip',
                name: 'Chủ Tịch VIP',
                email: 'chutich@nextai.com',
                isVip: true,
                isPro: true,
                role: 'ADMIN'
            }, savedToken);
        } else {
            fetch(`${BACKEND_URL}/api/auth/me?userId=${savedToken}`)
                .then(r => r.json())
                .then(data => {
                    if (data.success) setCurrentUser(data.user, savedToken);
                })
                .catch(() => {});
        }
    }
}

function setCurrentUser(user, token) {
    currentUser = user;
    if (token) localStorage.setItem('ai_user_token', token);

    const btnLoginModal = document.getElementById('btn-login-modal');
    const existingPill = document.getElementById('user-profile-pill');
    if (existingPill) existingPill.remove();

    const pill = document.createElement('div');
    pill.className = 'user-logged-pill';
    pill.id = 'user-profile-pill';
    pill.title = 'Click để xem thông tin hoặc đăng xuất';
    pill.innerHTML = `
        <img class="user-logged-avatar" src="${user.avatar || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + encodeURIComponent(user.email)}" alt="Avatar">
        <span>${user.name}</span>
        ${user.isVip || user.isPro ? '<span class="brand-pro">VIP</span>' : '<span style="font-size:11px;color:#9ca3af;">(Free)</span>'}
    `;

    pill.onclick = () => {
        if (confirm(`Bạn đang đăng nhập với tài khoản:\n${user.name} (${user.email})\nTrạng thái: ${user.isVip || user.isPro ? 'VIP PRO (Không giới hạn)' : 'FREE TIER (10 lượt)'}\n\nBạn có muốn đăng xuất không?`)) {
            localStorage.removeItem('ai_user_token');
            location.reload();
        }
    };

    if (btnLoginModal) {
        btnLoginModal.style.display = 'none';
        btnLoginModal.parentNode.appendChild(pill);
    }
}

// ==========================================================================
// 3. SNIFFER ENGINE & TẢI VIDEO
// ==========================================================================
function initDownloader() {
    const input = document.getElementById('video-url-input');
    const btnSniff = document.getElementById('btn-sniff-action');
    const btnPaste = document.getElementById('btn-paste-url');

    btnPaste.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            if (text) {
                input.value = text;
                showToast('Đã dán liên kết!');
            }
        } catch (e) {
            showToast('Vui lòng cấp quyền dán hoặc dán thủ công bằng Ctrl+V');
        }
    });

    btnSniff.addEventListener('click', () => {
        const url = input.value.trim();
        if (!url) {
            showToast('Vui lòng nhập đường dẫn URL video!', 'error');
            input.focus();
            return;
        }
        triggerSnifferAnalysis(url);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') btnSniff.click();
    });

    // Sample Links Click
    document.querySelectorAll('.sample-tag').forEach(tag => {
        tag.addEventListener('click', () => {
            const url = tag.getAttribute('data-url');
            input.value = url;
            triggerSnifferAnalysis(url);
        });
    });

    document.querySelectorAll('.platform-card').forEach(card => {
        card.addEventListener('click', () => {
            const url = card.getAttribute('data-url');
            input.value = url;
            triggerSnifferAnalysis(url);
        });
    });
}

async function triggerSnifferAnalysis(url) {
    if (typeof logFirebaseEvent === 'function') {
        logFirebaseEvent('Click_Search', { search_query: url, source: 'home_search_bar' }, 'Web Portal', true);
    }
    showToast('Đang kết nối Sniffer Engine và bóc tách luồng media...', 'info');

    try {
        const res = await fetch(`${BACKEND_URL}/api/sniff`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                url,
                userId: currentUser ? currentUser.id : null
            })
        });

        const result = await res.json();
        if (result.success && result.data) {
            currentSniffData = result.data;
            renderSnifferResult(result.data, result.limits);
            switchPanel('view-sniffer');
        } else {
            showToast(result.message || 'Không thể bóc tách luồng video này.', 'error');
        }
    } catch (e) {
        showToast('Lỗi kết nối Backend Port 5101. Vui lòng đảm bảo server đang chạy!', 'error');
    }
}

function renderSnifferResult(data, limits) {
    document.getElementById('res-thumb').src = data.thumbnail;
    document.getElementById('res-duration').innerText = data.duration;
    const durDetail = document.getElementById('res-duration-detail');
    if (durDetail) durDetail.innerText = data.duration;
    const countTag = document.getElementById('sniffer-status-count');
    if (countTag) countTag.innerHTML = `<span class="status-pulse"></span> Bóc tách thành công ${data.formats.length} luồng tải`;
    document.getElementById('res-platform-badge').innerText = data.platform.name;
    document.getElementById('res-author').innerText = `Nguồn: ${data.author}`;
    document.getElementById('res-title').innerText = data.title;
    document.getElementById('res-domain-category').innerHTML = data.isMonetized 
        ? `<strong style="color:#f59e0b;">🔒 Mạng Lưới Bản Quyền VIP (1822+)</strong>`
        : `Phân loại: <strong>Công cộng</strong>`;

    const container = document.getElementById('formats-list-container');
    container.innerHTML = '';

    data.formats.forEach(fmt => {
        const row = document.createElement('div');
        row.className = 'format-row';

        let badgeClass = 'badge-480';
        if (fmt.quality === '1080p') badgeClass = 'badge-1080';
        else if (fmt.quality === '720p') badgeClass = 'badge-720';
        else if (fmt.isAudioOnly) badgeClass = 'badge-audio';

        row.innerHTML = `
            <div class="fmt-left">
                <span class="fmt-badge ${badgeClass}">${fmt.quality}</span>
                <span class="fmt-title">${fmt.label}</span>
            </div>
            <div class="fmt-right">
                <span class="fmt-filesize">${fmt.size}</span>
                <button class="btn-fmt-dl" data-fmt="${fmt.id}" data-quality="${fmt.quality}">TẢI NGAY ⬇</button>
            </div>
        `;

        row.querySelector('.btn-fmt-dl').onclick = () => {
            handleDownloadAction(fmt);
        };

        container.appendChild(row);
    });
}

async function handleDownloadAction(format) {
    if (typeof logFirebaseEvent === 'function') {
        logFirebaseEvent('Click_button_Download', { quality: format.quality, format: format.id, is_monetized: false }, 'Web Portal', true);
    }
    const progressBox = document.getElementById('download-progress-box');
    const progFileName = document.getElementById('prog-file-name');
    const progFill = document.getElementById('prog-bar-fill');
    const progPercent = document.getElementById('prog-percent-text');
    const progStatus = document.getElementById('prog-status');

    showToast(`Đang kiểm tra quyền hạn tải ${format.quality}...`, 'info');

    try {
        const res = await fetch(`${BACKEND_URL}/api/download/trigger`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: currentUser ? currentUser.id : null,
                url: currentSniffData ? currentSniffData.originalUrl : 'demo',
                quality: format.quality,
                formatId: format.id,
                streamUrl: format.streamUrl || null,
                fileName: currentSniffData ? currentSniffData.title : null
            })
        });

        const data = await res.json();

        if (!data.success) {
            if (data.requiresSubscription) {
                // Đã đạt mốc 10 video hoặc website VIP -> Bật Paywall
                if (confirm(`${data.message}\n\nBạn có muốn chuyển sang màn hình Đăng ký VIP Pro ngay không?`)) {
                    switchPanel('view-paywall');
                }
            } else {
                showToast(data.message || 'Lỗi tải video', 'error');
            }
            return;
        }

        // Bắt đầu mô phỏng tiến trình tải trực quan (0% -> 100%)
        progressBox.style.display = 'block';
        progFileName.innerText = `Đang tải: ${data.fileName || 'Video_Ultra_HD.mp4'}`;
        progStatus.innerText = 'Đang kết nối luồng tốc độ cao (100MB/s)...';
        progFill.style.width = '0%';
        progPercent.innerText = '0%';

        let progress = 0;
        const interval = setInterval(() => {
            progress += 15;
            if (progress >= 100) {
                progress = 100;
                clearInterval(interval);
                progFill.style.width = '100%';
                progPercent.innerText = '100%';
                progStatus.innerText = 'Tải xuống hoàn tất! Đang lưu file về máy...';

                // Tải file về trình duyệt
                const link = document.createElement('a');
                link.href = `${BACKEND_URL}${data.downloadUrl}`;
                link.download = data.fileName;
                document.body.appendChild(link);
                link.click();
                link.remove();

                showToast('Tải video thành công về thiết bị!', 'success');
                setTimeout(() => progressBox.style.display = 'none', 4000);
            } else {
                progFill.style.width = `${progress}%`;
                progPercent.innerText = `${progress}%`;
            }
        }, 180);

    } catch (e) {
        showToast('Không thể kết nối đến Backend Port 5101', 'error');
    }
}

// ==========================================================================
// 4. ADMIN PORTAL (QUẢN TRỊ USER, GÓI SUB, DOANH THU)
// ==========================================================================
function initAdminPortal() {
    document.querySelectorAll('.admin-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.admin-tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.admin-tab-content').forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const target = btn.getAttribute('data-tab');
            document.getElementById(target).classList.add('active');
        });
    });
}

async function loadAdminDashboardData() {
    try {
        // 1. Tải Doanh thu
        const revRes = await fetch(`${BACKEND_URL}/api/admin/revenue`);
        const revData = await revRes.json();
        if (revData.success && revData.stats) {
            renderRevenueStats(revData.stats);
        }

        // 2. Tải Users
        const usersRes = await fetch(`${BACKEND_URL}/api/admin/users`);
        const usersData = await usersRes.json();
        if (usersData.success && usersData.users) {
            renderUsersTable(usersData.users);
        }

        // 3. Tải Gói Subscriptions & Transactions
        const subRes = await fetch(`${BACKEND_URL}/api/admin/subscriptions`);
        const subData = await subRes.json();
        if (subData.success && subData.subscriptions) {
            renderTransactionsList(subData.subscriptions);
        }

        // 4. Tải Danh sách gói cước
        const plansRes = await fetch(`${BACKEND_URL}/api/subscription/plans`);
        const plansData = await plansRes.json();
        if (plansData.success && plansData.plans) {
            renderPlansManagement(plansData.plans);
        }
    } catch (err) {
        console.error('Error loading admin data:', err);
    }
}

function renderRevenueStats(stats) {
    document.getElementById('kpi-today-usd').innerText = `$${stats.today.revenueUSD}`;
    document.getElementById('kpi-today-vnd').innerText = `≈ ${stats.today.revenueVND.toLocaleString()} đ`;
    document.getElementById('kpi-today-orders').innerText = `${stats.today.orders} đơn hàng thành công`;

    document.getElementById('kpi-week-usd').innerText = `$${stats.thisWeek.revenueUSD}`;
    document.getElementById('kpi-week-vnd').innerText = `≈ ${stats.thisWeek.revenueVND.toLocaleString()} đ`;
    document.getElementById('kpi-week-orders').innerText = `${stats.thisWeek.orders} đơn hàng`;

    document.getElementById('kpi-month-usd').innerText = `$${stats.thisMonth.revenueUSD}`;
    document.getElementById('kpi-month-vnd').innerText = `≈ ${stats.thisMonth.revenueVND.toLocaleString()} đ`;
    document.getElementById('kpi-month-orders').innerText = `${stats.thisMonth.orders} đơn hàng`;

    document.getElementById('kpi-year-usd').innerText = `$${stats.thisYear.revenueUSD}`;
    document.getElementById('kpi-year-vnd').innerText = `≈ ${stats.thisYear.revenueVND.toLocaleString()} đ`;
    document.getElementById('kpi-year-orders').innerText = `${stats.thisYear.orders} giao dịch`;

    document.getElementById('stat-total-users').innerText = stats.totalUsers || 5;
    document.getElementById('stat-vip-users').innerText = stats.vipUsers || 2;

    // Render Bar Chart 7 ngày
    const chartContainer = document.getElementById('revenue-chart-bars');
    chartContainer.innerHTML = '';
    const maxVal = Math.max(...stats.chartLast7Days.map(d => d.revenueUSD), 80);

    stats.chartLast7Days.forEach(day => {
        const heightPercent = Math.max(12, Math.round((day.revenueUSD / maxVal) * 100));
        const col = document.createElement('div');
        col.className = 'chart-col';
        col.innerHTML = `
            <span class="chart-val">$${day.revenueUSD}</span>
            <div class="chart-bar-rect" style="height: ${heightPercent}%;"></div>
            <span class="chart-date">${day.label}</span>
        `;
        chartContainer.appendChild(col);
    });
}

function renderUsersTable(users) {
    const tbody = document.getElementById('user-table-body');
    tbody.innerHTML = '';

    users.forEach(u => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                <div class="user-td-profile">
                    <img class="user-td-avatar" src="${u.avatar || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + u.email}">
                    <strong>${u.name}</strong>
                </div>
            </td>
            <td>${u.email}</td>
            <td>
                <span class="role-badge ${u.role === 'VIP' ? 'vip' : (u.role === 'ADMIN' ? 'admin' : 'user')}">
                    ${u.role}
                </span>
            </td>
            <td>${u.downloadCount} lượt</td>
            <td>${u.subscriptionPlan ? u.subscriptionPlan.toUpperCase() : 'Chưa đăng ký'}</td>
            <td>${u.subscriptionExpires ? new Date(u.subscriptionExpires).toLocaleDateString() : '—'}</td>
            <td>
                <button class="btn-toggle-vip" data-user-id="${u.id}">
                    ${u.isPro ? 'Hạ Cấp User' : 'Kích Hoạt VIP'}
                </button>
            </td>
        `;

        tr.querySelector('.btn-toggle-vip').onclick = async () => {
            try {
                const res = await fetch(`${BACKEND_URL}/api/admin/users/${u.id}/toggle-vip`, { method: 'POST' });
                const d = await res.json();
                showToast(d.message);
                loadAdminDashboardData();
            } catch (e) {
                showToast('Lỗi cập nhật người dùng', 'error');
            }
        };

        tbody.appendChild(tr);
    });
}

function renderTransactionsList(txs) {
    const box = document.getElementById('transactions-list-box');
    box.innerHTML = '';

    txs.slice(0, 10).forEach(tx => {
        const item = document.createElement('div');
        item.className = 'tx-row';
        item.innerHTML = `
            <div>
                <strong>${tx.userName}</strong> • <small style="color:#9ca3af;">${tx.planName}</small>
                <div style="font-size:11px;color:#6b7280;">${new Date(tx.createdAt).toLocaleString()} • ${tx.paymentMethod}</div>
            </div>
            <div class="tx-amount">+$${tx.amountUSD}</div>
        `;
        box.appendChild(item);
    });
}

function renderPlansManagement(plans) {
    const container = document.getElementById('plans-management-container');
    container.innerHTML = `
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px;">
            ${plans.map(p => `
                <div style="background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.08); border-radius:14px; padding:20px;">
                    <div style="font-size:11px;color:#10b981;font-weight:700;margin-bottom:6px;">${p.tag}</div>
                    <h3 style="font-size:17px;margin-bottom:8px;">${p.name}</h3>
                    <div style="font-size:24px;font-weight:800;color:#facc15;margin-bottom:4px;">$${p.price}</div>
                    <div style="font-size:12px;color:#9ca3af;margin-bottom:14px;">Thời hạn: ${p.durationDays} ngày</div>
                    <div style="font-size:11px;color:#34d399;">Trạng thái: Đang hoạt động</div>
                </div>
            `).join('')}
        </div>
    `;
}

// ==========================================================================
// 5. GÓI SUBSCRIPTION PRO & THANH TOÁN (PAYWALL CHUẨN APP)
// ==========================================================================
function initPaywall() {
    const priceCards = document.querySelectorAll('.price-card');
    const btnSubscribe = document.getElementById('btn-subscribe-trigger');
    const priceSummary = document.getElementById('sub-price-summary');

    priceCards.forEach(card => {
        card.addEventListener('click', () => {
            priceCards.forEach(c => {
                c.classList.remove('selected');
                const shimmer = c.querySelector('.card-shimmer');
                if (shimmer) shimmer.classList.remove('active-shimmer');
            });

            card.classList.add('selected');
            const shimmer = card.querySelector('.card-shimmer');
            if (shimmer) shimmer.classList.add('active-shimmer');

            selectedPlan = card.getAttribute('data-plan');

            if (selectedPlan === 'plan_monthly') {
                priceSummary.innerText = 'Chỉ $4.99 / tháng (Hủy bất cứ lúc nào)';
            } else if (selectedPlan === 'plan_annual') {
                priceSummary.innerText = 'Chỉ $39.99 / năm (Hủy bất cứ lúc nào)';
            } else {
                priceSummary.innerText = 'Chỉ $79.99 trọn đời (Thanh toán 1 lần)';
            }

            if (typeof logFirebaseEvent === 'function') {
                logFirebaseEvent('paywall_plan_selected', { plan_id: selectedPlan, price: priceSummary.innerText }, 'Web Portal', true);
            }
        });
    });

    btnSubscribe.addEventListener('click', async () => {
        if (typeof logFirebaseEvent === 'function') {
            logFirebaseEvent('paywall_cta_clicked', { plan_id: selectedPlan, price: priceSummary.innerText }, 'Web Portal', true);
        }
        if (!currentUser) {
            alert('Vui lòng đăng nhập hoặc tạo tài khoản trước khi đăng ký gói VIP!');
            document.getElementById('btn-login-modal').click();
            return;
        }

        showToast('Đang kết nối cổng thanh toán bảo mật...', 'info');

        try {
            const res = await fetch(`${BACKEND_URL}/api/subscription/subscribe`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: currentUser.id,
                    planId: selectedPlan,
                    paymentMethod: 'MoMo / VNPay QR'
                })
            });

            const data = await res.json();
            if (data.success) {
                currentUser.isPro = true;
                currentUser.role = 'VIP';
                setCurrentUser(currentUser);
                showToast(data.message, 'success');
                setTimeout(() => switchPanel('view-home'), 1500);
            } else {
                showToast(data.message || 'Đăng ký thất bại', 'error');
            }
        } catch (e) {
            showToast('Không thể kết nối Backend Port 5101', 'error');
        }
    });
}

// ==========================================================================
// 6. XỬ LÝ URL QUERY PARAMETERS (ĐỒNG BỘ VỚI EXTENSION)
// ==========================================================================
function handleUrlQueryParams() {
    const params = new URLSearchParams(window.location.search);
    const action = params.get('action');
    const url = params.get('url');

    if (url) {
        document.getElementById('video-url-input').value = decodeURIComponent(url);
        triggerSnifferAnalysis(decodeURIComponent(url));
    }

    if (action === 'login') {
        document.getElementById('btn-login-modal').click();
    } else if (action === 'register') {
        document.getElementById('btn-login-modal').click();
        document.getElementById('tab-btn-register').click();
    } else if (action === 'paywall') {
        switchPanel('view-paywall');
    }
}

// Toast Thông Báo Nhanh
function showToast(message, type = 'info') {
    const toast = document.getElementById('web-toast');
    const msg = document.getElementById('toast-msg');
    msg.innerText = message;

    if (type === 'error') {
        toast.style.borderColor = '#ef4444';
        toast.style.background = '#7f1d1d';
    } else if (type === 'success') {
        toast.style.borderColor = '#10b981';
        toast.style.background = '#064e3b';
    } else {
        toast.style.borderColor = '#3b82f6';
        toast.style.background = '#1e293b';
    }

    toast.style.display = 'block';
    setTimeout(() => toast.style.display = 'none', 3200);
}

// ==========================================================================
// 7. FIREBASE LIVE EVENT MONITOR & TELEMETRY HUB (PROJECT: ai-video-downloader-e9bc4)
// Tích hợp đầy đủ 28 sự kiện chuẩn từ Firebase/event.txt & Crashlytics
// ==========================================================================

const FIREBASE_CONFIG = {
    projectId: "ai-video-downloader-e9bc4",
    projectNumber: "70018456314",
    appId: "1:70018456314:android:0abf98ee32bc2504cf8743",
    apiKey: "AIzaSyBNiJq007rF_755yekW1e3rX5TlK4kB9IM",
    storageBucket: "ai-video-downloader-e9bc4.firebasestorage.app"
};

const ALL_28_EVENTS_SPEC = [
    // 1. Màn hình (5 events)
    { name: 'View_Home', cat: 'screen', platform: 'Android / Web', sample: { screen_name: 'Home_Browser', screen_class: 'MainActivity' } },
    { name: 'View_Progess', cat: 'screen', platform: 'Android / Web', sample: { screen_name: 'Progress_Downloader', active_tasks: 2 } },
    { name: 'View_Player', cat: 'screen', platform: 'Android App', sample: { screen_name: 'AcePlayer', video_title: 'Demo_Video_4K.mp4' } },
    { name: 'View_Setting', cat: 'screen', platform: 'Android App', sample: { screen_name: 'SettingsActivity', theme: 'dark' } },
    { name: 'screen_view', cat: 'screen', platform: 'Android / Web', sample: { screen_name: 'Admin_Portal', screen_class: 'AdminDashboard' } },

    // 2. Tương tác (4 events)
    { name: 'Click_Search', cat: 'action', platform: 'Android / Web', sample: { search_query: 'https://youtube.com/watch?v=sample', source: 'home_bar' } },
    { name: 'Click_Play_video', cat: 'action', platform: 'Android App', sample: { video_title: 'Sample_1080p.mp4', player_mode: 'hardware_accel' } },
    { name: 'Click_button_Download', cat: 'action', platform: 'Android / Web', sample: { quality: '1080p_60fps', format: 'mp4', is_monetized: false } },
    { name: 'Click_private Vault', cat: 'action', platform: 'Android App', sample: { action: 'open_secure_vault', pin_verified: true } },

    // 3. Quảng cáo AdMob (6 events)
    { name: 'ad_requested', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Interstitial', ad_unit_id: 'ca-app-pub-3940256099942544/1033173712' } },
    { name: 'ad_loaded', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Interstitial', latency_ms: 320 } },
    { name: 'ad_failed_to_load', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Rewarded', error_code: 3, error_message: 'No fill' } },
    { name: 'ad_impression', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Banner', screen: 'Home_Browser' } },
    { name: 'ad_clicked', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Banner', cpc_value: 0.15 } },
    { name: 'ad_dismissed', cat: 'ad', platform: 'Android App', sample: { ad_type: 'Interstitial', view_duration_sec: 5 } },

    // 4. Paywall & VIP (5 events)
    { name: 'paywall_view', cat: 'paywall', platform: 'Android / Web', sample: { trigger_reason: 'exceed_daily_limit_10_videos' } },
    { name: 'paywall_plan_selected', cat: 'paywall', platform: 'Android / Web', sample: { plan_id: 'plan_annual', plan_price: '$39.99/year' } },
    { name: 'paywall_cta_clicked', cat: 'paywall', platform: 'Android / Web', sample: { plan_id: 'plan_annual', payment_method: 'Google_Play_Billing' } },
    { name: 'paywall_restore_clicked', cat: 'paywall', platform: 'Android / Web', sample: { restore_status: 'checking_entitlements' } },
    { name: 'premium_feature_used', cat: 'paywall', platform: 'Android / Web', sample: { feature_name: '4k_ultra_download_multi_thread' } },

    // 5. Vòng đời & Crashlytics (8 events)
    { name: 'first_open', cat: 'lifecycle', platform: 'Android App', sample: { install_source: 'google_play', os_version: 'Android_14' } },
    { name: 'session_start', cat: 'lifecycle', platform: 'Android / Web', sample: { session_id: 'sess_' + Date.now(), user_type: 'anonymous' } },
    { name: 'app_update', cat: 'lifecycle', platform: 'Android App', sample: { previous_version: 219, current_version: 220 } },
    { name: 'os_update', cat: 'lifecycle', platform: 'Android App', sample: { previous_sdk: 33, current_sdk: 34 } },
    { name: 'settings_action_clicked', cat: 'lifecycle', platform: 'Android / Web', sample: { setting_key: 'app_theme', new_value: 'dark_glassmorphism' } },
    { name: 'app_clear_data', cat: 'lifecycle', platform: 'Android App', sample: { data_type: 'browser_cache_and_cookies', freed_mb: 142.5 } },
    { name: 'app_remove', cat: 'lifecycle', platform: 'Android App', sample: { retention_days: 14, uninstalled: false } },
    { name: 'app_exception', cat: 'crash', platform: 'Android (Crashlytics)', sample: { context_tag: 'ExoPlayer_Decoder', exception_class: 'MediaCodecVideoRendererException', error_message: 'Format unsupported fallback to software' } }
];

let firebaseEventsLog = [];

function getCategoryForEvent(name) {
    const item = ALL_28_EVENTS_SPEC.find(e => e.name === name);
    return item ? item.cat : 'screen';
}

function getDefaultPlatformForEvent(name) {
    const item = ALL_28_EVENTS_SPEC.find(e => e.name === name);
    return item ? item.platform : 'Android / Web';
}

function logFirebaseEvent(eventName, payload = {}, platform = null, isSilent = false) {
    const now = new Date();
    const timeStr = now.toTimeString().split(' ')[0] + '.' + String(now.getMilliseconds()).padStart(3, '0');
    const cat = getCategoryForEvent(eventName);
    const targetPlatform = platform || getDefaultPlatformForEvent(eventName);

    const eventEntry = {
        id: 'ev_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
        timestamp: timeStr,
        eventName: eventName,
        category: cat,
        payload: payload,
        platform: targetPlatform,
        status: '200 OK'
    };

    firebaseEventsLog.unshift(eventEntry);
    if (firebaseEventsLog.length > 100) {
        firebaseEventsLog.pop();
    }

    updateFirebaseUI(eventEntry);

    if (!isSilent) {
        showToast(`🔥 Firebase Event: ${eventName}`, 'success');
    }
}

function updateFirebaseUI(newEntry) {
    const countEl = document.getElementById('fb-total-events-count');
    if (countEl) {
        countEl.innerText = firebaseEventsLog.length;
    }

    const tbody = document.getElementById('fb-events-table-body');
    if (!tbody) return;

    const tr = document.createElement('tr');
    tr.style.animation = 'fadeIn 0.3s ease-out';

    const catBadgeClass = `fb-badge-cat ${newEntry.category}`;
    const payloadStr = JSON.stringify(newEntry.payload);

    tr.innerHTML = `
        <td style="font-family:'JetBrains Mono',monospace; font-size:12px; color:#94a3b8;">${newEntry.timestamp}</td>
        <td><strong class="fb-event-name">${newEntry.eventName}</strong></td>
        <td><span class="${catBadgeClass}">${newEntry.category}</span></td>
        <td><code class="fb-payload-code" title="${payloadStr}">${payloadStr}</code></td>
        <td><span class="fb-badge-platform">${newEntry.platform}</span></td>
        <td><span class="fb-badge-status">✓ Đồng Bộ</span></td>
    `;

    tbody.insertBefore(tr, tbody.firstChild);

    while (tbody.children.length > 50) {
        tbody.removeChild(tbody.lastChild);
    }
}

function renderAllFirebaseEvents() {
    const countEl = document.getElementById('fb-total-events-count');
    if (countEl) {
        countEl.innerText = firebaseEventsLog.length;
    }

    const tbody = document.getElementById('fb-events-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';
    firebaseEventsLog.forEach(ev => {
        const tr = document.createElement('tr');
        const catBadgeClass = `fb-badge-cat ${ev.category}`;
        const payloadStr = JSON.stringify(ev.payload);

        tr.innerHTML = `
            <td style="font-family:'JetBrains Mono',monospace; font-size:12px; color:#94a3b8;">${ev.timestamp}</td>
            <td><strong class="fb-event-name">${ev.eventName}</strong></td>
            <td><span class="${catBadgeClass}">${ev.category}</span></td>
            <td><code class="fb-payload-code" title="${payloadStr}">${payloadStr}</code></td>
            <td><span class="fb-badge-platform">${ev.platform}</span></td>
            <td><span class="fb-badge-status">✓ Đồng Bộ</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function initFirebaseMonitor() {
    // 1. Nạp sẵn một số sự kiện mẫu thực tế để dashboard không bị trống
    const initialSamples = [
        { name: 'first_open', sample: { install_source: 'google_play', os_version: 'Android_14' }, platform: 'Android App' },
        { name: 'session_start', sample: { session_id: 'sess_init_01', user_type: 'anonymous' }, platform: 'Android / Web' },
        { name: 'View_Home', sample: { screen_name: 'Home_Browser', screen_class: 'MainActivity' }, platform: 'Android / Web' },
        { name: 'ad_requested', sample: { ad_type: 'Banner', ad_unit_id: 'ca-app-pub-3940256099942544/6300978111' }, platform: 'Android App' },
        { name: 'ad_loaded', sample: { ad_type: 'Banner', latency_ms: 180 }, platform: 'Android App' },
        { name: 'ad_impression', sample: { ad_type: 'Banner', screen: 'Home_Browser' }, platform: 'Android App' },
        { name: 'Click_Search', sample: { search_query: 'https://youtube.com/watch?v=dQw4w9WgXcQ', source: 'home_bar' }, platform: 'Web Portal' }
    ];

    initialSamples.forEach((item, idx) => {
        const time = new Date(Date.now() - (initialSamples.length - idx) * 3000);
        const timeStr = time.toTimeString().split(' ')[0] + '.' + String(time.getMilliseconds()).padStart(3, '0');
        const cat = getCategoryForEvent(item.name);
        firebaseEventsLog.push({
            id: 'ev_init_' + idx,
            timestamp: timeStr,
            eventName: item.name,
            category: cat,
            payload: item.sample,
            platform: item.platform,
            status: '200 OK'
        });
    });

    renderAllFirebaseEvents();

    // 2. Gán sự kiện click cho từng nút test trong 28 events
    document.querySelectorAll('.btn-event-trigger').forEach(btn => {
        btn.addEventListener('click', () => {
            const evName = btn.getAttribute('data-event');
            const spec = ALL_28_EVENTS_SPEC.find(s => s.name === evName);
            const payload = spec ? spec.sample : { test: true };
            const platform = spec ? spec.platform : 'Test Trigger';
            logFirebaseEvent(evName, payload, platform, false);
        });
    });

    // 3. Nút kích hoạt tất cả 28 events mẫu tuần tự (sequential animation)
    const btnTriggerAll = document.getElementById('btn-fb-trigger-all');
    if (btnTriggerAll) {
        btnTriggerAll.addEventListener('click', () => {
            showToast('Đang phát liên tiếp 28 Sự Kiện chuẩn...', 'info');
            ALL_28_EVENTS_SPEC.forEach((spec, idx) => {
                setTimeout(() => {
                    logFirebaseEvent(spec.name, spec.sample, spec.platform, true);
                    if (idx === ALL_28_EVENTS_SPEC.length - 1) {
                        showToast('Đã phát thành công toàn bộ 28 Sự Kiện chuẩn!', 'success');
                    }
                }, idx * 60);
            });
        });
    }

    // 4. Nút xóa nhật ký
    const btnClearLogs = document.getElementById('btn-fb-clear-logs');
    if (btnClearLogs) {
        btnClearLogs.addEventListener('click', () => {
            firebaseEventsLog = [];
            renderAllFirebaseEvents();
            showToast('Đã xóa sạch nhật ký Firebase Event!', 'info');
        });
    }

    // 5. Gán sự kiện cho admin-tab-btn tab-firebase để cập nhật bảng
    const tabFbBtn = document.querySelector('.admin-tab-btn[data-tab="tab-firebase"]');
    if (tabFbBtn) {
        tabFbBtn.addEventListener('click', () => {
            renderAllFirebaseEvents();
        });
    }
}
