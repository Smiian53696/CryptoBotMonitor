
package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.data.auth.AuthRepositoryInterface

// Repozytorium używane podczas testów
class FakeAuthRepository : AuthRepositoryInterface {

    var loginCalls = 0
        private set

    var registerCalls = 0
        private set

    var lastEmail = ""
        private set

    var lastPassword = ""
        private set

    private var pendingResult:
            ((Boolean, String?) -> Unit)? = null

    // Symulacja logowania
    override fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        loginCalls++

        lastEmail = email
        lastPassword = password

        pendingResult = onResult
    }

    // Symulacja rejestracji
    override fun register(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        registerCalls++

        lastEmail = email
        lastPassword = password

        pendingResult = onResult
    }

    // Zakończenie operacji z sukcesem
    fun completeSuccess() {

        completeResult(true, null)
    }

    // Zakończenie operacji z błędem
    fun completeError(message: String?) {

        completeResult(false, message)
    }

    // Przekazanie wyniku do ViewModel
    private fun completeResult(
        success: Boolean,
        message: String?
    ) {

        val callback = checkNotNull(pendingResult) {
            "Brak oczekującej operacji."
        }

        pendingResult = null

        callback(success, message)
    }
}
