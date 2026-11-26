
package com.example.cryptobotmonitor.presentation.alerts

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.ui.components.CryptoButton
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.components.CryptoOutlinedButton
import com.example.cryptobotmonitor.ui.components.CryptoTextField
import com.example.cryptobotmonitor.ui.theme.*

@Composable
fun CreateAlertScreen(
    onBackClick: () -> Unit,
    onAlertSaved: () -> Unit,
    initialCoinId: String = "",
    initialCoinName: String = "",
    viewModel: AlertsViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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

    // Powrót po poprawnym zapisaniu alertu
    LaunchedEffect(state.message) {

        if (state.message == "Alert został dodany.") {

            Toast.makeText(
                context,
                "Alert został dodany.",
                Toast.LENGTH_SHORT
            ).show()

            onAlertSaved()
        }
    }

    val price = state.targetPrice
        .trim()
        .replace(",", ".")
        .toDoubleOrNull()

    val validPrice = price != null &&
            price.isFinite() &&
            price > 0 &&
            price <= 1_000_000_000_000.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            )
    ) {

        // Powrót do poprzedniego ekranu
        TextButton(
            onClick = onBackClick,
            contentPadding = PaddingValues(0.dp)
        ) {

            Text(
                text = "‹  Wróć",
                color = CryptoAccent,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nowy alert",
            style = MaterialTheme.typography.headlineLarge,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Otrzymaj powiadomienie o zmianie ceny",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Wybór kryptowaluty
        Text(
            text = "Kryptowaluta",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        CryptoOutlinedButton(
            text = if (state.coinName.isBlank()) {
                "Wybierz kryptowalutę"
            } else {
                state.coinName + "  ›"
            },

            enabled = !state.isSaving,

            onClick = {

                focusManager.clearFocus()

                viewModel.onCoinSearchQueryChange("")

                showCoinDialog = true
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cena docelowa alertu
        CryptoTextField(
            value = state.targetPrice,
            onValueChange = viewModel::onTargetPriceChange,
            label = "Cena docelowa (USD)",
            placeholder = "np. 65000",
            enabled = !state.isSaving,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Warunek wykonania alertu
        Text(
            text = "Warunek alertu",
            style = MaterialTheme.typography.titleSmall,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        CryptoCard {

            Column(
                modifier = Modifier.selectableGroup()
            ) {

                AlertConditionOption(
                    title = "Cena powyżej",
                    description = "Osiągnie lub przekroczy cel",
                    selected = state.condition == "ABOVE",
                    enabled = !state.isSaving,
                    onClick = {
                        viewModel.onConditionChange("ABOVE")
                    }
                )

                HorizontalDivider(color = CryptoBorder)

                AlertConditionOption(
                    title = "Cena poniżej",
                    description = "Spadnie do lub poniżej celu",
                    selected = state.condition == "BELOW",
                    enabled = !state.isSaving,
                    onClick = {
                        viewModel.onConditionChange("BELOW")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Podsumowanie tworzonego alertu
        CryptoCard(showBorder = true) {

            Text(
                text = "Powiadomienie",
                style = MaterialTheme.typography.titleMedium,
                color = CryptoAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (
                    state.coinName.isNotBlank() &&
                    validPrice
                ) {

                    val condition = if (
                        state.condition == "ABOVE"
                    ) {
                        "osiągnie lub przekroczy"
                    } else {
                        "spadnie do lub poniżej"
                    }

                    val target = state.targetPrice.trim()

                    "Otrzymasz powiadomienie, gdy cena ${state.coinName} $condition $target USD podczas sprawdzania cen."

                } else {

                    "Wybierz kryptowalutę, cenę docelową i warunek alertu."
                },

                color = CryptoTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Komunikat o błędzie
        state.error?.let { error ->

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                color = CryptoNegative,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Zapis alertu
        CryptoButton(
            text = "Zapisz alert",
            isLoading = state.isSaving,
            enabled = !state.isSaving,

            onClick = {
                focusManager.clearFocus()
                viewModel.addAlert()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Okno wyboru kryptowaluty
    if (showCoinDialog) {

        CoinPickerDialog(
            state = state,

            onSearchChange =
                viewModel::onCoinSearchQueryChange,

            onRetry =
                viewModel::loadAvailableCoins,

            onCoinSelected = { coin ->

                viewModel.selectCoin(coin)

                showCoinDialog = false
            },

            onDismiss = {
                showCoinDialog = false
            }
        )
    }
}

// Element wyboru warunku alertu
@Composable
private fun AlertConditionOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,

            colors = RadioButtonDefaults.colors(
                selectedColor = CryptoAccent,
                unselectedColor = CryptoTextSecondary
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = CryptoTextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = CryptoTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Okno z wyszukiwarką kryptowalut
@Composable
private fun CoinPickerDialog(
    state: AlertsUiState,
    onSearchChange: (String) -> Unit,
    onRetry: () -> Unit,
    onCoinSelected:
        (com.example.cryptobotmonitor.data.model.CoinModel) -> Unit,
    onDismiss: () -> Unit
) {

    val searchText = state.coinSearchQuery.trim()

    val filteredCoins = state.availableCoins.filter { coin ->

        coin.name.contains(
            searchText,
            ignoreCase = true
        ) ||

                coin.symbol.contains(
                    searchText,
                    ignoreCase = true
                )
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = CryptoSurface,

        title = {

            Text(
                text = "Wybierz kryptowalutę",
                color = CryptoTextPrimary
            )
        },

        text = {

            Column {

                CryptoTextField(
                    value = state.coinSearchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Szukaj kryptowaluty..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {

                    state.isCoinsLoading -> {

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

                    state.coinsError != null -> {

                        Text(
                            text = state.coinsError ?: "",
                            color = CryptoNegative
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        CryptoButton(
                            text = "Spróbuj ponownie",
                            onClick = onRetry
                        )
                    }

                    filteredCoins.isEmpty() -> {

                        Text(
                            text = "Nie znaleziono kryptowaluty.",
                            color = CryptoTextSecondary
                        )
                    }

                    else -> {

                        LazyColumn(
                            modifier = Modifier.heightIn(
                                max = 300.dp
                            )
                        ) {

                            items(
                                items = filteredCoins,
                                key = { it.id }
                            ) { coin ->

                                TextButton(
                                    onClick = {
                                        onCoinSelected(coin)
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {

                                    Column(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    ) {

                                        Text(
                                            text = coin.name,
                                            color = CryptoTextPrimary,
                                            style =
                                                MaterialTheme.typography.bodyLarge
                                        )

                                        Text(
                                            text =
                                                coin.symbol.uppercase(),

                                            color = CryptoTextSecondary,
                                            style =
                                                MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = CryptoBorder
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(onClick = onDismiss) {

                Text(
                    text = "Anuluj",
                    color = CryptoAccent
                )
            }
        }
    )
}
