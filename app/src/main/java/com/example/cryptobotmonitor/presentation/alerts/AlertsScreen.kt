
package com.example.cryptobotmonitor.presentation.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.worker.PriceAlertWorker

@Composable
fun AlertsScreen(
    onBackClick: () -> Unit,
    initialCoinId: String = "",
    initialCoinName: String = "",
    viewModel: AlertsViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCoinDialog by remember {
        mutableStateOf(false)
    }

    // Dane przekazane z ekranu szczegółów
    LaunchedEffect(initialCoinId, initialCoinName) {
        if (initialCoinId.isNotBlank()) {
            viewModel.setInitialCoinData(
                coinId = initialCoinId,
                coinName = initialCoinName
            )
        }
    }

    // Podział alertów na aktywne i wykonane
    val activeAlerts = state.alerts.filter { it.active }
    val completedAlerts = state.alerts.filter { !it.active }

    val displayedAlerts = if (state.showCompletedAlerts) {
        completedAlerts
    } else {
        activeAlerts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alerty cenowe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(onClick = onBackClick) {
                    Text("Wróć")
                }
            }
        }

        item {
            Text(
                text = "Dodaj nowy alert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Wybór kryptowaluty
        item {
            OutlinedButton(
                onClick = {
                    viewModel.onCoinSearchQueryChange("")
                    showCoinDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(
                    text = if (state.coinName.isBlank()) {
                        "Wybierz kryptowalutę"
                    } else {
                        state.coinName
                    }
                )
            }
        }

        // Pole ceny
        item {
            OutlinedTextField(
                value = state.targetPrice,
                onValueChange = viewModel::onTargetPriceChange,
                label = {
                    Text("Cena alertu (USD)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )
        }

        item {
            Text(
                text = "Warunek alertu",
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.condition == "ABOVE",
                    onClick = {
                        viewModel.onConditionChange("ABOVE")
                    },
                    enabled = !state.isSaving
                )

                Text("Cena powyżej")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.condition == "BELOW",
                    onClick = {
                        viewModel.onConditionChange("BELOW")
                    },
                    enabled = !state.isSaving
                )

                Text("Cena poniżej")
            }
        }

        // Dodawanie alertu
        item {
            Button(
                onClick = {
                    viewModel.addAlert()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {

                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Dodaj alert")
                }
            }
        }
        // Informacje o monitorowaniu cen
        item {
            MonitoringStatusCard()
        }
        // Ręczne sprawdzanie alertów
        item {
            OutlinedButton(
                onClick = {
                    PriceAlertWorker.runOnce(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sprawdź alerty teraz")
            }
        }

        // Komunikaty o błędach
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Komunikaty o wykonaniu operacji
        state.message?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Moje alerty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Filtrowanie alertów
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                FilterChip(
                    selected = !state.showCompletedAlerts,
                    onClick = {
                        viewModel.setShowCompletedAlerts(false)
                    },
                    label = {
                        Text("Aktywne (${activeAlerts.size})")
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = state.showCompletedAlerts,
                    onClick = {
                        viewModel.setShowCompletedAlerts(true)
                    },
                    label = {
                        Text("Wykonane (${completedAlerts.size})")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when {

            state.isLoading -> {
                item {
                    CircularProgressIndicator()
                }
            }

            displayedAlerts.isEmpty() -> {
                item {
                    Text(
                        text = if (state.showCompletedAlerts) {
                            "Brak wykonanych alertów."
                        } else {
                            "Brak aktywnych alertów."
                        }
                    )
                }
            }

            else -> {
                items(
                    items = displayedAlerts,
                    key = { it.id }
                ) { alert ->

                    AlertItem(
                        alert = alert,
                        onDeleteClick = {
                            viewModel.deleteAlert(alert.id)
                        }
                    )
                }
            }
        }
    }

    // Okno wyboru kryptowaluty
    if (showCoinDialog) {

        val searchText = state.coinSearchQuery.trim()

        val filteredCoins = state.availableCoins.filter { coin ->
            coin.name.contains(searchText, ignoreCase = true) ||
                    coin.symbol.contains(searchText, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                showCoinDialog = false
            },
            title = {
                Text("Wybierz kryptowalutę")
            },
            text = {
                Column {

                    OutlinedTextField(
                        value = state.coinSearchQuery,
                        onValueChange =
                            viewModel::onCoinSearchQueryChange,
                        label = {
                            Text("Szukaj kryptowaluty")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when {

                        state.isCoinsLoading -> {
                            CircularProgressIndicator()
                        }

                        state.coinsError != null -> {

                            Text(
                                text = state.coinsError ?: "",
                                color = MaterialTheme.colorScheme.error
                            )

                            Button(
                                onClick = {
                                    viewModel.loadAvailableCoins()
                                }
                            ) {
                                Text("Spróbuj ponownie")
                            }
                        }

                        filteredCoins.isEmpty() -> {
                            Text("Nie znaleziono kryptowaluty.")
                        }

                        else -> {

                            LazyColumn(
                                modifier = Modifier.heightIn(
                                    max = 300.dp
                                ),
                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                items(
                                    items = filteredCoins,
                                    key = { it.id }
                                ) { coin ->

                                    TextButton(
                                        onClick = {
                                            viewModel.selectCoin(coin)
                                            showCoinDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${coin.name} (${coin.symbol.uppercase()})",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCoinDialog = false
                    }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun AlertItem(
    alert: PriceAlert,
    onDeleteClick: () -> Unit
) {

    val conditionText = if (alert.condition == "ABOVE") {
        "powyżej"
    } else {
        "poniżej"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = alert.coinName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (alert.active) {
                        "Aktywny"
                    } else {
                        "Wykonany"
                    },
                    color = if (alert.active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cena $conditionText ${alert.targetPrice} USD"
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDeleteClick
            ) {
                Text("Usuń")
            }
        }
    }
}
