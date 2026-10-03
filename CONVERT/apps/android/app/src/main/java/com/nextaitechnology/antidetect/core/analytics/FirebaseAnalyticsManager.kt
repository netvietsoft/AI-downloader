package com.nextaitechnology.antidetect.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Quản Lý Firebase Analytics & Crashlytics Toàn Diện
 * Tích hợp theo chỉ đạo của Chủ tịch từ thư mục Firebase/event.txt & google-services.json
 *
 * Hỗ trợ đầy đủ 28 Events chuẩn hóa:
 * - Màn hình: View_Home, View_Progess, View_Player, View_Setting, screen_view
 * - Tương tác: Click_private Vault, Click_Search, Click_Play_video, Click_button_Download
 * - Quảng cáo (AdMob): ad_clicked, ad_dismissed, ad_failed_to_load, ad_impression, ad_loaded, ad_requested
 * - Paywall / Gói cước: paywall_view, paywall_plan_selected, paywall_cta_clicked, paywall_restore_clicked, premium_feature_used
 * - Vòng đời ứng dụng: first_open, session_start, app_update, os_update, app_clear_data, app_exception, app_remove
 * - Cài đặt: settings_action_clicked
 *
 * @author NextAI Technology Core Team
 */
object FirebaseAnalyticsManager {

    private const val TAG = "FirebaseAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var isInitialized = false

    // Danh sách lưu trữ 50 sự kiện gần nhất phục vụ kiểm tra và debug
    val recentEvents = mutableListOf<EventLogEntry>()

    data class EventLogEntry(
        val timestamp: Long,
        val eventName: String,
        val params: Map<String, String>
    )

    /**
     * Khởi tạo Firebase Analytics & Crashlytics
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            crashlytics = FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
            isInitialized = true
            Log.i(TAG, "Firebase Analytics & Crashlytics đã khởi tạo thành công với Project ID: ai-video-downloader-e9bc4")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khởi tạo Firebase Analytics: ${e.message}", e)
        }
    }

    /**
     * Hàm cốt lõi ghi nhận sự kiện (Core Event Logger)
     */
    fun logEvent(eventName: String, params: Bundle? = null) {
        val map = mutableMapOf<String, String>()
        params?.keySet()?.forEach { key ->
            map[key] = params.get(key)?.toString() ?: ""
        }

        synchronized(recentEvents) {
            recentEvents.add(EventLogEntry(System.currentTimeMillis(), eventName, map))
            if (recentEvents.size > 50) {
                recentEvents.removeAt(0)
            }
        }

        Log.d(TAG, "🔥 [Firebase Event] $eventName => $map")

        try {
            firebaseAnalytics?.logEvent(eventName, params)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gửi event $eventName lên Firebase: ${e.message}")
        }
    }

    // =========================================================================
    // 1. NHÓM SỰ KIỆN MÀN HÌNH (SCREEN VIEWS)
    // =========================================================================

    fun logViewHome() {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Home_Browser")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        logEvent("View_Home", bundle)
        logScreenView("Home_Browser", "MainActivity")
    }

    fun logViewProgress() {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Progress_Downloader")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ProgressFragment")
        }
        logEvent("View_Progess", bundle)
        logScreenView("Progress_Downloader", "ProgressFragment")
    }

    fun logViewPlayer(videoTitle: String? = null, videoUrl: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "AcePlayer")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AcePlayerActivity")
            putString("video_title", videoTitle ?: "Unknown")
            putString("video_url", videoUrl ?: "")
        }
        logEvent("View_Player", bundle)
        logScreenView("AcePlayer", "AcePlayerActivity")
    }

    fun logViewSetting() {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Settings")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SettingsActivity")
        }
        logEvent("View_Setting", bundle)
        logScreenView("Settings", "SettingsActivity")
    }

    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent("screen_view", bundle)
    }

    // =========================================================================
    // 2. NHÓM SỰ KIỆN TƯƠNG TÁC (USER ACTIONS)
    // =========================================================================

    fun logClickPrivateVault() {
        logEvent("Click_private Vault", Bundle().apply {
            putString("action", "open_secure_vault")
        })
    }

    fun logClickSearch(query: String, source: String = "home_search_bar") {
        val bundle = Bundle().apply {
            putString("search_query", query)
            putString("source", source)
        }
        logEvent("Click_Search", bundle)
    }

    fun logClickPlayVideo(videoTitle: String, videoPath: String) {
        val bundle = Bundle().apply {
            putString("video_title", videoTitle)
            putString("video_path", videoPath)
        }
        logEvent("Click_Play_video", bundle)
    }

    fun logClickButtonDownload(url: String, quality: String, format: String, isMonetized: Boolean = false) {
        val bundle = Bundle().apply {
            putString("target_url", url)
            putString("quality", quality)
            putString("format", format)
            putBoolean("is_monetized", isMonetized)
        }
        logEvent("Click_button_Download", bundle)
    }

    // =========================================================================
    // 3. NHÓM SỰ KIỆN QUẢNG CÁO ADMOB (AD EVENTS)
    // =========================================================================

    fun logAdRequested(adType: String, adUnitId: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_unit_id", adUnitId)
        }
        logEvent("ad_requested", bundle)
    }

    fun logAdLoaded(adType: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
        }
        logEvent("ad_loaded", bundle)
    }

    fun logAdFailedToLoad(adType: String, errorCode: Int, errorMessage: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putInt("error_code", errorCode)
            putString("error_message", errorMessage)
        }
        logEvent("ad_failed_to_load", bundle)
    }

    fun logAdImpression(adType: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
        }
        logEvent("ad_impression", bundle)
    }

    fun logAdClicked(adType: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
        }
        logEvent("ad_clicked", bundle)
    }

    fun logAdDismissed(adType: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
        }
        logEvent("ad_dismissed", bundle)
    }

    // =========================================================================
    // 4. NHÓM SỰ KIỆN PAYWALL & GÓI VIP (MONETIZATION & PAYWALL)
    // =========================================================================

    fun logPaywallView(reason: String = "manual_nav") {
        val bundle = Bundle().apply {
            putString("trigger_reason", reason)
        }
        logEvent("paywall_view", bundle)
    }

    fun logPaywallPlanSelected(planId: String, price: String) {
        val bundle = Bundle().apply {
            putString("plan_id", planId)
            putString("plan_price", price)
        }
        logEvent("paywall_plan_selected", bundle)
    }

    fun logPaywallCtaClicked(planId: String, price: String) {
        val bundle = Bundle().apply {
            putString("plan_id", planId)
            putString("plan_price", price)
        }
        logEvent("paywall_cta_clicked", bundle)
    }

    fun logPaywallRestoreClicked() {
        logEvent("paywall_restore_clicked")
    }

    fun logPremiumFeatureUsed(featureName: String) {
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
        }
        logEvent("premium_feature_used", bundle)
    }

    // =========================================================================
    // 5. NHÓM SỰ KIỆN VÒNG ĐỜI & HỆ THỐNG (LIFECYCLE & CRASHLYTICS)
    // =========================================================================

    fun logFirstOpen() {
        logEvent("first_open")
    }

    fun logSessionStart() {
        logEvent("session_start")
    }

    fun logAppUpdate(previousVersion: Int, currentVersion: Int) {
        val bundle = Bundle().apply {
            putInt("previous_version", previousVersion)
            putInt("current_version", currentVersion)
        }
        logEvent("app_update", bundle)
    }

    fun logOsUpdate(previousSdk: Int, currentSdk: Int) {
        val bundle = Bundle().apply {
            putInt("previous_sdk", previousSdk)
            putInt("current_sdk", currentSdk)
        }
        logEvent("os_update", bundle)
    }

    fun logAppClearData(dataType: String = "browser_cache_and_history") {
        val bundle = Bundle().apply {
            putString("data_type", dataType)
        }
        logEvent("app_clear_data", bundle)
    }

    fun logAppRemove() {
        logEvent("app_remove")
    }

    fun logSettingsActionClicked(settingKey: String, newValue: String) {
        val bundle = Bundle().apply {
            putString("setting_key", settingKey)
            putString("new_value", newValue)
        }
        logEvent("settings_action_clicked", bundle)
    }

    /**
     * Báo cáo lỗi ngoại lệ đồng bộ tới cả Crashlytics và Firebase Analytics
     */
    fun logAppException(throwable: Throwable, contextTag: String = "General") {
        crashlytics?.recordException(throwable)
        crashlytics?.setCustomKey("context_tag", contextTag)

        val bundle = Bundle().apply {
            putString("context_tag", contextTag)
            putString("exception_class", throwable.javaClass.simpleName)
            putString("exception_message", throwable.message ?: "No message")
        }
        logEvent("app_exception", bundle)
        Log.e(TAG, "🚨 [Crashlytics / App Exception] [$contextTag]: ${throwable.message}", throwable)
    }
}
