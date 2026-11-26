
package com.example.cryptobotmonitor.presentation.alerts

import androidx.lifecycle.ViewModelStore
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.fake.FakeAlertRepository
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
class AlertsViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    private lateinit var repository: FakeAlertRepository

    private lateinit var coinRepository: FakeCoinRepository

    private lateinit var viewModel: AlertsViewModel

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

    private val coins = listOf(bitcoin, ethereum)

    // Przykładowe alerty
    private val activeAlert = PriceAlert(
        id = "test-alert-1",
        userId = "test-user",
        coinId = "bitcoin",
        coinName = "Bitcoin",
        targetPrice = 70000.0,
        condition = "ABOVE",
        active = true
    )

    private val completedAlert = PriceAlert(
        id = "test-alert-2",
        userId = "test-user",
        coinId = "ethereum",
        coinName = "Ethereum",
        targetPrice = 2500.0,
        condition = "BELOW",
        active = false
    )

    @Before
    fun setUp() {

        dispatcher = StandardTestDispatcher()

        Dispatchers.setMain(dispatcher)

        repository = FakeAlertRepository()

        coinRepository = FakeCoinRepository()

        coinRepository.coins = coins

        viewModelStore = ViewModelStore()
    }

    // Tworzenie ViewModel
    private fun createViewModel() {

        viewModel = AlertsViewModel(
            repository = repository,
            coinRepository = coinRepository
        )

        viewModelStore.put(
            "alerts",
            viewModel
        )
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

    // Ustawienie poprawnych danych alertu
    private fun enterValidData() {

        viewModel.selectCoin(bitcoin)

        viewModel.onTargetPriceChange("70000")

        viewModel.onConditionChange("ABOVE")
    }

    // Testy stanu początkowego

    @Test
    fun initialState_defaultValues() = runViewModelTest {

        createViewModel()

        val state = viewModel.uiState.value

        assertTrue(state.alerts.isEmpty())
        assertTrue(state.isLoading)
        assertTrue(state.isCoinsLoading)
        assertFalse(state.isSaving)
        assertFalse(state.showCompletedAlerts)
        assertEquals("ABOVE", state.condition)
        assertEquals("", state.targetPrice)
        assertNull(state.error)
    }

    // Testy pobierania alertów

    @Test
    fun loadAlerts_observerStarts() = runViewModelTest {

        createViewModel()

        assertEquals(1, repository.observeCalls)
    }

    @Test
    fun loadAlerts_success() = runViewModelTest {

        createViewModel()

        repository.emitAlerts(
            listOf(activeAlert, completedAlert)
        )

        val state = viewModel.uiState.value

        assertEquals(
            listOf(activeAlert, completedAlert),
            state.alerts
        )

        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadAlerts_emptyResponse() = runViewModelTest {

        createViewModel()

        repository.emitAlerts(emptyList())

        assertTrue(viewModel.uiState.value.alerts.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadAlerts_error() = runViewModelTest {

        createViewModel()

        repository.emitError("Błąd Firestore")

        assertEquals(
            "Błąd Firestore",
            viewModel.uiState.value.error
        )

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadAlerts_successClearsError() =
        runViewModelTest {

            createViewModel()

            repository.emitError("Błąd Firestore")

            repository.emitAlerts(listOf(activeAlert))

            assertNull(viewModel.uiState.value.error)

            assertEquals(
                listOf(activeAlert),
                viewModel.uiState.value.alerts
            )
        }

    // Testy pobierania kryptowalut

    @Test
    fun loadAvailableCoins_success() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                coins,
                viewModel.uiState.value.availableCoins
            )

            assertFalse(viewModel.uiState.value.isCoinsLoading)
            assertNull(viewModel.uiState.value.coinsError)
        }

    @Test
    fun loadAvailableCoins_loadingState() =
        runViewModelTest {

            coinRepository.responseDelay = 1000L

            createViewModel()

            runCurrent()

            assertTrue(viewModel.uiState.value.isCoinsLoading)

            advanceTimeBy(1000L)

            runCurrent()

            assertFalse(viewModel.uiState.value.isCoinsLoading)

            assertEquals(
                coins,
                viewModel.uiState.value.availableCoins
            )
        }

    @Test
    fun loadAvailableCoins_error() =
        runViewModelTest {

            coinRepository.error = Exception("Błąd API")

            createViewModel()

            runCurrent()

            assertEquals(
                "Nie udało się pobrać kryptowalut.",
                viewModel.uiState.value.coinsError
            )

            assertFalse(viewModel.uiState.value.isCoinsLoading)
        }

    @Test
    fun loadAvailableCoins_retryAfterError() =
        runViewModelTest {

            coinRepository.error = Exception("Błąd API")

            createViewModel()

            runCurrent()

            assertNotNull(viewModel.uiState.value.coinsError)

            coinRepository.error = null

            viewModel.loadAvailableCoins()

            runCurrent()

            assertNull(viewModel.uiState.value.coinsError)

            assertEquals(
                coins,
                viewModel.uiState.value.availableCoins
            )
        }

    @Test
    fun loadAvailableCoins_blocksRepeatedRequest() =
        runViewModelTest {

            coinRepository.responseDelay = 1000L

            createViewModel()

            viewModel.loadAvailableCoins()
            viewModel.loadAvailableCoins()

            runCurrent()

            assertEquals(1, coinRepository.getCoinsCalls)

            advanceTimeBy(1000L)

            runCurrent()
        }

    // Testy formularza i filtrowania

    @Test
    fun selectCoin_updatesData() = runViewModelTest {

        createViewModel()

        viewModel.onCoinSearchQueryChange("BTC")

        viewModel.selectCoin(bitcoin)

        val state = viewModel.uiState.value

        assertEquals("bitcoin", state.coinId)
        assertEquals("Bitcoin", state.coinName)
        assertEquals("", state.coinSearchQuery)
        assertNull(state.error)
    }

    @Test
    fun coinSearch_updatesQuery() = runViewModelTest {

        createViewModel()

        viewModel.onCoinSearchQueryChange("ETH")

        assertEquals(
            "ETH",
            viewModel.uiState.value.coinSearchQuery
        )
    }

    @Test
    fun setShowCompletedAlerts_updatesState() =
        runViewModelTest {

            createViewModel()

            viewModel.setShowCompletedAlerts(true)

            assertTrue(
                viewModel.uiState.value.showCompletedAlerts
            )

            viewModel.setShowCompletedAlerts(false)

            assertFalse(
                viewModel.uiState.value.showCompletedAlerts
            )
        }

    @Test
    fun setInitialCoinData_updatesState() =
        runViewModelTest {

            createViewModel()

            viewModel.setInitialCoinData(
                "ethereum",
                "Ethereum"
            )

            assertEquals(
                "ethereum",
                viewModel.uiState.value.coinId
            )

            assertEquals(
                "Ethereum",
                viewModel.uiState.value.coinName
            )
        }

    @Test
    fun formChanges_clearError() = runViewModelTest {

        createViewModel()

        viewModel.addAlert()

        assertNotNull(viewModel.uiState.value.error)

        viewModel.onTargetPriceChange("50000")

        assertNull(viewModel.uiState.value.error)

        assertEquals(
            "50000",
            viewModel.uiState.value.targetPrice
        )
    }

    // Testy walidacji alertu

    @Test
    fun addAlert_withoutCoin() = runViewModelTest {

        createViewModel()

        viewModel.onTargetPriceChange("70000")

        viewModel.addAlert()

        assertEquals(
            "Wybierz kryptowalutę.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.addCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun addAlert_emptyPrice() = runViewModelTest {

        createViewModel()

        viewModel.selectCoin(bitcoin)

        viewModel.addAlert()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(0, repository.addCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun addAlert_invalidPrice() = runViewModelTest {

        createViewModel()

        viewModel.selectCoin(bitcoin)

        val invalidPrices = listOf(
            "abc",
            "0",
            "-100",
            "NaN",
            "Infinity",
            "1000000000001"
        )

        invalidPrices.forEach { price ->

            viewModel.onTargetPriceChange(price)

            viewModel.addAlert()

            assertNotNull(viewModel.uiState.value.error)

            assertFalse(viewModel.uiState.value.isSaving)
        }

        assertEquals(0, repository.addCalls)
    }

    @Test
    fun addAlert_invalidCondition() = runViewModelTest {

        createViewModel()

        enterValidData()

        viewModel.onConditionChange("INVALID")

        viewModel.addAlert()

        assertEquals(
            "Wybierz poprawny warunek alertu.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.addCalls)
    }

    // Testy zapisywania alertów

    @Test
    fun addAlert_setsSavingState() = runViewModelTest {

        createViewModel()

        enterValidData()

        viewModel.addAlert()

        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(1, repository.addCalls)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun addAlert_passesCorrectData() = runViewModelTest {

        createViewModel()

        viewModel.onCoinIdChange(" BTC ")
        viewModel.onCoinNameChange(" Bitcoin ")
        viewModel.onTargetPriceChange("70000,50")
        viewModel.onConditionChange("BELOW")

        viewModel.addAlert()

        val alert = checkNotNull(repository.lastAddedAlert)

        assertEquals("btc", alert.coinId)
        assertEquals("Bitcoin", alert.coinName)
        assertEquals(70000.50, alert.targetPrice, 0.001)
        assertEquals("BELOW", alert.condition)
        assertTrue(alert.active)
    }

    @Test
    fun addAlert_success() = runViewModelTest {

        createViewModel()

        enterValidData()

        viewModel.addAlert()

        repository.completeAdd(true)

        val state = viewModel.uiState.value

        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals("Alert został dodany.", state.message)
        assertEquals(1, state.alerts.size)

        assertEquals("", state.coinId)
        assertEquals("", state.coinName)
        assertEquals("", state.targetPrice)
        assertEquals("", state.coinSearchQuery)
        assertEquals("ABOVE", state.condition)
    }

    @Test
    fun addAlert_error() = runViewModelTest {

        createViewModel()

        enterValidData()

        viewModel.addAlert()

        repository.completeAdd(
            false,
            "Błąd zapisu"
        )

        val state = viewModel.uiState.value

        assertFalse(state.isSaving)
        assertEquals("Błąd zapisu", state.error)
        assertEquals("70000", state.targetPrice)
        assertTrue(state.alerts.isEmpty())
    }

    @Test
    fun addAlert_errorWithoutMessage() =
        runViewModelTest {

            createViewModel()

            enterValidData()

            viewModel.addAlert()

            repository.completeAdd(false)

            assertEquals(
                "Nie udało się dodać alertu.",
                viewModel.uiState.value.error
            )

            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun addAlert_blocksRepeatedRequest() =
        runViewModelTest {

            createViewModel()

            enterValidData()

            viewModel.addAlert()
            viewModel.addAlert()
            viewModel.addAlert()

            assertEquals(1, repository.addCalls)

            repository.completeAdd(true)

            assertEquals(1, viewModel.uiState.value.alerts.size)

            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun addAlert_retryAfterError() = runViewModelTest {

        createViewModel()

        enterValidData()

        viewModel.addAlert()

        repository.completeAdd(false, "Błąd zapisu")

        viewModel.addAlert()

        assertEquals(2, repository.addCalls)
        assertTrue(viewModel.uiState.value.isSaving)

        repository.completeAdd(true)

        assertNull(viewModel.uiState.value.error)

        assertEquals(
            "Alert został dodany.",
            viewModel.uiState.value.message
        )
    }

    // Testy usuwania alertów

    @Test
    fun deleteAlert_success() = runViewModelTest {

        createViewModel()

        repository.emitAlerts(
            listOf(activeAlert, completedAlert)
        )

        viewModel.deleteAlert(activeAlert.id)

        assertEquals(1, repository.deleteCalls)

        assertEquals(
            activeAlert.id,
            repository.lastDeletedId
        )

        repository.completeDelete(true)

        assertEquals(
            listOf(completedAlert),
            viewModel.uiState.value.alerts
        )
    }

    @Test
    fun deleteAlert_error() = runViewModelTest {

        createViewModel()

        repository.emitAlerts(listOf(activeAlert))

        viewModel.deleteAlert(activeAlert.id)

        repository.completeDelete(
            false,
            "Nie udało się usunąć alertu."
        )

        assertEquals(
            "Nie udało się usunąć alertu.",
            viewModel.uiState.value.error
        )

        assertEquals(
            listOf(activeAlert),
            viewModel.uiState.value.alerts
        )
    }

    // Test czyszczenia ViewModel

    @Test
    fun onCleared_removesListener() = runViewModelTest {

        createViewModel()

        runCurrent()

        viewModelStore.clear()

        runCurrent()

        assertEquals(
            1,
            repository.listenerRemoveCalls
        )
    }
}
