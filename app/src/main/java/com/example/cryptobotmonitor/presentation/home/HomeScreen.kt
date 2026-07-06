package com.example.cryptobotmonitor.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
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
import androidx.compose.material3.OutlinedButton
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onCoinClick: (String) -> Unit,
    onAlertsClick: () -> Unit,
    onBotClick: () -> Unit,
    onLogoutClick: () -> Unit
) {

    val coins by viewModel.coins.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Górna część ekranu
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CryptoBot Monitor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onLogoutClick
                ) {
                    Text("Wyloguj")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAlertsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Alerty")
                }

                Button(
                    onClick = onBotClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bot")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {

            isLoading -> {
                CircularProgressIndicator()
            }

            error != null -> {

                Text(text = error ?: "Nieznany błąd")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.loadCoins()
                    }
                ) {
                    Text(text = "Spróbuj ponownie")
                }
            }

            else -> {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(coins) { coin ->

                        CoinItem(
                            coin = coin,
                            onClick = {
                                onCoinClick(coin.id)
                            }
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun CoinItem(
    coin: CoinModel,
    onClick: () -> Unit
) {

    // Kolor zależny od wzrostu lub spadku ceny
    val priceColor =
        if ((coin.priceChange24h ?: 0.0) >= 0)
            Color(0xFF4CAF50)
        else
            Color(0xFFF44336)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )

    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Logo kryptowaluty
                AsyncImage(
                    model = coin.image,
                    contentDescription = coin.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column {

                    // Nazwa kryptowaluty
                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Symbol kryptowaluty
                    Text(
                        text = coin.symbol.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                // Aktualna cena
                Text(
                    text = "$${String.format("%.2f", coin.currentPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Zmiana ceny w %
                Text(
                    text = "${String.format("%.2f", coin.priceChange24h ?: 0.0)}%",
                    color = priceColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}