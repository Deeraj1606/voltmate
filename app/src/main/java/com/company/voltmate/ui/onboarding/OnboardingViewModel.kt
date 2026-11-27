package com.company.voltmate.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargerType
import com.company.voltmate.data.model.ParkingType
import com.company.voltmate.data.model.TataCarCatalog
import com.company.voltmate.data.model.TataCarModel
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.data.repository.AuthRepository
import com.company.voltmate.data.repository.ChargerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedRole: String = "DRIVER",
    // Driver fields
    val selectedCar: TataCarModel = TataCarCatalog.models[6], // Default Nexon.ev 45kWh
    val preferredConnector: String = "CCS2 & Type 2",
    val keychainTagId: String = "",
    val isScanningNfc: Boolean = false,
    // Owner fields
    val chargerName: String = "",
    val chargerType: ChargerType = ChargerType.AC_TYPE2_7_2,
    val powerKw: Double = 7.2,
    val parkingType: ParkingType = ParkingType.COVERED,
    val openFrom: String = "08:00",
    val openTo: String = "22:00",
    val selectedDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val ownerPresenceRequired: Boolean = false,
    val pricePerKwh: Double = 14.0,
    val address: String = "",
    val lat: Double = 13.0827,
    val lng: Double = 80.2707,
    // Status
    val isLoading: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val chargerRepository: ChargerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setRole(role: String) {
        _uiState.value = _uiState.value.copy(selectedRole = role, currentStep = 1)
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun selectCar(car: TataCarModel) {
        _uiState.value = _uiState.value.copy(selectedCar = car)
    }

    fun selectConnector(connector: String) {
        _uiState.value = _uiState.value.copy(preferredConnector = connector)
    }

    fun simulateNfcScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningNfc = true)
            kotlinx.coroutines.delay(1200)
            val randomTag = "VM-NFC-${(1000..9999).random()}"
            _uiState.value = _uiState.value.copy(
                isScanningNfc = false,
                keychainTagId = randomTag
            )
        }
    }

    // Owner mutators
    fun updateOwnerChargerType(type: ChargerType) {
        _uiState.value = _uiState.value.copy(chargerType = type, powerKw = type.defaultKw)
    }

    fun updateOwnerPowerKw(kw: Double) {
        _uiState.value = _uiState.value.copy(powerKw = kw)
    }

    fun updateOwnerParking(parking: ParkingType) {
        _uiState.value = _uiState.value.copy(parkingType = parking)
    }

    fun updateOwnerHours(from: String, to: String) {
        _uiState.value = _uiState.value.copy(openFrom = from, openTo = to)
    }

    fun toggleOwnerDay(day: String) {
        val days = _uiState.value.selectedDays.toMutableList()
        if (days.contains(day)) {
            if (days.size > 1) days.remove(day)
        } else {
            days.add(day)
        }
        _uiState.value = _uiState.value.copy(selectedDays = days)
    }

    fun setOwnerPresence(required: Boolean) {
        _uiState.value = _uiState.value.copy(ownerPresenceRequired = required)
    }

    fun setPricePerKwh(price: Double) {
        _uiState.value = _uiState.value.copy(pricePerKwh = price)
    }

    fun setLocationDetails(name: String, address: String, lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            chargerName = name,
            address = address,
            lat = lat,
            lng = lng
        )
    }

    fun finishDriverOnboarding() {
        val user = authRepository.currentUser ?: return
        authRepository.setLocalRole(user.uid, "DRIVER")
        authRepository.setLocalOnboarded(user.uid, true)
        _uiState.value = _uiState.value.copy(isLoading = false, isFinished = true)

        viewModelScope.launch {
            try {
                val profile = UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "EV Driver",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = "DRIVER",
                    onboarded = true,
                    carModel = _uiState.value.selectedCar.fullName,
                    carBatteryKwh = _uiState.value.selectedCar.batteryKwh,
                    preferredConnector = _uiState.value.preferredConnector,
                    keychainTagId = _uiState.value.keychainTagId
                )
                authRepository.saveUserProfile(profile)
            } catch (e: Exception) {
                // Non-blocking sync
            }
        }
    }

    fun finishOwnerOnboarding() {
        val user = authRepository.currentUser ?: return
        val state = _uiState.value
        authRepository.setLocalRole(user.uid, "OWNER")
        authRepository.setLocalOnboarded(user.uid, true)
        _uiState.value = _uiState.value.copy(isLoading = false, isFinished = true)

        viewModelScope.launch {
            try {
                val charger = Charger(
                    ownerId = user.uid,
                    name = state.chargerName.ifBlank { "${user.displayName ?: "Host"}'s EV Bay" },
                    lat = state.lat,
                    lng = state.lng,
                    address = state.address.ifBlank { "Chennai, Tamil Nadu" },
                    chargerType = state.chargerType.name,
                    powerKw = state.powerKw,
                    parkingType = state.parkingType.name,
                    openFrom = state.openFrom,
                    openTo = state.openTo,
                    days = state.selectedDays,
                    ownerPresenceRequired = state.ownerPresenceRequired,
                    pricePerKwh = state.pricePerKwh,
                    status = "AVAILABLE",
                    rating = 5.0,
                    amenities = listOf("Safe Parking", "CCTV", "Restroom Access"),
                    nfcRequired = true
                )
                chargerRepository.addCharger(charger)

                val profile = UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "Charger Host",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = "OWNER",
                    onboarded = true
                )
                authRepository.saveUserProfile(profile)
            } catch (e: Exception) {
                // Non-blocking sync
            }
        }
    }
}
