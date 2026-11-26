
package com.example.cryptobotmonitor.presentation.home

import androidx.lifecycle.ViewModelStore
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.example.cryptobotmonitor.fake.FakeCoinRepository
import com.example.cryptobotmonitor.fake.FakeFavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    private lateinit var repository: FakeCoinRepository

    private lateinit var favoriteRepository:
            FakeFavoriteRepository

    private lateinit var viewModel: HomeViewModel

    private lateinit var viewModelStore: ViewModelStore

    // Przykładowe dane do testów
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

    private val coins = listOf(
        bitcoin,
        ethereum
    )

    @Before
    fun setUp() {

        dispatcher = StandardTestDispatcher()

        Dispatchers.setMain(dispatcher)

        repository = FakeCoinRepository()
        favoriteRepository = FakeFavoriteRepository()

        repository.coins = coins

        viewModelStore = ViewModelStore()
    }

    // Tworzenie ViewModel z testowymi repozytoriami
    private fun createViewModel() {

        viewModel = HomeViewModel(
            repository = repository,
            favoriteRepository = favoriteRepository
        )

        viewModelStore.put(
            "home",
            viewModel
        )
    }
    // Uruchamianie testu i zatrzymanie ViewModel
    private fun runViewModelTest(
        testBody: suspend TestScope.() -> Unit
    ) = runTest(dispatcher) {

        try {

            testBody()

        } finally {

            // Zatrzymanie wszystkich zadań ViewModel
            viewModelStore.clear()

            runCurrent()
        }
    }

    // Czyszczenie danych po każdym teście
    @After
    fun tearDown() {

        viewModelStore.clear()

        dispatcher.scheduler.runCurrent()

        Dispatchers.resetMain()
    }

    // Testy pobierania kryptowalut

    @Test
    fun initialState_emptyCoins() = runViewModelTest {

        createViewModel()

        assertTrue(viewModel.coins.value.isEmpty())

        assertNull(viewModel.error.value)
    }

    @Test
    fun loadCoins_success() = runViewModelTest {

        createViewModel()

        runCurrent()

        assertEquals(coins, viewModel.coins.value)

        assertFalse(viewModel.isLoading.value)

        assertNull(viewModel.error.value)
    }

    @Test
    fun loadCoins_loadingState() = runViewModelTest {

        repository.responseDelay = 1000L

        createViewModel()

        runCurrent()

        assertTrue(viewModel.isLoading.value)

        advanceTimeBy(1000L)
        runCurrent()

        assertFalse(viewModel.isLoading.value)

        assertEquals(coins, viewModel.coins.value)
    }

    @Test
    fun loadCoins_emptyResponse() = runViewModelTest {

        repository.coins = emptyList()

        createViewModel()

        runCurrent()

        assertTrue(viewModel.coins.value.isEmpty())

        assertFalse(viewModel.isLoading.value)

        assertNull(viewModel.error.value)
    }

    @Test
    fun loadCoins_apiError() = runViewModelTest {

        repository.error = Exception("Błąd API")

        createViewModel()

        runCurrent()

        assertEquals(
            "Nie udało się pobrać danych: Błąd API",
            viewModel.error.value
        )

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun loadCoins_retryAfterError() = runViewModelTest {

        repository.error = Exception("Błąd API")

        createViewModel()

        runCurrent()

        assertNotNull(viewModel.error.value)

        repository.error = null

        viewModel.loadCoins()

        runCurrent()

        assertNull(viewModel.error.value)

        assertEquals(coins, viewModel.coins.value)
    }

    @Test
    fun loadCoins_preservesPreviousDataOnError() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            repository.error = Exception("Błąd API")

            viewModel.loadCoins()

            runCurrent()

            assertEquals(coins, viewModel.coins.value)

            assertNotNull(viewModel.error.value)
        }

    @Test
    fun loadCoins_refreshDoesNotShowLoading() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            repository.responseDelay = 1000L

            viewModel.loadCoins()

            runCurrent()

            assertFalse(viewModel.isLoading.value)

            advanceTimeBy(1000L)
            runCurrent()
        }

    // Testy automatycznego odświeżania

    @Test
    fun autoRefresh_firstRequest() = runViewModelTest {

        createViewModel()

        runCurrent()

        assertEquals(1, repository.getCoinsCalls)
    }

    @Test
    fun autoRefresh_after60Seconds() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            advanceTimeBy(60_000L)

            runCurrent()

            assertEquals(2, repository.getCoinsCalls)
        }

    @Test
    fun autoRefresh_before60Seconds() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            advanceTimeBy(59_999L)

            runCurrent()

            assertEquals(1, repository.getCoinsCalls)
        }

    // Testy wyszukiwania

    @Test
    fun search_updatesQuery() = runViewModelTest {

        createViewModel()

        viewModel.setSearchQuery("BTC")

        assertEquals(
            "BTC",
            viewModel.searchQuery.value
        )
    }

    @Test
    fun search_clearQuery() = runViewModelTest {

        createViewModel()

        viewModel.setSearchQuery("BTC")

        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun search_filtersCoins() = runViewModelTest {

        createViewModel()

        backgroundScope.launch(dispatcher) {
            viewModel.filteredCoins.collect {}
        }

        runCurrent()

        viewModel.setSearchQuery("BTC")

        runCurrent()

        assertEquals(
            listOf(bitcoin),
            viewModel.filteredCoins.value
        )
    }

    @Test
    fun favorites_changesFilter() = runViewModelTest {

        createViewModel()

        viewModel.setShowOnlyFavorites(true)

        assertTrue(viewModel.showOnlyFavorites.value)

        viewModel.setShowOnlyFavorites(false)

        assertFalse(viewModel.showOnlyFavorites.value)
    }

    // Testy obserwowania ulubionych

    @Test
    fun favorites_observerStarts() = runViewModelTest {

        createViewModel()

        assertEquals(
            1,
            favoriteRepository.observeCalls
        )
    }

    @Test
    fun favorites_updatesIds() = runViewModelTest {

        createViewModel()

        favoriteRepository.emitFavorites(
            listOf(
                FavoriteCoin(
                    coinId = "bitcoin",
                    coinName = "Bitcoin"
                )
            )
        )

        assertEquals(
            setOf("bitcoin"),
            viewModel.favoriteIds.value
        )
    }

    @Test
    fun favorites_onlyFavoriteCoins() =
        runViewModelTest {

            createViewModel()

            backgroundScope.launch(dispatcher) {
                viewModel.filteredCoins.collect {}
            }

            runCurrent()

            favoriteRepository.emitFavorites(
                listOf(
                    FavoriteCoin(
                        coinId = "bitcoin",
                        coinName = "Bitcoin"
                    )
                )
            )

            viewModel.setShowOnlyFavorites(true)

            runCurrent()

            assertEquals(
                listOf(bitcoin),
                viewModel.filteredCoins.value
            )
        }

    @Test
    fun favorites_observerError() = runViewModelTest {

        createViewModel()

        favoriteRepository.emitError(
            "Błąd Firestore"
        )

        assertEquals(
            "Błąd Firestore",
            viewModel.favoriteError.value
        )
    }

    @Test
    fun favorites_successClearsObserverError() =
        runViewModelTest {

            createViewModel()

            favoriteRepository.emitError(
                "Błąd Firestore"
            )

            favoriteRepository.emitFavorites(
                emptyList()
            )

            assertNull(
                viewModel.favoriteError.value
            )
        }

    // Testy dodawania i usuwania ulubionych

    @Test
    fun toggleFavorite_addCoin() = runViewModelTest {

        createViewModel()

        viewModel.toggleFavorite(bitcoin)

        runCurrent()

        assertEquals(
            1,
            favoriteRepository.addCalls
        )

        assertEquals(
            "bitcoin",
            favoriteRepository.lastCoinId
        )

        assertEquals(
            setOf("bitcoin"),
            viewModel.favoriteIds.value
        )
    }

    @Test
    fun toggleFavorite_removeCoin() =
        runViewModelTest {

            createViewModel()

            favoriteRepository.emitFavorites(
                listOf(
                    FavoriteCoin(
                        coinId = "bitcoin",
                        coinName = "Bitcoin"
                    )
                )
            )

            viewModel.toggleFavorite(bitcoin)

            runCurrent()

            assertEquals(
                1,
                favoriteRepository.removeCalls
            )

            assertTrue(
                viewModel.favoriteIds.value.isEmpty()
            )
        }

    @Test
    fun toggleFavorite_saveError() =
        runViewModelTest {

            createViewModel()

            favoriteRepository.error =
                Exception("Błąd zapisu")

            viewModel.toggleFavorite(bitcoin)

            runCurrent()

            assertEquals(
                "Nie udało się zmienić ulubionych: Błąd zapisu",
                viewModel.favoriteError.value
            )

            assertTrue(
                viewModel.pendingFavoriteIds.value.isEmpty()
            )
        }

    @Test
    fun toggleFavorite_blocksRepeatedRequest() =
        runViewModelTest {

            createViewModel()

            favoriteRepository.responseDelay = 1000L

            viewModel.toggleFavorite(bitcoin)
            viewModel.toggleFavorite(bitcoin)
            viewModel.toggleFavorite(bitcoin)

            runCurrent()

            assertEquals(
                1,
                favoriteRepository.addCalls
            )

            assertTrue(
                "bitcoin" in
                        viewModel.pendingFavoriteIds.value
            )

            advanceTimeBy(1000L)

            runCurrent()

            assertTrue(
                viewModel.pendingFavoriteIds.value.isEmpty()
            )
        }

    // Test czyszczenia ViewModel

    @Test
    fun onCleared_removesFavoritesListener() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            viewModelStore.clear()

            runCurrent()

            assertEquals(
                1,
                favoriteRepository.listenerRemoveCalls
            )

            advanceTimeBy(60_000L)

            runCurrent()

            assertEquals(
                1,
                repository.getCoinsCalls
            )
        }
}
