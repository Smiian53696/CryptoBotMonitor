package com.example.cryptobotmonitor.data.api

import com.example.cryptobotmonitor.data.model.CoinModel
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.cryptobotmonitor.data.model.MarketChartModel
import retrofit2.http.Path

// Interfejs API dla CoinGecko
interface CoinGeckoApi {

    // Pobieranie listy kryptowalut
    @GET("coins/markets")
    suspend fun getCoins(

        // Waluta
        @Query("vs_currency")
        currency: String = "usd",

        // Konkretne kryptowaluty, np. bitcoin,ethereum
        @Query("ids")
        ids: String? = null,

        // Sortowanie
        @Query("order")
        order: String = "market_cap_desc",

        // Ilość wyników
        @Query("per_page")
        perPage: Int = 20,

        // Numer strony
        @Query("page")
        page: Int = 1

    ): List<CoinModel>
    // Pobranie danych historycznych do wykresu
    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id")
        coinId: String,

        @Query("vs_currency")
        currency: String = "usd",

        @Query("days")
        days: String = "7"
    ): MarketChartModel
}