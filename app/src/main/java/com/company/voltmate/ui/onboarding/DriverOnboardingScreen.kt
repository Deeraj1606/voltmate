package com.company.voltmate.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.TataCarCatalog
import com.company.voltmate.data.model.TataCarModel
import com.company.voltmate.ui.theme.BorderHairline
import com.company.voltmate.ui.theme.DeepGreen
import com.company.voltmate.ui.theme.InkMuted
import com.company.voltmate.ui.theme.InkPrimary
import com.company.voltmate.ui.theme.InkSecondary
import com.company.voltmate.ui.theme.PrimaryGreen
import com.company.voltmate.ui.theme.SurfaceBg
import com.company.voltmate.ui.theme.TintGreen
import com.company.voltmate.ui.theme.WhiteCanvas

@Composable
fun DriverOnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onComplete()
        }
    }

    Scaffold(
        containerColor = WhiteCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 1) {
                    IconButton(onClick = { viewModel.prevStep() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = InkPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Step dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..3) {
                        val active = step <= uiState.currentStep
                        Box(
                            modifier = Modifier
                                .size(if (step == uiState.currentStep) 10.dp else 8.dp)
                                .background(
                                    if (active) PrimaryGreen else BorderHairline,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            when (uiState.currentStep) {
                1 -> StepVehicleSelect(
                    selectedCar = uiState.selectedCar,
                    onSelect = { viewModel.selectCar(it) }
                )
                2 -> StepConnectorPreference(
                    selectedConnector = uiState.preferredConnector,
                    onSelect = { viewModel.selectConnector(it) }
                )
                3 -> StepNfcKeychain(
                    keychainTagId = uiState.keychainTagId,
                    isScanning = uiState.isScanningNfc,
                    onScan = { viewModel.simulateNfcScan() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (uiState.currentStep < 3) {
                        viewModel.nextStep()
                    } else {
                        viewModel.finishDriverOnboarding()
                    }
                },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = WhiteCanvas
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = WhiteCanvas,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = if (uiState.currentStep < 3) "Next Step" else "Complete Registration",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WhiteCanvas
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepVehicleSelect(
    selectedCar: TataCarModel,
    onSelect: (TataCarModel) -> Unit
) {
    Column {
        Text(
            text = "STEP 1 OF 3",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select your Tata EV",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "We personalize charger speeds, range estimates and connector compatibility for your exact battery capacity.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(380.dp)
        ) {
            items(TataCarCatalog.models) { car ->
                val isSelected = car == selectedCar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) TintGreen else SurfaceBg)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGreen else BorderHairline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelect(car) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isSelected) PrimaryGreen else WhiteCanvas, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = if (isSelected) WhiteCanvas else InkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = car.name,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Text(
                                text = "${car.variant} • ${car.batteryKwh} kWh Pack",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepConnectorPreference(
    selectedConnector: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "CCS2 & Type 2 AC (Recommended)" to "Compatible with all public DC fast chargers & community home wallboxes.",
        "CCS2 DC Fast Only" to "Prioritize high-voltage DC stations (30-50 kW) for quick intercity top-ups.",
        "Type 2 AC Wallbox Only" to "Standard 3.3 kW or 7.2 kW overnight destination charging bays."
    )

    Column {
        Text(
            text = "STEP 2 OF 3",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Preferred connector",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Tata EVs support both European standard Type 2 AC and CCS2 DC. Choose your regular charging style.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        options.forEach { (title, subtitle) ->
            val isSelected = selectedConnector == title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) TintGreen else SurfaceBg)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryGreen else BorderHairline,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(title) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isSelected) PrimaryGreen else WhiteCanvas, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ElectricBolt,
                            contentDescription = null,
                            tint = if (isSelected) WhiteCanvas else InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNfcKeychain(
    keychainTagId: String,
    isScanning: Boolean,
    onScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STEP 3 OF 3",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "NFC Key Tag (Optional)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Register your physical VoltMate RFID tag or use your phone's NFC to tap and initiate charging without opening apps.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .background(TintGreen.copy(alpha = 0.4f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(if (keychainTagId.isNotBlank()) TintGreen else SurfaceBg, CircleShape)
                    .border(
                        1.5.dp,
                        if (keychainTagId.isNotBlank()) PrimaryGreen else BorderHairline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Nfc,
                    contentDescription = null,
                    tint = if (keychainTagId.isNotBlank()) PrimaryGreen else InkPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (keychainTagId.isNotBlank()) {
            Box(
                modifier = Modifier
                    .background(TintGreen, RoundedCornerShape(12.dp))
                    .border(1.dp, PrimaryGreen, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "TAG REGISTERED: $keychainTagId",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "Hold tag near the back of your phone",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = InkMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onScan() },
            enabled = !isScanning,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    color = PrimaryGreen,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning NFC Tag...", color = PrimaryGreen)
            } else {
                Text(
                    text = if (keychainTagId.isBlank()) "Tap to Scan Tag" else "Re-scan New Tag",
                    color = PrimaryGreen
                )
            }
        }
    }
}
