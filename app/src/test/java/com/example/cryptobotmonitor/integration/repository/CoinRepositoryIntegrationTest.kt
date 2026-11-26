
package com.example.cryptobotmonitor.integration.repository

import com.example.cryptobotmonitor.data.api.CoinGeckoApi
import com.example.cryptobotmonitor.data.repository.CoinRepository
import com.example.cryptobotmonitor.fake.FakeTimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

class CoinRepositoryIntegrationTest {

    private lateinit var server: MockWebServer

    private lateinit var client: OkHttpClient

    private lateinit var api: CoinGeckoApi

    private lateinit var repository: CoinRepository

    private lateinit var cache: CoinRepository.CacheState

    private lateinit var timeProvider: FakeTimeProvider

    // Przykładowe dane kryptowalut
    private fun bitcoinJson(
        price: Double = 60000.0
    ): String = """
        {
            "id": "bitcoin",
            "symbol": "btc",
            "name": "Bitcoin",
            "current_price": $price,
            "image": ""
        }
    """.trimIndent()

    private val ethereumJson = """
        {
            "id": "ethereum",
            "symbol": "eth",
            "name": "Ethereum",
            "current_price": 3000.0,
            "image": ""
        }
    """.trimIndent()

    private fun chartJson(
        price: Double = 60000.0
    ): String = """
        {
            "prices": [
                [1000.0, 58000.0],
                [2000.0, $price]
            ]
        }
    """.trimIndent()

    @Before
    fun setUp() {

        server = MockWebServer()

        server.start()

        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v3/"))
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(CoinGeckoApi::class.java)

        timeProvider = FakeTimeProvider()

        cache = CoinRepository.CacheState()

        repository = CoinRepository(
            api = api,
            timeProvider = timeProvider,
            cache = cache
        )
    }

    @After
    fun tearDown() {

        client.dispatcher.cancelAll()

        client.connectionPool.evictAll()

        client.dispatcher.executorService.shutdown()

        server.shutdown()
    }

    // Przygotowanie odpowiedzi serwera
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

    // Symulacja przekroczenia limitu API
    private fun enqueueRateLimit(
        retryAfter: String? = null
    ) {

        val response = MockResponse()
            .setResponseCode(429)
            .setBody(
                """{"error":"Rate limit exceeded"}"""
            )

        if (retryAfter != null) {

            response.setHeader(
                "Retry-After",
                retryAfter
            )
        }

        server.enqueue(response)
    }

    // Sprawdzenie kodu błędu HTTP
    private suspend fun expectHttpError(
        expectedCode: Int,
        action: suspend () -> Unit
    ) {

        try {

            action()

            fail("Oczekiwano błędu HTTP $expectedCode")

        } catch (e: HttpException) {

            assertEquals(expectedCode, e.code())
        }
    }

    // Sprawdzenie blokady API
    private suspend fun expectBlockedApi(
        action: suspend () -> Unit
    ) {

        try {

            action()

            fail("API powinno być zablokowane")

        } catch (e: IllegalStateException) {

            assertEquals(
                "Przekroczono limit API. Spróbuj ponownie później.",
                e.message
            )
        }
    }

    // Testy cache listy kryptowalut

    @Test
    fun getCoins_firstRequestUsesApi() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        val result = repository.getCoins()

        assertEquals(1, result.size)

        assertEquals("bitcoin", result.first().id)

        assertEquals(
            60000.0,
            result.first().currentPrice,
            0.001
        )

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_secondRequestUsesCache() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        val first = repository.getCoins()

        val second = repository.getCoins()

        assertEquals(first, second)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_cacheBeforeExpiration() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        repository.getCoins()

        timeProvider.advanceBy(59_999L)

        val result = repository.getCoins()

        assertEquals(
            60000.0,
            result.first().currentPrice,
            0.001
        )

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoins_refreshAfterExpiration() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        repository.getCoins()

        timeProvider.advanceBy(60_000L)

        enqueueJson("[${bitcoinJson(65000.0)}]")

        val result = repository.getCoins()

        assertEquals(
            65000.0,
            result.first().currentPrice,
            0.001
        )

        assertEquals(2, server.requestCount)
    }

    @Test
    fun getCoins_sharedCacheBetweenRepositories() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            val first = repository.getCoins()

            val secondRepository = CoinRepository(
                api = api,
                timeProvider = timeProvider,
                cache = cache
            )

            val second = secondRepository.getCoins()

            assertEquals(first, second)

            assertEquals(1, server.requestCount)
        }

    @Test
    fun getCoins_independentCaches() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        repository.getCoins()

        val secondRepository = CoinRepository(
            api = api,
            timeProvider = timeProvider,
            cache = CoinRepository.CacheState()
        )

        enqueueJson("[${bitcoinJson(70000.0)}]")

        val result = secondRepository.getCoins()

        assertEquals(
            70000.0,
            result.first().currentPrice,
            0.001
        )

        assertEquals(2, server.requestCount)
    }

    // Testy cache kryptowalut według ID

    @Test
    fun getCoinsByIds_firstRequest() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        val result = repository.getCoinsByIds(
            "bitcoin"
        )

        assertEquals(1, result.size)

        assertEquals("bitcoin", result.first().id)

        val request = checkNotNull(
            server.takeRequest(2, TimeUnit.SECONDS)
        )

        assertEquals(
            "bitcoin",
            request.requestUrl?.queryParameter("ids")
        )

        assertEquals(
            "50",
            request.requestUrl?.queryParameter("per_page")
        )
    }

    @Test
    fun getCoinsByIds_usesCachedCoin() = runBlocking {

        enqueueJson("[${bitcoinJson()}]")

        repository.getCoinsByIds("bitcoin")

        val result = repository.getCoinsByIds(
            "bitcoin"
        )

        assertEquals("bitcoin", result.first().id)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getCoinsByIds_usesMarketListCache() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            repository.getCoins()

            val result = repository.getCoinsByIds(
                "bitcoin"
            )

            assertEquals("bitcoin", result.first().id)

            assertEquals(1, server.requestCount)
        }

    @Test
    fun getCoinsByIds_requestsOnlyMissingCoins() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            repository.getCoinsByIds("bitcoin")

            enqueueJson("[$ethereumJson]")

            val result = repository.getCoinsByIds(
                "bitcoin,ethereum"
            )

            assertEquals(
                listOf("bitcoin", "ethereum"),
                result.map { it.id }
            )

            server.takeRequest(2, TimeUnit.SECONDS)

            val request = checkNotNull(
                server.takeRequest(2, TimeUnit.SECONDS)
            )

            assertEquals(
                "ethereum",
                request.requestUrl?.queryParameter("ids")
            )

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getCoinsByIds_removesDuplicateIds() =
        runBlocking {

            enqueueJson(
                "[${bitcoinJson()},$ethereumJson]"
            )

            val result = repository.getCoinsByIds(
                " bitcoin,bitcoin, ethereum, "
            )

            assertEquals(2, result.size)

            val request = checkNotNull(
                server.takeRequest(2, TimeUnit.SECONDS)
            )

            assertEquals(
                "bitcoin,ethereum",
                request.requestUrl?.queryParameter("ids")
            )
        }

    @Test
    fun getCoinsByIds_emptyIds() = runBlocking {

        val result = repository.getCoinsByIds(
            " , , "
        )

        assertTrue(result.isEmpty())

        assertEquals(0, server.requestCount)
    }

    @Test
    fun getCoinsByIds_refreshAfterExpiration() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            repository.getCoinsByIds("bitcoin")

            timeProvider.advanceBy(60_000L)

            enqueueJson("[${bitcoinJson(62000.0)}]")

            val result = repository.getCoinsByIds(
                "bitcoin"
            )

            assertEquals(
                62000.0,
                result.first().currentPrice,
                0.001
            )

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getCoinsByIds_doesNotReturnMissingOldCoin() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            repository.getCoinsByIds("bitcoin")

            timeProvider.advanceBy(60_000L)

            enqueueJson("[]")

            val result = repository.getCoinsByIds(
                "bitcoin"
            )

            assertTrue(result.isEmpty())

            assertEquals(2, server.requestCount)
        }

    // Testy cache wykresów

    @Test
    fun getMarketChart_firstRequest() = runBlocking {

        enqueueJson(chartJson())

        val result = repository.getMarketChart(
            "bitcoin",
            "7"
        )

        assertEquals(2, result.prices.size)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getMarketChart_usesCache() = runBlocking {

        enqueueJson(chartJson())

        val first = repository.getMarketChart(
            "bitcoin",
            "7"
        )

        timeProvider.advanceBy(299_999L)

        val second = repository.getMarketChart(
            "bitcoin",
            "7"
        )

        assertEquals(first, second)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun getMarketChart_refreshAfterExpiration() =
        runBlocking {

            enqueueJson(chartJson())

            repository.getMarketChart(
                "bitcoin",
                "7"
            )

            timeProvider.advanceBy(300_000L)

            enqueueJson(chartJson(68000.0))

            val result = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            assertEquals(
                68000.0,
                result.prices.last()[1],
                0.001
            )

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getMarketChart_differentPeriods() =
        runBlocking {

            enqueueJson(chartJson(60000.0))

            val first = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            enqueueJson(chartJson(65000.0))

            val second = repository.getMarketChart(
                "bitcoin",
                "30"
            )

            val cached = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            assertNotEquals(first, second)

            assertEquals(first, cached)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getMarketChart_differentCoins() =
        runBlocking {

            enqueueJson(chartJson(60000.0))

            val bitcoin = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            enqueueJson(chartJson(3000.0))

            val ethereum = repository.getMarketChart(
                "ethereum",
                "7"
            )

            assertNotEquals(bitcoin, ethereum)

            assertEquals(
                3000.0,
                ethereum.prices.last()[1],
                0.001
            )

            assertEquals(2, server.requestCount)
        }

    // Testy przekroczenia limitu API

    @Test
    fun getCoins_http429WithoutCache() =
        runBlocking {

            enqueueRateLimit()

            expectHttpError(429) {

                repository.getCoins()
            }

            expectBlockedApi {

                repository.getCoins()
            }

            assertEquals(1, server.requestCount)
        }

    @Test
    fun getCoins_http429ReturnsOldCache() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            val first = repository.getCoins()

            timeProvider.advanceBy(60_000L)

            enqueueRateLimit("60")

            val result = repository.getCoins()

            assertEquals(first, result)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getCoins_blockedApiUsesOldCache() =
        runBlocking {

            enqueueJson("[${bitcoinJson()}]")

            val first = repository.getCoins()

            timeProvider.advanceBy(60_000L)

            enqueueRateLimit("60")

            repository.getCoins()

            val result = repository.getCoins()

            assertEquals(first, result)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getCoins_retryAfterHeader() = runBlocking {

        enqueueRateLimit("30")

        expectHttpError(429) {

            repository.getCoins()
        }

        timeProvider.advanceBy(29_999L)

        expectBlockedApi {

            repository.getCoins()
        }

        assertEquals(1, server.requestCount)

        timeProvider.advanceBy(1L)

        enqueueJson("[${bitcoinJson()}]")

        val result = repository.getCoins()

        assertEquals(1, result.size)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun getCoins_defaultRetryTime() = runBlocking {

        enqueueRateLimit()

        expectHttpError(429) {

            repository.getCoins()
        }

        timeProvider.advanceBy(59_999L)

        expectBlockedApi {

            repository.getCoins()
        }

        timeProvider.advanceBy(1L)

        enqueueJson("[${bitcoinJson()}]")

        val result = repository.getCoins()

        assertFalse(result.isEmpty())

        assertEquals(2, server.requestCount)
    }

    @Test
    fun getCoins_invalidRetryAfterUsesDefault() =
        runBlocking {

            enqueueRateLimit("invalid")

            expectHttpError(429) {

                repository.getCoins()
            }

            timeProvider.advanceBy(59_999L)

            expectBlockedApi {

                repository.getCoins()
            }

            timeProvider.advanceBy(1L)

            enqueueJson("[${bitcoinJson()}]")

            repository.getCoins()

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getMarketChart_http429ReturnsOldCache() =
        runBlocking {

            enqueueJson(chartJson())

            val first = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            timeProvider.advanceBy(300_000L)

            enqueueRateLimit("60")

            val second = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            val third = repository.getMarketChart(
                "bitcoin",
                "7"
            )

            assertEquals(first, second)

            assertEquals(first, third)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun apiBlock_isSharedBetweenMethods() =
        runBlocking {

            enqueueRateLimit("60")

            expectHttpError(429) {

                repository.getCoins()
            }

            expectBlockedApi {

                repository.getCoinsByIds("bitcoin")
            }

            expectBlockedApi {

                repository.getMarketChart(
                    "bitcoin",
                    "7"
                )
            }

            assertEquals(1, server.requestCount)
        }

    // Testy błędów serwera i połączenia

    @Test
    fun getCoins_http500DoesNotBlockApi() =
        runBlocking {

            enqueueJson(
                """{"error":"Server error"}""",
                500
            )

            expectHttpError(500) {

                repository.getCoins()
            }

            enqueueJson("[${bitcoinJson()}]")

            val result = repository.getCoins()

            assertEquals(1, result.size)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getCoins_networkErrorDoesNotBlockApi() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setSocketPolicy(
                        SocketPolicy.DISCONNECT_AT_START
                    )
            )

            try {

                repository.getCoins()

                fail("Oczekiwano błędu połączenia")

            } catch (e: IOException) {

                assertNotNull(e)
            }

            enqueueJson("[${bitcoinJson()}]")

            val result = repository.getCoins()

            assertEquals(1, result.size)
        }

    // Test jednoczesnych zapytań

    @Test
    fun getCoins_concurrentRequestsUseOneApiCall() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody("[${bitcoinJson()}]")
                    .setBodyDelay(
                        100,
                        TimeUnit.MILLISECONDS
                    )
            )

            val secondRepository = CoinRepository(
                api = api,
                timeProvider = timeProvider,
                cache = cache
            )

            val results = listOf(

                async {
                    repository.getCoins()
                },

                async {
                    secondRepository.getCoins()
                },

                async {
                    repository.getCoins()
                }

            ).awaitAll()

            assertEquals(3, results.size)

            results.forEach { coins ->

                assertEquals(1, coins.size)

                assertEquals(
                    "bitcoin",
                    coins.first().id
                )
            }

            assertEquals(1, server.requestCount)
        }
}
