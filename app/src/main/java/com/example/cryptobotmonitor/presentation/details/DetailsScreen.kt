
package com.example.cryptobotmonitor.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.ui.components.CryptoButton
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.components.CryptoOutlinedButton
import com.example.cryptobotmonitor.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@Composable
fun DetailsScreen(
    coinId: String,
    onBackClick: () -> Unit,
    onCreateAlertClick: (String, String) -> Unit,
    viewModel: DetailsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(coinId) {
        viewModel.loadCoin(coinId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {

        // Powrót do poprzedniego ekranu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackClick,
                contentPadding = PaddingValues(
                    horizontal = 0.dp,
                    vertical = 8.dp
                )
            ) {
                Text(
                    text = "‹  Wróć",
                    color = CryptoAccent,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        when {

            // Ładowanie danych kryptowaluty
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CryptoAccent
                    )
                }
            }

            // Błąd pobierania danych
            state.error != null -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "Nieznany błąd",
                        color = CryptoNegative,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CryptoButton(
                        text = "Spróbuj ponownie",
                        onClick = {
                            viewModel.loadCoin(coinId)
                        }
                    )
                }
            }

            state.coin != null -> {

                CoinDetailsContent(
                    coin = state.coin!!,
                    chartPoints = state.chartPoints,
                    selectedPeriod = state.selectedPeriod,
                    isChartLoading = state.isChartLoading,
                    chartError = state.chartError,
                    onPeriodChange = viewModel::changePeriod,
                    onRetryChart = viewModel::retryChart,
                    onCreateAlertClick = onCreateAlertClick
                )
            }
        }
    }
}

@Composable
fun CoinDetailsContent(
    coin: CoinModel,
    chartPoints: List<ChartPoint>,
    selectedPeriod: String,
    isChartLoading: Boolean,
    chartError: String?,
    onPeriodChange: (String) -> Unit,
    onRetryChart: () -> Unit,
    onCreateAlertClick: (String, String) -> Unit
) {

    val priceChange = coin.priceChange24h

    val priceChangeColor = when {
        priceChange == null || !priceChange.isFinite() ->
            CryptoTextSecondary

        priceChange > 0 ->
            CryptoPositive

        priceChange < 0 ->
            CryptoNegative

        else ->
            CryptoTextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(8.dp))

        // Logo i nazwa kryptowaluty
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = coin.image,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = CryptoTextPrimary
                )

                Text(
                    text = "${coin.symbol.uppercase(Locale.ROOT)} / USD",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CryptoTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Aktualna cena
        Text(
            text = "Aktualna cena",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoTextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatDetailsPrice(coin.currentPrice),
            style = MaterialTheme.typography.displayLarge,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (
                priceChange != null &&
                priceChange.isFinite()
            ) {
                String.format(
                    Locale.US,
                    "%+.2f%% w ciągu 24h",
                    priceChange
                )
            } else {
                "Brak danych o zmianie ceny"
            },
            color = priceChangeColor,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Historia ceny
        Text(
            text = "Historia ceny",
            style = MaterialTheme.typography.titleLarge,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        ChartPeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Karta z wykresem
        CryptoCard(
            showBorder = true
        ) {

            when {

                isChartLoading -> {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        CircularProgressIndicator(
                            color = CryptoAccent
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Ładowanie wykresu...",
                            color = CryptoTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                chartError != null -> {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            text = chartError,
                            color = CryptoNegative,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CryptoOutlinedButton(
                            text = "Spróbuj ponownie",
                            onClick = onRetryChart
                        )
                    }
                }

                else -> {

                    PriceLineChart(
                        points = chartPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statystyki kryptowaluty
        CryptoCard {

            Text(
                text = "Statystyki 24h",
                style = MaterialTheme.typography.titleLarge,
                color = CryptoTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(
                label = "Najwyższa cena",
                value = formatDetailsPrice(coin.high24h)
            )

            HorizontalDivider(color = CryptoBorder)

            DetailRow(
                label = "Najniższa cena",
                value = formatDetailsPrice(coin.low24h)
            )

            HorizontalDivider(color = CryptoBorder)

            DetailRow(
                label = "Kapitalizacja",
                value = formatMarketValue(coin.marketCap)
            )

            HorizontalDivider(color = CryptoBorder)

            DetailRow(
                label = "Wolumen 24h",
                value = formatMarketValue(coin.totalVolume)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tworzenie alertu dla wybranej kryptowaluty
        CryptoButton(
            text = "Dodaj alert cenowy",
            onClick = {
                onCreateAlertClick(
                    coin.id,
                    coin.name
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Dane pochodzą z CoinGecko API",
            style = MaterialTheme.typography.bodySmall,
            color = CryptoTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Wiersz ze statystykami kryptowaluty
@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = CryptoTextPrimary
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoTextSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

// Wybór okresu wykresu
@Composable
fun ChartPeriodSelector(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit
) {

    val periods = listOf(
        "1" to "1D",
        "7" to "7D",
        "30" to "30D",
        "90" to "90D"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CryptoSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        periods.forEach { (value, label) ->

            val selected = selectedPeriod == value

            TextButton(
                onClick = {
                    onPeriodChange(value)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) {
                        CryptoPrimary
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        Color.White
                    } else {
                        CryptoTextSecondary
                    }
                )
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// Formatowanie ceny
private fun formatDetailsPrice(
    price: Double?
): String {

    if (price == null || !price.isFinite()) {
        return "—"
    }

    val pattern = when {
        abs(price) >= 1.0 -> "%,.2f"
        abs(price) >= 0.01 -> "%.4f"
        else -> "%.8f"
    }

    return "$" + String.format(
        Locale.US,
        pattern,
        price
    )
}

// Skrócony zapis dużych wartości
private fun formatMarketValue(
    value: Double?
): String {

    if (value == null || !value.isFinite()) {
        return "—"
    }

    return when {

        abs(value) >= 1_000_000_000_000 ->
            "$" + String.format(
                Locale.US,
                "%.2f T",
                value / 1_000_000_000_000
            )

        abs(value) >= 1_000_000_000 ->
            "$" + String.format(
                Locale.US,
                "%.2f B",
                value / 1_000_000_000
            )

        abs(value) >= 1_000_000 ->
            "$" + String.format(
                Locale.US,
                "%.2f M",
                value / 1_000_000
            )

        else -> formatDetailsPrice(value)
    }
}

// Formatowanie liczby
fun formatNumber(value: Double?): String {

    if (value == null || !value.isFinite()) {
        return "—"
    }

    return String.format(
        Locale.US,
        "%,.2f",
        value
    )
}
