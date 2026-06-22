package com.example.cryptobotmonitor.data.repository

import com.example.cryptobotmonitor.data.model.PriceAlert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AlertRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Kolekcja z alertami w Firestore
    private val alertsCollection = firestore.collection("alerts")

    fun addAlert(
        coinId: String,
        coinName: String,
        targetPrice: Double,
        condition: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        // Sprawdzenie czy użytkownik jest zalogowany
        if (userId == null) {
            onResult(false, "Użytkownik nie jest zalogowany.")
            return
        }

        // Nowy dokument w Firestore
        val document = alertsCollection.document()

        val alert = PriceAlert(
            id = document.id,
            userId = userId,
            coinId = coinId,
            coinName = coinName,
            targetPrice = targetPrice,
            condition = condition,
            isActive = true
        )

        // Zapis alertu w Firestore
        document.set(alert)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                onResult(false, exception.localizedMessage ?: "Nie udało się dodać alertu.")
            }
    }

    fun observeUserAlerts(
        onResult: (List<PriceAlert>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = auth.currentUser?.uid

        // Bez użytkownika nie pobieramy alertów
        if (userId == null) {
            onError("Użytkownik nie jest zalogowany.")
            return null
        }

        // Pobieranie alertów zalogowanego użytkownika
        return alertsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.localizedMessage ?: "Nie udało się pobrać alertów.")
                    return@addSnapshotListener
                }

                val alerts = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(PriceAlert::class.java)
                } ?: emptyList()

                onResult(alerts)
            }
    }

    fun deleteAlert(
        alertId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Usunięcie alertu z Firestore
        alertsCollection.document(alertId)
            .delete()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                onResult(false, exception.localizedMessage ?: "Nie udało się usunąć alertu.")
            }
    }
}