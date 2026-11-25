
package com.example.cryptobotmonitor.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // Pobranie aktualnego użytkownika
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Sprawdzenie stanu zalogowania
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Rejestracja użytkownika
    fun register(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {

                onResult(true, null)

            } else {

                onResult(
                    false,
                    getErrorMessage(task.exception)
                )
            }
        }
    }

    // Logowanie użytkownika
    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {

                onResult(true, null)

            } else {

                onResult(
                    false,
                    getErrorMessage(task.exception)
                )
            }
        }
    }

    // Wylogowanie użytkownika
    fun logout() {
        auth.signOut()
    }

    // Obsługa błędów Firebase
    private fun getErrorMessage(
        exception: Exception?
    ): String {

        return when (exception) {

            is FirebaseNetworkException ->
                "Brak połączenia z internetem. Spróbuj ponownie."

            is FirebaseTooManyRequestsException ->
                "Zbyt wiele prób. Spróbuj ponownie później."

            is FirebaseAuthException -> {

                when (exception.errorCode) {

                    "ERROR_INVALID_EMAIL" ->
                        "Podaj poprawny adres email."

                    "ERROR_WRONG_PASSWORD",
                    "ERROR_USER_NOT_FOUND",
                    "ERROR_INVALID_CREDENTIAL",
                    "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                        "Nieprawidłowy email lub hasło."

                    "ERROR_EMAIL_ALREADY_IN_USE" ->
                        "Konto z tym adresem email już istnieje."

                    "ERROR_WEAK_PASSWORD" ->
                        "Hasło nie spełnia wymagań bezpieczeństwa."

                    "ERROR_USER_DISABLED" ->
                        "To konto zostało zablokowane."

                    "ERROR_TOO_MANY_REQUESTS" ->
                        "Zbyt wiele prób. Spróbuj ponownie później."

                    else ->
                        "Nie udało się wykonać operacji. Spróbuj ponownie."
                }
            }

            else ->
                "Wystąpił nieoczekiwany błąd. Spróbuj ponownie."
        }
    }
}
