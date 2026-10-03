package com.nextaitechnology.antidetect.core.i18n

import android.content.Context
import android.content.res.Configuration
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
 * Handles in-app language switching and persistence across 50 popular languages.
 *
 * @author NextAI Technology Core Team
 */
object LocaleHelper {

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

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        // Đồng bộ toàn diện tài nguyên ApplicationContext và Context hiện tại
        try {
            val app = context.applicationContext
            val appRes = app?.resources ?: context.resources
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(locale)
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)

            val ctxRes = context.resources
            val ctxConfig = Configuration(ctxRes.configuration)
            ctxConfig.setLocale(locale)
            ctxRes.updateConfiguration(ctxConfig, ctxRes.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // AndroidX AppCompat Modern Per-App Language
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun wrapContext(context: Context): Context {
        val repo = SettingsRepository(context)
        val langCode = repo.languageCode
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        try {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return context.createConfigurationContext(config)
    }
}
