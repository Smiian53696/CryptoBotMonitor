package com.example.cryptobotmonitor.domain

import org.junit.Test
import org.junit.Assert.*
class AlertConditionTest {

    // Testy warunku ABOVE

    @Test
    fun above_priceHigherThanTarget() {
        assertTrue(
            AlertCondition.shouldTrigger(
                66000.0,
                65000.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun above_priceEqualToTarget() {
        assertTrue(
            AlertCondition.shouldTrigger(
                65000.0,
                65000.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun above_priceLowerThanTarget() {
        assertFalse(
            AlertCondition.shouldTrigger(
                64000.0,
                65000.0,
                "ABOVE"
            )
        )
    }

    // Testy warunku BELOW

    @Test
    fun below_priceLowerThanTarget() {
        assertTrue(
            AlertCondition.shouldTrigger(
                54000.0,
                55000.0,
                "BELOW"
            )
        )
    }

    @Test
    fun below_priceEqualToTarget() {
        assertTrue(
            AlertCondition.shouldTrigger(
                55000.0,
                55000.0,
                "BELOW"
            )
        )
    }

    @Test
    fun below_priceHigherThanTarget() {
        assertFalse(
            AlertCondition.shouldTrigger(
                56000.0,
                55000.0,
                "BELOW"
            )
        )
    }

    // Testy niepoprawnych warunków

    @Test
    fun condition_invalidCondition() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                65000.0,
                "EQUAL"
            )
        )
    }

    @Test
    fun condition_emptyCondition() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                65000.0,
                ""
            )
        )
    }

    // Testy niepoprawnej aktualnej ceny

    @Test
    fun currentPrice_null() {
        assertFalse(
            AlertCondition.shouldTrigger(
                null,
                65000.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun currentPrice_zero() {
        assertFalse(
            AlertCondition.shouldTrigger(
                0.0,
                65000.0,
                "BELOW"
            )
        )
    }

    @Test
    fun currentPrice_negative() {
        assertFalse(
            AlertCondition.shouldTrigger(
                -100.0,
                65000.0,
                "BELOW"
            )
        )
    }

    @Test
    fun currentPrice_nan() {
        assertFalse(
            AlertCondition.shouldTrigger(
                Double.NaN,
                65000.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun currentPrice_positiveInfinity() {
        assertFalse(
            AlertCondition.shouldTrigger(
                Double.POSITIVE_INFINITY,
                65000.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun currentPrice_negativeInfinity() {
        assertFalse(
            AlertCondition.shouldTrigger(
                Double.NEGATIVE_INFINITY,
                65000.0,
                "BELOW"
            )
        )
    }

    // Testy niepoprawnej ceny docelowej

    @Test
    fun targetPrice_zero() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                0.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun targetPrice_negative() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                -100.0,
                "ABOVE"
            )
        )
    }

    @Test
    fun targetPrice_nan() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                Double.NaN,
                "ABOVE"
            )
        )
    }

    @Test
    fun targetPrice_positiveInfinity() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                Double.POSITIVE_INFINITY,
                "BELOW"
            )
        )
    }

    @Test
    fun targetPrice_negativeInfinity() {
        assertFalse(
            AlertCondition.shouldTrigger(
                65000.0,
                Double.NEGATIVE_INFINITY,
                "ABOVE"
            )
        )
    }

    // Testy bardzo małych cen

    @Test
    fun above_verySmallPrice() {
        assertTrue(
            AlertCondition.shouldTrigger(
                0.00000002,
                0.00000001,
                "ABOVE"
            )
        )
    }

    @Test
    fun below_verySmallPrice() {
        assertTrue(
            AlertCondition.shouldTrigger(
                0.00000001,
                0.00000002,
                "BELOW"
            )
        )
    }

    @Test
    fun condition_equalVerySmallPrices() {
        assertTrue(
            AlertCondition.shouldTrigger(
                0.00000001,
                0.00000001,
                "ABOVE"
            )
        )

        assertTrue(
            AlertCondition.shouldTrigger(
                0.00000001,
                0.00000001,
                "BELOW"
            )
        )
    }

    // Testy funkcji pomocniczych

    @Test
    fun isValidCurrentPrice_correctPrice() {
        assertTrue(
            AlertCondition.isValidCurrentPrice(65000.0)
        )
    }

    @Test
    fun isValidTargetPrice_correctPrice() {
        assertTrue(
            AlertCondition.isValidTargetPrice(65000.0)
        )
    }
}