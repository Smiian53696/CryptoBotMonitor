
package com.example.cryptobotmonitor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Główny motyw kolorystyczny aplikacji
private val CryptoColorScheme = darkColorScheme(

    primary = CryptoPrimary,
    onPrimary = Color.White,

    secondary = CryptoAccent,
    onSecondary = CryptoBackground,

    tertiary = CryptoPositive,
    onTertiary = CryptoBackground,

    primaryContainer = CryptoSurface,
    onPrimaryContainer = CryptoTextPrimary,

    secondaryContainer = CryptoSurface,
    onSecondaryContainer = CryptoTextPrimary,

    background = CryptoBackground,
    onBackground = CryptoTextPrimary,

    surface = CryptoSurface,
    onSurface = CryptoTextPrimary,

    surfaceVariant = CryptoSurface,
    onSurfaceVariant = CryptoTextSecondary,

    outline = CryptoBorder,
    outlineVariant = CryptoBorder,

    error = CryptoNegative,
    onError = CryptoBackground
)

// Zaokrąglenia elementów interfejsu
private val CryptoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CryptoBotMonitorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CryptoColorScheme,
        typography = Typography,
        shapes = CryptoShapes,
        content = content
    )
}
