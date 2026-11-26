
package com.example.cryptobotmonitor.presentation.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.data.preferences.MonitoringPreferences
import com.example.cryptobotmonitor.data.preferences.MonitoringStatus
import com.example.cryptobotmonitor.notification.NotificationHelper
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitoringStatusCard(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val preferences = remember(context) {
        MonitoringPreferences(context)
    }

    var monitoringStatus by remember {
        mutableStateOf(MonitoringStatus())
    }

    var notificationsEnabled by remember {
        mutableStateOf(
            NotificationHelper.canSendNotifications(context)
        )
    }

    // Aktualizacja statusu monitorowania
    LaunchedEffect(Unit) {

        while (true) {

            val userId = FirebaseAuth
                .getInstance()
                .currentUser?.uid

            monitoringStatus = if (userId != null) {

                preferences.getMonitoringStatus(userId)

            } else {

                MonitoringStatus()
            }

            notificationsEnabled =
                NotificationHelper.canSendNotifications(context)

            delay(3000)
        }
    }

    CryptoCard(
        modifier = modifier,
        showBorder = true
    ) {

        Text(
            text = "Monitorowanie cen",
            style = MaterialTheme.typography.titleMedium,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status powiadomień
        Text(
            text = if (notificationsEnabled) {
                "● Powiadomienia włączone"
            } else {
                "● Powiadomienia wyłączone"
            },

            color = if (notificationsEnabled) {
                CryptoPositive
            } else {
                CryptoNegative
            },

            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = CryptoBorder)

        Spacer(modifier = Modifier.height(16.dp))

        // Czas ostatniego udanego sprawdzenia
        Text(
            text = "Ostatnie udane sprawdzenie",
            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatCheckTime(
                monitoringStatus.lastCheckTime
            ),

            color = CryptoTextPrimary,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Liczba wykonanych alertów
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Wykonane alerty",
                    color = CryptoTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Podczas ostatniego sprawdzenia",
                    color = CryptoTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = monitoringStatus.completedAlerts.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = CryptoAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = CryptoBorder)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Planowany interwał: 15 minut",
            color = CryptoTextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Rzeczywisty czas sprawdzania zależy od systemu Android.",
            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        // Informacja o braku zgody na powiadomienia
        if (!notificationsEnabled) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Włącz powiadomienia w ustawieniach telefonu, aby otrzymywać alerty cenowe.",
                color = CryptoNegative,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Formatowanie daty ostatniego sprawdzenia
private fun formatCheckTime(time: Long): String {

    if (time == 0L) {
        return "Jeszcze nie sprawdzano"
    }

    val formatter = SimpleDateFormat(
        "dd.MM.yyyy, HH:mm:ss",
        Locale.getDefault()
    )

    return formatter.format(Date(time))
}
