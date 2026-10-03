package com.nextaitechnology.antidetect.feature.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextaitechnology.antidetect.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

import com.nextaitechnology.antidetect.core.i18n.CurrencyFormatter
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager

/**
 * Màn hình Thanh Toán & Nâng Cấp Gói Cước (Payment Wall / Subscription Activity)
 * Displays VIP Pro features, subscription pricing, and handles purchase activation.
 *
 * @author NextAI Technology Core Team
 */
class PaymentWallActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private lateinit var settingsRepository: SettingsRepository
    private var selectedPlan: String = "yearly"
    private var sheenAnimator: ValueAnimator? = null
    private var cardSheenAnimator: ValueAnimator? = null

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

        settingsRepository = SettingsRepository(this)

        val pricing = CurrencyFormatter.getPricingForLanguage(settingsRepository.languageCode)
        findViewById<TextView>(R.id.tv_price_weekly)?.text = pricing.weeklyPrice
        findViewById<TextView>(R.id.tv_subtext_weekly)?.text = pricing.weeklySubtext
        findViewById<TextView>(R.id.tv_price_yearly)?.text = pricing.yearlyPrice
        findViewById<TextView>(R.id.tv_subtext_yearly)?.text = pricing.yearlySubtext
        findViewById<TextView>(R.id.tv_price_lifetime)?.text = pricing.lifetimePrice
        findViewById<TextView>(R.id.tv_subtext_lifetime)?.text = pricing.lifetimeSubtext

        val reason = intent.getStringExtra("EXTRA_REASON")
        if (reason == "monetized_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_monetized_notice)
        } else if (reason == "gallery_limit_reached") {
            findViewById<TextView>(R.id.tv_paywall_subtitle)?.text = getString(R.string.paywall_gallery_notice)
        }

        FirebaseAnalyticsManager.logPaywallView(reason ?: "direct")

        setupPlanSelection()
        setupButtons()
        startSheenAnimation()
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

    private fun setupPlanSelection() {
        val containerWeekly = findViewById<View>(R.id.card_plan_weekly_container)
        val containerYearly = findViewById<View>(R.id.card_plan_yearly_container)
        val containerLifetime = findViewById<View>(R.id.card_plan_lifetime_container)

        val cardWeekly = findViewById<View>(R.id.card_plan_weekly)
        val cardYearly = findViewById<View>(R.id.card_plan_yearly)
        val cardLifetime = findViewById<View>(R.id.card_plan_lifetime)

        val rbWeekly = findViewById<RadioButton>(R.id.rb_plan_weekly)
        val rbYearly = findViewById<RadioButton>(R.id.rb_plan_yearly)
        val rbLifetime = findViewById<RadioButton>(R.id.rb_plan_lifetime)

        fun selectPlan(plan: String) {
            selectedPlan = plan
            rbWeekly.isChecked = (plan == "weekly")
            rbYearly.isChecked = (plan == "yearly")
            rbLifetime.isChecked = (plan == "lifetime")

            containerWeekly.setBackgroundResource(if (plan == "weekly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)
            containerYearly.setBackgroundResource(if (plan == "yearly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)
            containerLifetime.setBackgroundResource(if (plan == "lifetime") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected)

            val pricing = CurrencyFormatter.getPricingForLanguage(settingsRepository.languageCode)
            val price = when (plan) {
                "weekly" -> pricing.weeklyPrice
                "lifetime" -> pricing.lifetimePrice
                else -> pricing.yearlyPrice
            }
            FirebaseAnalyticsManager.logPaywallPlanSelected(plan, price)

            startSelectedCardSheen(plan)
        }

        val clickWeekly = View.OnClickListener { selectPlan("weekly") }
        containerWeekly.setOnClickListener(clickWeekly)
        cardWeekly.setOnClickListener(clickWeekly)

        val clickYearly = View.OnClickListener { selectPlan("yearly") }
        containerYearly.setOnClickListener(clickYearly)
        cardYearly.setOnClickListener(clickYearly)

        val clickLifetime = View.OnClickListener { selectPlan("lifetime") }
        containerLifetime.setOnClickListener(clickLifetime)
        cardLifetime.setOnClickListener(clickLifetime)

        // Khởi chạy vệt sáng trượt chéo cho plan mặc định (yearly)
        selectPlan(selectedPlan)
    }

    /**
     * Hiệu ứng vệt sáng trượt chéo nhẹ qua Card gói cước đang được chọn (Selected Plan Card Sheen)
     */
    private fun startSelectedCardSheen(plan: String) {
        val sheenWeekly = findViewById<View>(R.id.view_sheen_weekly)
        val sheenYearly = findViewById<View>(R.id.view_sheen_yearly)
        val sheenLifetime = findViewById<View>(R.id.view_sheen_lifetime)

        sheenWeekly?.visibility = if (plan == "weekly") View.VISIBLE else View.GONE
        sheenYearly?.visibility = if (plan == "yearly") View.VISIBLE else View.GONE
        sheenLifetime?.visibility = if (plan == "lifetime") View.VISIBLE else View.GONE

        val (targetSheen, targetContainer) = when (plan) {
            "weekly" -> sheenWeekly to findViewById<View>(R.id.card_plan_weekly_container)
            "lifetime" -> sheenLifetime to findViewById<View>(R.id.card_plan_lifetime_container)
            else -> sheenYearly to findViewById<View>(R.id.card_plan_yearly_container)
        }

        if (targetSheen == null || targetContainer == null) return

        targetContainer.post {
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
                    override fun onAnimationEnd(animation: Animator) {
                        targetContainer.postDelayed({
                            if (!isFinishing && !isDestroyed && selectedPlan == plan) {
                                startSelectedCardSheen(plan)
                            }
                        }, 2200L)
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
        val sheenView = findViewById<View>(R.id.view_shimmer_sheen) ?: return
        val container = findViewById<View>(R.id.layout_btn_subscribe_container) ?: return

        container.post {
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
                    override fun onAnimationEnd(animation: Animator) {
                        container.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                startSheenAnimation()
                            }
                        }, 2000L)
                    }
                })
                start()
            }
        }
    }

    override fun onDestroy() {
        sheenAnimator?.cancel()
        sheenAnimator = null
        cardSheenAnimator?.cancel()
        cardSheenAnimator = null
        super.onDestroy()
    }

    private fun handleSubscribe() {
        val pricing = CurrencyFormatter.getPricingForLanguage(settingsRepository.languageCode)
        val price = when (selectedPlan) {
            "weekly" -> pricing.weeklyPrice
            "lifetime" -> pricing.lifetimePrice
            else -> pricing.yearlyPrice
        }
        FirebaseAnalyticsManager.logPaywallCtaClicked(selectedPlan, price)
        FirebaseAnalyticsManager.logPremiumFeatureUsed("vip_pro_subscription")

        settingsRepository.isProUser = true
        settingsRepository.subscriptionPlan = selectedPlan
        Toast.makeText(this, getString(R.string.msg_pro_unlocked), Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun handleRestore() {
        FirebaseAnalyticsManager.logPaywallRestoreClicked()
        settingsRepository.isProUser = true
        settingsRepository.subscriptionPlan = "yearly"
        Toast.makeText(this, "✅ " + getString(R.string.msg_pro_unlocked), Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
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
            .setNegativeButton("OK", null)
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
