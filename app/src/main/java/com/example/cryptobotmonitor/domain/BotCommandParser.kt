
package com.example.cryptobotmonitor.domain

import com.example.cryptobotmonitor.validation.AlertValidator

// Dostępne rodzaje komend
enum class BotCommandType {
    HELP,
    PRICE,
    INFO,
    TOP_GAINERS,
    TOP_LOSERS,
    FAVORITES,
    ALL_ALERTS,
    ACTIVE_ALERTS,
    COMPLETED_ALERTS,
    CREATE_ALERT,
    DELETE_ALERT,
    INVALID
}

// Dane rozpoznanej komendy
data class BotCommand(
    val type: BotCommandType,
    val coinQuery: String = "",
    val condition: String = "",
    val price: Double? = null,
    val error: String? = null
)

object BotCommandParser {

    // Przygotowanie tekstu komendy
    fun normalize(command: String): String {

        return command
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    // Rozpoznawanie komendy użytkownika
    fun parse(command: String): BotCommand {

        val cleanCommand = normalize(command)
        val parts = cleanCommand.split(" ")

        return when {

            cleanCommand == "pomoc" ->
                BotCommand(BotCommandType.HELP)

            cleanCommand == "moje ulubione" ->
                BotCommand(BotCommandType.FAVORITES)

            cleanCommand == "moje alerty" ->
                BotCommand(BotCommandType.ALL_ALERTS)

            cleanCommand == "aktywne alerty" ->
                BotCommand(BotCommandType.ACTIVE_ALERTS)

            cleanCommand == "wykonane alerty" ->
                BotCommand(BotCommandType.COMPLETED_ALERTS)

            cleanCommand == "top wzrosty" ->
                BotCommand(BotCommandType.TOP_GAINERS)

            cleanCommand == "top spadki" ->
                BotCommand(BotCommandType.TOP_LOSERS)

            // Pobranie aktualnej ceny
            parts[0] == "cena" -> {

                if (parts.size != 2) {
                    invalid("Poprawna komenda: cena bitcoin")
                } else {
                    BotCommand(
                        type = BotCommandType.PRICE,
                        coinQuery = parts[1]
                    )
                }
            }

            // Informacje o kryptowalucie
            parts[0] == "info" -> {

                if (parts.size < 2) {
                    invalid("Poprawna komenda: info bitcoin")
                } else {
                    BotCommand(
                        type = BotCommandType.INFO,
                        coinQuery = parts.drop(1).joinToString(" ")
                    )
                }
            }

            // Tworzenie alertu
            parts[0] == "alert" ->
                parseAlert(parts)

            // Usuwanie alertów
            parts[0] == "usuń" || parts[0] == "usun" -> {

                if (parts.size != 3 || parts[1] != "alert") {

                    invalid(
                        "Poprawna komenda: usuń alert bitcoin"
                    )

                } else {

                    BotCommand(
                        type = BotCommandType.DELETE_ALERT,
                        coinQuery = parts[2]
                    )
                }
            }

            else -> invalid(
                "Nie rozumiem komendy. Wpisz pomoc, aby zobaczyć przykłady."
            )
        }
    }

    // Rozpoznawanie komendy tworzenia alertu
    private fun parseAlert(
        parts: List<String>
    ): BotCommand {

        if (parts.size != 4) {

            return invalid(
                "Poprawna komenda: alert bitcoin powyżej 100000"
            )
        }

        val price = AlertValidator.parsePrice(parts[3])

        // Sprawdzenie poprawności ceny
        if (!AlertValidator.isValidPrice(price)) {

            return invalid(
                "Cena musi być większa od zera i nie może przekraczać 1 biliona USD."
            )
        }

        // Sprawdzenie warunku alertu
        val condition = when (parts[2]) {

            "powyżej", "powyzej" -> "ABOVE"

            "poniżej", "ponizej" -> "BELOW"

            else -> return invalid(
                "Warunek musi być: powyżej albo poniżej."
            )
        }

        return BotCommand(
            type = BotCommandType.CREATE_ALERT,
            coinQuery = parts[1],
            condition = condition,
            price = price
        )
    }

    // Niepoprawna komenda
    private fun invalid(message: String): BotCommand {

        return BotCommand(
            type = BotCommandType.INVALID,
            error = message
        )
    }
}
