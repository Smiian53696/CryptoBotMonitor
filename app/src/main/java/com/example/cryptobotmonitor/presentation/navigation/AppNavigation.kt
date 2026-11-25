
package com.example.cryptobotmonitor.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cryptobotmonitor.presentation.alerts.AlertsScreen
import com.example.cryptobotmonitor.presentation.auth.LoginScreen
import com.example.cryptobotmonitor.presentation.auth.RegisterScreen
import com.example.cryptobotmonitor.presentation.bot.BotScreen
import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import com.example.cryptobotmonitor.presentation.home.HomeScreen
import com.google.firebase.auth.FirebaseAuth

// Główna nawigacja aplikacji
@Composable
fun AppNavigation() {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var userId by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    // Obserwowanie stanu logowania użytkownika
    DisposableEffect(auth) {

        val listener = FirebaseAuth.AuthStateListener {
                firebaseAuth ->

            userId = firebaseAuth.currentUser?.uid
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    // Oddzielna nawigacja dla każdego użytkownika
    key(userId) {

        if (userId == null) {
            AuthNavigation()
        } else {
            UserNavigation(
                onLogout = {
                    auth.signOut()
                }
            )
        }
    }
}

// Nawigacja użytkownika niezalogowanego
@Composable
private fun AuthNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        // Ekran logowania
        composable("login") {

            LoginScreen(
                onLoginSuccess = {
                    // Zmiana ekranu przez AuthStateListener
                },
                onRegisterClick = {
                    navController.navigate("register") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Ekran rejestracji
        composable("register") {

            RegisterScreen(
                onRegisterSuccess = {
                    // Zmiana ekranu przez AuthStateListener
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Nawigacja zalogowanego użytkownika
@Composable
private fun UserNavigation(
    onLogout: () -> Unit
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        // Ekran główny
        composable("home") {

            HomeScreen(
                onCoinClick = { coinId ->

                    val encodedCoinId = Uri.encode(coinId)

                    navController.navigate(
                        "details/$encodedCoinId"
                    ) {
                        launchSingleTop = true
                    }
                },
                onAlertsClick = {

                    navController.navigate("alerts") {
                        launchSingleTop = true
                    }
                },
                onBotClick = {

                    navController.navigate("bot") {
                        launchSingleTop = true
                    }
                },
                onLogoutClick = onLogout
            )
        }

        // Ekran alertów cenowych
        composable(
            route = "alerts?coinId={coinId}&coinName={coinName}",
            arguments = listOf(
                navArgument("coinId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("coinName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val coinId =
                backStackEntry.arguments?.getString("coinId") ?: ""

            val coinName =
                backStackEntry.arguments?.getString("coinName") ?: ""

            AlertsScreen(
                initialCoinId = coinId,
                initialCoinName = coinName,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Ekran szczegółów kryptowaluty
        composable(
            route = "details/{coinId}",
            arguments = listOf(
                navArgument("coinId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val coinId =
                backStackEntry.arguments?.getString("coinId") ?: ""

            DetailsScreen(
                coinId = coinId,
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateAlertClick = {
                        selectedCoinId, selectedCoinName ->

                    val encodedCoinId =
                        Uri.encode(selectedCoinId)

                    val encodedCoinName =
                        Uri.encode(selectedCoinName)

                    navController.navigate(
                        "alerts?coinId=$encodedCoinId&coinName=$encodedCoinName"
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Ekran bota tekstowego
        composable("bot") {

            BotScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
