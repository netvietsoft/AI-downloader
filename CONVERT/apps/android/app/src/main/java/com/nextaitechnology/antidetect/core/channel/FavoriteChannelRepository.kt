package com.nextaitechnology.antidetect.core.channel

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

data class NormalizedChannelTarget(
    val platform: String, // "facebook" | "instagram" | "youtube"
    val inputUrl: String,
    val normalizedPageUrl: String,
    val reelsTabUrl: String,
    val pageSlug: String,
    val isSingleReel: Boolean,
    val singleReelId: String?
)

data class ParsedVideoTextMeta(
    val title: String,
    val caption: String,
    val hashtags: List<String>
)

data class DashManifestResult(
    val bestVideoUrl: String? = null,
    val bestVideoWidth: Int = 0,
    val bestVideoHeight: Int = 0,
    val bestVideoBitrate: Int = 0,
    val bestAudioUrl: String? = null,
    val bestAudioBitrate: Int = 0
)

data class ChannelReelCandidate(
    val reelId: String,
    val reelUrl: String,
    val platform: String,
    var title: String = "",
    var caption: String = "",
    var hashtags: List<String> = emptyList(),
    var viewCountText: String = "",
    var durationSec: Int = 0,
    var hdUrl: String? = null,
    var sdUrl: String? = null,
    var dashVideoUrl: String? = null,
    var dashVideoWidth: Int = 0,
    var dashVideoHeight: Int = 0
) {
    val bestStreamUrl: String?
        get() = hdUrl ?: dashVideoUrl ?: sdUrl
}

data class FavoriteChannel(
    val id: String,
    val platform: String,
    val url: String,
    val normalizedPageUrl: String,
    val reelsTabUrl: String,
    val pageSlug: String,
    var title: String,
    var preferQuality: String = "1080p",
    var lastCheckedAt: Long = 0L,
    var lastDiscoveredCount: Int = 0,
    val downloadedReelIds: MutableSet<String> = mutableSetOf()
)

/**
 * Hệ thống Chuẩn Hóa Kênh, Bóc Tách Hàng Loạt Reels/Shorts (FB, IG, YT) & Quản Lý Kênh Yêu Thích
 * Ported directly from D:\Decompiler\App\Downloader\download\src\services\facebookReels.service.ts
 *
 * @author NextAI Technology Core Team
 */
class FavoriteChannelRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nextai_favorite_channels", Context.MODE_PRIVATE)

    fun getFavorites(): List<FavoriteChannel> {
        val raw = prefs.getString(KEY_FAVORITES_JSON, "[]") ?: "[]"
        val result = mutableListOf<FavoriteChannel>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val dlIds = mutableSetOf<String>()
                val idsArr = obj.optJSONArray("downloadedReelIds")
                if (idsArr != null) {
                    for (j in 0 until idsArr.length()) {
                        val idVal = idsArr.optString(j)
                        if (idVal.isNotBlank()) dlIds.add(idVal)
                    }
                }
                result.add(
                    FavoriteChannel(
                        id = obj.optString("id"),
                        platform = obj.optString("platform", "facebook"),
                        url = obj.optString("url"),
                        normalizedPageUrl = obj.optString("normalizedPageUrl"),
                        reelsTabUrl = obj.optString("reelsTabUrl"),
                        pageSlug = obj.optString("pageSlug"),
                        title = obj.optString("title", obj.optString("pageSlug")),
                        preferQuality = obj.optString("preferQuality", "1080p"),
                        lastCheckedAt = obj.optLong("lastCheckedAt", 0L),
                        lastDiscoveredCount = obj.optInt("lastDiscoveredCount", 0),
                        downloadedReelIds = dlIds
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun saveFavorites(list: List<FavoriteChannel>) {
        val arr = JSONArray()
        for (f in list) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("platform", f.platform)
                put("url", f.url)
                put("normalizedPageUrl", f.normalizedPageUrl)
                put("reelsTabUrl", f.reelsTabUrl)
                put("pageSlug", f.pageSlug)
                put("title", f.title)
                put("preferQuality", f.preferQuality)
                put("lastCheckedAt", f.lastCheckedAt)
                put("lastDiscoveredCount", f.lastDiscoveredCount)
                val idsArr = JSONArray()
                f.downloadedReelIds.forEach { idsArr.put(it) }
                put("downloadedReelIds", idsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES_JSON, arr.toString()).apply()
    }

    fun addOrUpdateFavorite(rawUrl: String, customTitle: String? = null): FavoriteChannel {
        val target = normalizeChannelInput(rawUrl)
        val id = "fav_${target.pageSlug}"
        val current = getFavorites().toMutableList()
        val existingIdx = current.indexOfFirst { it.id == id || it.pageSlug == target.pageSlug }

        val favorite = if (existingIdx >= 0) {
            val ex = current[existingIdx]
            ex.copy(
                url = target.inputUrl,
                normalizedPageUrl = target.normalizedPageUrl,
                reelsTabUrl = target.reelsTabUrl,
                platform = target.platform,
                title = customTitle?.takeIf { it.isNotBlank() } ?: ex.title
            ).also { current[existingIdx] = it }
        } else {
            FavoriteChannel(
                id = id,
                platform = target.platform,
                url = target.inputUrl,
                normalizedPageUrl = target.normalizedPageUrl,
                reelsTabUrl = target.reelsTabUrl,
                pageSlug = target.pageSlug,
                title = customTitle?.takeIf { it.isNotBlank() } ?: target.pageSlug
            ).also { current.add(0, it) }
        }

        saveFavorites(current)
        return favorite
    }

    fun removeFavorite(id: String): Boolean {
        val current = getFavorites().toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) saveFavorites(current)
        return removed
    }

    fun recordChannelScanResult(pageSlug: String, discoveredCount: Int, newlyDownloadedIds: Collection<String>) {
        val current = getFavorites().toMutableList()
        val fav = current.firstOrNull { it.pageSlug == pageSlug }
        if (fav != null) {
            fav.lastCheckedAt = System.currentTimeMillis()
            fav.lastDiscoveredCount = discoveredCount
            fav.downloadedReelIds.addAll(newlyDownloadedIds)
            saveFavorites(current)
        }
        val globalSet = getDownloadedIdsForSlug(pageSlug).toMutableSet()
        globalSet.addAll(newlyDownloadedIds)
        prefs.edit().putStringSet("dl_ids_$pageSlug", globalSet).apply()
    }

    fun getDownloadedIdsForSlug(pageSlug: String): Set<String> {
        val fromSet = prefs.getStringSet("dl_ids_$pageSlug", emptySet()) ?: emptySet()
        val fromFav = getFavorites().firstOrNull { it.pageSlug == pageSlug }?.downloadedReelIds ?: emptySet()
        return fromSet + fromFav
    }

    companion object {
        private const val KEY_FAVORITES_JSON = "pref_favorite_channels_json"

        fun unescapeMediaUrl(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw.trim()
                .replace("\\/", "/")
                .replace("\\u0026", "&", ignoreCase = true)
                .replace("\\u0025", "%", ignoreCase = true)
                .replace("\\u003d", "=", ignoreCase = true)
                .replace("\\u003c", "<", ignoreCase = true)
                .replace("\\u003e", ">", ignoreCase = true)
                .replace("\\\"", "\"")
                .replace("&amp;", "&")
        }

        fun stripByteRangeParams(rawUrl: String?): String {
            return unescapeMediaUrl(rawUrl)
                .replace(Regex("([?&])bytestart=\\d+&?"), "$1")
                .replace(Regex("([?&])byteend=\\d+&?"), "$1")
                .replace("?&", "?")
                .trimEnd('?', '&')
        }

        fun sanitizeFolderSlug(raw: String): String {
            val cleaned = raw.trim()
                .replace(Regex("""[<>:"/\\|?*\x00-\x1F]"""), "_")
                .replace(Regex("""\s+"""), "_")
                .trim('.', '_', ' ')
            return cleaned.ifEmpty { "channel_videos" }
        }

        fun extractCaptionTitleAndHashtags(
            rawCaption: String?,
            rawTitle: String? = null,
            rawTags: List<String>? = null
        ): ParsedVideoTextMeta {
            fun cleanStr(s: String?): String =
                (s ?: "").replace(Regex("""[\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF]"""), "").trim()

            val cap = cleanStr(rawCaption)
            var tit = cleanStr(rawTitle)

            if (tit.startsWith("Video by ", ignoreCase = true)) {
                tit = ""
            }
            if (tit.isNotEmpty() && Regex("""\bviews\b.*\|.*\|""", RegexOption.IGNORE_CASE).containsMatchIn(tit)) {
                val parts = tit.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    tit = parts[1]
                }
            }

            val combinedSource = listOf(cap, tit).filter { it.isNotEmpty() }.joinToString("\n")
            val hashtagRegex = Regex("""#[\p{L}\p{N}_]+""")
            val seenLower = mutableSetOf<String>()
            val hashtags = mutableListOf<String>()

            fun pushTag(tagRaw: String) {
                val cleaned = tagRaw.trim().trimStart('#')
                if (cleaned.isEmpty()) return
                val normalized = cleaned.replace(Regex("""[^\p{L}\p{N}_]"""), "")
                if (normalized.isEmpty()) return
                val tag = "#$normalized"
                val low = tag.lowercase(Locale.ROOT)
                if (seenLower.add(low)) {
                    hashtags.add(tag)
                }
            }

            hashtagRegex.findAll(combinedSource).forEach { pushTag(it.value) }
            rawTags?.forEach { if (it.isNotBlank()) pushTag(it) }

            val baseText = cap.ifEmpty { tit }
            val bodyWithoutHashtags = baseText
                .replace(hashtagRegex, " ")
                .replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF]+"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim(' ', '-', '–', '—', '•', '|', ',', ':', ';')

            val title = bodyWithoutHashtags.ifEmpty {
                if (hashtags.isNotEmpty()) hashtags.joinToString(" ") else ""
            }
            val fullCaption = baseText.ifEmpty {
                listOf(bodyWithoutHashtags, hashtags.joinToString(" ")).filter { it.isNotEmpty() }.joinToString("\n")
            }

            return ParsedVideoTextMeta(title = title, caption = fullCaption, hashtags = hashtags)
        }

        fun sanitizeForFileName(input: String): String {
            return input
                .replace(Regex("""[\u0000-\u001F\u007F-\u009F\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF]"""), " ")
                .replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF]+"""), " ")
                .replace(Regex("""[<>:"/\\|?*]"""), " - ")
                .replace(Regex("""\s+"""), " ")
                .replace(Regex("""(?:\s*-\s*)+"""), " - ")
                .trim(' ', '.', '-', '_')
        }

        fun buildVideoFileName(
            reelId: String,
            rawCaption: String? = null,
            rawTitle: String? = null,
            rawTags: List<String>? = null
        ): String {
            val safeId = reelId.replace(Regex("""[^A-Za-z0-9_-]"""), "").ifEmpty { "video" }
            val meta = extractCaptionTitleAndHashtags(rawCaption, rawTitle, rawTags)

            var cleanBody = sanitizeForFileName(meta.title)
            if (cleanBody.length > 85) {
                cleanBody = cleanBody.take(85).substringBeforeLast(' ', cleanBody.take(85)).trim()
            }

            var cleanTags = sanitizeForFileName(meta.hashtags.take(6).joinToString(" "))
            if (cleanTags.length > 55) {
                cleanTags = cleanTags.take(55).substringBeforeLast(' ', cleanTags.take(55)).trim()
            }

            val parts = mutableListOf<String>()
            if (cleanBody.isNotEmpty()) parts.add(cleanBody)
            if (cleanTags.isNotEmpty() && !cleanTags.equals(cleanBody, ignoreCase = true)) {
                parts.add(cleanTags)
            }

            if (parts.isEmpty()) {
                return "$safeId.mp4"
            }

            val prefix = parts.joinToString(" - ").trim(' ', '.', '-', '_')
            return "$prefix [$safeId].mp4"
        }

        fun extractReelIdFromFileName(fileName: String): String {
            val base = fileName.substringAfterLast('/').substringAfterLast('\\')
            if (!base.lowercase(Locale.ROOT).endsWith(".mp4")) return ""
            val withoutExt = base.dropLast(4).trim()
            val m = Regex("""\[([A-Za-z0-9_-]{6,30})\]$""").find(withoutExt)
            return m?.groupValues?.get(1) ?: withoutExt
        }

        fun normalizeChannelInput(rawInput: String): NormalizedChannelTarget {
            var trimmed = rawInput.trim()
            require(trimmed.isNotEmpty()) { "URL Page / Kênh không được để trống." }

            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                val low = trimmed.lowercase(Locale.ROOT)
                trimmed = when {
                    low.contains("facebook.com") || low.contains("fb.watch") || low.contains("fb.com") ||
                    low.contains("instagram.com") || low.contains("youtube.com") || low.contains("youtu.be") -> {
                        "https://" + trimmed.trimStart('/')
                    }
                    trimmed.startsWith("@") -> {
                        "https://www.youtube.com/$trimmed"
                    }
                    else -> {
                        "https://www.facebook.com/" + trimmed.trimStart('/')
                    }
                }
            }

            val uri = try {
                URI(trimmed)
            } catch (e: Exception) {
                throw IllegalArgumentException("URL không hợp lệ: $rawInput")
            }

            val host = (uri.host ?: "").lowercase(Locale.ROOT)
            val rawPath = (uri.path ?: "").trimEnd('/')
            val query = uri.rawQuery ?: ""

            fun getQueryParam(name: String): String? {
                return query.split("&")
                    .mapNotNull { part ->
                        val kv = part.split("=", limit = 2)
                        if (kv.size == 2 && kv[0] == name) kv[1] else null
                    }
                    .firstOrNull()
            }

            // 1. YOUTUBE
            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                if (host.contains("youtu.be")) {
                    val vid = rawPath.trimStart('/').substringBefore('/')
                    require(vid.isNotEmpty()) { "URL YouTube rút gọn không hợp lệ." }
                    val watchUrl = "https://www.youtube.com/watch?v=$vid"
                    return NormalizedChannelTarget(
                        platform = "youtube",
                        inputUrl = rawInput.trim(),
                        normalizedPageUrl = watchUrl,
                        reelsTabUrl = watchUrl,
                        pageSlug = "yt_single_videos",
                        isSingleReel = true,
                        singleReelId = vid
                    )
                }

                val watchVid = getQueryParam("v")
                val shortsMatch = Regex("""^/shorts/([A-Za-z0-9_-]{6,20})""", RegexOption.IGNORE_CASE).find(rawPath)
                if (shortsMatch != null || !watchVid.isNullOrEmpty()) {
                    val vid = shortsMatch?.groupValues?.get(1) ?: watchVid!!
                    val singleUrl = if (shortsMatch != null) {
                        "https://www.youtube.com/shorts/$vid"
                    } else {
                        "https://www.youtube.com/watch?v=$vid"
                    }
                    return NormalizedChannelTarget(
                        platform = "youtube",
                        inputUrl = rawInput.trim(),
                        normalizedPageUrl = singleUrl,
                        reelsTabUrl = singleUrl,
                        pageSlug = "yt_single_videos",
                        isSingleReel = true,
                        singleReelId = vid
                    )
                }

                val parts = rawPath.split("/").filter { it.isNotEmpty() }
                require(parts.isNotEmpty()) { "Vui lòng nhập URL Kênh YouTube cụ thể." }

                val channelBase: String
                val rawSlug: String
                var subTab = "shorts"

                if (parts[0].startsWith("@")) {
                    channelBase = "https://www.youtube.com/${parts[0]}"
                    rawSlug = parts[0].drop(1)
                    if (parts.size >= 2 && parts[1].lowercase(Locale.ROOT) in setOf("shorts", "videos", "streams")) {
                        subTab = parts[1].lowercase(Locale.ROOT)
                    }
                } else if (parts[0].lowercase(Locale.ROOT) in setOf("channel", "c", "user") && parts.size >= 2) {
                    channelBase = "https://www.youtube.com/${parts[0]}/${parts[1]}"
                    rawSlug = parts[1]
                    if (parts.size >= 3 && parts[2].lowercase(Locale.ROOT) in setOf("shorts", "videos", "streams")) {
                        subTab = parts[2].lowercase(Locale.ROOT)
                    }
                } else {
                    channelBase = "https://www.youtube.com/${parts[0]}"
                    rawSlug = parts[0]
                    if (parts.size >= 2 && parts[1].lowercase(Locale.ROOT) in setOf("shorts", "videos", "streams")) {
                        subTab = parts[1].lowercase(Locale.ROOT)
                    }
                }

                val cleanSlug = sanitizeFolderSlug("yt_$rawSlug")
                return NormalizedChannelTarget(
                    platform = "youtube",
                    inputUrl = rawInput.trim(),
                    normalizedPageUrl = "$channelBase/",
                    reelsTabUrl = "$channelBase/$subTab",
                    pageSlug = cleanSlug,
                    isSingleReel = false,
                    singleReelId = null
                )
            }

            // 2. INSTAGRAM
            if (host.contains("instagram.com")) {
                val singleMatch = Regex("""^/(?:reel|reels|p|tv)/([A-Za-z0-9_-]{5,25})""", RegexOption.IGNORE_CASE).find(rawPath)
                if (singleMatch != null) {
                    val code = singleMatch.groupValues[1]
                    val singleUrl = "https://www.instagram.com/reel/$code/"
                    return NormalizedChannelTarget(
                        platform = "instagram",
                        inputUrl = rawInput.trim(),
                        normalizedPageUrl = singleUrl,
                        reelsTabUrl = singleUrl,
                        pageSlug = "ig_single_reels",
                        isSingleReel = true,
                        singleReelId = code
                    )
                }

                val parts = rawPath.split("/").filter { it.isNotEmpty() }
                require(parts.isNotEmpty() && parts[0].lowercase(Locale.ROOT) !in setOf("explore", "accounts", "direct", "stories")) {
                    "Vui lòng nhập URL Kênh/Profile Instagram cụ thể."
                }
                val username = parts[0].trimStart('@')
                val cleanSlug = sanitizeFolderSlug("ig_$username")
                return NormalizedChannelTarget(
                    platform = "instagram",
                    inputUrl = rawInput.trim(),
                    normalizedPageUrl = "https://www.instagram.com/$username/",
                    reelsTabUrl = "https://www.instagram.com/$username/reels/",
                    pageSlug = cleanSlug,
                    isSingleReel = false,
                    singleReelId = null
                )
            }

            // 3. FACEBOOK
            val reelMatch = Regex("""/reel/(\d{8,25})""", RegexOption.IGNORE_CASE).find(rawPath)
                ?: Regex("""/videos/(\d{8,25})""", RegexOption.IGNORE_CASE).find(rawPath)
            val watchVid = getQueryParam("v")
            if (reelMatch != null || (!watchVid.isNullOrEmpty() && watchVid.all { it.isDigit() } && watchVid.length >= 8)) {
                val rid = reelMatch?.groupValues?.get(1) ?: watchVid!!
                val singleUrl = "https://www.facebook.com/reel/$rid/"
                return NormalizedChannelTarget(
                    platform = "facebook",
                    inputUrl = rawInput.trim(),
                    normalizedPageUrl = singleUrl,
                    reelsTabUrl = singleUrl,
                    pageSlug = "single_reels",
                    isSingleReel = true,
                    singleReelId = rid
                )
            }

            if (rawPath.equals("/profile.php", ignoreCase = true)) {
                val profileId = getQueryParam("id") ?: "unknown"
                val basePage = "https://www.facebook.com/profile.php?id=$profileId"
                return NormalizedChannelTarget(
                    platform = "facebook",
                    inputUrl = rawInput.trim(),
                    normalizedPageUrl = basePage,
                    reelsTabUrl = "$basePage&sk=reels_tab",
                    pageSlug = sanitizeFolderSlug("profile_$profileId"),
                    isSingleReel = false,
                    singleReelId = null
                )
            }

            val peopleMatch = Regex("""^/people/([^/]+)/(\d{6,25})""", RegexOption.IGNORE_CASE).find(rawPath)
            if (peopleMatch != null) {
                val namePart = peopleMatch.groupValues[1]
                val idPart = peopleMatch.groupValues[2]
                val basePage = "https://www.facebook.com/profile.php?id=$idPart"
                return NormalizedChannelTarget(
                    platform = "facebook",
                    inputUrl = rawInput.trim(),
                    normalizedPageUrl = basePage,
                    reelsTabUrl = "$basePage&sk=reels_tab",
                    pageSlug = sanitizeFolderSlug("${namePart}_$idPart"),
                    isSingleReel = false,
                    singleReelId = null
                )
            }

            val segments = rawPath.split("/").filter { it.isNotEmpty() }
            require(segments.isNotEmpty()) { "Vui lòng nhập URL cụ thể của Page Facebook." }
            var pageSegment = segments[0]
            if (pageSegment.equals("pg", ignoreCase = true) && segments.size >= 2) {
                pageSegment = segments[1]
            }

            val cleanSlug = sanitizeFolderSlug(pageSegment)
            return NormalizedChannelTarget(
                platform = "facebook",
                inputUrl = rawInput.trim(),
                normalizedPageUrl = "https://www.facebook.com/$pageSegment/",
                reelsTabUrl = "https://www.facebook.com/$pageSegment/reels/",
                pageSlug = cleanSlug,
                isSingleReel = false,
                singleReelId = null
            )
        }

        fun parseDashManifestXml(rawXml: String?): DashManifestResult {
            if (rawXml.isNullOrBlank()) return DashManifestResult()
            val xml = rawXml
                .replace("\\u003C", "<", ignoreCase = true)
                .replace("\\u003E", ">", ignoreCase = true)
                .replace("\\u0026", "&", ignoreCase = true)
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("&amp;", "&")

            val repPattern = Pattern.compile("""<Representation\b([^>]*)>([\s\S]*?)</Representation>""", Pattern.CASE_INSENSITIVE)
            val matcher = repPattern.matcher(xml)

            var bestVideoScore = -1L
            var bestVideoUrl: String? = null
            var bestVideoWidth = 0
            var bestVideoHeight = 0
            var bestVideoBitrate = 0

            var bestAudioBitrate = -1
            var bestAudioUrl: String? = null

            while (matcher.find()) {
                val attrs = matcher.group(1) ?: ""
                val inner = matcher.group(2) ?: ""
                val baseMatcher = Pattern.compile("""<BaseURL>([^<]+)</BaseURL>""", Pattern.CASE_INSENSITIVE).matcher(inner)
                if (!baseMatcher.find()) continue

                val url = stripByteRangeParams(baseMatcher.group(1)?.trim())
                if (!url.startsWith("http")) continue

                val width = Regex("""\bwidth="(\d+)"""").find(attrs)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val height = Regex("""\bheight="(\d+)"""").find(attrs)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val bandwidth = Regex("""\bbandwidth="(\d+)"""").find(attrs)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val mime = (Regex("""\bmimeType="([^"]+)"""").find(attrs)?.groupValues?.get(1) ?: "").lowercase(Locale.ROOT)
                val codecs = (Regex("""\bcodecs="([^"]+)"""").find(attrs)?.groupValues?.get(1) ?: "").lowercase(Locale.ROOT)

                val isAudio = mime.startsWith("audio/") ||
                        codecs.startsWith("mp4a") ||
                        codecs.contains("opus") ||
                        (width == 0 && height == 0 && (attrs.lowercase(Locale.ROOT).contains("audio") || url.contains("audio")))

                if (isAudio) {
                    val aScore = if (bandwidth > 0) bandwidth else 64000
                    if (aScore > bestAudioBitrate) {
                        bestAudioBitrate = aScore
                        bestAudioUrl = url
                    }
                } else {
                    val effectiveDim = maxOf(width, height).toLong()
                    val codecBonus = if (codecs.startsWith("avc1")) 50_000L else 0L
                    val score = effectiveDim * 10_000_000L + bandwidth.toLong() + codecBonus
                    if (score > bestVideoScore) {
                        bestVideoScore = score
                        bestVideoUrl = url
                        bestVideoWidth = width
                        bestVideoHeight = height
                        bestVideoBitrate = bandwidth
                    }
                }
            }

            return DashManifestResult(
                bestVideoUrl = bestVideoUrl,
                bestVideoWidth = bestVideoWidth,
                bestVideoHeight = bestVideoHeight,
                bestVideoBitrate = bestVideoBitrate,
                bestAudioUrl = bestAudioUrl,
                bestAudioBitrate = if (bestAudioBitrate > 0) bestAudioBitrate else 0
            )
        }

        /**
         * Quét và trích xuất danh sách Reels / Shorts từ HTML & JSON Payload của trang Kênh / Page
         */
        fun harvestChannelReelsFromHtml(pageUrl: String, htmlContent: String): List<ChannelReelCandidate> {
            if (htmlContent.isBlank()) return emptyList()
            val lowerUrl = pageUrl.lowercase(Locale.ROOT)
            val platform = when {
                lowerUrl.contains("instagram.com") -> "instagram"
                lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> "youtube"
                else -> "facebook"
            }

            val map = LinkedHashMap<String, ChannelReelCandidate>()

            fun upsert(
                id: String,
                url: String,
                caption: String? = null,
                hd: String? = null,
                sd: String? = null,
                dash: String? = null,
                w: Int = 0,
                h: Int = 0
            ) {
                if (id.isBlank()) return
                val existing = map.getOrPut(id) {
                    ChannelReelCandidate(reelId = id, reelUrl = url, platform = platform)
                }
                if (!caption.isNullOrBlank() && (existing.caption.isBlank() || caption.length > existing.caption.length)) {
                    val meta = extractCaptionTitleAndHashtags(caption)
                    existing.caption = meta.caption
                    existing.title = meta.title
                    existing.hashtags = meta.hashtags
                }
                if (!hd.isNullOrBlank() && existing.hdUrl.isNullOrBlank()) {
                    existing.hdUrl = stripByteRangeParams(hd)
                }
                if (!sd.isNullOrBlank() && existing.sdUrl.isNullOrBlank()) {
                    existing.sdUrl = stripByteRangeParams(sd)
                }
                if (!dash.isNullOrBlank() && existing.dashVideoUrl.isNullOrBlank()) {
                    existing.dashVideoUrl = stripByteRangeParams(dash)
                    existing.dashVideoWidth = w
                    existing.dashVideoHeight = h
                }
            }

            if (platform == "facebook") {
                // 1. Bóc tách các cặp video_id + stream URL từ Relay JSON payload
                val pairRegex = Pattern.compile(
                    """"(?:video_id|id)"\s*:\s*"(\d{8,25})"[\s\S]{1,3500}?"(browser_native_sd_url|playable_url)"\s*:\s*"(https?:[^"]+)"(?:[\s\S]{1,1500}?"(browser_native_hd_url|playable_url_quality_hd)"\s*:\s*"(https?:[^"]+)")?"""
                )
                val pm = pairRegex.matcher(htmlContent)
                while (pm.find()) {
                    val vid = pm.group(1) ?: continue
                    val sd = pm.group(3)
                    val hd = pm.group(5)
                    upsert(vid, "https://www.facebook.com/reel/$vid/", hd = hd, sd = sd)
                }

                // 2. Bóc tách các link /reel/(\d{8,25}) trên lưới Page Reels
                val linkRegex = Pattern.compile("""/reel/(\d{8,25})""")
                val lm = linkRegex.matcher(htmlContent)
                while (lm.find()) {
                    val vid = lm.group(1) ?: continue
                    upsert(vid, "https://www.facebook.com/reel/$vid/")
                }
            } else if (platform == "instagram") {
                // 1. Bóc tách shortcode + video_url / video_versions
                val igJsonRegex = Pattern.compile(
                    """"(?:code|shortcode)"\s*:\s*"([A-Za-z0-9_-]{6,20})"[\s\S]{1,2500}?"(?:video_url|url)"\s*:\s*"(https?:[^"]+\.mp4[^"]*)""""
                )
                val im = igJsonRegex.matcher(htmlContent)
                while (im.find()) {
                    val code = im.group(1) ?: continue
                    val vUrl = im.group(2)
                    upsert(code, "https://www.instagram.com/reel/$code/", hd = vUrl)
                }

                // 2. Bóc tách các thẻ a href="/reel/CODE/"
                val igLinkRegex = Pattern.compile("""/(?:reel|p)/([A-Za-z0-9_-]{6,20})""")
                val ilm = igLinkRegex.matcher(htmlContent)
                while (ilm.find()) {
                    val code = ilm.group(1) ?: continue
                    upsert(code, "https://www.instagram.com/reel/$code/")
                }
            } else if (platform == "youtube") {
                // 1. Bóc tách từ link /shorts/VIDEO_ID hoặc watch?v=VIDEO_ID
                val ytShortsRegex = Pattern.compile("""/shorts/([A-Za-z0-9_-]{11})""")
                val ysm = ytShortsRegex.matcher(htmlContent)
                while (ysm.find()) {
                    val vid = ysm.group(1) ?: continue
                    upsert(vid, "https://www.youtube.com/shorts/$vid")
                }

                val ytVidRegex = Pattern.compile(""""videoId"\s*:\s*"([A-Za-z0-9_-]{11})"""")
                val yvm = ytVidRegex.matcher(htmlContent)
                while (yvm.find()) {
                    val vid = yvm.group(1) ?: continue
                    upsert(vid, "https://www.youtube.com/shorts/$vid")
                }
            }

            return map.values.toList()
        }
    }
}
