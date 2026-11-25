package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.api.RetrofitInstance
import com.example.cryptobotmonitor.data.model.CoinModel

// Repozytorium odpowiada za pobieranie danych z API
class CoinRepository {

    // Funkcja pobiera listę kryptowalut z CoinGecko
    suspend fun getCoins(): List<CoinModel> {
        return RetrofitInstance.api.getCoins()
    }
    // Pobranie jednej lub kilku kryptowalut po id
    suspend fun getCoinsByIds(ids: String): List<CoinModel> {
        return RetrofitInstance.api.getCoins(
            ids = ids,
            perPage = 50
        )
    }
    // Pobranie danych do wykresu ceny
    suspend fun getMarketChart(
        coinId: String,
        days: String = "7"
    ) = RetrofitInstance.api.getMarketChart(
        coinId = coinId,
        days = days
    )
}