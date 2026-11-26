package com.example.cryptobotmonitor.presentation.home
import com.example.cryptobotmonitor.domain.CoinFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cryptobotmonitor.data.model.CoinModel
import com.example.cryptobotmonitor.ui.components.CryptoButton
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.components.CryptoTextField
import com.example.cryptobotmonitor.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onlyFavorites: Boolean = false,
    onCoinClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {

    val coins by viewModel.coins.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val favoriteError by viewModel.favoriteError.collectAsState()

    val pendingFavoriteIds by
    viewModel.pendingFavoriteIds.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()

    // Filtrowanie według aktualnego ekranu i wyszukiwania
    // Filtrowanie kryptowalut
    val displayedCoins = CoinFilter.filterCoins(
        coins = coins,
        favoriteIds = favoriteIds,
        onlyFavorites = onlyFavorites,
        query = searchQuery
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Nagłówek ekranu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = if (onlyFavorites) {
                        "Ulubione"
                    } else {
                        "Rynek kryptowalut"
                    },

                    style = MaterialTheme.typography.headlineLarge,
                    color = CryptoTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (onlyFavorites) {
                        "Twoje obserwowane kryptowaluty"
                    } else {
                        "Aktualne ceny · CoinGecko"
                    },

                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoTextSecondary
                )
            }

            ProfileIconButton(
                onClick = onProfileClick
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Podsumowanie aktualnej ceny Bitcoina
        if (!onlyFavorites && searchQuery.isBlank()) {

            val bitcoin = coins.firstOrNull {
                it.id == "bitcoin"
            }

            if (bitcoin != null) {

                BitcoinSummaryCard(
                    coin = bitcoin,
                    onClick = {
                        onCoinClick(bitcoin.id)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Wyszukiwanie kryptowalut
        CryptoTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = "Szukaj kryptowaluty...",

            trailingIcon = {

                if (searchQuery.isNotEmpty()) {

                    IconButton(
                        onClick = viewModel::clearSearch
                    ) {

                        Text(
                            text = "×",
                            color = CryptoAccent,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics {
                                contentDescription = "Wyczyść wyszukiwanie"
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Informacja o liczbie kryptowalut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = if (onlyFavorites) {
                    "Obserwowane"
                } else {
                    "Wszystkie"
                },
                style = MaterialTheme.typography.titleMedium,
                color = CryptoTextPrimary
            )

            Text(
                text = "${displayedCoins.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Informacja o błędzie ulubionych
        favoriteError?.let { message ->

            Text(
                text = message,
                color = CryptoNegative,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Informacja o błędzie odświeżania danych
        if (error != null && coins.isNotEmpty()) {

            Text(
                text = error ?: "",
                color = CryptoNegative,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Główna lista kryptowalut
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when {

                isLoading -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                        color = CryptoAccent
                    )
                }

                error != null && coins.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = error ?: "Nieznany błąd",
                            color = CryptoNegative,
                            textAlign = TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        CryptoButton(
                            text = "Spróbuj ponownie",
                            onClick = viewModel::loadCoins
                        )
                    }
                }

                displayedCoins.isEmpty() -> {

                    Column(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = if (onlyFavorites) {
                                "☆"
                            } else {
                                "⌕"
                            },

                            style =
                                MaterialTheme.typography.displayLarge,

                            color = CryptoAccent
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = when {

                                searchQuery.isNotBlank() ->
                                    "Nie znaleziono kryptowaluty."

                                onlyFavorites &&
                                        favoriteIds.isEmpty() ->
                                    "Brak ulubionych kryptowalut."

                                onlyFavorites ->
                                    "Brak ulubionych na aktualnej liście."

                                else ->
                                    "Brak kryptowalut do wyświetlenia."
                            },

                            style =
                                MaterialTheme.typography.bodyMedium,

                            textAlign = TextAlign.Center,
                            color = CryptoTextSecondary
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp),

                        contentPadding = PaddingValues(
                            bottom = 16.dp
                        )
                    ) {

                        items(
                            items = displayedCoins,
                            key = { it.id }
                        ) { coin ->

                            CoinItem(
                                coin = coin,

                                isFavorite =
                                    coin.id in favoriteIds,

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
}

// Karta z aktualną ceną Bitcoina
@Composable
private fun BitcoinSummaryCard(
    coin: CoinModel,
    onClick: () -> Unit
) {

    val change = coin.priceChange24h

    CryptoCard(
        onClick = onClick,
        showBorder = true
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = coin.image,
                contentDescription = "Bitcoin",

                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Bitcoin · BTC",
                color = CryptoTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = formatMarketPrice(coin.currentPrice),
            color = CryptoTextPrimary,
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (change != null && change.isFinite()) {
                "${formatMarketChange(change)} · 24h"
            } else {
                "Brak danych · 24h"
            },

            color = marketChangeColor(change),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Element listy kryptowalut
@Composable
fun CoinItem(
    coin: CoinModel,
    isFavorite: Boolean,
    isFavoritePending: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {

    val change = coin.priceChange24h

    CryptoCard(
        onClick = onClick
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Logo kryptowaluty
            AsyncImage(
                model = coin.image,
                contentDescription = coin.name,

                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Nazwa i symbol kryptowaluty
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = coin.symbol.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Aktualna cena i zmiana procentowa
            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = formatMarketPrice(
                        coin.currentPrice
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatMarketChange(change),
                    color = marketChangeColor(change),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Dodawanie i usuwanie z ulubionych
            IconButton(

                onClick = onFavoriteClick,
                enabled = !isFavoritePending,

                modifier = Modifier.semantics {

                    contentDescription = if (isFavorite) {
                        "Usuń ${coin.name} z ulubionych"
                    } else {
                        "Dodaj ${coin.name} do ulubionych"
                    }
                }

            ) {

                if (isFavoritePending) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CryptoAccent,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text = if (isFavorite) {
                            "★"
                        } else {
                            "☆"
                        },

                        style =
                            MaterialTheme.typography.headlineMedium,

                        color = if (isFavorite) {
                            CryptoAccent
                        } else {
                            CryptoTextSecondary
                        }
                    )
                }
            }
        }
    }
}

// Formatowanie ceny kryptowaluty
private fun formatMarketPrice(
    price: Double?
): String {

    if (price == null || !price.isFinite()) {
        return "—"
    }

    val formatted = when {

        price >= 1.0 ->
            String.format(Locale.US, "%,.2f", price)

        price >= 0.01 ->
            String.format(Locale.US, "%.4f", price)

        else ->
            String.format(Locale.US, "%.8f", price)
    }

    return "$" + formatted
}

// Formatowanie zmiany procentowej
private fun formatMarketChange(
    change: Double?
): String {

    if (change == null || !change.isFinite()) {
        return "—"
    }

    return String.format(
        Locale.US,
        "%+.2f%%",
        change
    )
}

// Kolor zależny od zmiany ceny
private fun marketChangeColor(
    change: Double?
): androidx.compose.ui.graphics.Color {

    return when {

        change == null || !change.isFinite() ->
            CryptoTextSecondary

        change > 0 ->
            CryptoPositive

        change < 0 ->
            CryptoNegative

        else ->
            CryptoTextSecondary
    }

}

// Przycisk otwierający profil użytkownika
@Composable
private fun ProfileIconButton(
    onClick: () -> Unit
) {

    IconButton(
        onClick = onClick,

        modifier = Modifier
            .size(48.dp)
            .background(
                color = CryptoSurface,
                shape = CircleShape
            )
            .semantics {
                contentDescription = "Otwórz profil użytkownika"
            }
    ) {

        Canvas(
            modifier = Modifier.size(24.dp)
        ) {

            val w = size.width
            val h = size.height

            // Głowa użytkownika
            drawCircle(
                color = CryptoAccent,
                radius = w * 0.17f,

                center = Offset(
                    w * 0.5f,
                    h * 0.25f
                ),

                style = Stroke(
                    width = 1.8.dp.toPx()
                )
            )

            // Sylwetka użytkownika
            val path = Path().apply {

                moveTo(w * 0.12f, h * 0.90f)

                cubicTo(
                    w * 0.12f, h * 0.45f,
                    w * 0.88f, h * 0.45f,
                    w * 0.88f, h * 0.90f
                )
            }

            drawPath(
                path = path,
                color = CryptoAccent,

                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

