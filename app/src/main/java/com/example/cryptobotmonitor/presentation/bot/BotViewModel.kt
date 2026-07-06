package com.example.cryptobotmonitor.presentation.bot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.repository.AlertRepository
import com.example.cryptobotmonitor.data.repository.CoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class BotMessage(
    val text: String,
    val isUser: Boolean
)

data class BotUiState(
    val command: String = "",
    val messages: List<BotMessage> = listOf(
        BotMessage(
            text = "Cześć! Wpisz pomoc, aby zobaczyć dostępne komendy.",
            isUser = false
        )
    ),
    val isLoading: Boolean = false
)

class BotViewModel : ViewModel() {

    private val coinRepository = CoinRepository()
    private val alertRepository = AlertRepository()

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState

    fun onCommandChange(value: String) {
        _uiState.update {
            it.copy(command = value)
        }
    }

    fun sendCommand() {
        val command = _uiState.value.command.trim()

        if (command.isBlank()) {
            return
        }

        // Dodanie wiadomości użytkownika
        addMessage(command, true)

        _uiState.update {
            it.copy(
                command = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            val answer = handleCommand(command)

            // Dodanie odpowiedzi bota
            addMessage(answer, false)

            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private suspend fun handleCommand(command: String): String {
        val cleanCommand = command.trim().lowercase()
        val parts = cleanCommand.split(Regex("\\s+"))

        return when {
            cleanCommand == "pomoc" -> {
                getHelpText()
            }

            cleanCommand == "moje alerty" -> {
                showMyAlerts()
            }

            parts.size >= 2 && parts[0] == "cena" -> {
                val coinId = parts[1]
                showCoinPrice(coinId)
            }

            parts.size >= 4 && parts[0] == "alert" -> {
                val coinId = parts[1]
                val conditionText = parts[2]
                val price = parts[3].replace(",", ".").toDoubleOrNull()

                addPriceAlert(
                    coinId = coinId,
                    conditionText = conditionText,
                    price = price
                )
            }

            parts.size >= 3 && parts[0] == "usuń" && parts[1] == "alert" -> {
                val coinId = parts[2]
                deleteAlert(coinId)
            }

            parts.size >= 3 && parts[0] == "usun" && parts[1] == "alert" -> {
                val coinId = parts[2]
                deleteAlert(coinId)
            }

            else -> {
                "Nie rozumiem komendy. Wpisz pomoc, aby zobaczyć przykłady."
            }
        }
    }

    private suspend fun showCoinPrice(coinId: String): String {
        return try {
            // Pobranie ceny z API
            val coins = coinRepository.getCoinsByIds(coinId)
            val coin = coins.firstOrNull()

            if (coin == null) {
                "Nie znaleziono kryptowaluty: $coinId"
            } else {
                val price = String.format(
                    Locale.US,
                    "%.2f",
                    coin.currentPrice
                )

                "Aktualna cena ${coin.name} wynosi $price USD."
            }
        } catch (e: Exception) {
            "Nie udało się pobrać ceny. Sprawdź Internet albo nazwę kryptowaluty."
        }
    }

    private suspend fun addPriceAlert(
        coinId: String,
        conditionText: String,
        price: Double?
    ): String {
        if (price == null || price <= 0) {
            return "Podaj poprawną cenę alertu."
        }

        val condition =
            when (conditionText) {
                "powyżej", "powyzej" -> "ABOVE"
                "poniżej", "ponizej" -> "BELOW"
                else -> return "Warunek musi być: powyżej albo poniżej."
            }

        return try {
            // Pobranie nazwy kryptowaluty z API
            val coins = coinRepository.getCoinsByIds(coinId)
            val coin = coins.firstOrNull()

            val coinName = coin?.name ?: coinId

            // Dodanie alertu do Firestore
            val success = alertRepository.addAlertSuspend(
                coinId = coinId,
                coinName = coinName,
                targetPrice = price,
                condition = condition
            )

            if (success) {
                "Dodano alert dla $coinName na cenę $conditionText $price USD."
            } else {
                "Nie udało się dodać alertu. Sprawdź, czy jesteś zalogowany."
            }
        } catch (e: Exception) {
            "Nie udało się dodać alertu."
        }
    }

    private suspend fun showMyAlerts(): String {
        return try {
            // Pobranie alertów użytkownika
            val alerts = alertRepository.getUserAlertsOnce()

            if (alerts.isEmpty()) {
                return "Nie masz jeszcze żadnych alertów."
            }

            alerts.joinToString(separator = "\n") { alert ->
                val conditionText =
                    if (alert.condition == "ABOVE") {
                        "powyżej"
                    } else {
                        "poniżej"
                    }

                val status =
                    if (alert.active) {
                        "aktywny"
                    } else {
                        "wykonany"
                    }

                "${alert.coinName}: cena $conditionText ${alert.targetPrice} USD, status: $status"
            }
        } catch (e: Exception) {
            "Nie udało się pobrać alertów."
        }
    }

    private suspend fun deleteAlert(coinId: String): String {
        return try {
            // Usunięcie alertu po id kryptowaluty
            val deleted = alertRepository.deleteAlertByCoinId(coinId)

            if (deleted) {
                "Usunięto alerty dla: $coinId"
            } else {
                "Nie znaleziono alertu dla: $coinId"
            }
        } catch (e: Exception) {
            "Nie udało się usunąć alertu."
        }
    }

    private fun getHelpText(): String {
        return """
            Dostępne komendy:
            cena bitcoin
            alert bitcoin powyżej 100000
            alert ethereum poniżej 3000
            moje alerty
            usuń alert bitcoin
        """.trimIndent()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        _uiState.update {
            it.copy(
                messages = it.messages + BotMessage(
                    text = text,
                    isUser = isUser
                )
            )
        }
    }
}