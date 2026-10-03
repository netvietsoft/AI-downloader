package com.nextaitechnology.antidetect.feature.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.content.Intent
import android.net.Uri
import com.nextaitechnology.antidetect.BuildConfig
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.i18n.LanguageItem
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import com.nextaitechnology.antidetect.feature.ad.AdMobManager
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager

/**
 * Tab Cài Đặt (SettingsFragment)
 * Quản lý 5 Card trung tâm:
 * 1. Subscription => Payment Wall (Mở màn hình VIP Pro Paywall)
 * 2. Language => Bộ chuyển đổi 50 ngôn ngữ toàn cầu tức thì
 * 3. Policy => Chính sách bảo vệ dữ liệu người dùng
 * 4. TOS => Điều khoản dịch vụ NextAI
 * 5. Version => Phiên bản v2.2.0-pro kèm nút kiểm tra cập nhật
 * + AdMob Banner Container ở chân trang
 *
 * @author NextAI Technology Core Team
 */
class SettingsFragment : Fragment() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var tvProBadgeTop: TextView
    private lateinit var tvSubBtnStatus: TextView
    private lateinit var tvCurrentLanguageName: TextView
    private lateinit var tvLangFlagIcon: TextView
    private lateinit var adContainer: FrameLayout
    private var sheenAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        settingsRepository = SettingsRepository(context)

        tvProBadgeTop = view.findViewById(R.id.tv_pro_badge_top)
        tvSubBtnStatus = view.findViewById(R.id.tv_sub_btn_status)
        tvCurrentLanguageName = view.findViewById(R.id.tv_current_language_name)
        tvLangFlagIcon = view.findViewById(R.id.tv_lang_flag_icon)
        adContainer = view.findViewById(R.id.ad_container_settings)

        view.findViewById<TextView>(R.id.tv_version_desc)?.text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

        setupCards(view)
        updateUiState()
        startSubscriptionCardSheen()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
        // Nạp AdMob banner nếu chưa phải bản Pro
        AdMobManager.loadBanner(requireActivity(), adContainer)
    }

    private fun updateUiState() {
        val isPro = settingsRepository.isProUser
        if (isPro) {
            tvProBadgeTop.text = "👑 PRO"
            tvProBadgeTop.setTextColor(0xFFF59E0B.toInt())
            tvSubBtnStatus.text = "ACTIVE"
            tvSubBtnStatus.setBackgroundResource(R.drawable.bg_pill_btn_outline)
            tvSubBtnStatus.setTextColor(0xFFF59E0B.toInt())
            adContainer.visibility = View.GONE
        } else {
            tvProBadgeTop.text = "FREE"
            tvProBadgeTop.setTextColor(0xFF94A3B8.toInt())
            tvSubBtnStatus.text = getString(R.string.btn_upgrade_pro)
            tvSubBtnStatus.setBackgroundResource(R.drawable.bg_btn_pro_gold)
            tvSubBtnStatus.setTextColor(0xFF090D16.toInt())
        }

        val curLang = LocaleHelper.getCurrentLanguage(requireContext())
        tvCurrentLanguageName.text = "${curLang.flag} ${curLang.nativeName} (${curLang.englishName})"
        tvLangFlagIcon.text = curLang.flag
    }

    private fun setupCards(rootView: View) {
        // Card 1: Subscription => Payment Wall
        val openPaywallAction = View.OnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("open_paywall", "subscription_card")
            PaymentWallActivity.start(requireContext())
        }
        rootView.findViewById<View>(R.id.card_subscription_container)?.setOnClickListener(openPaywallAction)
        rootView.findViewById<View>(R.id.card_subscription)?.setOnClickListener(openPaywallAction)

        // Card 2: Language Picker
        rootView.findViewById<LinearLayout>(R.id.card_language).setOnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("open_language_picker", settingsRepository.languageCode)
            showLanguagePickerDialog()
        }

        // Card 3: Privacy Policy (Mở trực tiếp link hoặc hiện dialog nếu lỗi)
        rootView.findViewById<LinearLayout>(R.id.card_policy).setOnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("view_privacy_policy", URL_PRIVACY_POLICY)
            openWebUrl(URL_PRIVACY_POLICY)
        }

        // Card 4: Terms of Service (TOS) (Mở trực tiếp link hoặc hiện dialog nếu lỗi)
        rootView.findViewById<LinearLayout>(R.id.card_tos).setOnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("view_tos", URL_TERMS_OF_USE)
            openWebUrl(URL_TERMS_OF_USE)
        }

        // Card 5: Version & Check Updates
        rootView.findViewById<LinearLayout>(R.id.card_version).setOnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("check_update", "v${BuildConfig.VERSION_NAME}")
            checkUpdate()
        }
        rootView.findViewById<TextView>(R.id.btn_check_update).setOnClickListener {
            FirebaseAnalyticsManager.logSettingsActionClicked("check_update_button", "v${BuildConfig.VERSION_NAME}")
            checkUpdate()
        }
    }

    /**
     * Hiệu ứng ánh sáng nhẹ trượt chéo qua card gói đăng ký AI Download Pro VIP
     */
    private fun startSubscriptionCardSheen() {
        val sheenView = view?.findViewById<View>(R.id.view_subscription_sheen) ?: return
        val container = view?.findViewById<View>(R.id.card_subscription_container) ?: return

        container.post {
            val totalWidth = container.width.toFloat()
            if (totalWidth <= 0) return@post

            sheenAnimator?.cancel()
            sheenAnimator = ValueAnimator.ofFloat(-160f, totalWidth + 160f).apply {
                duration = 1100L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    sheenView.translationX = animator.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.postDelayed({
                            if (isAdded && !isDetached && view != null) {
                                startSubscriptionCardSheen()
                            }
                        }, 2500L)
                    }
                })
                start()
            }
        }
    }

    override fun onDestroyView() {
        sheenAnimator?.cancel()
        sheenAnimator = null
        super.onDestroyView()
    }

    private fun showLanguagePickerDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_language_picker, null)
        dialog.setContentView(view)

        val rvLanguages = view.findViewById<RecyclerView>(R.id.rv_languages)
        val etSearch = view.findViewById<EditText>(R.id.et_search_lang)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close_dialog)

        btnClose.setOnClickListener { dialog.dismiss() }

        val allLangs = LocaleHelper.SUPPORTED_LANGUAGES
        val currentCode = settingsRepository.languageCode

        val adapter = LanguageAdapter(allLangs, currentCode) { selectedLang ->
            dialog.dismiss()
            if (!selectedLang.code.equals(currentCode, ignoreCase = true)) {
                FirebaseAnalyticsManager.logSettingsActionClicked("change_language", selectedLang.code)
                LocaleHelper.applyLanguage(requireContext(), selectedLang.code)
                val localizedContext = LocaleHelper.wrapContext(requireContext())
                Toast.makeText(
                    localizedContext,
                    localizedContext.getString(R.string.toast_lang_changed, selectedLang.nativeName),
                    Toast.LENGTH_SHORT
                ).show()
                // Làm mới Activity để toàn bộ chuỗi string hiển thị theo ngôn ngữ mới
                requireActivity().recreate()
            }
        }

        rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        rvLanguages.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                val filtered = if (query.isEmpty()) {
                    allLangs
                } else {
                    allLangs.filter {
                        it.nativeName.lowercase().contains(query) ||
                                it.englishName.lowercase().contains(query) ||
                                it.code.lowercase().contains(query)
                    }
                }
                adapter.updateData(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.setOnShowListener {
            (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), url, Toast.LENGTH_LONG).show()
        }
    }

    private fun showPolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.card_policy_title))
            .setMessage(getString(R.string.policy_content) + "\n\n🔗 " + URL_PRIVACY_POLICY)
            .setPositiveButton("🌐 " + getString(R.string.btn_ok)) { _, _ ->
                openWebUrl(URL_PRIVACY_POLICY)
            }
            .setNegativeButton(getString(R.string.btn_close), null)
            .show()
    }

    private fun showTosDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.card_tos_title))
            .setMessage(getString(R.string.tos_content) + "\n\n🔗 " + URL_TERMS_OF_USE)
            .setPositiveButton("🌐 " + getString(R.string.btn_ok)) { _, _ ->
                openWebUrl(URL_TERMS_OF_USE)
            }
            .setNegativeButton(getString(R.string.btn_close), null)
            .show()
    }

    private fun checkUpdate() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.card_version_title))
            .setMessage(getString(R.string.msg_update_latest) + "\n\n" + "Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            .setPositiveButton(getString(R.string.btn_ok), null)
            .setNeutralButton("🧹 Clear Cache") { _, _ ->
                try {
                    requireContext().cacheDir.deleteRecursively()
                    FirebaseAnalyticsManager.logAppClearData("browser_and_storage_cache")
                    Toast.makeText(requireContext(), "✅ Cache đã được làm sạch!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    FirebaseAnalyticsManager.logAppException(e, "ClearCache")
                }
            }
            .show()
    }

    companion object {
        const val URL_PRIVACY_POLICY = "https://sites.google.com/view/aivideodownloaderprivacy/"
        const val URL_TERMS_OF_USE = "https://sites.google.com/view/ai-videodownloader-term-of-use"
    }

    /**
     * Adapter cho danh sách 50 ngôn ngữ
     */
    private class LanguageAdapter(
        private var list: List<LanguageItem>,
        private val currentCode: String,
        private val onSelect: (LanguageItem) -> Unit
    ) : RecyclerView.Adapter<LanguageViewHolder>() {

        fun updateData(newList: List<LanguageItem>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            val holder = LanguageViewHolder(v)
            v.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < list.size) {
                    onSelect(list[pos])
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val item = list[position]
            holder.tvFlag.text = item.flag
            holder.tvNative.text = item.nativeName
            holder.tvEnglish.text = item.englishName

            val isSelected = item.code.equals(currentCode, ignoreCase = true)
            holder.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int = list.size
    }

    private class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFlag: TextView = view.findViewById(R.id.tv_lang_flag)
        val tvNative: TextView = view.findViewById(R.id.tv_lang_native_name)
        val tvEnglish: TextView = view.findViewById(R.id.tv_lang_english_name)
        val ivSelected: ImageView = view.findViewById(R.id.iv_lang_selected)
    }
}
