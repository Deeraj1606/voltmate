package com.company.voltmate.data.repository

import com.company.voltmate.data.model.ChargingSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface SessionRepository {
    fun getDriverSessions(driverId: String): Flow<List<ChargingSession>>
    fun getOwnerSessions(ownerId: String): Flow<List<ChargingSession>>
    suspend fun saveSession(session: ChargingSession): Result<String>
    suspend fun getSessionById(sessionId: String): ChargingSession?
    fun getDemoOwnerSessions(): List<ChargingSession>
}

class FirestoreSessionRepository(
    private val firestore: FirebaseFirestore
) : SessionRepository {

    override fun getDriverSessions(driverId: String): Flow<List<ChargingSession>> = callbackFlow {
        trySend(emptyList())
        val listener = firestore.collection("sessions")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChargingSession::class.java) }
                    ?.sortedByDescending { it.endedAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun getOwnerSessions(ownerId: String): Flow<List<ChargingSession>> = callbackFlow {
        trySend(emptyList())
        val listener = firestore.collection("sessions")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChargingSession::class.java) }
                    ?.sortedByDescending { it.endedAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveSession(session: ChargingSession): Result<String> {
        return try {
            val docRef = if (session.id.isNotBlank()) {
                firestore.collection("sessions").document(session.id)
            } else {
                firestore.collection("sessions").document()
            }
            val finalSession = session.copy(id = docRef.id)
            kotlinx.coroutines.withTimeoutOrNull(4000) {
                docRef.set(finalSession).await()
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.success(session.id.ifBlank { "local_sess_${System.currentTimeMillis()}" })
        }
    }

    override suspend fun getSessionById(sessionId: String): ChargingSession? {
        return try {
            kotlinx.coroutines.withTimeoutOrNull(4000) {
                val doc = firestore.collection("sessions").document(sessionId).get().await()
                if (doc.exists()) doc.toObject(ChargingSession::class.java) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getDemoOwnerSessions(): List<ChargingSession> {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        return listOf(
            ChargingSession(
                id = "demo_sess_1",
                bookingId = "bk_demo_1",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_1",
                ownerId = "owner",
                startedAt = now - dayMs * 1 + 3600000,
                endedAt = now - dayMs * 1 + 7200000,
                kWh = 22.4,
                amount = 313.6,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_2",
                bookingId = "bk_demo_2",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_2",
                ownerId = "owner",
                startedAt = now - dayMs * 2 + 1800000,
                endedAt = now - dayMs * 2 + 5400000,
                kWh = 18.2,
                amount = 254.8,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_3",
                bookingId = "bk_demo_3",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_3",
                ownerId = "owner",
                startedAt = now - dayMs * 3 + 2400000,
                endedAt = now - dayMs * 3 + 6000000,
                kWh = 31.0,
                amount = 434.0,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_4",
                bookingId = "bk_demo_4",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_4",
                ownerId = "owner",
                startedAt = now - dayMs * 4 + 7200000,
                endedAt = now - dayMs * 4 + 10800000,
                kWh = 15.5,
                amount = 217.0,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_5",
                bookingId = "bk_demo_5",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_5",
                ownerId = "owner",
                startedAt = now - dayMs * 5 + 3600000,
                endedAt = now - dayMs * 5 + 7200000,
                kWh = 28.0,
                amount = 392.0,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_6",
                bookingId = "bk_demo_6",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_6",
                ownerId = "owner",
                startedAt = now - dayMs * 6 + 1800000,
                endedAt = now - dayMs * 6 + 4800000,
                kWh = 19.8,
                amount = 277.2,
                paid = true
            ),
            ChargingSession(
                id = "demo_sess_7",
                bookingId = "bk_demo_7",
                chargerId = "demo_charger_owner",
                driverId = "demo_driver_7",
                ownerId = "owner",
                startedAt = now - dayMs * 7 + 4000000,
                endedAt = now - dayMs * 7 + 7600000,
                kWh = 24.5,
                amount = 343.0,
                paid = true
            )
        )
    }
}
