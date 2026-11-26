
package com.example.cryptobotmonitor.presentation.profile

import androidx.lifecycle.ViewModelStore
import com.example.cryptobotmonitor.data.auth.CurrentUser
import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.fake.FakeCurrentUserProvider
import com.example.cryptobotmonitor.fake.FakeFavoriteRepository
import com.example.cryptobotmonitor.fake.FakeAlertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    private lateinit var userProvider:
            FakeCurrentUserProvider

    private lateinit var favoriteRepository:
            FakeFavoriteRepository

    private lateinit var alertRepository:
            FakeAlertRepository

    private lateinit var viewModel: ProfileViewModel

    private lateinit var viewModelStore: ViewModelStore

    // Przykładowe ulubione kryptowaluty
    private val bitcoinFavorite = FavoriteCoin(
        id = "favorite-1",
        userId = "test-user",
        coinId = "bitcoin",
        coinName = "Bitcoin"
    )

    private val ethereumFavorite = FavoriteCoin(
        id = "favorite-2",
        userId = "test-user",
        coinId = "ethereum",
        coinName = "Ethereum"
    )

    private val favorites = listOf(
        bitcoinFavorite,
        ethereumFavorite
    )

    // Przykładowe alerty użytkownika
    private val activeAlert = PriceAlert(
        id = "alert-1",
        userId = "test-user",
        coinId = "bitcoin",
        coinName = "Bitcoin",
        targetPrice = 70000.0,
        condition = "ABOVE",
        active = true
    )

    private val secondActiveAlert = PriceAlert(
        id = "alert-2",
        userId = "test-user",
        coinId = "ethereum",
        coinName = "Ethereum",
        targetPrice = 4000.0,
        condition = "ABOVE",
        active = true
    )

    private val completedAlert = PriceAlert(
        id = "alert-3",
        userId = "test-user",
        coinId = "bitcoin",
        coinName = "Bitcoin",
        targetPrice = 55000.0,
        condition = "BELOW",
        active = false
    )

    private val alerts = listOf(
        activeAlert,
        secondActiveAlert,
        completedAlert
    )

    @Before
    fun setUp() {

        dispatcher = StandardTestDispatcher()

        Dispatchers.setMain(dispatcher)

        userProvider = FakeCurrentUserProvider()

        favoriteRepository = FakeFavoriteRepository()

        alertRepository = FakeAlertRepository()

        favoriteRepository.emitFavorites(favorites)

        alertRepository.emitAlerts(alerts)

        viewModelStore = ViewModelStore()
    }

    // Tworzenie ViewModel
    private fun createViewModel() {

        viewModel = ProfileViewModel(
            userProvider = userProvider,
            favoriteRepository = favoriteRepository,
            alertRepository = alertRepository
        )

        viewModelStore.put("profile", viewModel)
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

    // Testy stanu początkowego

    @Test
    fun initialState_loadingProfile() =
        runViewModelTest {

            createViewModel()

            val state = viewModel.uiState.value

            assertEquals("user@gmail.com", state.email)

            assertTrue(state.isLoading)

            assertEquals(0, state.favoritesCount)

            assertEquals(0, state.activeAlertsCount)

            assertEquals(0, state.completedAlertsCount)

            assertNull(state.error)
        }

    // Testy danych użytkownika

    @Test
    fun loadProfile_returnsUserEmail() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                "user@gmail.com",
                viewModel.uiState.value.email
            )

            assertFalse(viewModel.uiState.value.isLoading)

            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun loadProfile_userWithoutEmail() =
        runViewModelTest {

            userProvider.user = CurrentUser(
                uid = "test-user",
                email = null
            )

            createViewModel()

            runCurrent()

            assertEquals(
                "",
                viewModel.uiState.value.email
            )

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun loadProfile_userNotLoggedIn() =
        runViewModelTest {

            userProvider.user = null

            createViewModel()

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals(
                "Użytkownik nie jest zalogowany.",
                state.error
            )

            assertFalse(state.isLoading)

            assertEquals("", state.email)

            assertEquals(0, favoriteRepository.getFavoritesCalls)

            assertEquals(0, alertRepository.getAlertsCalls)
        }

    // Testy statystyk użytkownika

    @Test
    fun loadProfile_countsFavorites() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                2,
                viewModel.uiState.value.favoritesCount
            )
        }

    @Test
    fun loadProfile_countsActiveAlerts() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                2,
                viewModel.uiState.value.activeAlertsCount
            )
        }

    @Test
    fun loadProfile_countsCompletedAlerts() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                1,
                viewModel.uiState.value.completedAlertsCount
            )
        }

    @Test
    fun loadProfile_emptyStatistics() =
        runViewModelTest {

            favoriteRepository.emitFavorites(emptyList())

            alertRepository.emitAlerts(emptyList())

            createViewModel()

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals(0, state.favoritesCount)

            assertEquals(0, state.activeAlertsCount)

            assertEquals(0, state.completedAlertsCount)

            assertFalse(state.isLoading)

            assertNull(state.error)
        }

    @Test
    fun loadProfile_onlyActiveAlerts() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(activeAlert, secondActiveAlert)
            )

            createViewModel()

            runCurrent()

            assertEquals(
                2,
                viewModel.uiState.value.activeAlertsCount
            )

            assertEquals(
                0,
                viewModel.uiState.value.completedAlertsCount
            )
        }

    @Test
    fun loadProfile_onlyCompletedAlerts() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(completedAlert)
            )

            createViewModel()

            runCurrent()

            assertEquals(
                0,
                viewModel.uiState.value.activeAlertsCount
            )

            assertEquals(
                1,
                viewModel.uiState.value.completedAlertsCount
            )
        }

    // Testy pobierania danych

    @Test
    fun loadProfile_callsRepositories() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                1,
                favoriteRepository.getFavoritesCalls
            )

            assertEquals(
                1,
                alertRepository.getAlertsCalls
            )
        }

    @Test
    fun loadProfile_loadingState() =
        runViewModelTest {

            favoriteRepository.responseDelay = 1000L

            createViewModel()

            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)

            advanceTimeBy(1000L)

            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(
                2,
                viewModel.uiState.value.favoritesCount
            )
        }

    // Testy obsługi błędów

    @Test
    fun loadProfile_favoritesError() =
        runViewModelTest {

            favoriteRepository.error =
                Exception("Błąd Firestore")

            createViewModel()

            runCurrent()

            assertEquals(
                "Nie udało się pobrać danych profilu.",
                viewModel.uiState.value.error
            )

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(0, alertRepository.getAlertsCalls)
        }

    @Test
    fun loadProfile_alertsError() =
        runViewModelTest {

            alertRepository.suspendError =
                Exception("Błąd pobierania alertów")

            createViewModel()

            runCurrent()

            assertEquals(
                "Nie udało się pobrać danych profilu.",
                viewModel.uiState.value.error
            )

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun loadProfile_retryAfterError() =
        runViewModelTest {

            favoriteRepository.error =
                Exception("Błąd Firestore")

            createViewModel()

            runCurrent()

            assertNotNull(viewModel.uiState.value.error)

            favoriteRepository.error = null

            viewModel.loadProfile()

            runCurrent()

            val state = viewModel.uiState.value

            assertNull(state.error)

            assertFalse(state.isLoading)

            assertEquals(2, state.favoritesCount)

            assertEquals(2, state.activeAlertsCount)

            assertEquals(1, state.completedAlertsCount)
        }

    // Testy ponownego pobierania profilu

    @Test
    fun refreshProfile_updatesStatistics() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            favoriteRepository.emitFavorites(
                listOf(bitcoinFavorite)
            )

            alertRepository.emitAlerts(
                listOf(completedAlert)
            )

            viewModel.loadProfile()

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals(1, state.favoritesCount)

            assertEquals(0, state.activeAlertsCount)

            assertEquals(1, state.completedAlertsCount)

            assertEquals(2, favoriteRepository.getFavoritesCalls)

            assertEquals(2, alertRepository.getAlertsCalls)
        }

    @Test
    fun refreshProfile_cancelsPreviousRequest() =
        runViewModelTest {

            favoriteRepository.responseDelay = 1000L

            createViewModel()

            runCurrent()

            advanceTimeBy(500L)

            viewModel.loadProfile()

            runCurrent()

            advanceTimeBy(500L)

            runCurrent()

            // Poprzednie zapytanie zostało anulowane
            assertTrue(viewModel.uiState.value.isLoading)

            assertEquals(0, alertRepository.getAlertsCalls)

            advanceTimeBy(500L)

            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(2, favoriteRepository.getFavoritesCalls)

            assertEquals(1, alertRepository.getAlertsCalls)
        }

    // Testy zmiany użytkownika

    @Test
    fun loadProfile_userChangedDuringRequest() =
        runViewModelTest {

            favoriteRepository.responseDelay = 1000L

            createViewModel()

            runCurrent()

            userProvider.user = CurrentUser(
                uid = "other-user",
                email = "other@gmail.com"
            )

            advanceTimeBy(1000L)

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals(
                "Użytkownik został zmieniony.",
                state.error
            )

            assertFalse(state.isLoading)

            assertEquals("", state.email)

            assertEquals(0, state.favoritesCount)

            assertEquals(0, state.activeAlertsCount)

            assertEquals(0, state.completedAlertsCount)
        }

    @Test
    fun refreshProfile_afterLogout() =
        runViewModelTest {

            createViewModel()

            runCurrent()

            assertEquals(
                2,
                viewModel.uiState.value.favoritesCount
            )

            userProvider.user = null

            viewModel.loadProfile()

            runCurrent()

            val state = viewModel.uiState.value

            assertEquals("", state.email)

            assertEquals(0, state.favoritesCount)

            assertEquals(0, state.activeAlertsCount)

            assertEquals(0, state.completedAlertsCount)

            assertFalse(state.isLoading)

            assertEquals(
                "Użytkownik nie jest zalogowany.",
                state.error
            )
        }

    // Test czyszczenia ViewModel

    @Test
    fun onCleared_cancelsProfileRequest() =
        runViewModelTest {

            favoriteRepository.responseDelay = 1000L

            createViewModel()

            runCurrent()

            viewModelStore.clear()

            runCurrent()

            advanceTimeBy(2000L)

            runCurrent()

            assertEquals(
                0,
                viewModel.uiState.value.favoritesCount
            )

            assertEquals(
                0,
                alertRepository.getAlertsCalls
            )
        }
}
