package com.nextaitechnology.antidetect.feature.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager

/**
 * Quản Lý Mạng Quảng Cáo AdMob (AdMob Manager)
 * Initializes Google Mobile Ads SDK, handles Banner, Interstitial, and Rewarded Ads.
 * Bypasses all ads if user has subscribed to PRO.
 *
 * @author NextAI Technology Core Team
 */
object AdMobManager {

    private const val TAG = "AdMobManager"

    // Google Test Ad Unit IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { status ->
                Log.i(TAG, "Google Mobile Ads SDK đã khởi tạo thành công: $status")
                isInitialized = true
                loadInterstitial(context)
                loadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khởi tạo MobileAds: ${e.message}", e)
        }
    }

    /**
     * Tải quảng cáo Banner và gắn vào container
     */
    fun loadBanner(activity: Activity, container: ViewGroup, adUnitId: String = TEST_BANNER_ID) {
        val settings = SettingsRepository(activity)
        if (settings.isProUser) {
            container.removeAllViews()
            container.visibility = android.view.View.GONE
            return
        }

        try {
            FirebaseAnalyticsManager.logAdRequested("banner", adUnitId)
            container.removeAllViews()
            val adView = AdView(activity).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.BANNER)
            }
            container.addView(adView)
            container.visibility = android.view.View.VISIBLE

            val adRequest = AdRequest.Builder().build()
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner Ad đã nạp thành công.")
                    FirebaseAnalyticsManager.logAdLoaded("banner")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Banner Ad nạp thất bại: ${error.message}")
                    FirebaseAnalyticsManager.logAdFailedToLoad("banner", error.code, error.message)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner Ad được click.")
                    FirebaseAnalyticsManager.logAdClicked("banner")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner Ad impression ghi nhận.")
                    FirebaseAnalyticsManager.logAdImpression("banner")
                }
            }
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi nạp Banner Ad: ${e.message}", e)
            FirebaseAnalyticsManager.logAppException(e, "AdMob_loadBanner")
        }
    }

    /**
     * Tải trước quảng cáo xen kẽ (Interstitial Ad)
     */
    fun loadInterstitial(context: Context, adUnitId: String = TEST_INTERSTITIAL_ID) {
        val settings = SettingsRepository(context)
        if (settings.isProUser) return

        FirebaseAnalyticsManager.logAdRequested("interstitial", adUnitId)
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad đã sẵn sàng.")
                    interstitialAd = ad
                    FirebaseAnalyticsManager.logAdLoaded("interstitial")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Nạp Interstitial Ad thất bại: ${error.message}")
                    interstitialAd = null
                    FirebaseAnalyticsManager.logAdFailedToLoad("interstitial", error.code, error.message)
                }
            }
        )
    }

    /**
     * Hiển thị quảng cáo xen kẽ
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val settings = SettingsRepository(activity)
        if (settings.isProUser || interstitialAd == null) {
            onDismissed()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FirebaseAnalyticsManager.logAdImpression("interstitial")
            }

            override fun onAdClicked() {
                FirebaseAnalyticsManager.logAdClicked("interstitial")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad đã đóng.")
                FirebaseAnalyticsManager.logAdDismissed("interstitial")
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Hiển thị Interstitial Ad thất bại: ${adError.message}")
                FirebaseAnalyticsManager.logAdFailedToLoad("interstitial", adError.code, adError.message)
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
        }
        interstitialAd?.show(activity)
    }

    /**
     * Tải trước quảng cáo có thưởng (Rewarded Ad)
     */
    fun loadRewarded(context: Context, adUnitId: String = TEST_REWARDED_ID) {
        val settings = SettingsRepository(context)
        if (settings.isProUser) return

        FirebaseAnalyticsManager.logAdRequested("rewarded", adUnitId)
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad đã sẵn sàng.")
                    rewardedAd = ad
                    FirebaseAnalyticsManager.logAdLoaded("rewarded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Nạp Rewarded Ad thất bại: ${error.message}")
                    rewardedAd = null
                    FirebaseAnalyticsManager.logAdFailedToLoad("rewarded", error.code, error.message)
                }
            }
        )
    }

    /**
     * Hiển thị quảng cáo có thưởng (vd: mở khóa tải nhanh)
     */
    fun showRewarded(activity: Activity, onUserEarnedReward: () -> Unit, onDismissed: () -> Unit) {
        val settings = SettingsRepository(activity)
        if (settings.isProUser || rewardedAd == null) {
            onUserEarnedReward()
            onDismissed()
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FirebaseAnalyticsManager.logAdImpression("rewarded")
            }

            override fun onAdClicked() {
                FirebaseAnalyticsManager.logAdClicked("rewarded")
            }

            override fun onAdDismissedFullScreenContent() {
                FirebaseAnalyticsManager.logAdDismissed("rewarded")
                rewardedAd = null
                loadRewarded(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                FirebaseAnalyticsManager.logAdFailedToLoad("rewarded", adError.code, adError.message)
                rewardedAd = null
                loadRewarded(activity)
                onDismissed()
            }
        }
        rewardedAd?.show(activity) {
            onUserEarnedReward()
        }
    }
}
