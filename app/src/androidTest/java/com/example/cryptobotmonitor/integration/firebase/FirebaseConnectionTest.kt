
package com.example.cryptobotmonitor.integration.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseConnectionTest {

    private lateinit var environment:
            FirebaseTestEnvironment

    @Before
    fun setUp() {

        environment = FirebaseTestEnvironment()

        environment.initialize()
    }

    @After
    fun tearDown() = runBlocking {

        withTimeout(15_000L) {

            environment.close()
        }
    }

    // Test połączenia z Authentication
    @Test
    fun authEmulator_connectionSuccess() = runBlocking {

        withTimeout(15_000L) {

            val email = environment.generateEmail()

            val uid = environment.createUser(email)

            assertTrue(uid.isNotBlank())

            assertEquals(
                email,
                environment.auth.currentUser?.email
            )

            assertEquals(
                uid,
                environment.auth.currentUser?.uid
            )
        }
    }

    // Test zapisu i odczytu danych z Firestore
    @Test
    fun firestoreEmulator_connectionSuccess() =
        runBlocking {

            withTimeout(15_000L) {

                val uid = environment.createUser()

                val alert = hashMapOf(
                    "id" to "connection-test-alert",
                    "userId" to uid,
                    "coinId" to "bitcoin",
                    "coinName" to "Bitcoin",
                    "targetPrice" to 70000.0,
                    "condition" to "ABOVE",
                    "active" to true,
                    "createdAt" to System.currentTimeMillis()
                )

                val document = environment.firestore
                    .collection("alerts")
                    .document("connection-test-$uid")

                // ID dokumentu musi być zgodne z polem id
                alert["id"] = document.id

                try {

                    document.set(alert).await()

                    val result = document.get().await()

                    assertTrue(result.exists())

                    assertEquals(
                        uid,
                        result.getString("userId")
                    )

                    assertEquals(
                        "bitcoin",
                        result.getString("coinId")
                    )

                    assertEquals(
                        true,
                        result.getBoolean("active")
                    )

                } finally {

                    document.delete().await()
                }
            }
        }
}
