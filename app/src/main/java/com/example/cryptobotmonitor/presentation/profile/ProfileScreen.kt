
package com.example.cryptobotmonitor.presentation.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.notification.NotificationHelper
import com.example.cryptobotmonitor.ui.components.CryptoCard
import com.example.cryptobotmonitor.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    var notificationsEnabled by remember {
        mutableStateOf(
            NotificationHelper.canSendNotifications(context)
        )
    }

    // Pobranie aktualnej wersji aplikacji
    val appVersion = remember(context) {

        try {
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            ).versionName ?: "—"

        } catch (e: Exception) {
            "—"
        }
    }

    // Aktualizacja statusu powiadomień
    LaunchedEffect(Unit) {

        while (true) {

            notificationsEnabled =
                NotificationHelper.canSendNotifications(context)

            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {

        // Powrót do poprzedniego ekranu
        TextButton(
            onClick = onBackClick,
            contentPadding = PaddingValues(0.dp)
        ) {

            Text(
                text = "‹  Wróć",
                color = CryptoAccent,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Profil",
                style = MaterialTheme.typography.headlineLarge,
                color = CryptoTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ustawienia konta",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Informacje o użytkowniku
            ProfileAccountHeader(
                email = state.email
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Statystyki użytkownika
            ProfileStatisticsCard(
                state = state,
                onRefreshClick = viewModel::loadProfile
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Ustawienia aplikacji
            CryptoCard {

                Text(
                    text = "Ustawienia",
                    style = MaterialTheme.typography.titleMedium,
                    color = CryptoTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Przejście do ustawień powiadomień
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            val intent = if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.O
                            ) {

                                Intent(
                                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                ).apply {

                                    putExtra(
                                        Settings.EXTRA_APP_PACKAGE,
                                        context.packageName
                                    )
                                }

                            } else {

                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                ).apply {

                                    data = Uri.parse(
                                        "package:${context.packageName}"
                                    )
                                }
                            }

                            context.startActivity(intent)
                        }
                        .padding(vertical = 12.dp),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Powiadomienia",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CryptoTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (notificationsEnabled) {
                                "Włączone"
                            } else {
                                "Wyłączone"
                            },

                            color = if (notificationsEnabled) {
                                CryptoPositive
                            } else {
                                CryptoNegative
                            },

                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "›",
                        color = CryptoAccent,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                HorizontalDivider(color = CryptoBorder)

                ProfileInfoRow(
                    label = "Motyw aplikacji",
                    value = "Ciemny"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Informacje o aplikacji
            CryptoCard {

                Text(
                    text = "Informacje o aplikacji",
                    style = MaterialTheme.typography.titleMedium,
                    color = CryptoTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProfileInfoRow(
                    label = "Nazwa",
                    value = "CryptoBotMonitor"
                )

                HorizontalDivider(color = CryptoBorder)

                ProfileInfoRow(
                    label = "Wersja",
                    value = appVersion
                )

                HorizontalDivider(color = CryptoBorder)

                ProfileInfoRow(
                    label = "Źródło danych",
                    value = "CoinGecko API"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Przycisk wylogowania
            OutlinedButton(
                onClick = {
                    showLogoutDialog = true
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),

                shape = RoundedCornerShape(12.dp),

                border = BorderStroke(
                    1.dp,
                    CryptoNegative
                ),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CryptoNegative
                )
            ) {

                Text(
                    text = "Wyloguj się",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CryptoBotMonitor © 2026",
                style = MaterialTheme.typography.bodySmall,
                color = CryptoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Potwierdzenie wylogowania
    if (showLogoutDialog) {

        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },

            containerColor = CryptoSurface,

            title = {
                Text(
                    text = "Wylogować się?",
                    color = CryptoTextPrimary
                )
            },

            text = {
                Text(
                    text = "Czy na pewno chcesz wylogować się z konta?",
                    color = CryptoTextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {

                    Text(
                        text = "Wyloguj się",
                        color = CryptoNegative
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {

                    Text(
                        text = "Anuluj",
                        color = CryptoAccent
                    )
                }
            }
        )
    }
}

// Dane zalogowanego użytkownika
@Composable
private fun ProfileAccountHeader(
    email: String
) {

    val initial = email
        .firstOrNull()
        ?.toString()
        ?.uppercase(Locale.ROOT)
        ?: "?"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Awatar z pierwszą literą adresu e-mail
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = CryptoPrimary,
                    shape = CircleShape
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = initial,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Twoje konto",
            style = MaterialTheme.typography.titleLarge,
            color = CryptoTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = email.ifBlank {
                "Brak adresu e-mail"
            },

            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (email.isNotBlank()) {

            Text(
                text = "● Zalogowano",
                color = CryptoPositive,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Karta ze statystykami użytkownika
@Composable
private fun ProfileStatisticsCard(
    state: ProfileUiState,
    onRefreshClick: () -> Unit
) {

    CryptoCard {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Twoja aktywność",
                style = MaterialTheme.typography.titleMedium,
                color = CryptoTextPrimary,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onRefreshClick,
                enabled = !state.isLoading
            ) {

                Text(
                    text = "Odśwież",
                    color = if (state.isLoading) {
                        CryptoTextSecondary
                    } else {
                        CryptoAccent
                    },

                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {

            state.isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),

                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = CryptoAccent
                    )
                }
            }

            state.error != null -> {

                Text(
                    text = state.error,
                    color = CryptoNegative,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            else -> {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    ProfileStatItem(
                        title = "Ulubione",
                        value = state.favoritesCount,
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatItem(
                        title = "Aktywne",
                        value = state.activeAlertsCount,
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatItem(
                        title = "Wykonane",
                        value = state.completedAlertsCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Pojedynczy element statystyk
@Composable
private fun ProfileStatItem(
    title: String,
    value: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                color = CryptoBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 4.dp,
                vertical = 16.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value.toString(),
            color = CryptoAccent,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

// Wiersz z informacją o aplikacji
@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
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
            color = CryptoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = CryptoTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}
