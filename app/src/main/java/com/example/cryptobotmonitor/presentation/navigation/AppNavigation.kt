package com.example.cryptobotmonitor.presentation.navigation

import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cryptobotmonitor.presentation.alerts.AlertsScreen
import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import com.example.cryptobotmonitor.presentation.home.HomeScreen
import com.example.cryptobotmonitor.presentation.auth.LoginScreen
import com.example.cryptobotmonitor.presentation.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.cryptobotmonitor.presentation.bot.BotScreen
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

// Główna nawigacja aplikacji
@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val startDestination =
        if (FirebaseAuth.getInstance().currentUser != null) {
            "home"
        } else {
            "login"
        }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Login ekran
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }
        // Register ekran
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        // Ekran główny
        composable("home") {
            HomeScreen(
                onCoinClick = { coinId ->
                    navController.navigate("details/$coinId")
                },
                onAlertsClick = {
                    navController.navigate("alerts")
                },
                onBotClick = {
                    navController.navigate("bot")
                },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()

                    navController.navigate("login") {
                        popUpTo("home") {
                            inclusive = true
                        }
                    }
                }
            )
        }
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

            val coinId = backStackEntry.arguments?.getString("coinId") ?: ""
            val coinName = backStackEntry.arguments?.getString("coinName") ?: ""

            AlertsScreen(
                initialCoinId = coinId,
                initialCoinName = coinName,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Ekran szczegółów kryptowaluty
        composable("details/{coinId}") { backStackEntry ->

            val coinId = backStackEntry.arguments?.getString("coinId") ?: ""

            DetailsScreen(
                coinId = coinId,
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateAlertClick = { selectedCoinId, selectedCoinName ->

                    val encodedCoinId = Uri.encode(selectedCoinId)
                    val encodedCoinName = Uri.encode(selectedCoinName)

                    navController.navigate(
                        "alerts?coinId=$encodedCoinId&coinName=$encodedCoinName"
                    )
                }
            )
        }
        composable("bot") {
            BotScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}