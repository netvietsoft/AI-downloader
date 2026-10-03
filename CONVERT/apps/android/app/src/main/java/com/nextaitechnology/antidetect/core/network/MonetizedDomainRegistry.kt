package com.nextaitechnology.antidetect.core.network

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Quản Lý & Nhận Diện Danh Sách 1822+ Website Video Giới Hạn / Thu Phí (MonetizedDomainRegistry)
 * Nạp từ danh sách web: Bao_cao_web_video.csv (assets/monetized_domains.txt).
 *
 * Chính sách:
 * - Người dùng được phép tải miễn phí 1 video đầu tiên.
 * - Từ video thứ 2 trở đi trên bất kỳ website nào trong danh sách này:
 *   Khi bắt được video hoặc người dùng yêu cầu tải, lập tức kích hoạt Payment Wall bắt buộc đăng ký Pro Subscription.
 *
 * @author NextAI Technology Core Team
 */
object MonetizedDomainRegistry {

    private const val TAG = "MonetizedDomains"
    private val domainSet = HashSet<String>()
    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                context.assets.open("monetized_domains.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                        lines.forEach { rawLine ->
                            val clean = cleanDomainString(rawLine)
                            if (clean.isNotEmpty() && !clean.startsWith("#")) {
                                domainSet.add(clean)
                            }
                        }
                    }
                }
                isInitialized = true
                Log.i(TAG, "Đã khởi tạo thành công ${domainSet.size} domain từ assets/monetized_domains.txt")
            } catch (e: Exception) {
                Log.w(TAG, "Không thể đọc assets/monetized_domains.txt, sử dụng danh sách fallback", e)
                initFallback()
            }
        }
    }

    private fun initFallback() {
        val fallbackDomains = listOf(
            "pornhub.com", "pornhub.org", "xvideos.com", "xvideos.es", "xhamster.com", "xhamsterlive.com",
            "xhamster19.com", "xhamster46.desi", "xhamster.desi", "xnxx.com", "xnxx2.com", "xnxx.es",
            "spankbang.com", "eporner.com", "chaturbate.com", "stripchat.com", "stripchatgirls.com",
            "livejasmin.com", "onlyfans.com", "erome.com", "faphouse.com", "faphouse2.com", "xhaccess.com",
            "dmm.co.jp", "noodlemagazine.com", "ixxx.com", "simpcity.cr", "tnaflix.com", "qorno.com",
            "missav.ws", "missav.ai", "missav.live", "cityheaven.net", "marzaent.com", "sowve.com",
            "wvdme.com", "rule34.xxx", "viiukuhe.com", "xgroovy.com", "youporn.com", "pimpbunny.com",
            "njavtv.com", "nn125.com", "pawchive.pw", "thequizgo.com", "beeg.com", "theporndude.com",
            "gigglemagnetismunaired.com", "redgifs.com", "dlsite.com", "redtube.com", "youjizz.com", "txxx.com"
        )
        domainSet.addAll(fallbackDomains)
        isInitialized = true
    }

    /**
     * Kiểm tra xem URL hoặc Domain có thuộc danh sách website monetization giới hạn tải hay không
     */
    fun isMonetizedDomain(urlOrHost: String?): Boolean {
        if (urlOrHost.isNullOrBlank()) return false
        val cleanHost = extractHost(urlOrHost)
        if (cleanHost.isEmpty()) return false

        // 1. Kiểm tra khớp chính xác host
        if (domainSet.contains(cleanHost)) return true

        // 2. Kiểm tra khớp domain gốc (subdomain match)
        // Ví dụ: www.xnxx2.com -> xnxx2.com, m.pornhub.com -> pornhub.com, v.eporner.com -> eporner.com
        val parts = cleanHost.split(".")
        if (parts.size >= 2) {
            for (i in 0 until parts.size - 1) {
                val candidate = parts.drop(i).joinToString(".")
                if (domainSet.contains(candidate)) return true
            }
        }

        return false
    }

    /**
     * Bóc tách hostname chuẩn từ URL bất kỳ
     */
    fun extractHost(raw: String): String {
        var str = raw.trim().lowercase()
        str = str.replace("http://", "").replace("https://", "")
        str = str.substringBefore("/").substringBefore("?").substringBefore(":")
        if (str.startsWith("www.")) {
            str = str.substring(4)
        }
        return str.trim()
    }

    private fun cleanDomainString(raw: String): String {
        return raw.trim().lowercase()
            .replace("http://", "")
            .replace("https://", "")
            .replace("www.", "")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")
            .trim()
    }

    val totalDomains: Int
        get() = domainSet.size
}
