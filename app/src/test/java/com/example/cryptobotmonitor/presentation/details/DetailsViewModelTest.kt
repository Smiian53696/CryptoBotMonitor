
package com.example.cryptobotmonitor.presentation.details

import androidx.lifecycle.ViewModelStore
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.MarketChartModel
import com.example.cryptobotmonitor.fake.FakeCoinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    private lateinit var repository: FakeCoinRepository

    private lateinit var viewModel: DetailsViewModel

    private lateinit var viewModelStore: ViewModelStore

    // Przykładowe kryptowaluty
    private val bitcoin = CoinModel(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        currentPrice = 60000.0,
        image = ""
    )

    private val ethereum = CoinModel(
        id = "ethereum",
        symbol = "eth",
        name = "Ethereum",
        currentPrice = 3000.0,
        image = ""
    )

    // Przykładowe dane historyczne
    private val chart = MarketChartModel(
        prices = listOf(
            listOf(1000.0, 58000.0),
            listOf(2000.0, 59000.0),
            listOf(3000.0, 60000.0)
        )
    )

    private val expectedPoints = listOf(
        ChartPoint(1000L, 58000.0),
        ChartPoint(2000L, 59000.0),
        ChartPoint(3000L, 60000.0)
    )

    @Before
    fun setUp() {

        dispatcher = StandardTestDispatcher()

        Dispatchers.setMain(dispatcher)

        repository = FakeCoinRepository()

        repository.coins = listOf(bitcoin, ethereum)

        repository.marketChart = chart

        viewModelStore = ViewModelStore()

        viewModel = DetailsViewModel(
            coinRepository = repository
        )

        viewModelStore.put("details", viewModel)
    }

    // Uruchamianie testu i zatrzymanie ViewModel
    private fun runViewModelTest(
        testBody: suspend TestScope.() -> Unit
    ) = runTest(
        context = dispatcher,
        timeout = 5.seconds
    ) {

        try {

            testBody()

        } finally {

            viewModelStore.clear()

            runCurrent()
        }
    }

    @After
    fun tearDown() {

        viewModelStore.clear()

        dispatcher.scheduler.runCurrent()

        Dispatchers.resetMain()
    }

    // Test stanu początkowego

    @Test
    fun initialState_defaultValues() =
        runViewModelTest {

            assertEquals(
                DetailsUiState(),
                viewModel.uiState.value
            )

            assertEquals(0, repository.getCoinsByIdsCalls)

            assertEquals(0, repository.getMarketChartCalls)
        }

    // Testy pobierania kryptowalut

    @Test
    fun loadCoin_success() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        val state = viewModel.uiState.value

        assertEquals(bitcoin, state.coin)

        assertFalse(state.isLoading)

        assertNull(state.error)

        assertEquals(
            "bitcoin",
            repository.lastCoinIds
        )
    }

    @Test
    fun loadCoin_loadingState() = runViewModelTest {

        repository.responseDelay = 1000L

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)

        assertNull(viewModel.uiState.value.coin)

        advanceTimeBy(1000L)

        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(
            bitcoin,
            viewModel.uiState.value.coin
        )
    }

    @Test
    fun loadCoin_blankId() = runViewModelTest {

        viewModel.loadCoin(" ")

        runCurrent()

        assertEquals(
            "Brak id kryptowaluty.",
            viewModel.uiState.value.error
        )

        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(0, repository.getCoinsByIdsCalls)

        assertEquals(0, repository.getMarketChartCalls)
    }

    @Test
    fun loadCoin_notFound() = runViewModelTest {

        viewModel.loadCoin("unknown-coin")

        runCurrent()

        assertEquals(
            "Nie znaleziono kryptowaluty.",
            viewModel.uiState.value.error
        )

        assertNull(viewModel.uiState.value.coin)

        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(0, repository.getMarketChartCalls)
    }

    @Test
    fun loadCoin_apiError() = runViewModelTest {

        repository.error = Exception("Błąd API")

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertEquals(
            "Nie udało się pobrać danych kryptowaluty.",
            viewModel.uiState.value.error
        )

        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(0, repository.getMarketChartCalls)
    }

    @Test
    fun loadCoin_retryAfterError() =
        runViewModelTest {

            repository.error = Exception("Błąd API")

            viewModel.loadCoin("bitcoin")

            runCurrent()

            assertNotNull(viewModel.uiState.value.error)

            repository.error = null

            viewModel.loadCoin("bitcoin")

            runCurrent()

            assertNull(viewModel.uiState.value.error)

            assertEquals(
                bitcoin,
                viewModel.uiState.value.coin
            )

            assertEquals(2, repository.getCoinsByIdsCalls)
        }

    // Testy pobierania wykresu

    @Test
    fun loadChart_defaultPeriod() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertEquals(
            "7",
            viewModel.uiState.value.selectedPeriod
        )

        assertEquals("7", repository.lastChartDays)

        assertEquals(
            "bitcoin",
            repository.lastChartCoinId
        )

        assertEquals(1, repository.getMarketChartCalls)
    }

    @Test
    fun loadChart_success() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertEquals(
            expectedPoints,
            viewModel.uiState.value.chartPoints
        )

        assertFalse(viewModel.uiState.value.isChartLoading)

        assertNull(viewModel.uiState.value.chartError)
    }

    @Test
    fun loadChart_loadingState() = runViewModelTest {

        repository.responseDelay = 1000L

        viewModel.loadCoin("bitcoin")

        runCurrent()

        advanceTimeBy(1000L)

        runCurrent()

        assertEquals(
            bitcoin,
            viewModel.uiState.value.coin
        )

        assertTrue(viewModel.uiState.value.isChartLoading)

        advanceTimeBy(1000L)

        runCurrent()

        assertFalse(viewModel.uiState.value.isChartLoading)

        assertEquals(
            expectedPoints,
            viewModel.uiState.value.chartPoints
        )
    }

    @Test
    fun loadChart_emptyResponse() = runViewModelTest {

        repository.marketChart = MarketChartModel(
            prices = emptyList()
        )

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertTrue(
            viewModel.uiState.value.chartPoints.isEmpty()
        )

        assertEquals(
            "Brak danych do wykresu.",
            viewModel.uiState.value.chartError
        )

        assertFalse(viewModel.uiState.value.isChartLoading)
    }

    @Test
    fun loadChart_singlePoint() = runViewModelTest {

        repository.marketChart = MarketChartModel(
            prices = listOf(
                listOf(1000.0, 60000.0)
            )
        )

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertEquals(
            1,
            viewModel.uiState.value.chartPoints.size
        )

        assertEquals(
            "Brak danych do wykresu.",
            viewModel.uiState.value.chartError
        )
    }

    @Test
    fun loadChart_sortsPoints() = runViewModelTest {

        repository.marketChart = MarketChartModel(
            prices = listOf(
                listOf(3000.0, 60000.0),
                listOf(1000.0, 58000.0),
                listOf(2000.0, 59000.0)
            )
        )

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertEquals(
            expectedPoints,
            viewModel.uiState.value.chartPoints
        )
    }

    @Test
    fun loadChart_apiError() = runViewModelTest {

        repository.chartError = Exception("Błąd wykresu")

        viewModel.loadCoin("bitcoin")

        runCurrent()

        val state = viewModel.uiState.value

        assertEquals(bitcoin, state.coin)

        assertNull(state.error)

        assertEquals(
            "Nie udało się pobrać wykresu.",
            state.chartError
        )

        assertFalse(state.isChartLoading)
    }

    // Testy zmiany okresu wykresu

    @Test
    fun changePeriod_oneDay() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        viewModel.changePeriod("1")

        runCurrent()

        assertEquals(
            "1",
            viewModel.uiState.value.selectedPeriod
        )

        assertEquals("1", repository.lastChartDays)

        assertEquals(2, repository.getMarketChartCalls)
    }

    @Test
    fun changePeriod_thirtyDays() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        viewModel.changePeriod("30")

        runCurrent()

        assertEquals(
            "30",
            viewModel.uiState.value.selectedPeriod
        )

        assertEquals("30", repository.lastChartDays)
    }

    @Test
    fun changePeriod_ninetyDays() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        viewModel.changePeriod("90")

        runCurrent()

        assertEquals(
            "90",
            viewModel.uiState.value.selectedPeriod
        )

        assertEquals("90", repository.lastChartDays)
    }

    @Test
    fun changePeriod_invalidPeriod() =
        runViewModelTest {

            viewModel.loadCoin("bitcoin")

            runCurrent()

            viewModel.changePeriod("365")

            runCurrent()

            assertEquals(
                "7",
                viewModel.uiState.value.selectedPeriod
            )

            assertEquals(1, repository.getMarketChartCalls)
        }

    @Test
    fun changePeriod_samePeriod() = runViewModelTest {

        viewModel.loadCoin("bitcoin")

        runCurrent()

        viewModel.changePeriod("7")

        runCurrent()

        assertEquals(1, repository.getMarketChartCalls)
    }

    @Test
    fun changePeriod_withoutCoin() =
        runViewModelTest {

            viewModel.changePeriod("30")

            runCurrent()

            assertEquals(
                "7",
                viewModel.uiState.value.selectedPeriod
            )

            assertEquals(0, repository.getMarketChartCalls)
        }

    // Testy ponownego pobierania wykresu

    @Test
    fun retryChart_afterError() = runViewModelTest {

        repository.chartError = Exception("Błąd API")

        viewModel.loadCoin("bitcoin")

        runCurrent()

        assertNotNull(
            viewModel.uiState.value.chartError
        )

        repository.chartError = null

        viewModel.retryChart()

        runCurrent()

        assertNull(viewModel.uiState.value.chartError)

        assertEquals(
            expectedPoints,
            viewModel.uiState.value.chartPoints
        )

        assertEquals(2, repository.getMarketChartCalls)
    }

    @Test
    fun retryChart_whileLoading() =
        runViewModelTest {

            repository.responseDelay = 1000L

            viewModel.loadCoin("bitcoin")

            runCurrent()

            advanceTimeBy(1000L)

            runCurrent()

            assertTrue(viewModel.uiState.value.isChartLoading)

            viewModel.retryChart()

            runCurrent()

            assertEquals(1, repository.getMarketChartCalls)
        }

    // Testy anulowania poprzednich zapytań

    @Test
    fun changePeriod_cancelsPreviousChart() =
        runViewModelTest {

            viewModel.loadCoin("bitcoin")

            runCurrent()

            repository.responseDelay = 1000L

            viewModel.changePeriod("30")

            runCurrent()

            advanceTimeBy(500L)

            viewModel.changePeriod("90")

            runCurrent()

            assertEquals(1, repository.cancelledChartCalls)

            // Dane dla ostatniego zapytania
            repository.marketChart = MarketChartModel(
                prices = listOf(
                    listOf(1000.0, 50000.0),
                    listOf(2000.0, 65000.0)
                )
            )

            // Poprzednie zapytanie powinno być anulowane
            advanceTimeBy(500L)

            runCurrent()

            assertTrue(viewModel.uiState.value.isChartLoading)

            assertTrue(
                viewModel.uiState.value.chartPoints.isEmpty()
            )

            advanceTimeBy(500L)

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals("90", state.selectedPeriod)

            assertFalse(state.isChartLoading)

            assertEquals("90", repository.lastChartDays)

            assertEquals(
                listOf(
                    ChartPoint(1000L, 50000.0),
                    ChartPoint(2000L, 65000.0)
                ),
                state.chartPoints
            )

            assertEquals(3, repository.getMarketChartCalls)
        }

    @Test
    fun loadCoin_cancelsPreviousRequest() =
        runViewModelTest {

            repository.responseDelay = 1000L

            viewModel.loadCoin("bitcoin")

            runCurrent()

            advanceTimeBy(500L)

            viewModel.loadCoin("ethereum")

            runCurrent()

            advanceTimeBy(500L)

            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)

            assertNull(viewModel.uiState.value.coin)

            advanceTimeBy(500L)

            runCurrent()

            assertEquals(
                ethereum,
                viewModel.uiState.value.coin
            )

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(
                "ethereum",
                repository.lastChartCoinId
            )

            assertEquals(2, repository.getCoinsByIdsCalls)

            assertEquals(1, repository.getMarketChartCalls)
        }

    // Test czyszczenia ViewModel

    @Test
    fun onCleared_cancelsChartRequest() =
        runViewModelTest {

            repository.responseDelay = 1000L

            viewModel.loadCoin("bitcoin")

            runCurrent()

            advanceTimeBy(1000L)

            runCurrent()

            assertTrue(viewModel.uiState.value.isChartLoading)

            viewModelStore.clear()

            runCurrent()

            assertEquals(1, repository.cancelledChartCalls)

            advanceTimeBy(2000L)

            runCurrent()

            assertTrue(
                viewModel.uiState.value.chartPoints.isEmpty()
            )

            assertEquals(1, repository.getMarketChartCalls)
        }
}
