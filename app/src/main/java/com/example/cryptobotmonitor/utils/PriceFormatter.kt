
package com.example.cryptobotmonitor.utils

import java.util.Locale
import kotlin.math.abs

object PriceFormatter {

    // Formatowanie ceny kryptowaluty
    fun formatPrice(
        price: Double?,
        withDollar: Boolean = true,
        missingText: String = "—"
    ): String {

        if (price == null || !price.isFinite()) {
            return missingText
        }

        val pattern = when {
            abs(price) >= 1.0 -> "%,.2f"
            abs(price) >= 0.01 -> "%.4f"
            else -> "%.8f"
        }

        val formatted = String.format(
            Locale.US,
            pattern,
            price
        )

        return if (withDollar) {
            "$" + formatted
        } else {
            formatted
        }
    }

    // Formatowanie zmiany procentowej
    fun formatChange(
        change: Double?,
        missingText: String = "—"
    ): String {

        if (change == null || !change.isFinite()) {
            return missingText
        }

        return String.format(
            Locale.US,
            "%+.2f%%",
            change
        )
    }

    // Formatowanie dużych wartości rynkowych
    fun formatMarketValue(value: Double?): String {

        if (value == null || !value.isFinite()) {
            return "—"
        }

        return when {

            abs(value) >= 1_000_000_000_000 ->
                "$" + String.format(
                    Locale.US,
                    "%.2f T",
                    value / 1_000_000_000_000
                )

            abs(value) >= 1_000_000_000 ->
                "$" + String.format(
                    Locale.US,
                    "%.2f B",
                    value / 1_000_000_000
                )

            abs(value) >= 1_000_000 ->
                "$" + String.format(
                    Locale.US,
                    "%.2f M",
                    value / 1_000_000
                )

            else -> formatPrice(value)
        }
    }

    // Formatowanie zwykłej liczby
    fun formatNumber(value: Double?): String {

        if (value == null || !value.isFinite()) {
            return "—"
        }

        return String.format(
            Locale.US,
            "%,.2f",
            value
        )
    }

    // Formatowanie ceny wyświetlanej przez bota
    fun formatOptionalPrice(price: Double?): String {

        if (price == null || !price.isFinite()) {
            return "Brak danych"
        }

        return formatPrice(
            price = price,
            withDollar = false
        ) + " USD"
    }
}
