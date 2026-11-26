
package com.example.cryptobotmonitor.data.auth

import com.google.firebase.auth.FirebaseAuth

// Dane zalogowanego użytkownika
data class CurrentUser(
    val uid: String,
    val email: String?
)

// Interfejs pobierania aktualnego użytkownika
interface CurrentUserProvider {

    fun getCurrentUser(): CurrentUser?
}

// Pobieranie użytkownika z Firebase
class FirebaseCurrentUserProvider(
    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()
) : CurrentUserProvider {

    override fun getCurrentUser(): CurrentUser? {

        val user = auth.currentUser ?: return null

        return CurrentUser(
            uid = user.uid,
            email = user.email
        )
    }
}
