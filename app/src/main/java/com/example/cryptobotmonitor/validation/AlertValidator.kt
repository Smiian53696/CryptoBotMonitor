
package com.example.cryptobotmonitor.validation

object AlertValidator {

    // Maksymalna cena zgodna z regułami Firestore
    const val MAX_PRICE = 1_000_000_000_000.0

    // Zamiana tekstu na liczbę
    fun parsePrice(value: String): Double? {

        return value
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()
    }

    // Sprawdzenie poprawności ceny
    fun isValidPrice(price: Double?): Boolean {

        return price != null &&
                price.isFinite() &&
                price > 0 &&
                price <= MAX_PRICE
    }

    // Sprawdzenie warunku alertu
    fun isValidCondition(condition: String): Boolean {

        return condition == "ABOVE" ||
                condition == "BELOW"
    }

    // Sprawdzenie danych kryptowaluty
    fun isValidCoin(
        coinId: String,
        coinName: String
    ): Boolean {

        return coinId.isNotBlank() &&
                coinName.isNotBlank() &&
                coinId.trim().length <= 200 &&
                coinName.trim().length <= 200
    }
}
