package com.example.cryptobotmonitor.data.model

import com.google.gson.annotations.SerializedName

// Model reprezentujący kryptowalutę z API CoinGecko
data class CoinModel(

    // Id kryptowaluty
    val id: String,

    // Symbol kryptowaluty
    val symbol: String,

    // Nazwa kryptowaluty
    val name: String,

    // Aktualna cena
    @SerializedName("current_price")
    val currentPrice: Double,

    // Link do obrazka kryptowaluty
    val image: String,

    // Zmiana ceny w ciągu 24h
    @SerializedName("price_change_percentage_24h")
    val priceChange24h: Double?
)