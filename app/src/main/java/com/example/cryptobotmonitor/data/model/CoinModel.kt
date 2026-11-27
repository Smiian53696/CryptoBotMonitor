package com.example.cryptobotmonitor.data.model

import com.google.gson.annotations.SerializedName

// Model kryptowaluty pobieranej z API
data class CoinModel(
    val id: String,
    val symbol: String,
    val name: String,

    @SerializedName("current_price")
    val currentPrice: Double,

    val image: String,

    @SerializedName("price_change_percentage_24h")
    val priceChange24h: Double? = null,

    @SerializedName("market_cap")
    val marketCap: Double? = null,

    @SerializedName("total_volume")
    val totalVolume: Double? = null,

    @SerializedName("high_24h")
    val high24h: Double? = null,

    @SerializedName("low_24h")
    val low24h: Double? = null
)