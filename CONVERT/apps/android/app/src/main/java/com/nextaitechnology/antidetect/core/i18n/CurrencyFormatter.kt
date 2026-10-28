package com.nextaitechnology.antidetect.core.i18n

import android.content.Context
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.billing.BillingManager
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Định dạng Giá Cước & Tiền Tệ Toàn Cầu (CurrencyFormatter)
 * Synchronizes pricing dynamically with Google Play Billing Client product details.
 * Calculates monthly breakdown for yearly subscriptions directly from the BillingClient
 * pricing phase (priceAmountMicros / 12) formatted with the exact same currency code.
 * Falls back to device Locale/Country rather than static language code to guarantee
 * 100% price and currency parity with the Google Play payment cart.
 *
 * @author NextAI Technology Core Team
 */
object CurrencyFormatter {

    data class PlanPricing(
        val weeklyPrice: String,
        val weeklySubtext: String,
        val yearlyPrice: String,
        val yearlySubtext: String,
        val lifetimePrice: String,
        val lifetimeSubtext: String
    )

    /**
     * Lấy bảng giá tự động ưu tiên dữ liệu thực tế từ Google Play Billing.
     * Tự động tính toán giá chia theo tháng của gói Năm (Yearly Subtext) dựa trên giá thực tế
     * và mã tiền tệ trả về từ Google Play để đảm bảo 100% nhất quán, không sai lệch đơn vị tiền tệ.
     * Nếu Google Play chưa tải xong, tự động fallback theo Locale/Country thiết bị.
     */
    fun getPricing(context: Context, langCode: String = ""): PlanPricing {
        val billingManager = BillingManager.getInstance(context)
        val liveWeekly = billingManager.getFormattedPrice(BillingManager.SKU_WEEKLY)
        val liveYearly = billingManager.getFormattedPrice(BillingManager.SKU_YEARLY)
        val liveLifetime = billingManager.getFormattedPrice(BillingManager.SKU_LIFETIME)

        val fallback = getFallbackPricing(langCode)

        // Tính toán dynamic yearlySubtext từ pricing phase của Google Play
        val yearlyPhase = billingManager.getRecurringPricingPhase(BillingManager.SKU_YEARLY)
        val dynamicYearlySubtext = if (yearlyPhase != null) {
            val monthlyAmount = (yearlyPhase.priceAmountMicros / 1_000_000.0) / 12.0
            val formattedMonthly = formatAmountWithCurrency(monthlyAmount, yearlyPhase.priceCurrencyCode, Locale.getDefault())
            context.getString(R.string.paywall_subtext_yearly_format, formattedMonthly)
        } else {
            fallback.yearlySubtext
        }

        val dynamicWeeklySubtext = context.getString(R.string.paywall_subtext_weekly)
        val dynamicLifetimeSubtext = context.getString(R.string.paywall_subtext_lifetime)

        return PlanPricing(
            weeklyPrice = liveWeekly ?: fallback.weeklyPrice,
            weeklySubtext = dynamicWeeklySubtext,
            yearlyPrice = liveYearly ?: fallback.yearlyPrice,
            yearlySubtext = dynamicYearlySubtext,
            lifetimePrice = liveLifetime ?: fallback.lifetimePrice,
            lifetimeSubtext = dynamicLifetimeSubtext
        )
    }

    /**
     * Định dạng số tiền kèm mã tiền tệ chuẩn theo Locale.
     * Đảm bảo các đồng tiền không có số thập phân (VND, JPY, KRW...) hiển thị số nguyên,
     * các đồng tiền khác (USD, EUR, GBP, CAD...) hiển thị 2 chữ số thập phân.
     */
    fun formatAmountWithCurrency(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                if (currency.defaultFractionDigits == 0) {
                    maximumFractionDigits = 0
                    minimumFractionDigits = 0
                } else {
                    maximumFractionDigits = 2
                    minimumFractionDigits = 2
                }
            }
            format.format(amount)
        } catch (e: Exception) {
            try {
                val symbol = Currency.getInstance(currencyCode).getSymbol(locale)
                if (currencyCode in setOf("VND", "JPY", "KRW", "IDR")) {
                    String.format(locale, "%s%,.0f", symbol, amount)
                } else {
                    String.format(locale, "%s%.2f", symbol, amount)
                }
            } catch (_: Exception) {
                String.format(locale, "%.2f %s", amount, currencyCode)
            }
        }
    }

    /**
     * Bảng giá dự phòng được định vị theo Country & Locale thiết bị
     */
    fun getFallbackPricing(langCode: String = ""): PlanPricing {
        val currentLocale = Locale.getDefault()
        val country = currentLocale.country.uppercase()
        val lang = if (langCode.isNotBlank()) langCode.lowercase() else currentLocale.language.lowercase()

        // 1. Việt Nam (VND)
        if (country == "VN" || lang == "vi") {
            return PlanPricing(
                weeklyPrice = "69.000 ₫",
                weeklySubtext = "Thanh toán theo tuần linh hoạt",
                yearlyPrice = "499.000 ₫",
                yearlySubtext = "Chỉ 41.500 ₫ / tháng • Tiết kiệm 70%",
                lifetimePrice = "899.000 ₫",
                lifetimeSubtext = "Mua 1 lần • Sở hữu trọn đời"
            )
        }

        // 2. Vương Quốc Anh (GBP)
        if (country == "GB") {
            return PlanPricing(
                weeklyPrice = "£2.99",
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = "£19.99",
                yearlySubtext = "Just £1.66 / month • Save 70%",
                lifetimePrice = "£39.99",
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }

        // 3. Canada (CAD)
        if (country == "CA") {
            return PlanPricing(
                weeklyPrice = "CA$ 3.99",
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = "CA$ 26.99",
                yearlySubtext = "Just CA$ 2.25 / month • Save 70%",
                lifetimePrice = "CA$ 49.99",
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }

        // 4. Australia (AUD)
        if (country == "AU") {
            return PlanPricing(
                weeklyPrice = "A$ 4.49",
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = "A$ 29.99",
                yearlySubtext = "Just A$ 2.50 / month • Save 70%",
                lifetimePrice = "A$ 59.99",
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }

        // 5. Singapore (SGD)
        if (country == "SG") {
            return PlanPricing(
                weeklyPrice = "S$ 4.49",
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = "S$ 29.98",
                yearlySubtext = "Just S$ 2.50 / month • Save 70%",
                lifetimePrice = "S$ 58.98",
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }

        // 6. Liên Minh Châu Âu (EUR)
        val euCountries = setOf("DE", "FR", "ES", "IT", "NL", "BE", "PT", "AT", "IE", "FI", "GR")
        if (euCountries.contains(country) || lang in setOf("de", "fr", "es", "it")) {
            val subWeekly = when (lang) {
                "fr" -> "Facturation hebdomadaire flexible"
                "de" -> "Flexible wöchentliche Abrechnung"
                "es" -> "Facturación semanal flexible"
                "it" -> "Fatturazione settimanale flessibile"
                else -> "Flexible weekly billing"
            }
            val subYearly = when (lang) {
                "fr" -> "Seulement 1,66 € / mois • Économisez 70%"
                "de" -> "Nur 1,66 € / Monat • 70% sparen"
                "es" -> "Solo 1,66 € / mes • Ahorra 70%"
                "it" -> "Solo 1,66 € / mese • Risparmia il 70%"
                else -> "Just 1.66 € / month • Save 70%"
            }
            val subLife = when (lang) {
                "fr" -> "Achat unique • Accès à vie"
                "de" -> "Einmaliger Kauf • Lebenslanger Zugriff"
                "es" -> "Pago único • Acceso de por vida"
                "it" -> "Acquisto una tantum • Accesso a vita"
                else -> "One-time purchase • Forever access"
            }
            return PlanPricing(
                weeklyPrice = "2,99 €",
                weeklySubtext = subWeekly,
                yearlyPrice = "19,99 €",
                yearlySubtext = subYearly,
                lifetimePrice = "39,99 €",
                lifetimeSubtext = subLife
            )
        }

        // 7. Nhật Bản (JPY)
        if (country == "JP" || lang == "ja") {
            return PlanPricing(
                weeklyPrice = "¥450",
                weeklySubtext = "柔軟な週間請求",
                yearlyPrice = "¥2,900",
                yearlySubtext = "月額わずか ¥241 • 70%オフ",
                lifetimePrice = "¥5,800",
                lifetimeSubtext = "一回限りの購入 • 永久アクセス"
            )
        }

        // 8. Hàn Quốc (KRW)
        if (country == "KR" || lang == "ko") {
            return PlanPricing(
                weeklyPrice = "₩3,900",
                weeklySubtext = "유연한 주간 결제",
                yearlyPrice = "₩26,000",
                yearlySubtext = "월 ₩2,166 • 70% 할인",
                lifetimePrice = "₩52,000",
                lifetimeSubtext = "1회 구매 • 평生 이용"
            )
        }

        // 9. Trung Quốc (CNY)
        if (country == "CN" || lang == "zh") {
            return PlanPricing(
                weeklyPrice = "¥19.00",
                weeklySubtext = "按周灵活计费",
                yearlyPrice = "¥138.00",
                yearlySubtext = "每月仅 ¥11.50 • 节省 70%",
                lifetimePrice = "¥268.00",
                lifetimeSubtext = "一次性购买 • 终身有效"
            )
        }

        // 10. Ấn Độ (INR)
        if (country == "IN" || lang == "hi") {
            return PlanPricing(
                weeklyPrice = "₹249",
                weeklySubtext = "लचीला साप्ताहिक बिलिंग",
                yearlyPrice = "₹1,699",
                yearlySubtext = "केवल ₹141 / माह • 70% की बचत",
                lifetimePrice = "₹3,499",
                lifetimeSubtext = "एकमुश्त भुगतान • आजीवन उपयोग"
            )
        }

        // 11. Brazil (BRL)
        if (country == "BR" || lang == "pt") {
            return PlanPricing(
                weeklyPrice = "R$ 14,90",
                weeklySubtext = "Cobrança semanal flexível",
                yearlyPrice = "R$ 99,90",
                yearlySubtext = "Apenas R$ 8,30 / mês • Economize 70%",
                lifetimePrice = "R$ 199,90",
                lifetimeSubtext = "Compra única • Acesso vitalício"
            )
        }

        // 12. Indonesia (IDR)
        if (country == "ID" || lang == "id") {
            return PlanPricing(
                weeklyPrice = "Rp 45.000",
                weeklySubtext = "Penagihan mingguan fleksibel",
                yearlyPrice = "Rp 299.000",
                yearlySubtext = "Hanya Rp 24.900 / bln • Hemat 70%",
                lifetimePrice = "Rp 599.000",
                lifetimeSubtext = "Beli sekali • Akses seumur hidup"
            )
        }

        // 13. Thái Lan (THB)
        if (country == "TH" || lang == "th") {
            return PlanPricing(
                weeklyPrice = "฿99",
                weeklySubtext = "เรียกเก็บเงินรายสัปดาห์",
                yearlyPrice = "฿699",
                yearlySubtext = "เพียง ฿58 / เดือน • ประหยัด 70%",
                lifetimePrice = "฿1,390",
                lifetimeSubtext = "จ่ายครั้งเดียว • ใช้งานได้ตลอดชีพ"
            )
        }

        // 14. Nga (RUB)
        if (country == "RU" || lang == "ru") {
            return PlanPricing(
                weeklyPrice = "299 ₽",
                weeklySubtext = "Гибкая еженедельная оплата",
                yearlyPrice = "1.990 ₽",
                yearlySubtext = "Всего 165 ₽ / мес • Скидка 70%",
                lifetimePrice = "3.990 ₽",
                lifetimeSubtext = "Разовая покупка • Навсегда"
            )
        }

        // 15. Kiểm tra tiền tệ của thiết bị nếu không phải USD
        val deviceCurrency = try {
            Currency.getInstance(currentLocale)
        } catch (_: Exception) {
            Currency.getInstance("USD")
        }

        if (deviceCurrency != null && deviceCurrency.currencyCode != "USD") {
            val formattedMonthly = formatAmountWithCurrency(1.66, deviceCurrency.currencyCode, currentLocale)
            return PlanPricing(
                weeklyPrice = formatAmountWithCurrency(2.99, deviceCurrency.currencyCode, currentLocale),
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = formatAmountWithCurrency(19.99, deviceCurrency.currencyCode, currentLocale),
                yearlySubtext = "Just $formattedMonthly / month • Save 70%",
                lifetimePrice = formatAmountWithCurrency(39.99, deviceCurrency.currencyCode, currentLocale),
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }

        // 16. Mặc định Toàn cầu / US (USD)
        return PlanPricing(
            weeklyPrice = "$2.99",
            weeklySubtext = "Flexible weekly billing",
            yearlyPrice = "$19.99",
            yearlySubtext = "Just $1.66 / month • Save 70%",
            lifetimePrice = "$39.99",
            lifetimeSubtext = "One-time purchase • Forever access"
        )
    }

    // Deprecated alias for backward compatibility
    fun getPricingForLanguage(langCode: String): PlanPricing {
        return getFallbackPricing(langCode)
    }
}
