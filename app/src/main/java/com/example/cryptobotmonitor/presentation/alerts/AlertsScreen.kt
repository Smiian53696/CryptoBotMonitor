package com.example.cryptobotmonitor.presentation.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.data.model.PriceAlert
import androidx.compose.ui.platform.LocalContext
import com.example.cryptobotmonitor.worker.PriceAlertWorker

@Composable
fun AlertsScreen(
    onBackClick: () -> Unit,
    viewModel: AlertsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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

                OutlinedButton(
                    onClick = onBackClick
                ) {
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

        item {
            OutlinedTextField(
                value = state.coinId,
                onValueChange = viewModel::onCoinIdChange,
                label = {
                    Text("Coin ID, np. bitcoin")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = state.coinName,
                onValueChange = viewModel::onCoinNameChange,
                label = {
                    Text("Nazwa, np. Bitcoin")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = state.targetPrice,
                onValueChange = viewModel::onTargetPriceChange,
                label = {
                    Text("Cena alertu")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
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
                    }
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
                    }
                )

                Text("Cena poniżej")
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.addAlert()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dodaj alert")
            }
        }
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

        if (state.error != null) {
            item {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (state.message != null) {
            item {
                Text(
                    text = state.message ?: "",
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

        if (state.isLoading) {
            item {
                CircularProgressIndicator()
            }
        } else if (state.alerts.isEmpty()) {
            item {
                Text("Nie masz jeszcze żadnych alertów.")
            }
        } else {
            items(state.alerts) { alert ->
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

@Composable
fun AlertItem(
    alert: PriceAlert,
    onDeleteClick: () -> Unit
) {
    val conditionText =
        if (alert.condition == "ABOVE") {
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
            Text(
                text = alert.coinName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "ID: ${alert.coinId}"
            )

            Text(
                text = "Warunek: cena $conditionText ${alert.targetPrice} USD"
            )

            Text(
                text = if (alert.active) {
                    "Status: aktywny"
                } else {
                    "Status: wykonany"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDeleteClick
            ) {
                Text("Usuń")
            }
        }
    }
}

