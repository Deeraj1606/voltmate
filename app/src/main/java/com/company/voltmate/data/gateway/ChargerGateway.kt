package com.company.voltmate.data.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ChargerTelemetry(
    val chargerId: String = "",
    val bookingId: String = "",
    val isCharging: Boolean = false,
    val voltageV: Double = 230.0,
    val currentA: Double = 0.0,
    val powerKw: Double = 0.0,
    val energyKwh: Double = 0.0,
    val socPercent: Int = 25,
    val temperatureC: Double = 32.5,
    val connectorLocked: Boolean = false,
    val sessionDurationSec: Long = 0L,
    val runningCostRs: Double = 0.0,
    val ratePerKwh: Double = 14.0
)

interface ChargerGateway {
    val telemetryFlow: StateFlow<ChargerTelemetry>
    val demoSpeedMultiplier: StateFlow<Int>
    fun setDemoSpeed(multiplier: Int)
    suspend fun authorizeNfc(tagId: String, chargerId: String): Result<Boolean>
    suspend fun startCharging(
        chargerId: String,
        bookingId: String,
        targetPowerKw: Double,
        initialSoc: Int,
        batteryCapacityKwh: Double,
        pricePerKwh: Double
    ): Result<Boolean>
    suspend fun stopCharging(chargerId: String): Result<Double>
    suspend fun unlockConnector(chargerId: String): Result<Boolean>
    fun resetSession()
}

class SimulatedChargerGateway(
    private val scope: CoroutineScope
) : ChargerGateway {

    private val _telemetryFlow = MutableStateFlow(ChargerTelemetry())
    override val telemetryFlow: StateFlow<ChargerTelemetry> = _telemetryFlow.asStateFlow()

    private val _demoSpeedMultiplier = MutableStateFlow(10) // default 10x for pleasant testing
    override val demoSpeedMultiplier: StateFlow<Int> = _demoSpeedMultiplier.asStateFlow()

    private var chargingJob: Job? = null
    private var maxPowerKw: Double = 7.2
    private var carBatteryKwh: Double = 45.0

    override fun setDemoSpeed(multiplier: Int) {
        _demoSpeedMultiplier.value = when (multiplier) {
            1 -> 1
            60 -> 60
            else -> 10
        }
    }

    override suspend fun authorizeNfc(tagId: String, chargerId: String): Result<Boolean> {
        delay(700) // realistic NFC / secure element handshake latency
        return if (tagId.isNotBlank()) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException("Invalid NFC key tag"))
        }
    }

    override suspend fun startCharging(
        chargerId: String,
        bookingId: String,
        targetPowerKw: Double,
        initialSoc: Int,
        batteryCapacityKwh: Double,
        pricePerKwh: Double
    ): Result<Boolean> {
        delay(1200) // simulated hardware contactor lock & pre-charge insulation check
        maxPowerKw = if (targetPowerKw > 0) targetPowerKw else 7.2
        carBatteryKwh = if (batteryCapacityKwh > 0) batteryCapacityKwh else 45.0

        _telemetryFlow.value = _telemetryFlow.value.copy(
            chargerId = chargerId,
            bookingId = bookingId,
            isCharging = true,
            voltageV = 230.0 + Random.nextDouble(-1.5, 1.5),
            currentA = (maxPowerKw * 1000.0) / 230.0,
            powerKw = maxPowerKw,
            socPercent = initialSoc,
            connectorLocked = true,
            ratePerKwh = pricePerKwh,
            temperatureC = 33.0
        )

        chargingJob?.cancel()
        chargingJob = scope.launch {
            var currentSoc = initialSoc.toDouble()
            var accumulatedEnergy = _telemetryFlow.value.energyKwh
            var elapsedSec = _telemetryFlow.value.sessionDurationSec

            while (isActive && _telemetryFlow.value.isCharging) {
                delay(1000)
                val speed = _demoSpeedMultiplier.value
                val simulatedSeconds = speed.toDouble()
                elapsedSec += speed

                // Power curve calculation: taper current when SoC > 80% (standard CC-CV EV curve)
                val effectivePower = if (currentSoc >= 80.0) {
                    val taperFactor = max(0.2, (100.0 - currentSoc) / 20.0)
                    maxPowerKw * taperFactor
                } else {
                    maxPowerKw * (1.0 + Random.nextDouble(-0.02, 0.02))
                }

                val currentVoltage = 230.0 + Random.nextDouble(-2.0, 2.0)
                val currentAmps = (effectivePower * 1000.0) / currentVoltage
                val addedKwh = effectivePower * (simulatedSeconds / 3600.0)
                accumulatedEnergy += addedKwh

                val addedSocPercent = (addedKwh / carBatteryKwh) * 100.0
                currentSoc = min(100.0, currentSoc + addedSocPercent)

                // realistic thermal rise
                val tempTarget = 32.0 + (effectivePower / maxPowerKw) * 11.0 + Random.nextDouble(-0.3, 0.3)

                _telemetryFlow.value = _telemetryFlow.value.copy(
                    isCharging = currentSoc < 100.0,
                    voltageV = currentVoltage,
                    currentA = currentAmps,
                    powerKw = effectivePower,
                    energyKwh = accumulatedEnergy,
                    socPercent = currentSoc.toInt(),
                    temperatureC = tempTarget,
                    sessionDurationSec = elapsedSec,
                    runningCostRs = accumulatedEnergy * pricePerKwh
                )

                if (currentSoc >= 100.0) {
                    break
                }
            }
        }

        return Result.success(true)
    }

    override suspend fun stopCharging(chargerId: String): Result<Double> {
        delay(800) // simulated ramp-down relay disconnect
        chargingJob?.cancel()
        chargingJob = null

        val finalEnergy = _telemetryFlow.value.energyKwh
        _telemetryFlow.value = _telemetryFlow.value.copy(
            isCharging = false,
            powerKw = 0.0,
            currentA = 0.0,
            connectorLocked = true // Must stay locked until paid!
        )
        return Result.success(finalEnergy)
    }

    override suspend fun unlockConnector(chargerId: String): Result<Boolean> {
        delay(1000) // simulated solenoid release sound/actuation
        _telemetryFlow.value = _telemetryFlow.value.copy(
            connectorLocked = false
        )
        return Result.success(true)
    }

    override fun resetSession() {
        chargingJob?.cancel()
        chargingJob = null
        _telemetryFlow.value = ChargerTelemetry()
    }
}
