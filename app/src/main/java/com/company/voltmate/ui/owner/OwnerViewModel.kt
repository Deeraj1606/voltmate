package com.company.voltmate.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.voltmate.data.gateway.ChargerGateway
import com.company.voltmate.data.gateway.ChargerTelemetry
import com.company.voltmate.data.model.Booking
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargingSession
import com.company.voltmate.data.model.TransactionType
import com.company.voltmate.data.model.Wallet
import com.company.voltmate.data.model.WalletTransaction
import com.company.voltmate.data.repository.BookingRepository
import com.company.voltmate.data.repository.ChargerRepository
import com.company.voltmate.data.repository.SessionRepository
import com.company.voltmate.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OwnerViewModel(
    private val chargerRepository: ChargerRepository,
    private val bookingRepository: BookingRepository,
    private val sessionRepository: SessionRepository,
    private val walletRepository: WalletRepository,
    private val chargerGateway: ChargerGateway,
    val userId: String
) : ViewModel() {

    val fallbackCharger: Charger = chargerRepository.getFallbackOwnerCharger(userId)

    val wallet: StateFlow<Wallet> = walletRepository.observeWallet(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Wallet(uid = userId, balance = 50000L))

    val walletTransactions: StateFlow<List<WalletTransaction>> = walletRepository.observeTransactions(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chargers: StateFlow<List<Charger>> = chargerRepository.getOwnerChargers(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(fallbackCharger))

    val communityChargers: StateFlow<List<Charger>> = chargerRepository.getMergedChargers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, chargerRepository.getDemoChargers())

    val bookings: StateFlow<List<Booking>> = bookingRepository.getOwnerBookings(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sessions: StateFlow<List<ChargingSession>> = sessionRepository.getOwnerSessions(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val demoSessions: List<ChargingSession> = sessionRepository.getDemoOwnerSessions()

    val telemetry: StateFlow<ChargerTelemetry> = chargerGateway.telemetryFlow

    val totalEarnings: StateFlow<Double> = combine(sessions, walletTransactions) { sessionList, txList ->
        val walletEarnings = txList.filter { it.type == TransactionType.EARNING.name }.sumOf { it.amountRupees }
        val sessionEarnings = sessionList.sumOf { it.amount }
        val demoEarnings = demoSessions.sumOf { it.amount }
        walletEarnings + sessionEarnings + demoEarnings
    }.stateIn(viewModelScope, SharingStarted.Eagerly, demoSessions.sumOf { it.amount })

    val totalEnergy: StateFlow<Double> = sessions.map { list ->
        val real = list.sumOf { it.kWh }
        val demo = demoSessions.sumOf { it.kWh }
        real + demo
    }.stateIn(viewModelScope, SharingStarted.Eagerly, demoSessions.sumOf { it.kWh })

    fun toggleChargerStatus(charger: Charger) {
        viewModelScope.launch {
            val newStatus = when (charger.status) {
                "AVAILABLE" -> "OFFLINE"
                "OFFLINE" -> "AVAILABLE"
                else -> charger.status
            }
            chargerRepository.updateCharger(charger.copy(status = newStatus))
        }
    }
}
