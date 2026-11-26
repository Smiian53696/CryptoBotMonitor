
package com.example.cryptobotmonitor.integration.api

import com.example.cryptobotmonitor.data.api.CoinGeckoApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CoinGeckoApiTest {

    private lateinit var server: MockWebServer

    private lateinit var api: CoinGeckoApi

    private lateinit var client: OkHttpClient

    // Przykładowa odpowiedź API
    private val bitcoinJson = """
        {
            "id": "bitcoin",
            "symbol": "btc",
            "name": "Bitcoin",
            "current_price": 60000.0,
            "image": "https://example.com/bitcoin.png",
            "price_change_percentage_24h": 5.25,
            "market_cap": 1200000000000.0,
            "total_volume": 25000000000.0,
            "high_24h": 62000.0,
            "low_24h": 58000.0
        }
    """.trimIndent()

    private val ethereumJson = """
        {
            "id": "ethereum",
            "symbol": "eth",
            "name": "Ethereum",
            "current_price": 3000.0,
            "image": "https://example.com/ethereum.png",
            "price_change_percentage_24h": -2.5
        }
    """.trimIndent()

    private val chartJson = """
        {
            "prices": [
                [1000.0, 58000.0],
                [2000.0, 59000.0],
                [3000.0, 60000.0]
            ]
        }
    """.trimIndent()

    @Before
    fun setUp() {

        // Uruchomienie lokalnego serwera
        server = MockWebServer()

        server.start()

        // Konfiguracja klienta testowego
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        // Retrofit korzystający z lokalnego serwera
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v3/"))
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(CoinGeckoApi::class.java)
    }

    @After
    fun tearDown() {

        client.dispatcher.cancelAll()

        client.connectionPool.evictAll()

        client.dispatcher.executorService.shutdown()

        server.shutdown()
    }

    // Przygotowanie odpowiedzi JSON
    private fun enqueueJson(
        body: String,
        code: Int = 200
    ) {

        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader(
                    "Content-Type",
                    "application/json"
                )
                .setBody(body)
        )
    }

    // Sprawdzenie błędu HTTP
    private suspend fun expectHttpError(
        expectedCode: Int,
        request: suspend () -> Unit
    ): HttpException {

        try {

            request()

            throw AssertionError(
                "Oczekiwano błędu HTTP $expectedCode"
            )

        } catch (e: HttpException) {

            assertEquals(expectedCode, e.code())

            return e
        }
    }

    // Testy pobierania kryptowalut

    @Test
    fun getCoins_success() = runBlocking {

        enqueueJson("[$bitcoinJson]")

        val result = api.getCoins()

        assertEquals(1, result.size)

        assertEquals("bitcoin", result.first().id)

        assertEquals("Bitcoin", result.first().name)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_mapsBasicFields() = runBlocking {

        enqueueJson("[$bitcoinJson]")

        val coin = api.getCoins().first()

        assertEquals("bitcoin", coin.id)

        assertEquals("btc", coin.symbol)

        assertEquals("Bitcoin", coin.name)

        assertEquals(
            60000.0,
            coin.currentPrice,
            0.001
        )

        assertEquals(
            "https://example.com/bitcoin.png",
            coin.image
        )
    }

    @Test
    fun getCoins_mapsOptionalFields() = runBlocking {

        enqueueJson("[$bitcoinJson]")

        val coin = api.getCoins().first()

        assertEquals(
            5.25,
            checkNotNull(coin.priceChange24h),
            0.001
        )

        assertEquals(
            1200000000000.0,
            checkNotNull(coin.marketCap),
            0.001
        )

        assertEquals(
            25000000000.0,
            checkNotNull(coin.totalVolume),
            0.001
        )

        assertEquals(
            62000.0,
            checkNotNull(coin.high24h),
            0.001
        )

        assertEquals(
            58000.0,
            checkNotNull(coin.low24h),
            0.001
        )
    }

    @Test
    fun getCoins_multipleCoins() = runBlocking {

        enqueueJson(
            "[$bitcoinJson, $ethereumJson]"
        )

        val result = api.getCoins()

        assertEquals(2, result.size)

        assertEquals("bitcoin", result[0].id)

        assertEquals("ethereum", result[1].id)

        assertEquals(
            3000.0,
            result[1].currentPrice,
            0.001
        )
    }

    @Test
    fun getCoins_emptyResponse() = runBlocking {

        enqueueJson("[]")

        val result = api.getCoins()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getCoins_missingOptionalFields() = runBlocking {

        enqueueJson("[$ethereumJson]")

        val coin = api.getCoins().first()

        assertEquals("ethereum", coin.id)

        assertNull(coin.marketCap)

        assertNull(coin.totalVolume)

        assertNull(coin.high24h)

        assertNull(coin.low24h)
    }

    @Test
    fun getCoins_nullOptionalFields() = runBlocking {

        enqueueJson(
            """
            [
                {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 60000.0,
                    "image": "",
                    "price_change_percentage_24h": null,
                    "market_cap": null,
                    "total_volume": null,
                    "high_24h": null,
                    "low_24h": null
                }
            ]
            """.trimIndent()
        )

        val coin = api.getCoins().first()

        assertNull(coin.priceChange24h)

        assertNull(coin.marketCap)

        assertNull(coin.totalVolume)

        assertNull(coin.high24h)

        assertNull(coin.low24h)
    }

    // Testy parametrów zapytań HTTP

    @Test
    fun getCoins_correctEndpoint() = runBlocking {

        enqueueJson("[]")

        api.getCoins()

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        assertEquals("GET", request.method)

        assertEquals(
            "/api/v3/coins/markets",
            request.requestUrl?.encodedPath
        )
    }

    @Test
    fun getCoins_defaultParameters() = runBlocking {

        enqueueJson("[]")

        api.getCoins()

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        val url = checkNotNull(request.requestUrl)

        assertEquals(
            "usd",
            url.queryParameter("vs_currency")
        )

        assertEquals(
            "market_cap_desc",
            url.queryParameter("order")
        )

        assertEquals(
            "20",
            url.queryParameter("per_page")
        )

        assertEquals(
            "1",
            url.queryParameter("page")
        )

        assertNull(url.queryParameter("ids"))
    }

    @Test
    fun getCoins_idsParameter() = runBlocking {

        enqueueJson(
            "[$bitcoinJson, $ethereumJson]"
        )

        api.getCoins(
            ids = "bitcoin,ethereum",
            perPage = 50
        )

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        val url = checkNotNull(request.requestUrl)

        assertEquals(
            "bitcoin,ethereum",
            url.queryParameter("ids")
        )

        assertEquals(
            "50",
            url.queryParameter("per_page")
        )
    }

    @Test
    fun getCoins_customParameters() = runBlocking {

        enqueueJson("[]")

        api.getCoins(
            currency = "eur",
            order = "market_cap_asc",
            perPage = 10,
            page = 2
        )

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        val url = checkNotNull(request.requestUrl)

        assertEquals(
            "eur",
            url.queryParameter("vs_currency")
        )

        assertEquals(
            "market_cap_asc",
            url.queryParameter("order")
        )

        assertEquals(
            "10",
            url.queryParameter("per_page")
        )

        assertEquals(
            "2",
            url.queryParameter("page")
        )
    }

    // Testy danych historycznych

    @Test
    fun getMarketChart_mapsPrices() = runBlocking {

        enqueueJson(chartJson)

        val result = api.getMarketChart(
            coinId = "bitcoin"
        )

        assertEquals(3, result.prices.size)

        assertEquals(
            listOf(1000.0, 58000.0),
            result.prices[0]
        )

        assertEquals(
            listOf(3000.0, 60000.0),
            result.prices[2]
        )
    }

    @Test
    fun getMarketChart_defaultParameters() = runBlocking {

        enqueueJson(chartJson)

        api.getMarketChart(
            coinId = "bitcoin"
        )

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        val url = checkNotNull(request.requestUrl)

        assertEquals("GET", request.method)

        assertEquals(
            "/api/v3/coins/bitcoin/market_chart",
            url.encodedPath
        )

        assertEquals(
            "usd",
            url.queryParameter("vs_currency")
        )

        assertEquals(
            "7",
            url.queryParameter("days")
        )
    }

    @Test
    fun getMarketChart_supportedPeriods() = runBlocking {

        val periods = listOf(
            "1",
            "7",
            "30",
            "90"
        )

        periods.forEach { period ->

            enqueueJson(chartJson)

            api.getMarketChart(
                coinId = "ethereum",
                days = period
            )

            val request = checkNotNull(
                server.takeRequest(2, TimeUnit.SECONDS)
            )

            val url = checkNotNull(request.requestUrl)

            assertEquals(
                "/api/v3/coins/ethereum/market_chart",
                url.encodedPath
            )

            assertEquals(
                period,
                url.queryParameter("days")
            )
        }

        assertEquals(4, server.requestCount)
    }

    @Test
    fun getMarketChart_emptyPrices() = runBlocking {

        enqueueJson(
            """
            {
                "prices": []
            }
            """.trimIndent()
        )

        val result = api.getMarketChart(
            coinId = "bitcoin"
        )

        assertTrue(result.prices.isEmpty())
    }

    // Testy błędów HTTP

    @Test
    fun getMarketChart_http404() = runBlocking {

        enqueueJson(
            body = """{"error":"Coin not found"}""",
            code = 404
        )

        expectHttpError(404) {

            api.getMarketChart(
                coinId = "unknown-coin"
            )
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_http429_retryAfter() = runBlocking {

        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "60")
                .setBody(
                    """{"error":"Rate limit exceeded"}"""
                )
        )

        val error = expectHttpError(429) {

            api.getCoins()
        }

        assertEquals(
            "60",
            error.response()
                ?.headers()
                ?.get("Retry-After")
        )

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_http500() = runBlocking {

        enqueueJson(
            body = """{"error":"Internal server error"}""",
            code = 500
        )

        expectHttpError(500) {

            api.getCoins()
        }

        assertEquals(1, server.requestCount)
    }

    // Test niepoprawnej odpowiedzi JSON

    @Test
    fun getCoins_invalidJson() = runBlocking {

        // API powinno zwrócić listę, a nie pojedynczy obiekt
        enqueueJson(
            """{"unexpected":"data"}"""
        )

        try {

            api.getCoins()

            fail("Oczekiwano błędu konwersji JSON")

        } catch (e: IllegalStateException) {

            assertNotNull(e.message)
        }

        assertEquals(1, server.requestCount)
    }

    // Test błędu połączenia

    @Test
    fun getCoins_networkError() = runBlocking {

        server.enqueue(
            MockResponse()
                .setSocketPolicy(
                    SocketPolicy.DISCONNECT_AT_START
                )
        )

        try {

            api.getCoins()

            fail("Oczekiwano błędu połączenia")

        } catch (e: IOException) {

            assertNotNull(e)
        }
    }
}
