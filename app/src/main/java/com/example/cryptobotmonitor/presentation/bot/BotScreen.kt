
package com.example.cryptobotmonitor.presentation.bot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.ui.components.CryptoTextField
import com.example.cryptobotmonitor.ui.theme.*

@Composable
fun BotScreen(
    viewModel: BotViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val canSend = !state.isLoading &&
            state.command.isNotBlank()

    // Wysyłanie komendy
    val sendAction: () -> Unit = {

        if (canSend) {
            focusManager.clearFocus()
            viewModel.sendCommand()
        }
    }

    // Automatyczne przewijanie wiadomości
    LaunchedEffect(
        state.messages.size,
        state.isLoading
    ) {

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
            .padding(horizontal = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Nagłówek ekranu
        BotHeader()

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = CryptoBorder)

        // Lista wiadomości
        LazyColumn(
            state = listState,

            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),

            contentPadding = PaddingValues(
                vertical = 16.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(state.messages) { message ->

                BotMessageItem(message = message)
            }

            // Informacja o oczekiwaniu na odpowiedź
            if (state.isLoading) {

                item {
                    BotLoadingIndicator()
                }
            }
        }

        HorizontalDivider(color = CryptoBorder)

        Spacer(modifier = Modifier.height(12.dp))

        // Szybkie komendy
        Text(
            text = "Szybkie komendy",
            style = MaterialTheme.typography.labelMedium,
            color = CryptoTextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        QuickCommands(
            enabled = !state.isLoading,

            onCommandClick = { command ->

                focusManager.clearFocus()

                viewModel.sendQuickCommand(command)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pole komendy i przycisk wysyłania
        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            CryptoTextField(
                value = state.command,

                onValueChange =
                    viewModel::onCommandChange,

                placeholder = "Wpisz komendę...",

                modifier = Modifier.weight(1f),

                enabled = !state.isLoading,

                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),

                keyboardActions = KeyboardActions(
                    onSend = {
                        sendAction()
                    }
                )
            )

            BotSendButton(
                enabled = canSend,
                onClick = sendAction
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Nagłówek z ikoną bota
@Composable
private fun BotHeader() {

    Row(
        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically
    ) {

        BotAvatar(size = 48)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "CryptoBot",
                style = MaterialTheme.typography.headlineLarge,
                color = CryptoTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Twój asystent rynku",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary
            )
        }
    }
}

// Ikona bota
@Composable
private fun BotAvatar(
    size: Int = 32
) {

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                color = CryptoPrimary,
                shape = RoundedCornerShape(12.dp)
            ),

        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.size((size * 0.65f).dp)
        ) {

            val w = this.size.width
            val h = this.size.height
            val stroke = 1.8.dp.toPx()

            // Głowa bota
            drawRoundRect(
                color = Color.White,

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

            // Antena
            drawLine(
                color = Color.White,
                start = Offset(w * 0.50f, h * 0.25f),
                end = Offset(w * 0.50f, h * 0.08f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )

            // Oczy
            drawCircle(
                color = Color.White,
                radius = w * 0.06f,
                center = Offset(w * 0.35f, h * 0.48f)
            )

            drawCircle(
                color = Color.White,
                radius = w * 0.06f,
                center = Offset(w * 0.65f, h * 0.48f)
            )

            // Usta
            drawLine(
                color = Color.White,
                start = Offset(w * 0.35f, h * 0.68f),
                end = Offset(w * 0.65f, h * 0.68f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
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
        "Moje alerty" to "moje alerty",
        "Aktywne alerty" to "aktywne alerty",
        "Wykonane alerty" to "wykonane alerty",
        "Pomoc" to "pomoc"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        items(commands) { (label, command) ->

            AssistChip(
                onClick = {
                    onCommandClick(command)
                },

                enabled = enabled,

                shape = RoundedCornerShape(10.dp),

                label = {

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },

                colors = AssistChipDefaults.assistChipColors(
                    containerColor = CryptoSurface,
                    labelColor = CryptoAccent,
                    disabledContainerColor = CryptoSurface,
                    disabledLabelColor =
                        CryptoTextSecondary.copy(alpha = 0.6f)
                )
            )
        }
    }
}

// Wiadomość użytkownika lub bota
@Composable
fun BotMessageItem(
    message: BotMessage
) {

    val isUser = message.isUser

    val backgroundColor = if (isUser) {
        CryptoPrimary
    } else {
        CryptoSurface
    }

    val messageColor = if (isUser) {
        Color.White
    } else {
        CryptoTextPrimary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = if (isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        },

        verticalAlignment = Alignment.Top
    ) {

        if (!isUser) {

            BotAvatar(size = 32)

            Spacer(modifier = Modifier.width(8.dp))
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {

            val bubbleWidth = maxWidth * 0.90f

            Surface(
                modifier = Modifier
                    .align(
                        if (isUser) {
                            Alignment.TopEnd
                        } else {
                            Alignment.TopStart
                        }
                    )
                    .widthIn(max = bubbleWidth),

                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,

                    bottomStart = if (isUser) {
                        16.dp
                    } else {
                        4.dp
                    },

                    bottomEnd = if (isUser) {
                        4.dp
                    } else {
                        16.dp
                    }
                ),

                color = backgroundColor,
                contentColor = messageColor
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = if (isUser) {
                            "Ty"
                        } else {
                            "CryptoBot"
                        },

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.SemiBold,

                        color = if (isUser) {
                            Color(0xFFE0E7FF)
                        } else {
                            CryptoAccent
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Możliwość kopiowania wiadomości
                    SelectionContainer {

                        Text(
                            text = message.text,
                            color = messageColor,

                            style =
                                MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

// Informacja o przetwarzaniu komendy
@Composable
private fun BotLoadingIndicator() {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        BotAvatar(size = 32)

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = CryptoSurface,
            shape = RoundedCornerShape(12.dp)
        ) {

            Row(
                modifier = Modifier.padding(14.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = CryptoAccent,
                    strokeWidth = 2.dp
                )

                Text(
                    text = "Bot przetwarza komendę...",
                    color = CryptoTextSecondary,

                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Przycisk wysyłania wiadomości
@Composable
private fun BotSendButton(
    enabled: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier = Modifier
            .size(56.dp)
            .semantics {
                contentDescription = "Wyślij komendę"
            },

        enabled = enabled,

        shape = RoundedCornerShape(12.dp),

        contentPadding = PaddingValues(0.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = CryptoPrimary,
            contentColor = Color.White,
            disabledContainerColor = CryptoSurface,
            disabledContentColor = CryptoTextSecondary
        )
    ) {

        val iconColor = LocalContentColor.current

        Canvas(
            modifier = Modifier.size(24.dp)
        ) {

            val w = size.width
            val h = size.height

            val path = Path().apply {

                moveTo(w * 0.08f, h * 0.42f)
                lineTo(w * 0.92f, h * 0.08f)
                lineTo(w * 0.58f, h * 0.92f)
                lineTo(w * 0.42f, h * 0.58f)
                close()

                moveTo(w * 0.42f, h * 0.58f)
                lineTo(w * 0.92f, h * 0.08f)
            }

            drawPath(
                path = path,
                color = iconColor,

                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
