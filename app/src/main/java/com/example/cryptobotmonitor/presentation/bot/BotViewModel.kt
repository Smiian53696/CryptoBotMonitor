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
import com.example.cryptobotmonitor.data.model.CoinModel
import kotlinx.coroutines.CancellationException
import com.example.cryptobotmonitor.data.repository.FavoriteRepository
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

    private val favoriteRepository = FavoriteRepository()

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState


    // Wyświetlanie ulubionych kryptowalut
    private suspend fun showMyFavorites(): String {

        return try {

            val favorites = favoriteRepository.getUserFavorites()

            if (favorites.isEmpty()) {
                return "Nie masz jeszcze ulubionych kryptowalut."
            }

            val result = favorites
                .sortedBy { it.coinName }
                .joinToString("\n") { favorite ->
                    "★ ${favorite.coinName}"
                }

            "Twoje ulubione kryptowaluty (${favorites.size}):\n\n$result"

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Nie udało się pobrać ulubionych. ${e.message ?: ""}"
        }
    }

    fun onCommandChange(value: String) {
        _uiState.update {
            it.copy(command = value)
        }
    }


    // Wysyłanie komendy do bota
    fun sendCommand() {

        val command = _uiState.value.command.trim()

        // Blokada pustych i kolejnych komend
        if (command.isBlank() || _uiState.value.isLoading) {
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
            try {

                val answer = handleCommand(command)

                addMessage(answer, false)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                addMessage(
                    "Wystąpił błąd podczas przetwarzania komendy.",
                    false
                )

            } finally {

                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    // Wysyłanie szybkiej komendy
    fun sendQuickCommand(command: String) {

        if (_uiState.value.isLoading) return

        onCommandChange(command)
        sendCommand()
    }



    private suspend fun handleCommand(command: String): String {

        val cleanCommand = command
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

        val parts = cleanCommand.split(" ")

        return when {

            // Ulubione kryptowaluty
            cleanCommand == "moje ulubione" -> {
                showMyFavorites()
            }

            // Aktywne alerty
            cleanCommand == "aktywne alerty" -> {
                showMyAlerts(true)
            }

            // Wykonane alerty
            cleanCommand == "wykonane alerty" -> {
                showMyAlerts(false)
            }

            cleanCommand == "pomoc" -> {
                getHelpText()
            }

            cleanCommand == "moje alerty" -> {
                showMyAlerts()
            }

            // Największe wzrosty cen
            cleanCommand == "top wzrosty" -> {
                showTopMovers(true)
            }

            // Największe spadki cen
            cleanCommand == "top spadki" -> {
                showTopMovers(false)
            }

            // Szczegółowe informacje o kryptowalucie
            parts[0] == "info" -> {

                if (parts.size < 2) {
                    return "Poprawna komenda: info bitcoin"
                }

                val query = parts.drop(1).joinToString(" ")

                showCoinInfo(query)
            }

            parts[0] == "cena" -> {

                if (parts.size != 2) {
                    return "Poprawna komenda: cena bitcoin"
                }

                showCoinPrice(parts[1])
            }

            parts[0] == "alert" -> {

                if (parts.size != 4) {
                    return "Poprawna komenda: alert bitcoin powyżej 100000"
                }

                val coinId = parts[1]
                val conditionText = parts[2]

                val price = parts[3]
                    .replace(",", ".")
                    .toDoubleOrNull()

                addPriceAlert(
                    coinId = coinId,
                    conditionText = conditionText,
                    price = price
                )
            }

            parts[0] == "usuń" || parts[0] == "usun" -> {

                if (parts.size != 3 || parts[1] != "alert") {
                    return "Poprawna komenda: usuń alert bitcoin"
                }

                try {
                    // Szukanie kryptowaluty po nazwie lub symbolu
                    val coin = findCoin(parts[2])
                        ?: return "Nie znaleziono kryptowaluty: ${parts[2]}"

                    deleteAlert(coin.id)

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "Nie udało się usunąć alertu. ${e.message ?: ""}"
                }
            }

            else -> {
                "Nie rozumiem komendy. Wpisz pomoc, aby zobaczyć przykłady."
            }
        }
    }

    // Wyświetlanie największych wzrostów i spadków
    private suspend fun showTopMovers(
        showGainers: Boolean
    ): String {

        return try {

            val coins = coinRepository.getCoins()

            // Filtrowanie kryptowalut według zmiany ceny
            val filteredCoins = coins.filter { coin ->

                val change = coin.priceChange24h

                change != null &&
                        change.isFinite() &&
                        if (showGainers) {
                            change > 0
                        } else {
                            change < 0
                        }
            }

            // Sortowanie kryptowalut
            val sortedCoins = if (showGainers) {
                filteredCoins.sortedByDescending {
                    it.priceChange24h
                }
            } else {
                filteredCoins.sortedBy {
                    it.priceChange24h
                }
            }

            val topCoins = sortedCoins.take(5)

            if (topCoins.isEmpty()) {
                return "Brak kryptowalut spełniających warunki."
            }

            val title = if (showGainers) {
                "Największe wzrosty 24h:"
            } else {
                "Największe spadki 24h:"
            }

            val result = topCoins.mapIndexed { index, coin ->

                "${index + 1}. ${coin.name} " +
                        "(${coin.symbol.uppercase()}): " +
                        formatBotChange(coin.priceChange24h)

            }.joinToString("\n")

            "$title\n\n$result\n\n" +
                    "Wyniki na podstawie pobranej listy kryptowalut."

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Nie udało się pobrać danych. ${e.message ?: ""}"
        }
    }

    // Wyszukiwanie kryptowaluty po ID, nazwie lub symbolu
    private suspend fun findCoin(query: String): CoinModel? {

        val searchText = query.trim().lowercase()

        if (searchText.isBlank()) return null

        // Pobranie listy kryptowalut
        val coins = coinRepository.getCoins()

        // Szukanie po ID
        coins.firstOrNull {
            it.id.equals(searchText, ignoreCase = true)
        }?.let {
            return it
        }

        // Szukanie po nazwie
        coins.firstOrNull {
            it.name.equals(searchText, ignoreCase = true)
        }?.let {
            return it
        }

        // Szukanie po symbolu
        val matchingSymbols = coins.filter {
            it.symbol.equals(searchText, ignoreCase = true)
        }

        if (matchingSymbols.size == 1) {
            return matchingSymbols.first()
        }

        if (matchingSymbols.size > 1) {
            throw IllegalArgumentException(
                "Znaleziono kilka kryptowalut z tym symbolem. Podaj ID."
            )
        }

        // Szukanie poza aktualną listą po ID
        return coinRepository
            .getCoinsByIds(searchText)
            .firstOrNull {
                it.id.equals(searchText, ignoreCase = true)
            }
    }


    // Wyświetlanie szczegółowych informacji o kryptowalucie
    private suspend fun showCoinInfo(query: String): String {

        return try {

            val coin = findCoin(query)
                ?: return "Nie znaleziono kryptowaluty: $query"

            """
            ${coin.name} (${coin.symbol.uppercase()})

            Aktualna cena: ${formatBotPrice(coin.currentPrice)} USD
            Zmiana 24h: ${formatBotChange(coin.priceChange24h)}
            Najwyższa cena 24h: ${formatOptionalPrice(coin.high24h)}
            Najniższa cena 24h: ${formatOptionalPrice(coin.low24h)}
            Kapitalizacja: ${formatOptionalPrice(coin.marketCap)}
            Wolumen 24h: ${formatOptionalPrice(coin.totalVolume)}
            """.trimIndent()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Nie udało się pobrać informacji. ${e.message ?: ""}"
        }
    }

    // Formatowanie zmiany procentowej
    private fun formatBotChange(change: Double?): String {

        if (change == null || !change.isFinite()) {
            return "Brak danych"
        }

        return String.format(
            Locale.US,
            "%+.2f%%",
            change
        )
    }

    // Formatowanie ceny opcjonalnej
    private fun formatOptionalPrice(price: Double?): String {

        if (price == null || !price.isFinite()) {
            return "Brak danych"
        }

        return "${formatBotPrice(price)} USD"
    }

    // Wyświetlanie aktualnej ceny kryptowaluty
    private suspend fun showCoinPrice(coinId: String): String {

        return try {

            val coin = findCoin(coinId)
                ?: return "Nie znaleziono kryptowaluty: $coinId"

            val price = formatBotPrice(coin.currentPrice)

            "Aktualna cena ${coin.name} " +
                    "(${coin.symbol.uppercase()}) wynosi $price USD."

        } catch (e: CancellationException) {
            throw e

        } catch (e: retrofit2.HttpException) {

            if (e.code() == 429) {
                "Przekroczono limit API. Spróbuj ponownie później."
            } else {
                "Nie udało się pobrać ceny kryptowaluty."
            }

        } catch (e: IllegalArgumentException) {
            e.message ?: "Niepoprawna nazwa kryptowaluty."

        } catch (e: Exception) {
            "Nie udało się pobrać ceny. ${e.message ?: ""}"
        }
    }



    private suspend fun addPriceAlert(
        coinId: String,
        conditionText: String,
        price: Double?
    ): String {

        // Sprawdzenie poprawności ceny
        if (price == null || !price.isFinite() || price <= 0) {
            return "Podaj poprawną cenę większą od zera."
        }

        // Sprawdzenie warunku alertu
        val condition = when (conditionText) {
            "powyżej", "powyzej" -> "ABOVE"
            "poniżej", "ponizej" -> "BELOW"
            else -> return "Warunek musi być: powyżej albo poniżej."
        }

        return try {

            // Sprawdzenie czy kryptowaluta istnieje
            val coin = findCoin(coinId)
                ?: return "Nie znaleziono kryptowaluty: $coinId"

            // Dodanie alertu do Firestore
            val success = alertRepository.addAlertSuspend(
                coinId = coin.id,
                coinName = coin.name,
                targetPrice = price,
                condition = condition
            )

            if (success) {
                "Dodano alert dla ${coin.name} na cenę $conditionText $price USD."
            } else {
                "Nie udało się dodać alertu. Sprawdź, czy jesteś zalogowany."
            }

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                "Przekroczono limit API. Spróbuj ponownie później."
            } else {
                "Nie udało się pobrać danych kryptowaluty."
            }
        } catch (e: Exception) {
            "Nie udało się dodać alertu. ${e.message ?: ""}"
        }
    }


    private suspend fun showMyAlerts(
        activeOnly: Boolean? = null
    ): String {
        return try {
            // Pobranie alertów użytkownika
            val alerts = alertRepository.getUserAlertsOnce()
            // Filtrowanie alertów według statusu
            val filteredAlerts = when (activeOnly) {
                true -> alerts.filter { it.active }
                false -> alerts.filter { !it.active }
                null -> alerts
            }
            if (filteredAlerts.isEmpty()) {
                return when (activeOnly) {
                    true -> "Nie masz aktywnych alertów."
                    false -> "Nie masz wykonanych alertów."
                    null -> "Nie masz jeszcze żadnych alertów."
                }
            }

            filteredAlerts.joinToString(separator = "\n") { alert ->
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


    // Lista dostępnych komend bota
    private fun getHelpText(): String {
        return """
            Dostępne komendy:

            INFORMACJE O KRYPTOWALUTACH

            cena btc
            - aktualna cena kryptowaluty

            info bitcoin
            - szczegółowe informacje

            top wzrosty
            - największe wzrosty cen w ciągu 24h

            top spadki
            - największe spadki cen w ciągu 24h

            ULUBIONE

            moje ulubione
            - lista ulubionych kryptowalut

            ALERTY CENOWE

            alert btc powyżej 70000
            - utworzenie alertu

            alert eth poniżej 1500
            - utworzenie alertu

            moje alerty
            - wszystkie alerty

            aktywne alerty
            - tylko aktywne alerty

            wykonane alerty
            - tylko wykonane alerty

            usuń alert btc
            - usunięcie alertów dla Bitcoin

            POMOC

            pomoc
            - lista dostępnych komend

            Możesz używać ID, nazw oraz symboli kryptowalut dostępnych na liście.
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

    // Formatowanie ceny kryptowaluty
    private fun formatBotPrice(price: Double): String {

        val pattern = when {
            price >= 1 -> "%,.2f"
            price >= 0.01 -> "%.4f"
            else -> "%.8f"
        }

        return String.format(
            Locale.US,
            pattern,
            price
        )
    }

}