
package com.example.cryptobotmonitor.domain

object AlertCondition {

    // Sprawdzenie poprawności aktualnej ceny
    fun isValidCurrentPrice(price: Double?): Boolean {
        return price != null &&
                price.isFinite() &&
                price > 0
    }

    // Sprawdzenie poprawności ceny docelowej
    fun isValidTargetPrice(price: Double): Boolean {
        return price.isFinite() &&
                price > 0
    }

    // Sprawdzenie warunku wykonania alertu
    fun shouldTrigger(
        currentPrice: Double?,
        targetPrice: Double,
        condition: String
    ): Boolean {

        if (currentPrice == null ||
            !isValidCurrentPrice(currentPrice)
        ) {
            return false
        }

        if (!isValidTargetPrice(targetPrice)) {
            return false
        }

        return when (condition) {
            "ABOVE" -> currentPrice >= targetPrice
            "BELOW" -> currentPrice <= targetPrice
            else -> false
        }
    }
}
