
package com.example.cryptobotmonitor.presentation.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.data.preferences.MonitoringPreferences
import com.example.cryptobotmonitor.data.preferences.MonitoringStatus
import com.example.cryptobotmonitor.notification.NotificationHelper
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
        mutableStateOf(false)
    }

    // Aktualizacja informacji o monitorowaniu
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "Monitorowanie cen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Status powiadomień
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text("Powiadomienia")

                Text(
                    text = if (notificationsEnabled) {
                        "Włączone"
                    } else {
                        "Wyłączone"
                    },
                    color = if (notificationsEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            // Czas ostatniego sprawdzenia
            Text(
                text = "Ostatnie udane sprawdzenie:",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = formatCheckTime(
                    monitoringStatus.lastCheckTime
                ),
                fontWeight = FontWeight.Medium
            )

            // Liczba wykonanych alertów
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Wykonane alerty:",
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = monitoringStatus
                        .completedAlerts.toString(),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Podczas ostatniego sprawdzenia",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text(
                text = "Planowany interwał: 15 minut",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Rzeczywisty czas sprawdzania zależy od systemu Android.",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Informacja o wyłączonych powiadomieniach
            if (!notificationsEnabled) {

                Text(
                    text = "Włącz powiadomienia w ustawieniach telefonu, aby otrzymywać alerty cenowe.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
