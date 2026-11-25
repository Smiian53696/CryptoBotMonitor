package com.example.cryptobotmonitor.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cryptobotmonitor.data.model.CoinModel
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Szczegóły",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onBackClick
            ) {
                Text("Wróć")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.error != null -> {
                Text(
                    text = state.error ?: "Nieznany błąd",
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.loadCoin(coinId)
                    }
                ) {
                    Text("Spróbuj ponownie")
                }
            }

            state.coin != null -> {

                CoinDetailsContent(
                    coin = state.coin!!,
                    chartPoints = state.chartPoints,
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
    onCreateAlertClick: (String, String) -> Unit
) {
    val priceChange = coin.priceChange24h ?: 0.0

    val priceChangeColor =
        if (priceChange >= 0) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFF44336)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = coin.image,
                    contentDescription = coin.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column {
                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = coin.symbol.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Wykres ceny z ostatnich 7 dni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PriceLineChart(
                points = chartPoints,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                label = "Aktualna cena",
                value = "${formatNumber(coin.currentPrice)} USD"
            )

            DetailRow(
                label = "Zmiana 24h",
                value = "${formatNumber(priceChange)}%",
                valueColor = priceChangeColor
            )

            DetailRow(
                label = "Najwyższa cena 24h",
                value = "${formatNumber(coin.high24h)} USD"
            )

            DetailRow(
                label = "Najniższa cena 24h",
                value = "${formatNumber(coin.low24h)} USD"
            )

            DetailRow(
                label = "Kapitalizacja",
                value = "${formatNumber(coin.marketCap)} USD"
            )

            DetailRow(
                label = "Wolumen",
                value = "${formatNumber(coin.totalVolume)} USD"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onCreateAlertClick(coin.id, coin.name)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Utwórz alert")
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray
        )

        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatNumber(value: Double?): String {
    if (value == null) {
        return "-"
    }

    return String.format(
        Locale.US,
        "%,.2f",
        value
    )
}