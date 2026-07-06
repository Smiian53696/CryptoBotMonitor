package com.example.cryptobotmonitor.data.model

import com.google.gson.annotations.SerializedName

// Model danych do wykresu ceny
data class MarketChartModel(
    @SerializedName("prices")
    val prices: List<List<Double>> = emptyList()
)