
package com.example.cryptobotmonitor.presentation.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.repository.CoinRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Punkt na wykresie
data class ChartPoint(
    val time: Long,
    val price: Double
)

// Stan ekranu szczegółów
data class DetailsUiState(
    val coin: CoinModel? = null,
    val chartPoints: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // Dane wykresu
    val selectedPeriod: String = "7",
    val isChartLoading: Boolean = false,
    val chartError: String? = null
)

class DetailsViewModel : ViewModel() {

    private val coinRepository = CoinRepository()

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState

    private var currentCoinId = ""

    private var coinJob: Job? = null
    private var chartJob: Job? = null

    // Pobieranie danych kryptowaluty
    fun loadCoin(coinId: String) {

        coinJob?.cancel()
        chartJob?.cancel()

        currentCoinId = coinId

        if (coinId.isBlank()) {
            _uiState.value = DetailsUiState(
                error = "Brak id kryptowaluty."
            )
            return
        }

        _uiState.update {
            it.copy(
                coin = null,
                chartPoints = emptyList(),
                isLoading = true,
                isChartLoading = false,
                error = null,
                chartError = null
            )
        }

        coinJob = viewModelScope.launch {

            try {

                val coin = coinRepository
                    .getCoinsByIds(coinId)
                    .firstOrNull()

                if (coin == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Nie znaleziono kryptowaluty."
                        )
                    }
                    return@launch
                }

                // Wyświetlenie podstawowych danych
                _uiState.update {
                    it.copy(
                        coin = coin,
                        isLoading = false,
                        error = null
                    )
                }

                // Pobranie wykresu
                loadChart(_uiState.value.selectedPeriod)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                Log.e(
                    "DetailsViewModel",
                    "Błąd pobierania danych monety",
                    e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Nie udało się pobrać danych kryptowaluty."
                    )
                }
            }
        }
    }

    // Zmiana okresu wykresu
    fun changePeriod(period: String) {

        if (period !in listOf("1", "7", "30", "90")) {
            return
        }

        if (currentCoinId.isBlank()) return

        if (_uiState.value.coin == null) return

        if (period == _uiState.value.selectedPeriod) {
            return
        }

        loadChart(period)
    }

    // Pobieranie danych historycznych
    private fun loadChart(period: String) {

        chartJob?.cancel()

        val coinId = currentCoinId

        if (coinId.isBlank()) return

        _uiState.update {
            it.copy(
                selectedPeriod = period,
                chartPoints = emptyList(),
                isChartLoading = true,
                chartError = null
            )
        }

        chartJob = viewModelScope.launch {

            try {

                val chart = coinRepository.getMarketChart(
                    coinId = coinId,
                    days = period
                )

                // Zamiana danych API na punkty wykresu
                val points = chart.prices.mapNotNull { item ->

                    val time = item.getOrNull(0)?.toLong()
                    val price = item.getOrNull(1)

                    if (time != null &&
                        price != null &&
                        price.isFinite()
                    ) {
                        ChartPoint(
                            time = time,
                            price = price
                        )
                    } else {
                        null
                    }

                }.sortedBy { it.time }

                _uiState.update {
                    it.copy(
                        chartPoints = points,
                        isChartLoading = false,
                        chartError = if (points.size < 2) {
                            "Brak danych do wykresu."
                        } else {
                            null
                        }
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                Log.e(
                    "DetailsViewModel",
                    "Błąd pobierania wykresu",
                    e
                )

                _uiState.update {
                    it.copy(
                        isChartLoading = false,
                        chartError = "Nie udało się pobrać wykresu."
                    )
                }
            }
        }
    }

    // Ponowne pobranie wykresu
    fun retryChart() {
        if (!_uiState.value.isChartLoading &&
            _uiState.value.coin != null
        ) {
            loadChart(_uiState.value.selectedPeriod)
        }
    }
}
