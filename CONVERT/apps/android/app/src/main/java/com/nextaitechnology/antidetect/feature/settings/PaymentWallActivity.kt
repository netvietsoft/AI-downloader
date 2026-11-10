package com.nextaitechnology.antidetect.feature.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import com.nextaitechnology.antidetect.core.billing.BillingManager
import com.nextaitechnology.antidetect.core.i18n.CurrencyFormatter
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Màn hình Thanh Toán & Nâng Cấp Gói Cước (Payment Wall / Subscription Activity)
 * Displays VIP Pro features, subscription pricing, and handles Google Play purchase activation.
 * Guarantees 100% currency consistency between the Offers Page and Google Play Payment Cart.
 *
 * @author NextAI Technology Core Team
 */
class PaymentWallActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var billingManager: BillingManager
    private var selectedPlan: String = "yearly"
    private var sheenAnimator: ValueAnimator? = null
    private var cardSheenAnimator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cardSheenRunnable: Runnable? = null
    private var ctaSheenRunnable: Runnable? = null
    private var cachedPricing: CurrencyFormatter.PlanPricing? = null

    // Cached View references to eliminate repeated findViewById() calls during plan switching
    private var tvPriceWeekly: TextView? = null
    private var tvSubtextWeekly: TextView? = null
    private var tvPriceYearly: TextView? = null
    private var tvSubtextYearly: TextView? = null
    private var tvPriceLifetime: TextView? = null
    private var tvSubtextLifetime: TextView? = null
    private var sheenWeekly: View? = null
    private var sheenYearly: View? = null
    private var sheenLifetime: View? = null
    private var containerWeeklyRef: View? = null
    private var containerYearlyRef: View? = null
    private var containerLifetimeRef: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_payment_wall)

        findViewById<View>(R.id.root_payment_wall)?.let { root ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                val insets = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )
                root.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                windowInsets
            }
        }

        tvPriceWeekly = findViewById(R.id.tv_price_weekly)
        tvSubtextWeekly = findViewById(R.id.tv_subtext_weekly)
        tvPriceYearly = findViewById(R.id.tv_price_yearly)
        tvSubtextYearly = findViewById(R.id.tv_subtext_yearly)
        tvPriceLifetime = findViewById(R.id.tv_price_lifetime)
        tvSubtextLifetime = findViewById(R.id.tv_subtext_lifetime)

        sheenWeekly = findViewById<View>(R.id.view_sheen_weekly)?.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        sheenYearly = findViewById<View>(R.id.view_sheen_yearly)?.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        sheenLifetime = findViewById<View>(R.id.view_sheen_lifetime)?.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        containerWeeklyRef = findViewById(R.id.card_plan_weekly_container)
        containerYearlyRef = findViewById(R.id.card_plan_yearly_container)
        containerLifetimeRef = findViewById(R.id.card_plan_lifetime_container)

        settingsRepository = SettingsRepository(this)
        billingManager = BillingManager.getInstance(this)

        // Hiển thị giá nếu đã có dữ liệu từ Google Play, hoặc chờ kết quả truy vấn đang diễn ra
        refreshPricing()

        // Lắng nghe cập nhật giá và tiền tệ thực tế từ Google Play Billing
        lifecycleScope.launch {
            billingManager.productDetailsMap.collectLatest {
                runOnUiThread {
                    refreshPricing()
                }
            }
        }

        lifecycleScope.launch {
            billingManager.isLoadingProducts.collectLatest {
                runOnUiThread {
                    refreshPricing()
                }
            }
        }

        val reason = intent.getStringExtra("EXTRA_REASON")
        if (reason == "monetized_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_monetized_notice)
        } else if (reason == "gallery_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_gallery_notice)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            FirebaseAnalyticsManager.logPaywallView(reason ?: "direct")
        }

        setupPlanSelection()
        setupButtons()
        startSheenAnimation()
    }

    override fun onResume() {
        super.onResume()
        refreshPricing()
        try {
            billingManager.queryAvailableProducts {
                runOnUiThread { refreshPricing() }
            }
        } catch (e: Exception) {
            android.util.Log.e("PaymentWallActivity", "Error querying billing products in onResume", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val reason = intent.getStringExtra("EXTRA_REASON")
        if (reason == "monetized_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_monetized_notice)
        } else if (reason == "gallery_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_gallery_notice)
        }
    }

    private fun TextView?.setTextIfChanged(newText: String) {
        if (this != null && this.text?.toString() != newText) {
            this.text = newText
        }
    }

    private fun refreshPricing() {
        val isStillLoadingInitial = billingManager.isLoadingProducts.value && !billingManager.hasAnyLoadedProduct()
        if (isStillLoadingInitial) {
            val loadingText = getString(R.string.paywall_price_loading)
            tvPriceWeekly.setTextIfChanged(loadingText)
            tvSubtextWeekly.setTextIfChanged(getString(R.string.paywall_subtext_weekly))
            tvPriceYearly.setTextIfChanged(loadingText)
            tvSubtextYearly.setTextIfChanged(getString(R.string.paywall_subtext_yearly))
            tvPriceLifetime.setTextIfChanged(loadingText)
            tvSubtextLifetime.setTextIfChanged(getString(R.string.paywall_subtext_lifetime))
            return
        }

        val pricing = CurrencyFormatter.getPricing(this, settingsRepository.languageCode)
        cachedPricing = pricing
        tvPriceWeekly.setTextIfChanged(pricing.weeklyPrice)
        tvSubtextWeekly.setTextIfChanged(pricing.weeklySubtext)
        tvPriceYearly.setTextIfChanged(pricing.yearlyPrice)
        tvSubtextYearly.setTextIfChanged(pricing.yearlySubtext)
        tvPriceLifetime.setTextIfChanged(pricing.lifetimePrice)
        tvSubtextLifetime.setTextIfChanged(pricing.lifetimeSubtext)
    }

    private fun setupPlanSelection() {
        val containerWeekly = containerWeeklyRef ?: findViewById(R.id.card_plan_weekly_container)
        val containerYearly = containerYearlyRef ?: findViewById(R.id.card_plan_yearly_container)
        val containerLifetime = containerLifetimeRef ?: findViewById(R.id.card_plan_lifetime_container)

        val cardWeekly = findViewById<View>(R.id.card_plan_weekly)
        val cardYearly = findViewById<View>(R.id.card_plan_yearly)
        val cardLifetime = findViewById<View>(R.id.card_plan_lifetime)

        val rbWeekly = findViewById<RadioButton>(R.id.rb_plan_weekly)
        val rbYearly = findViewById<RadioButton>(R.id.rb_plan_yearly)
        val rbLifetime = findViewById<RadioButton>(R.id.rb_plan_lifetime)

        fun selectPlan(plan: String, forceInit: Boolean = false) {
            if (!forceInit && selectedPlan == plan) return
            selectedPlan = plan

            rbWeekly.isChecked = (plan == "weekly")
            rbYearly.isChecked = (plan == "yearly")
            rbLifetime.isChecked = (plan == "lifetime")

            containerWeekly.setBackgroundResource(if (plan == "weekly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)
            containerYearly.setBackgroundResource(if (plan == "yearly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)
            containerLifetime.setBackgroundResource(if (plan == "lifetime") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)

            startSelectedCardSheen(plan)

            val current = cachedPricing
            val price = when (plan) {
                "weekly" -> current?.weeklyPrice ?: ""
                "lifetime" -> current?.lifetimePrice ?: ""
                else -> current?.yearlyPrice ?: ""
            }
            lifecycleScope.launch(Dispatchers.IO) {
                FirebaseAnalyticsManager.logPaywallPlanSelected(plan, price)
            }
        }

        val clickWeekly = View.OnClickListener { selectPlan("weekly") }
        containerWeekly.setOnClickListener(clickWeekly)
        cardWeekly.setOnClickListener(clickWeekly)
        rbWeekly.setOnClickListener(clickWeekly)

        val clickYearly = View.OnClickListener { selectPlan("yearly") }
        containerYearly.setOnClickListener(clickYearly)
        cardYearly.setOnClickListener(clickYearly)
        rbYearly.setOnClickListener(clickYearly)

        val clickLifetime = View.OnClickListener { selectPlan("lifetime") }
        containerLifetime.setOnClickListener(clickLifetime)
        cardLifetime.setOnClickListener(clickLifetime)
        rbLifetime.setOnClickListener(clickLifetime)

        // Khởi chạy vệt sáng trượt chéo cho plan mặc định (yearly)
        selectPlan(selectedPlan, forceInit = true)
    }

    /**
     * Hiệu ứng vệt sáng trượt chéo nhẹ qua Card gói cước đang được chọn (Selected Plan Card Sheen)
     * Sử dụng View.INVISIBLE thay vì View.GONE để tránh hoàn toàn việc ConstraintLayout phải re-layout.
     */
    private fun startSelectedCardSheen(plan: String) {
        cardSheenRunnable?.let { mainHandler.removeCallbacks(it) }
        cardSheenRunnable = null
        cardSheenAnimator?.cancel()
        cardSheenAnimator = null

        val sWeekly = sheenWeekly
        val sYearly = sheenYearly
        val sLifetime = sheenLifetime

        sWeekly?.visibility = if (plan == "weekly") View.VISIBLE else View.INVISIBLE
        sYearly?.visibility = if (plan == "yearly") View.VISIBLE else View.INVISIBLE
        sLifetime?.visibility = if (plan == "lifetime") View.VISIBLE else View.INVISIBLE

        val (targetSheen, targetContainer) = when (plan) {
            "weekly" -> sWeekly to containerWeeklyRef
            "lifetime" -> sLifetime to containerLifetimeRef
            else -> sYearly to containerYearlyRef
        }

        if (targetSheen == null || targetContainer == null) return
        targetSheen.translationX = -160f

        targetContainer.post {
            if (isFinishing || isDestroyed || selectedPlan != plan) return@post
            val totalWidth = targetContainer.width.toFloat()
            if (totalWidth <= 0) return@post

            cardSheenAnimator?.cancel()
            cardSheenAnimator = ValueAnimator.ofFloat(-160f, totalWidth + 160f).apply {
                duration = 1100L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    targetSheen.translationX = animator.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    private var isCancelled = false

                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (isCancelled || isFinishing || isDestroyed || selectedPlan != plan) return
                        val nextRun = Runnable {
                            if (!isFinishing && !isDestroyed && selectedPlan == plan) {
                                startSelectedCardSheen(plan)
                            }
                        }
                        cardSheenRunnable = nextRun
                        mainHandler.postDelayed(nextRun, 2200L)
                    }
                })
                start()
            }
        }
    }

    private fun setupButtons() {
        findViewById<ImageView>(R.id.btn_close_paywall).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btn_restore_top).setOnClickListener {
            handleRestore()
        }

        val subscribeAction = View.OnClickListener {
            handleSubscribe()
        }
        findViewById<View>(R.id.layout_btn_subscribe_container)?.setOnClickListener(subscribeAction)
        findViewById<View>(R.id.btn_paywall_subscribe)?.setOnClickListener(subscribeAction)

        findViewById<TextView>(R.id.tv_paywall_legal).setOnClickListener {
            showLegalDialog()
        }
    }

    /**
     * Hiệu ứng vệt sáng trượt chéo nhẹ qua nút SUBSCRIBE NOW (Sheen Shimmer Effect)
     */
    private fun startSheenAnimation() {
        ctaSheenRunnable?.let { mainHandler.removeCallbacks(it) }
        ctaSheenRunnable = null
        val sheenView = findViewById<View>(R.id.view_shimmer_sheen) ?: return
        val container = findViewById<View>(R.id.layout_btn_subscribe_container) ?: return
        sheenView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        container.post {
            if (isFinishing || isDestroyed) return@post
            val totalWidth = container.width.toFloat()
            if (totalWidth <= 0) return@post

            sheenAnimator?.cancel()
            sheenAnimator = ValueAnimator.ofFloat(-120f, totalWidth + 120f).apply {
                duration = 900L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    sheenView.translationX = animator.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    private var isCancelled = false

                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (isCancelled || isFinishing || isDestroyed) return
                        val nextRun = Runnable {
                            if (!isFinishing && !isDestroyed) {
                                startSheenAnimation()
                            }
                        }
                        ctaSheenRunnable = nextRun
                        mainHandler.postDelayed(nextRun, 2000L)
                    }
                })
                start()
            }
        }
    }

    override fun onDestroy() {
        cardSheenRunnable?.let { mainHandler.removeCallbacks(it) }
        cardSheenRunnable = null
        ctaSheenRunnable?.let { mainHandler.removeCallbacks(it) }
        ctaSheenRunnable = null
        sheenAnimator?.cancel()
        sheenAnimator = null
        cardSheenAnimator?.cancel()
        cardSheenAnimator = null
        super.onDestroy()
    }

    private fun handleSubscribe() {
        val sku = when (selectedPlan) {
            "weekly" -> BillingManager.SKU_WEEKLY
            "lifetime" -> BillingManager.SKU_LIFETIME
            else -> BillingManager.SKU_YEARLY
        }

        val pricing = CurrencyFormatter.getPricing(this, settingsRepository.languageCode)
        val price = when (selectedPlan) {
            "weekly" -> pricing.weeklyPrice
            "lifetime" -> pricing.lifetimePrice
            else -> pricing.yearlyPrice
        }
        FirebaseAnalyticsManager.logPaywallCtaClicked(selectedPlan, price)
        FirebaseAnalyticsManager.logPremiumFeatureUsed("vip_pro_subscription")

        val btnSubscribe = findViewById<TextView>(R.id.btn_paywall_subscribe)
        val originalText = btnSubscribe?.text?.toString() ?: getString(R.string.paywall_btn_subscribe)
        btnSubscribe?.text = "⏳ " + getString(R.string.btn_upgrade_pro) + "..."
        btnSubscribe?.isEnabled = false

        val cachedDetails = billingManager.getProductDetails(sku)
        if (cachedDetails == null) {
            val connectingDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.billing_dialog_title))
                .setMessage(getString(R.string.billing_connecting))
                .setCancelable(false)
                .create()
            connectingDialog.show()

            billingManager.queryAvailableProducts {
                runOnUiThread {
                    if (connectingDialog.isShowing) {
                        connectingDialog.dismiss()
                    }
                    // Bắt buộc đồng bộ lại giá trên màn hình Offer ngay lập tức trước khi mở Google Play Cart
                    refreshPricing()
                    startBillingFlowInternal(sku, originalText)
                }
            }
        } else {
            refreshPricing()
            startBillingFlowInternal(sku, originalText)
        }
    }

    private fun startBillingFlowInternal(sku: String, originalBtnText: String) {
        val btnSubscribe = findViewById<TextView>(R.id.btn_paywall_subscribe)
        billingManager.launchBillingFlow(this, sku) { success, errorMsg ->
            runOnUiThread {
                btnSubscribe?.text = originalBtnText
                btnSubscribe?.isEnabled = true

                if (success) {
                    Toast.makeText(this, "🎉 " + getString(R.string.msg_pro_unlocked), Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else if (!errorMsg.isNullOrBlank() &&
                    !errorMsg.contains("hủy", ignoreCase = true) &&
                    !errorMsg.contains("cancel", ignoreCase = true)
                ) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.billing_dialog_title))
                        .setMessage(errorMsg)
                        .setPositiveButton(getString(R.string.btn_ok), null)
                        .show()
                }
            }
        }
    }

    private fun handleRestore() {
        FirebaseAnalyticsManager.logPaywallRestoreClicked()

        val progressDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.paywall_btn_restore))
            .setMessage(getString(R.string.billing_connecting))
            .setCancelable(false)
            .create()
        progressDialog.show()

        billingManager.restorePurchases { success, message ->
            runOnUiThread {
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.paywall_btn_restore))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                        if (success) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                    .show()
            }
        }
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }

    private fun showLegalDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.card_tos_title))
            .setMessage(getString(R.string.paywall_legal_text))
            .setPositiveButton(getString(R.string.card_tos_title)) { _, _ ->
                openWebUrl(URL_TERMS_OF_USE)
            }
            .setNeutralButton(getString(R.string.card_policy_title)) { _, _ ->
                openWebUrl(URL_PRIVACY_POLICY)
            }
            .setNegativeButton(getString(R.string.btn_ok), null)
            .show()
    }

    companion object {
        const val URL_PRIVACY_POLICY = "https://sites.google.com/view/aivideodownloaderprivacy/"
        const val URL_TERMS_OF_USE = "https://sites.google.com/view/ai-videodownloader-term-of-use"

        fun start(context: Context) {
            val intent = Intent(context, PaymentWallActivity::class.java)
            context.startActivity(intent)
        }
    }
}
