
package com.example.cryptobotmonitor.data.repository

import android.util.Log
import com.example.cryptobotmonitor.data.api.CoinGeckoApi
import com.example.cryptobotmonitor.data.api.RetrofitInstance
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.MarketChartModel
import com.example.cryptobotmonitor.utils.SystemTimeProvider
import com.example.cryptobotmonitor.utils.TimeProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class CoinRepository(
    private val api: CoinGeckoApi = RetrofitInstance.api,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val cache: CacheState = sharedCache
) : CoinRepositoryInterface {

    // Dane przechowywane w pamięci
    class CacheState {

        internal var cachedCoins: List<CoinModel> =
            emptyList()

        internal var lastCoinsUpdate = 0L

        internal val coinsByIdCache =
            mutableMapOf<String, Pair<CoinModel, Long>>()

        internal val chartCache =
            mutableMapOf<String, Pair<MarketChartModel, Long>>()

        internal var blockedUntil = 0L

        internal val mutex = Mutex()
    }

    companion object {

        // Czas przechowywania danych w pamięci
        private const val COINS_CACHE_TIME = 60_000L

        private const val CHART_CACHE_TIME = 300_000L

        // Przerwa po przekroczeniu limitu API
        private const val RETRY_TIME = 60_000L

        // Wspólna pamięć dla wszystkich repozytoriów
        private val sharedCache = CacheState()
    }

    // Pobieranie listy kryptowalut
    override suspend fun getCoins(): List<CoinModel> =
        cache.mutex.withLock {

            val now = timeProvider.now()

            // Sprawdzenie zapisanych danych
            if (cache.cachedCoins.isNotEmpty() &&
                now - cache.lastCoinsUpdate < COINS_CACHE_TIME
            ) {

                Log.d(
                    "CoinRepository",
                    "Lista pobrana z cache"
                )

                return@withLock cache.cachedCoins
            }

            // Korzystanie z zapisanych danych podczas blokady API
            if (now < cache.blockedUntil &&
                cache.cachedCoins.isNotEmpty()
            ) {

                return@withLock cache.cachedCoins
            }

            checkApiAvailability()

            try {

                val result = api.getCoins()

                val updateTime = timeProvider.now()

                cache.cachedCoins = result

                cache.lastCoinsUpdate = updateTime

                result.forEach { coin ->

                    cache.coinsByIdCache[coin.id] =
                        Pair(coin, updateTime)
                }

                Log.d(
                    "CoinRepository",
                    "Lista pobrana z API"
                )

                result

            } catch (e: HttpException) {

                handleApiError(e)

                if (e.code() == 429 &&
                    cache.cachedCoins.isNotEmpty()
                ) {

                    cache.cachedCoins

                } else {

                    throw e
                }
            }
        }

    // Pobieranie kryptowalut po ID
    override suspend fun getCoinsByIds(
        ids: String
    ): List<CoinModel> = cache.mutex.withLock {

        val now = timeProvider.now()

        val requestedIds = ids.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (requestedIds.isEmpty()) {

            return@withLock emptyList()
        }

        // Sprawdzenie czy dane są już zapisane
        val missingIds = requestedIds.filter { id ->

            val cached = cache.coinsByIdCache[id]

            cached == null ||
                    now - cached.second >= COINS_CACHE_TIME
        }

        if (missingIds.isEmpty()) {

            Log.d(
                "CoinRepository",
                "Dane monety pobrane z cache"
            )

            return@withLock requestedIds.mapNotNull {

                cache.coinsByIdCache[it]?.first
            }
        }

        checkApiAvailability()

        try {

            val result = api.getCoins(
                ids = missingIds.joinToString(","),
                perPage = 50
            )

            val updateTime = timeProvider.now()

            result.forEach { coin ->

                cache.coinsByIdCache[coin.id] =
                    Pair(coin, updateTime)
            }

            Log.d(
                "CoinRepository",
                "Dane monety pobrane z API"
            )

            requestedIds.mapNotNull { id ->

                cache.coinsByIdCache[id]?.first?.takeIf {

                    id !in missingIds ||
                            result.any { coin ->

                                coin.id == id
                            }
                }
            }

        } catch (e: HttpException) {

            handleApiError(e)

            throw e
        }
    }

    // Pobieranie danych historycznych
    override suspend fun getMarketChart(
        coinId: String,
        days: String
    ): MarketChartModel = cache.mutex.withLock {

        val key = "${coinId}_${days}"

        val now = timeProvider.now()

        val cached = cache.chartCache[key]

        // Sprawdzenie aktualności wykresu
        if (cached != null &&
            now - cached.second < CHART_CACHE_TIME
        ) {

            Log.d(
                "CoinRepository",
                "Wykres pobrany z cache"
            )

            return@withLock cached.first
        }

        // Korzystanie z cache podczas blokady API
        if (now < cache.blockedUntil &&
            cached != null
        ) {

            return@withLock cached.first
        }

        checkApiAvailability()

        try {

            val result = api.getMarketChart(
                coinId = coinId,
                days = days
            )

            cache.chartCache[key] = Pair(
                result,
                timeProvider.now()
            )

            Log.d(
                "CoinRepository",
                "Wykres pobrany z API"
            )

            result

        } catch (e: HttpException) {

            handleApiError(e)

            if (e.code() == 429 &&
                cached != null
            ) {

                cached.first

            } else {

                throw e
            }
        }
    }

    // Sprawdzenie dostępności API
    private fun checkApiAvailability() {

        if (timeProvider.now() < cache.blockedUntil) {

            throw IllegalStateException(
                "Przekroczono limit API. Spróbuj ponownie później."
            )
        }
    }

    // Obsługa przekroczenia limitu zapytań
    private fun handleApiError(error: HttpException) {

        if (error.code() == 429) {

            val retryAfter = error.response()
                ?.headers()
                ?.get("Retry-After")
                ?.toLongOrNull()

            val waitTime = retryAfter
                ?.coerceIn(1L, 86400L)
                ?.times(1000L)
                ?: RETRY_TIME

            cache.blockedUntil =
                timeProvider.now() + waitTime

            Log.w(
                "CoinRepository",
                "Przekroczono limit API (429)"
            )
        }
    }
}
