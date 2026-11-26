
package com.example.cryptobotmonitor.validation

object AuthValidator {

    // Minimalna długość hasła podczas rejestracji
    const val MIN_PASSWORD_LENGTH = 6

    // Wzór do sprawdzania adresu email
    private val emailRegex = Regex(
        "^[a-zA-Z0-9_+%'-]+(?:\\.[a-zA-Z0-9_+%'-]+)*@" +
                "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+" +
                "[a-zA-Z]{2,63}$"
    )

    // Usuwanie zbędnych spacji z adresu email
    fun normalizeEmail(email: String): String {
        return email.trim()
    }

    // Sprawdzenie poprawności adresu email
    fun isValidEmail(email: String): Boolean {
        return emailRegex.matches(
            normalizeEmail(email)
        )
    }

    // Sprawdzenie danych logowania i rejestracji
    fun validateData(
        email: String,
        password: String,
        isRegister: Boolean
    ): String? {

        return when {

            email.isBlank() || password.isBlank() ->
                "Wpisz email i hasło."

            !isValidEmail(email) ->
                "Podaj poprawny adres email."

            isRegister &&
                    password.length < MIN_PASSWORD_LENGTH ->
                "Hasło musi mieć minimum 6 znaków."

            else -> null
        }
    }
}
