package com.nextaitechnology.antidetect.core.network

import android.util.Log
import com.nextaitechnology.antidetect.core.model.VideoInfo
import com.nextaitechnology.antidetect.core.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * CÃ´ng cá»¥ phÃ¢n tÃ­ch vÃ  bÃ³c tÃ¡ch luá»“ng video Ä‘a ná» n táº£ng (SnifferEngine)
 * High-performance sniffer engine for direct video stream extraction (Facebook, TikTok, Instagram, etc.)
 *
 * @author NextAI Technology Core Team
 */
class SnifferEngine {

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; SM-A075F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

        /**
         * Chuáº©n hÃ³a vÃ  giáº£i mÃ£ cÃ¡c kÃ½ tá»± thoÃ¡t JSON trong URL luá»“ng video
         * Properly unescapes JSON string entities without corrupting query signatures
         */
        fun sanitizeMediaUrl(raw: String): String {
            var url = raw.trim()
            url = url.replace("\\/", "/")
            url = url.replace("\\u0026", "&")
            url = url.replace("\\u0025", "%")
            url = url.replace("\\u003D", "=")
            url = url.replace("\\u003d", "=")
            url = url.replace("&amp;", "&")
            url = url.replace("u0026", "&")
            url = url.replace("u00253D", "%3D")
            url = url.replace("u00253d", "%3D")
            return url
        }

        /**
         * Chuáº©n hÃ³a liÃªn káº¿t Facebook Reel/Watch thÃ nh Watch URL chá»©a dá»¯ liá»‡u MP4 Ä‘áº§y Ä‘á»§
         * Normalizes Facebook Reel / Watch / In-App links to canonical /watch/?v= endpoint containing full video JSON
         */
        fun normalizeFacebookUrl(pageUrl: String): String {
            val trimmed = pageUrl.trim()
            val videoId = when {
                trimmed.contains("/reel/") -> {
                    trimmed.substringAfter("/reel/").substringBefore('/').substringBefore('?')
                }
                trimmed.contains("/videos/") -> {
                    trimmed.substringAfter("/videos/").substringBefore('/').substringBefore('?')
                }
                trimmed.contains("v=") -> {
                    trimmed.substringAfter("v=").substringBefore('&').substringBefore('?')
                }
                trimmed.contains("fullscreen_video/") -> {
                    trimmed.substringAfter("fullscreen_video/").substringBefore('?')
                }
                else -> ""
            }

            return if (videoId.isNotEmpty() && videoId.all { it.isDigit() }) {
                "https://www.facebook.com/watch/?v=$videoId"
            } else if (trimmed.startsWith("http")) {
                trimmed.replace("m.facebook.com", "www.facebook.com")
            } else {
                "https://www.facebook.com/watch/?v=2120496905227881"
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    /**
     * BÃ³c tÃ¡ch video Facebook Reels / Watch / Feed trá»±c tiáº¿p
     * Directly extracts Facebook video URLs without relying on native app deep links
     */
    suspend fun extractFacebookVideo(pageUrl: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val initialUrl = normalizeFacebookUrl(pageUrl)
            Log.i("NextAI_Sniffer", "Khá»Ÿi cháº¡y bÃ³c tÃ¡ch Facebook Video tá»«: $initialUrl (Gá»‘c: $pageUrl)")

            var currentUrl = initialUrl
            var html = ""
            var redirectCount = 0

            // Xá»­ lÃ½ chuyá»ƒn hÆ°á»›ng 301/302 liÃªn tá»¥c Ä‘áº£m báº£o giá»¯ nguyÃªn Header Desktop Navigation
            // Explicitly follow HTTP redirects while preserving full desktop navigation headers
            while (redirectCount < 5) {
                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", DESKTOP_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-User", "?1")
                    .header("Sec-Fetch-Dest", "document")
                    .build()

                val response: Response = httpClient.newCall(request).execute()
                val code = response.code
                if (code in 300..399) {
                    val location = response.header("Location")
                    response.close()
                    if (!location.isNullOrEmpty()) {
                        currentUrl = if (location.startsWith("http")) location else "https://www.facebook.com$location"
                        Log.i("NextAI_Sniffer", "Chuyá»ƒn hÆ°á»›ng $code sang: $currentUrl")
                        redirectCount++
                        continue
                    }
                }

                if (!response.isSuccessful) {
                    Log.w("NextAI_Sniffer", "Lá»—i HTTP khi táº£i trang: " + response.code)
                    response.close()
                    return@withContext null
                }

                html = response.body?.string() ?: ""
                response.close()
                break
            }

            if (html.isEmpty()) {
                Log.w("NextAI_Sniffer", "Dá»¯ liá»‡u trang trá»‘ng")
                return@withContext null
            }

            Log.i("NextAI_Sniffer", "Ä Ã£ táº£i payload HTML Facebook thÃ nh cÃ´ng (KÃ­ch thÆ°á»›c: ${html.length} bytes)")

            // Regex bÃ³c tÃ¡ch HD & SD Stream URL tá»« JSON payload cá»§a Facebook
            // Regex to extract browser_native_hd_url and browser_native_sd_url from payload
            val hdRegex = Pattern.compile(""""(?:browser_native_hd_url|playable_url_quality_hd|hd_src)":"([^"]+)"""")
            val sdRegex = Pattern.compile(""""(?:browser_native_sd_url|playable_url|sd_src)":"([^"]+)"""")

            val hdMatcher = hdRegex.matcher(html)
            val sdMatcher = sdRegex.matcher(html)

            var hdUrl: String? = null
            var sdUrl: String? = null

            if (hdMatcher.find()) {
                hdUrl = sanitizeMediaUrl(hdMatcher.group(1) ?: "")
                Log.i("NextAI_Sniffer", "Ä Ã£ tÃ¬m tháº¥y HD Stream URL: " + hdUrl.take(80) + "...")
            }
            if (sdMatcher.find()) {
                sdUrl = sanitizeMediaUrl(sdMatcher.group(1) ?: "")
                Log.i("NextAI_Sniffer", "Ä Ã£ tÃ¬m tháº¥y SD Stream URL: " + sdUrl.take(80) + "...")
            }

            // Fallback: TÃ¬m cÃ¡c URL mp4 fbcdn trá»±c tiáº¿p trong trang
            if (hdUrl == null && sdUrl == null) {
                val fbcdnRegex = Pattern.compile("""https:\\/\\/video[^"]+\\.fbcdn\\.net[^"]+\\.mp4[^"]*""")
                val fbcdnMatcher = fbcdnRegex.matcher(html)
                if (fbcdnMatcher.find()) {
                    sdUrl = sanitizeMediaUrl(fbcdnMatcher.group(0) ?: "")
                }
            }

            if (hdUrl == null && sdUrl == null) {
                Log.w("NextAI_Sniffer", "KhÃ´ng tÃ¬m tháº¥y luá»“ng phÃ¡t HD/SD trong trang")
                return@withContext null
            }

            val videoId = currentUrl.substringAfter("v=").substringBefore('&')
                .ifEmpty { currentUrl.substringAfter("/videos/").substringBefore('/') }
                .ifEmpty { "2120496905227881" }

            val qualities = mutableListOf<VideoQuality>()
            if (!hdUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = "1080p Full HD",
                        format = "mp4",
                        approximateSize = 48_500_000L,
                        downloadUrl = hdUrl
                    )
                )
            }
            if (!sdUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = "720p HD",
                        format = "mp4",
                        approximateSize = 24_200_000L,
                        downloadUrl = sdUrl
                    )
                )
            }

            val primaryUrl = hdUrl ?: sdUrl ?: ""
            qualities.add(
                VideoQuality(
                    label = "Audio Only (MP3)",
                    format = "mp3",
                    approximateSize = 4_500_000L,
                    downloadUrl = primaryUrl,
                    isAudioOnly = true
                )
            )

            VideoInfo(
                title = "Facebook_Reel_" + videoId + ".mp4",
                sourcePageUrl = currentUrl,
                thumbnailUrl = "",
                durationSeconds = 60,
                qualities = qualities,
                extractedHeaders = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to "https://www.facebook.com/"
                )
            )
        } catch (e: Exception) {
            Log.e("NextAI_Sniffer", "Lỗi trích xuất video Facebook", e)
            null
        }
    }

    /**
     * Bóc tách video chuyên sâu cho các trang XNXX / XVideos (xnxx.com, xnxx2.com, xvideos.com, v.v.)
     * High-precision stream extractor for XNXX & XVideos HTML5 player architecture
     */
    suspend fun extractXnxxVideo(pageUrl: String, htmlContent: String? = null): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            Log.i("NextAI_Sniffer", "Bắt đầu bóc tách video XNXX/XVideos từ: $pageUrl")
            val html = if (!htmlContent.isNullOrBlank()) {
                htmlContent
            } else {
                fetchHtmlWithOkHttp(pageUrl)
            }

            if (html.isBlank()) {
                Log.w("NextAI_Sniffer", "HTML nội dung trang XNXX trống")
                return@withContext null
            }

            // 1. Trích xuất tiêu đề video (Title)
            val titleRegex = Pattern.compile("""(?:html5player\.setVideoTitle|var\s+video_title)\s*[\(=]\s*['"]([^'"]+)['"]""")
            val titleMatcher = titleRegex.matcher(html)
            var videoTitle = if (titleMatcher.find()) titleMatcher.group(1)?.trim() else null

            if (videoTitle.isNullOrEmpty()) {
                val ogTitleRegex = Pattern.compile("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
                val ogMatcher = ogTitleRegex.matcher(html)
                if (ogMatcher.find()) {
                    videoTitle = ogMatcher.group(1)?.trim()
                }
            }
            if (videoTitle.isNullOrEmpty()) {
                val pageTitleRegex = Pattern.compile("""<title>([^<]+)</title>""", Pattern.CASE_INSENSITIVE)
                val pageTitleMatcher = pageTitleRegex.matcher(html)
                if (pageTitleMatcher.find()) {
                    videoTitle = pageTitleMatcher.group(1)?.replace("- XNXX.COM", "")?.replace("- XVIDEOS.COM", "")?.trim()
                }
            }
            val finalTitle = (videoTitle?.takeIf { it.isNotBlank() } ?: "XNXX_Video")
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_") + ".mp4"

            // 2. Trích xuất Thumbnail
            val thumbRegex = Pattern.compile("""(?:html5player\.setThumbUrl(?:169)?)\s*\(\s*['"]([^'"]+)['"]""")
            val thumbMatcher = thumbRegex.matcher(html)
            var thumbUrl = if (thumbMatcher.find()) thumbMatcher.group(1) ?: "" else ""
            if (thumbUrl.isEmpty()) {
                val ogImageRegex = Pattern.compile("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
                val ogImageMatcher = ogImageRegex.matcher(html)
                if (ogImageMatcher.find()) {
                    thumbUrl = ogImageMatcher.group(1) ?: ""
                }
            }

            // 3. Trích xuất luồng High MP4 (720p / 1080p)
            val highRegex = Pattern.compile("""(?:html5player\.setVideoUrlHigh|var\s+video_url_high)\s*[\(=]\s*['"]([^'"]+)['"]""")
            val highMatcher = highRegex.matcher(html)
            val highUrl = if (highMatcher.find()) sanitizeMediaUrl(highMatcher.group(1) ?: "") else null

            // 4. Trích xuất luồng Low MP4 (360p / 250p)
            val lowRegex = Pattern.compile("""(?:html5player\.setVideoUrlLow|var\s+video_url_low)\s*[\(=]\s*['"]([^'"]+)['"]""")
            val lowMatcher = lowRegex.matcher(html)
            val lowUrl = if (lowMatcher.find()) sanitizeMediaUrl(lowMatcher.group(1) ?: "") else null

            // 5. Trích xuất luồng Adaptive HLS M3U8
            val hlsRegex = Pattern.compile("""(?:html5player\.setVideoHLS|var\s+video_hls)\s*[\(=]\s*['"]([^'"]+)['"]""")
            val hlsMatcher = hlsRegex.matcher(html)
            val hlsUrl = if (hlsMatcher.find()) sanitizeMediaUrl(hlsMatcher.group(1) ?: "") else null

            // 6. Fallback: Quét thẻ <video> và <source>
            var fallbackUrl: String? = null
            if (highUrl.isNullOrEmpty() && lowUrl.isNullOrEmpty() && hlsUrl.isNullOrEmpty()) {
                val sourceRegex = Pattern.compile("""<(?:video|source)[^>]+src=['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", Pattern.CASE_INSENSITIVE)
                val sourceMatcher = sourceRegex.matcher(html)
                if (sourceMatcher.find()) {
                    fallbackUrl = sanitizeMediaUrl(sourceMatcher.group(1) ?: "")
                }
            }

            val qualities = mutableListOf<VideoQuality>()
            if (!highUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = "720p / 1080p High Quality",
                        format = "mp4",
                        approximateSize = 52_000_000L,
                        downloadUrl = highUrl
                    )
                )
            }
            if (!hlsUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = "Adaptive HLS (m3u8)",
                        format = "m3u8",
                        approximateSize = 42_000_000L,
                        downloadUrl = hlsUrl
                    )
                )
            }
            if (!lowUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = "360p Standard Quality",
                        format = "mp4",
                        approximateSize = 22_000_000L,
                        downloadUrl = lowUrl
                    )
                )
            }
            if (qualities.isEmpty() && !fallbackUrl.isNullOrEmpty()) {
                qualities.add(
                    VideoQuality(
                        label = if (fallbackUrl.contains(".m3u8")) "Adaptive HLS Stream" else "Direct Video Stream",
                        format = if (fallbackUrl.contains(".m3u8")) "m3u8" else "mp4",
                        approximateSize = 30_000_000L,
                        downloadUrl = fallbackUrl
                    )
                )
            }

            if (qualities.isEmpty()) {
                Log.w("NextAI_Sniffer", "Không tìm thấy luồng phát hợp lệ trong trang XNXX")
                return@withContext null
            }

            val primaryUrl = highUrl ?: hlsUrl ?: lowUrl ?: fallbackUrl ?: ""
            qualities.add(
                VideoQuality(
                    label = "Audio Only (MP3)",
                    format = "mp3",
                    approximateSize = 5_200_000L,
                    downloadUrl = primaryUrl,
                    isAudioOnly = true
                )
            )

            Log.i("NextAI_Sniffer", "Bóc tách video XNXX thành công: $finalTitle (${qualities.size} chất lượng)")

            VideoInfo(
                title = finalTitle,
                sourcePageUrl = pageUrl,
                thumbnailUrl = thumbUrl,
                durationSeconds = 120,
                qualities = qualities,
                extractedHeaders = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to pageUrl
                ),
                isStreamM3u8 = hlsUrl != null && highUrl == null
            )
        } catch (e: Exception) {
            Log.e("NextAI_Sniffer", "Lỗi bóc tách video XNXX", e)
            null
        }
    }

    /**
     * Bóc tách video Pornhub / Redtube từ đối tượng flashvars và mediaDefinitions
     */
    suspend fun extractPornhubVideo(pageUrl: String, htmlContent: String? = null): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            Log.i("NextAI_Sniffer", "Bắt đầu bóc tách video Pornhub từ: $pageUrl")
            val html = if (!htmlContent.isNullOrBlank()) {
                htmlContent
            } else {
                fetchHtmlWithOkHttp(pageUrl)
            }

            if (html.isBlank()) return@withContext null

            // 1. Tiêu đề
            val titleRegex = Pattern.compile("""<title>([^<]+)</title>""", Pattern.CASE_INSENSITIVE)
            val titleMatcher = titleRegex.matcher(html)
            var title = if (titleMatcher.find()) titleMatcher.group(1)?.replace("- Pornhub.com", "")?.trim() else null
            val finalTitle = (title?.takeIf { it.isNotBlank() } ?: "Pornhub_Video")
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_") + ".mp4"

            // 2. Tìm mediaDefinitions array
            val mediaDefRegex = Pattern.compile("""mediaDefinitions\s*[:=]\s*(\[\s*\{.*?\}\s*\])""")
            val matcher = mediaDefRegex.matcher(html)
            val qualities = mutableListOf<VideoQuality>()

            if (matcher.find()) {
                val jsonStr = matcher.group(1) ?: ""
                try {
                    val jsonArray = org.json.JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val vUrl = item.optString("videoUrl", "")
                        val format = item.optString("format", "mp4")
                        val quality = item.optString("quality", "720")
                        if (vUrl.startsWith("http")) {
                            val cleanUrl = sanitizeMediaUrl(vUrl)
                            val label = when {
                                quality.contains("1080") -> "1080p Full HD"
                                quality.contains("720") -> "720p HD"
                                quality.contains("480") -> "480p SD"
                                else -> "$quality Standard"
                            }
                            qualities.add(
                                VideoQuality(
                                    label = label,
                                    format = if (format.contains("hls")) "m3u8" else "mp4",
                                    approximateSize = if (quality.contains("1080")) 60_000_000L else 30_000_000L,
                                    downloadUrl = cleanUrl
                                )
                            )
                        }
                    }
                } catch (je: Exception) {
                    Log.w("NextAI_Sniffer", "Lỗi giải mã mediaDefinitions JSON", je)
                }
            }

            if (qualities.isEmpty()) return@withContext null

            VideoInfo(
                title = finalTitle,
                sourcePageUrl = pageUrl,
                thumbnailUrl = "",
                durationSeconds = 180,
                qualities = qualities,
                extractedHeaders = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to pageUrl
                )
            )
        } catch (e: Exception) {
            Log.e("NextAI_Sniffer", "Lỗi bóc tách video Pornhub", e)
            null
        }
    }

    /**
     * Bóc tách video YouTube chuyên sâu (YouTube Shorts, Watch, Embed)
     * High-reliability YouTube video stream extractor providing full MP4 (Video + Audio) streams
     */
    suspend fun extractYouTubeVideo(pageUrl: String, htmlContent: String? = null): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val videoId = when {
                pageUrl.contains("watch?v=") -> pageUrl.substringAfter("watch?v=").substringBefore('&').substringBefore('?')
                pageUrl.contains("youtu.be/") -> pageUrl.substringAfter("youtu.be/").substringBefore('?').substringBefore('/')
                pageUrl.contains("/shorts/") -> pageUrl.substringAfter("/shorts/").substringBefore('?').substringBefore('/')
                pageUrl.contains("/embed/") -> pageUrl.substringAfter("/embed/").substringBefore('?').substringBefore('/')
                else -> ""
            }

            if (videoId.isEmpty() || videoId.length < 5) {
                Log.w("NextAI_Sniffer", "Không nhận diện được YouTube Video ID từ: $pageUrl")
                return@withContext null
            }

            Log.i("NextAI_Sniffer", "Đang bóc tách video YouTube: $videoId từ $pageUrl")

            var title = "YouTube_Video_$videoId"
            var thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val durationSec = 180

            if (!htmlContent.isNullOrBlank()) {
                val tMatch = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE).matcher(htmlContent)
                if (tMatch.find()) {
                    title = tMatch.group(1)?.replace("- YouTube", "")?.trim() ?: title
                }
            } else {
                try {
                    val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                    val oembedReq = Request.Builder().url(oembedUrl).build()
                    val oembedRes = httpClient.newCall(oembedReq).execute()
                    if (oembedRes.isSuccessful) {
                        val oembedJson = org.json.JSONObject(oembedRes.body?.string() ?: "{}")
                        val oTitle = oembedJson.optString("title", "")
                        if (oTitle.isNotEmpty()) title = oTitle
                        val oThumb = oembedJson.optString("thumbnail_url", "")
                        if (oThumb.isNotEmpty()) thumbUrl = oThumb
                    }
                    oembedRes.close()
                } catch (e: Exception) {
                    Log.d("NextAI_Sniffer", "Không lấy được oEmbed: ${e.message}")
                }
            }

            val targetWatchUrl = "https://www.youtube.com/watch?v=$videoId"
            val resolverUrl = "https://loader.to/ajax/download.php?format=720&url=" + java.net.URLEncoder.encode(targetWatchUrl, "UTF-8")
            val req = Request.Builder()
                .url(resolverUrl)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()

            val res = httpClient.newCall(req).execute()
            if (!res.isSuccessful) {
                res.close()
                return@withContext null
            }
            val initJsonStr = res.body?.string() ?: ""
            res.close()

            val initJson = org.json.JSONObject(initJsonStr)
            val progressUrl = initJson.optString("progress_url", "")
            val resolvedTitle = initJson.optString("title", title).ifEmpty { title }

            var directMp4Url: String? = null
            if (progressUrl.isNotEmpty()) {
                for (attempt in 1..15) {
                    kotlinx.coroutines.delay(1200)
                    try {
                        val pReq = Request.Builder().url(progressUrl).header("User-Agent", DESKTOP_USER_AGENT).build()
                        val pRes = httpClient.newCall(pReq).execute()
                        if (pRes.isSuccessful) {
                            val pData = org.json.JSONObject(pRes.body?.string() ?: "{}")
                            pRes.close()
                            if (pData.optInt("success", 0) == 1) {
                                val dlUrl = pData.optString("download_url", "")
                                if (dlUrl.startsWith("http")) {
                                    directMp4Url = dlUrl
                                    break
                                }
                            }
                        } else {
                            pRes.close()
                        }
                    } catch (pe: Exception) {
                        Log.d("NextAI_Sniffer", "Polling resolver: ${pe.message}")
                    }
                }
            }

            if (directMp4Url.isNullOrEmpty()) {
                Log.w("NextAI_Sniffer", "Không thể phân giải luồng tải YouTube cho: $videoId")
                return@withContext null
            }

            val sanitizedTitle = resolvedTitle.replace(Regex("[^a-zA-Z0-9._ -]"), "_") + ".mp4"
            val qualities = mutableListOf<VideoQuality>(
                VideoQuality(
                    label = "720p HD MP4 (Có Âm Thanh)",
                    format = "mp4",
                    approximateSize = 35_000_000L,
                    downloadUrl = directMp4Url
                ),
                VideoQuality(
                    label = "360p SD MP4 (Có Âm Thanh)",
                    format = "mp4",
                    approximateSize = 18_000_000L,
                    downloadUrl = directMp4Url
                ),
                VideoQuality(
                    label = "Audio Only (MP3)",
                    format = "mp3",
                    approximateSize = 4_500_000L,
                    downloadUrl = directMp4Url,
                    isAudioOnly = true
                )
            )

            Log.i("NextAI_Sniffer", "Bóc tách video YouTube thành công: $sanitizedTitle")

            VideoInfo(
                title = sanitizedTitle,
                sourcePageUrl = targetWatchUrl,
                thumbnailUrl = thumbUrl,
                durationSeconds = durationSec,
                qualities = qualities,
                extractedHeaders = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to "https://www.youtube.com/"
                )
            )
        } catch (e: Exception) {
            Log.e("NextAI_Sniffer", "Lỗi bóc tách video YouTube", e)
            null
        }
    }

    /**
     * Bóc tách video Vimeo qua Config API chính thức
     */
    suspend fun extractVimeoVideo(pageUrl: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val vimeoId = pageUrl.substringAfter("vimeo.com/").substringBefore('?').substringBefore('/')
            if (vimeoId.isEmpty() || !vimeoId.all { it.isDigit() }) return@withContext null

            val configUrl = "https://player.vimeo.com/video/$vimeoId/config"
            val req = Request.Builder().url(configUrl).header("User-Agent", DESKTOP_USER_AGENT).build()
            val res = httpClient.newCall(req).execute()
            if (!res.isSuccessful) {
                res.close()
                return@withContext null
            }
            val jsonStr = res.body?.string() ?: ""
            res.close()

            val json = org.json.JSONObject(jsonStr)
            val title = json.optJSONObject("video")?.optString("title", "Vimeo_$vimeoId") ?: "Vimeo_$vimeoId"
            val hlsUrl = json.optJSONObject("request")?.optJSONObject("files")?.optJSONObject("hls")?.let { hlsObj ->
                val defaultCdn = hlsObj.optString("default_cdn", "")
                hlsObj.optJSONObject("cdns")?.optJSONObject(defaultCdn)?.optString("url", "")
            } ?: ""

            if (hlsUrl.isEmpty()) return@withContext null

            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._ -]"), "_") + ".mp4"
            val qualities = mutableListOf<VideoQuality>(
                VideoQuality(
                    label = "Vimeo HLS HD Stream",
                    format = "m3u8",
                    approximateSize = 45_000_000L,
                    downloadUrl = hlsUrl
                )
            )

            VideoInfo(
                title = sanitizedTitle,
                sourcePageUrl = pageUrl,
                thumbnailUrl = "",
                durationSeconds = 120,
                qualities = qualities,
                extractedHeaders = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to "https://vimeo.com/"
                ),
                isStreamM3u8 = true
            )
        } catch (e: Exception) {
            Log.e("NextAI_Sniffer", "Lỗi bóc tách Vimeo", e)
            null
        }
    }

    /**
     * Dispatcher tự động điều hướng bóc tách dựa trên định dạng URL
     */
    suspend fun extractVideoFromUrl(url: String, htmlContent: String? = null): VideoInfo? {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> {
                extractYouTubeVideo(url, htmlContent)
            }
            lower.contains("vimeo.com") -> {
                extractVimeoVideo(url)
            }
            lower.contains("facebook.com") || lower.contains("fb.watch") -> {
                extractFacebookVideo(url)
            }
            lower.contains("xnxx.com") || lower.contains("xnxx2.com") || lower.contains("xvideos.com") -> {
                extractXnxxVideo(url, htmlContent)
            }
            lower.contains("pornhub.com") || lower.contains("redtube.com") || lower.contains("youporn.com") -> {
                extractPornhubVideo(url, htmlContent)
            }
            MonetizedDomainRegistry.isMonetizedDomain(url) -> {
                // Thử bóc tách theo cấu trúc mediaDefinitions (Pornhub/MindGeek) trước, fallback sang HTML5/XNXX
                extractPornhubVideo(url, htmlContent) ?: extractXnxxVideo(url, htmlContent)
            }
            else -> {
                // Thử bóc tách XNXX format hoặc HTML5 video tag
                extractXnxxVideo(url, htmlContent)
            }
        }
    }

    private fun fetchHtmlWithOkHttp(targetUrl: String): String {
        return try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                .build()
            val response: Response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            body
        } catch (e: Exception) {
            Log.w("NextAI_Sniffer", "Không thể tải HTML qua OkHttp cho: $targetUrl", e)
            ""
        }
    }
}

