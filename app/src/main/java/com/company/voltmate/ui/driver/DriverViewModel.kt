package com.company.voltmate.ui.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.voltmate.data.model.Booking
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargingSession
import com.company.voltmate.data.gateway.ChargerGateway
import com.company.voltmate.data.gateway.ChargerTelemetry
import com.company.voltmate.data.model.Wallet
import com.company.voltmate.data.repository.BookingRepository
import com.company.voltmate.data.repository.ChargerRepository
import com.company.voltmate.data.repository.SessionRepository
import com.company.voltmate.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DriverViewModel(
    private val chargerRepository: ChargerRepository,
    private val bookingRepository: BookingRepository,
    private val sessionRepository: SessionRepository,
    private val walletRepository: WalletRepository,
    private val chargerGateway: ChargerGateway,
    val userId: String
) : ViewModel() {

    val wallet: StateFlow<Wallet> = walletRepository.observeWallet(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Wallet(uid = userId, balance = 50000L))

    val chargers: StateFlow<List<Charger>> = chargerRepository.getMergedChargers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, chargerRepository.getDemoChargers())

    val bookings: StateFlow<List<Booking>> = bookingRepository.getDriverBookings(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sessions: StateFlow<List<ChargingSession>> = sessionRepository.getDriverSessions(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCharger = MutableStateFlow<Charger?>(null)
    val selectedCharger: StateFlow<Charger?> = _selectedCharger.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val bookingState: StateFlow<BookingUiState> = _bookingState.asStateFlow()

    private val _nfcState = MutableStateFlow<NfcUiState>(NfcUiState.Idle)
    val nfcState: StateFlow<NfcUiState> = _nfcState.asStateFlow()

    val telemetry: StateFlow<ChargerTelemetry> = chargerGateway.telemetryFlow

    val demoSpeed: StateFlow<Int> = chargerGateway.demoSpeedMultiplier

    private val _sessionComplete = MutableStateFlow<SessionCompleteData?>(null)
    val sessionComplete: StateFlow<SessionCompleteData?> = _sessionComplete.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCharger(charger: Charger) {
        _selectedCharger.value = charger
    }

    fun loadChargerById(chargerId: String) {
        viewModelScope.launch {
            _selectedCharger.value = chargerRepository.getChargerById(chargerId)
        }
    }

    fun bookSlot(charger: Charger, slotStart: Long, slotEnd: Long) {
        if (userId.isNotBlank() && userId == charger.ownerId) {
            _bookingState.value = BookingUiState.Error("You cannot book your own registered charger bay")
            return
        }
        _bookingState.value = BookingUiState.Loading
        viewModelScope.launch {
            val booking = Booking(
                chargerId = charger.id,
                driverId = userId,
                ownerId = charger.ownerId,
                slotStart = slotStart,
                slotEnd = slotEnd
            )
            val result = bookingRepository.createBookingTransaction(booking)
            _bookingState.value = if (result.isSuccess) {
                BookingUiState.Success(result.getOrThrow())
            } else {
                BookingUiState.Error(result.exceptionOrNull()?.message ?: "Booking failed")
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
        }
    }

    fun simulateNfcTap(bookingId: String, chargerId: String, tagId: String) {
        _nfcState.value = NfcUiState.Scanning
        viewModelScope.launch {
            val result = chargerGateway.authorizeNfc(tagId, chargerId)
            if (result.isSuccess) {
                bookingRepository.updateBookingStatus(bookingId, "ARRIVED")
                _nfcState.value = NfcUiState.Authorized(bookingId)
            } else {
                _nfcState.value = NfcUiState.Error("NFC authorization failed")
            }
        }
    }

    fun startCharging(
        charger: Charger,
        bookingId: String,
        initialSoc: Int,
        batteryKwh: Double
    ) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, "ACTIVE")
            chargerGateway.startCharging(
                chargerId = charger.id,
                bookingId = bookingId,
                targetPowerKw = charger.powerKw,
                initialSoc = initialSoc,
                batteryCapacityKwh = batteryKwh,
                pricePerKwh = charger.pricePerKwh
            )
        }
    }

    fun stopCharging(charger: Charger, bookingId: String) {
        viewModelScope.launch {
            val result = chargerGateway.stopCharging(charger.id)
            if (result.isSuccess) {
                val finalKwh = result.getOrDefault(0.0)
                val amount = finalKwh * charger.pricePerKwh
                val session = ChargingSession(
                    bookingId = bookingId,
                    chargerId = charger.id,
                    driverId = userId,
                    ownerId = charger.ownerId,
                    startedAt = telemetry.value.sessionDurationSec,
                    endedAt = System.currentTimeMillis(),
                    kWh = finalKwh,
                    amount = amount,
                    paid = false
                )
                sessionRepository.saveSession(session)
                _sessionComplete.value = SessionCompleteData(
                    kWh = finalKwh,
                    amount = amount,
                    durationSec = telemetry.value.sessionDurationSec,
                    chargerId = charger.id,
                    bookingId = bookingId
                )
            }
        }
    }

    fun payAndUnlock(
        charger: Charger,
        bookingId: String,
        amountRupees: Double,
        method: String = "WALLET",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong()
            if (method == "WALLET") {
                val currentBalance = wallet.value.balance
                if (currentBalance < amountPaise) {
                    onResult(false, "Insufficient wallet balance")
                    return@launch
                }
            } else {
                walletRepository.topUp(userId, amountPaise, "UPI Instant Pay")
            }

            val payResult = walletRepository.payForChargingSession(
                driverId = userId,
                ownerId = charger.ownerId,
                sessionId = bookingId,
                chargerName = charger.name,
                amountPaise = amountPaise
            )

            if (payResult.isSuccess) {
                bookingRepository.updateBookingStatus(bookingId, "COMPLETED")
                val data = _sessionComplete.value
                chargerGateway.unlockConnector(data?.chargerId ?: charger.id)
                _sessionComplete.value = null
                _nfcState.value = NfcUiState.Idle
                _bookingState.value = BookingUiState.Idle
                chargerGateway.resetSession()
                onResult(true, null)
            } else {
                onResult(false, payResult.exceptionOrNull()?.message ?: "Payment failed")
            }
        }
    }

    fun topUpWallet(amountRupees: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong()
            walletRepository.topUp(userId, amountPaise, "Quick Top-up")
            onDone()
        }
    }

    fun confirmPayment(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, "COMPLETED")
            val data = _sessionComplete.value
            if (data != null) {
                chargerGateway.unlockConnector(data.chargerId)
            }
            _sessionComplete.value = null
            _nfcState.value = NfcUiState.Idle
            _bookingState.value = BookingUiState.Idle
            chargerGateway.resetSession()
        }
    }

    fun setDemoSpeed(speed: Int) {
        chargerGateway.setDemoSpeed(speed)
    }

    fun resetBookingState() {
        _bookingState.value = BookingUiState.Idle
    }

    fun resetNfcState() {
        _nfcState.value = NfcUiState.Idle
    }
}

sealed class BookingUiState {
    data object Idle : BookingUiState()
    data object Loading : BookingUiState()
    data class Success(val booking: Booking) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

sealed class NfcUiState {
    data object Idle : NfcUiState()
    data object Scanning : NfcUiState()
    data class Authorized(val bookingId: String) : NfcUiState()
    data class Error(val message: String) : NfcUiState()
}

data class SessionCompleteData(
    val kWh: Double,
    val amount: Double,
    val durationSec: Long,
    val chargerId: String,
    val bookingId: String
)
