
package com.example.cryptobotmonitor.domain

import com.example.cryptobotmonitor.data.model.CoinModel

object CoinFilter {

    // Filtrowanie kryptowalut według nazwy, symbolu i ulubionych
    fun filterCoins(
        coins: List<CoinModel>,
        favoriteIds: Set<String>,
        onlyFavorites: Boolean,
        query: String
    ): List<CoinModel> {

        val searchText = query.trim()

        return coins.filter { coin ->

            val matchesFavorites =
                !onlyFavorites || coin.id in favoriteIds

            val matchesSearch =
                searchText.isBlank() ||
                        coin.name.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        coin.symbol.contains(
                            searchText,
                            ignoreCase = true
                        )

            matchesFavorites && matchesSearch
        }
    }
}
