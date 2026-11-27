package com.company.voltmate.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignUpMode: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    private val _activeRole = MutableStateFlow<String?>(null)
    val activeRole: StateFlow<String?> = _activeRole.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            _activeRole.value = authRepository.getLocalRole(uid)
        }
    }

    fun switchRole(newRole: String) {
        val uid = authRepository.currentUser?.uid ?: return
        authRepository.setLocalRole(uid, newRole)
        _activeRole.value = newRole
        viewModelScope.launch {
            try {
                val profile = authRepository.resolveUserProfile(uid)
                if (profile != null) {
                    authRepository.saveUserProfile(profile.copy(role = newRole))
                }
            } catch (e: Exception) {
                // background sync
            }
        }
    }

    fun updateProfile(name: String, carModel: String? = null, preferredConnector: String? = null) {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            val role = _activeRole.value ?: authRepository.getLocalRole(user.uid)
            val onboarded = authRepository.isLocalOnboarded(user.uid)
            val updated = UserProfile(
                uid = user.uid,
                name = name.ifBlank { user.displayName ?: "EV Driver" },
                email = user.email ?: "",
                role = role,
                onboarded = onboarded,
                carModel = carModel ?: "Nexon.ev 45 kWh",
                preferredConnector = preferredConnector ?: "CCS2"
            )
            authRepository.saveUserProfile(updated)
            _uiState.value = _uiState.value.copy(infoMessage = "Profile updated successfully")
        }
    }

    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null,
            infoMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, infoMessage = null)
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGoogle(context)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.localizedMessage ?: "Google Sign-In failed"
                    )
                }
            )
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter both email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(email, pass)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.localizedMessage ?: "Sign-in failed. Please check credentials."
                    )
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        if (email.isBlank() || pass.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signUpWithEmail(email, pass, name)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.localizedMessage ?: "Account creation failed"
                    )
                }
            )
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.sendPasswordReset(email)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "Password reset instructions sent to your email"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.localizedMessage ?: "Could not send reset email"
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
