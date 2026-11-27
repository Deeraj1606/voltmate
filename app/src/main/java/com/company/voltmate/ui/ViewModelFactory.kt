package com.company.voltmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.company.voltmate.di.AppContainer
import com.company.voltmate.ui.auth.AuthViewModel
import com.company.voltmate.ui.driver.DriverViewModel
import com.company.voltmate.ui.owner.OwnerViewModel

class AppViewModelFactory(
    private val container: AppContainer,
    private val userId: String = ""
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.authRepository) as T
            }
            modelClass.isAssignableFrom(com.company.voltmate.ui.onboarding.OnboardingViewModel::class.java) -> {
                com.company.voltmate.ui.onboarding.OnboardingViewModel(
                    container.authRepository,
                    container.chargerRepository
                ) as T
            }
            modelClass.isAssignableFrom(DriverViewModel::class.java) -> {
                DriverViewModel(
                    container.chargerRepository,
                    container.bookingRepository,
                    container.sessionRepository,
                    container.walletRepository,
                    container.chargerGateway,
                    userId
                ) as T
            }
            modelClass.isAssignableFrom(OwnerViewModel::class.java) -> {
                OwnerViewModel(
                    container.chargerRepository,
                    container.bookingRepository,
                    container.sessionRepository,
                    container.walletRepository,
                    container.chargerGateway,
                    userId
                ) as T
            }
            modelClass.isAssignableFrom(com.company.voltmate.ui.wallet.WalletViewModel::class.java) -> {
                com.company.voltmate.ui.wallet.WalletViewModel(
                    container.walletRepository,
                    userId
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
