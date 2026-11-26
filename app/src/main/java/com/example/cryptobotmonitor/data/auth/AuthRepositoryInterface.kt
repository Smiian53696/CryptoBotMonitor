package com.example.cryptobotmonitor.data.auth

// Interfejs repozytorium autoryzacji
interface AuthRepositoryInterface {

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    )

    fun register(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    )
}