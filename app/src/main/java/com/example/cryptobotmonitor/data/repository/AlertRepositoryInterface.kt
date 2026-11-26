
package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.model.PriceAlert
import com.google.firebase.firestore.ListenerRegistration

// Interfejs repozytorium alertów
interface AlertRepositoryInterface {

    fun addAlert(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String,
        onResult: (Boolean, String?) -> Unit
    )

    suspend fun getUserAlertsOnce(): List<PriceAlert>

    suspend fun addAlertSuspend(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String
    ): Boolean

    suspend fun deleteAlertByCoinId(
        coinId: String
    ): Boolean

    fun observeUserAlerts(
        onResult: (List<PriceAlert>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    fun deleteAlert(
        alertId: String,
        onResult: (Boolean, String?) -> Unit
    )
}
