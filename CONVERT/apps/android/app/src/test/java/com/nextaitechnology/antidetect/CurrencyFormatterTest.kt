package com.nextaitechnology.antidetect

import com.nextaitechnology.antidetect.core.i18n.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            org.junit.Assert.assertTrue(pricing.yearlySubtext.contains("€"))
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
            org.junit.Assert.assertTrue(pricing.yearlySubtext.contains("CA$"))
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
            org.junit.Assert.assertTrue(pricing.yearlySubtext.contains("A$"))
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
            org.junit.Assert.assertTrue(pricing.yearlySubtext.contains("S$"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testFormatAmountWithCurrency_VND() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(41583.33, "VND", Locale("vi", "VN"))
        org.junit.Assert.assertTrue("Should contain currency symbol or VND: $formatted", formatted.contains("₫") || formatted.contains("VND"))
        org.junit.Assert.assertFalse("Should not contain dollar sign: $formatted", formatted.contains("$"))
    }

    @Test
    fun testFormatAmountWithCurrency_USD() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(1.6658, "USD", Locale.US)
        assertEquals("$1.67", formatted)
    }

    @Test
    fun testFormatAmountWithCurrency_EUR() {
        val formatted = CurrencyFormatter.formatAmountWithCurrency(1.6658, "EUR", Locale.GERMANY)
        org.junit.Assert.assertTrue("Should contain 1,67: $formatted", formatted.contains("1,67"))
        org.junit.Assert.assertTrue("Should contain €: $formatted", formatted.contains("€"))
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
                // If country is not US, yearlySubtext should never contain the raw '$' unless it's CA$ or A$ or S$
                if (loc.country != "US") {
                    val subtext = pricing.yearlySubtext
                    val price = pricing.yearlyPrice
                    if (!price.contains("$")) {
                        org.junit.Assert.assertFalse(
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
}
