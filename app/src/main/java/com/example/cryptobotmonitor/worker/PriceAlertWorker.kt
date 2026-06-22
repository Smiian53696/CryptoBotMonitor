package com.example.cryptobotmonitor.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cryptobotmonitor.data.api.RetrofitInstance
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PriceAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            // Bez użytkownika nie ma czego sprawdzać
            if (userId == null) {
                Log.d("PriceAlertWorker", "Brak zalogowanego użytkownika")
                return Result.success()
            }

            val firestore = FirebaseFirestore.getInstance()

            // Pobranie aktywnych alertów użytkownika
            val snapshot = firestore.collection("alerts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()

            val alerts = snapshot.documents.mapNotNull { document ->
                document.toObject(PriceAlert::class.java)
            }

            if (alerts.isEmpty()) {
                Log.d("PriceAlertWorker", "Brak aktywnych alertów")
                return Result.success()
            }

            // Lista coinów do sprawdzenia w API
            val coinIds = alerts
                .map { it.coinId }
                .distinct()
                .joinToString(",")

            // Pobranie aktualnych cen z CoinGecko
            val coins = RetrofitInstance.api.getCoins(
                ids = coinIds,
                perPage = alerts.size
            )

            val prices = coins.associate {
                it.id to it.currentPrice
            }

            alerts.forEach { alert ->

                val currentPrice = prices[alert.coinId] ?: return@forEach

                val shouldSendNotification =
                    when (alert.condition) {
                        "ABOVE" -> currentPrice >= alert.targetPrice
                        "BELOW" -> currentPrice <= alert.targetPrice
                        else -> false
                    }

                if (shouldSendNotification) {
                    // Pokazanie powiadomienia
                    NotificationHelper.showPriceAlert(
                        context = applicationContext,
                        alert = alert,
                        currentPrice = currentPrice
                    )

                    // Wyłączenie alertu, żeby nie spamować
                    firestore.collection("alerts")
                        .document(alert.id)
                        .update("active", false)
                        .await()

                    Log.d("PriceAlertWorker", "Alert wykonany: ${alert.coinName}")
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Błąd sprawdzania alertów", e)
            Result.retry()
        }
    }

    companion object {

        private const val PERIODIC_WORK_NAME = "price_alert_worker"
        private const val ONE_TIME_WORK_NAME = "price_alert_worker_once"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Sprawdzanie alertów co 15 minut
            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Jednorazowe sprawdzenie do testu
            val request = OneTimeWorkRequestBuilder<PriceAlertWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

