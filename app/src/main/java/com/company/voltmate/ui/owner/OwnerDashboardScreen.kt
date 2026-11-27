package com.company.voltmate.ui.owner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.gateway.ChargerTelemetry
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargingSession
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.ui.components.VoltMateMapView
import com.company.voltmate.ui.theme.*

enum class OwnerTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map),
    CHARGERS("Chargers", Icons.Filled.EvStation, Icons.Outlined.EvStation),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerViewModel,
    userProfile: UserProfile?,
    onSignOut: () -> Unit,
    onSwitchToDriver: () -> Unit,
    onNavigateToCharger: (Charger) -> Unit,
    onNavigateToWallet: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(OwnerTab.DASHBOARD) }
    val chargers by viewModel.chargers.collectAsStateWithLifecycle()
    val communityChargers by viewModel.communityChargers.collectAsStateWithLifecycle()
    val totalEarnings by viewModel.totalEarnings.collectAsStateWithLifecycle()
    val totalEnergy by viewModel.totalEnergy.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val allSessions = sessions + viewModel.demoSessions

    val effectiveChargers = if (chargers.isEmpty()) listOf(viewModel.fallbackCharger) else chargers

    Scaffold(
        containerColor = SurfaceBg,
        bottomBar = {
            NavigationBar(
                containerColor = WhiteCanvas,
                tonalElevation = 4.dp
            ) {
                OwnerTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGreen,
                            selectedTextColor = PrimaryGreen,
                            unselectedIconColor = Color(0xFF6B7280),
                            unselectedTextColor = Color(0xFF6B7280),
                            indicatorColor = TintGreen
                        )
                    )
                }
            }
        }

        // Available Wallet Balance KPI Card
        Box(
            Box(modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToWallet),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TintGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available Wallet Balance", fontSize = 12.sp, color = InkSecondary)
                            Text("₹%.2f".format(wallet.balanceRupees), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text("Instant withdrawal or spend on charging", fontSize = 11.sp, color = InkSecondary)
                        }
                        Button(
                            onClick = onNavigateToWallet,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Withdraw", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                OwnerTab.DASHBOARD -> OwnerDashboardTab(
                    chargers = effectiveChargers,
                    totalEarnings = totalEarnings,
                    totalEnergy = totalEnergy,
                    wallet = wallet,
                    recentSessions = allSessions.take(5),
                    telemetry = telemetry,
                    userProfile = userProfile,
                    onSwitchToDriver = onSwitchToDriver,
                    onNavigateToWallet = onNavigateToWallet
                )
                OwnerTab.MAP -> OwnerMapTab(
                    myChargers = effectiveChargers,
                    communityChargers = communityChargers,
                    onBookCharger = onNavigateToCharger,
                    onSwitchToDriver = onSwitchToDriver
                )
                OwnerTab.CHARGERS -> OwnerChargersTab(
                    chargers = effectiveChargers,
                    onToggleStatus = { viewModel.toggleChargerStatus(it) }
                )
                OwnerTab.PROFILE -> OwnerProfileTab(
                    userProfile = userProfile,
                    wallet = wallet,
                    onSignOut = onSignOut,
                    onSwitchToDriver = onSwitchToDriver,
                    onNavigateToWallet = onNavigateToWallet,
                    allSessions = allSessions
                )
            }
        }
    }
}

@Composable
fun OwnerDashboardTab(
    chargers: List<Charger>,
    totalEarnings: Double,
    totalEnergy: Double,
    wallet: com.company.voltmate.data.model.Wallet,
    recentSessions: List<ChargingSession>,
    telemetry: ChargerTelemetry,
    userProfile: UserProfile?,
    onSwitchToDriver: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero gradient header
        Box(
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(listOf(DeepGreen, PrimaryGreen))
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Host Dashboard",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                userProfile?.name ?: "Charger Host",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        // Driver Mode Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.clickable(onClick = onSwitchToDriver)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Drive Mode",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats cards row (overlapping hero)
        Box(
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OwnerStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CurrencyRupee,
                    label = "Earnings",
                    value = String.format("₹%.0f", totalEarnings),
                    color = PrimaryGreen
                )
                OwnerStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Bolt,
                    label = "Energy",
                    value = String.format("%.1f kWh", totalEnergy),
                    color = Color(0xFF3B82F6)
                )
                OwnerStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.EvStation,
                    label = "Chargers",
                    value = "${chargers.size}",
                    color = WarningAmber
                )
            }
        }

        // Dual-Role Prompt: Need to charge?
        Box(
            Box(modifier = Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSwitchToDriver),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TintGreen, contentColor = DeepGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need to charge your EV today?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepGreen)
                            Text("Find & book community chargers across Chennai", fontSize = 11.sp, color = InkSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }

        // Live Telemetry Panel
        Box(
            Box(modifier = Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp)) {
                OwnerTelemetryCard(telemetry = telemetry, primaryCharger = chargers.firstOrNull())
            }
        }

        // 7-Day Performance Chart (Canvas Bar Chart)
        Box(
            Box(modifier = Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp)) {
                OwnerWeeklyChartCard(recentSessions = recentSessions)
            }
        }

        // Payout Breakdown Card
        Box(
            Box(modifier = Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp)) {
                OwnerPayoutBreakdownCard(totalEarnings = totalEarnings)
            }
        }

        // Recent Sessions header
        Box(
            Text(
                "Recent Charging Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp)
            )
        }

        // Sessions list items
        if (recentSessions.isEmpty()) {
            Box(
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No sessions recorded yet", color = InkSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(recentSessions) { session ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
fun OwnerTelemetryCard(telemetry: ChargerTelemetry, primaryCharger: Charger?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isCharging) WarningAmber else PrimaryGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Live Bay Telemetry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TintGreen
                ) {
                    Text(
                        if (telemetry.isCharging) "CHARGING" else "AVAILABLE",
                        color = DeepGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                primaryCharger?.name ?: "Tata Power Bay 1",
                fontSize = 13.sp,
                color = InkSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem("Voltage", String.format("%.0f V", telemetry.voltageV))
                TelemetryMetricItem("Current", String.format("%.1f A", telemetry.currentA))
                TelemetryMetricItem("Power", String.format("%.1f kW", telemetry.powerKw))
                TelemetryMetricItem("Temp", String.format("%.0f°C", telemetry.temperatureC))
            }
        }
    }
}

@Composable
fun TelemetryMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkPrimary)
        Text(label, fontSize = 10.sp, color = InkSecondary)
    }
}

@Composable
fun OwnerWeeklyChartCard(recentSessions: List<ChargingSession>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val values = listOf(140f, 280f, 210f, 420f, 350f, 560f, 490f)
    val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "7-Day Earnings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = InkPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceBg
                ) {
                    Text(
                        "Demo data",
                        fontSize = 10.sp,
                        color = InkSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val barWidth = 22.dp.toPx()
                val totalBars = values.size
                val spaceBetween = (size.width - (barWidth * totalBars)) / (totalBars + 1).coerceAtLeast(1)

                values.forEachIndexed { index, value ->
                    val safeValue = value.coerceAtLeast(0f)
                    val barHeight = ((safeValue / maxVal) * (size.height - 20.dp.toPx())).coerceAtLeast(4.dp.toPx())
                    val left = spaceBetween + index * (barWidth + spaceBetween)
                    val top = size.height - barHeight

                    drawRoundRect(
                        color = if (index == totalBars - 1) PrimaryGreen else TintGreen,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach { day ->
                    Text(day, fontSize = 11.sp, color = InkSecondary)
                }
            }
        }
    }
}

@Composable
fun OwnerPayoutBreakdownCard(totalEarnings: Double) {
    val gross = totalEarnings.coerceAtLeast(0.0)
    val fee = gross * 0.05
    val net = gross - fee

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Payout Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = InkPrimary
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gross Revenue", color = InkSecondary, fontSize = 13.sp)
                Text(String.format("₹%.2f", gross), fontWeight = FontWeight.SemiBold, color = InkPrimary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Platform Fee (5%)", color = InkSecondary, fontSize = 13.sp)
                Text(String.format("-₹%.2f", fee), color = ErrorRed, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BorderHairline)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Net Payout", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 14.sp)
                Text(String.format("₹%.2f", net), fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Weekly direct transfer to Bank Account ending in 4102",
                fontSize = 11.sp,
                color = InkSecondary
            )
        }
    }
}

@Composable
fun OwnerMapTab(
    myChargers: List<Charger>,
    communityChargers: List<Charger>,
    onBookCharger: (Charger) -> Unit,
    onSwitchToDriver: () -> Unit
) {
    var isMapView by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf("COMMUNITY") } // "COMMUNITY" or "MY_BAYS"
    var mapSearchQuery by remember { mutableStateOf("") }
    var selectedChargerOnMap by remember { mutableStateOf<Charger?>(null) }

    val activeList = if (viewMode == "COMMUNITY") {
        if (mapSearchQuery.isBlank()) communityChargers
        else communityChargers.filter {
            it.name.contains(mapSearchQuery, ignoreCase = true) ||
            it.address.contains(mapSearchQuery, ignoreCase = true)
        }
    } else {
        if (mapSearchQuery.isBlank()) myChargers
        else myChargers.filter {
            it.name.contains(mapSearchQuery, ignoreCase = true) ||
            it.address.contains(mapSearchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("EV Charger Network", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = InkPrimary)
                Text(
                    if (isMapView) "Interactive Map (${activeList.size} stations)" else "List View (${activeList.size} stations)",
                    fontSize = 11.sp,
                    color = InkSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { isMapView = !isMapView },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = TintGreen, contentColor = DeepGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(if (isMapView) Icons.Filled.List else Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isMapView) "List" else "Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onSwitchToDriver,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Drive Dashboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = mapSearchQuery,
            onValueChange = { mapSearchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search chargers by area, locality or name...", fontSize = 13.sp, color = InkSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryGreen) },
            trailingIcon = {
                if (mapSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { mapSearchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = InkSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = WhiteCanvas,
                unfocusedContainerColor = WhiteCanvas,
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderHairline,
                focusedTextColor = InkPrimary,
                unfocusedTextColor = InkPrimary
            ),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        // Toggle: Community Chargers vs My Bays
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isCommunity = viewMode == "COMMUNITY"
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isCommunity) PrimaryGreen else WhiteCanvas,
                border = if (isCommunity) null else androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
                modifier = Modifier.clickable { viewMode = "COMMUNITY" }
            ) {
                Text(
                    "Need a Charge (${communityChargers.size})",
                    color = if (isCommunity) Color.White else InkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (!isCommunity) PrimaryGreen else WhiteCanvas,
                border = if (!isCommunity) null else androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
                modifier = Modifier.clickable { viewMode = "MY_BAYS" }
            ) {
                Text(
                    "My Registered Bays (${myChargers.size})",
                    color = if (!isCommunity) Color.White else InkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isMapView) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                VoltMateMapView(
                    chargers = activeList,
                    selectedCharger = selectedChargerOnMap,
                    onChargerSelected = { selectedChargerOnMap = it },
                    onChargerBookClick = onBookCharger,
                    showBookButton = viewMode == "COMMUNITY"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList) { charger ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (charger.status == "AVAILABLE") TintGreen else Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EvStation,
                                    contentDescription = null,
                                    tint = if (charger.status == "AVAILABLE") PrimaryGreen else InkSecondary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        charger.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    if (charger.isDc) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = TintGreen) {
                                            Text("DC FAST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(
                                    charger.address,
                                    color = InkSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "${charger.powerKw} kW · ₹${charger.pricePerKwh}/kWh",
                                        fontSize = 11.sp,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "★ ${charger.rating}",
                                        fontSize = 11.sp,
                                        color = InkSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (viewMode == "COMMUNITY") {
                                Button(
                                    onClick = { onBookCharger(charger) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (charger.status == "AVAILABLE") TintGreen else Color(0xFFF3F4F6)
                                ) {
                                    Text(
                                        charger.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (charger.status == "AVAILABLE") DeepGreen else InkSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = InkPrimary)
            Text(label, fontSize = 10.sp, color = InkSecondary)
        }
    }
}

@Composable
fun SessionCard(session: ChargingSession) {
    val sdf = remember { java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TintGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(String.format("%.1f kWh", session.kWh), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InkPrimary)
                Text(sdf.format(java.util.Date(session.endedAt)), fontSize = 11.sp, color = InkSecondary)
            }
            Text(
                String.format("₹%.0f", session.amount),
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                fontSize = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerChargersTab(
    chargers: List<Charger>,
    onToggleStatus: (Charger) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        TopAppBar(
            title = { Text("My Chargers", fontWeight = FontWeight.Bold, color = InkPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteCanvas, titleContentColor = InkPrimary)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chargers) { charger ->
                OwnerChargerCard(charger = charger, onToggle = { onToggleStatus(charger) })
            }
        }
    }
}

@Composable
fun OwnerChargerCard(charger: Charger, onToggle: () -> Unit) {
    val isAvailable = charger.status == "AVAILABLE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAvailable) PrimaryGreen else Color(0xFF6B7280)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EvStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(charger.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkPrimary)
                        Text("${charger.powerKw} kW · ₹${charger.pricePerKwh}/kWh", fontSize = 12.sp, color = InkSecondary)
                    }
                }

                Switch(
                    checked = isAvailable,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BorderHairline
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BorderHairline)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(charger.address, fontSize = 12.sp, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun OwnerProfileTab(
    userProfile: UserProfile?,
    wallet: com.company.voltmate.data.model.Wallet,
    onSignOut: () -> Unit,
    onSwitchToDriver: () -> Unit,
    onNavigateToWallet: () -> Unit,
    allSessions: List<ChargingSession>
) {
    var showAccountSettings by remember { mutableStateOf(false) }
    var showBankDetails by remember { mutableStateOf(false) }
    var showSupportFaq by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(DeepGreen, PrimaryGreen))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userProfile?.name?.firstOrNull()?.uppercase() ?: "O",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            userProfile?.name ?: "Owner",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )
        Text(userProfile?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)

        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = TintGreen) {
            Text(
                "⚡ Charger Host & Tata EV Owner",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = DeepGreen,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // Big Primary Role Switch Button
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSwitchToDriver),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen, contentColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Switch to Driver Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("Find nearby chargers & charge your car", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        OwnerProfileItem(Icons.Filled.AccountBalanceWallet, "VoltMate Wallet (₹%.2f)".format(wallet.balanceRupees), onClick = onNavigateToWallet)
        OwnerProfileItem(Icons.Filled.Settings, "Account Settings", onClick = { showAccountSettings = true })
        OwnerProfileItem(Icons.Filled.AccountBalance, "Bank & Payout Details", onClick = { showBankDetails = true })
        OwnerProfileItem(Icons.Filled.Help, "Support & Roadside Helpline", onClick = { showSupportFaq = true })

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.SemiBold)
        }
    }

    // Account Settings Dialog
    if (showAccountSettings) {
        AlertDialog(
            onDismissRequest = { showAccountSettings = false },
            title = { Text("Account Settings", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Display Name: ${userProfile?.name ?: "Host"}", color = InkPrimary, fontSize = 14.sp)
                    Text("Registered Email: ${userProfile?.email ?: ""}", color = InkSecondary, fontSize = 13.sp)
                    Text("Role: EV Charger Host", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Community Notifications: Enabled", color = InkSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccountSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Close")
                }
            },
            containerColor = WhiteCanvas
        )
    }

    // Bank Details Dialog
    if (showBankDetails) {
        AlertDialog(
            onDismissRequest = { showBankDetails = false },
            title = { Text("Bank & Payout Setup", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bank: HDFC Bank Ltd.", fontWeight = FontWeight.SemiBold, color = InkPrimary, fontSize = 14.sp)
                    Text("Account: •••••••• 4102", color = InkSecondary, fontSize = 13.sp)
                    Text("IFSC Code: HDFC0001234", color = InkSecondary, fontSize = 13.sp)
                    Text("Payout Cycle: Automated every Monday", color = PrimaryGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBankDetails = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Done")
                }
            },
            containerColor = WhiteCanvas
        )
    }

    // Support & Helpline Dialog
    if (showSupportFaq) {
        AlertDialog(
            onDismissRequest = { showSupportFaq = false },
            title = { Text("Support & Helpline", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tata EV Roadside Assistance:", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 13.sp)
                    Text("📞 1800 209 8282 (Toll-Free 24/7)", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("VoltMate Community Support:", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 13.sp)
                    Text("💬 WhatsApp: +91 98840 12345", color = InkSecondary, fontSize = 13.sp)
                    Text("⚡ Safe charging protected by surge limiters", color = InkSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSupportFaq = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("OK")
                }
            },
            containerColor = WhiteCanvas
        )
    }
}

@Composable
fun OwnerProfileItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = InkPrimary)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp))
        }
    }
}
