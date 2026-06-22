package com.example.cryptobotmonitor.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cryptobotmonitor.data.model.PriceAlert

object NotificationHelper {

    private const val CHANNEL_ID = "price_alerts_channel"

    fun showPriceAlert(
        context: Context,
        alert: PriceAlert,
        currentPrice: Double
    ) {
        createChannel(context)

        // Sprawdzenie pozwolenia na powiadomienia
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val conditionText =
            if (alert.condition == "ABOVE") {
                "powyżej"
            } else {
                "poniżej"
            }

        // Treść powiadomienia
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Alert cenowy: ${alert.coinName}")
            .setContentText(
                "Cena jest $conditionText ${alert.targetPrice} USD. Aktualnie: ${
                    String.format("%.2f", currentPrice)
                } USD"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            alert.id.hashCode(),
            notification
        )
    }

    private fun createChannel(context: Context) {
        // Kanał jest potrzebny dla nowszych wersji Androida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alerty cenowe",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
