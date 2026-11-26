
package com.example.cryptobotmonitor.domain

import com.example.cryptobotmonitor.presentation.details.ChartPoint
import kotlin.math.abs

// Zakres cen na osi Y
data class ChartPriceRange(
    val lowerPrice: Double,
    val upperPrice: Double
) {
    val priceRange: Double
        get() = upperPrice - lowerPrice
}

object ChartDataProcessor {

    // Zamiana danych API na punkty wykresu
    fun processPrices(
        prices: List<List<Double>>
    ): List<ChartPoint> {

        return prices.mapNotNull { item ->

            val time = item.getOrNull(0)
            val price = item.getOrNull(1)

            if (
                time != null &&
                time.isFinite() &&
                time >= 0 &&
                time < Long.MAX_VALUE.toDouble() &&
                price != null &&
                price.isFinite()
            ) {
                ChartPoint(
                    time = time.toLong(),
                    price = price
                )
            } else {
                null
            }

        }.sortedBy { it.time }
    }

    // Sortowanie punktów według czasu
    fun sortPoints(
        points: List<ChartPoint>
    ): List<ChartPoint> {

        return points.sortedBy { it.time }
    }

    // Obliczanie zakresu cen na wykresie
    fun calculatePriceRange(
        points: List<ChartPoint>
    ): ChartPriceRange? {

        if (points.isEmpty()) return null

        val minPrice = points.minOf { it.price }
        val maxPrice = points.maxOf { it.price }

        val padding = if (maxPrice == minPrice) {

            maxOf(
                abs(maxPrice) * 0.01,
                0.00000001
            )

        } else {

            (maxPrice - minPrice) * 0.05
        }

        return ChartPriceRange(
            lowerPrice = minPrice - padding,
            upperPrice = maxPrice + padding
        )
    }

    // Obliczanie zakresu czasu
    fun calculateTimeRange(
        points: List<ChartPoint>
    ): Long {

        if (points.size < 2) return 1L

        return (
                points.maxOf { it.time } -
                        points.minOf { it.time }
                ).coerceAtLeast(1L)
    }

    // Sprawdzenie czy okres nie przekracza jednej doby
    fun isShortPeriod(timeRange: Long): Boolean {

        return timeRange <= 86_400_000L
    }

    // Szukanie punktu najbliższego miejscu dotknięcia
    fun findNearestPoint(
        points: List<ChartPoint>,
        touchX: Float,
        chartWidth: Float
    ): Int {

        if (points.isEmpty()) return -1

        if (
            !chartWidth.isFinite() ||
            chartWidth <= 0 ||
            !touchX.isFinite()
        ) {
            return 0
        }

        val fraction = (touchX / chartWidth)
            .coerceIn(0f, 1f)

        val firstTime = points.minOf { it.time }
        val lastTime = points.maxOf { it.time }

        val selectedTime = firstTime +
                ((lastTime - firstTime) *
                        fraction.toDouble()).toLong()

        return points.indices.minByOrNull { index ->

            abs(
                points[index].time - selectedTime
            )

        } ?: 0
    }
}
