
package com.example.cryptobotmonitor.presentation.navigation
import com.example.cryptobotmonitor.presentation.alerts.CreateAlertScreen
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cryptobotmonitor.presentation.alerts.AlertsScreen
import com.example.cryptobotmonitor.presentation.auth.LoginScreen
import com.example.cryptobotmonitor.presentation.auth.RegisterScreen
import com.example.cryptobotmonitor.presentation.bot.BotScreen
import com.example.cryptobotmonitor.presentation.details.DetailsScreen
import com.example.cryptobotmonitor.presentation.home.HomeScreen
import com.example.cryptobotmonitor.presentation.home.HomeViewModel
import com.example.cryptobotmonitor.ui.theme.CryptoBackground
import com.example.cryptobotmonitor.ui.theme.CryptoTextPrimary
import com.google.firebase.auth.FirebaseAuth
import com.example.cryptobotmonitor.presentation.profile.ProfileScreen

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

    val backStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route

    // Ekrany z dolnym paskiem nawigacji
    val bottomBarRoutes = setOf(
        "home",
        "favorites",
        "alerts",
        "bot"
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    // Przechodzenie między głównymi ekranami
    val navigateToTab: (String) -> Unit = { route ->

        navController.navigate(route) {

            popUpTo("home") {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    // Przejście do szczegółów kryptowaluty
    val openDetails: (String) -> Unit = { coinId ->

        val encodedCoinId = Uri.encode(coinId)

        navController.navigate(
            "details/$encodedCoinId"
        ) {
            launchSingleTop = true
        }
    }

    Scaffold(

        modifier = Modifier.fillMaxSize(),

        containerColor = CryptoBackground,
        contentColor = CryptoTextPrimary,

        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        bottomBar = {

            if (showBottomBar) {

                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = navigateToTab
                )
            }
        }

    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {

            // Główny ekran rynku
            composable("home") { entry ->

                val homeViewModel: HomeViewModel =
                    viewModel(viewModelStoreOwner = entry)

                HomeScreen(
                    viewModel = homeViewModel,
                    onlyFavorites = false,
                    onCoinClick = openDetails,

                    onProfileClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Ekran ulubionych kryptowalut
            // Ekran ulubionych kryptowalut
            composable("favorites") { entry ->

                val homeEntry = remember(entry) {
                    navController.getBackStackEntry("home")
                }

                // Wspólny ViewModel dla rynku i ulubionych
                val homeViewModel: HomeViewModel =
                    viewModel(viewModelStoreOwner = homeEntry)

                HomeScreen(
                    viewModel = homeViewModel,
                    onlyFavorites = true,
                    onCoinClick = openDetails,

                    onProfileClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Główny ekran alertów
            // Główny ekran alertów
            composable("alerts") {

                AlertsScreen(
                    onCreateAlertClick = {

                        navController.navigate("createAlert") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Szczegóły kryptowaluty
            composable(
                route = "details/{coinId}",

                arguments = listOf(

                    navArgument("coinId") {
                        type = NavType.StringType
                    }
                )

            ) { entry ->

                val coinId =
                    entry.arguments?.getString("coinId") ?: ""

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
                            "createAlert?coinId=$encodedCoinId" +
                                    "&coinName=$encodedCoinName"
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }


// Formularz tworzenia alertu
            composable(
                route = "createAlert?coinId={coinId}&coinName={coinName}",

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

            ) { entry ->

                val coinId =
                    entry.arguments?.getString("coinId") ?: ""

                val coinName =
                    entry.arguments?.getString("coinName") ?: ""

                CreateAlertScreen(
                    initialCoinId = coinId,
                    initialCoinName = coinName,

                    onBackClick = {
                        navController.popBackStack()
                    },

                    onAlertSaved = {
                        navController.popBackStack()
                    }
                )
            }






            // Ekran bota tekstowego
            composable("bot") {
                BotScreen()
            }

            // Ekran profilu użytkownika
            composable("profile") {

                ProfileScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },

                    onLogoutClick = onLogout
                )
            }
        }
    }
}

