package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.api.RetrofitInstance
import com.example.cryptobotmonitor.data.model.CoinModel

// Repozytorium odpowiada za pobieranie danych z API
class CoinRepository {

    // Funkcja pobiera listę kryptowalut z CoinGecko
    suspend fun getCoins(): List<CoinModel> {
        return RetrofitInstance.api.getCoins()
    }
}