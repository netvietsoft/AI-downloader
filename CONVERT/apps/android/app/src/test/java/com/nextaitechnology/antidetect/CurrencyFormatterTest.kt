package com.nextaitechnology.antidetect

import com.nextaitechnology.antidetect.core.i18n.CurrencyFormatter
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyFormatterTest {

    @Test
    fun testFallbackPricing_Vietnam() {
        val pricing = CurrencyFormatter.getFallbackPricing("vi")
        assertNotNull(pricing)
        assertEquals("69.000 ₫", pricing.weeklyPrice)
        assertEquals("499.000 ₫", pricing.yearlyPrice)
        assertEquals("899.000 ₫", pricing.lifetimePrice)
    }

    @Test
    fun testFallbackPricing_EnglishUS() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val pricing = CurrencyFormatter.getFallbackPricing("en")
            assertNotNull(pricing)
            assertEquals("$2.99", pricing.weeklyPrice)
            assertEquals("$19.99", pricing.yearlyPrice)
            assertEquals("$39.99", pricing.lifetimePrice)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFallbackPricing_UK() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.UK)
            val pricing = CurrencyFormatter.getFallbackPricing("en")
            assertNotNull(pricing)
            assertEquals("£2.99", pricing.weeklyPrice)
            assertEquals("£19.99", pricing.yearlyPrice)
            assertEquals("£39.99", pricing.lifetimePrice)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFallbackPricing_Europe() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val pricing = CurrencyFormatter.getFallbackPricing("de")
            assertNotNull(pricing)
            assertEquals("2,99 €", pricing.weeklyPrice)
            assertEquals("19,99 €", pricing.yearlyPrice)
            assertEquals("39,99 €", pricing.lifetimePrice)
            assertTrue(pricing.yearlySubtext.contains("€"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFallbackPricing_Canada() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.CANADA)
            val pricing = CurrencyFormatter.getFallbackPricing("en")
            assertNotNull(pricing)
            assertEquals("CA$ 3.99", pricing.weeklyPrice)
            assertEquals("CA$ 26.99", pricing.yearlyPrice)
            assertEquals("CA$ 49.99", pricing.lifetimePrice)
            assertTrue(pricing.yearlySubtext.contains("CA$"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFallbackPricing_Australia() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("en", "AU"))
            val pricing = CurrencyFormatter.getFallbackPricing("en")
            assertNotNull(pricing)
            assertEquals("A$ 4.49", pricing.weeklyPrice)
            assertEquals("A$ 29.99", pricing.yearlyPrice)
            assertEquals("A$ 59.99", pricing.lifetimePrice)
            assertTrue(pricing.yearlySubtext.contains("A$"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFallbackPricing_Singapore() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("en", "SG"))
            val pricing = CurrencyFormatter.getFallbackPricing("en")
            assertNotNull(pricing)
            assertEquals("S$ 4.49", pricing.weeklyPrice)
            assertEquals("S$ 29.98", pricing.yearlyPrice)
            assertEquals("S$ 58.98", pricing.lifetimePrice)
            assertTrue(pricing.yearlySubtext.contains("S$"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFormatAmountWithCurrency_VND() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(41583.33, "VND", Locale("vi", "VN"))
        assertTrue("Should contain currency symbol or VND: $formatted", formatted.contains("₫") || formatted.contains("VND"))
        assertFalse("Should not contain dollar sign: $formatted", formatted.contains("$"))
    }

    @Test
    fun testFormatAmountWithCurrency_USD() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(1.6658, "USD", Locale.US)
        assertEquals("$1.67", formatted)
    }

    @Test
    fun testFormatAmountWithCurrency_EUR() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(1.6658, "EUR", Locale.GERMANY)
        assertTrue("Should contain 1,67: $formatted", formatted.contains("1,67"))
        assertTrue("Should contain €: $formatted", formatted.contains("€"))
    }

    @Test
    fun testCurrencyParityAcrossAllFallbacks() {
        val locales = listOf(
            Locale("vi", "VN"),
            Locale.UK,
            Locale.CANADA,
            Locale("en", "AU"),
            Locale("en", "SG"),
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.KOREA
        )
        val originalLocale = Locale.getDefault()
        try {
            for (loc in locales) {
                Locale.setDefault(loc)
                val pricing = CurrencyFormatter.getFallbackPricing()
                if (loc.country != "US") {
                    val subtext = pricing.yearlySubtext
                    val price = pricing.yearlyPrice
                    if (!price.contains("$")) {
                        assertFalse(
                            "Yearly subtext '$subtext' should not contain '$' when price is '$price' for locale $loc",
                            subtext.contains("$")
                        )
                    }
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testLocaleHelper_PreservesCountryForEnglishRegions() {
        val originalLocale = Locale.getDefault()
        try {
            // Simulate reviewer in India with English language (en_IN)
            Locale.setDefault(Locale("en", "IN"))
            val resolvedIndia = LocaleHelper.resolveLocaleWithCountry("en")
            assertEquals("IN", resolvedIndia.country)
            Locale.setDefault(resolvedIndia)
            val indiaPricing = CurrencyFormatter.getFallbackPricing("en")
            assertTrue("India en_IN should use INR ₹, got ${indiaPricing.yearlyPrice}", indiaPricing.yearlyPrice.contains("₹"))
            assertTrue("India subtext should use INR ₹, got ${indiaPricing.yearlySubtext}", indiaPricing.yearlySubtext.contains("₹"))

            // Simulate reviewer in Philippines with English language (en_PH)
            Locale.setDefault(Locale("en", "PH"))
            val resolvedPh = LocaleHelper.resolveLocaleWithCountry("en")
            assertEquals("PH", resolvedPh.country)
            Locale.setDefault(resolvedPh)
            val phPricing = CurrencyFormatter.getFallbackPricing("en")
            assertTrue("Philippines en_PH should use PHP ₱, got ${phPricing.yearlyPrice}", phPricing.yearlyPrice.contains("₱"))
            assertTrue("Philippines subtext should use PHP ₱, got ${phPricing.yearlySubtext}", phPricing.yearlySubtext.contains("₱"))

            // Simulate reviewer in Poland (pl_PL)
            Locale.setDefault(Locale("pl", "PL"))
            val resolvedPl = LocaleHelper.resolveLocaleWithCountry("pl")
            assertEquals("PL", resolvedPl.country)
            Locale.setDefault(resolvedPl)
            val plPricing = CurrencyFormatter.getFallbackPricing("pl")
            assertTrue("Poland pl_PL should use zł, got ${plPricing.yearlyPrice}", plPricing.yearlyPrice.contains("zł"))
            assertTrue("Poland subtext should use zł, got ${plPricing.yearlySubtext}", plPricing.yearlySubtext.contains("zł"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFormatAmountMatchingPlayPrice_ClonesExactPrefixSuffixAndSeparators() {
        assertEquals(
            "$1.67",
            CurrencyFormatter.formatAmountMatchingPlayPrice(19.99 / 12.0, "USD", "$19.99")
        )
        assertEquals(
            "US$1.67",
            CurrencyFormatter.formatAmountMatchingPlayPrice(19.99 / 12.0, "USD", "US$19.99")
        )
        assertEquals(
            "CA$ 2.25",
            CurrencyFormatter.formatAmountMatchingPlayPrice(26.99 / 12.0, "CAD", "CA$ 26.99")
        )
        assertEquals(
            "1,67 €",
            CurrencyFormatter.formatAmountMatchingPlayPrice(19.99 / 12.0, "EUR", "19,99 €")
        )
        assertEquals(
            "41.583 ₫",
            CurrencyFormatter.formatAmountMatchingPlayPrice(499000.0 / 12.0, "VND", "499.000 ₫")
        )
        assertEquals(
            "Rp 24.917",
            CurrencyFormatter.formatAmountMatchingPlayPrice(299000.0 / 12.0, "IDR", "Rp 299.000")
        )
        assertEquals(
            "₹141.58",
            CurrencyFormatter.formatAmountMatchingPlayPrice(1699.0 / 12.0, "INR", "₹1,699.00")
        )
        assertEquals(
            "¥242",
            CurrencyFormatter.formatAmountMatchingPlayPrice(2900.0 / 12.0, "JPY", "¥2,900")
        )
        assertEquals(
            "7,50 zł",
            CurrencyFormatter.formatAmountMatchingPlayPrice(89.99 / 12.0, "PLN", "89,99 zł")
        )
    }

    @Test
    fun testAll50SupportedLanguagesResolveValidCountryAndConsistentCurrency() {
        val originalLocale = Locale.getDefault()
        try {
            for (item in LocaleHelper.SUPPORTED_LANGUAGES) {
                val resolved = LocaleHelper.resolveLocaleWithCountry(item.code)
                assertEquals(
                    "Language ${item.code} must resolve to a 2-letter ISO country code",
                    2,
                    resolved.country.length
                )
                Locale.setDefault(resolved)
                val pricing = CurrencyFormatter.getFallbackPricing(item.code)
                assertNotNull(pricing)
                if (item.code != "en") {
                    assertFalse(
                        "Non-English language '${item.code}' (${resolved.country}) should not default to US '$19.99', got '${pricing.yearlyPrice}'",
                        pricing.yearlyPrice == "$19.99"
                    )
                    assertFalse(
                        "Non-English language '${item.code}' (${resolved.country}) should not have '$1.66' in subtext, got '${pricing.yearlySubtext}'",
                        pricing.yearlySubtext.contains("$1.66")
                    )
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
