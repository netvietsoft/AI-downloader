package com.nextaitechnology.antidetect.feature.browser

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import java.net.URLEncoder
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextaitechnology.antidetect.MainActivity
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.channel.ChannelReelCandidate
import com.nextaitechnology.antidetect.core.channel.FavoriteChannelRepository
import com.nextaitechnology.antidetect.core.network.SnifferEngine
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MÃ n hÃ¬nh TrÃ¬nh duyá»‡t & Dashboard Trang chá»§ (HomeFragment)
 * Home Dashboard with Quick Access Social Shortcuts & In-App Web Browser with Anti-DeepLink Protection
 *
 * @author NextAI Technology Core Team
 */
class HomeFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var webProgressBar: ProgressBar
    private lateinit var etUrlInput: EditText
    private lateinit var btnGoUrl: View
    private lateinit var btnClearUrl: View
    private lateinit var scrollHomeContent: View
    private lateinit var btnPasteDetect: View
    private var btnFloatingChannelScan: TextView? = null
    private var layoutFavoriteChannelsList: LinearLayout? = null
    private var tvFavoriteChannelsHeader: TextView? = null
    private lateinit var favoriteChannelRepository: FavoriteChannelRepository
    private val discoveredChannelReels = LinkedHashMap<String, ChannelReelCandidate>()
    private var pendingAutoShowChannelDialog: Boolean = false

    private val snifferEngine = SnifferEngine()
    private var defaultMobileUserAgent: String = SnifferEngine.MOBILE_USER_AGENT
    @Volatile
    private var currentWebTitle: String? = null
    private var pendingUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoriteChannelRepository = FavoriteChannelRepository(requireContext())
        webView = view.findViewById(R.id.web_view)
        webProgressBar = view.findViewById(R.id.web_progress_bar)
        etUrlInput = view.findViewById(R.id.et_url_input)
        btnGoUrl = view.findViewById(R.id.btn_go_url)
        btnClearUrl = view.findViewById(R.id.btn_clear_url)
        scrollHomeContent = view.findViewById(R.id.scroll_home_content)
        btnPasteDetect = view.findViewById(R.id.btn_paste_detect)
        btnFloatingChannelScan = view.findViewById(R.id.btn_floating_channel_scan)
        layoutFavoriteChannelsList = view.findViewById(R.id.layout_favorite_channels_list)
        tvFavoriteChannelsHeader = view.findViewById(R.id.tv_favorite_channels_header)

        setupWebView()
        setupSearchBar()
        setupChannelDownloaderCard(view)
        renderFavoriteChannelsList()

        pendingUrl?.let {
            val toLoad = it
            pendingUrl = null
            handleUserUrl(toLoad)
        }
    }

    private fun setupWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(AndroidSnifferBridge(), "AndroidSniffer")
            settings.allowFileAccess = false
            // Chặn JavaScript tự động mở cửa sổ mới khi chưa có tương tác & vô hiệu hóa đa cửa sổ
            // Disable automatic popups from JavaScript & disable multiple windows
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowContentAccess = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            // Bật Cookie & Third-Party Cookies chuẩn trình duyệt để Google Search, YouTube, Facebook lưu phiên hợp lệ
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
            } catch (_: Exception) {
            }

            // Đồng bộ User-Agent thực tế của Chromium hệ thống (loại bỏ cờ "; wv" để khớp 100% với Sec-CH-UA Client Hints, tránh bị Google Search chặn nhầm là bot)
            val defaultUa = try {
                WebSettings.getDefaultUserAgent(requireContext())
                    .replace("; wv)", ")")
                    .replace("Version/4.0 ", "")
            } catch (_: Exception) {
                SnifferEngine.MOBILE_USER_AGENT
            }
            defaultMobileUserAgent = defaultUa
            settings.userAgentString = defaultUa
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.defaultTextEncodingName = "UTF-8"
            WebView.setWebContentsDebuggingEnabled(com.nextaitechnology.antidetect.BuildConfig.DEBUG)

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // [VI] Chặn triệt để yêu cầu tạo popup/cửa sổ mới, bảo vệ trang web gốc không bị đè
                    // [EN] Block all requests to create new windows/popups, keeping original page protected
                    return false
                }
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrEmpty() && !title.startsWith("http")) {
                        currentWebTitle = title
                    }
                }
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        webProgressBar.visibility = View.VISIBLE
                        webProgressBar.progress = newProgress
                    } else {
                        webProgressBar.visibility = View.GONE
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null) {
                        etUrlInput.setText(url)
                        btnClearUrl.visibility = if (url.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    (activity as? MainActivity)?.resetDetectionState()
                    // [VI] Tiêm script vô hiệu hóa popup, Mobile Responsive và bộ bắt luồng Kênh/Reels (Fetch/XHR/DOM Hook) ngay khi bắt đầu tải trang
                    view?.evaluateJavascript(ANTI_POPUP_JS, null)
                    view?.evaluateJavascript(MOBILE_RESPONSIVE_JS, null)
                    view?.evaluateJavascript(CHANNEL_HARVESTER_JS, null)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: ""
                    return handleUrlOverride(view, url)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    return handleUrlOverride(view, url ?: "")
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    inspectUrlForVideo(url, currentWebTitle)
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (url != null) {
                        inspectUrlForVideo(url, currentWebTitle)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        CookieManager.getInstance().flush()
                    } catch (_: Exception) {
                    }
                    val pageTitle = view?.title ?: ""
                    if (pageTitle.isNotEmpty() && !pageTitle.startsWith("http")) { currentWebTitle = pageTitle }
                    if (url != null) {
                        (activity as? MainActivity)?.updateCurrentTabInfo(url, if (pageTitle.isNotEmpty() && !pageTitle.startsWith("http")) pageTitle else (currentWebTitle ?: "Web Page"))
                    }
                    // [VI] Tái củng cố script chặn popup, responsive & channel harvester sau khi toàn bộ DOM & scripts bên thứ 3 đã nạp
                    view?.evaluateJavascript(ANTI_POPUP_JS, null)
                    view?.evaluateJavascript(MOBILE_RESPONSIVE_JS, null)
                    view?.evaluateJavascript(CHANNEL_HARVESTER_JS, null)

                    // 1. Phân tích trực tiếp toàn bộ DOM HTML đã nạp trong WebView (Bảo đảm vượt tường lửa ISP)
                    if (url != null && (url.contains("instagram.com") || url.contains("facebook.com") ||
                            url.contains("fb.watch") || url.contains("tiktok.com") ||
                            url.contains("xnxx.com") || url.contains("xnxx2.com") ||
                            url.contains("xvideos.com") || url.contains("pornhub.com") ||
                            url.contains("youtube.com") || url.contains("youtu.be") ||
                            url.contains("vimeo.com") || url.contains("dailymotion.com") ||
                            com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.isMonetizedDomain(url))) {
                        view?.evaluateJavascript("(function() { if (window.__harvestChannelDom) window.__harvestChannelDom(); return document.documentElement.outerHTML; })();") { rawHtmlJson ->
                            if (!rawHtmlJson.isNullOrBlank() && rawHtmlJson != "null") {
                                lifecycleScope.launch {
                                    try {
                                        val cleanHtml = org.json.JSONTokener(rawHtmlJson).nextValue().toString()
                                        val videoInfo = snifferEngine.extractVideoFromUrl(url, cleanHtml)
                                        if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                                            activity?.runOnUiThread {
                                                (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                                            }
                                        }
                                        val channelReels = FavoriteChannelRepository.harvestChannelReelsFromHtml(url, cleanHtml)
                                        activity?.runOnUiThread {
                                            channelReels.forEach { mergeDiscoveredChannelReel(it) }
                                            updateFloatingChannelScanButton(url)
                                            if (pendingAutoShowChannelDialog) {
                                                pendingAutoShowChannelDialog = false
                                                pollForChannelReelsAndShowDialog(url, 0)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.w("NextAI_Sniffer", "Lỗi phân tích DOM HTML", e)
                                        if (pendingAutoShowChannelDialog) {
                                            activity?.runOnUiThread {
                                                pendingAutoShowChannelDialog = false
                                                pollForChannelReelsAndShowDialog(url, 0)
                                            }
                                        }
                                    }
                                }
                            } else if (pendingAutoShowChannelDialog) {
                                pendingAutoShowChannelDialog = false
                                pollForChannelReelsAndShowDialog(url, 0)
                            }
                        }
                    }

                    // 2. Tiêm Javascript quét tìm video streams đa tầng (HTML5 player, script blocks, media tags, prototypes)
                    view?.evaluateJavascript("""
                        (function() {
                            var checkCount = 0;
                            var snifferTimer = setInterval(function() {
                                checkCount++;
                                try {
                                    var stream = '';
                                    var title = '';

                                    // A. Quét các thẻ <script> tìm hàm khởi tạo player XNXX / XVideos / Pornhub
                                    var scripts = document.getElementsByTagName('script');
                                    for (var i = 0; i < scripts.length; i++) {
                                        var scText = scripts[i].textContent || scripts[i].innerText || '';
                                        if (scText.indexOf('setVideoHLS') !== -1 || scText.indexOf('setVideoUrlHigh') !== -1 || scText.indexOf('video_url_high') !== -1) {
                                            var mHls = scText.match(/(?:setVideoHLS|video_hls)\s*[\(=]\s*['"]([^'"]+)['"]/);
                                            var mHigh = scText.match(/(?:setVideoUrlHigh|video_url_high)\s*[\(=]\s*['"]([^'"]+)['"]/);
                                            var mLow = scText.match(/(?:setVideoUrlLow|video_url_low)\s*[\(=]\s*['"]([^'"]+)['"]/);
                                            var mTitle = scText.match(/(?:setVideoTitle|video_title)\s*[\(=]\s*['"]([^'"]+)['"]/);
                                            if (mTitle && mTitle[1]) title = mTitle[1];
                                            if (mHigh && mHigh[1]) stream = mHigh[1];
                                            else if (mHls && mHls[1]) stream = mHls[1];
                                            else if (mLow && mLow[1]) stream = mLow[1];
                                            if (stream) break;
                                        }
                                        if (scText.indexOf('mediaDefinitions') !== -1) {
                                            var mDef = scText.match(/mediaDefinitions\s*[:=]\s*(\[\s*\{.*?\}\s*\])/);
                                            if (mDef && mDef[1]) {
                                                try {
                                                    var defs = JSON.parse(mDef[1]);
                                                    for (var d = 0; d < defs.length; d++) {
                                                        if (defs[d].videoUrl && defs[d].videoUrl.startsWith('http')) {
                                                            stream = defs[d].videoUrl;
                                                            break;
                                                        }
                                                    }
                                                } catch(e) {}
                                            }
                                            if (stream) break;
                                        }
                                    }

                                    // B. Quét đối tượng toàn cục xplayer & initials
                                    if (!stream && window.xplayer && window.xplayer.core && window.xplayer.core.options) {
                                        var opts = window.xplayer.core.options;
                                        var s = opts.sources;
                                        if (s) {
                                            if (s.hls && s.hls.h264 && s.hls.h264.url) stream = s.hls.h264.url;
                                            else if (s.hls && s.hls.av1 && s.hls.av1.url) stream = s.hls.av1.url;
                                            else if (s.standard && s.standard.h264 && s.standard.h264.length > 0) {
                                                stream = s.standard.h264[s.standard.h264.length - 1].url;
                                            }
                                        }
                                        if (opts.videoInfo && opts.videoInfo.title) {
                                            title = opts.videoInfo.title;
                                        }
                                    }
                                    if (!stream && window.initials) {
                                        var model = window.initials.videoModel || window.initials.xplayerSettings;
                                        if (model && model.sources) {
                                            var ms = model.sources;
                                            if (ms.hls && typeof ms.hls === 'string' && ms.hls.startsWith('http')) stream = ms.hls;
                                            else if (ms.hls && ms.hls.url && ms.hls.url.startsWith('http')) stream = ms.hls.url;
                                        }
                                    }

                                    // C. Quét thẻ meta og:video hoặc video tags (Instagram, Facebook, etc.)
                                    if (!stream) {
                                        var metaVid = document.querySelector('meta[property="og:video"]') ||
                                                      document.querySelector('meta[property="og:video:secure_url"]');
                                        if (metaVid && metaVid.content && metaVid.content.startsWith('http')) {
                                            stream = metaVid.content;
                                        }
                                    }

                                    // D. Quét thẻ <video> và <source> trong DOM
                                    if (!stream) {
                                        var vids = document.querySelectorAll('video, video source');
                                        for (var j = 0; j < vids.length; j++) {
                                            var vSrc = vids[j].src || vids[j].getAttribute('src') || '';
                                            if (vSrc && vSrc.startsWith('http')) {
                                                if (vSrc.indexOf('.mp4') !== -1 || vSrc.indexOf('.m3u8') !== -1 ||
                                                    vSrc.indexOf('cdninstagram.com') !== -1 || vSrc.indexOf('fbcdn.net') !== -1 ||
                                                    vSrc.indexOf('tiktokcdn.com') !== -1) {
                                                    stream = vSrc;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    // E. Hook prototype play & event listeners để bắt luồng khi video phát
                                    if (!window.__videoPlayHooked) {
                                        window.__videoPlayHooked = true;
                                        var origPlay = HTMLMediaElement.prototype.play;
                                        HTMLMediaElement.prototype.play = function() {
                                            var ps = this.currentSrc || this.src || '';
                                            if (ps && ps.startsWith('http') && window.AndroidSniffer) {
                                                window.AndroidSniffer.onVideoFound(ps, document.title);
                                            }
                                            return origPlay.apply(this, arguments);
                                        };

                                        var allVids = document.querySelectorAll('video');
                                        for (var vIdx = 0; vIdx < allVids.length; vIdx++) {
                                            allVids[vIdx].addEventListener('play', function() {
                                                var s = this.currentSrc || this.src || '';
                                                if (s && s.startsWith('http') && window.AndroidSniffer) {
                                                    window.AndroidSniffer.onVideoFound(s, document.title);
                                                }
                                            });
                                        }
                                    }

                                    if (stream && stream.startsWith('http')) {
                                        clearInterval(snifferTimer);
                                        if (window.AndroidSniffer) {
                                            window.AndroidSniffer.onVideoFound(stream, title || document.title);
                                        }
                                        return;
                                    }
                                } catch(e) {}
                                if (checkCount >= 25) {
                                    clearInterval(snifferTimer);
                                }
                            }, 500);
                        })();
                    """.trimIndent(), null)
                }
            }
        }
    }

    fun getCurrentUrl(): String? {
        val inputUrl = if (::etUrlInput.isInitialized) etUrlInput.text.toString().trim().takeIf { it.startsWith("http") } else null
        return inputUrl ?: if (::webView.isInitialized) webView.url else null
    }

    fun getPageTitle(): String? {
        return currentWebTitle ?: if (::webView.isInitialized) webView.title else null
    }

    private fun inspectUrlForVideo(url: String, pageTitle: String? = null) {
        val lower = url.lowercase()
        val isMetaVideo = (lower.contains("cdninstagram.com") || lower.contains(".fbcdn.net"))

        // Bỏ qua các phân đoạn chunk byte range, segment files, init files và static assets
        // Filter out chunk segments, range requests, init segments, tracking & non-video assets
        if (!isMetaVideo && (lower.contains("bytestart") || lower.contains("byteend"))) {
            return
        }
        if (lower.contains(".m4s") || lower.contains(".ts") || lower.contains("seg-") ||
            lower.contains("-seg") || lower.contains("/frag/") || lower.contains("init-") ||
            lower.contains("init.mp4") || lower.contains(".aac")) {
            return
        }

        // Bỏ qua analytics, trackers và ads
        if (lower.contains("google-analytics") || lower.contains("doubleclick") ||
            lower.contains("googlesyndication") || lower.contains("facebook.com/tr") ||
            lower.contains("telemetry") || lower.contains("beacon") ||
            lower.contains("modusygunaciro.com") || lower.contains("448x250") ||
            lower.contains("250x250") || lower.contains("160x120") || lower.contains("320x180") ||
            lower.contains("/preview/") || lower.contains("preview.mp4") || lower.contains("preview.m3u8") ||
            lower.contains("thumb.mp4") || lower.contains("thumb.m3u8") || lower.contains(".sprite.") ||
            lower.contains("/trailer") || lower.contains("_trailer")) {
            return
        }

        // QUY TẮC CỐT LÕI: TUYỆT ĐỐI KHÔNG BẮT GÓI ĐIỀU KHIỂN SABR/UMP HOẶC CHUNK RANGE CỦA YOUTUBE
        // Raw googlevideo.com URLs with sabr/ump/alr/range contain only 31-byte text error packets
        if (lower.contains("googlevideo.com") && (lower.contains("alr=yes") || lower.contains("sabr=1") ||
            lower.contains("range=") || lower.contains("live=1") || lower.contains("ump=1"))) {
            return
        }

        val isVideoUrl = (lower.contains(".m3u8") || lower.contains(".mpd") ||
            (lower.contains(".mp4") && !lower.contains(".m4s")) ||
            (isMetaVideo && (lower.contains(".mp4") || lower.contains("/v/") || lower.contains("/o1/v/") || lower.contains("oe="))) ||
            (lower.contains("tiktokcdn.com") && lower.contains(".mp4")) ||
            (lower.contains("video.twimg.com") && (lower.contains(".mp4") || lower.contains(".m3u8"))) ||
            (lower.contains("v.redd.it") && (lower.contains(".mp4") || lower.contains(".m3u8"))) ||
            (lower.contains("vimeocdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8"))) ||
            (lower.contains("dmcdn.net") && (lower.contains(".mp4") || lower.contains(".m3u8"))) ||
            (lower.contains("xhcdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/"))) ||
            (lower.contains("xvideos-cdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/"))) ||
            (lower.contains("phncdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/")))) &&
            !lower.contains(".jpg") && !lower.contains(".png") && !lower.contains(".webp") &&
            !lower.contains(".js") && !lower.contains(".css") && !lower.contains(".svg") &&
            !lower.contains(".woff") && !lower.contains(".woff2") && !lower.contains(".ttf") &&
            !lower.contains(".gif")

        if (isVideoUrl) {
            var targetMediaUrl = url
            if (isMetaVideo) {
                targetMediaUrl = targetMediaUrl
                    .replace(Regex("&?bytestart=\\d+"), "")
                    .replace(Regex("&?byteend=\\d+"), "")
            }
            val sanitized = SnifferEngine.sanitizeMediaUrl(targetMediaUrl)
            val cleanTitle = pageTitle?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("http") }
            activity?.runOnUiThread {
                (activity as? MainActivity)?.notifyVideoDetected(sanitized, cleanTitle)
            }
        }
    }

    private fun setupSearchBar() {
        // Lắng nghe thay đổi văn bản để hiển thị/ẩn nút '✕' xóa nhanh
        // Listen to text changes to show/hide the clear '✕' button
        etUrlInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearUrl.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Bấm nút '✕' để xóa toàn bộ URL/từ khóa và focus lại ô nhập
        // Tap '✕' button to clear URL/keyword and refocus input
        btnClearUrl.setOnClickListener {
            etUrlInput.text?.clear()
            btnClearUrl.visibility = View.GONE
            etUrlInput.requestFocus()
        }

        // Bấm nút SEARCH hoặc nhấn Enter/Go trên bàn phím ảo
        // Tap SEARCH button or press Enter/Go on soft keyboard
        btnGoUrl.setOnClickListener {
            val input = etUrlInput.text.toString().trim()
            if (input.isNotEmpty()) {
                FirebaseAnalyticsManager.logClickSearch(input, "search_button")
            }
            handleUserUrl(input)
            hideKeyboard()
        }

        etUrlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etUrlInput.text.toString().trim()
                if (input.isNotEmpty()) {
                    FirebaseAnalyticsManager.logClickSearch(input, "keyboard_enter")
                }
                handleUserUrl(input)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        btnPasteDetect.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (clipText.startsWith("http://", ignoreCase = true) || clipText.startsWith("https://", ignoreCase = true)) {
                FirebaseAnalyticsManager.logClickSearch(clipText, "paste_detect_button")
                etUrlInput.setText(clipText)
                handleUserUrl(clipText)
                Toast.makeText(context, getString(R.string.toast_link_detected, clipText), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(etUrlInput.windowToken, 0)
    }

    /**
     * Xử lý liên kết hoặc từ khóa tìm kiếm:
     * - Mặc định công cụ tìm kiếm Google nếu nhập từ khóa, tên website hoặc nội dung cần tìm.
     * - Tự động duyệt web trực tiếp nếu nhập URL hợp lệ.
     *
     * Handle input query or URL:
     * - Defaults to Google Search if the user enters keywords, website names, or search terms.
     * - Directly navigates to site if valid URL is provided.
     */
    fun handleUserUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            loadUrl("https://www.google.com")
            return
        }

        if (!::webView.isInitialized) {
            pendingUrl = rawInput
            return
        }

        val targetUrl: String = when {
            trimmed.startsWith("fb://fullscreen_video/") -> {
                val vid = trimmed.substringAfter("fullscreen_video/").substringBefore('?')
                "https://www.facebook.com/reel/" + vid
            }
            trimmed.startsWith("@") && !trimmed.contains(" ") -> {
                "https://www.youtube.com/$trimmed"
            }
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                trimmed
            }
            // Nếu có khoảng trắng hoặc không có dấu chấm phân cách -> Từ khóa tìm kiếm Google
            trimmed.contains(" ") || !trimmed.contains(".") || trimmed.endsWith(".") -> {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            }
            // Nếu là định dạng domain hợp lệ (vd: facebook.com, xhamster.desi) -> mở trực tiếp
            android.util.Patterns.WEB_URL.matcher("https://" + trimmed).matches() -> {
                "https://" + trimmed
            }
            else -> {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            }
        }

        // Nếu là liên kết YouTube, Instagram, Facebook, Vimeo hoặc thuộc 1822+ Monetized Domains -> Trích xuất trực tiếp song song
        if (targetUrl.contains("youtube.com") || targetUrl.contains("youtu.be") ||
            targetUrl.contains("instagram.com") ||
            targetUrl.contains("facebook.com") || targetUrl.contains("fb.watch") ||
            targetUrl.contains("vimeo.com") ||
            com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.isMonetizedDomain(targetUrl)) {
            extractVideoDirectly(targetUrl)
        }

        loadUrl(targetUrl)
    }

    fun extractVideoDirectly(url: String) {
        val platformName = when {
            url.contains("youtube.com") || url.contains("youtu.be") -> "YouTube"
            url.contains("instagram.com") -> "Instagram"
            url.contains("vimeo.com") -> "Vimeo"
            url.contains("facebook") || url.contains("fb.watch") -> "Facebook"
            url.contains("xnxx") -> "XNXX"
            url.contains("xvideos") -> "XVideos"
            url.contains("pornhub") -> "Pornhub"
            else -> com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.extractHost(url).ifEmpty { "Video" }
        }
        val safeContext = context ?: activity?.applicationContext
        if (safeContext != null) {
            Toast.makeText(safeContext, getString(R.string.toast_extracting_video, platformName), Toast.LENGTH_SHORT).show()
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val videoInfo = snifferEngine.extractVideoFromUrl(url)
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }


    fun loadUrl(url: String) {
        scrollHomeContent.visibility = View.GONE
        webView.visibility = View.VISIBLE
        etUrlInput.setText(url)
        discoveredChannelReels.clear()
        updateFloatingChannelScanButton(url)
        (activity as? MainActivity)?.resetDetectionState()

        val lower = url.lowercase()
        val useDesktopChannelMode = pendingAutoShowChannelDialog ||
            lower.contains("instagram.com") ||
            lower.contains("facebook.com") ||
            lower.contains("fb.watch") ||
            (lower.contains("youtube.com") && (lower.contains("/@") || lower.contains("/shorts") || lower.contains("/channel/") || lower.contains("/videos")))

        if (useDesktopChannelMode) {
            webView.settings.userAgentString = SnifferEngine.DESKTOP_USER_AGENT
            val navHeaders = mapOf(
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "none",
                "Sec-Fetch-User" to "?1",
                "Upgrade-Insecure-Requests" to "1"
            )
            webView.loadUrl(url, navHeaders)
        } else {
            webView.settings.userAgentString = defaultMobileUserAgent
            webView.loadUrl(url)
        }
    }

    /**
     * Đặt lại trình duyệt về Dashboard Trang Chủ và giải phóng tài nguyên trang web
     */
    fun resetToHomeDashboard() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.visibility = View.GONE
        }
        btnFloatingChannelScan?.visibility = View.GONE
        discoveredChannelReels.clear()
        scrollHomeContent.visibility = View.VISIBLE
        renderFavoriteChannelsList()
        etUrlInput.setText("")
        btnClearUrl.visibility = View.GONE
        (activity as? MainActivity)?.resetDetectionState()
    }

    fun canGoBack(): Boolean {
        return if (::webView.isInitialized && webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
            true
        } else if (::webView.isInitialized && webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            btnFloatingChannelScan?.visibility = View.GONE
            scrollHomeContent.visibility = View.VISIBLE
            renderFavoriteChannelsList()
            etUrlInput.setText("")
            (activity as? MainActivity)?.resetDetectionState()
            true
        } else {
            false
        }
    }

    private fun setupChannelDownloaderCard(root: View) {
        root.findViewById<View>(R.id.chip_sample_fb)?.setOnClickListener {
            etUrlInput.setText("https://www.facebook.com/samfamily.6826/reels/")
            btnClearUrl.visibility = View.VISIBLE
        }
        root.findViewById<View>(R.id.chip_sample_ig)?.setOnClickListener {
            etUrlInput.setText("https://www.instagram.com/instagram/reels/")
            btnClearUrl.visibility = View.VISIBLE
        }
        root.findViewById<View>(R.id.chip_sample_yt)?.setOnClickListener {
            etUrlInput.setText("https://www.youtube.com/@YouTube/shorts")
            btnClearUrl.visibility = View.VISIBLE
        }

        root.findViewById<View>(R.id.btn_scan_channel)?.setOnClickListener {
            val raw = etUrlInput.text.toString().trim()
            if (raw.isEmpty()) {
                Toast.makeText(context, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startChannelScanFlow(raw)
        }

        root.findViewById<View>(R.id.btn_save_favorite_channel)?.setOnClickListener {
            val raw = etUrlInput.text.toString().trim()
            if (raw.isEmpty()) {
                Toast.makeText(context, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val fav = favoriteChannelRepository.addOrUpdateFavorite(raw)
                renderFavoriteChannelsList()
                Toast.makeText(context, getString(R.string.toast_channel_saved, fav.title), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "URL không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }

        btnFloatingChannelScan?.setOnClickListener {
            val currentUrl = getCurrentUrl() ?: return@setOnClickListener
            harvestCurrentWebViewAndShowChannelDialog(currentUrl)
        }
    }

    private fun updateFloatingChannelScanButton(url: String?) {
        val u = (url ?: "").lowercase()
        val isSupportedChannel = u.contains("facebook.com") || u.contains("instagram.com") || u.contains("youtube.com")
        if (webView.visibility == View.VISIBLE && isSupportedChannel) {
            btnFloatingChannelScan?.visibility = View.VISIBLE
            btnFloatingChannelScan?.text = getString(R.string.btn_floating_scan_channel, discoveredChannelReels.size)
        } else {
            btnFloatingChannelScan?.visibility = View.GONE
        }
    }

    private fun startChannelScanFlow(rawInput: String) {
        try {
            val target = FavoriteChannelRepository.normalizeChannelInput(rawInput)
            pendingAutoShowChannelDialog = true
            hideKeyboard()
            loadUrl(target.reelsTabUrl)
            lifecycleScope.launch(Dispatchers.IO) {
                val directReels = snifferEngine.scanChannelReels(target.reelsTabUrl)
                if (directReels.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        directReels.forEach { mergeDiscoveredChannelReel(it) }
                        updateFloatingChannelScanButton(target.reelsTabUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "URL không hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mergeDiscoveredChannelReel(incoming: ChannelReelCandidate) {
        if (incoming.reelId.isBlank()) return
        val existing = discoveredChannelReels[incoming.reelId]
        if (existing == null) {
            discoveredChannelReels[incoming.reelId] = incoming
        } else {
            if (existing.hdUrl.isNullOrBlank() && !incoming.hdUrl.isNullOrBlank()) {
                existing.hdUrl = incoming.hdUrl
            }
            if (existing.sdUrl.isNullOrBlank() && !incoming.sdUrl.isNullOrBlank()) {
                existing.sdUrl = incoming.sdUrl
            }
            if (existing.dashVideoUrl.isNullOrBlank() && !incoming.dashVideoUrl.isNullOrBlank()) {
                existing.dashVideoUrl = incoming.dashVideoUrl
            }
            if (existing.caption.isBlank() && incoming.caption.isNotBlank()) {
                val meta = FavoriteChannelRepository.extractCaptionTitleAndHashtags(incoming.caption)
                existing.caption = meta.caption
                existing.title = meta.title
                existing.hashtags = meta.hashtags
            }
        }
    }

    /**
     * Đợi trang SPA (Instagram / Facebook / YouTube) render xong lưới Reels/Shorts qua XHR/GraphQL
     * rồi mới mở hộp thoại kết quả quét kênh (tránh mở sớm ở 0ms khi DOM chưa render kịp).
     */
    private fun pollForChannelReelsAndShowDialog(url: String, attempt: Int) {
        if (!isAdded) return
        webView.evaluateJavascript("(function() { if (window.__harvestChannelDom) window.__harvestChannelDom(); return document.documentElement.outerHTML; })();") { rawHtmlJson ->
            lifecycleScope.launch {
                if (!rawHtmlJson.isNullOrBlank() && rawHtmlJson != "null") {
                    try {
                        val cleanHtml = withContext(Dispatchers.Default) {
                            org.json.JSONTokener(rawHtmlJson).nextValue().toString()
                        }
                        val harvested = withContext(Dispatchers.Default) {
                            FavoriteChannelRepository.harvestChannelReelsFromHtml(url, cleanHtml)
                        }
                        harvested.forEach { mergeDiscoveredChannelReel(it) }
                        updateFloatingChannelScanButton(url)
                    } catch (_: Exception) {
                    }
                }

                // Nếu đã tìm thấy danh sách Reels (sau ít nhất 1 nhịp chờ để gom đủ lưới) hoặc đã hết 7 nhịp chờ (~4.5s) -> mở Dialog
                if ((discoveredChannelReels.size >= 3 && attempt >= 1) ||
                    (discoveredChannelReels.isNotEmpty() && attempt >= 3) ||
                    attempt >= 6
                ) {
                    showChannelReelsBatchDialog(url)
                } else {
                    if (attempt == 2) {
                        webView.evaluateJavascript("window.scrollBy(0, 650);", null)
                    }
                    webView.postDelayed({
                        if (isAdded) {
                            pollForChannelReelsAndShowDialog(url, attempt + 1)
                        }
                    }, 700L)
                }
            }
        }
    }

    private fun harvestCurrentWebViewAndShowChannelDialog(url: String) {
        webView.evaluateJavascript("(function() { if (window.__harvestChannelDom) window.__harvestChannelDom(); return document.documentElement.outerHTML; })();") { rawHtmlJson ->
            if (!rawHtmlJson.isNullOrBlank() && rawHtmlJson != "null") {
                lifecycleScope.launch {
                    try {
                        val cleanHtml = withContext(Dispatchers.Default) {
                            org.json.JSONTokener(rawHtmlJson).nextValue().toString()
                        }
                        val harvested = withContext(Dispatchers.Default) {
                            FavoriteChannelRepository.harvestChannelReelsFromHtml(url, cleanHtml)
                        }
                        harvested.forEach { mergeDiscoveredChannelReel(it) }
                        updateFloatingChannelScanButton(url)
                    } catch (_: Exception) {
                    }
                    showChannelReelsBatchDialog(url)
                }
            } else {
                showChannelReelsBatchDialog(url)
            }
        }
    }

    private fun showChannelReelsBatchDialog(currentUrl: String) {
        val ctx = context ?: return
        val target = try {
            FavoriteChannelRepository.normalizeChannelInput(currentUrl)
        } catch (e: Exception) {
            return
        }

        val allList = discoveredChannelReels.values.toList()
        val alreadyDownloaded = favoriteChannelRepository.getDownloadedIdsForSlug(target.pageSlug)
        val newList = allList.filter { !alreadyDownloaded.contains(it.reelId) }
        val existingCount = allList.size - newList.size

        val dialogTitle = getString(R.string.channel_scan_dialog_title, target.pageSlug)
        val summaryMsg = buildString {
            append(getString(R.string.channel_scan_summary, allList.size, newList.size, existingCount))
            append("\n• URL: ${target.reelsTabUrl}")
            if (newList.isNotEmpty()) {
                append("\n\n")
                newList.take(8).forEachIndexed { idx, item ->
                    val fileName = FavoriteChannelRepository.buildVideoFileName(item.reelId, item.caption, item.title, item.hashtags)
                    val streamTag = if (item.bestStreamUrl != null) "HD Ready" else "Auto-Resolve"
                    append("${idx + 1}. $fileName [$streamTag]\n")
                }
                if (newList.size > 8) {
                    append("... (+${newList.size - 8} video khác)")
                }
            }
        }

        val builder = AlertDialog.Builder(ctx)
            .setTitle(dialogTitle)
            .setMessage(summaryMsg)
            .setNeutralButton(getString(R.string.btn_scroll_scan_more)) { _, _ ->
                webView.evaluateJavascript("window.scrollBy(0, Math.max(1200, window.innerHeight * 1.6));", null)
                webView.postDelayed({
                    if (isAdded) harvestCurrentWebViewAndShowChannelDialog(currentUrl)
                }, 1600L)
            }
            .setNegativeButton(getString(R.string.btn_save_favorite_channel)) { _, _ ->
                val fav = favoriteChannelRepository.addOrUpdateFavorite(target.inputUrl, getPageTitle())
                renderFavoriteChannelsList()
                Toast.makeText(ctx, getString(R.string.toast_channel_saved, fav.title), Toast.LENGTH_SHORT).show()
            }

        if (newList.isNotEmpty()) {
            builder.setPositiveButton(getString(R.string.btn_download_all_new, newList.size)) { _, _ ->
                batchEnqueueChannelReels(target.pageSlug, newList)
            }
        } else {
            builder.setPositiveButton(getString(R.string.btn_ok), null)
        }

        builder.show()
    }

    private fun batchEnqueueChannelReels(pageSlug: String, candidates: List<ChannelReelCandidate>) {
        val mainAct = activity as? MainActivity ?: return
        val enqueuedIds = mutableListOf<String>()

        Toast.makeText(
            mainAct,
            getString(R.string.toast_channel_batch_started, candidates.size),
            Toast.LENGTH_LONG
        ).show()

        lifecycleScope.launch(Dispatchers.IO) {
            for (cand in candidates) {
                var streamUrl = cand.bestStreamUrl
                var fileName = FavoriteChannelRepository.buildVideoFileName(cand.reelId, cand.caption, cand.title, cand.hashtags)
                val referer = when (cand.platform) {
                    "instagram" -> "https://www.instagram.com/"
                    "youtube" -> "https://www.youtube.com/"
                    else -> "https://www.facebook.com/"
                }

                if (streamUrl.isNullOrBlank()) {
                    val info = snifferEngine.extractVideoFromUrl(cand.reelUrl)
                    streamUrl = info?.qualities?.firstOrNull()?.downloadUrl
                    if (!info?.title.isNullOrBlank()) {
                        fileName = info!!.title
                    }
                }

                if (!streamUrl.isNullOrBlank()) {
                    enqueuedIds.add(cand.reelId)
                    withContext(Dispatchers.Main) {
                        mainAct.downloadEngine.enqueueDownload(
                            "${pageSlug}_$fileName",
                            streamUrl,
                            "1080p Full HD",
                            "mp4",
                            referer
                        )
                    }
                }
            }
            favoriteChannelRepository.recordChannelScanResult(pageSlug, discoveredChannelReels.size, enqueuedIds)
            withContext(Dispatchers.Main) {
                renderFavoriteChannelsList()
            }
        }
    }

    private fun renderFavoriteChannelsList() {
        val container = layoutFavoriteChannelsList ?: return
        val ctx = context ?: return
        container.removeAllViews()

        val favorites = favoriteChannelRepository.getFavorites()
        tvFavoriteChannelsHeader?.text = getString(R.string.home_favorites_header, favorites.size)

        if (favorites.isEmpty()) {
            val emptyTv = TextView(ctx).apply {
                text = getString(R.string.home_favorites_empty)
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, 6, 0, 6)
            }
            container.addView(emptyTv)
            return
        }

        val density = ctx.resources.displayMetrics.density
        for (fav in favorites) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_plan_unselected)
                val padH = (10 * density).toInt()
                val padV = (7 * density).toInt()
                setPadding(padH, padV, padH, padV)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = (6 * density).toInt()
                layoutParams = lp
            }

            val badgeIcon = when (fav.platform) {
                "instagram" -> "📷 IG"
                "youtube" -> "▶ YT"
                else -> "📘 FB"
            }

            val infoTv = TextView(ctx).apply {
                text = "$badgeIcon • ${fav.title} (${fav.downloadedReelIds.size})"
                setTextColor(Color.WHITE)
                textSize = 11.5f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val scanBtn = TextView(ctx).apply {
                text = getString(R.string.btn_scan_new)
                setTextColor(Color.parseColor("#46CC6B"))
                textSize = 11f
                maxLines = 1
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding((10 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                setOnClickListener {
                    etUrlInput.setText(fav.reelsTabUrl)
                    startChannelScanFlow(fav.reelsTabUrl)
                }
            }

            val delBtn = TextView(ctx).apply {
                text = "✕"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                setPadding((8 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
                setOnClickListener {
                    favoriteChannelRepository.removeFavorite(fav.id)
                    renderFavoriteChannelsList()
                }
            }

            row.addView(infoTv)
            row.addView(scanBtn)
            row.addView(delBtn)
            container.addView(row)
        }
    }

    /**
     * Bridge JavaScript hai chiều giao tiếp trực tiếp với player engine của trang web (xplayer / video element)
     * Bidirectional JS Bridge communicating directly with web page video player engines
     */
    inner class AndroidSnifferBridge {
        @JavascriptInterface
        fun onVideoFound(streamUrl: String, title: String?) {
            activity?.runOnUiThread {
                if (streamUrl.isNotEmpty() && streamUrl.startsWith("http")) {
                    val sanitized = SnifferEngine.sanitizeMediaUrl(streamUrl)
                    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("http") } ?: getPageTitle()
                    (activity as? MainActivity)?.notifyVideoDetected(sanitized, cleanTitle, isMaster = true)
                }
            }
        }

        @JavascriptInterface
        fun onChannelReelFound(
            platform: String,
            reelId: String,
            reelUrl: String,
            streamUrl: String?,
            caption: String?
        ) {
            val cleanId = reelId.trim()
            if (cleanId.isEmpty()) return
            activity?.runOnUiThread {
                val meta = FavoriteChannelRepository.extractCaptionTitleAndHashtags(caption)
                val cleanStream = streamUrl?.trim()?.takeIf { it.startsWith("http") }?.let {
                    FavoriteChannelRepository.stripByteRangeParams(it)
                }
                val candidate = ChannelReelCandidate(
                    reelId = cleanId,
                    reelUrl = reelUrl.trim(),
                    platform = platform.trim().ifEmpty { "facebook" },
                    title = meta.title,
                    caption = meta.caption,
                    hashtags = meta.hashtags,
                    hdUrl = cleanStream
                )
                mergeDiscoveredChannelReel(candidate)
                updateFloatingChannelScanButton(getCurrentUrl())
            }
        }
    }


    
    private fun handleUrlOverride(view: WebView?, url: String): Boolean {
        if (url.isEmpty()) return false

        // 1. Chặn các scheme mở ứng dụng ngoài (fb://, intent://, market://, vnd.youtube:)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains("fullscreen_video/")) {
                val videoId = url.substringAfter("fullscreen_video/").substringBefore('?')
                extractVideoDirectly("https://www.facebook.com/reel/" + videoId)
            }
            return true // Chặn scheme ngoài gây văng hoặc chiếm quyền
        }

        // 2. Chặn các domain quảng cáo popup/cờ bạc/cam site đè mất trang web chính của người dùng
        val adBlockKeywords = listOf(
            "flirtify", "faphouse", "stripchat", "chaturbate", "bongacams",
            "adsterra", "popads", "propellerads", "exoclick", "exosrv",
            "bet365", "1xbet", "kubet", "thabet", "w88", "fun88",
            "trafficjunky", "tsyndicate", "adclick", "adservice"
        )
        val host = try { android.net.Uri.parse(url).host?.lowercase() ?: "" } catch(e: Exception) { "" }
        if (adBlockKeywords.any { host.contains(it) }) {
            android.util.Log.d("AntiPopup", "Blocked ad redirect/popup to: " + host)
            return true // Chặn không cho tải URL quảng cáo đè trang chủ
        }

        return false
    }

    companion object {
        // [VI] Script JavaScript vô hiệu hóa window.open, popunder và các liên kết popup ngoài trang chủ
        // [EN] Anti-Popup JS script to block window.open, popunders, and external ad popup links
        private const val ANTI_POPUP_JS = """
            (function() {
                try {
                    // 1. Ghi đè window.open để chặn pop-up và pop-under
                    // Override window.open to return null
                    window.open = function(url, target, features) {
                        console.log('[AntiPopup] Blocked window.open popup to:', url);
                        return null;
                    };

                    // 2. Chặn iframe hoặc script ngoài ghi đè lại window.open
                    if (window.top && window.top !== window) {
                        try { window.top.open = window.open; } catch(e) {}
                    }

                    // 3. Chặn các liên kết target="_blank" trỏ sang domain quảng cáo ngoài
                    // Block external popup ad links with target="_blank"
                    document.addEventListener('click', function(e) {
                        var el = e.target;
                        while (el && el.tagName !== 'A') {
                            el = el.parentElement;
                        }
                        if (el && el.tagName === 'A') {
                            if (el.target === '_blank') {
                                var linkHost = el.hostname ? el.hostname.toLowerCase() : '';
                                var currHost = window.location.hostname ? window.location.hostname.toLowerCase() : '';
                                // Nếu là domain ngoài khác với website đang xem -> CHẶN HOÀN TOÀN để không bị đè trang!
                                if (linkHost && linkHost !== currHost && !currHost.endsWith(linkHost) && !linkHost.endsWith(currHost)) {
                                    console.log('[AntiPopup] Blocked external popup ad link to:', el.href);
                                    e.preventDefault();
                                    e.stopPropagation();
                                    return false;
                                } else {
                                    // Cùng domain -> cho phép mở trên cùng tab
                                    el.target = '_self';
                                }
                            }
                        }
                    }, true);

                    // 4. Phát hiện và loại bỏ các lớp phủ tàng hình bẫy click trên trang web bên thứ 3
                    // Tuyệt đối không can thiệp vào DOM của Google, reCAPTCHA, YouTube, Facebook, Instagram, TikTok
                    var currHost = (window.location && window.location.hostname) ? window.location.hostname.toLowerCase() : '';
                    var isTrustedFirstParty = currHost.indexOf('google.') !== -1 ||
                                              currHost.indexOf('gstatic.') !== -1 ||
                                              currHost.indexOf('recaptcha.') !== -1 ||
                                              currHost.indexOf('youtube.') !== -1 ||
                                              currHost.indexOf('facebook.') !== -1 ||
                                              currHost.indexOf('instagram.') !== -1 ||
                                              currHost.indexOf('tiktok.') !== -1;
                    if (!isTrustedFirstParty) {
                        var removeOverlays = function() {
                            try {
                                var elements = document.querySelectorAll('div, a, span');
                                for (var i = 0; i < elements.length; i++) {
                                    var el = elements[i];
                                    if (el.querySelector && el.querySelector('iframe')) continue;
                                    var style = window.getComputedStyle(el);
                                    if (style.position === 'fixed' || style.position === 'absolute') {
                                        var zIndex = parseInt(style.zIndex, 10);
                                        if (zIndex >= 9999) {
                                            var rect = el.getBoundingClientRect();
                                            if (rect.width >= window.innerWidth * 0.8 && rect.height >= window.innerHeight * 0.8) {
                                                var opacity = parseFloat(style.opacity);
                                                if (opacity === 0 || style.backgroundColor === 'transparent' || style.backgroundColor.indexOf('rgba(0, 0, 0, 0)') !== -1) {
                                                    console.log('[AntiPopup] Removed invisible ad overlay:', el);
                                                    el.remove();
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch(e) {}
                        };
                        setTimeout(removeOverlays, 800);
                        setTimeout(removeOverlays, 2000);
                    }
                } catch(err) {
                    console.error('[AntiPopup] Error injecting script:', err);
                }
            })();
        """

        // [VI] Script JavaScript bảo đảm trang web hiển thị chuẩn Mobile Responsive mà KHÔNG làm hỏng kích thước iframe/table/sprite (như Google Search / Widget)
        private const val MOBILE_RESPONSIVE_JS = """
            (function() {
                try {
                    var currHost = (window.location && window.location.hostname) ? window.location.hostname.toLowerCase() : '';
                    if (currHost.indexOf('google.') !== -1 || currHost.indexOf('gstatic.') !== -1 || currHost.indexOf('recaptcha.') !== -1) {
                        var oldStyle = document.getElementById('ai-downloader-mobile-responsive');
                        if (oldStyle) oldStyle.remove();
                        return;
                    }
                    // 1. Kiểm tra hoặc chèn thẻ meta viewport chuẩn thiết bị di động
                    var vp = document.querySelector('meta[name="viewport"]');
                    if (!vp) {
                        vp = document.createElement('meta');
                        vp.setAttribute('name', 'viewport');
                        (document.head || document.documentElement).appendChild(vp);
                    }
                    vp.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');

                    // 2. Chèn CSS Responsive nhẹ nhàng, KHÔNG ép iframe { height: auto !important } hay table { display: block !important } làm cắt mất khung iframe
                    var styleId = 'ai-downloader-mobile-responsive';
                    if (!document.getElementById(styleId)) {
                        var css = document.createElement('style');
                        css.id = styleId;
                        css.type = 'text/css';
                        css.innerHTML = 'html, body { max-width: 100vw !important; overflow-x: hidden !important; -webkit-text-size-adjust: 100% !important; } video { max-width: 100% !important; height: auto !important; } pre, code { white-space: pre-wrap !important; word-break: break-word !important; }';
                        (document.head || document.documentElement).appendChild(css);
                    }
                } catch(e) {}
            })();
        """

        // [VI] Script JavaScript tự động bắt toàn bộ danh sách Reels/Shorts từ DOM + Fetch + XHR GraphQL của Instagram, Facebook, YouTube
        private const val CHANNEL_HARVESTER_JS = """
            (function() {
                try {
                    var reservedIg = {
                        'reels': 1, 'reel': 1, 'videos': 1, 'audio': 1, 'explore': 1,
                        'accounts': 1, 'direct': 1, 'stories': 1, 'about': 1, 'legal': 1,
                        'privacy': 1, 'terms': 1, 'p': 1, 'tv': 1
                    };

                    function reportReel(platform, id, url, stream, caption) {
                        if (!id || !window.AndroidSniffer || !window.AndroidSniffer.onChannelReelFound) return;
                        try {
                            window.AndroidSniffer.onChannelReelFound(
                                String(platform || ''),
                                String(id || ''),
                                String(url || ''),
                                String(stream || ''),
                                String(caption || '')
                            );
                        } catch(e) {}
                    }

                    function walkPayloadObj(node) {
                        if (!node || typeof node !== 'object') return;
                        if (Array.isArray(node)) {
                            for (var i = 0; i < node.length; i++) walkPayloadObj(node[i]);
                            return;
                        }
                        var host = (window.location && window.location.hostname) ? window.location.hostname.toLowerCase() : '';
                        if (host.indexOf('instagram.com') !== -1) {
                            var code = node.code || node.shortcode;
                            if (typeof code === 'string' && /^[A-Za-z0-9_-]{8,25}$/.test(code) && !reservedIg[code.toLowerCase()]) {
                                var vUrl = '';
                                if (Array.isArray(node.video_versions) && node.video_versions.length > 0 && node.video_versions[0].url) {
                                    vUrl = node.video_versions[0].url;
                                } else if (typeof node.video_url === 'string') {
                                    vUrl = node.video_url;
                                }
                                var cap = (node.caption && node.caption.text) ? node.caption.text :
                                          (node.edge_media_to_caption && node.edge_media_to_caption.edges && node.edge_media_to_caption.edges[0] && node.edge_media_to_caption.edges[0].node) ? node.edge_media_to_caption.edges[0].node.text : '';
                                if (vUrl || node.is_video || node.media_type === 2 || node.product_type === 'clips' || node.clips_metadata) {
                                    reportReel('instagram', code, 'https://www.instagram.com/reel/' + code + '/', vUrl, cap);
                                }
                            }
                        } else if (host.indexOf('facebook.com') !== -1) {
                            var fid = node.video_id || node.id;
                            var fidStr = fid ? String(fid) : '';
                            if (/^\d{8,25}$/.test(fidStr)) {
                                var hd = node.browser_native_hd_url || node.playable_url_quality_hd || node.hd_src || '';
                                var sd = node.browser_native_sd_url || node.playable_url || node.sd_src || '';
                                if (!hd && Array.isArray(node.progressive_urls)) {
                                    for (var p = 0; p < node.progressive_urls.length; p++) {
                                        var pu = node.progressive_urls[p];
                                        if (pu && pu.progressive_url) {
                                            if (pu.metadata && pu.metadata.quality === 'HD') hd = pu.progressive_url;
                                            else if (!sd) sd = pu.progressive_url;
                                        }
                                    }
                                }
                                var fcap = (node.message && node.message.text) ? node.message.text :
                                           (node.creation_story && node.creation_story.message && node.creation_story.message.text) ? node.creation_story.message.text : '';
                                if (hd || sd || node.__typename === 'Video') {
                                    reportReel('facebook', fidStr, 'https://www.facebook.com/reel/' + fidStr + '/', hd || sd, fcap);
                                }
                            }
                        } else if (host.indexOf('youtube.com') !== -1) {
                            var yid = node.videoId;
                            if (typeof yid === 'string' && /^[A-Za-z0-9_-]{11}$/.test(yid)) {
                                var ytitle = (node.headline && node.headline.simpleText) ? node.headline.simpleText :
                                             (node.title && node.title.simpleText) ? node.title.simpleText :
                                             (node.title && node.title.runs && node.title.runs[0]) ? node.title.runs[0].text : '';
                                reportReel('youtube', yid, 'https://www.youtube.com/shorts/' + yid, '', ytitle);
                            }
                        }
                        var keys = Object.keys(node);
                        for (var k = 0; k < keys.length; k++) {
                            var v = node[keys[k]];
                            if (v && typeof v === 'object') walkPayloadObj(v);
                        }
                    }

                    window.__harvestChannelDom = function() {
                        try {
                            var host = (window.location && window.location.hostname) ? window.location.hostname.toLowerCase() : '';
                            var anchors = document.querySelectorAll('a[href]');
                            for (var i = 0; i < anchors.length; i++) {
                                var a = anchors[i];
                                var href = a.href || a.getAttribute('href') || '';
                                if (!href) continue;
                                if (host.indexOf('instagram.com') !== -1) {
                                    var mIg = href.match(/\/(?:reel|reels|p|tv)\/([A-Za-z0-9_-]{8,25})(?:[\/?#]|$)/i);
                                    if (mIg && mIg[1] && !reservedIg[mIg[1].toLowerCase()]) {
                                        var img = a.querySelector('img[alt]');
                                        var alt = img ? (img.getAttribute('alt') || '') : (a.getAttribute('aria-label') || '');
                                        reportReel('instagram', mIg[1], 'https://www.instagram.com/reel/' + mIg[1] + '/', '', alt);
                                    }
                                } else if (host.indexOf('facebook.com') !== -1) {
                                    var mFb = href.match(/(?:\/(?:reel|reels|videos)\/|watch\/?\?v=)(\d{8,25})/i);
                                    if (mFb && mFb[1]) {
                                        var label = a.getAttribute('aria-label') || a.innerText || '';
                                        reportReel('facebook', mFb[1], 'https://www.facebook.com/reel/' + mFb[1] + '/', '', label);
                                    }
                                } else if (host.indexOf('youtube.com') !== -1) {
                                    var mYt = href.match(/(?:\/shorts\/|watch\?v=)([A-Za-z0-9_-]{11})/i);
                                    if (mYt && mYt[1]) {
                                        var yLabel = a.getAttribute('title') || a.getAttribute('aria-label') || a.innerText || '';
                                        reportReel('youtube', mYt[1], 'https://www.youtube.com/shorts/' + mYt[1], '', yLabel);
                                    }
                                }
                            }

                            var scripts = document.querySelectorAll('script[type="application/json"], script:not([src])');
                            for (var s = 0; s < scripts.length; s++) {
                                var sc = scripts[s];
                                if (sc.__harvested) continue;
                                var txt = sc.textContent || '';
                                if (txt.length < 30) continue;
                                if (txt.indexOf('shortcode') !== -1 || txt.indexOf('"code"') !== -1 || txt.indexOf('video_versions') !== -1 ||
                                    txt.indexOf('browser_native_') !== -1 || txt.indexOf('playable_url') !== -1 ||
                                    txt.indexOf('videoId') !== -1 || txt.indexOf('/reel/') !== -1) {
                                    sc.__harvested = true;
                                    try {
                                        if (host.indexOf('instagram.com') !== -1) {
                                            var igRe = /"(?:code|shortcode)"\s*:\s*"([A-Za-z0-9_-]{9,15})"/g;
                                            var igMatch;
                                            while ((igMatch = igRe.exec(txt)) !== null) {
                                                var c = igMatch[1];
                                                if (c && !reservedIg[c.toLowerCase()]) {
                                                    reportReel('instagram', c, 'https://www.instagram.com/reel/' + c + '/', '', '');
                                                }
                                            }
                                        }
                                        if (txt.trim().charAt(0) === '{' || txt.trim().charAt(0) === '[') {
                                            walkPayloadObj(JSON.parse(txt));
                                        }
                                    } catch(e) {}
                                }
                            }
                        } catch(e) {}
                    };

                    if (!window.__channelHarvesterHooked) {
                        window.__channelHarvesterHooked = true;

                        // Hook window.fetch
                        if (window.fetch) {
                            var origFetch = window.fetch;
                            window.fetch = function() {
                                return origFetch.apply(this, arguments).then(function(response) {
                                    try {
                                        var u = (response && response.url) ? response.url.toLowerCase() : '';
                                        if (u.indexOf('graphql') !== -1 || u.indexOf('/api/v1/') !== -1 || u.indexOf('clips') !== -1 || u.indexOf('youtubei/v1/browse') !== -1) {
                                            response.clone().text().then(function(t) {
                                                try {
                                                    var clean = t.replace(/^for\s*\(\s*;\s*;\s*\)\s*;/, '').trim();
                                                    var lines = clean.split(/\r?\n/);
                                                    for (var l = 0; l < lines.length; l++) {
                                                        var line = lines[l].trim();
                                                        if (line.charAt(0) === '{' || line.charAt(0) === '[') {
                                                            walkPayloadObj(JSON.parse(line));
                                                        }
                                                    }
                                                } catch(e) {}
                                            }).catch(function(){});
                                        }
                                    } catch(e) {}
                                    return response;
                                });
                            };
                        }

                        // Hook XMLHttpRequest
                        if (window.XMLHttpRequest) {
                            var origSend = XMLHttpRequest.prototype.send;
                            XMLHttpRequest.prototype.send = function() {
                                this.addEventListener('load', function() {
                                    try {
                                        var rUrl = (this.responseURL || '').toLowerCase();
                                        if (rUrl.indexOf('graphql') !== -1 || rUrl.indexOf('/api/v1/') !== -1 || rUrl.indexOf('clips') !== -1 || rUrl.indexOf('youtubei/v1/browse') !== -1) {
                                            var txt = this.responseText || '';
                                            var clean = txt.replace(/^for\s*\(\s*;\s*;\s*\)\s*;/, '').trim();
                                            var lines = clean.split(/\r?\n/);
                                            for (var l = 0; l < lines.length; l++) {
                                                var line = lines[l].trim();
                                                if (line.charAt(0) === '{' || line.charAt(0) === '[') {
                                                    walkPayloadObj(JSON.parse(line));
                                                }
                                            }
                                        }
                                    } catch(e) {}
                                });
                                return origSend.apply(this, arguments);
                            };
                        }

                        window.addEventListener('scroll', function() {
                            if (window.__harvestChannelDom) window.__harvestChannelDom();
                        }, { passive: true });

                        var sweepCount = 0;
                        var sweepTimer = setInterval(function() {
                            sweepCount++;
                            if (window.__harvestChannelDom) window.__harvestChannelDom();
                            if (sweepCount >= 25) clearInterval(sweepTimer);
                        }, 750);
                    }

                    window.__harvestChannelDom();
                } catch(e) {}
            })();
        """
    }
}
