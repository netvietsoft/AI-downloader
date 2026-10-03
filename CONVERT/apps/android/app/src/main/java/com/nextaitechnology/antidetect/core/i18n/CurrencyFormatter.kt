package com.nextaitechnology.antidetect.core.i18n

import android.content.Context

/**
 * Định dạng Giá Cước & Tiền Tệ Toàn Cầu Theo Ngôn Ngữ Được Chọn (CurrencyFormatter)
 * Formats subscription prices and currency symbols based on the user's active language/region.
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

    fun getPricingForLanguage(langCode: String): PlanPricing {
        return when (langCode.lowercase()) {
            "vi" -> PlanPricing(
                weeklyPrice = "69.000 ₫",
                weeklySubtext = "Thanh toán theo tuần linh hoạt",
                yearlyPrice = "499.000 ₫",
                yearlySubtext = "Chỉ 41.500 ₫ / tháng • Tiết kiệm 70%",
                lifetimePrice = "899.000 ₫",
                lifetimeSubtext = "Mua 1 lần • Sở hữu trọn đời"
            )
            "zh" -> PlanPricing(
                weeklyPrice = "¥19.00",
                weeklySubtext = "按周灵活计费",
                yearlyPrice = "¥138.00",
                yearlySubtext = "每月仅 ¥11.50 • 节省 70%",
                lifetimePrice = "¥268.00",
                lifetimeSubtext = "一次性购买 • 终身有效"
            )
            "es" -> PlanPricing(
                weeklyPrice = "2,99 €",
                weeklySubtext = "Facturación semanal flexible",
                yearlyPrice = "19,99 €",
                yearlySubtext = "Solo 1,66 € / mes • Ahorra 70%",
                lifetimePrice = "39,99 €",
                lifetimeSubtext = "Pago único • Acceso de por vida"
            )
            "fr" -> PlanPricing(
                weeklyPrice = "2,99 €",
                weeklySubtext = "Facturation hebdomadaire flexible",
                yearlyPrice = "19,99 €",
                yearlySubtext = "Seulement 1,66 € / mois • Économisez 70%",
                lifetimePrice = "39,99 €",
                lifetimeSubtext = "Achat unique • Accès à vie"
            )
            "de" -> PlanPricing(
                weeklyPrice = "2,99 €",
                weeklySubtext = "Flexible wöchentliche Abrechnung",
                yearlyPrice = "19,99 €",
                yearlySubtext = "Nur 1,66 € / Monat • 70% sparen",
                lifetimePrice = "39,99 €",
                lifetimeSubtext = "Einmaliger Kauf • Lebenslanger Zugriff"
            )
            "ja" -> PlanPricing(
                weeklyPrice = "¥450",
                weeklySubtext = "柔軟な週間請求",
                yearlyPrice = "¥2,900",
                yearlySubtext = "月額わずか ¥241 • 70%オフ",
                lifetimePrice = "¥5,800",
                lifetimeSubtext = "一回限りの購入 • 永久アクセス"
            )
            "ko" -> PlanPricing(
                weeklyPrice = "₩3,900",
                weeklySubtext = "유연한 주간 결제",
                yearlyPrice = "₩26,000",
                yearlySubtext = "월 ₩2,166 • 70% 할인",
                lifetimePrice = "₩52,000",
                lifetimeSubtext = "1회 구매 • 평생 이용"
            )
            "ru" -> PlanPricing(
                weeklyPrice = "299 ₽",
                weeklySubtext = "Гибкая еженедельная оплата",
                yearlyPrice = "1.990 ₽",
                yearlySubtext = "Всего 165 ₽ / мес • Скидка 70%",
                lifetimePrice = "3.990 ₽",
                lifetimeSubtext = "Разовая покупка • Навсегда"
            )
            "pt" -> PlanPricing(
                weeklyPrice = "R$ 14,90",
                weeklySubtext = "Cobrança semanal flexível",
                yearlyPrice = "R$ 99,90",
                yearlySubtext = "Apenas R$ 8,30 / mês • Economize 70%",
                lifetimePrice = "R$ 199,90",
                lifetimeSubtext = "Compra única • Acesso vitalício"
            )
            "id" -> PlanPricing(
                weeklyPrice = "Rp 45.000",
                weeklySubtext = "Penagihan mingguan fleksibel",
                yearlyPrice = "Rp 299.000",
                yearlySubtext = "Hanya Rp 24.900 / bln • Hemat 70%",
                lifetimePrice = "Rp 599.000",
                lifetimeSubtext = "Beli sekali • Akses seumur hidup"
            )
            "hi" -> PlanPricing(
                weeklyPrice = "₹249",
                weeklySubtext = "लचीला साप्ताहिक बिलिंग",
                yearlyPrice = "₹1,699",
                yearlySubtext = "केवल ₹141 / माह • 70% की बचत",
                lifetimePrice = "₹3,499",
                lifetimeSubtext = "एकमुश्त भुगतान • आजीवन उपयोग"
            )
            "th" -> PlanPricing(
                weeklyPrice = "฿99",
                weeklySubtext = "เรียกเก็บเงินรายสัปดาห์",
                yearlyPrice = "฿699",
                yearlySubtext = "เพียง ฿58 / เดือน • ประหยัด 70%",
                lifetimePrice = "฿1,390",
                lifetimeSubtext = "จ่ายครั้งเดียว • ใช้งานได้ตลอดชีพ"
            )
            "ar" -> PlanPricing(
                weeklyPrice = "11.99 ر.س",
                weeklySubtext = "فوترة أسبوعية مرنة",
                yearlyPrice = "79.99 ر.س",
                yearlySubtext = "فقط 6.66 ر.س / شهر • وفر 70%",
                lifetimePrice = "159.99 ر.س",
                lifetimeSubtext = "شراء لمرة واحدة • مدى الحياة"
            )
            else -> PlanPricing(
                weeklyPrice = "$2.99",
                weeklySubtext = "Flexible weekly billing",
                yearlyPrice = "$19.99",
                yearlySubtext = "Just $1.66 / month • Save 70%",
                lifetimePrice = "$39.99",
                lifetimeSubtext = "One-time purchase • Forever access"
            )
        }
    }
}
