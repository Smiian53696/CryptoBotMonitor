
package com.example.cryptobotmonitor.presentation.bot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BotScreen(
    onBackClick: () -> Unit,
    viewModel: BotViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Automatyczne przewijanie do ostatniej wiadomości
    LaunchedEffect(state.messages.size, state.isLoading) {

        val lastIndex = state.messages.lastIndex +
                if (state.isLoading) 1 else 0

        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {

        // Nagłówek ekranu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "CryptoBot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Asystent rynku kryptowalut",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = onBackClick
            ) {
                Text("Wróć")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        // Lista wiadomości
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(state.messages) { message ->

                BotMessageItem(
                    message = message
                )
            }

            // Informacja o przetwarzaniu komendy
            if (state.isLoading) {

                item {

                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        Text(
                            text = "Bot przetwarza komendę...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        // Szybkie komendy
        Text(
            text = "Szybkie komendy",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        QuickCommands(
            enabled = !state.isLoading,
            onCommandClick = { command ->

                focusManager.clearFocus()

                viewModel.sendQuickCommand(command)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pole wpisywania komendy
        OutlinedTextField(
            value = state.command,
            onValueChange = viewModel::onCommandChange,
            label = {
                Text("Komenda")
            },
            placeholder = {
                Text("np. cena btc")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,

            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),

            keyboardActions = KeyboardActions(
                onSend = {

                    if (
                        state.command.isNotBlank() &&
                        !state.isLoading
                    ) {

                        focusManager.clearFocus()

                        viewModel.sendCommand()
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Przycisk wysyłania komendy
        Button(
            onClick = {

                focusManager.clearFocus()

                viewModel.sendCommand()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading &&
                    state.command.isNotBlank()
        ) {
            Text("Wyślij")
        }
    }
}

// Lista szybkich komend
@Composable
fun QuickCommands(
    enabled: Boolean,
    onCommandClick: (String) -> Unit
) {

    val commands = listOf(
        "Cena BTC" to "cena btc",
        "Informacje BTC" to "info btc",
        "Ulubione" to "moje ulubione",
        "Wzrosty" to "top wzrosty",
        "Spadki" to "top spadki",
        "Aktywne alerty" to "aktywne alerty",
        "Wykonane alerty" to "wykonane alerty",
        "Pomoc" to "pomoc"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(commands) { (label, command) ->

            AssistChip(
                onClick = {
                    onCommandClick(command)
                },
                enabled = enabled,
                label = {
                    Text(label)
                }
            )
        }
    }
}

// Wyświetlanie wiadomości
@Composable
fun BotMessageItem(
    message: BotMessage
) {

    val alignment = if (message.isUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }

    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {

            Column(
                modifier = Modifier.padding(12.dp)
            ) {

                Text(
                    text = if (message.isUser) {
                        "Ty"
                    } else {
                        "CryptoBot"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    color = textColor
                )
            }
        }
    }
}
