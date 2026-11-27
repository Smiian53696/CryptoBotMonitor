
package com.example.cryptobotmonitor.utils

import org.junit.Assert.*
import org.junit.Test

class PriceFormatterTest {

    // Testy formatowania cen

    @Test
    fun price_largeValue() {
        assertEquals(
            "$60,430.00",
            PriceFormatter.formatPrice(60430.0)
        )
    }

    @Test
    fun price_decimalValue() {
        assertEquals(
            "$1,693.20",
            PriceFormatter.formatPrice(1693.2)
        )
    }

    @Test
    fun price_equalToOne() {
        assertEquals(
            "$1.00",
            PriceFormatter.formatPrice(1.0)
        )
    }

    @Test
    fun price_belowOne() {
        assertEquals(
            "$0.9989",
            PriceFormatter.formatPrice(0.9989)
        )
    }

    @Test
    fun price_equalToOneCent() {
        assertEquals(
            "$0.0100",
            PriceFormatter.formatPrice(0.01)
        )
    }

    @Test
    fun price_belowOneCent() {
        assertEquals(
            "$0.00123456",
            PriceFormatter.formatPrice(0.00123456)
        )
    }

    @Test
    fun price_verySmallValue() {
        assertEquals(
            "$0.00000001",
            PriceFormatter.formatPrice(0.00000001)
        )
    }

    @Test
    fun price_rounding() {
        assertEquals(
            "$10.13",
            PriceFormatter.formatPrice(10.126)
        )
    }

    @Test
    fun price_zero() {
        assertEquals(
            "$0.00000000",
            PriceFormatter.formatPrice(0.0)
        )
    }

    @Test
    fun price_negativeValue() {
        assertEquals(
            "$-10.50",
            PriceFormatter.formatPrice(-10.5)
        )
    }

    @Test
    fun price_withoutDollar() {
        assertEquals(
            "60,430.00",
            PriceFormatter.formatPrice(
                price = 60430.0,
                withDollar = false
            )
        )
    }

    @Test
    fun price_null() {
        assertEquals(
            "—",
            PriceFormatter.formatPrice(null)
        )
    }

    @Test
    fun price_nan() {
        assertEquals(
            "—",
            PriceFormatter.formatPrice(Double.NaN)
        )
    }

    @Test
    fun price_positiveInfinity() {
        assertEquals(
            "—",
            PriceFormatter.formatPrice(
                Double.POSITIVE_INFINITY
            )
        )
    }

    @Test
    fun price_negativeInfinity() {
        assertEquals(
            "—",
            PriceFormatter.formatPrice(
                Double.NEGATIVE_INFINITY
            )
        )
    }

    // Testy zmian procentowych

    @Test
    fun change_positive() {
        assertEquals(
            "+1.51%",
            PriceFormatter.formatChange(1.51)
        )
    }

    @Test
    fun change_negative() {
        assertEquals(
            "-0.19%",
            PriceFormatter.formatChange(-0.19)
        )
    }

    @Test
    fun change_zero() {
        assertEquals(
            "+0.00%",
            PriceFormatter.formatChange(0.0)
        )
    }

    @Test
    fun change_rounding() {
        assertEquals(
            "+1.24%",
            PriceFormatter.formatChange(1.236)
        )
    }

    @Test
    fun change_null() {
        assertEquals(
            "—",
            PriceFormatter.formatChange(null)
        )
    }

    @Test
    fun change_nan() {
        assertEquals(
            "—",
            PriceFormatter.formatChange(Double.NaN)
        )
    }

    @Test
    fun change_infinity() {
        assertEquals(
            "—",
            PriceFormatter.formatChange(
                Double.POSITIVE_INFINITY
            )
        )
    }

    // Testy wartości rynkowych

    @Test
    fun marketValue_million() {
        assertEquals(
            "$1.50 M",
            PriceFormatter.formatMarketValue(1_500_000.0)
        )
    }

    @Test
    fun marketValue_billion() {
        assertEquals(
            "$2.50 B",
            PriceFormatter.formatMarketValue(
                2_500_000_000.0
            )
        )
    }

    @Test
    fun marketValue_trillion() {
        assertEquals(
            "$1.25 T",
            PriceFormatter.formatMarketValue(
                1_250_000_000_000.0
            )
        )
    }

    @Test
    fun marketValue_belowMillion() {
        assertEquals(
            "$500,000.00",
            PriceFormatter.formatMarketValue(500_000.0)
        )
    }

    @Test
    fun marketValue_null() {
        assertEquals(
            "—",
            PriceFormatter.formatMarketValue(null)
        )
    }

    @Test
    fun marketValue_nan() {
        assertEquals(
            "—",
            PriceFormatter.formatMarketValue(Double.NaN)
        )
    }

    // Testy zwykłych liczb

    @Test
    fun number_correctValue() {
        assertEquals(
            "12,345.60",
            PriceFormatter.formatNumber(12345.6)
        )
    }

    @Test
    fun number_null() {
        assertEquals(
            "—",
            PriceFormatter.formatNumber(null)
        )
    }

    @Test
    fun number_infinity() {
        assertEquals(
            "—",
            PriceFormatter.formatNumber(
                Double.POSITIVE_INFINITY
            )
        )
    }

    // Testy formatowania dla bota

    @Test
    fun bot_optionalPrice() {
        assertEquals(
            "60,430.00 USD",
            PriceFormatter.formatOptionalPrice(60430.0)
        )
    }

    @Test
    fun bot_missingPrice() {
        assertEquals(
            "Brak danych",
            PriceFormatter.formatOptionalPrice(null)
        )
    }

    @Test
    fun bot_invalidPrice() {
        assertEquals(
            "Brak danych",
            PriceFormatter.formatOptionalPrice(Double.NaN)
        )
    }

    @Test
    fun bot_missingChange() {
        assertEquals(
            "Brak danych",
            PriceFormatter.formatChange(
                change = null,
                missingText = "Brak danych"
            )
        )
    }
}
