
package com.example.cryptobotmonitor.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.ui.components.CryptoButton
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.components.CryptoOutlinedButton
import com.example.cryptobotmonitor.ui.theme.*
import com.example.cryptobotmonitor.worker.PriceAlertWorker
import java.util.Locale
import kotlin.math.abs

@Composable
fun AlertsScreen(
    onCreateAlertClick: () -> Unit,
    viewModel: AlertsViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var alertToDelete by remember {
        mutableStateOf<PriceAlert?>(null)
    }

    // Podział alertów na aktywne i wykonane
    val activeAlerts = state.alerts.filter {
        it.active
    }

    val completedAlerts = state.alerts.filter {
        !it.active
    }

    val displayedAlerts = if (state.showCompletedAlerts) {
        completedAlerts
    } else {
        activeAlerts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 24.dp
        ),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Nagłówek ekranu
        item {

            Text(
                text = "Alerty cenowe",
                style = MaterialTheme.typography.headlineLarge,
                color = CryptoTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Zarządzaj powiadomieniami",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary
            )
        }

        // Przejście do formularza alertu
        item {

            CryptoButton(
                text = "+ Dodaj alert",
                onClick = onCreateAlertClick
            )
        }

        // Status monitorowania cen
        item {
            MonitoringStatusCard()
        }

        // Ręczne sprawdzanie alertów
        item {

            CryptoOutlinedButton(
                text = "Sprawdź alerty teraz",
                onClick = {
                    PriceAlertWorker.runOnce(context)
                }
            )
        }

        // Komunikat o błędzie
        state.error?.let { error ->

            item {

                Text(
                    text = error,
                    color = CryptoNegative,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {

            Text(
                text = "Moje alerty",
                style = MaterialTheme.typography.titleLarge,
                color = CryptoTextPrimary
            )
        }

        // Przełączanie listy alertów
        item {

            AlertFilterSelector(
                showCompleted = state.showCompletedAlerts,
                activeCount = activeAlerts.size,
                completedCount = completedAlerts.size,
                onSelectionChange =
                    viewModel::setShowCompletedAlerts
            )
        }

        when {

            state.isLoading -> {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = CryptoAccent
                        )
                    }
                }
            }

            displayedAlerts.isEmpty() -> {

                item {

                    CryptoCard {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = if (
                                    state.showCompletedAlerts
                                ) {
                                    "Brak wykonanych alertów"
                                } else {
                                    "Brak aktywnych alertów"
                                },

                                color = CryptoTextPrimary,
                                style =
                                    MaterialTheme.typography.titleMedium,

                                textAlign = TextAlign.Center
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = when {
                                    state.error != null ->
                                        "Sprawdź połączenie i spróbuj ponownie."

                                    state.showCompletedAlerts ->
                                        "Tutaj pojawią się wykonane alerty."

                                    else ->
                                        "Dodaj alert, aby monitorować cenę kryptowaluty."
                                },

                                color = CryptoTextSecondary,
                                style =
                                    MaterialTheme.typography.bodySmall,

                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            else -> {

                items(
                    items = displayedAlerts.sortedByDescending {
                        it.createdAt
                    },
                    key = { it.id }
                ) { alert ->

                    AlertItem(
                        alert = alert,
                        onDeleteClick = {
                            alertToDelete = alert
                        }
                    )
                }
            }
        }
    }

    // Potwierdzenie usunięcia alertu
    alertToDelete?.let { alert ->

        AlertDialog(
            onDismissRequest = {
                alertToDelete = null
            },

            containerColor = CryptoSurface,

            title = {
                Text("Usunąć alert?")
            },

            text = {
                Text(
                    text = "Czy na pewno chcesz usunąć alert dla ${alert.coinName}?",
                    color = CryptoTextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.deleteAlert(alert.id)
                        alertToDelete = null
                    }
                ) {

                    Text(
                        text = "Usuń",
                        color = CryptoNegative
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        alertToDelete = null
                    }
                ) {

                    Text(
                        text = "Anuluj",
                        color = CryptoAccent
                    )
                }
            }
        )
    }
}

// Wybór aktywnych lub wykonanych alertów
@Composable
private fun AlertFilterSelector(
    showCompleted: Boolean,
    activeCount: Int,
    completedCount: Int,
    onSelectionChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CryptoSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),

        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        listOf(
            false to "Aktywne ($activeCount)",
            true to "Wykonane ($completedCount)"
        ).forEach { (value, label) ->

            val selected = showCompleted == value

            TextButton(
                onClick = {
                    onSelectionChange(value)
                },

                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),

                shape = RoundedCornerShape(8.dp),

                contentPadding = PaddingValues(
                    horizontal = 4.dp
                ),

                colors = ButtonDefaults.textButtonColors(

                    containerColor = if (selected) {
                        CryptoPrimary
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },

                    contentColor = if (selected) {
                        CryptoTextPrimary
                    } else {
                        CryptoTextSecondary
                    }
                )
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// Karta pojedynczego alertu
@Composable
fun AlertItem(
    alert: PriceAlert,
    onDeleteClick: () -> Unit
) {

    val conditionText = if (
        alert.condition == "ABOVE"
    ) {
        "Cena osiągnie lub przekroczy"
    } else {
        "Cena spadnie do lub poniżej"
    }

    CryptoCard {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = alert.coinName,
                    style = MaterialTheme.typography.titleMedium,
                    color = CryptoTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (alert.active) {
                        "● Aktywny"
                    } else {
                        "✓ Wykonany"
                    },

                    color = if (alert.active) {
                        CryptoPositive
                    } else {
                        CryptoTextSecondary
                    },

                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = onDeleteClick
            ) {

                Text(
                    text = "Usuń",
                    color = CryptoNegative,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = CryptoBorder)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = conditionText,
            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatAlertPrice(alert.targetPrice) + " USD",
            color = CryptoTextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Formatowanie ceny alertu
private fun formatAlertPrice(price: Double): String {

    val pattern = when {
        abs(price) >= 1.0 -> "%,.2f"
        abs(price) >= 0.01 -> "%.4f"
        else -> "%.8f"
    }

    return String.format(
        Locale.US,
        pattern,
        price
    )
}
