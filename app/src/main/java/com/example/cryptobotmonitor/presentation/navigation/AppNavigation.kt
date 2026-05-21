package com.example.cryptobotmonitor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import com.example.cryptobotmonitor.presentation.home.HomeScreen

// Główna nawigacja aplikacji
@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        // Ekran główny
        composable("home") {
            HomeScreen(
                onCoinClick = { coinId ->
                    navController.navigate("details/$coinId")
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
                }
            )
        }
    }
}