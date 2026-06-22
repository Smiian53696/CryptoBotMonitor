package com.example.cryptobotmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cryptobotmonitor.presentation.home.HomeScreen
import com.example.cryptobotmonitor.presentation.navigation.AppNavigation
import com.example.cryptobotmonitor.ui.theme.CryptoBotMonitorTheme

import com.example.cryptobotmonitor.worker.PriceAlertWorker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Uruchomienie sprawdzania alertów w tle
        PriceAlertWorker.schedule(this)

        enableEdgeToEdge()

        setContent {
            CryptoBotMonitorTheme {
                RequestNotificationPermission()
                AppNavigation()
            }
        }
    }
}
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        // Od Androida 13 trzeba zapytać o zgodę na powiadomienia
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permission != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello $name!",

    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CryptoBotMonitorTheme {
        Greeting("Android")
    }
}