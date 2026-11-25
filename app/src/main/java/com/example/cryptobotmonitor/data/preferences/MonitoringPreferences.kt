
package com.example.cryptobotmonitor.data.preferences

import android.content.Context

data class MonitoringStatus(
    val lastCheckTime: Long = 0L,
    val completedAlerts: Int = 0
)

class MonitoringPreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "monitoring_preferences",
            Context.MODE_PRIVATE
        )

    // Zapis ostatniego sprawdzenia alertów
    fun saveLastCheck(
        userId: String,
        completedAlerts: Int
    ) {
        preferences.edit()
            .putLong(
                "${userId}_last_check_time",
                System.currentTimeMillis()
            )
            .putInt(
                "${userId}_completed_alerts",
                completedAlerts
            )
            .apply()
    }

    // Pobranie informacji o monitorowaniu
    fun getMonitoringStatus(userId: String): MonitoringStatus {

        return MonitoringStatus(
            lastCheckTime = preferences.getLong(
                "${userId}_last_check_time",
                0L
            ),
            completedAlerts = preferences.getInt(
                "${userId}_completed_alerts",
                0
            )
        )
    }
}
