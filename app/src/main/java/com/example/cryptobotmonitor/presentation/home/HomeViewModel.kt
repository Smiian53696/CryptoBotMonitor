package com.example.cryptobotmonitor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.repository.CoinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel przechowuje dane dla ekranu głównego
class HomeViewModel : ViewModel() {

    private val repository = CoinRepository()

    // Zadanie odpowiedzialne za automatyczne odświeżanie danych
    private var refreshJob: Job? = null

    // Lista kryptowalut widoczna na ekranie
    private val _coins = MutableStateFlow<List<CoinModel>>(emptyList())
    val coins: StateFlow<List<CoinModel>> = _coins

    // Informacja czy dane są aktualnie ładowane
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Informacja o błędzie
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        startAutoRefresh()
    }

    // Uruchomienie automatycznego odświeżania cen
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

            } catch (e: Exception) {
                _error.value = "Nie udało się pobrać danych: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Zatrzymanie odświeżania po zamknięciu ekranu
        refreshJob?.cancel()
    }
}