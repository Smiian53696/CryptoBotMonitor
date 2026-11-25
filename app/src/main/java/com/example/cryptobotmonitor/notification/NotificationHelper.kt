
package com.example.cryptobotmonitor.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cryptobotmonitor.data.model.PriceAlert
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "price_alerts_channel"
    private const val TAG = "NotificationHelper"

    // Sprawdzenie czy można wysyłać powiadomienia
    fun canSendNotifications(context: Context): Boolean {

        createChannel(context)

        // Sprawdzenie uprawnień w Androidzie 13 i nowszym
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Brak uprawnienia do powiadomień")
                return false
            }
        }

        // Sprawdzenie ustawień aplikacji
        if (!NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            Log.w(TAG, "Powiadomienia aplikacji są wyłączone")
            return false
        }

        // Sprawdzenie kanału powiadomień
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            val channel = manager.getNotificationChannel(CHANNEL_ID)

            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "Kanał alertów jest wyłączony")
                return false
            }
        }

        return true
    }

    // Wyświetlanie powiadomienia o alercie
    fun showPriceAlert(
        context: Context,
        alert: PriceAlert,
        currentPrice: Double
    ): Boolean {

        if (!canSendNotifications(context)) {
            return false
        }

        val conditionText = when (alert.condition) {
            "ABOVE" -> "powyżej"
            "BELOW" -> "poniżej"
            else -> return false
        }

        val message =
            "Cena $conditionText ${formatPrice(alert.targetPrice)} USD. " +
                    "Aktualnie: ${formatPrice(currentPrice)} USD"

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Alert cenowy: ${alert.coinName}")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        return try {

            NotificationManagerCompat.from(context).notify(
                alert.id,
                0,
                notification
            )

            Log.d(TAG, "Wysłano powiadomienie: ${alert.coinName}")

            true

        } catch (e: SecurityException) {

            Log.e(TAG, "Brak dostępu do powiadomień", e)

            false
        }
    }

    // Formatowanie ceny kryptowaluty
    private fun formatPrice(price: Double): String {

        val pattern = when {
            price >= 1 -> "%,.2f"
            price >= 0.01 -> "%.4f"
            else -> "%.8f"
        }

        return String.format(
            Locale.US,
            pattern,
            price
        )
    }

    // Tworzenie kanału powiadomień
    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alerty cenowe",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o zmianach cen kryptowalut"
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }
}
