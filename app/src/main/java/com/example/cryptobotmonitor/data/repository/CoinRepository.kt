
package com.example.cryptobotmonitor.data.repository

import android.os.SystemClock
import android.util.Log
import com.example.cryptobotmonitor.data.api.RetrofitInstance
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.MarketChartModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class CoinRepository : CoinRepositoryInterface {

    companion object {

        // Czas przechowywania danych w pamięci
        private const val COINS_CACHE_TIME = 60_000L
        private const val CHART_CACHE_TIME = 300_000L

        // Przerwa po przekroczeniu limitu API
        private const val RETRY_TIME = 60_000L

        // Wspólna pamięć dla wszystkich repozytoriów
        private var cachedCoins: List<CoinModel> = emptyList()
        private var lastCoinsUpdate = 0L

        private val coinsByIdCache =
            mutableMapOf<String, Pair<CoinModel, Long>>()

        private val chartCache =
            mutableMapOf<String, Pair<MarketChartModel, Long>>()

        private var blockedUntil = 0L

        // Blokada jednoczesnych zapytań
        private val mutex = Mutex()
    }

    // Pobieranie listy kryptowalut
    override suspend fun getCoins(): List<CoinModel> =
        mutex.withLock {

        val now = SystemClock.elapsedRealtime()

        // Sprawdzenie zapisanych danych
        if (cachedCoins.isNotEmpty() &&
            now - lastCoinsUpdate < COINS_CACHE_TIME
        ) {
            Log.d("CoinRepository", "Lista pobrana z cache")
            return@withLock cachedCoins
        }

        // Korzystanie z zapisanych danych podczas blokady API
        if (now < blockedUntil && cachedCoins.isNotEmpty()) {
            return@withLock cachedCoins
        }

        checkApiAvailability()

        try {
            val result = RetrofitInstance.api.getCoins()
            val updateTime = SystemClock.elapsedRealtime()

            cachedCoins = result
            lastCoinsUpdate = updateTime

            result.forEach { coin ->
                coinsByIdCache[coin.id] = Pair(coin, updateTime)
            }

            Log.d("CoinRepository", "Lista pobrana z API")

            result

        } catch (e: HttpException) {
            handleApiError(e)

            if (e.code() == 429 && cachedCoins.isNotEmpty()) {
                cachedCoins
            } else {
                throw e
            }
        }
    }

    // Pobieranie kryptowalut po ID
    override suspend fun getCoinsByIds(
        ids: String
    ): List<CoinModel> = mutex.withLock {

        val now = SystemClock.elapsedRealtime()

        val requestedIds = ids.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (requestedIds.isEmpty()) {
            return@withLock emptyList()
        }

        // Sprawdzenie czy dane są już zapisane
        val missingIds = requestedIds.filter { id ->
            val cached = coinsByIdCache[id]

            cached == null ||
                    now - cached.second >= COINS_CACHE_TIME
        }

        if (missingIds.isEmpty()) {
            Log.d("CoinRepository", "Dane monety pobrane z cache")

            return@withLock requestedIds.mapNotNull {
                coinsByIdCache[it]?.first
            }
        }

        checkApiAvailability()

        try {
            val result = RetrofitInstance.api.getCoins(
                ids = missingIds.joinToString(","),
                perPage = 50
            )

            val updateTime = SystemClock.elapsedRealtime()

            result.forEach { coin ->
                coinsByIdCache[coin.id] = Pair(coin, updateTime)
            }

            Log.d("CoinRepository", "Dane monety pobrane z API")

            requestedIds.mapNotNull { id ->
                coinsByIdCache[id]?.first?.takeIf {
                    id !in missingIds || result.any { coin ->
                        coin.id == id
                    }
                }
            }

        } catch (e: HttpException) {
            handleApiError(e)
            throw e
        }
    }

    override suspend fun getMarketChart(
        coinId: String,
        days: String
    ): MarketChartModel = mutex.withLock {

        val key = "${coinId}_${days}"
        val now = SystemClock.elapsedRealtime()

        val cached = chartCache[key]

        if (cached != null &&
            now - cached.second < CHART_CACHE_TIME
        ) {
            Log.d("CoinRepository", "Wykres pobrany z cache")
            return@withLock cached.first
        }

        if (now < blockedUntil && cached != null) {
            return@withLock cached.first
        }

        checkApiAvailability()

        try {
            val result = RetrofitInstance.api.getMarketChart(
                coinId = coinId,
                days = days
            )

            chartCache[key] = Pair(
                result,
                SystemClock.elapsedRealtime()
            )

            Log.d("CoinRepository", "Wykres pobrany z API")

            result

        } catch (e: HttpException) {
            handleApiError(e)

            if (e.code() == 429 && cached != null) {
                cached.first
            } else {
                throw e
            }
        }
    }

    // Sprawdzenie czy API nie jest tymczasowo zablokowane
    private fun checkApiAvailability() {
        if (SystemClock.elapsedRealtime() < blockedUntil) {
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

            blockedUntil =
                SystemClock.elapsedRealtime() + waitTime

            Log.w("CoinRepository", "Przekroczono limit API (429)")
        }
    }
}
