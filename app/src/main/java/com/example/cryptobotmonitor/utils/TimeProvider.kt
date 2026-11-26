package com.example.cryptobotmonitor.utils

import android.os.SystemClock

// Interfejs pobierania aktualnego czasu
interface TimeProvider {

    fun now(): Long
}

// Pobieranie czasu systemowego
class SystemTimeProvider : TimeProvider {

    override fun now(): Long {

        return SystemClock.elapsedRealtime()
    }
}