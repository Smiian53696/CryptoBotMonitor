
package com.example.cryptobotmonitor.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.data.repository.CoinRepository
import com.example.cryptobotmonitor.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import com.example.cryptobotmonitor.data.preferences.MonitoringPreferences
class PriceAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val coinRepository = CoinRepository()
    private val monitoringPreferences =
        MonitoringPreferences(applicationContext)
    override suspend fun doWork(): Result {

        // Blokada równoczesnego sprawdzania alertów
        return workerMutex.withLock {
            checkAlerts()
        }
    }

    // Sprawdzanie alertów użytkownika
    private suspend fun checkAlerts(): Result {

        return try {

            Log.d(TAG, "Rozpoczęto sprawdzanie alertów")

            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            // Sprawdzenie zalogowanego użytkownika
            if (userId == null) {
                Log.d(TAG, "Brak zalogowanego użytkownika")
                return Result.success()
            }

            // Sprawdzenie dostępu do powiadomień
            if (!NotificationHelper.canSendNotifications(
                    applicationContext
                )
            ) {
                Log.w(TAG, "Powiadomienia są wyłączone")
                return Result.success()
            }

            val firestore = FirebaseFirestore.getInstance()

            // Pobranie aktywnych alertów
            val snapshot = firestore.collection("alerts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()

            val alerts = snapshot.documents.mapNotNull { document ->
                document.toObject(PriceAlert::class.java)
            }

            Log.d(TAG, "Liczba aktywnych alertów: ${alerts.size}")

            if (alerts.isEmpty()) {

                // Zapis sprawdzenia bez aktywnych alertów
                monitoringPreferences.saveLastCheck(
                    userId = userId,
                    completedAlerts = 0
                )

                Log.d(TAG, "Brak aktywnych alertów")

                return Result.success()
            }

            // Pobranie unikalnych ID kryptowalut
            val coinIds = alerts
                .map { it.coinId }
                .filter { it.isNotBlank() }
                .distinct()

            // Pobranie cen przez wspólne repozytorium
            val coins = coinIds.chunked(50).flatMap { ids ->
                coinRepository.getCoinsByIds(
                    ids.joinToString(",")
                )
            }

            val prices = coins.associate {
                it.id to it.currentPrice
            }

            var completedAlerts = 0
            var missingPrices = false

            for (alert in alerts) {

                // Sprawdzenie czy użytkownik się nie zmienił
                if (auth.currentUser?.uid != userId) {
                    Log.w(TAG, "Użytkownik został zmieniony")
                    return Result.success()
                }

                val currentPrice = prices[alert.coinId]

                if (currentPrice == null ||
                    !currentPrice.isFinite() ||
                    currentPrice <= 0
                ) {
                    Log.w(TAG, "Brak ceny: ${alert.coinId}")
                    missingPrices = true
                    continue
                }

                // Sprawdzenie poprawności ceny docelowej
                if (!alert.targetPrice.isFinite() ||
                    alert.targetPrice <= 0
                ) {
                    Log.w(TAG, "Niepoprawna cena alertu")
                    continue
                }

                // Sprawdzenie warunku alertu
                val shouldSendNotification = when (alert.condition) {
                    "ABOVE" -> currentPrice >= alert.targetPrice
                    "BELOW" -> currentPrice <= alert.targetPrice
                    else -> false
                }

                Log.d(
                    TAG,
                    "${alert.coinName}: cena=$currentPrice, " +
                            "cel=${alert.targetPrice}, " +
                            "warunek=$shouldSendNotification"
                )

                if (!shouldSendNotification) {
                    continue
                }

                // Ponowne sprawdzenie stanu alertu
                val document = firestore.collection("alerts")
                    .document(alert.id)

                val currentAlert = document
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
                    .toObject(PriceAlert::class.java)

                if (currentAlert == null ||
                    !currentAlert.active ||
                    currentAlert != alert ||
                    auth.currentUser?.uid != userId
                ) {
                    continue
                }

                // Wysłanie powiadomienia
                val notificationSent =
                    NotificationHelper.showPriceAlert(
                        context = applicationContext,
                        alert = alert,
                        currentPrice = currentPrice
                    )

                // Alert zostaje aktywny, jeśli nie wysłano powiadomienia
                if (!notificationSent) {
                    Log.w(
                        TAG,
                        "Nie wysłano powiadomienia: ${alert.coinName}"
                    )
                    continue
                }

                // Wyłączenie alertu po wysłaniu powiadomienia
                document.update("active", false).await()

                completedAlerts++

                Log.d(TAG, "Alert wykonany: ${alert.coinName}")
            }

            Log.d(
                TAG,
                "Zakończono sprawdzanie. " +
                        "Wykonane alerty: $completedAlerts"
            )


            if (missingPrices) {

                Result.retry()

            } else {

                // Zapis ostatniego udanego sprawdzenia
                monitoringPreferences.saveLastCheck(
                    userId = userId,
                    completedAlerts = completedAlerts
                )

                Log.d(TAG, "Zapisano czas ostatniego sprawdzenia")

                Result.success()
            }


        } catch (e: CancellationException) {

            Log.d(TAG, "Sprawdzanie zostało anulowane")
            throw e

        } catch (e: HttpException) {

            Log.e(TAG, "Błąd API: ${e.code()}", e)

            Result.retry()

        } catch (e: Exception) {

            Log.e(TAG, "Błąd sprawdzania alertów", e)

            Result.retry()
        }
    }

    companion object {

        private const val TAG = "PriceAlertWorker"

        private const val PERIODIC_WORK_NAME =
            "price_alert_worker"

        private const val ONE_TIME_WORK_NAME =
            "price_alert_worker_once"

        // Wspólna blokada dla wszystkich workerów
        private val workerMutex = Mutex()

        // Automatyczne sprawdzanie alertów
        fun schedule(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    60,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )

            Log.d(TAG, "Zaplanowano monitorowanie okresowe")
        }

        // Ręczne sprawdzanie alertów
        fun runOnce(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request =
                OneTimeWorkRequestBuilder<PriceAlertWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        60,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )

            Log.d(TAG, "Zlecono ręczne sprawdzenie alertów")
        }
    }
}
