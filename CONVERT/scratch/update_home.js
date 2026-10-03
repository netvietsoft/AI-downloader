const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt');
let content = fs.readFileSync(filePath, 'utf8');

// 1. Thay thế đoạn onPageFinished
const oldOnPageFinishedPattern = /override\s+fun\s+onPageFinished\s*\(\s*view:\s*WebView\?,\s*url:\s*String\?\s*\)\s*\{[\s\S]*?view\?\.evaluateJavascript\("""[\s\S]*?checkCount\s*>=\s*20[\s\S]*?\}\s*,\s*500\s*\);\s*\}\)\(\);[\s\S]*?"""\.trimIndent\(\),\s*null\)\s*\}/;

const newOnPageFinished = `override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val pageTitle = view?.title ?: ""
                    if (pageTitle.isNotEmpty() && !pageTitle.startsWith("http")) { currentWebTitle = pageTitle }
                    // [VI] Tái củng cố script chặn popup sau khi toàn bộ DOM & scripts bên thứ 3 đã nạp
                    // [EN] Reinforce anti-popup script after DOM & third-party scripts have loaded
                    view?.evaluateJavascript(ANTI_POPUP_JS, null)

                    // 1. Phân tích trực tiếp toàn bộ DOM HTML đã nạp trong WebView (Bảo đảm vượt tường lửa ISP)
                    if (url != null && (url.contains("xnxx.com") || url.contains("xnxx2.com") ||
                            url.contains("xvideos.com") || url.contains("pornhub.com"))) {
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
                                            var mHls = scText.match(/(?:setVideoHLS|video_hls)\\s*[\\(=]\\s*['"]([^'"]+)['"]/);
                                            var mHigh = scText.match(/(?:setVideoUrlHigh|video_url_high)\\s*[\\(=]\\s*['"]([^'"]+)['"]/);
                                            var mLow = scText.match(/(?:setVideoUrlLow|video_url_low)\\s*[\\(=]\\s*['"]([^'"]+)['"]/);
                                            var mTitle = scText.match(/(?:setVideoTitle|video_title)\\s*[\\(=]\\s*['"]([^'"]+)['"]/);
                                            if (mTitle && mTitle[1]) title = mTitle[1];
                                            if (mHigh && mHigh[1]) stream = mHigh[1];
                                            else if (mHls && mHls[1]) stream = mHls[1];
                                            else if (mLow && mLow[1]) stream = mLow[1];
                                            if (stream) break;
                                        }
                                        if (scText.indexOf('mediaDefinitions') !== -1) {
                                            var mDef = scText.match(/mediaDefinitions\\s*[:=]\\s*(\\[\\s*\\{.*?\\}\\s*\\])/);
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
                }`;

if (!oldOnPageFinishedPattern.test(content)) {
    console.error("Pattern oldOnPageFinishedPattern not matched!");
    process.exit(1);
}

content = content.replace(oldOnPageFinishedPattern, newOnPageFinished);

// 2. Cập nhật inspectUrlForVideo: Whitelist CDN và bỏ 640x360 chặn
content = content.replace('lower.contains("modusygunaciro.com") || lower.contains("640x360") || lower.contains("448x250") ||',
    'lower.contains("modusygunaciro.com") || lower.contains("448x250") ||');

const cdnBlockOld = `lower.contains("xhcdn.com") ||
            lower.contains("xvideos-cdn.com") ||
            lower.contains("phncdn.com")) &&`;

const cdnBlockNew = `lower.contains("xhcdn.com") ||
            lower.contains("xnxx-cdn.com") ||
            lower.contains("xvideos-cdn.com") ||
            lower.contains("red-cdn.com") ||
            lower.contains("gold-cdn.com") ||
            lower.contains("others-cdn.com") ||
            lower.contains("cdn77.org") ||
            lower.contains("phncdn.com")) &&`;

content = content.replace(cdnBlockOld, cdnBlockNew);

// 3. Cập nhật handleUserUrl để nhận diện xnxx, xvideos, pornhub
const directCheckOld = `        // Nếu là liên kết Facebook Reel / Watch -> Trích xuất trực tiếp song song
        if (targetUrl.contains("facebook.com") || targetUrl.contains("fb.watch")) {
            extractVideoDirectly(targetUrl)
        }`;

const directCheckNew = `        // Nếu là liên kết Facebook Reel / Watch / XNXX / XVideos / Pornhub -> Trích xuất trực tiếp song song
        if (targetUrl.contains("facebook.com") || targetUrl.contains("fb.watch") ||
            targetUrl.contains("xnxx.com") || targetUrl.contains("xnxx2.com") ||
            targetUrl.contains("xvideos.com") || targetUrl.contains("pornhub.com")) {
            extractVideoDirectly(targetUrl)
        }`;

content = content.replace(directCheckOld, directCheckNew);

// 4. Cập nhật extractVideoDirectly
const extractDirectlyOld = `    fun extractVideoDirectly(url: String) {
        Toast.makeText(context, "⚡ Đang bóc tách video Facebook HD...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val videoInfo = snifferEngine.extractFacebookVideo(url)
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }`;

const extractDirectlyNew = `    fun extractVideoDirectly(url: String) {
        Toast.makeText(context, "⚡ Đang bóc tách luồng video HD...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val videoInfo = snifferEngine.extractVideoFromUrl(url)
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }`;

content = content.replace(extractDirectlyOld, extractDirectlyNew);

fs.writeFileSync(filePath, content, 'utf8');
console.log("Successfully updated HomeFragment.kt!");
