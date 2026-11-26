
package com.example.cryptobotmonitor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.ui.theme.*

// Uniwersalna karta aplikacji
@Composable
fun CryptoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = CryptoSurface,
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = if (showBorder) {
        BorderStroke(1.dp, CryptoBorder)
    } else {
        null
    }

    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = CryptoTextPrimary
    )

    // Karta z możliwością kliknięcia
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = colors,
            border = border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    } else {
        // Zwykła karta bez kliknięcia
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = colors,
            border = border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}
