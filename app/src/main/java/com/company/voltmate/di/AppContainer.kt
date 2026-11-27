package com.company.voltmate.di

import android.content.Context
import com.company.voltmate.data.gateway.ChargerGateway
import com.company.voltmate.data.gateway.SimulatedChargerGateway
import com.company.voltmate.data.repository.AuthRepository
import com.company.voltmate.data.repository.BookingRepository
import com.company.voltmate.data.repository.ChargerRepository
import com.company.voltmate.data.repository.FirebaseAuthRepository
import com.company.voltmate.data.repository.FirestoreBookingRepository
import com.company.voltmate.data.repository.FirestoreChargerRepository
import com.company.voltmate.data.repository.FirestoreSessionRepository
import com.company.voltmate.data.repository.FirestoreWalletRepository
import com.company.voltmate.data.repository.SessionRepository
import com.company.voltmate.data.repository.WalletRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val applicationScope: CoroutineScope
    val authRepository: AuthRepository
    val chargerRepository: ChargerRepository
    val bookingRepository: BookingRepository
    val sessionRepository: SessionRepository
    val walletRepository: WalletRepository
    val chargerGateway: ChargerGateway
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        FirebaseApp.initializeApp(context)
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth, firestore, context)
    }

    override val chargerRepository: ChargerRepository by lazy {
        FirestoreChargerRepository(firestore)
    }

    override val bookingRepository: BookingRepository by lazy {
        FirestoreBookingRepository(firestore)
    }

    override val sessionRepository: SessionRepository by lazy {
        FirestoreSessionRepository(firestore)
    }

    override val walletRepository: WalletRepository by lazy {
        FirestoreWalletRepository(firestore, context)
    }

    override val chargerGateway: ChargerGateway by lazy {
        SimulatedChargerGateway(applicationScope)
    }
}
