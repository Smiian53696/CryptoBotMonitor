package com.example.cryptobotmonitor.presentation.alerts

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
    val message: String? = null
)

class AlertsViewModel : ViewModel() {

    private val repository = AlertRepository()

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    init {
        loadAlerts()
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

        // Zamiana przecinka na kropkę, żeby działało 12,5 i 12.5
        val price = state.targetPrice.replace(",", ".").toDoubleOrNull()

        if (state.coinId.isBlank() || state.coinName.isBlank()) {
            _uiState.update {
                it.copy(error = "Wpisz id oraz nazwę kryptowaluty.")
            }
            return
        }

        if (price == null || price <= 0) {
            _uiState.update {
                it.copy(error = "Wpisz poprawną cenę.")
            }
            return
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
                        targetPrice = "",
                        condition = "ABOVE",
                        error = null,
                        message = "Alert został dodany."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(error = message)
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

