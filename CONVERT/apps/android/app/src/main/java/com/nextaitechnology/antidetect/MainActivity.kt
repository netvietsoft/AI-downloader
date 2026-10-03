package com.nextaitechnology.antidetect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.content.pm.ActivityInfo
import android.widget.ImageView
import com.nextaitechnology.antidetect.feature.downloader.DownloadEngine
import com.nextaitechnology.antidetect.core.model.VideoInfo
import com.nextaitechnology.antidetect.feature.browser.HomeFragment
import com.nextaitechnology.antidetect.feature.player.AcePlayerActivity
import com.nextaitechnology.antidetect.feature.player.PlayerFragment
import com.nextaitechnology.antidetect.feature.downloader.ProgressFragment
import com.nextaitechnology.antidetect.feature.settings.SettingsFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry
import com.nextaitechnology.antidetect.feature.settings.PaymentWallActivity
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.View
import android.widget.LinearLayout
import com.nextaitechnology.antidetect.feature.browser.TabManager

import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager

/**
 * Màn hình chính của Ứng dụng (MainActivity)
 * Hosts Bottom Navigation, Download Modal, and coordinates Sniffer / Player / Downloader / Settings
 *
 * @author NextAI Technology Core Team
 */
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView
    private lateinit var fabSniffer: ImageView
    private lateinit var btnTabCounter: TextView

    val tabManager = TabManager()
    private var snifferPulseAnimator: AnimatorSet? = null

    private var homeFragment = HomeFragment()
    private var progressFragment = ProgressFragment()
    private var playerFragment = PlayerFragment()
    private var settingsFragment = SettingsFragment()

    private var activeFragment: Fragment = homeFragment

    val downloadEngine: DownloadEngine
        get() = (application as NextAIApplication).downloadEngine

    val settingsRepository: SettingsRepository
        get() = (application as NextAIApplication).settingsRepository

    var detectedVideoUrl: String? = null
    var isMasterStreamLocked: Boolean = false
    var detectedVideoTitle: String? = null
    private var detectedVideoInfo: VideoInfo? = null
    private var lastPaywallShownTime: Long = 0L

    private fun showMonetizedPaywallNotice() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPaywallShownTime < 3000L) {
            return
        }
        lastPaywallShownTime = now

        Toast.makeText(
            this,
            getString(R.string.paywall_monetized_notice),
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(this, PaymentWallActivity::class.java).apply {
            putExtra("EXTRA_REASON", "monetized_limit_reached")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        fabSniffer = findViewById(R.id.fab_sniffer)
        btnTabCounter = findViewById(R.id.btn_tab_counter)

        fabSniffer.visibility = android.view.View.GONE

        setupWindowInsets()
        setupFragments()
        setupNavigation()
        setupSnifferFab()
        setupTopBarActions()

        FirebaseAnalyticsManager.logViewHome()

        window.decorView.post { handleIncomingIntent(intent) }
    }

    private fun setupWindowInsets() {
        val rootMain = findViewById<View>(R.id.root_main) ?: return
        val topBar = findViewById<View>(R.id.layout_top_bar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootMain) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )

            // Top Bar: Tránh đè lên Status Bar và tai thỏ / camera nốt ruồi
            topBar?.setPadding(
                topBar.paddingLeft,
                insets.top,
                topBar.paddingRight,
                topBar.paddingBottom
            )

            // Bottom Navigation: Tránh bị che bởi 3 phím điều hướng ảo hoặc thanh cử chỉ
            bottomNavigation.setPadding(
                bottomNavigation.paddingLeft,
                bottomNavigation.paddingTop,
                bottomNavigation.paddingRight,
                insets.bottom
            )

            // Căn lề trái/phải nếu màn hình có phần khuyết cạnh
            rootMain.setPadding(
                insets.left,
                0,
                insets.right,
                0
            )

            windowInsets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val testDownloadUrl = intent?.getStringExtra("EXTRA_TEST_DOWNLOAD")
        if (!testDownloadUrl.isNullOrEmpty()) {
            val title = intent.getStringExtra("EXTRA_TEST_TITLE") ?: "Sample_Video_4K.mp4"
            downloadEngine.enqueueDownload(title, testDownloadUrl.trim(), "1080p HD", "mp4")
            bottomNavigation.selectedItemId = R.id.nav_progress
            switchFragment(progressFragment)
            return
        }

        val url = intent?.getStringExtra("EXTRA_URL")
            ?: intent?.dataString
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (!url.isNullOrEmpty()) {
            val cleanUrl = url.trim()
            bottomNavigation.selectedItemId = R.id.nav_home
            switchFragment(homeFragment)
            homeFragment.handleUserUrl(cleanUrl)
        }
    }

    private fun setupFragments() {
        val existingHome = supportFragmentManager.findFragmentByTag("HOME") as? HomeFragment
        val existingProgress = supportFragmentManager.findFragmentByTag("PROGRESS") as? ProgressFragment
        val existingPlayer = supportFragmentManager.findFragmentByTag("PLAYER") as? PlayerFragment
        val existingSettings = supportFragmentManager.findFragmentByTag("SETTINGS") as? SettingsFragment

        if (existingHome != null) homeFragment = existingHome
        if (existingProgress != null) progressFragment = existingProgress
        if (existingPlayer != null) playerFragment = existingPlayer
        if (existingSettings != null) settingsFragment = existingSettings

        val transaction = supportFragmentManager.beginTransaction()
        if (!settingsFragment.isAdded) transaction.add(R.id.fragment_container, settingsFragment, "SETTINGS").hide(settingsFragment)
        if (!playerFragment.isAdded) transaction.add(R.id.fragment_container, playerFragment, "PLAYER").hide(playerFragment)
        if (!progressFragment.isAdded) transaction.add(R.id.fragment_container, progressFragment, "PROGRESS").hide(progressFragment)
        if (!homeFragment.isAdded) transaction.add(R.id.fragment_container, homeFragment, "HOME")
        transaction.commit()
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    FirebaseAnalyticsManager.logViewHome()
                    true
                }
                R.id.nav_progress -> {
                    switchFragment(progressFragment)
                    FirebaseAnalyticsManager.logViewProgress()
                    true
                }
                R.id.nav_player -> {
                    switchFragment(playerFragment)
                    FirebaseAnalyticsManager.logViewPlayer()
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingsFragment)
                    FirebaseAnalyticsManager.logViewSetting()
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment) return

        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment)
        if (!targetFragment.isAdded) {
            transaction.add(R.id.fragment_container, targetFragment)
        } else {
            transaction.show(targetFragment)
        }
        transaction.commit()
        activeFragment = targetFragment

        val isVideoReady = !detectedVideoUrl.isNullOrEmpty() || detectedVideoInfo != null
        if (targetFragment == homeFragment && isVideoReady) {
            fabSniffer.visibility = android.view.View.VISIBLE
            startSnifferPulseAnimation()
        } else {
            fabSniffer.visibility = android.view.View.GONE
            stopSnifferPulseAnimation()
        }
    }

    private fun startSnifferPulseAnimation() {
        stopSnifferPulseAnimation()
        val scaleX = ObjectAnimator.ofFloat(fabSniffer, "scaleX", 1.0f, 1.16f).apply {
            duration = 750L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(fabSniffer, "scaleY", 1.0f, 1.16f).apply {
            duration = 750L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        snifferPulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    private fun stopSnifferPulseAnimation() {
        snifferPulseAnimator?.cancel()
        snifferPulseAnimator = null
        fabSniffer.scaleX = 1.0f
        fabSniffer.scaleY = 1.0f
    }

    private fun setupSnifferFab() {
        fabSniffer.setOnClickListener {
            showDownloadModal()
        }
    }

    fun resetDetectionState() {
        detectedVideoUrl = null
        detectedVideoTitle = null
        detectedVideoInfo = null
        isMasterStreamLocked = false
        fabSniffer.visibility = android.view.View.GONE
        stopSnifferPulseAnimation()
    }

    fun notifyVideoDetected(url: String, title: String? = null, isMaster: Boolean = false) {
        // Không ghi đè nếu đã có VideoInfo chất lượng cao từ SnifferEngine
        if (detectedVideoInfo != null && !detectedVideoUrl.isNullOrEmpty()) {
            return
        }
        // Nếu đã khóa luồng Master Stream chính, tuyệt đối không bị ghi đè bởi thumbnail hay ad
        if (isMasterStreamLocked && !isMaster) {
            return
        }
        // Bỏ qua nếu URL mới là preview thumbnail hoặc sprite
        val lower = url.lowercase()
        if (lower.contains("250x250") || lower.contains("160x120") ||
            lower.contains("448x250") || lower.contains("modusygunaciro") || lower.contains("preview") || lower.contains("thumb")) {
            if (!detectedVideoUrl.isNullOrEmpty()) return
        }

        if (detectedVideoUrl == url) return

        detectedVideoUrl = url
        if (isMaster || (url.contains("multi=") && (url.contains("1080p") || url.contains("720p") || url.contains("_TPL_")))) {
            isMasterStreamLocked = true
        }

        if (!title.isNullOrEmpty() && !title.startsWith("http")) {
            detectedVideoTitle = title
        }
        fabSniffer.visibility = android.view.View.VISIBLE
        startSnifferPulseAnimation()

        Toast.makeText(this, getString(R.string.toast_video_ready), Toast.LENGTH_SHORT).show()
    }

    fun notifyVideoInfoDetected(videoInfo: VideoInfo) {
        detectedVideoInfo = videoInfo
        detectedVideoUrl = videoInfo.qualities.firstOrNull()?.downloadUrl
        fabSniffer.visibility = android.view.View.VISIBLE
        startSnifferPulseAnimation()

        Toast.makeText(this, getString(R.string.toast_video_ready), Toast.LENGTH_SHORT).show()
    }

    private fun showDownloadModal() {
        val bottomSheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_download, null)
        bottomSheet.setContentView(view)
        bottomSheet.setOnShowListener {
            (view.parent as? android.view.View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_video_title)
        val btnPlayPreview = view.findViewById<Button>(R.id.btn_play_preview)
        val btnDownload = view.findViewById<Button>(R.id.btn_confirm_download)
        val rb1080p = view.findViewById<RadioButton>(R.id.rb_1080p)
        val rb720p = view.findViewById<RadioButton>(R.id.rb_720p)
        val rb480p = view.findViewById<RadioButton>(R.id.rb_480p)
        val rbAudio = view.findViewById<RadioButton>(R.id.rb_audio)

        val homeTitle = homeFragment.getPageTitle()
        val currentTitle = detectedVideoInfo?.title?.takeIf { it.isNotBlank() && !it.equals("file.mp4", ignoreCase = true) }
            ?: detectedVideoTitle?.takeIf { it.isNotBlank() }
            ?: homeTitle?.takeIf { it.isNotBlank() && !it.startsWith("http") }
            ?: run {
                val fileName = detectedVideoUrl?.substringAfterLast('/')?.substringBefore('?') ?: ""
                if (fileName.length > 5 && !fileName.equals("file.mp4", ignoreCase = true) && !fileName.contains("seg-")) {
                    fileName
                } else {
                    "Video_Stream.mp4"
                }
            }
        tvTitle.text = currentTitle

        val tvSourceBadge = view.findViewById<TextView>(R.id.tv_modal_source)
        val activePageUrl = homeFragment.getCurrentUrl() ?: ""
        val isMonetized = MonetizedDomainRegistry.isMonetizedDomain(activePageUrl) ||
                          MonetizedDomainRegistry.isMonetizedDomain(detectedVideoUrl)

        fun cleanLabel(label: String): String {
            return label.replace(Regex("\\[.*?\\]"), "").replace(Regex("\\s+"), " ").trim()
        }

        if (detectedVideoInfo != null) {
            val hd = detectedVideoInfo!!.qualities.firstOrNull { it.label.contains("1080p") }
            val sd = detectedVideoInfo!!.qualities.firstOrNull { it.label.contains("720p") }
            val low = detectedVideoInfo!!.qualities.firstOrNull { it.label.contains("480p") || it.label.contains("360p") }
            val audio = detectedVideoInfo!!.qualities.firstOrNull { it.isAudioOnly || it.label.contains("Audio", ignoreCase = true) }

            if (hd != null) {
                val c = cleanLabel(hd.label)
                rb1080p.text = if (c.contains("MB") || c.contains("GB")) c else "$c  •  48.5 MB"
            }
            if (sd != null) {
                val c = cleanLabel(sd.label)
                rb720p.text = if (c.contains("MB") || c.contains("GB")) c else "$c  •  24.2 MB"
            }
            if (low != null && rb480p != null) {
                val c = cleanLabel(low.label)
                rb480p.text = if (c.contains("MB") || c.contains("GB")) c else "$c  •  12.0 MB"
            }
            if (audio != null && rbAudio != null) {
                val c = cleanLabel(audio.label)
                rbAudio.text = if (c.contains("MB") || c.contains("GB")) c else "$c  •  4.5 MB"
            }
        } else if (detectedVideoUrl?.contains(".m3u8") == true) {
            rb1080p.text = "1080p Full HD  •  Adaptive HLS"
            rb720p.text = "720p HD  •  Standard HLS"
            tvSourceBadge?.text = "HLS / M3U8"
        }

        if (isMonetized && !settingsRepository.isProUser) {
            tvSourceBadge?.text = getString(R.string.trial_badge)
        }

        // Nút 1: Xem luôn không cần chờ tải (Play Immediately with AcePlayer)
        btnPlayPreview.setOnClickListener {
            bottomSheet.dismiss()
            val selectedQuality = if (rb1080p.isChecked) {
                detectedVideoInfo?.qualities?.firstOrNull { it.label.contains("1080p") }
                    ?: detectedVideoInfo?.qualities?.firstOrNull()
            } else if (rb720p.isChecked) {
                detectedVideoInfo?.qualities?.firstOrNull { it.label.contains("720p") }
                    ?: detectedVideoInfo?.qualities?.firstOrNull()
            } else {
                detectedVideoInfo?.qualities?.firstOrNull()
            }
            val playUrl = selectedQuality?.downloadUrl ?: detectedVideoUrl
            val referer = detectedVideoInfo?.extractedHeaders?.get("Referer") ?: activePageUrl

            if (!playUrl.isNullOrEmpty()) {
                FirebaseAnalyticsManager.logClickPlayVideo(currentTitle, playUrl)
                AcePlayerActivity.start(this, playUrl, currentTitle, referer)
            } else {
                Toast.makeText(this, getString(R.string.toast_live_stream_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // Nút 2: Tải video về máy (Download to device)
        btnDownload.setOnClickListener {
            val selectedQuality = if (rb1080p.isChecked) {
                detectedVideoInfo?.qualities?.firstOrNull { it.label.contains("1080p") }
                    ?: detectedVideoInfo?.qualities?.firstOrNull()
            } else if (rb720p.isChecked) {
                detectedVideoInfo?.qualities?.firstOrNull { it.label.contains("720p") }
                    ?: detectedVideoInfo?.qualities?.firstOrNull()
            } else {
                detectedVideoInfo?.qualities?.firstOrNull()
            }
            val downloadUrl = selectedQuality?.downloadUrl ?: detectedVideoUrl

            val targetMonetized = MonetizedDomainRegistry.isMonetizedDomain(activePageUrl) ||
                                  MonetizedDomainRegistry.isMonetizedDomain(downloadUrl)

            if (targetMonetized && !settingsRepository.canDownloadMonetizedVideo()) {
                bottomSheet.dismiss()
                showMonetizedPaywallNotice()
                return@setOnClickListener
            }

            bottomSheet.dismiss()
            val rawQuality = selectedQuality?.label ?: "1080p Full HD"
            val qualityLabel = cleanLabel(rawQuality)
            val referer = detectedVideoInfo?.extractedHeaders?.get("Referer") ?: activePageUrl
            if (!downloadUrl.isNullOrEmpty()) {
                FirebaseAnalyticsManager.logClickButtonDownload(downloadUrl, qualityLabel, "mp4", targetMonetized)
                if (settingsRepository.isProUser) {
                    FirebaseAnalyticsManager.logPremiumFeatureUsed("vip_fast_download")
                }
                downloadEngine.enqueueDownload(currentTitle, downloadUrl, qualityLabel, "mp4", referer)
                if (targetMonetized && !settingsRepository.isProUser) {
                    settingsRepository.incrementMonetizedDownloadCount()
                    Toast.makeText(this, getString(R.string.toast_trial_download_started), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_added_to_queue), Toast.LENGTH_SHORT).show()
                }
            }

            // Chuyển sang Tab Progress để xem tiến trình
            bottomNavigation.selectedItemId = R.id.nav_progress
        }

        bottomSheet.show()
    }

    private fun setupTopBarActions() {
        btnTabCounter.setOnClickListener {
            showTabManagerBottomSheet()
        }

        findViewById<android.view.View>(R.id.btn_settings).setOnClickListener {
            bottomNavigation.selectedItemId = R.id.nav_settings
        }
    }

    /**
     * Mở Trình Quản Lý Thẻ (Tab Manager BottomSheet)
     * Cho phép đóng từng tab hoặc đóng tất cả tab để giải phóng tối đa tài nguyên WebView
     */
    private fun showTabManagerBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_tabs, null)
        bottomSheet.setContentView(dialogView)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_tabs_sheet_title)
        val btnCloseAll = dialogView.findViewById<View>(R.id.btn_tabs_close_all)
        val btnNewTab = dialogView.findViewById<View>(R.id.btn_tabs_new_tab)
        val btnDismiss = dialogView.findViewById<View>(R.id.btn_tabs_dismiss)
        val containerTabs = dialogView.findViewById<LinearLayout>(R.id.container_tabs_list)

        fun renderTabs() {
            containerTabs.removeAllViews()
            val allTabs = tabManager.getAllTabs()
            tvTitle.text = getString(R.string.tab_manager_open_tabs, allTabs.size)
            btnTabCounter.text = allTabs.size.toString()

            val inflater = LayoutInflater.from(this)
            for (tab in allTabs) {
                val tabView = inflater.inflate(R.layout.item_tab_card, containerTabs, false)
                val cardRoot = tabView.findViewById<LinearLayout>(R.id.card_tab_root)
                val tvTabTitle = tabView.findViewById<TextView>(R.id.tv_tab_title)
                val tvTabUrl = tabView.findViewById<TextView>(R.id.tv_tab_url)
                val badgeActive = tabView.findViewById<TextView>(R.id.tv_tab_active_badge)
                val btnClose = tabView.findViewById<View>(R.id.btn_close_single_tab)

                val displayTitle = if (tab.title.isNotEmpty() && !tab.title.startsWith("http")) {
                    tab.title
                } else if (tab.currentUrl == "about:blank" || tab.currentUrl.isEmpty()) {
                    "Home Dashboard"
                } else {
                    tab.currentUrl
                }

                val displayUrl = if (tab.currentUrl == "about:blank" || tab.currentUrl.isEmpty()) {
                    "https://www.google.com"
                } else {
                    tab.currentUrl
                }

                tvTabTitle.text = displayTitle
                tvTabUrl.text = displayUrl

                val isActive = (tab.id == tabManager.getActiveTabId())
                if (isActive) {
                    cardRoot.setBackgroundResource(R.drawable.bg_tab_card_active)
                    badgeActive.visibility = View.VISIBLE
                } else {
                    cardRoot.setBackgroundResource(R.drawable.bg_tab_card_normal)
                    badgeActive.visibility = View.GONE
                }

                cardRoot.setOnClickListener {
                    tabManager.selectTab(tab.id)
                    btnTabCounter.text = tabManager.tabsCount.toString()
                    if (tab.currentUrl == "about:blank" || tab.currentUrl.isEmpty()) {
                        homeFragment.resetToHomeDashboard()
                    } else {
                        homeFragment.loadUrl(tab.currentUrl)
                    }
                    bottomNavigation.selectedItemId = R.id.nav_home
                    bottomSheet.dismiss()
                }

                btnClose.setOnClickListener {
                    val newActive = tabManager.closeTab(tab.id)
                    btnTabCounter.text = tabManager.tabsCount.toString()
                    if (isActive) {
                        if (newActive?.currentUrl == "about:blank" || newActive?.currentUrl.isNullOrEmpty()) {
                            homeFragment.resetToHomeDashboard()
                        } else if (newActive != null) {
                            homeFragment.loadUrl(newActive.currentUrl)
                        }
                    }
                    Toast.makeText(this, getString(R.string.toast_tab_closed), Toast.LENGTH_SHORT).show()
                    renderTabs()
                }

                containerTabs.addView(tabView)
            }
        }

        renderTabs()

        btnCloseAll.setOnClickListener {
            tabManager.closeAllTabs()
            homeFragment.resetToHomeDashboard()
            btnTabCounter.text = "1"
            bottomNavigation.selectedItemId = R.id.nav_home
            Toast.makeText(this, getString(R.string.toast_all_tabs_closed), Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        btnNewTab.setOnClickListener {
            tabManager.createNewTab("Home", "about:blank")
            homeFragment.resetToHomeDashboard()
            btnTabCounter.text = tabManager.tabsCount.toString()
            bottomNavigation.selectedItemId = R.id.nav_home
            bottomSheet.dismiss()
        }

        btnDismiss.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    fun updateCurrentTabInfo(url: String, title: String) {
        tabManager.updateCurrentTabUrl(url, title)
        btnTabCounter.text = tabManager.tabsCount.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (activeFragment == homeFragment && homeFragment.canGoBack()) {
            return
        }
        if (activeFragment != homeFragment) {
            bottomNavigation.selectedItemId = R.id.nav_home
            return
        }
        super.onBackPressed()
    }
}
