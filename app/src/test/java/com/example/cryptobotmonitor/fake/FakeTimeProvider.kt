package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.utils.TimeProvider

// Czas używany podczas testów
class FakeTimeProvider(
    private var currentTime: Long = 1000000L
) : TimeProvider {

    override fun now(): Long {
        return currentTime
    }

    // Przesunięcie czasu
    fun advanceBy(milliseconds: Long) {

        require(milliseconds >= 0)

        currentTime += milliseconds
    }
}