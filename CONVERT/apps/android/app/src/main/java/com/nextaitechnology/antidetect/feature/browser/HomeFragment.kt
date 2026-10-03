package com.nextaitechnology.antidetect.feature.browser

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextaitechnology.antidetect.MainActivity
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.network.SnifferEngine
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import kotlinx.coroutines.launch

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

    private val snifferEngine = SnifferEngine()
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

        webView = view.findViewById(R.id.web_view)
        webProgressBar = view.findViewById(R.id.web_progress_bar)
        etUrlInput = view.findViewById(R.id.et_url_input)
        btnGoUrl = view.findViewById(R.id.btn_go_url)
        btnClearUrl = view.findViewById(R.id.btn_clear_url)
        scrollHomeContent = view.findViewById(R.id.scroll_home_content)
        btnPasteDetect = view.findViewById(R.id.btn_paste_detect)

        setupWebView()
        setupSearchBar()

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

            // Cấu hình Mobile User-Agent & Responsive hiển thị tối ưu cho màn hình di động
            settings.userAgentString = SnifferEngine.MOBILE_USER_AGENT
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
                    // [VI] Tiêm script vô hiệu hóa popup và kích hoạt Mobile Responsive ngay khi bắt đầu tải trang
                    // [EN] Inject anti-popup and mobile responsive scripts right as page starts loading
                    view?.evaluateJavascript(ANTI_POPUP_JS, null)
                    view?.evaluateJavascript(MOBILE_RESPONSIVE_JS, null)
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
                    val pageTitle = view?.title ?: ""
                    if (pageTitle.isNotEmpty() && !pageTitle.startsWith("http")) { currentWebTitle = pageTitle }
                    if (url != null) {
                        (activity as? MainActivity)?.updateCurrentTabInfo(url, if (pageTitle.isNotEmpty() && !pageTitle.startsWith("http")) pageTitle else (currentWebTitle ?: "Web Page"))
                    }
                    // [VI] Tái củng cố script chặn popup & responsive sau khi toàn bộ DOM & scripts bên thứ 3 đã nạp
                    // [EN] Reinforce anti-popup & responsive scripts after DOM & third-party scripts have loaded
                    view?.evaluateJavascript(ANTI_POPUP_JS, null)
                    view?.evaluateJavascript(MOBILE_RESPONSIVE_JS, null)

                    // 1. Phân tích trực tiếp toàn bộ DOM HTML đã nạp trong WebView (Bảo đảm vượt tường lửa ISP)
                    if (url != null && (url.contains("xnxx.com") || url.contains("xnxx2.com") ||
                            url.contains("xvideos.com") || url.contains("pornhub.com") ||
                            url.contains("youtube.com") || url.contains("youtu.be") ||
                            url.contains("vimeo.com") || url.contains("dailymotion.com") ||
                            com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.isMonetizedDomain(url))) {
                        view?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { rawHtmlJson ->
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
                                    } catch (e: Exception) {
                                        android.util.Log.w("NextAI_Sniffer", "Lỗi phân tích DOM HTML", e)
                                    }
                                }
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

                                    // C. Quét thẻ <video> và <source> trong DOM
                                    if (!stream) {
                                        var vids = document.querySelectorAll('video, video source');
                                        for (var j = 0; j < vids.length; j++) {
                                            var vSrc = vids[j].src || vids[j].getAttribute('src') || '';
                                            if (vSrc && vSrc.startsWith('http') && (vSrc.indexOf('.mp4') !== -1 || vSrc.indexOf('.m3u8') !== -1)) {
                                                stream = vSrc;
                                                break;
                                            }
                                        }
                                    }

                                    // D. Hook prototype play để bắt luồng khi người dùng bấm phát
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
        // Bỏ qua các phân đoạn chunk byte range, segment files, init files và static assets
        // Filter out chunk segments, range requests, init segments, tracking & non-video assets
        if (lower.contains("bytestart") || lower.contains("byteend") ||
            lower.contains(".m4s") || lower.contains(".ts") || lower.contains("seg-") ||
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
            (lower.contains("video.") && lower.contains(".fbcdn.net") && lower.contains(".mp4")) ||
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
            val sanitized = SnifferEngine.sanitizeMediaUrl(url)
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

        // Nếu là liên kết YouTube, Facebook, Vimeo hoặc thuộc 1822+ Monetized Domains -> Trích xuất trực tiếp song song
        if (targetUrl.contains("youtube.com") || targetUrl.contains("youtu.be") ||
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
        (activity as? MainActivity)?.resetDetectionState()
        webView.loadUrl(url)
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
        scrollHomeContent.visibility = View.VISIBLE
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
            scrollHomeContent.visibility = View.VISIBLE
            etUrlInput.setText("")
            (activity as? MainActivity)?.resetDetectionState()
            true
        } else {
            false
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

                    // 4. Phát hiện và loại bỏ các lớp phủ tàng hình bẫy click (invisible ad overlay / click-jacking)
                    var removeOverlays = function() {
                        try {
                            var elements = document.querySelectorAll('div, a, span');
                            for (var i = 0; i < elements.length; i++) {
                                var el = elements[i];
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
                } catch(err) {
                    console.error('[AntiPopup] Error injecting script:', err);
                }
            })();
        """

        // [VI] Script JavaScript bảo đảm 100% trang web hiển thị chuẩn Mobile Responsive
        // [EN] JavaScript script ensuring 100% of websites render fluidly and responsively for mobile screens
        private const val MOBILE_RESPONSIVE_JS = """
            (function() {
                try {
                    // 1. Kiểm tra hoặc chèn thẻ meta viewport chuẩn thiết bị di động
                    var vp = document.querySelector('meta[name="viewport"]');
                    if (!vp) {
                        vp = document.createElement('meta');
                        vp.setAttribute('name', 'viewport');
                        (document.head || document.documentElement).appendChild(vp);
                    }
                    vp.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');

                    // 2. Chèn CSS Reset Responsive di động chống tràn ngang và co giãn media tự động
                    var styleId = 'ai-downloader-mobile-responsive';
                    if (!document.getElementById(styleId)) {
                        var css = document.createElement('style');
                        css.id = styleId;
                        css.type = 'text/css';
                        css.innerHTML = 'html, body { max-width: 100vw !important; overflow-x: hidden !important; -webkit-text-size-adjust: 100% !important; } img, video, iframe, embed, object { max-width: 100% !important; height: auto !important; } table { max-width: 100% !important; display: block !important; overflow-x: auto !important; -webkit-overflow-scrolling: touch !important; } pre, code { white-space: pre-wrap !important; word-break: break-word !important; }';
                        (document.head || document.documentElement).appendChild(css);
                    }
                } catch(e) {}
            })();
        """
    }
}
