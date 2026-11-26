
package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.data.repository.AlertRepositoryInterface
import com.google.firebase.firestore.ListenerRegistration

// Repozytorium alertów do testów
class FakeAlertRepository : AlertRepositoryInterface {

    var suspendError: Exception? = null

    var suspendAddSuccess = true

    var getAlertsCalls = 0
        private set

    var deleteByCoinCalls = 0
        private set

    var lastDeletedCoinId = ""
        private set
    var alerts: List<PriceAlert> = emptyList()
        private set

    var addCalls = 0
        private set

    var deleteCalls = 0
        private set

    var observeCalls = 0
        private set

    var listenerRemoveCalls = 0
        private set

    var lastAddedAlert: PriceAlert? = null
        private set

    var lastDeletedId = ""
        private set

    private var nextId = 1

    private var resultListener:
            ((List<PriceAlert>) -> Unit)? = null

    private var errorListener:
            ((String) -> Unit)? = null

    private var pendingAddResult:
            ((Boolean, String?) -> Unit)? = null

    private var pendingDeleteResult:
            ((Boolean, String?) -> Unit)? = null

    // Tworzenie przykładowego alertu
    private fun createAlert(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String
    ): PriceAlert {

        return PriceAlert(
            id = "alert-${nextId++}",
            userId = "test-user",
            coinId = coinId,
            coinName = coinName,
            targetPrice = targetPrice,
            condition = condition,
            active = true
        )
    }

    // Symulacja dodawania alertu
    override fun addAlert(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        addCalls++

        lastAddedAlert = createAlert(
            coinId,
            coinName,
            targetPrice,
            condition
        )

        pendingAddResult = onResult
    }

    // Zakończenie zapisywania alertu
    fun completeAdd(
        success: Boolean,
        message: String? = null
    ) {

        val callback = checkNotNull(pendingAddResult)

        pendingAddResult = null

        if (success) {

            val alert = checkNotNull(lastAddedAlert)

            alerts = alerts + alert

            resultListener?.invoke(alerts)
        }

        callback(success, message)
    }

    // Pobieranie listy alertów
    override suspend fun getUserAlertsOnce():
            List<PriceAlert> {

        getAlertsCalls++

        suspendError?.let {
            throw it
        }

        return alerts
    }

    // Dodawanie alertu przez bota
    override suspend fun addAlertSuspend(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String
    ): Boolean {

        addCalls++

        suspendError?.let {
            throw it
        }

        if (!suspendAddSuccess) {
            return false
        }

        val alert = createAlert(
            coinId,
            coinName,
            targetPrice,
            condition
        )

        lastAddedAlert = alert

        alerts = alerts + alert

        resultListener?.invoke(alerts)

        return true
    }

    // Usuwanie alertów według ID kryptowaluty
    override suspend fun deleteAlertByCoinId(
        coinId: String
    ): Boolean {

        deleteByCoinCalls++

        lastDeletedCoinId = coinId

        suspendError?.let {
            throw it
        }

        val previousSize = alerts.size

        alerts = alerts.filterNot {
            it.coinId == coinId
        }

        resultListener?.invoke(alerts)

        return alerts.size < previousSize
    }

    // Symulacja obserwowania Firestore
    override fun observeUserAlerts(
        onResult: (List<PriceAlert>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        observeCalls++

        resultListener = onResult
        errorListener = onError

        return object : ListenerRegistration {

            override fun remove() {

                listenerRemoveCalls++

                resultListener = null
                errorListener = null
            }
        }
    }

    // Symulacja usuwania alertu
    override fun deleteAlert(
        alertId: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        deleteCalls++

        lastDeletedId = alertId

        pendingDeleteResult = onResult
    }

    // Zakończenie usuwania alertu
    fun completeDelete(
        success: Boolean,
        message: String? = null
    ) {

        val callback = checkNotNull(pendingDeleteResult)

        pendingDeleteResult = null

        if (success) {

            alerts = alerts.filterNot {
                it.id == lastDeletedId
            }

            resultListener?.invoke(alerts)
        }

        callback(success, message)
    }

    // Symulacja aktualizacji listy alertów
    fun emitAlerts(newAlerts: List<PriceAlert>) {

        alerts = newAlerts

        resultListener?.invoke(alerts)
    }

    // Symulacja błędu Firestore
    fun emitError(message: String) {

        errorListener?.invoke(message)
    }
}
