package com.example.cryptobotmonitor.presentation.details
import com.example.cryptobotmonitor.domain.ChartDataProcessor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.cryptobotmonitor.ui.theme.CryptoAccent
import com.example.cryptobotmonitor.ui.theme.CryptoBorder
import com.example.cryptobotmonitor.ui.theme.CryptoTextSecondary
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PriceLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {

    // Sprawdzenie czy są dane do wykresu
    if (points.size < 2) {
        Box(
            modifier = modifier.height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wykresu")
        }
        return
    }

    val sortedPoints = remember(points) {
        ChartDataProcessor.sortPoints(points)
    }

    // Kolory wykresu
    val lineColor = CryptoAccent

    val gridColor = CryptoBorder.copy(
        alpha = 0.65f
    )

    val labelColor = CryptoTextSecondary

    // Wybrany punkt wykresu
    var selectedIndex by remember(sortedPoints) {
        mutableStateOf(sortedPoints.lastIndex)
    }

    val selectedPoint = sortedPoints[selectedIndex]

    // Zakres cen na osi Y
    val range = remember(sortedPoints) {
        ChartDataProcessor.calculatePriceRange(sortedPoints)
    } ?: return

    val lowerPrice = range.lowerPrice
    val upperPrice = range.upperPrice
    val priceRange = range.priceRange

    // Zakres czasu na osi X
    val startTime = sortedPoints.first().time

    val timeRange = remember(sortedPoints) {
        ChartDataProcessor.calculateTimeRange(sortedPoints)
    }

    val isShortPeriod =
        ChartDataProcessor.isShortPeriod(timeRange)

    val axisDateFormat = remember(isShortPeriod) {
        SimpleDateFormat(
            if (isShortPeriod) "HH:mm" else "dd.MM",
            Locale.getDefault()
        )
    }

    val selectedDateFormat = remember {
        SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        // Informacje o wybranym punkcie
        Text(
            text = "Wybrana cena",
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )

        Text(
            text = "${formatChartPrice(selectedPoint.price)} USD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = lineColor
        )

        Text(
            text = selectedDateFormat.format(
                Date(selectedPoint.time)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)

                // Wybór punktu po kliknięciu
                .pointerInput(sortedPoints) {
                    detectTapGestures { offset ->

                        val chartWidth = (
                                size.width - 78.dp.toPx()
                                ).coerceAtLeast(1f)

                        selectedIndex = findNearestPoint(
                            points = sortedPoints,
                            touchX = offset.x,
                            chartWidth = chartWidth
                        )
                    }
                }

                // Wybór punktu podczas przesuwania palca
                .pointerInput(sortedPoints) {
                    detectDragGestures(

                        onDragStart = { offset ->

                            val chartWidth = (
                                    size.width - 78.dp.toPx()
                                    ).coerceAtLeast(1f)

                            selectedIndex = findNearestPoint(
                                points = sortedPoints,
                                touchX = offset.x,
                                chartWidth = chartWidth
                            )
                        }

                    ) { change, _ ->

                        change.consume()

                        val chartWidth = (
                                size.width - 78.dp.toPx()
                                ).coerceAtLeast(1f)

                        selectedIndex = findNearestPoint(
                            points = sortedPoints,
                            touchX = change.position.x,
                            chartWidth = chartWidth
                        )
                    }
                }
        ) {

            val chartWidth =
                (size.width - 78.dp.toPx()).coerceAtLeast(1f)

            val chartHeight =
                (size.height - 28.dp.toPx()).coerceAtLeast(1f)

            // Ustawienia tekstu na osiach
            val textPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = labelColor.toArgb()
                textSize = 10.sp.toPx()
            }

            // Linie poziome i ceny
            val gridLines = 4

            for (i in 0..gridLines) {

                val y = chartHeight * i / gridLines

                val price = upperPrice -
                        priceRange * i / gridLines

                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            4.dp.toPx(),
                            4.dp.toPx()
                        )
                    )
                )

                drawContext.canvas.nativeCanvas.drawText(
                    formatChartPrice(price),
                    chartWidth + 5.dp.toPx(),
                    (y + 4.dp.toPx()).coerceAtLeast(
                        10.dp.toPx()
                    ),
                    textPaint
                )
            }

            // Daty na osi czasu
            textPaint.textAlign = Paint.Align.CENTER

            for (i in 0..3) {

                val fraction = i / 3f

                val time = startTime +
                        (timeRange * fraction).toLong()

                val date = axisDateFormat.format(
                    Date(time)
                )

                val textWidth = textPaint.measureText(date)

                val x = (chartWidth * fraction)
                    .coerceIn(
                        textWidth / 2,
                        maxOf(
                            textWidth / 2,
                            chartWidth - textWidth / 2
                        )
                    )

                drawContext.canvas.nativeCanvas.drawText(
                    date,
                    x,
                    chartHeight + 20.dp.toPx(),
                    textPaint
                )
            }

            // Obliczanie pozycji punktów
            fun getX(point: ChartPoint): Float {
                return (
                        (point.time - startTime).toDouble() /
                                timeRange * chartWidth
                        ).toFloat()
            }

            fun getY(point: ChartPoint): Float {

                val normalizedPrice =
                    (point.price - lowerPrice) / priceRange

                return chartHeight -
                        normalizedPrice.toFloat() * chartHeight
            }

            // Tworzenie linii wykresu
            val linePath = Path()

            sortedPoints.forEachIndexed { index, point ->

                val x = getX(point)
                val y = getY(point)

                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }
            }

            // Wypełnienie pod wykresem
            val fillPath = Path()

            fillPath.moveTo(
                getX(sortedPoints.first()),
                chartHeight
            )

            sortedPoints.forEach { point ->
                fillPath.lineTo(
                    getX(point),
                    getY(point)
                )
            }

            fillPath.lineTo(
                getX(sortedPoints.last()),
                chartHeight
            )

            fillPath.close()

            // Wypełnienie gradientowe pod wykresem
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Rysowanie linii ceny
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Pozycja wybranego punktu
            val selectedX = getX(selectedPoint)
            val selectedY = getY(selectedPoint)

            // Linia pionowa
            drawLine(
                color = lineColor.copy(alpha = 0.7f),
                start = Offset(selectedX, 0f),
                end = Offset(selectedX, chartHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 6f)
                )
            )

            // Zaznaczenie punktu
            drawCircle(
                color = lineColor.copy(alpha = 0.2f),
                radius = 9.dp.toPx(),
                center = Offset(selectedX, selectedY)
            )

            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(selectedX, selectedY)
            )
        }
    }
}

// Szukanie punktu najbliższego miejscu dotknięcia
private fun findNearestPoint(
    points: List<ChartPoint>,
    touchX: Float,
    chartWidth: Float
): Int {

    return ChartDataProcessor.findNearestPoint(
        points = points,
        touchX = touchX,
        chartWidth = chartWidth
    )
}

// Formatowanie ceny na wykresie
private fun formatChartPrice(price: Double): String {

    val pattern = when {
        abs(price) >= 1 -> "%,.2f"
        abs(price) >= 0.01 -> "%.4f"
        else -> "%.8f"
    }

    return String.format(
        Locale.US,
        pattern,
        price
    )
}
