package com.company.voltmate.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.voltmate.data.model.TransactionType
import com.company.voltmate.data.model.Wallet
import com.company.voltmate.data.model.WalletTransaction
import com.company.voltmate.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val selectedFilter: String = "ALL" // ALL | EARNING | CHARGE_PAYMENT | TOPUP | WITHDRAWAL
)

class WalletViewModel(
    private val walletRepository: WalletRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    val wallet: StateFlow<Wallet> = walletRepository.observeWallet(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Wallet(uid = userId, balance = 50000L))

    val transactions: StateFlow<List<WalletTransaction>> = walletRepository.observeTransactions(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun topUp(amountRupees: Double, methodTitle: String = "Tata Pay UPI") {
        if (amountRupees <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val amountPaise = (amountRupees * 100).toLong()
            val result = walletRepository.topUp(userId, amountPaise, methodTitle)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "₹%.2f added to your VoltMate Wallet".format(amountRupees)
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to add money"
                    )
                }
            )
        }
    }

    fun withdraw(amountRupees: Double, destination: String) {
        if (amountRupees < 100.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Minimum withdrawal is ₹100")
            return
        }
        val currentBalanceRupees = wallet.value.balanceRupees
        if (amountRupees > currentBalanceRupees) {
            _uiState.value = _uiState.value.copy(errorMessage = "Withdrawal exceeds available balance")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val amountPaise = (amountRupees * 100).toLong()
            val result = walletRepository.withdraw(userId, amountPaise, destination.ifBlank { "tata.ev.owner@okhdfcbank" })
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Withdrawal of ₹%.2f initiated (Processing)".format(amountRupees)
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Withdrawal failed"
                    )
                }
            )
        }
    }

    fun getSavedDestination(): String {
        return walletRepository.getSavedWithdrawalDestination(userId)
    }

    fun saveDestination(dest: String) {
        walletRepository.saveWithdrawalDestination(userId, dest)
    }
}
