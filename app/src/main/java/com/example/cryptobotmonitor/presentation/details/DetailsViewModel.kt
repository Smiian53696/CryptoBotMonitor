package com.example.cryptobotmonitor.presentation.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.repository.CoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChartPoint(
    val time: Long,
    val price: Double
)

data class DetailsUiState(
    val coin: CoinModel? = null,
    val chartPoints: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DetailsViewModel : ViewModel() {

    private val coinRepository = CoinRepository()

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState

    fun loadCoin(coinId: String) {
        if (coinId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Brak id kryptowaluty."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    coin = null,
                    chartPoints = emptyList(),
                    isLoading = true,
                    error = null
                )
            }

            try {
                // Najpierw pobieramy podstawowe dane kryptowaluty
                val coin = coinRepository.getCoinsByIds(coinId).firstOrNull()

                if (coin == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Nie znaleziono kryptowaluty."
                        )
                    }
                    return@launch
                }

                // Od razu pokazujemy dane monety
                _uiState.update {
                    it.copy(
                        coin = coin,
                        isLoading = false,
                        error = null
                    )
                }

                // Potem próbujemy pobrać wykres
                try {
                    val chart = coinRepository.getMarketChart(
                        coinId = coinId,
                        days = "7"
                    )

                    val points = chart.prices.mapNotNull { item ->
                        val time = item.getOrNull(0)?.toLong()
                        val price = item.getOrNull(1)

                        if (time != null && price != null) {
                            ChartPoint(
                                time = time,
                                price = price
                            )
                        } else {
                            null
                        }
                    }

                    _uiState.update {
                        it.copy(
                            chartPoints = points
                        )
                    }

                } catch (e: Exception) {
                    // Jeśli wykres się nie pobierze, ekran nadal działa
                    Log.e("DetailsViewModel", "Błąd pobierania wykresu", e)

                    _uiState.update {
                        it.copy(
                            chartPoints = emptyList()
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("DetailsViewModel", "Błąd pobierania danych monety", e)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Nie udało się pobrać danych kryptowaluty."
                    )
                }
            }
        }
    }
}