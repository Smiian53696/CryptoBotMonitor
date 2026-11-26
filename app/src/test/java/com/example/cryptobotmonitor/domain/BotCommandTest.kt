
package com.example.cryptobotmonitor.domain

import org.junit.Assert.*
import org.junit.Test

class BotCommandTest {

    // Testy normalizacji komend

    @Test
    fun normalize_uppercase() {
        assertEquals(
            "cena btc",
            BotCommandParser.normalize("CENA BTC")
        )
    }

    @Test
    fun normalize_spaces() {
        assertEquals(
            "cena btc",
            BotCommandParser.normalize("  cena   btc  ")
        )
    }

    @Test
    fun normalize_tabs() {
        assertEquals(
            "cena btc",
            BotCommandParser.normalize("cena\tbtc")
        )
    }

    @Test
    fun normalize_emptyText() {
        assertEquals(
            "",
            BotCommandParser.normalize("   ")
        )
    }

    // Testy podstawowych komend

    @Test
    fun parse_help() {
        assertEquals(
            BotCommandType.HELP,
            BotCommandParser.parse("pomoc").type
        )
    }

    @Test
    fun parse_favorites() {
        assertEquals(
            BotCommandType.FAVORITES,
            BotCommandParser.parse("moje ulubione").type
        )
    }

    @Test
    fun parse_allAlerts() {
        assertEquals(
            BotCommandType.ALL_ALERTS,
            BotCommandParser.parse("moje alerty").type
        )
    }

    @Test
    fun parse_activeAlerts() {
        assertEquals(
            BotCommandType.ACTIVE_ALERTS,
            BotCommandParser.parse("aktywne alerty").type
        )
    }

    @Test
    fun parse_completedAlerts() {
        assertEquals(
            BotCommandType.COMPLETED_ALERTS,
            BotCommandParser.parse("wykonane alerty").type
        )
    }

    @Test
    fun parse_topGainers() {
        assertEquals(
            BotCommandType.TOP_GAINERS,
            BotCommandParser.parse("top wzrosty").type
        )
    }

    @Test
    fun parse_topLosers() {
        assertEquals(
            BotCommandType.TOP_LOSERS,
            BotCommandParser.parse("top spadki").type
        )
    }

    // Testy pobierania ceny

    @Test
    fun price_bitcoinSymbol() {

        val result = BotCommandParser.parse("cena btc")

        assertEquals(BotCommandType.PRICE, result.type)
        assertEquals("btc", result.coinQuery)
    }

    @Test
    fun price_coinName() {

        val result = BotCommandParser.parse("cena ethereum")

        assertEquals(BotCommandType.PRICE, result.type)
        assertEquals("ethereum", result.coinQuery)
    }

    @Test
    fun price_uppercase() {

        val result = BotCommandParser.parse("CENA BTC")

        assertEquals(BotCommandType.PRICE, result.type)
        assertEquals("btc", result.coinQuery)
    }

    @Test
    fun price_missingArgument() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse("cena").type
        )
    }

    @Test
    fun price_tooManyArguments() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse("cena btc eth").type
        )
    }

    // Testy informacji o kryptowalucie

    @Test
    fun info_correctCommand() {

        val result = BotCommandParser.parse("info bitcoin")

        assertEquals(BotCommandType.INFO, result.type)
        assertEquals("bitcoin", result.coinQuery)
    }

    @Test
    fun info_multipleWords() {

        val result = BotCommandParser.parse(
            "info bitcoin cash"
        )

        assertEquals(BotCommandType.INFO, result.type)
        assertEquals("bitcoin cash", result.coinQuery)
    }

    @Test
    fun info_uppercaseAndSpaces() {

        val result = BotCommandParser.parse(
            " INFO   BITCOIN CASH "
        )

        assertEquals(BotCommandType.INFO, result.type)
        assertEquals("bitcoin cash", result.coinQuery)
    }

    @Test
    fun info_missingArgument() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse("info").type
        )
    }

    // Testy tworzenia alertów

    @Test
    fun alert_above() {

        val result = BotCommandParser.parse(
            "alert btc powyżej 70000"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals("btc", result.coinQuery)
        assertEquals("ABOVE", result.condition)
        assertEquals(70000.0, result.price!!, 0.000001)
    }

    @Test
    fun alert_below() {

        val result = BotCommandParser.parse(
            "alert eth poniżej 1500"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals("eth", result.coinQuery)
        assertEquals("BELOW", result.condition)
        assertEquals(1500.0, result.price!!, 0.000001)
    }

    @Test
    fun alert_aboveWithoutPolishCharacters() {

        val result = BotCommandParser.parse(
            "alert btc powyzej 70000"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals("ABOVE", result.condition)
    }

    @Test
    fun alert_belowWithoutPolishCharacters() {

        val result = BotCommandParser.parse(
            "alert eth ponizej 1500"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals("BELOW", result.condition)
    }

    @Test
    fun alert_decimalComma() {

        val result = BotCommandParser.parse(
            "alert sol powyżej 150,50"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals(150.50, result.price!!, 0.000001)
    }

    @Test
    fun alert_decimalDot() {

        val result = BotCommandParser.parse(
            "alert sol powyżej 150.50"
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals(150.50, result.price!!, 0.000001)
    }

    @Test
    fun alert_uppercaseAndSpaces() {

        val result = BotCommandParser.parse(
            " ALERT   BTC  POWYŻEJ  70000 "
        )

        assertEquals(BotCommandType.CREATE_ALERT, result.type)
        assertEquals("btc", result.coinQuery)
        assertEquals("ABOVE", result.condition)
    }

    @Test
    fun alert_zeroPrice() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej 0"
            ).type
        )
    }

    @Test
    fun alert_negativePrice() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej -100"
            ).type
        )
    }

    @Test
    fun alert_invalidPrice() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej abc"
            ).type
        )
    }

    @Test
    fun alert_aboveMaximumPrice() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej 1000000000001"
            ).type
        )
    }

    @Test
    fun alert_invalidCondition() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc rowno 70000"
            ).type
        )
    }

    @Test
    fun alert_missingPrice() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej"
            ).type
        )
    }

    @Test
    fun alert_tooManyArguments() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse(
                "alert btc powyżej 70000 usd"
            ).type
        )
    }

    // Testy usuwania alertów

    @Test
    fun delete_correctCommand() {

        val result = BotCommandParser.parse(
            "usuń alert btc"
        )

        assertEquals(BotCommandType.DELETE_ALERT, result.type)
        assertEquals("btc", result.coinQuery)
    }

    @Test
    fun delete_withoutPolishCharacters() {

        val result = BotCommandParser.parse(
            "usun alert eth"
        )

        assertEquals(BotCommandType.DELETE_ALERT, result.type)
        assertEquals("eth", result.coinQuery)
    }

    @Test
    fun delete_missingCoin() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse("usuń alert").type
        )
    }

    @Test
    fun delete_invalidArgument() {
        assertEquals(
            BotCommandType.INVALID,
            BotCommandParser.parse("usuń bitcoin btc").type
        )
    }

    // Testy nieznanych komend

    @Test
    fun parse_unknownCommand() {

        val result = BotCommandParser.parse("hello")

        assertEquals(BotCommandType.INVALID, result.type)
        assertNotNull(result.error)
    }

    @Test
    fun parse_emptyCommand() {

        val result = BotCommandParser.parse("")

        assertEquals(BotCommandType.INVALID, result.type)
        assertNotNull(result.error)
    }
}
