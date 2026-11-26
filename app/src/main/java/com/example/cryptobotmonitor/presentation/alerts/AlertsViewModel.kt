package com.example.cryptobotmonitor.presentation.alerts

import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.repository.CoinRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.data.repository.AlertRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


data class AlertsUiState(
    val alerts: List<PriceAlert> = emptyList(),
    val coinId: String = "",
    val coinName: String = "",
    val targetPrice: String = "",
    val condition: String = "ABOVE",
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,

    // Dane do wyboru kryptowaluty
    val availableCoins: List<CoinModel> = emptyList(),
    val coinSearchQuery: String = "",
    val isCoinsLoading: Boolean = false,
    val coinsError: String? = null,

    // Filtrowanie alertów
    val showCompletedAlerts: Boolean = false,

    // Informacja o zapisywaniu alertu
    val isSaving: Boolean = false
)


class AlertsViewModel : ViewModel() {

    private val repository = AlertRepository()
    private val coinRepository = CoinRepository()

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    init {
        loadAlerts()
        loadAvailableCoins()
    }

    private fun loadAlerts() {
        // Pobranie alertów użytkownika
        listenerRegistration = repository.observeUserAlerts(
            onResult = { alerts ->
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error
                    )
                }
            }
        )
    }

    // Pobieranie kryptowalut do formularza alertu
    fun loadAvailableCoins() {

        if (_uiState.value.isCoinsLoading) return

        _uiState.update {
            it.copy(
                isCoinsLoading = true,
                coinsError = null
            )
        }

        viewModelScope.launch {
            try {
                val coins = coinRepository.getCoins()

                _uiState.update {
                    it.copy(
                        availableCoins = coins,
                        isCoinsLoading = false
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCoinsLoading = false,
                        coinsError = "Nie udało się pobrać kryptowalut."
                    )
                }
            }
        }
    }

    // Wybór kryptowaluty z listy
    fun selectCoin(coin: CoinModel) {
        _uiState.update {
            it.copy(
                coinId = coin.id,
                coinName = coin.name,
                coinSearchQuery = "",
                error = null,
                message = null
            )
        }
    }

    // Wyszukiwanie kryptowaluty
    fun onCoinSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(coinSearchQuery = query)
        }
    }

    // Zmiana widoku alertów
    fun setShowCompletedAlerts(show: Boolean) {
        _uiState.update {
            it.copy(showCompletedAlerts = show)
        }
    }

    fun onCoinIdChange(value: String) {
        _uiState.update {
            it.copy(coinId = value, error = null, message = null)
        }
    }

    fun onCoinNameChange(value: String) {
        _uiState.update {
            it.copy(coinName = value, error = null, message = null)
        }
    }

    fun onTargetPriceChange(value: String) {
        _uiState.update {
            it.copy(targetPrice = value, error = null, message = null)
        }
    }

    fun onConditionChange(value: String) {
        _uiState.update {
            it.copy(condition = value, error = null, message = null)
        }
    }

    fun setInitialCoinData(
        coinId: String,
        coinName: String
    ) {
        _uiState.update {
            it.copy(
                coinId = coinId,
                coinName = coinName
            )
        }
    }


    fun addAlert() {
        val state = _uiState.value

        // Blokada ponownego zapisu
        if (state.isSaving) return

        // Zamiana przecinka na kropkę
        val price = state.targetPrice
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        // Sprawdzenie kryptowaluty
        if (state.coinId.isBlank() || state.coinName.isBlank()) {
            _uiState.update {
                it.copy(
                    error = "Wybierz kryptowalutę.",
                    message = null
                )
            }
            return
        }


        // Sprawdzenie poprawności i zakresu ceny
        if (
            price == null ||
            !price.isFinite() ||
            price <= 0 ||
            price > 1_000_000_000_000.0
        ) {
            _uiState.update {
                it.copy(
                    error = "Cena musi być większa od zera i nie może przekraczać 1 biliona USD.",
                    message = null
                )
            }
            return
        }

        // Sprawdzenie warunku alertu
        if (state.condition != "ABOVE" &&
            state.condition != "BELOW"
        ) {
            _uiState.update {
                it.copy(error = "Wybierz poprawny warunek alertu.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                error = null,
                message = null
            )
        }

        // Dodanie alertu do Firestore
        repository.addAlert(
            coinId = state.coinId.trim().lowercase(),
            coinName = state.coinName.trim(),
            targetPrice = price,
            condition = state.condition
        ) { success, message ->

            if (success) {
                _uiState.update {
                    it.copy(
                        coinId = "",
                        coinName = "",
                        coinSearchQuery = "",
                        targetPrice = "",
                        condition = "ABOVE",
                        isSaving = false,
                        error = null,
                        message = "Alert został dodany."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = message ?: "Nie udało się dodać alertu."
                    )
                }
            }
        }
    }


    fun deleteAlert(alertId: String) {
        // Usunięcie alertu
        repository.deleteAlert(alertId) { success, message ->
            if (!success) {
                _uiState.update {
                    it.copy(error = message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Zatrzymanie pobierania po zamknięciu ekranu
        listenerRegistration?.remove()
    }
}

