
package com.example.cryptobotmonitor.presentation.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// Element dolnej nawigacji
private data class NavigationItem(
    val route: String,
    val label: String
)

private val navigationItems = listOf(
    NavigationItem("home", "Rynek"),
    NavigationItem("favorites", "Ulubione"),
    NavigationItem("alerts", "Alerty"),
    NavigationItem("bot", "Bot")
)

// Dolny pasek nawigacji
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Column {

        HorizontalDivider(
            color = CryptoBorder,
            thickness = 1.dp
        )

        NavigationBar(
            containerColor = CryptoBackground,
            contentColor = CryptoTextSecondary,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {

            navigationItems.forEach { item ->

                val selected = currentRoute == item.route

                NavigationBarItem(
                    selected = selected,

                    onClick = {
                        if (!selected) {
                            onNavigate(item.route)
                        }
                    },

                    icon = {
                        NavigationIcon(
                            route = item.route,
                            color = if (selected) {
                                CryptoAccent
                            } else {
                                CryptoTextSecondary
                            }
                        )
                    },

                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },

                    alwaysShowLabel = true,

                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CryptoAccent,
                        selectedTextColor = CryptoAccent,
                        unselectedIconColor = CryptoTextSecondary,
                        unselectedTextColor = CryptoTextSecondary,
                        indicatorColor = CryptoSurface
                    )
                )
            }
        }
    }
}

// Ikony nawigacji rysowane za pomocą Canvas
@Composable
private fun NavigationIcon(
    route: String,
    color: Color
) {
    Canvas(
        modifier = Modifier.size(24.dp)
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        when (route) {

            // Ikona rynku
            "home" -> {

                val path = Path().apply {
                    moveTo(w * 0.10f, h * 0.75f)
                    lineTo(w * 0.35f, h * 0.45f)
                    lineTo(w * 0.55f, h * 0.60f)
                    lineTo(w * 0.90f, h * 0.15f)

                    moveTo(w * 0.65f, h * 0.15f)
                    lineTo(w * 0.90f, h * 0.15f)
                    lineTo(w * 0.90f, h * 0.40f)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )
            }

            // Ikona ulubionych
            "favorites" -> {

                val path = Path()

                for (i in 0 until 10) {

                    val angle =
                        Math.PI * i / 5 - Math.PI / 2

                    val radius = if (i % 2 == 0) {
                        w * 0.44f
                    } else {
                        w * 0.20f
                    }

                    val x = w / 2 +
                            (cos(angle) * radius).toFloat()

                    val y = h / 2 +
                            (sin(angle) * radius).toFloat()

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                path.close()

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = stroke)
                )
            }

            // Ikona powiadomień
            "alerts" -> {

                val path = Path().apply {

                    moveTo(w * 0.15f, h * 0.75f)

                    lineTo(w * 0.25f, h * 0.62f)
                    lineTo(w * 0.25f, h * 0.35f)

                    quadraticBezierTo(
                        w * 0.50f, h * -0.05f,
                        w * 0.75f, h * 0.35f
                    )

                    lineTo(w * 0.75f, h * 0.62f)
                    lineTo(w * 0.85f, h * 0.75f)

                    close()
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    color = color,
                    radius = w * 0.07f,
                    center = Offset(
                        w * 0.50f,
                        h * 0.90f
                    )
                )
            }

            // Ikona bota
            "bot" -> {

                drawRoundRect(
                    color = color,

                    topLeft = Offset(
                        w * 0.10f,
                        h * 0.25f
                    ),

                    size = Size(
                        w * 0.80f,
                        h * 0.60f
                    ),

                    cornerRadius = CornerRadius(
                        w * 0.15f
                    ),

                    style = Stroke(width = stroke)
                )

                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.25f),
                    end = Offset(w * 0.50f, h * 0.08f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = color,
                    radius = w * 0.06f,
                    center = Offset(w * 0.35f, h * 0.48f)
                )

                drawCircle(
                    color = color,
                    radius = w * 0.06f,
                    center = Offset(w * 0.65f, h * 0.48f)
                )

                drawLine(
                    color = color,
                    start = Offset(w * 0.35f, h * 0.68f),
                    end = Offset(w * 0.65f, h * 0.68f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
