package com.example.cryptobotmonitor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import com.example.cryptobotmonitor.presentation.home.HomeScreen
import com.example.cryptobotmonitor.presentation.auth.LoginScreen
import com.example.cryptobotmonitor.presentation.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

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