package com.company.voltmate.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "DRIVER", // DRIVER | OWNER
    val onboarded: Boolean = false,
    val carModel: String = "Nexon.ev 45 kWh",
    val carBatteryKwh: Double = 45.0,
    val preferredConnector: String = "CCS2",
    val keychainTagId: String = ""
)

enum class UserRole {
    DRIVER,
    OWNER
}

enum class ChargerType(val displayName: String, val isDc: Boolean, val defaultKw: Double) {
    AC_TYPE2_3_3("AC Type 2 (3.3 kW)", false, 3.3),
    AC_TYPE2_7_2("AC Type 2 (7.2 kW Fast)", false, 7.2),
    DC_CCS2_30("DC Fast CCS2 (30 kW)", true, 30.0),
    DC_CCS2_50("DC Fast CCS2 (50 kW)", true, 50.0)
}

enum class ParkingType(val displayName: String) {
    OPEN("Open Driveway"),
    COVERED("Covered Porch"),
    GATED("Gated Society"),
    ROADSIDE("Dedicated Roadside Bay")
}

data class Charger(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val lat: Double = 13.0827,
    val lng: Double = 80.2707,
    val address: String = "",
    val chargerType: String = ChargerType.AC_TYPE2_7_2.name,
    val powerKw: Double = 7.2,
    val parkingType: String = ParkingType.COVERED.name,
    val openFrom: String = "08:00",
    val openTo: String = "22:00",
    val days: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val ownerPresenceRequired: Boolean = false,
    val pricePerKwh: Double = 14.0,
    val status: String = "AVAILABLE", // AVAILABLE | BUSY | OFFLINE
    val rating: Double = 4.8,
    val amenities: List<String> = listOf("Washroom", "Wi-Fi", "CCTV", "Waiting Area"),
    val nfcRequired: Boolean = true
) {
    val isDc: Boolean
        get() = chargerType.startsWith("DC")
}

data class Booking(
    val id: String = "", // deterministic: chargerId_slotStartEpoch
    val chargerId: String = "",
    val driverId: String = "",
    val ownerId: String = "",
    val slotStart: Long = 0L,
    val slotEnd: Long = 0L,
    val status: String = "BOOKED" // BOOKED | ARRIVED | ACTIVE | COMPLETED | CANCELLED
)

data class ChargingSession(
    val id: String = "",
    val bookingId: String = "",
    val chargerId: String = "",
    val driverId: String = "",
    val ownerId: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val kWh: Double = 0.0,
    val amount: Double = 0.0,
    val paid: Boolean = false
)

data class TataCarModel(
    val name: String,
    val variant: String,
    val batteryKwh: Double,
    val ports: List<String> = listOf("Type 2 AC", "CCS2 DC")
) {
    val fullName: String get() = "$name $variant (${batteryKwh} kWh)"
}

object TataCarCatalog {
    val models = listOf(
        TataCarModel("Tiago.ev", "Medium Range", 19.2),
        TataCarModel("Tiago.ev", "Long Range", 24.0),
        TataCarModel("Tigor.ev", "Standard", 26.0),
        TataCarModel("Punch.ev", "Standard Range", 25.0),
        TataCarModel("Punch.ev", "Long Range", 35.0),
        TataCarModel("Nexon.ev", "Medium Range", 30.0),
        TataCarModel("Nexon.ev", "Long Range", 45.0),
        TataCarModel("Curvv.ev", "Medium Range", 45.0),
        TataCarModel("Curvv.ev", "Long Range", 55.0),
        TataCarModel("Harrier.ev", "Standard", 65.0),
        TataCarModel("Harrier.ev", "Long Range", 75.0)
    )
}

enum class TransactionType {
    TOPUP,
    CHARGE_PAYMENT,
    EARNING,
    WITHDRAWAL
}

data class Wallet(
    val uid: String = "",
    val balance: Long = 50000L // in paise, seeded with Rs 500 (50000 paise)
) {
    val balanceRupees: Double get() = balance / 100.0
    val formattedRupees: String get() = "₹%.2f".format(balanceRupees)
}

data class WalletTransaction(
    val id: String = "",
    val uid: String = "",
    val type: String = TransactionType.TOPUP.name,
    val amount: Long = 0L, // in paise
    val title: String = "",
    val counterparty: String = "",
    val sessionId: String = "",
    val status: String = "COMPLETED", // COMPLETED | PROCESSING | FAILED
    val createdAt: Long = System.currentTimeMillis()
) {
    val amountRupees: Double get() = amount / 100.0
    val formattedAmount: String get() {
        val prefix = if (type == TransactionType.TOPUP.name || type == TransactionType.EARNING.name) "+" else "-"
        return "$prefix₹%.2f".format(amountRupees)
    }
}

