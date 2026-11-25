
package com.example.cryptobotmonitor.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val favoriteError by viewModel.favoriteError.collectAsState()
    val pendingFavoriteIds by viewModel.pendingFavoriteIds.collectAsState()

    // Dane wyszukiwania
    val searchQuery by viewModel.searchQuery.collectAsState()
    val displayedCoins by viewModel.filteredCoins.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Górna część ekranu
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

            OutlinedButton(onClick = onLogoutClick) {
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

        Spacer(modifier = Modifier.height(12.dp))

        // Pole wyszukiwania kryptowalut
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                viewModel.setSearchQuery(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Szukaj kryptowaluty")
            },
            placeholder = {
                Text("np. Bitcoin lub BTC")
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.clearSearch()
                        }
                    ) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Przyciski do filtrowania listy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected = !showOnlyFavorites,
                onClick = {
                    viewModel.setShowOnlyFavorites(false)
                },
                label = {
                    Text("Wszystkie")
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = showOnlyFavorites,
                onClick = {
                    viewModel.setShowOnlyFavorites(true)
                },
                label = {
                    Text("Ulubione (${favoriteIds.size})")
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Błąd związany z ulubionymi
        favoriteError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {

            isLoading -> {
                CircularProgressIndicator()
            }

            error != null && coins.isEmpty() -> {

                Text(text = error ?: "Nieznany błąd")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.loadCoins()
                    }
                ) {
                    Text("Spróbuj ponownie")
                }
            }


            displayedCoins.isEmpty() -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            searchQuery.isNotBlank() ->
                                "Nie znaleziono kryptowaluty."

                            showOnlyFavorites && favoriteIds.isEmpty() ->
                                "Brak ulubionych kryptowalut."

                            showOnlyFavorites ->
                                "Brak ulubionych na aktualnej liście."

                            else ->
                                "Brak kryptowalut do wyświetlenia."
                        },
                        color = Color.Gray
                    )
                }
            }


            else -> {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = displayedCoins,
                        key = { it.id }
                    ) { coin ->

                        CoinItem(
                            coin = coin,
                            isFavorite = coin.id in favoriteIds,
                            isFavoritePending =
                                coin.id in pendingFavoriteIds,
                            onFavoriteClick = {
                                viewModel.toggleFavorite(coin)
                            },
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
    isFavorite: Boolean,
    isFavoritePending: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {

    // Kolor zależny od zmiany ceny
    val priceColor =
        if ((coin.priceChange24h ?: 0.0) >= 0)
            Color(0xFF4CAF50)
        else
            Color(0xFFF44336)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Logo kryptowaluty
            AsyncImage(
                model = coin.image,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = coin.symbol.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = "$${String.format("%.2f", coin.currentPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${String.format("%.2f", coin.priceChange24h ?: 0.0)}%",
                    color = priceColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Dodawanie i usuwanie z ulubionych
            IconButton(
                onClick = onFavoriteClick,
                enabled = !isFavoritePending
            ) {
                if (isFavoritePending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isFavorite) "★" else "☆",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isFavorite) {
                            Color(0xFFFFB300)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
