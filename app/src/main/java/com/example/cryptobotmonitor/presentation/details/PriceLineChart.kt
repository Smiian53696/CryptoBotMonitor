package com.example.cryptobotmonitor.presentation.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PriceLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wykresu")
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 12.dp)
    ) {
        val prices = points.map { it.price }

        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        val priceRange = maxPrice - minPrice

        val chartWidth = size.width
        val chartHeight = size.height

        // Linie pomocnicze w tle
        val gridLines = 4

        for (i in 0..gridLines) {
            val y = chartHeight / gridLines * i

            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
        }

        val path = Path()

        points.forEachIndexed { index, point ->
            val x = chartWidth * index / (points.size - 1)

            val normalizedPrice =
                if (priceRange == 0.0) {
                    0.5
                } else {
                    (point.price - minPrice) / priceRange
                }

            val y = chartHeight - (normalizedPrice.toFloat() * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Linia wykresu
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 4f
            )
        )
    }
}