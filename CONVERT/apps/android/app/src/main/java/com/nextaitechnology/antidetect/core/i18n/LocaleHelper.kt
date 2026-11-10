package com.nextaitechnology.antidetect.core.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import java.util.Locale

/**
 * Data Model cho một Ngôn ngữ trong ứng dụng
 */
data class LanguageItem(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flag: String
)

/**
 * Hệ thống Đa Ngôn Ngữ Toàn Cầu 50 Quốc Gia (Global i18n Locale Helper)
 * Handles in-app language switching and persistence across 50 popular languages
 * while strictly preserving the device's ISO 3166-1 country code so that
 * currency localization never loses regional context.
 *
 * @author NextAI Technology Core Team
 */
object LocaleHelper {

    /**
     * Lưu giữ Locale mặc định ban đầu của hệ điều hành trước khi bất kỳ hàm wrapContext nào can thiệp
     */
    private val initialBootLocale: Locale = Locale.getDefault()

    @Volatile
    private var cachedDeviceCountryCode: String? = null

    /**
     * Ánh xạ 50 ngôn ngữ hỗ trợ sang mã quốc gia ISO 3166-1 chuẩn tương ứng
     */
    val DEFAULT_COUNTRY_FOR_LANGUAGE: Map<String, String> = mapOf(
        "vi" to "VN",
        "en" to "US",
        "zh" to "CN",
        "es" to "ES",
        "ar" to "SA",
        "hi" to "IN",
        "fr" to "FR",
        "ru" to "RU",
        "pt" to "BR",
        "id" to "ID",
        "de" to "DE",
        "ja" to "JP",
        "ko" to "KR",
        "tr" to "TR",
        "it" to "IT",
        "th" to "TH",
        "pl" to "PL",
        "nl" to "NL",
        "uk" to "UA",
        "fa" to "IR",
        "ro" to "RO",
        "ms" to "MY",
        "bn" to "BD",
        "ur" to "PK",
        "fil" to "PH",
        "tl" to "PH",
        "cs" to "CZ",
        "el" to "GR",
        "sv" to "SE",
        "hu" to "HU",
        "he" to "IL",
        "iw" to "IL",
        "da" to "DK",
        "fi" to "FI",
        "no" to "NO",
        "nb" to "NO",
        "sk" to "SK",
        "bg" to "BG",
        "hr" to "HR",
        "sr" to "RS",
        "lt" to "LT",
        "sl" to "SI",
        "lv" to "LV",
        "et" to "EE",
        "sw" to "KE",
        "ta" to "IN",
        "te" to "IN",
        "mr" to "IN",
        "gu" to "IN",
        "kn" to "IN",
        "ml" to "IN",
        "my" to "MM",
        "km" to "KH"
    )

    val SUPPORTED_LANGUAGES = listOf(
        LanguageItem("vi", "Tiếng Việt", "Vietnamese", "🇻🇳"),
        LanguageItem("en", "English", "English", "🇺🇸"),
        LanguageItem("zh", "简体中文", "Chinese Simplified", "🇨🇳"),
        LanguageItem("es", "Español", "Spanish", "🇪🇸"),
        LanguageItem("ar", "العربية", "Arabic", "🇸🇦"),
        LanguageItem("hi", "हिन्दी", "Hindi", "🇮🇳"),
        LanguageItem("fr", "Français", "French", "🇫🇷"),
        LanguageItem("ru", "Русский", "Russian", "🇷🇺"),
        LanguageItem("pt", "Português", "Portuguese", "🇧🇷"),
        LanguageItem("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
        LanguageItem("de", "Deutsch", "German", "🇩🇪"),
        LanguageItem("ja", "日本語", "Japanese", "🇯🇵"),
        LanguageItem("ko", "한국어", "Korean", "🇰🇷"),
        LanguageItem("tr", "Türkçe", "Turkish", "🇹🇷"),
        LanguageItem("it", "Italiano", "Italian", "🇮🇹"),
        LanguageItem("th", "ไทย", "Thai", "🇹🇭"),
        LanguageItem("pl", "Polski", "Polish", "🇵🇱"),
        LanguageItem("nl", "Nederlands", "Dutch", "🇳🇱"),
        LanguageItem("uk", "Українська", "Ukrainian", "🇺🇦"),
        LanguageItem("fa", "فارسی", "Persian", "🇮🇷"),
        LanguageItem("ro", "Română", "Romanian", "🇷🇴"),
        LanguageItem("ms", "Bahasa Melayu", "Malay", "🇲🇾"),
        LanguageItem("bn", "বাংলা", "Bengali", "🇧🇩"),
        LanguageItem("ur", "اردو", "Urdu", "🇵🇰"),
        LanguageItem("fil", "Filipino", "Filipino", "🇵🇭"),
        LanguageItem("cs", "Čeština", "Czech", "🇨🇿"),
        LanguageItem("el", "Ελληνικά", "Greek", "🇬🇷"),
        LanguageItem("sv", "Svenska", "Swedish", "🇸🇪"),
        LanguageItem("hu", "Magyar", "Hungarian", "🇭🇺"),
        LanguageItem("he", "עברית", "Hebrew", "🇮🇱"),
        LanguageItem("da", "Dansk", "Danish", "🇩🇰"),
        LanguageItem("fi", "Suomi", "Finnish", "🇫🇮"),
        LanguageItem("no", "Norsk", "Norwegian", "🇳🇴"),
        LanguageItem("sk", "Slovenčina", "Slovak", "🇸🇰"),
        LanguageItem("bg", "Български", "Bulgarian", "🇧🇬"),
        LanguageItem("hr", "Hrvatski", "Croatian", "🇭🇷"),
        LanguageItem("sr", "Српски", "Serbian", "🇷🇸"),
        LanguageItem("lt", "Lietuvių", "Lithuanian", "🇱🇹"),
        LanguageItem("sl", "Slovenščina", "Slovenian", "🇸🇮"),
        LanguageItem("lv", "Latviešu", "Latvian", "🇱🇻"),
        LanguageItem("et", "Eesti", "Estonian", "🇪🇪"),
        LanguageItem("sw", "Kiswahili", "Swahili", "🇰🇪"),
        LanguageItem("ta", "தமிழ்", "Tamil", "🇮🇳"),
        LanguageItem("te", "తెలుగు", "Telugu", "🇮🇳"),
        LanguageItem("mr", "मराठी", "Marathi", "🇮🇳"),
        LanguageItem("gu", "ગુજરાતી", "Gujarati", "🇮🇳"),
        LanguageItem("kn", "ಕನ್ನಡ", "Kannada", "🇮🇳"),
        LanguageItem("ml", "മലയാളം", "Malayalam", "🇮🇳"),
        LanguageItem("my", "မြန်မာ", "Burmese", "🇲🇲"),
        LanguageItem("km", "ភាសាខ្មែរ", "Khmer", "🇰🇭")
    )

    /**
     * Trích xuất Locale gốc của hệ điều hành thiết bị (không bị ảnh hưởng bởi Locale(langCode) không có country)
     */
    fun getSystemDeviceLocale(): Locale {
        try {
            val sysConfig = Resources.getSystem()?.configuration
            if (sysConfig != null) {
                val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sysConfig.locales.get(0)
                } else {
                    @Suppress("DEPRECATION")
                    sysConfig.locale
                }
                if (sysLocale != null && sysLocale.country.isNotBlank()) {
                    return sysLocale
                }
            }
        } catch (_: Exception) {
            // Ignored in local JVM unit test environment where Resources.getSystem() is not mocked
        }

        val currentDefault = Locale.getDefault()
        if (currentDefault.country.isNotBlank()) {
            return currentDefault
        }
        if (initialBootLocale.country.isNotBlank()) {
            return initialBootLocale
        }
        return currentDefault
    }

    /**
     * Ánh xạ TimeZone ID phổ biến sang mã quốc gia ISO 3166-1 khi thiết bị Wi-Fi/Test không lắp SIM và để mặc định en_US
     */
    private val TIMEZONE_TO_COUNTRY: Map<String, String> = mapOf(
        "Asia/Ho_Chi_Minh" to "VN",
        "Asia/Saigon" to "VN",
        "Asia/Bangkok" to "TH",
        "Asia/Jakarta" to "ID",
        "Asia/Pontianak" to "ID",
        "Asia/Makassar" to "ID",
        "Asia/Jayapura" to "ID",
        "Asia/Manila" to "PH",
        "Asia/Kuala_Lumpur" to "MY",
        "Asia/Kuching" to "MY",
        "Asia/Singapore" to "SG",
        "Asia/Kolkata" to "IN",
        "Asia/Calcutta" to "IN",
        "Asia/Tokyo" to "JP",
        "Asia/Seoul" to "KR",
        "Asia/Shanghai" to "CN",
        "Asia/Chongqing" to "CN",
        "Asia/Harbin" to "CN",
        "Asia/Urumqi" to "CN",
        "Asia/Hong_Kong" to "HK",
        "Asia/Taipei" to "TW",
        "Asia/Karachi" to "PK",
        "Asia/Dhaka" to "BD",
        "Asia/Colombo" to "LK",
        "Asia/Kathmandu" to "NP",
        "Asia/Yangon" to "MM",
        "Asia/Rangoon" to "MM",
        "Asia/Phnom_Penh" to "KH",
        "Asia/Vientiane" to "LA",
        "Asia/Riyadh" to "SA",
        "Asia/Dubai" to "AE",
        "Asia/Jerusalem" to "IL",
        "Asia/Tel_Aviv" to "IL",
        "Asia/Tehran" to "IR",
        "Asia/Baghdad" to "IQ",
        "Asia/Kuwait" to "KW",
        "Asia/Qatar" to "QA",
        "Asia/Almaty" to "KZ",
        "Asia/Tashkent" to "UZ",
        "Europe/London" to "GB",
        "Europe/Belfast" to "GB",
        "Europe/Dublin" to "IE",
        "Europe/Berlin" to "DE",
        "Europe/Paris" to "FR",
        "Europe/Madrid" to "ES",
        "Europe/Rome" to "IT",
        "Europe/Amsterdam" to "NL",
        "Europe/Brussels" to "BE",
        "Europe/Vienna" to "AT",
        "Europe/Zurich" to "CH",
        "Europe/Lisbon" to "PT",
        "Europe/Athens" to "GR",
        "Europe/Helsinki" to "FI",
        "Europe/Warsaw" to "PL",
        "Europe/Prague" to "CZ",
        "Europe/Budapest" to "HU",
        "Europe/Bucharest" to "RO",
        "Europe/Sofia" to "BG",
        "Europe/Zagreb" to "HR",
        "Europe/Belgrade" to "RS",
        "Europe/Bratislava" to "SK",
        "Europe/Ljubljana" to "SI",
        "Europe/Vilnius" to "LT",
        "Europe/Riga" to "LV",
        "Europe/Tallinn" to "EE",
        "Europe/Stockholm" to "SE",
        "Europe/Oslo" to "NO",
        "Europe/Copenhagen" to "DK",
        "Europe/Kiev" to "UA",
        "Europe/Kyiv" to "UA",
        "Europe/Istanbul" to "TR",
        "Europe/Moscow" to "RU",
        "Australia/Sydney" to "AU",
        "Australia/Melbourne" to "AU",
        "Australia/Brisbane" to "AU",
        "Australia/Perth" to "AU",
        "Australia/Adelaide" to "AU",
        "Pacific/Auckland" to "NZ",
        "America/Toronto" to "CA",
        "America/Vancouver" to "CA",
        "America/Montreal" to "CA",
        "America/Edmonton" to "CA",
        "America/Winnipeg" to "CA",
        "America/Halifax" to "CA",
        "America/Sao_Paulo" to "BR",
        "America/Fortaleza" to "BR",
        "America/Recife" to "BR",
        "America/Manaus" to "BR",
        "America/Mexico_City" to "MX",
        "America/Monterrey" to "MX",
        "America/Tijuana" to "MX",
        "America/Argentina/Buenos_Aires" to "AR",
        "America/Buenos_Aires" to "AR",
        "America/Bogota" to "CO",
        "America/Santiago" to "CL",
        "America/Lima" to "PE",
        "Africa/Cairo" to "EG",
        "Africa/Lagos" to "NG",
        "Africa/Johannesburg" to "ZA",
        "Africa/Nairobi" to "KE",
        "Africa/Casablanca" to "MA"
    )

    /**
     * Lấy mã quốc gia ISO 3166-1 alpha-2 chính xác của thiết bị (ví dụ: "VN", "US", "IN", "GB", "DE", "PH", "KR"...)
     */
    fun getDeviceCountryCode(context: Context? = null): String {
        // Khi chạy trên thiết bị Android thực (context != null): dùng cache O(1) để không gọi lặp lại Binder IPC TelephonyManager trên UI thread
        if (context != null) {
            cachedDeviceCountryCode?.let { if (it.length == 2) return it }

            var resolved = ""
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT).orEmpty()
                if (simCountry.length == 2 && simCountry.all { it in 'A'..'Z' }) {
                    resolved = simCountry
                } else {
                    val netCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT).orEmpty()
                    if (netCountry.length == 2 && netCountry.all { it in 'A'..'Z' }) {
                        resolved = netCountry
                    }
                }
            } catch (_: Exception) {
            }

            if (resolved.isEmpty()) {
                val sysCountry = getSystemDeviceLocale().country.trim().uppercase(Locale.ROOT)
                if (sysCountry.length == 2 && sysCountry != "US") {
                    resolved = sysCountry
                }
            }

            // Nếu thiết bị không có SIM và để mặc định en_US, kiểm tra TimeZone của thiết bị
            if (resolved.isEmpty()) {
                try {
                    val tzId = java.util.TimeZone.getDefault().id
                    val tzCountry = TIMEZONE_TO_COUNTRY[tzId]
                    if (!tzCountry.isNullOrEmpty()) {
                        resolved = tzCountry
                    }
                } catch (_: Exception) {
                }
            }

            if (resolved.isEmpty()) {
                val sysCountry = getSystemDeviceLocale().country.trim().uppercase(Locale.ROOT)
                if (sysCountry.length == 2) {
                    resolved = sysCountry
                }
            }

            if (resolved.length == 2) {
                cachedDeviceCountryCode = resolved
                return resolved
            }
        }

        // Kiểm tra Locale hiện tại (hỗ trợ cả JVM Unit Test khi test gọi Locale.setDefault)
        val currentCountry = Locale.getDefault().country.trim().uppercase(Locale.ROOT)
        if (currentCountry.length == 2) {
            return currentCountry
        }

        // Kiểm tra System Resources Locale gốc của Android OS
        val sysCountry = getSystemDeviceLocale().country.trim().uppercase(Locale.ROOT)
        if (sysCountry.length == 2) {
            return sysCountry
        }

        // Fallback về mã quốc gia lúc khởi động tiến trình
        val bootCountry = initialBootLocale.country.trim().uppercase(Locale.ROOT)
        if (bootCountry.length == 2) {
            return bootCountry
        }

        return ""
    }

    /**
     * Xây dựng đối tượng Locale luôn bảo toàn mã quốc gia ISO (Không bao giờ tạo Locale rỗng country)
     */
    fun resolveLocaleWithCountry(langCode: String, context: Context? = null): Locale {
        val clean = langCode.trim()
        if (clean.contains("-") || clean.contains("_")) {
            val parts = clean.split("-", "_")
            val lang = parts[0].lowercase(Locale.ROOT).ifEmpty { "en" }
            val region = parts.getOrNull(1)?.uppercase(Locale.ROOT).orEmpty()
            if (region.length == 2) {
                return Locale(lang, region)
            }
        }

        val lang = clean.lowercase(Locale.ROOT).ifEmpty { "en" }
        val deviceCountry = getDeviceCountryCode(context)

        // Nếu ngôn ngữ là tiếng Anh ("en") hoặc chưa đổi ngôn ngữ khác với ngôn ngữ hệ thống,
        // giữ nguyên mã quốc gia thực tế của thiết bị (VD: en_IN, en_GB, en_CA, en_AU, en_SG, en_PH, en_VN...)
        val sysLang = getSystemDeviceLocale().language.lowercase(Locale.ROOT)
        val resolvedCountry = when {
            deviceCountry.length == 2 && (lang == "en" || lang == sysLang) -> deviceCountry
            DEFAULT_COUNTRY_FOR_LANGUAGE.containsKey(lang) -> DEFAULT_COUNTRY_FOR_LANGUAGE[lang]!!
            deviceCountry.length == 2 -> deviceCountry
            else -> "US"
        }

        return Locale(lang, resolvedCountry)
    }

    fun getCurrentLanguage(context: Context): LanguageItem {
        val repo = SettingsRepository(context)
        val code = repo.languageCode
        return SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.firstOrNull { it.code == "en" }
            ?: SUPPORTED_LANGUAGES[0]
    }

    fun applyLanguage(context: Context, langCode: String) {
        val repo = SettingsRepository(context)
        repo.languageCode = langCode

        val locale = resolveLocaleWithCountry(langCode, context)
        Locale.setDefault(locale)

        // Đồng bộ toàn diện tài nguyên ApplicationContext và Context hiện tại
        try {
            val app = context.applicationContext
            val appRes = app?.resources ?: context.resources
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(locale)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)

            val ctxRes = context.resources
            val ctxConfig = Configuration(ctxRes.configuration)
            ctxConfig.setLocale(locale)
            @Suppress("DEPRECATION")
            ctxRes.updateConfiguration(ctxConfig, ctxRes.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // AndroidX AppCompat Modern Per-App Language với đầy đủ tag ngôn ngữ + quốc gia (ví dụ: en-IN, vi-VN)
        val languageTag = if (locale.country.isNotEmpty()) "${locale.language}-${locale.country}" else langCode
        val appLocale = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun wrapContext(context: Context): Context {
        val repo = SettingsRepository(context)
        val langCode = repo.languageCode
        val locale = resolveLocaleWithCountry(langCode, context)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return context.createConfigurationContext(config)
    }
}
