package com.company.voltmate.ui.driver

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.ui.theme.*

enum class ChargingStep { NFC_TAP, CHARGING, PAYMENT, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSessionScreen(
    viewModel: DriverViewModel,
    charger: Charger,
    bookingId: String,
    userProfile: UserProfile?,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val nfcState by viewModel.nfcState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val sessionComplete by viewModel.sessionComplete.collectAsStateWithLifecycle()
    val demoSpeed by viewModel.demoSpeed.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(ChargingStep.NFC_TAP) }

    LaunchedEffect(nfcState) {
        if (nfcState is NfcUiState.Authorized) {
            currentStep = ChargingStep.CHARGING
        }
    }

    LaunchedEffect(sessionComplete) {
        if (sessionComplete != null) {
            currentStep = ChargingStep.PAYMENT
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charging Session", fontWeight = FontWeight.Bold, color = InkPrimary) },
                navigationIcon = {
                    if (currentStep == ChargingStep.NFC_TAP) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteCanvas, titleContentColor = InkPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SurfaceBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            StepIndicator(currentStep)
            Spacer(Modifier.height(20.dp))

            when (currentStep) {
                ChargingStep.NFC_TAP -> NfcTapSection(
                    viewModel = viewModel,
                    charger = charger,
                    bookingId = bookingId,
                    userProfile = userProfile,
                    nfcState = nfcState
                )

                ChargingStep.CHARGING -> ChargingLiveSection(
                    viewModel = viewModel,
                    charger = charger,
                    bookingId = bookingId,
                    userProfile = userProfile,
                    demoSpeed = demoSpeed
                )

                ChargingStep.PAYMENT -> PaymentSection(
                    viewModel = viewModel,
                    sessionComplete = sessionComplete,
                    charger = charger,
                    onPaid = {
                        currentStep = ChargingStep.DONE
                    }
                )

                ChargingStep.DONE -> DoneSection(onDone = onDone)
            }
        }
    }
}

@Composable
fun StepIndicator(current: ChargingStep) {
    val steps = listOf("NFC", "Charging", "Pay", "Done")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val step = ChargingStep.entries[index]
            val isActive = current.ordinal >= step.ordinal
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isActive) PrimaryGreen else BorderHairline),
                    contentAlignment = Alignment.Center
                ) {
                    if (current.ordinal > step.ordinal) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${index + 1}", fontSize = 12.sp, color = if (isActive) Color.White else InkMuted, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 10.sp, color = if (isActive) PrimaryGreen else InkMuted, fontWeight = FontWeight.Medium)
            }
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .background(if (current.ordinal > step.ordinal) PrimaryGreen else BorderHairline)
                )
            }
        }
    }
}

@Composable
fun NfcTapSection(
    viewModel: DriverViewModel,
    charger: Charger,
    bookingId: String,
    userProfile: UserProfile?,
    nfcState: NfcUiState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "nfc_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // NFC animation circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(if (nfcState is NfcUiState.Scanning) scale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = 0.2f),
                            PrimaryGreen.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryGreen, AccentGreen))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Tap NFC to Authenticate",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Hold your phone or NFC key tag near the charger\nto verify your identity and unlock the connector.",
            textAlign = TextAlign.Center,
            color = InkSecondary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        when (nfcState) {
            is NfcUiState.Scanning -> {
                CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("Scanning…", color = PrimaryGreen, fontWeight = FontWeight.Medium)
            }
            is NfcUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        (nfcState as NfcUiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = ErrorRed,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            else -> {}
        }

        // Simulate NFC tap button (demo mode)
        Button(
            onClick = {
                val tagId = userProfile?.keychainTagId?.ifBlank { "demo_tag_${System.currentTimeMillis()}" }
                    ?: "demo_tag_${System.currentTimeMillis()}"
                viewModel.simulateNfcTap(bookingId, charger.id, tagId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = nfcState !is NfcUiState.Scanning,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Icon(Icons.Filled.Nfc, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Simulate NFC Tap", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ChargingLiveSection(
    viewModel: DriverViewModel,
    charger: Charger,
    bookingId: String,
    userProfile: UserProfile?,
    demoSpeed: Int
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    var hasStarted by remember { mutableStateOf(false) }

    // Auto-start charging
    LaunchedEffect(Unit) {
        if (!hasStarted) {
            hasStarted = true
            viewModel.startCharging(
                charger = charger,
                bookingId = bookingId,
                initialSoc = 25,
                batteryKwh = userProfile?.carBatteryKwh ?: 45.0
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "charging_pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ), label = "bolt_rotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SoC ring
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { (telemetry.socPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.size(160.dp),
                color = if (telemetry.socPercent > 80) AccentGreen else PrimaryGreen,
                trackColor = BorderHairline,
                strokeWidth = 10.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${telemetry.socPercent}%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen
                )
                Text("SoC", fontSize = 12.sp, color = InkMuted)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (telemetry.isCharging) {
            Icon(
                Icons.Filled.Bolt,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(rotation * 0.1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Telemetry grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(Modifier.weight(1f), "Power", String.format("%.1f kW", telemetry.powerKw), PrimaryGreen)
            TelemetryCard(Modifier.weight(1f), "Energy", String.format("%.2f kWh", telemetry.energyKwh), Color(0xFF3B82F6))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(Modifier.weight(1f), "Voltage", String.format("%.0f V", telemetry.voltageV), WarningAmber)
            TelemetryCard(Modifier.weight(1f), "Current", String.format("%.1f A", telemetry.currentA), Color(0xFF8B5CF6))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(Modifier.weight(1f), "Duration", formatDuration(telemetry.sessionDurationSec), InkSecondary)
            TelemetryCard(Modifier.weight(1f), "Cost", String.format("₹%.1f", telemetry.runningCostRs), PrimaryGreen)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(Modifier.weight(1f), "Temp", String.format("%.1f°C", telemetry.temperatureC), ErrorRed)
            TelemetryCard(Modifier.weight(1f), "Speed", "${demoSpeed}x", InkSecondary)
        }

        Spacer(Modifier.height(16.dp))

        // Demo speed controls
        Text("Demo Speed", fontSize = 12.sp, color = InkSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 10, 60).forEach { speed ->
                val selected = demoSpeed == speed
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setDemoSpeed(speed) },
                    label = { Text("${speed}x") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = WhiteCanvas,
                        labelColor = InkPrimary,
                        selectedContainerColor = PrimaryGreen,
                        selectedLabelColor = Color.White
                    ),
                    border = if (selected) null else FilterChipDefaults.filterChipBorder(
                        borderColor = BorderHairline,
                        borderWidth = 1.dp,
                        enabled = true,
                        selected = false
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Stop button
        Button(
            onClick = { viewModel.stopCharging(charger, bookingId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.StopCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Stop Charging", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TelemetryCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = InkSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun PaymentSection(
    viewModel: DriverViewModel,
    sessionComplete: SessionCompleteData?,
    charger: Charger,
    onPaid: () -> Unit
) {
    val data = sessionComplete ?: return
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    var selectedMethod by remember { mutableStateOf("WALLET") } // "WALLET" or "UPI"
    var isPaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var quickAddSuccess by remember { mutableStateOf(false) }

    val totalAmount = data.amount
    val isWalletShort = wallet.balanceRupees < totalAmount
    val shortAmount = (totalAmount - wallet.balanceRupees).coerceAtLeast(0.0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Session Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = InkPrimary)
                Spacer(Modifier.height(16.dp))

                PaymentRow("Energy Used", String.format("%.2f kWh", data.kWh))
                PaymentRow("Rate", "₹${charger.pricePerKwh}/kWh")
                PaymentRow("Duration", formatDuration(data.durationSec))
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                PaymentRow("Total Payable", String.format("₹%.2f", data.amount), bold = true)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Payment Method Selection
        Text(
            "Select Payment Method",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = InkPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Option 1: VoltMate Wallet (First & Default)
        val isWalletSelected = selectedMethod == "WALLET"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedMethod = "WALLET" },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isWalletSelected) TintGreen.copy(alpha = 0.5f) else WhiteCanvas,
                contentColor = InkPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                if (isWalletSelected) 2.dp else 1.dp,
                if (isWalletSelected) PrimaryGreen else BorderHairline
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VoltMate Wallet", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = TintGreen) {
                                Text("DEFAULT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "Available: ₹%.2f".format(wallet.balanceRupees),
                            fontSize = 12.sp,
                            color = if (isWalletShort) WarningAmber else PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    RadioButton(
                        selected = isWalletSelected,
                        onClick = { selectedMethod = "WALLET" },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                    )
                }

                if (isWalletSelected && isWalletShort) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Short by ₹%.2f".format(shortAmount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Button(
                                onClick = {
                                    val needed = Math.ceil(shortAmount)
                                    viewModel.topUpWallet(needed) {
                                        quickAddSuccess = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Add ₹%.0f".format(Math.ceil(shortAmount)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Option 2: Tata Pay UPI / Card
        val isUpiSelected = selectedMethod == "UPI"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedMethod = "UPI" },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUpiSelected) TintGreen.copy(alpha = 0.5f) else WhiteCanvas,
                contentColor = InkPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                if (isUpiSelected) 2.dp else 1.dp,
                if (isUpiSelected) PrimaryGreen else BorderHairline
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Payment, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tata Pay UPI / Cards (Simulated)", fontWeight = FontWeight.SemiBold, color = InkPrimary, fontSize = 14.sp)
                    Text("Auto-debits exact session amount", fontSize = 12.sp, color = InkSecondary)
                }
                RadioButton(
                    selected = isUpiSelected,
                    onClick = { selectedMethod = "UPI" },
                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "⚠️ Connector unlocks instantly upon confirmed payment",
            fontSize = 12.sp,
            color = WarningAmber,
            textAlign = TextAlign.Center
        )

        errorMessage?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        val canPay = (selectedMethod == "UPI") || (!isWalletShort)

        Button(
            onClick = {
                isPaying = true
                errorMessage = null
                viewModel.payAndUnlock(charger, data.bookingId, data.amount, selectedMethod) { success, err ->
                    isPaying = false
                    if (success) {
                        onPaid()
                    } else {
                        errorMessage = err
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = canPay && !isPaying,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
        ) {
            if (isPaying) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.LockOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pay ₹%.2f & Unlock Connector".format(data.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun PaymentRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = InkSecondary, fontSize = 14.sp)
        Text(
            value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (bold) 18.sp else 14.sp,
            color = if (bold) PrimaryGreen else InkPrimary
        )
    }
}

@Composable
fun DoneSection(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(TintGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "All Done! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Connector unlocked. Safe travels!",
            color = InkSecondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%dh %02dm", h, m)
    else String.format("%dm %02ds", m, s)
}
