package com.nextaitechnology.antidetect.core.i18n

import android.content.Context
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.billing.BillingManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Định dạng Giá Cước & Tiền Tệ Toàn Cầu (CurrencyFormatter)
 * - Synchronizes pricing dynamically with Google Play Billing Client product details.
 * - Clones exact currency symbol prefix/suffix and number separators directly from Google Play's
 *   formattedPrice (formatAmountMatchingPlayPrice) to guarantee 100% visual currency parity
 *   between weeklyPrice, yearlyPrice, yearlySubtext, lifetimePrice, and the Google Play Payment Cart.
 * - Preserves device ISO 3166-1 country via LocaleHelper (SIM, Network, System Locale, TimeZone,
 *   and 50-language mapping) so non-US countries never fall back to USD.
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

    private val ZERO_DECIMAL_CURRENCIES = setOf(
        "VND", "JPY", "KRW", "IDR", "CLP", "PYG", "ISK", "HUF",
        "TWD", "COP", "PKR", "IRR", "MMK", "KHR", "UGX", "RWF", "XAF", "XOF"
    )

    /**
     * Lấy bảng giá tự động ưu tiên dữ liệu thực tế từ Google Play Billing.
     * Tự động tính toán giá chia theo tháng của gói Năm (Yearly Subtext) bằng cách sao chép
     * chính xác ký hiệu tiền tệ (prefix/suffix) và dấu phân cách của liveYearly từ Google Play
     * để đảm bảo 100% nhất quán tuyệt đối, không bao giờ sai lệch đơn vị tiền tệ.
     */
    fun getPricing(context: Context, langCode: String = ""): PlanPricing {
        val billingManager = BillingManager.getInstance(context)
        val liveWeekly = billingManager.getFormattedPrice(BillingManager.SKU_WEEKLY)
        val liveYearly = billingManager.getFormattedPrice(BillingManager.SKU_YEARLY)
        val liveLifetime = billingManager.getFormattedPrice(BillingManager.SKU_LIFETIME)

        val fallback = getFallbackPricing(langCode, context)

        // Nếu có ít nhất 1 gói từ Google Play đã tải xong nhưng gói khác chưa tải kịp,
        // đồng bộ toàn bộ theo đúng mã tiền tệ và định dạng của Google Play để không bao giờ trộn 2 loại tiền tệ trên cùng màn hình
        val detectedPlayCurrency = billingManager.getDetectedPlayCurrencyCode()
        val detectedRefPrice = billingManager.getDetectedReferenceFormattedPrice()

        val alignedFallback = if (!detectedPlayCurrency.isNullOrBlank()) {
            buildPricingForCurrencyCode(detectedPlayCurrency, detectedRefPrice, langCode, context)
        } else {
            fallback
        }

        // Tính toán dynamic yearlySubtext từ pricing phase trả phí thực tế của Google Play
        val yearlyPhase = billingManager.getRecurringPricingPhase(BillingManager.SKU_YEARLY)
        val dynamicYearlySubtext = if (yearlyPhase != null && yearlyPhase.priceAmountMicros > 0) {
            val monthlyAmount = (yearlyPhase.priceAmountMicros / 1_000_000.0) / 12.0
            val referencePrice = liveYearly ?: detectedRefPrice
            val formattedMonthly = formatAmountMatchingPlayPrice(
                amount = monthlyAmount,
                currencyCode = yearlyPhase.priceCurrencyCode,
                referenceFormattedPrice = referencePrice,
                locale = LocaleHelper.getSystemDeviceLocale()
            )
            context.getString(R.string.paywall_subtext_yearly_format, formattedMonthly)
        } else {
            alignedFallback.yearlySubtext
        }

        val dynamicWeeklySubtext = context.getString(R.string.paywall_subtext_weekly)
        val dynamicLifetimeSubtext = context.getString(R.string.paywall_subtext_lifetime)

        return PlanPricing(
            weeklyPrice = liveWeekly ?: alignedFallback.weeklyPrice,
            weeklySubtext = dynamicWeeklySubtext,
            yearlyPrice = liveYearly ?: alignedFallback.yearlyPrice,
            yearlySubtext = dynamicYearlySubtext,
            lifetimePrice = liveLifetime ?: alignedFallback.lifetimePrice,
            lifetimeSubtext = dynamicLifetimeSubtext
        )
    }

    /**
     * Định dạng số tiền dựa theo chính xác mẫu chuỗi formattedPrice trả về từ Google Play Billing.
     * Ví dụ:
     * - referenceFormattedPrice = "$19.99" -> "$1.67"
     * - referenceFormattedPrice = "US$19.99" -> "US$1.67"
     * - referenceFormattedPrice = "CA$ 26.99" -> "CA$ 2.25"
     * - referenceFormattedPrice = "19,99 €" -> "1,67 €"
     * - referenceFormattedPrice = "499.000 ₫" -> "41.583 ₫"
     * - referenceFormattedPrice = "Rp 299.000" -> "Rp 24.917"
     * - referenceFormattedPrice = "₹1,699.00" -> "₹141.58"
     * Đảm bảo 100% ký hiệu tiền tệ của subtext giống hệt ký hiệu tiền tệ của giá chính và Google Play Payment Cart.
     */
    fun formatAmountMatchingPlayPrice(
        amount: Double,
        currencyCode: String,
        referenceFormattedPrice: String?,
        locale: Locale = Locale.getDefault()
    ): String {
        if (!referenceFormattedPrice.isNullOrBlank()) {
            val firstDigitIdx = referenceFormattedPrice.indexOfFirst { it in '0'..'9' }
            val lastDigitIdx = referenceFormattedPrice.indexOfLast { it in '0'..'9' }
            if (firstDigitIdx >= 0 && lastDigitIdx >= firstDigitIdx) {
                val prefix = referenceFormattedPrice.substring(0, firstDigitIdx)
                val suffix = referenceFormattedPrice.substring(lastDigitIdx + 1)
                val numericCore = referenceFormattedPrice.substring(firstDigitIdx, lastDigitIdx + 1)

                val lastDot = numericCore.lastIndexOf('.')
                val lastComma = numericCore.lastIndexOf(',')
                val lastSepIdx = maxOf(lastDot, lastComma)

                val digitsAfterLastSep = if (lastSepIdx >= 0) numericCore.length - 1 - lastSepIdx else 0
                val isZeroDecimal = ZERO_DECIMAL_CURRENCIES.contains(currencyCode.uppercase(Locale.ROOT)) ||
                        (lastSepIdx < 0) ||
                        (digitsAfterLastSep == 3)

                if (isZeroDecimal) {
                    val groupSep = when {
                        lastSepIdx >= 0 -> numericCore[lastSepIdx]
                        currencyCode.uppercase(Locale.ROOT) in setOf("VND", "IDR") -> '.'
                        else -> ','
                    }
                    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
                        groupingSeparator = groupSep
                    }
                    val df = DecimalFormat("#,###", symbols).apply {
                        isGroupingUsed = true
                        maximumFractionDigits = 0
                        minimumFractionDigits = 0
                    }
                    return "$prefix${df.format(amount.roundToLong())}$suffix"
                } else {
                    val decimalSep = if (lastSepIdx >= 0) numericCore[lastSepIdx] else '.'
                    val groupSep = if (decimalSep == ',') '.' else ','
                    val fractionDigits = if (digitsAfterLastSep in 1..2) digitsAfterLastSep else 2
                    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
                        decimalSeparator = decimalSep
                        groupingSeparator = groupSep
                    }
                    val pattern = "#,##0." + "0".repeat(fractionDigits)
                    val df = DecimalFormat(pattern, symbols).apply {
                        isGroupingUsed = true
                        maximumFractionDigits = fractionDigits
                        minimumFractionDigits = fractionDigits
                    }
                    return "$prefix${df.format(amount)}$suffix"
                }
            }
        }
        return formatAmountWithCurrency(amount, currencyCode, locale)
    }

    /**
     * Định dạng số tiền kèm mã tiền tệ chuẩn theo Locale.
     * Đảm bảo các đồng tiền không có số thập phân (VND, JPY, KRW...) hiển thị số nguyên,
     * các đồng tiền khác (USD, EUR, GBP, CAD...) hiển thị 2 chữ số thập phân.
     */
    fun formatAmountWithCurrency(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val cleanCode = currencyCode.trim().uppercase(Locale.ROOT)
        val effectiveLocale = if (locale.country.isBlank()) {
            LocaleHelper.resolveLocaleWithCountry(locale.language.ifEmpty { "en" })
        } else {
            locale
        }
        return try {
            val currency = Currency.getInstance(cleanCode)
            val isZeroDec = ZERO_DECIMAL_CURRENCIES.contains(cleanCode) || currency.defaultFractionDigits == 0
            val format = NumberFormat.getCurrencyInstance(effectiveLocale).apply {
                this.currency = currency
                if (isZeroDec) {
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
                val symbol = Currency.getInstance(cleanCode).getSymbol(effectiveLocale)
                if (cleanCode in ZERO_DECIMAL_CURRENCIES) {
                    String.format(effectiveLocale, "%s%,.0f", symbol, amount)
                } else {
                    String.format(effectiveLocale, "%s%.2f", symbol, amount)
                }
            } catch (_: Exception) {
                String.format(effectiveLocale, "%.2f %s", amount, cleanCode)
            }
        }
    }

    private data class CurrencyRateSpec(
        val currencyCode: String,
        val weeklyAmount: Double,
        val yearlyAmount: Double,
        val lifetimeAmount: Double,
        val sampleFormat: String? = null
    )

    private val CURRENCY_SPECS: Map<String, CurrencyRateSpec> = mapOf(
        "VND" to CurrencyRateSpec("VND", 69000.0, 499000.0, 899000.0, "499.000 ₫"),
        "GBP" to CurrencyRateSpec("GBP", 2.99, 19.99, 39.99, "£19.99"),
        "CAD" to CurrencyRateSpec("CAD", 3.99, 26.99, 49.99, "CA$ 26.99"),
        "AUD" to CurrencyRateSpec("AUD", 4.49, 29.99, 59.99, "A$ 29.99"),
        "NZD" to CurrencyRateSpec("NZD", 4.99, 32.99, 64.99, "NZ$ 32.99"),
        "SGD" to CurrencyRateSpec("SGD", 4.49, 29.98, 58.98, "S$ 29.98"),
        "EUR" to CurrencyRateSpec("EUR", 2.99, 19.99, 39.99, "19,99 €"),
        "JPY" to CurrencyRateSpec("JPY", 450.0, 2900.0, 5800.0, "¥2,900"),
        "KRW" to CurrencyRateSpec("KRW", 3900.0, 26000.0, 52000.0, "₩26,000"),
        "CNY" to CurrencyRateSpec("CNY", 19.0, 138.0, 268.0, "¥138.00"),
        "HKD" to CurrencyRateSpec("HKD", 23.0, 158.0, 318.0, "HK$ 158.00"),
        "TWD" to CurrencyRateSpec("TWD", 95.0, 650.0, 1290.0, "NT$650"),
        "INR" to CurrencyRateSpec("INR", 249.0, 1699.0, 3499.0, "₹1,699"),
        "BRL" to CurrencyRateSpec("BRL", 14.90, 99.90, 199.90, "R$ 99,90"),
        "IDR" to CurrencyRateSpec("IDR", 45000.0, 299000.0, 599000.0, "Rp 299.000"),
        "THB" to CurrencyRateSpec("THB", 99.0, 699.0, 1390.0, "฿699"),
        "RUB" to CurrencyRateSpec("RUB", 299.0, 1990.0, 3990.0, "1.990 ₽"),
        "PHP" to CurrencyRateSpec("PHP", 169.0, 1150.0, 2290.0, "₱1,150.00"),
        "MYR" to CurrencyRateSpec("MYR", 13.90, 89.90, 179.90, "RM 89.90"),
        "PLN" to CurrencyRateSpec("PLN", 12.99, 89.99, 179.99, "89,99 zł"),
        "TRY" to CurrencyRateSpec("TRY", 99.99, 699.99, 1399.99, "₺699,99"),
        "SAR" to CurrencyRateSpec("SAR", 11.99, 74.99, 149.99, "74.99 SAR"),
        "AED" to CurrencyRateSpec("AED", 10.99, 72.99, 144.99, "72.99 AED"),
        "EGP" to CurrencyRateSpec("EGP", 149.0, 999.0, 1999.0, "E£ 999.00"),
        "PKR" to CurrencyRateSpec("PKR", 850.0, 5500.0, 11000.0, "Rs 5,500"),
        "BDT" to CurrencyRateSpec("BDT", 350.0, 2300.0, 4600.0, "৳2,300"),
        "UAH" to CurrencyRateSpec("UAH", 119.0, 799.0, 1599.0, "799,00 ₴"),
        "RON" to CurrencyRateSpec("RON", 14.99, 99.99, 199.99, "99,99 RON"),
        "CZK" to CurrencyRateSpec("CZK", 69.0, 479.0, 959.0, "479,00 Kč"),
        "SEK" to CurrencyRateSpec("SEK", 32.0, 219.0, 439.0, "219,00 kr"),
        "NOK" to CurrencyRateSpec("NOK", 32.0, 219.0, 439.0, "219,00 kr"),
        "DKK" to CurrencyRateSpec("DKK", 21.0, 149.0, 299.0, "149,00 kr."),
        "HUF" to CurrencyRateSpec("HUF", 1090.0, 7490.0, 14990.0, "7.490 Ft"),
        "ILS" to CurrencyRateSpec("ILS", 10.90, 74.90, 149.90, "₪74.90"),
        "BGN" to CurrencyRateSpec("BGN", 5.99, 39.99, 79.99, "39,99 лв."),
        "RSD" to CurrencyRateSpec("RSD", 349.0, 2390.0, 4790.0, "2.390 RSD"),
        "CHF" to CurrencyRateSpec("CHF", 2.90, 19.00, 38.00, "CHF 19.00"),
        "MXN" to CurrencyRateSpec("MXN", 59.0, 399.0, 799.0, "MX$ 399.00"),
        "ZAR" to CurrencyRateSpec("ZAR", 55.0, 369.0, 739.0, "R 369.00"),
        "NGN" to CurrencyRateSpec("NGN", 4500.0, 29900.0, 59900.0, "₦29,900.00"),
        "KES" to CurrencyRateSpec("KES", 390.0, 2590.0, 5190.0, "KSh 2,590.00"),
        "USD" to CurrencyRateSpec("USD", 2.99, 19.99, 39.99, "$19.99")
    )

    private fun buildPricingForCurrencyCode(
        currencyCode: String,
        referenceFormattedPrice: String?,
        langCode: String,
        context: Context? = null
    ): PlanPricing {
        val cleanCurrency = currencyCode.trim().uppercase(Locale.ROOT)
        val spec = CURRENCY_SPECS[cleanCurrency] ?: CurrencyRateSpec(cleanCurrency, 2.99, 19.99, 39.99, referenceFormattedPrice)
        val ref = referenceFormattedPrice ?: spec.sampleFormat
        val sysLocale = LocaleHelper.getSystemDeviceLocale()

        val weeklyFormatted = formatAmountMatchingPlayPrice(spec.weeklyAmount, cleanCurrency, ref, sysLocale)
        val yearlyFormatted = formatAmountMatchingPlayPrice(spec.yearlyAmount, cleanCurrency, ref, sysLocale)
        val lifetimeFormatted = formatAmountMatchingPlayPrice(spec.lifetimeAmount, cleanCurrency, ref, sysLocale)
        val monthlyFormatted = formatAmountMatchingPlayPrice(spec.yearlyAmount / 12.0, cleanCurrency, ref, sysLocale)

        val yearlySub = if (context != null) {
            context.getString(R.string.paywall_subtext_yearly_format, monthlyFormatted)
        } else if (langCode.lowercase(Locale.ROOT) == "vi") {
            "Chỉ $monthlyFormatted / tháng • Tiết kiệm 70%"
        } else {
            "Just $monthlyFormatted / month • Save 70%"
        }

        val weeklySub = if (context != null) context.getString(R.string.paywall_subtext_weekly) else "Flexible weekly billing"
        val lifeSub = if (context != null) context.getString(R.string.paywall_subtext_lifetime) else "One-time purchase • Forever access"

        return PlanPricing(
            weeklyPrice = weeklyFormatted,
            weeklySubtext = weeklySub,
            yearlyPrice = yearlyFormatted,
            yearlySubtext = yearlySub,
            lifetimePrice = lifetimeFormatted,
            lifetimeSubtext = lifeSub
        )
    }

    /**
     * Bảng giá dự phòng được định vị chính xác theo Country ISO 3166-1 & 50 Ngôn ngữ hỗ trợ
     */
    fun getFallbackPricing(langCode: String = "", context: Context? = null): PlanPricing {
        val currentLocale = Locale.getDefault()
        val lang = if (langCode.isNotBlank()) {
            langCode.trim().lowercase(Locale.ROOT)
        } else {
            currentLocale.language.lowercase(Locale.ROOT)
        }

        val rawCountry = if (context != null) {
            LocaleHelper.getDeviceCountryCode(context)
        } else {
            currentLocale.country.trim().uppercase(Locale.ROOT).ifEmpty {
                LocaleHelper.getDeviceCountryCode(null)
            }
        }

        // Nếu mã quốc gia đang rỗng hoặc là US nhưng ngôn ngữ truyền vào là ngôn ngữ đặc thù quốc gia khác (ví dụ: "vi", "de", "ja", "hi", "ta"...)
        val country = if (rawCountry.isEmpty() || (rawCountry == "US" && lang != "en" && LocaleHelper.DEFAULT_COUNTRY_FOR_LANGUAGE.containsKey(lang))) {
            LocaleHelper.DEFAULT_COUNTRY_FOR_LANGUAGE[lang] ?: rawCountry
        } else {
            rawCountry
        }

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
        if (country == "GB" || country == "UK") {
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

        // 6. Liên Minh Châu Âu / Khu vực đồng Euro (EUR)
        val euCountries = setOf(
            "DE", "FR", "ES", "IT", "NL", "BE", "PT", "AT", "IE", "FI", "GR",
            "SK", "SI", "LT", "LV", "EE", "HR", "LU", "MT", "CY"
        )
        val euLanguages = setOf("de", "fr", "es", "it", "nl", "el", "fi", "sk", "sl", "lt", "lv", "et", "hr")
        if (euCountries.contains(country) || (country.isEmpty() && euLanguages.contains(lang))) {
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
                lifetimeSubtext = "1회 구매 • 평생 이용"
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

        // 10. Ấn Độ (INR) - Hỗ trợ cả en_IN và 7 ngôn ngữ Ấn Độ trong app (hi, ta, te, mr, gu, kn, ml)
        val indianLanguages = setOf("hi", "ta", "te", "mr", "gu", "kn", "ml")
        if (country == "IN" || indianLanguages.contains(lang)) {
            return PlanPricing(
                weeklyPrice = "₹249",
                weeklySubtext = if (lang == "hi") "लचीला साप्ताहिक बिलिंग" else "Flexible weekly billing",
                yearlyPrice = "₹1,699",
                yearlySubtext = if (lang == "hi") "केवल ₹141 / माह • 70% की बचत" else "Just ₹141.58 / month • Save 70%",
                lifetimePrice = "₹3,499",
                lifetimeSubtext = if (lang == "hi") "एकमुश्त भुगतान • आजीवन उपयोग" else "One-time purchase • Forever access"
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

        // 15. Kiểm tra tất cả các quốc gia khác theo ISO Country & Currency
        val resolvedLocale = if (country.length == 2) Locale(lang.ifEmpty { "en" }, country) else currentLocale
        val deviceCurrency = try {
            Currency.getInstance(resolvedLocale)
        } catch (_: Exception) {
            null
        }

        if (deviceCurrency != null && deviceCurrency.currencyCode != "USD") {
            return buildPricingForCurrencyCode(deviceCurrency.currencyCode, null, lang, context)
        }

        // 16. Mặc định US (USD)
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
