
package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.MarketChartModel
import com.example.cryptobotmonitor.data.repository.CoinRepositoryInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

// Repozytorium kryptowalut do testów
class FakeCoinRepository : CoinRepositoryInterface {

    var coins: List<CoinModel> = emptyList()

    var marketChart = MarketChartModel()

    var error: Exception? = null

    var chartError: Exception? = null

    var cancelledChartCalls = 0
        private set

    var responseDelay = 0L

    var getCoinsCalls = 0
        private set

    var getCoinsByIdsCalls = 0
        private set

    var getMarketChartCalls = 0
        private set

    var lastCoinIds = ""
        private set

    var lastChartCoinId = ""
        private set

    var lastChartDays = ""
        private set

    // Symulacja pobierania listy kryptowalut
    override suspend fun getCoins(): List<CoinModel> {

        getCoinsCalls++

        simulateResponse()

        return coins
    }

    // Symulacja pobierania kryptowalut po ID
    override suspend fun getCoinsByIds(
        ids: String
    ): List<CoinModel> {

        getCoinsByIdsCalls++

        lastCoinIds = ids

        simulateResponse()

        val requestedIds = ids.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return requestedIds.mapNotNull { id ->

            coins.firstOrNull { coin ->
                coin.id == id
            }
        }
    }

    // Symulacja pobierania wykresu
    // Symulacja pobierania wykresu
    override suspend fun getMarketChart(
        coinId: String,
        days: String
    ): MarketChartModel {

        getMarketChartCalls++

        lastChartCoinId = coinId
        lastChartDays = days

        try {

            simulateResponse()

            chartError?.let {
                throw it
            }

            return marketChart

        } catch (e: CancellationException) {

            cancelledChartCalls++

            throw e
        }
    }

    // Symulacja opóźnienia i błędu API
    private suspend fun simulateResponse() {

        if (responseDelay > 0) {
            delay(responseDelay)
        }

        error?.let {
            throw it
        }
    }
}
