package com.example.cryptobotmonitor.data.model
// Model jednego alertu cenowego
data class PriceAlert(
    val id: String = "",
    val userId: String = "",
    val coinId: String = "",
    val coinName: String = "",
    val targetPrice: Double = 0.0,
    val condition: String = "ABOVE", // gdy cena wyszcza
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)