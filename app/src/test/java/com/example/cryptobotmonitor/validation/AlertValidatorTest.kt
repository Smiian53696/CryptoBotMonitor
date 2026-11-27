
package com.example.cryptobotmonitor.validation

import org.junit.Assert.*
import org.junit.Test

class AlertValidatorTest {

    // Testy zamiany tekstu na liczbę

    @Test
    fun parsePrice_correctNumber() {
        assertEquals(
            65000.0,
            AlertValidator.parsePrice("65000")!!,
            0.000001
        )
    }

    @Test
    fun parsePrice_decimalComma() {
        assertEquals(
            65000.50,
            AlertValidator.parsePrice("65000,50")!!,
            0.000001
        )
    }

    @Test
    fun parsePrice_decimalDot() {
        assertEquals(
            65000.50,
            AlertValidator.parsePrice("65000.50")!!,
            0.000001
        )
    }

    @Test
    fun parsePrice_withSpaces() {
        assertEquals(
            65000.0,
            AlertValidator.parsePrice(" 65000 ")!!,
            0.000001
        )
    }

    @Test
    fun parsePrice_invalidText() {
        assertNull(
            AlertValidator.parsePrice("abc")
        )
    }

    @Test
    fun parsePrice_emptyText() {
        assertNull(
            AlertValidator.parsePrice("")
        )
    }

    // Testy poprawności ceny

    @Test
    fun isValidPrice_positivePrice() {
        assertTrue(
            AlertValidator.isValidPrice(65000.0)
        )
    }

    @Test
    fun isValidPrice_smallPrice() {
        assertTrue(
            AlertValidator.isValidPrice(0.00000001)
        )
    }

    @Test
    fun isValidPrice_zeroPrice() {
        assertFalse(
            AlertValidator.isValidPrice(0.0)
        )
    }

    @Test
    fun isValidPrice_negativePrice() {
        assertFalse(
            AlertValidator.isValidPrice(-100.0)
        )
    }

    @Test
    fun isValidPrice_maximumPrice() {
        assertTrue(
            AlertValidator.isValidPrice(
                1_000_000_000_000.0
            )
        )
    }

    @Test
    fun isValidPrice_aboveMaximumPrice() {
        assertFalse(
            AlertValidator.isValidPrice(
                1_000_000_000_001.0
            )
        )
    }

    @Test
    fun isValidPrice_nullPrice() {
        assertFalse(
            AlertValidator.isValidPrice(null)
        )
    }

    @Test
    fun isValidPrice_nan() {
        assertFalse(
            AlertValidator.isValidPrice(Double.NaN)
        )
    }

    @Test
    fun isValidPrice_positiveInfinity() {
        assertFalse(
            AlertValidator.isValidPrice(
                Double.POSITIVE_INFINITY
            )
        )
    }

    @Test
    fun isValidPrice_negativeInfinity() {
        assertFalse(
            AlertValidator.isValidPrice(
                Double.NEGATIVE_INFINITY
            )
        )
    }

    // Testy warunku alertu

    @Test
    fun isValidCondition_above() {
        assertTrue(
            AlertValidator.isValidCondition("ABOVE")
        )
    }

    @Test
    fun isValidCondition_below() {
        assertTrue(
            AlertValidator.isValidCondition("BELOW")
        )
    }

    @Test
    fun isValidCondition_invalidCondition() {
        assertFalse(
            AlertValidator.isValidCondition("EQUAL")
        )
    }

    @Test
    fun isValidCondition_emptyCondition() {
        assertFalse(
            AlertValidator.isValidCondition("")
        )
    }

    @Test
    fun isValidCondition_lowercaseCondition() {
        assertFalse(
            AlertValidator.isValidCondition("above")
        )
    }

    // Testy danych kryptowaluty

    @Test
    fun isValidCoin_correctData() {
        assertTrue(
            AlertValidator.isValidCoin(
                "bitcoin",
                "Bitcoin"
            )
        )
    }

    @Test
    fun isValidCoin_emptyId() {
        assertFalse(
            AlertValidator.isValidCoin(
                "",
                "Bitcoin"
            )
        )
    }

    @Test
    fun isValidCoin_emptyName() {
        assertFalse(
            AlertValidator.isValidCoin(
                "bitcoin",
                ""
            )
        )
    }

    @Test
    fun isValidCoin_blankData() {
        assertFalse(
            AlertValidator.isValidCoin(
                "   ",
                "   "
            )
        )
    }

    @Test
    fun isValidCoin_tooLongName() {
        assertFalse(
            AlertValidator.isValidCoin(
                "bitcoin",
                "a".repeat(201)
            )
        )
    }

    @Test
    fun isValidCoin_tooLongId() {
        assertFalse(
            AlertValidator.isValidCoin(
                "a".repeat(201),
                "Bitcoin"
            )
        )
    }

    @Test
    fun isValidCoin_maximumLength() {
        assertTrue(
            AlertValidator.isValidCoin(
                "a".repeat(200),
                "b".repeat(200)
            )
        )
    }
}
