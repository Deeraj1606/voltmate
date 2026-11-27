package com.company.voltmate.ui.onboarding

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.ChargerType
import com.company.voltmate.data.model.ParkingType
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
fun OwnerOnboardingScreen(
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

                // Step dots 1..4
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..4) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            when (uiState.currentStep) {
                1 -> OwnerStep1Hardware(
                    chargerType = uiState.chargerType,
                    parkingType = uiState.parkingType,
                    onSelectType = { viewModel.updateOwnerChargerType(it) },
                    onSelectParking = { viewModel.updateOwnerParking(it) }
                )
                2 -> OwnerStep2Schedule(
                    selectedDays = uiState.selectedDays,
                    openFrom = uiState.openFrom,
                    openTo = uiState.openTo,
                    ownerPresent = uiState.ownerPresenceRequired,
                    pricePerKwh = uiState.pricePerKwh,
                    onToggleDay = { viewModel.toggleOwnerDay(it) },
                    onHoursChange = { from, to -> viewModel.updateOwnerHours(from, to) },
                    onPresenceChange = { viewModel.setOwnerPresence(it) },
                    onPriceChange = { viewModel.setPricePerKwh(it) }
                )
                3 -> OwnerStep3Location(
                    name = uiState.chargerName,
                    address = uiState.address,
                    lat = uiState.lat,
                    lng = uiState.lng,
                    onSave = { n, a, lt, lg -> viewModel.setLocationDetails(n, a, lt, lg) }
                )
                4 -> OwnerStep4Review(
                    uiState = uiState
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (uiState.currentStep < 4) {
                        viewModel.nextStep()
                    } else {
                        viewModel.finishOwnerOnboarding()
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
                        text = if (uiState.currentStep < 4) "Next Step" else "Publish Charger",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WhiteCanvas
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OwnerStep1Hardware(
    chargerType: ChargerType,
    parkingType: ParkingType,
    onSelectType: (ChargerType) -> Unit,
    onSelectParking: (ParkingType) -> Unit
) {
    Column {
        Text(
            text = "STEP 1 OF 4",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Text(
            text = "Charger & Parking Setup",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Specify your charger hardware rating and parking accessibility for incoming Tata EV drivers.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Text(
            text = "CHARGER HARDWARE TYPE",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        ChargerType.values().forEach { type ->
            val isSelected = type == chargerType
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) TintGreen else SurfaceBg)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryGreen else BorderHairline,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectType(type) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ElectricBolt,
                        contentDescription = null,
                        tint = if (isSelected) PrimaryGreen else InkSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.displayName,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Text(
                            text = if (type.isDc) "Fast DC CCS2 • Suitable for highway/express" else "AC Type 2 • Ideal for home/office slots",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PARKING BAY TYPE",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        ParkingType.values().forEach { parking ->
            val isSelected = parking == parkingType
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) TintGreen else SurfaceBg)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryGreen else BorderHairline,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectParking(parking) }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = parking.displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerStep2Schedule(
    selectedDays: List<String>,
    openFrom: String,
    openTo: String,
    ownerPresent: Boolean,
    pricePerKwh: Double,
    onToggleDay: (String) -> Unit,
    onHoursChange: (String, String) -> Unit,
    onPresenceChange: (Boolean) -> Unit,
    onPriceChange: (Double) -> Unit
) {
    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column {
        Text(
            text = "STEP 2 OF 4",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Text(
            text = "Schedule & Rates",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Set your weekly open days, operational hours, presence requirements and rate per kWh.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Text(
            text = "OPEN DAYS",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allDays.forEach { day ->
                val selected = selectedDays.contains(day)
                FilterChip(
                    selected = selected,
                    onClick = { onToggleDay(day) },
                    label = { Text(day) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TintGreen,
                        selectedLabelColor = DeepGreen
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = BorderHairline,
                        selectedBorderColor = PrimaryGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "OPEN HOURS (24H FORMAT)",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = openFrom,
                onValueChange = { onHoursChange(it, openTo) },
                label = { Text("From") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderHairline
                )
            )
            OutlinedTextField(
                value = openTo,
                onValueChange = { onHoursChange(openFrom, it) },
                label = { Text("To") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderHairline
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Owner presence toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceBg, RoundedCornerShape(14.dp))
                .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Owner presence required",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = "Require yourself to be home/at shop to guide driver parking",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
                Switch(
                    checked = ownerPresent,
                    onCheckedChange = { onPresenceChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WhiteCanvas,
                        checkedTrackColor = PrimaryGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Price per kWh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRICE PER KWH",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = InkMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${pricePerKwh.toInt()} / kWh",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = pricePerKwh.toFloat(),
            onValueChange = { onPriceChange(it.toDouble()) },
            valueRange = 10f..25f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryGreen,
                activeTrackColor = PrimaryGreen,
                inactiveTrackColor = BorderHairline
            )
        )
    }
}

@Composable
private fun OwnerStep3Location(
    name: String,
    address: String,
    lat: Double,
    lng: Double,
    onSave: (String, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var chargerName by remember { mutableStateOf(name.ifBlank { "Green Bay Point" }) }
    var chargerAddress by remember { mutableStateOf(address.ifBlank { "Anna Nagar, Chennai" }) }
    var currentLat by remember { mutableStateOf(lat) }
    var currentLng by remember { mutableStateOf(lng) }

    Column {
        Text(
            text = "STEP 3 OF 4",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Text(
            text = "Location & Name",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Drivers will see this name on the map and use navigation to drive directly to your charger.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = chargerName,
            onValueChange = {
                chargerName = it
                onSave(chargerName, chargerAddress, currentLat, currentLng)
            },
            label = { Text("Charger Name") },
            placeholder = { Text("e.g. Alwarpet Villa Fast Charger") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderHairline
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = chargerAddress,
            onValueChange = {
                chargerAddress = it
                onSave(chargerName, chargerAddress, currentLat, currentLng)
            },
            label = { Text("Street Address") },
            placeholder = { Text("Door no, Street name, Area, City") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderHairline
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // "Use my current location" Pin Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TintGreen, RoundedCornerShape(14.dp))
                .border(1.dp, PrimaryGreen, RoundedCornerShape(14.dp))
                .clickable {
                    // Fetch device location or default to Chennai
                    try {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (lastKnown != null) {
                            currentLat = lastKnown.latitude
                            currentLng = lastKnown.longitude
                        } else {
                            currentLat = 13.0827
                            currentLng = 80.2707
                        }
                    } catch (e: Exception) {
                        currentLat = 13.0827
                        currentLng = 80.2707
                    }
                    onSave(chargerName, chargerAddress, currentLat, currentLng)
                }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = WhiteCanvas,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Use Current Location Pin",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = DeepGreen
                    )
                    Text(
                        text = "Lat: ${"%.4f".format(currentLat)}, Lng: ${"%.4f".format(currentLng)}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerStep4Review(
    uiState: OnboardingUiState
) {
    Column {
        Text(
            text = "STEP 4 OF 4",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = PrimaryGreen
        )
        Text(
            text = "Review & Publish",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Text(
            text = "Verify your listing details. Your bay will immediately go live on the VoltMate network.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Gradient Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepGreen, Color(0xFF0D5E48))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VOLTMATE HOST BAY",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(TintGreen, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LIVE ON MAP",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = DeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.chargerName.ifBlank { "Host Charger Bay" },
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = WhiteCanvas
                )

                Text(
                    text = uiState.address.ifBlank { "Chennai, Tamil Nadu" },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HARDWARE",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${uiState.powerKw} kW (${uiState.chargerType.displayName})",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = WhiteCanvas
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RATE",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${uiState.pricePerKwh.toInt()} / kWh",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = WhiteCanvas
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HOURS",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${uiState.openFrom} - ${uiState.openTo}",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = WhiteCanvas
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PARKING",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = uiState.parkingType.displayName,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = WhiteCanvas
                        )
                    }
                }
            }
        }
    }
}
