
package com.example.cryptobotmonitor.integration.firebase

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.tasks.await
import java.util.UUID

// Środowisko testowe Firebase
class FirebaseTestEnvironment {

    lateinit var app: FirebaseApp
        private set

    lateinit var auth: FirebaseAuth
        private set

    lateinit var firestore: FirebaseFirestore
        private set

    // Konfiguracja emulatorów Firebase
    fun initialize() {

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val options = FirebaseOptions.Builder()
            .setProjectId("demo-cryptobotmonitor")
            .setApplicationId(
                "1:1234567890:android:1234567890abcdef"
            )
            .setApiKey("fake-api-key")
            .build()

        // Osobna instancja Firebase dla każdego testu
        app = FirebaseApp.initializeApp(
            context,
            options,
            "test-${UUID.randomUUID()}"
        )

        // Emulator Authentication
        auth = FirebaseAuth.getInstance(app)

        auth.useEmulator(
            "10.0.2.2",
            9099
        )

        // Emulator Firestore
        firestore = FirebaseFirestore.getInstance(app)

        firestore.useEmulator(
            "10.0.2.2",
            8080
        )

        // Cache tylko w pamięci
        firestore.firestoreSettings =
            FirebaseFirestoreSettings.Builder(
                firestore.firestoreSettings
            )
                .setLocalCacheSettings(
                    MemoryCacheSettings.newBuilder().build()
                )
                .build()
    }

    // Tworzenie unikalnego adresu e-mail
    fun generateEmail(): String {

        return "test-${UUID.randomUUID()}@example.com"
    }

    // Tworzenie użytkownika testowego
    suspend fun createUser(
        email: String = generateEmail(),
        password: String = "TestPassword123"
    ): String {

        val result = auth
            .createUserWithEmailAndPassword(
                email,
                password
            )
            .await()

        return checkNotNull(result.user).uid
    }

    // Czyszczenie środowiska po teście
    // Czyszczenie środowiska po teście
    suspend fun close() {

        if (::firestore.isInitialized) {

            firestore.terminate().await()
        }

        if (::auth.isInitialized) {

            auth.signOut()
        }
    }
}
