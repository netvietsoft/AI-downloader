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
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
