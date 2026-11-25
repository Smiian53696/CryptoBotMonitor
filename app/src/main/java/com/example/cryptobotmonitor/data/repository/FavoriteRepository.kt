
package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FavoriteRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Kolekcja ulubionych kryptowalut
    private val favoritesCollection = firestore.collection("favorites")

    // Dodawanie kryptowaluty do ulubionych
    suspend fun addFavorite(
        coinId: String,
        coinName: String
    ) {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("Użytkownik nie jest zalogowany.")

        val documentId = "${userId}_${coinId}"

        val favorite = FavoriteCoin(
            id = documentId,
            userId = userId,
            coinId = coinId,
            coinName = coinName
        )

        favoritesCollection
            .document(documentId)
            .set(favorite)
            .await()
    }

    // Usuwanie kryptowaluty z ulubionych
    suspend fun removeFavorite(coinId: String) {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("Użytkownik nie jest zalogowany.")

        val documentId = "${userId}_${coinId}"

        favoritesCollection
            .document(documentId)
            .delete()
            .await()
    }

    // Pobieranie ulubionych kryptowalut użytkownika
    fun observeUserFavorites(
        onResult: (List<FavoriteCoin>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {

        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError("Użytkownik nie jest zalogowany.")
            return null
        }

        return favoritesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(
                        error.localizedMessage
                            ?: "Nie udało się pobrać ulubionych."
                    )
                    return@addSnapshotListener
                }

                val favorites = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(FavoriteCoin::class.java)
                    } ?: emptyList()

                onResult(favorites)
            }
    }
}
