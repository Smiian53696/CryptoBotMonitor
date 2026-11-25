
package com.example.cryptobotmonitor.data.model

// Model ulubionej kryptowaluty
data class FavoriteCoin(
    val id: String = "",
    val userId: String = "",
    val coinId: String = "",
    val coinName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
