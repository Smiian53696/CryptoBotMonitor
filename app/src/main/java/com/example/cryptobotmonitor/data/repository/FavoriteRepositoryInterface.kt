
package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.google.firebase.firestore.ListenerRegistration

// Interfejs repozytorium ulubionych kryptowalut
interface FavoriteRepositoryInterface {

    suspend fun addFavorite(
        coinId: String,
        coinName: String
    )

    suspend fun removeFavorite(
        coinId: String
    )

    fun observeUserFavorites(
        onResult: (List<FavoriteCoin>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    suspend fun getUserFavorites(): List<FavoriteCoin>
}
