
package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.MarketChartModel

// Interfejs repozytorium kryptowalut
interface CoinRepositoryInterface {

    suspend fun getCoins(): List<CoinModel>

    suspend fun getCoinsByIds(
        ids: String
    ): List<CoinModel>

    suspend fun getMarketChart(
        coinId: String,
        days: String = "7"
    ): MarketChartModel
}
