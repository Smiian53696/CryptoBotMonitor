
package com.example.cryptobotmonitor.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.repository.AlertRepository
import com.example.cryptobotmonitor.data.repository.FavoriteRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.cryptobotmonitor.data.auth.CurrentUserProvider
import com.example.cryptobotmonitor.data.auth.FirebaseCurrentUserProvider
import com.example.cryptobotmonitor.data.repository.FavoriteRepositoryInterface
import com.example.cryptobotmonitor.data.repository.AlertRepositoryInterface
// Stan ekranu profilu
data class ProfileUiState(
    val email: String = "",
    val favoritesCount: Int = 0,
    val activeAlertsCount: Int = 0,
    val completedAlertsCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel(
    private val userProvider: CurrentUserProvider =
        FirebaseCurrentUserProvider(),

    private val favoriteRepository: FavoriteRepositoryInterface =
        FavoriteRepository(),

    private val alertRepository: AlertRepositoryInterface =
        AlertRepository()

) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadProfile()
    }

    // Pobieranie danych zalogowanego użytkownika
    fun loadProfile() {

        loadJob?.cancel()

        val user = userProvider.getCurrentUser()

        if (user == null) {

            _uiState.value = ProfileUiState(
                isLoading = false,
                error = "Użytkownik nie jest zalogowany."
            )

            return
        }

        _uiState.update {
            it.copy(
                email = user.email ?: "",
                isLoading = true,
                error = null
            )
        }

        loadJob = viewModelScope.launch {

            try {

                // Pobranie ulubionych i alertów z Firestore
                val favorites =
                    favoriteRepository.getUserFavorites()

                val alerts =
                    alertRepository.getUserAlertsOnce()

                if (userProvider.getCurrentUser()?.uid != user.uid) {

                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        error = "Użytkownik został zmieniony."
                    )

                    return@launch
                }

                // Aktualizacja statystyk użytkownika
                _uiState.update {
                    it.copy(
                        favoritesCount = favorites.size,

                        activeAlertsCount = alerts.count {
                            it.active
                        },

                        completedAlertsCount = alerts.count {
                            !it.active
                        },

                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Nie udało się pobrać danych profilu."
                    )
                }
            }
        }
    }
}
