
package com.example.cryptobotmonitor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.repository.CoinRepository
import com.example.cryptobotmonitor.data.repository.FavoriteRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel przechowuje dane dla ekranu głównego
class HomeViewModel : ViewModel() {

    private val repository = CoinRepository()
    private val favoriteRepository = FavoriteRepository()

    // Zadanie odpowiedzialne za odświeżanie danych
    private var refreshJob: Job? = null

    // Obserwowanie zmian w Firestore
    private var favoritesListener: ListenerRegistration? = null

    // Lista kryptowalut
    private val _coins = MutableStateFlow<List<CoinModel>>(emptyList())
    val coins: StateFlow<List<CoinModel>> = _coins

    // Informacja o ładowaniu danych
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Informacja o błędzie
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Id ulubionych kryptowalut
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    // Wyświetlanie tylko ulubionych
    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    // Informacja o błędzie ulubionych
    private val _favoriteError = MutableStateFlow<String?>(null)
    val favoriteError: StateFlow<String?> = _favoriteError

    // Kryptowaluty, dla których trwa zapis
    private val _pendingFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingFavoriteIds: StateFlow<Set<String>> = _pendingFavoriteIds

    init {
        startAutoRefresh()
        observeFavorites()
    }

    // Automatyczne odświeżanie cen
    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                loadCoins()
                delay(60_000)
            }
        }
    }

    // Pobieranie danych z API
    fun loadCoins() {
        viewModelScope.launch {
            try {
                _isLoading.value = _coins.value.isEmpty()
                _error.value = null

                val result = repository.getCoins()
                _coins.value = result

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Nie udało się pobrać danych: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Pobieranie ulubionych z Firestore
    private fun observeFavorites() {
        favoritesListener?.remove()

        favoritesListener = favoriteRepository.observeUserFavorites(
            onResult = { favorites ->
                _favoriteIds.value = favorites
                    .map { it.coinId }
                    .toSet()

                _favoriteError.value = null
            },
            onError = { message ->
                _favoriteError.value = message
            }
        )
    }

    // Dodawanie lub usuwanie z ulubionych
    fun toggleFavorite(coin: CoinModel) {

        if (coin.id in _pendingFavoriteIds.value) {
            return
        }

        _pendingFavoriteIds.value =
            _pendingFavoriteIds.value + coin.id

        viewModelScope.launch {
            try {
                _favoriteError.value = null

                if (coin.id in _favoriteIds.value) {
                    favoriteRepository.removeFavorite(coin.id)
                } else {
                    favoriteRepository.addFavorite(
                        coinId = coin.id,
                        coinName = coin.name
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _favoriteError.value =
                    "Nie udało się zmienić ulubionych: ${e.message}"
            } finally {
                _pendingFavoriteIds.value =
                    _pendingFavoriteIds.value - coin.id
            }
        }
    }

    // Zmiana filtra kryptowalut
    fun setShowOnlyFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
    }

    override fun onCleared() {

        // Zatrzymanie odświeżania cen
        refreshJob?.cancel()

        // Zatrzymanie obserwowania Firestore
        favoritesListener?.remove()

        super.onCleared()
    }
}
