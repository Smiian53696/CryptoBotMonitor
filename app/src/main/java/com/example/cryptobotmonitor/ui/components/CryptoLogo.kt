
package com.example.cryptobotmonitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.ui.theme.*

// Ikona aplikacji
@Composable
fun CryptoLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(
                color = CryptoPrimary,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(48.dp)
        ) {
            val w = size.width
            val h = size.height

            // Linia przedstawiająca wzrost ceny
            val path = Path().apply {
                moveTo(w * 0.12f, h * 0.75f)
                lineTo(w * 0.35f, h * 0.45f)
                lineTo(w * 0.55f, h * 0.60f)
                lineTo(w * 0.88f, h * 0.15f)
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Strzałka na końcu wykresu
            drawLine(
                color = Color.White,
                start = Offset(w * 0.65f, h * 0.15f),
                end = Offset(w * 0.88f, h * 0.15f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White,
                start = Offset(w * 0.88f, h * 0.15f),
                end = Offset(w * 0.88f, h * 0.38f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// Logo razem z nazwą aplikacji
@Composable
fun CryptoBrand(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CryptoLogo()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CryptoBotMonitor",
            style = MaterialTheme.typography.headlineLarge,
            color = CryptoTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Monitoruj rynek kryptowalut",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
