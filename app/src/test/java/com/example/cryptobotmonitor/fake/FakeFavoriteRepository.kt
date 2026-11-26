
package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.example.cryptobotmonitor.data.repository.FavoriteRepositoryInterface
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay

// Repozytorium ulubionych do testów
class FakeFavoriteRepository : FavoriteRepositoryInterface {

    var favorites: List<FavoriteCoin> = emptyList()
        private set

    var error: Exception? = null

    var responseDelay = 0L

    var addCalls = 0
        private set

    var removeCalls = 0
        private set

    var observeCalls = 0
        private set

    var getFavoritesCalls = 0
        private set

    var listenerRemoveCalls = 0
        private set

    var lastCoinId = ""
        private set

    var lastCoinName = ""
        private set

    private var resultListener:
            ((List<FavoriteCoin>) -> Unit)? = null

    private var errorListener:
            ((String) -> Unit)? = null

    // Symulacja dodawania do ulubionych
    override suspend fun addFavorite(
        coinId: String,
        coinName: String
    ) {

        addCalls++

        lastCoinId = coinId
        lastCoinName = coinName

        simulateResponse()

        val favorite = FavoriteCoin(
            id = "test-user_$coinId",
            userId = "test-user",
            coinId = coinId,
            coinName = coinName
        )

        favorites = favorites
            .filterNot { it.coinId == coinId } + favorite

        resultListener?.invoke(favorites)
    }

    // Symulacja usuwania z ulubionych
    override suspend fun removeFavorite(
        coinId: String
    ) {

        removeCalls++

        lastCoinId = coinId

        simulateResponse()

        favorites = favorites.filterNot {
            it.coinId == coinId
        }

        resultListener?.invoke(favorites)
    }

    // Symulacja obserwowania zmian w Firestore
    override fun observeUserFavorites(
        onResult: (List<FavoriteCoin>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        observeCalls++

        resultListener = onResult
        errorListener = onError

        onResult(favorites)

        return object : ListenerRegistration {

            override fun remove() {

                listenerRemoveCalls++

                resultListener = null
                errorListener = null
            }
        }
    }

    // Pobranie zapisanej listy
    override suspend fun getUserFavorites():
            List<FavoriteCoin> {

        getFavoritesCalls++

        simulateResponse()

        return favorites
    }

    // Symulacja zmiany danych w Firestore
    fun emitFavorites(
        newFavorites: List<FavoriteCoin>
    ) {

        favorites = newFavorites

        resultListener?.invoke(favorites)
    }

    // Symulacja błędu obserwowania danych
    fun emitError(message: String) {

        errorListener?.invoke(message)
    }

    // Symulacja opóźnienia i błędu zapisu
    private suspend fun simulateResponse() {

        if (responseDelay > 0) {
            delay(responseDelay)
        }

        error?.let {
            throw it
        }
    }
}
