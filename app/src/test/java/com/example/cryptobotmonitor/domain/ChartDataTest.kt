
package com.example.cryptobotmonitor.domain

import com.example.cryptobotmonitor.presentation.details.ChartPoint
import org.junit.Assert.*
import org.junit.Test

class ChartDataTest {

    private val points = listOf(
        ChartPoint(1000L, 100.0),
        ChartPoint(2000L, 150.0),
        ChartPoint(3000L, 200.0)
    )

    // Testy przetwarzania danych API

    @Test
    fun process_correctData() {

        val prices = listOf(
            listOf(1000.0, 100.0),
            listOf(2000.0, 150.0),
            listOf(3000.0, 200.0)
        )

        assertEquals(
            points,
            ChartDataProcessor.processPrices(prices)
        )
    }

    @Test
    fun process_unsortedData() {

        val prices = listOf(
            listOf(3000.0, 200.0),
            listOf(1000.0, 100.0),
            listOf(2000.0, 150.0)
        )

        assertEquals(
            points,
            ChartDataProcessor.processPrices(prices)
        )
    }

    @Test
    fun process_emptyList() {

        assertTrue(
            ChartDataProcessor
                .processPrices(emptyList())
                .isEmpty()
        )
    }

    @Test
    fun process_missingPrice() {

        val prices = listOf(
            listOf(1000.0)
        )

        assertTrue(
            ChartDataProcessor
                .processPrices(prices)
                .isEmpty()
        )
    }

    @Test
    fun process_emptyItem() {

        val prices = listOf(
            emptyList<Double>()
        )

        assertTrue(
            ChartDataProcessor
                .processPrices(prices)
                .isEmpty()
        )
    }

    @Test
    fun process_nanPrice() {

        val prices = listOf(
            listOf(1000.0, Double.NaN)
        )

        assertTrue(
            ChartDataProcessor
                .processPrices(prices)
                .isEmpty()
        )
    }

    @Test
    fun process_infinitePrice() {

        val prices = listOf(
            listOf(1000.0, Double.POSITIVE_INFINITY)
        )

        assertTrue(
            ChartDataProcessor
                .processPrices(prices)
                .isEmpty()
        )
    }

    @Test
    fun process_invalidTimestamp() {

        val prices = listOf(
            listOf(Double.NaN, 100.0),
            listOf(Double.POSITIVE_INFINITY, 200.0),
            listOf(-1000.0, 300.0)
        )

        assertTrue(
            ChartDataProcessor
                .processPrices(prices)
                .isEmpty()
        )
    }

    @Test
    fun process_mixedData() {

        val prices = listOf(
            listOf(1000.0, 100.0),
            listOf(2000.0, Double.NaN),
            listOf(3000.0, 200.0),
            emptyList()
        )

        val result = ChartDataProcessor
            .processPrices(prices)

        assertEquals(
            listOf(
                ChartPoint(1000L, 100.0),
                ChartPoint(3000L, 200.0)
            ),
            result
        )
    }

    @Test
    fun process_verySmallPrice() {

        val prices = listOf(
            listOf(1000.0, 0.00000001)
        )

        val result = ChartDataProcessor
            .processPrices(prices)

        assertEquals(
            0.00000001,
            result.first().price,
            0.000000000001
        )
    }

    // Testy sortowania punktów

    @Test
    fun sort_unsortedPoints() {

        assertEquals(
            points,
            ChartDataProcessor.sortPoints(
                points.reversed()
            )
        )
    }

    @Test
    fun sort_emptyList() {

        assertTrue(
            ChartDataProcessor
                .sortPoints(emptyList())
                .isEmpty()
        )
    }

    @Test
    fun sort_sameTimestamp() {

        val sameTimePoints = listOf(
            ChartPoint(1000L, 100.0),
            ChartPoint(1000L, 200.0)
        )

        assertEquals(
            sameTimePoints,
            ChartDataProcessor.sortPoints(sameTimePoints)
        )
    }

    // Testy zakresu cen

    @Test
    fun priceRange_correctRange() {

        val result = ChartDataProcessor
            .calculatePriceRange(points)!!

        assertEquals(
            95.0,
            result.lowerPrice,
            0.000001
        )

        assertEquals(
            205.0,
            result.upperPrice,
            0.000001
        )
    }

    @Test
    fun priceRange_totalRange() {

        val result = ChartDataProcessor
            .calculatePriceRange(points)!!

        assertEquals(
            110.0,
            result.priceRange,
            0.000001
        )
    }

    @Test
    fun priceRange_equalPrices() {

        val equalPoints = listOf(
            ChartPoint(1000L, 100.0),
            ChartPoint(2000L, 100.0)
        )

        val result = ChartDataProcessor
            .calculatePriceRange(equalPoints)!!

        assertEquals(
            99.0,
            result.lowerPrice,
            0.000001
        )

        assertEquals(
            101.0,
            result.upperPrice,
            0.000001
        )
    }

    @Test
    fun priceRange_zeroPrices() {

        val zeroPoints = listOf(
            ChartPoint(1000L, 0.0),
            ChartPoint(2000L, 0.0)
        )

        val result = ChartDataProcessor
            .calculatePriceRange(zeroPoints)!!

        assertTrue(result.priceRange > 0)
    }

    @Test
    fun priceRange_emptyList() {

        assertNull(
            ChartDataProcessor
                .calculatePriceRange(emptyList())
        )
    }

    @Test
    fun priceRange_singlePoint() {

        val result = ChartDataProcessor
            .calculatePriceRange(
                listOf(ChartPoint(1000L, 100.0))
            )!!

        assertTrue(result.lowerPrice < 100.0)
        assertTrue(result.upperPrice > 100.0)
    }

    // Testy zakresu czasu

    @Test
    fun timeRange_correctRange() {

        assertEquals(
            2000L,
            ChartDataProcessor.calculateTimeRange(points)
        )
    }

    @Test
    fun timeRange_sameTimestamps() {

        val sameTimePoints = listOf(
            ChartPoint(1000L, 100.0),
            ChartPoint(1000L, 200.0)
        )

        assertEquals(
            1L,
            ChartDataProcessor.calculateTimeRange(
                sameTimePoints
            )
        )
    }

    @Test
    fun timeRange_emptyList() {

        assertEquals(
            1L,
            ChartDataProcessor.calculateTimeRange(
                emptyList()
            )
        )
    }

    @Test
    fun period_oneDay() {

        assertTrue(
            ChartDataProcessor.isShortPeriod(
                86_400_000L
            )
        )
    }

    @Test
    fun period_longerThanOneDay() {

        assertFalse(
            ChartDataProcessor.isShortPeriod(
                86_400_001L
            )
        )
    }

    // Testy wyboru punktu wykresu

    @Test
    fun nearestPoint_start() {

        assertEquals(
            0,
            ChartDataProcessor.findNearestPoint(
                points,
                0f,
                200f
            )
        )
    }

    @Test
    fun nearestPoint_middle() {

        assertEquals(
            1,
            ChartDataProcessor.findNearestPoint(
                points,
                100f,
                200f
            )
        )
    }

    @Test
    fun nearestPoint_end() {

        assertEquals(
            2,
            ChartDataProcessor.findNearestPoint(
                points,
                200f,
                200f
            )
        )
    }

    @Test
    fun nearestPoint_beforeChart() {

        assertEquals(
            0,
            ChartDataProcessor.findNearestPoint(
                points,
                -50f,
                200f
            )
        )
    }

    @Test
    fun nearestPoint_afterChart() {

        assertEquals(
            2,
            ChartDataProcessor.findNearestPoint(
                points,
                250f,
                200f
            )
        )
    }

    @Test
    fun nearestPoint_irregularTimestamps() {

        val irregularPoints = listOf(
            ChartPoint(1000L, 100.0),
            ChartPoint(2000L, 150.0),
            ChartPoint(10000L, 200.0)
        )

        assertEquals(
            1,
            ChartDataProcessor.findNearestPoint(
                irregularPoints,
                50f,
                100f
            )
        )
    }
}
