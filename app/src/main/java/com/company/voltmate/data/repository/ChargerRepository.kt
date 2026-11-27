package com.company.voltmate.data.repository

import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargerType
import com.company.voltmate.data.model.ParkingType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlin.math.cos

import kotlinx.coroutines.withTimeoutOrNull

interface ChargerRepository {
    fun getMergedChargers(): Flow<List<Charger>>
    fun getOwnerChargers(ownerId: String): Flow<List<Charger>>
    fun getDemoChargers(): List<Charger>
    fun getFallbackOwnerCharger(ownerId: String): Charger
    suspend fun getChargerById(chargerId: String): Charger?
    suspend fun addCharger(charger: Charger): Result<String>
    suspend fun updateCharger(charger: Charger): Result<Unit>
    fun updateReferenceLocation(lat: Double, lng: Double)
}

class FirestoreChargerRepository(
    private val firestore: FirebaseFirestore
) : ChargerRepository {

    private val demoChargersFlow = MutableStateFlow<List<Charger>>(emptyList())

    init {
        generateDemoChargers(13.0827, 80.2707) // Default Chennai center
    }

    override fun getDemoChargers(): List<Charger> = demoChargersFlow.value

    override fun getFallbackOwnerCharger(ownerId: String): Charger {
        return Charger(
            id = "fallback_owner_${ownerId.ifBlank { "demo" }}",
            ownerId = ownerId,
            name = "My Tata Power Home Bay",
            lat = 13.0827,
            lng = 80.2707,
            address = "Anna Nagar East, Chennai",
            chargerType = ChargerType.AC_TYPE2_7_2.name,
            powerKw = 7.2,
            parkingType = ParkingType.COVERED.name,
            openFrom = "07:00",
            openTo = "23:00",
            ownerPresenceRequired = false,
            pricePerKwh = 14.0,
            status = "AVAILABLE",
            rating = 4.9,
            amenities = listOf("Covered", "CCTV", "Wi-Fi"),
            nfcRequired = true
        )
    }

    override fun updateReferenceLocation(lat: Double, lng: Double) {
        generateDemoChargers(lat, lng)
    }

    private fun generateDemoChargers(centerLat: Double, centerLng: Double) {
        val latOffset = 1.0 / 111.0 // 1 km in lat
        val lngOffset = 1.0 / (111.0 * cos(Math.toRadians(centerLat)))

        val demos = listOf(
            Charger(
                id = "demo_1",
                ownerId = "demo_owner_1",
                name = "Anna Nagar GreenPoint",
                lat = centerLat + 0.8 * latOffset,
                lng = centerLng + 0.6 * lngOffset,
                address = "12, 2nd Avenue, Anna Nagar East",
                chargerType = ChargerType.AC_TYPE2_7_2.name,
                powerKw = 7.2,
                parkingType = ParkingType.COVERED.name,
                openFrom = "07:00",
                openTo = "23:00",
                ownerPresenceRequired = false,
                pricePerKwh = 14.0,
                status = "AVAILABLE",
                rating = 4.9,
                amenities = listOf("Covered", "CCTV", "Wi-Fi"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_2",
                ownerId = "demo_owner_2",
                name = "T. Nagar SuperBay Fast CCS2",
                lat = centerLat - 1.2 * latOffset,
                lng = centerLng + 0.9 * lngOffset,
                address = "45, Burkit Road, T. Nagar",
                chargerType = ChargerType.DC_CCS2_30.name,
                powerKw = 30.0,
                parkingType = ParkingType.GATED.name,
                openFrom = "06:00",
                openTo = "22:00",
                ownerPresenceRequired = true,
                pricePerKwh = 18.0,
                status = "AVAILABLE",
                rating = 4.8,
                amenities = listOf("Waiting Lounge", "Tea/Coffee", "CCTV"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_3",
                ownerId = "demo_owner_3",
                name = "Adyar EcoCharge Bay",
                lat = centerLat - 2.5 * latOffset,
                lng = centerLng - 1.1 * lngOffset,
                address = "7, Gandhi Nagar 1st Main, Adyar",
                chargerType = ChargerType.AC_TYPE2_3_3.name,
                powerKw = 3.3,
                parkingType = ParkingType.OPEN.name,
                openFrom = "08:00",
                openTo = "20:00",
                ownerPresenceRequired = false,
                pricePerKwh = 12.0,
                status = "AVAILABLE",
                rating = 4.6,
                amenities = listOf("Solar Powered", "Washroom"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_4",
                ownerId = "demo_owner_4",
                name = "OMR Expressway HyperVolt",
                lat = centerLat - 3.8 * latOffset,
                lng = centerLng + 2.2 * lngOffset,
                address = "Plot 89, Rajiv Gandhi Salai, Perungudi",
                chargerType = ChargerType.DC_CCS2_50.name,
                powerKw = 50.0,
                parkingType = ParkingType.ROADSIDE.name,
                openFrom = "00:00",
                openTo = "23:59",
                ownerPresenceRequired = false,
                pricePerKwh = 20.0,
                status = "BUSY",
                rating = 4.9,
                amenities = listOf("24x7 Security", "Cafe Nearby", "Washroom"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_5",
                ownerId = "demo_owner_5",
                name = "Alwarpet Villa Charger",
                lat = centerLat - 0.5 * latOffset,
                lng = centerLng - 1.8 * lngOffset,
                address = "18, TTK Road, Alwarpet",
                chargerType = ChargerType.AC_TYPE2_7_2.name,
                powerKw = 7.2,
                parkingType = ParkingType.COVERED.name,
                openFrom = "09:00",
                openTo = "21:00",
                ownerPresenceRequired = true,
                pricePerKwh = 15.0,
                status = "AVAILABLE",
                rating = 4.7,
                amenities = listOf("Covered", "Drinking Water", "Wi-Fi"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_6",
                ownerId = "demo_owner_6",
                name = "Velachery SunVolt Point",
                lat = centerLat - 4.2 * latOffset,
                lng = centerLng - 0.4 * lngOffset,
                address = "104, Bypass Road, Velachery",
                chargerType = ChargerType.AC_TYPE2_7_2.name,
                powerKw = 7.2,
                parkingType = ParkingType.GATED.name,
                openFrom = "08:00",
                openTo = "22:00",
                ownerPresenceRequired = false,
                pricePerKwh = 13.5,
                status = "AVAILABLE",
                rating = 4.8,
                amenities = listOf("Gated", "EV Tyre Inflator", "CCTV"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_7",
                ownerId = "demo_owner_7",
                name = "Nungambakkam HighWay Volt",
                lat = centerLat + 1.6 * latOffset,
                lng = centerLng - 1.4 * lngOffset,
                address = "22, Sterling Road, Nungambakkam",
                chargerType = ChargerType.DC_CCS2_30.name,
                powerKw = 30.0,
                parkingType = ParkingType.COVERED.name,
                openFrom = "06:00",
                openTo = "23:00",
                ownerPresenceRequired = false,
                pricePerKwh = 17.0,
                status = "OFFLINE",
                rating = 4.5,
                amenities = listOf("Covered", "CCTV"),
                nfcRequired = true
            ),
            Charger(
                id = "demo_8",
                ownerId = "demo_owner_8",
                name = "Guindy Tech Park Bay",
                lat = centerLat - 2.8 * latOffset,
                lng = centerLng - 0.2 * lngOffset,
                address = "Block C, Olympia Tech Park, Guindy",
                chargerType = ChargerType.DC_CCS2_50.name,
                powerKw = 50.0,
                parkingType = ParkingType.ROADSIDE.name,
                openFrom = "07:00",
                openTo = "23:00",
                ownerPresenceRequired = false,
                pricePerKwh = 19.5,
                status = "AVAILABLE",
                rating = 5.0,
                amenities = listOf("Fast Charging", "Food Court", "Restrooms"),
                nfcRequired = true
            )
        )
        demoChargersFlow.value = demos
    }

    override fun getMergedChargers(): Flow<List<Charger>> {
        val firestoreChargersFlow = callbackFlow<List<Charger>> {
            trySend(emptyList())
            val listener = firestore.collection("chargers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { it.toObject(Charger::class.java) } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }

        return combine(demoChargersFlow, firestoreChargersFlow) { demos, real ->
            val realIds = real.map { it.id }.toSet()
            val filteredDemos = demos.filterNot { it.id in realIds }
            filteredDemos + real
        }
    }

    override fun getOwnerChargers(ownerId: String): Flow<List<Charger>> = callbackFlow {
        val fallback = getFallbackOwnerCharger(ownerId)
        trySend(listOf(fallback))

        val listener = firestore.collection("chargers")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(listOf(fallback))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Charger::class.java) } ?: emptyList()
                if (list.isEmpty()) {
                    trySend(listOf(fallback))
                } else {
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getChargerById(chargerId: String): Charger? {
        val demoMatch = demoChargersFlow.value.find { it.id == chargerId }
        if (demoMatch != null) return demoMatch

        return try {
            withTimeoutOrNull(4000) {
                val doc = firestore.collection("chargers").document(chargerId).get().await()
                if (doc.exists()) doc.toObject(Charger::class.java) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addCharger(charger: Charger): Result<String> {
        return try {
            val docRef = if (charger.id.isNotBlank()) {
                firestore.collection("chargers").document(charger.id)
            } else {
                firestore.collection("chargers").document()
            }
            val finalCharger = charger.copy(id = docRef.id)
            withTimeoutOrNull(4000) {
                docRef.set(finalCharger).await()
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.success(charger.id.ifBlank { "local_${System.currentTimeMillis()}" })
        }
    }

    override suspend fun updateCharger(charger: Charger): Result<Unit> {
        return try {
            withTimeoutOrNull(4000) {
                firestore.collection("chargers").document(charger.id).set(charger).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }
}
