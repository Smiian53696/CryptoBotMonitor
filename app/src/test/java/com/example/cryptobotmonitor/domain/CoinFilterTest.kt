
package com.example.cryptobotmonitor.domain

import com.example.cryptobotmonitor.data.model.CoinModel
import org.junit.Assert.*
import org.junit.Test

class CoinFilterTest {

    // Przykładowe kryptowaluty do testów
    private val bitcoin = CoinModel(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        currentPrice = 60000.0,
        image = ""
    )

    private val ethereum = CoinModel(
        id = "ethereum",
        symbol = "eth",
        name = "Ethereum",
        currentPrice = 3000.0,
        image = ""
    )

    private val solana = CoinModel(
        id = "solana",
        symbol = "sol",
        name = "Solana",
        currentPrice = 150.0,
        image = ""
    )

    private val bitcoinCash = CoinModel(
        id = "bitcoin-cash",
        symbol = "bch",
        name = "Bitcoin Cash",
        currentPrice = 500.0,
        image = ""
    )

    private val ripple = CoinModel(
        id = "ripple",
        symbol = "xrp",
        name = "XRP",
        currentPrice = 1.0,
        image = ""
    )

    // Lista wszystkich kryptowalut
    private val coins = listOf(
        bitcoin,
        ethereum,
        solana,
        bitcoinCash,
        ripple
    )

    // Przykładowe ulubione kryptowaluty
    private val favorites = setOf(
        "bitcoin",
        "solana"
    )

    // Testy wyszukiwania

    @Test
    fun search_fullName() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "Ethereum"
        )

        assertEquals(
            listOf(ethereum),
            result
        )
    }

    @Test
    fun search_bySymbol() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "BTC"
        )

        assertEquals(
            listOf(bitcoin),
            result
        )
    }

    @Test
    fun search_lowercase() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "ethereum"
        )

        assertEquals(
            listOf(ethereum),
            result
        )
    }

    @Test
    fun search_uppercase() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "SOLANA"
        )

        assertEquals(
            listOf(solana),
            result
        )
    }

    @Test
    fun search_partialName() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "bit"
        )

        assertEquals(
            listOf(bitcoin, bitcoinCash),
            result
        )
    }

    @Test
    fun search_partialSymbol() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "et"
        )

        assertEquals(
            listOf(ethereum),
            result
        )
    }

    @Test
    fun search_withSpaces() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "  BTC  "
        )

        assertEquals(
            listOf(bitcoin),
            result
        )
    }

    @Test
    fun search_emptyQuery() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            ""
        )

        assertEquals(coins, result)
    }

    @Test
    fun search_blankQuery() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "   "
        )

        assertEquals(coins, result)
    }

    @Test
    fun search_unknownCoin() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            "unknowncoin"
        )

        assertTrue(result.isEmpty())
    }

    // Testy filtrowania ulubionych

    @Test
    fun favorites_onlyFavorites() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            true,
            ""
        )

        assertEquals(
            listOf(bitcoin, solana),
            result
        )
    }

    @Test
    fun favorites_disabledFilter() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            false,
            ""
        )

        assertEquals(coins, result)
    }

    @Test
    fun favorites_emptyFavorites() {

        val result = CoinFilter.filterCoins(
            coins,
            emptySet(),
            true,
            ""
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun favorites_allCoinsAreFavorites() {

        val allFavoriteIds = coins
            .map { it.id }
            .toSet()

        val result = CoinFilter.filterCoins(
            coins,
            allFavoriteIds,
            true,
            ""
        )

        assertEquals(coins, result)
    }

    @Test
    fun favorites_unknownFavoriteId() {

        val result = CoinFilter.filterCoins(
            coins,
            setOf("unknown-coin"),
            true,
            ""
        )

        assertTrue(result.isEmpty())
    }

    // Testy wyszukiwania w ulubionych

    @Test
    fun combined_searchFavoriteCoin() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            true,
            "BTC"
        )

        assertEquals(
            listOf(bitcoin),
            result
        )
    }

    @Test
    fun combined_searchNonFavoriteCoin() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            true,
            "ETH"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun combined_partialSearchInFavorites() {

        val result = CoinFilter.filterCoins(
            coins,
            favorites,
            true,
            "bit"
        )

        assertEquals(
            listOf(bitcoin),
            result
        )
    }

    // Testy przypadków brzegowych

    @Test
    fun filter_emptyCoinList() {

        val result = CoinFilter.filterCoins(
            emptyList(),
            favorites,
            false,
            "BTC"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun filter_preservesOriginalOrder() {

        val reversedCoins = coins.reversed()

        val result = CoinFilter.filterCoins(
            reversedCoins,
            favorites,
            true,
            ""
        )

        assertEquals(
            listOf(solana, bitcoin),
            result
        )
    }
}
