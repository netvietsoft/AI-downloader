package com.nextaitechnology.antidetect

import android.app.Application
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import com.nextaitechnology.antidetect.core.network.SnifferEngine
import com.nextaitechnology.antidetect.feature.ad.AdMobManager
import com.nextaitechnology.antidetect.feature.downloader.DownloadEngine
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import java.io.File

/**
 * Ứng Dụng Khởi Tạo Toàn Cục NextAI Video Downloader
 * Application Root Initialization (AdMob, Firebase, Locale, Engines)
 *
 * @author NextAI Technology Core Team
 */
class NextAIApplication : Application() {

    lateinit var downloadEngine: DownloadEngine
        private set
    lateinit var snifferEngine: SnifferEngine
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(base))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.wrapContext(this)
    }

    override fun onCreate() {
        super.onCreate()

        settingsRepository = SettingsRepository(this)
        snifferEngine = SnifferEngine()

        val downloadDir = File(getExternalFilesDir(null), "Videos").apply {
            if (!exists()) mkdirs()
        }
        downloadEngine = DownloadEngine(downloadDir, context = this)

        // Khởi tạo Firebase Analytics & Crashlytics
        com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.initialize(this)

        // Bắt lỗi ngoại lệ toàn cục chuyển tới Crashlytics & Firebase Analytics (app_exception)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.logAppException(throwable, "UncaughtException")
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Kiểm tra vòng đời: first_open, session_start, app_update, os_update
        val prefs = getSharedPreferences("firebase_lifecycle_tracker", android.content.Context.MODE_PRIVATE)
        val isFirstOpen = prefs.getBoolean("is_first_open", true)
        if (isFirstOpen) {
            com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.logFirstOpen()
            prefs.edit().putBoolean("is_first_open", false).apply()
        }

        val lastVersion = prefs.getInt("last_app_version", BuildConfig.VERSION_CODE)
        if (BuildConfig.VERSION_CODE > lastVersion) {
            com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.logAppUpdate(lastVersion, BuildConfig.VERSION_CODE)
            prefs.edit().putInt("last_app_version", BuildConfig.VERSION_CODE).apply()
        }

        val lastSdk = prefs.getInt("last_os_sdk", android.os.Build.VERSION.SDK_INT)
        if (android.os.Build.VERSION.SDK_INT != lastSdk) {
            com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.logOsUpdate(lastSdk, android.os.Build.VERSION.SDK_INT)
            prefs.edit().putInt("last_os_sdk", android.os.Build.VERSION.SDK_INT).apply()
        }

        com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager.logSessionStart()

        // Khởi tạo mạng quảng cáo Google AdMob
        AdMobManager.initialize(this)

        // Khởi tạo danh sách 1822+ Monetized Domains từ Bao_cao_web_video.csv
        com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.init(this)

        // Khởi tạo ngôn ngữ đã lưu
        val savedLang = settingsRepository.languageCode
        if (savedLang.isNotEmpty()) {
            LocaleHelper.applyLanguage(this, savedLang)
        }
    }
}
