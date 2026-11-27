
package com.example.cryptobotmonitor.presentation.bot

import androidx.lifecycle.ViewModelStore
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.data.model.FavoriteCoin
import com.example.cryptobotmonitor.data.model.PriceAlert
import com.example.cryptobotmonitor.fake.FakeCoinRepository
import com.example.cryptobotmonitor.fake.FakeAlertRepository
import com.example.cryptobotmonitor.fake.FakeFavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BotViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    private lateinit var coinRepository: FakeCoinRepository

    private lateinit var alertRepository: FakeAlertRepository

    private lateinit var favoriteRepository:
            FakeFavoriteRepository

    private lateinit var viewModel: BotViewModel

    private lateinit var viewModelStore: ViewModelStore

    // Przykładowe kryptowaluty
    private val bitcoin = CoinModel(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        currentPrice = 60000.0,
        image = "",
        priceChange24h = 5.0
    )

    private val ethereum = CoinModel(
        id = "ethereum",
        symbol = "eth",
        name = "Ethereum",
        currentPrice = 3000.0,
        image = "",
        priceChange24h = -3.0
    )

    private val solana = CoinModel(
        id = "solana",
        symbol = "sol",
        name = "Solana",
        currentPrice = 150.0,
        image = "",
        priceChange24h = 10.0
    )

    // Przykładowe alerty
    private val activeAlert = PriceAlert(
        id = "alert-1",
        userId = "test-user",
        coinId = "bitcoin",
        coinName = "Bitcoin",
        targetPrice = 70000.0,
        condition = "ABOVE",
        active = true
    )

    private val completedAlert = PriceAlert(
        id = "alert-2",
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

        coinRepository = FakeCoinRepository()

        alertRepository = FakeAlertRepository()

        favoriteRepository = FakeFavoriteRepository()

        coinRepository.coins = listOf(
            bitcoin,
            ethereum,
            solana
        )

        viewModelStore = ViewModelStore()

        viewModel = BotViewModel(
            coinRepository = coinRepository,
            alertRepository = alertRepository,
            favoriteRepository = favoriteRepository
        )

        viewModelStore.put("bot", viewModel)
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

    // Wysłanie komendy i pobranie odpowiedzi
    private fun TestScope.sendCommand(
        command: String
    ): String {

        viewModel.onCommandChange(command)

        viewModel.sendCommand()

        runCurrent()

        return viewModel.uiState.value.messages.last().text
    }

    // Testy stanu początkowego i formularza

    @Test
    fun initialState_defaultValues() =
        runViewModelTest {

            val state = viewModel.uiState.value

            assertEquals("", state.command)

            assertFalse(state.isLoading)

            assertEquals(1, state.messages.size)

            assertFalse(state.messages.first().isUser)

            assertTrue(
                state.messages.first().text.contains("pomoc")
            )
        }

    @Test
    fun commandChange_updatesState() =
        runViewModelTest {

            viewModel.onCommandChange("cena btc")

            assertEquals(
                "cena btc",
                viewModel.uiState.value.command
            )
        }

    @Test
    fun sendCommand_emptyCommand() =
        runViewModelTest {

            viewModel.onCommandChange("   ")

            viewModel.sendCommand()

            runCurrent()

            assertEquals(
                1,
                viewModel.uiState.value.messages.size
            )

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(0, coinRepository.getCoinsCalls)
        }

    @Test
    fun sendCommand_clearsInput() =
        runViewModelTest {

            sendCommand("pomoc")

            assertEquals(
                "",
                viewModel.uiState.value.command
            )
        }

    @Test
    fun sendCommand_trimsUserMessage() =
        runViewModelTest {

            sendCommand("   pomoc   ")

            val messages = viewModel.uiState.value.messages

            assertEquals("pomoc", messages[1].text)

            assertTrue(messages[1].isUser)
        }

    @Test
    fun sendCommand_preservesMessageHistory() =
        runViewModelTest {

            sendCommand("cena btc")

            sendCommand("cena eth")

            val messages = viewModel.uiState.value.messages

            assertEquals(5, messages.size)

            assertEquals("cena btc", messages[1].text)

            assertTrue(messages[1].isUser)

            assertFalse(messages[2].isUser)

            assertEquals("cena eth", messages[3].text)

            assertFalse(messages[4].isUser)
        }

    @Test
    fun sendCommand_loadingState() =
        runViewModelTest {

            coinRepository.responseDelay = 1000L

            viewModel.onCommandChange("cena btc")

            viewModel.sendCommand()

            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)

            assertEquals(
                2,
                viewModel.uiState.value.messages.size
            )

            advanceTimeBy(1000L)

            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)

            assertEquals(
                3,
                viewModel.uiState.value.messages.size
            )
        }

    @Test
    fun sendCommand_blocksRepeatedRequest() =
        runViewModelTest {

            coinRepository.responseDelay = 1000L

            viewModel.onCommandChange("cena btc")

            viewModel.sendCommand()

            viewModel.onCommandChange("cena eth")

            viewModel.sendCommand()

            runCurrent()

            assertEquals(1, coinRepository.getCoinsCalls)

            advanceTimeBy(1000L)

            runCurrent()

            assertEquals(
                3,
                viewModel.uiState.value.messages.size
            )
        }

    @Test
    fun sendQuickCommand_success() =
        runViewModelTest {

            viewModel.sendQuickCommand("pomoc")

            runCurrent()

            val messages = viewModel.uiState.value.messages

            assertEquals("pomoc", messages[1].text)

            assertTrue(
                messages.last().text.contains("Dostępne komendy")
            )

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun sendQuickCommand_blocksWhileLoading() =
        runViewModelTest {

            coinRepository.responseDelay = 1000L

            viewModel.sendQuickCommand("cena btc")

            viewModel.sendQuickCommand("cena eth")

            runCurrent()

            advanceTimeBy(1000L)

            runCurrent()

            assertEquals(1, coinRepository.getCoinsCalls)

            assertEquals(
                3,
                viewModel.uiState.value.messages.size
            )
        }

    // Testy podstawowych komend

    @Test
    fun help_returnsAvailableCommands() =
        runViewModelTest {

            val answer = sendCommand("pomoc")

            assertTrue(answer.contains("Dostępne komendy"))

            assertTrue(answer.contains("cena btc"))

            assertTrue(answer.contains("moje alerty"))

            assertTrue(answer.contains("moje ulubione"))

            assertEquals(0, coinRepository.getCoinsCalls)
        }

    @Test
    fun unknownCommand_returnsError() =
        runViewModelTest {

            val answer = sendCommand("nieznana komenda")

            assertTrue(answer.isNotBlank())

            assertEquals(0, coinRepository.getCoinsCalls)

            assertEquals(0, alertRepository.addCalls)

            assertEquals(0, alertRepository.deleteByCoinCalls)

            assertFalse(viewModel.uiState.value.isLoading)

            assertFalse(
                viewModel.uiState.value.messages.last().isUser
            )
        }

    // Testy pobierania cen

    @Test
    fun price_returnsCoinPrice() =
        runViewModelTest {

            val answer = sendCommand("cena btc")

            assertEquals(
                "Aktualna cena Bitcoin (BTC) wynosi 60,000.00 USD.",
                answer
            )
        }

    @Test
    fun price_findsCoinById() =
        runViewModelTest {

            val answer = sendCommand("cena bitcoin")

            assertTrue(answer.contains("Bitcoin"))

            assertTrue(answer.contains("60,000.00 USD"))
        }

    @Test
    fun price_ignoresLetterCase() =
        runViewModelTest {

            val answer = sendCommand("CENA BTC")

            assertTrue(answer.contains("Bitcoin"))

            assertTrue(answer.contains("60,000.00 USD"))
        }

    @Test
    fun price_coinNotFound() =
        runViewModelTest {

            val answer = sendCommand("cena unknown")

            assertEquals(
                "Nie znaleziono kryptowaluty: unknown",
                answer
            )

            assertEquals(
                1,
                coinRepository.getCoinsByIdsCalls
            )

            assertEquals(
                "unknown",
                coinRepository.lastCoinIds
            )
        }

    @Test
    fun price_ambiguousSymbol() =
        runViewModelTest {

            coinRepository.coins = listOf(
                bitcoin,
                bitcoin.copy(
                    id = "another-bitcoin",
                    name = "Another Bitcoin"
                )
            )

            val answer = sendCommand("cena btc")

            assertEquals(
                "Znaleziono kilka kryptowalut z tym symbolem. Podaj ID.",
                answer
            )
        }

    @Test
    fun price_apiError() =
        runViewModelTest {

            coinRepository.error = Exception("Błąd API")

            val answer = sendCommand("cena btc")

            assertEquals(
                "Nie udało się pobrać ceny. Błąd API",
                answer
            )

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun price_rateLimitError() =
        runViewModelTest {

            coinRepository.error = HttpException(
                Response.error<String>(
                    429,
                    "Too Many Requests".toResponseBody()
                )
            )

            val answer = sendCommand("cena btc")

            assertEquals(
                "Przekroczono limit API. Spróbuj ponownie później.",
                answer
            )
        }

    // Test informacji o kryptowalucie

    @Test
    fun info_returnsCoinDetails() =
        runViewModelTest {

            val answer = sendCommand("info bitcoin")

            assertTrue(answer.contains("Bitcoin (BTC)"))

            assertTrue(answer.contains("Aktualna cena:"))

            assertTrue(answer.contains("60,000.00 USD"))

            assertTrue(answer.contains("Zmiana 24h: +5.00%"))

            assertTrue(answer.contains("Kapitalizacja:"))

            assertTrue(answer.contains("Wolumen 24h:"))
        }

    // Testy wzrostów i spadków

    @Test
    fun topGainers_sortsDescending() =
        runViewModelTest {

            val answer = sendCommand("top wzrosty")

            assertTrue(answer.contains("Największe wzrosty"))

            assertTrue(answer.contains("1. Solana"))

            assertTrue(answer.contains("2. Bitcoin"))

            assertFalse(answer.contains("Ethereum"))
        }

    @Test
    fun topLosers_filtersNegativeChanges() =
        runViewModelTest {

            val answer = sendCommand("top spadki")

            assertTrue(answer.contains("Największe spadki"))

            assertTrue(answer.contains("1. Ethereum"))

            assertFalse(answer.contains("Bitcoin"))

            assertFalse(answer.contains("Solana"))
        }

    @Test
    fun topMovers_limitsResultsToFive() =
        runViewModelTest {

            coinRepository.coins = (1..7).map { number ->

                CoinModel(
                    id = "coin-$number",
                    symbol = "c$number",
                    name = "Coin $number",
                    currentPrice = 100.0,
                    image = "",
                    priceChange24h = number.toDouble()
                )
            }

            val answer = sendCommand("top wzrosty")

            assertTrue(answer.contains("1. Coin 7"))

            assertTrue(answer.contains("5. Coin 3"))

            assertFalse(answer.contains("6. Coin"))

            assertFalse(answer.contains("Coin 1 ("))
        }

    // Testy ulubionych kryptowalut

    @Test
    fun favorites_emptyList() =
        runViewModelTest {

            val answer = sendCommand("moje ulubione")

            assertEquals(
                "Nie masz jeszcze ulubionych kryptowalut.",
                answer
            )
        }

    @Test
    fun favorites_returnsSortedList() =
        runViewModelTest {

            favoriteRepository.emitFavorites(
                listOf(
                    FavoriteCoin(
                        coinId = "ethereum",
                        coinName = "Ethereum"
                    ),
                    FavoriteCoin(
                        coinId = "bitcoin",
                        coinName = "Bitcoin"
                    )
                )
            )

            val answer = sendCommand("moje ulubione")

            assertEquals(
                "Twoje ulubione kryptowaluty (2):\n\n" +
                        "★ Bitcoin\n★ Ethereum",
                answer
            )
        }

    @Test
    fun favorites_repositoryError() =
        runViewModelTest {

            favoriteRepository.error =
                Exception("Błąd Firestore")

            val answer = sendCommand("moje ulubione")

            assertEquals(
                "Nie udało się pobrać ulubionych. Błąd Firestore",
                answer
            )
        }

    // Testy wyświetlania alertów

    @Test
    fun alerts_returnsAllAlerts() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(activeAlert, completedAlert)
            )

            val answer = sendCommand("moje alerty")

            assertTrue(answer.contains("Bitcoin"))

            assertTrue(answer.contains("Ethereum"))

            assertTrue(answer.contains("status: aktywny"))

            assertTrue(answer.contains("status: wykonany"))
        }

    @Test
    fun alerts_returnsOnlyActiveAlerts() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(activeAlert, completedAlert)
            )

            val answer = sendCommand("aktywne alerty")

            assertTrue(answer.contains("Bitcoin"))

            assertFalse(answer.contains("Ethereum"))
        }

    @Test
    fun alerts_returnsOnlyCompletedAlerts() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(activeAlert, completedAlert)
            )

            val answer = sendCommand("wykonane alerty")

            assertTrue(answer.contains("Ethereum"))

            assertFalse(answer.contains("Bitcoin"))
        }

    @Test
    fun alerts_emptyList() =
        runViewModelTest {

            val answer = sendCommand("moje alerty")

            assertEquals(
                "Nie masz jeszcze żadnych alertów.",
                answer
            )
        }

    // Testy tworzenia alertów przez bota

    @Test
    fun createAlert_aboveCondition() =
        runViewModelTest {

            val answer = sendCommand(
                "alert btc powyżej 70000"
            )

            val alert = checkNotNull(
                alertRepository.lastAddedAlert
            )

            assertEquals("bitcoin", alert.coinId)

            assertEquals("Bitcoin", alert.coinName)

            assertEquals(70000.0, alert.targetPrice, 0.001)

            assertEquals("ABOVE", alert.condition)

            assertTrue(alert.active)

            assertEquals(1, alertRepository.addCalls)

            assertTrue(
                answer.contains("Dodano alert dla Bitcoin")
            )
        }

    @Test
    fun createAlert_belowCondition() =
        runViewModelTest {

            sendCommand("alert eth poniżej 1500")

            val alert = checkNotNull(
                alertRepository.lastAddedAlert
            )

            assertEquals("ethereum", alert.coinId)

            assertEquals(1500.0, alert.targetPrice, 0.001)

            assertEquals("BELOW", alert.condition)
        }

    @Test
    fun createAlert_invalidPrice() =
        runViewModelTest {

            sendCommand("alert btc powyżej -100")

            assertEquals(0, alertRepository.addCalls)

            assertTrue(alertRepository.alerts.isEmpty())

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun createAlert_repositoryError() =
        runViewModelTest {

            alertRepository.suspendError =
                Exception("Błąd zapisu")

            val answer = sendCommand(
                "alert btc powyżej 70000"
            )

            assertEquals(
                "Nie udało się dodać alertu. Błąd zapisu",
                answer
            )

            assertTrue(alertRepository.alerts.isEmpty())
        }

    // Test usuwania alertów przez bota

    @Test
    fun deleteAlert_success() =
        runViewModelTest {

            alertRepository.emitAlerts(
                listOf(activeAlert, completedAlert)
            )

            val answer = sendCommand("usuń alert btc")

            assertEquals(
                "Usunięto alerty dla: bitcoin",
                answer
            )

            assertEquals(
                "bitcoin",
                alertRepository.lastDeletedCoinId
            )

            assertEquals(
                1,
                alertRepository.deleteByCoinCalls
            )

            assertEquals(
                listOf(completedAlert),
                alertRepository.alerts
            )
        }
}
