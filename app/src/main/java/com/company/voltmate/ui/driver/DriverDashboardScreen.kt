package com.company.voltmate.ui.driver

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.Booking
import com.company.voltmate.data.model.Charger
import com.company.voltmate.data.model.ChargingSession
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.ui.components.VoltMateMapView
import com.company.voltmate.ui.theme.*

enum class DriverTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map),
    TRIPS("Trips", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    viewModel: DriverViewModel,
    userProfile: UserProfile?,
    onChargerClick: (Charger) -> Unit,
    onBookingClick: (Booking) -> Unit,
    onProfileClick: () -> Unit,
    onActiveSessionClick: (String) -> Unit,
    onSwitchToOwner: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DriverTab.HOME) }
    val chargers by viewModel.chargers.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SurfaceBg,
        bottomBar = {
            NavigationBar(
                containerColor = WhiteCanvas,
                tonalElevation = 4.dp
            ) {
                DriverTab.entries.forEach { tab ->
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

// Wallet Balance Card
Box(
    modifier = Modifier
        .offset(y = (-24).dp)
        .padding(horizontal = 16.dp)
) {
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
                Text("VoltMate In-App Wallet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkPrimary)
                Text("Balance: ₹%.2f".format(wallet.balanceRupees), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGreen)
                Text("Use for charging or receive host earnings", fontSize = 11.sp, color = InkSecondary)
            }
            Button(
                onClick = onNavigateToWallet,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TintGreen, contentColor = DeepGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
    ) { padding ->
        // Wallet Balance Card (moved)
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                DriverTab.HOME -> DriverHomeTab(
                    chargers = chargers,
                    userProfile = userProfile,
                    wallet = wallet,
                    onChargerClick = onChargerClick,
                    onFindMore = { selectedTab = DriverTab.MAP },
                    onSwitchToOwner = onSwitchToOwner,
                    onNavigateToWallet = onNavigateToWallet
                )
                DriverTab.MAP -> DriverMapListTab(
                    chargers = chargers,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onChargerClick = onChargerClick
                )
                DriverTab.TRIPS -> DriverTripsTab(
                    bookings = bookings,
                    chargers = chargers,
                    onBookingClick = onBookingClick,
                    onActiveSessionClick = onActiveSessionClick,
                    onFindCharger = { selectedTab = DriverTab.MAP }
                )
                DriverTab.PROFILE -> DriverProfileTab(
                    userProfile = userProfile,
                    wallet = wallet,
                    onProfileClick = onProfileClick,
                    onSwitchToOwner = onSwitchToOwner,
                    onNavigateToWallet = onNavigateToWallet,
                    onSignOut = onSignOut,
                    sessions = sessions
                )
            }
        }
    }
}

@Composable
fun DriverHomeTab(
    chargers: List<Charger>,
    userProfile: UserProfile?,
    wallet: com.company.voltmate.data.model.Wallet,
    onChargerClick: (Charger) -> Unit,
    onFindMore: () -> Unit,
    onSwitchToOwner: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val availableCount = chargers.count { it.status == "AVAILABLE" }
    val dcCount = chargers.count { it.isDc }
    val topRatedCount = chargers.count { it.rating >= 4.8 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero gradient banner
        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
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
                                "EV Driver Network",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                userProfile?.name ?: "Tata EV Driver",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        // Host Mode Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.clickable(onClick = onSwitchToOwner)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.EvStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Host Bay",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$availableCount community chargers ready near Chennai",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Quick Stats row
        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DriverQuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle,
                    label = "Available",
                    value = "$availableCount",
                    color = PrimaryGreen
                )
                DriverQuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Bolt,
                    label = "DC Fast",
                    value = "$dcCount",
                    color = WarningAmber
                )
                DriverQuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Star,
                    label = "Top Rated",
                    value = "$topRatedCount",
                    color = Color(0xFF8B5CF6)
                )
            }
        }

        // Dual-Role Prompt: Own a home charger?
        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {modifier = Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSwitchToOwner),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TintGreen, contentColor = DeepGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EvStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Have an idle home charger?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepGreen)
                            Text("Share your socket with fellow Tata owners & earn ₹₹", fontSize = 11.sp, color = InkSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }

        // Section header
        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-8).dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nearby Community Chargers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Text(
                    "View all",
                    fontSize = 13.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onFindMore)
                )
            }
        }

        // Chargers list
        if (chargers.isEmpty()) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
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
                        Text("No chargers available right now", color = InkSecondary)
                    }
                }
            }
        } else {
            items(chargers.take(6)) { charger ->
                // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {modifier = Modifier.offset(y = (-8).dp).padding(horizontal = 16.dp)) {
                    ChargerMapCard(charger = charger, onClick = { onChargerClick(charger) })
                }
            }
        }
    }
}

@Composable
fun DriverQuickStatCard(
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
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkPrimary)
            Text(label, fontSize = 10.sp, color = InkSecondary)
        }
    }
}

@Composable
fun ChargerMapCard(charger: Charger, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (charger.isDc) Brush.linearGradient(listOf(WarningAmber, Color(0xFFFF8C00)))
                        else Brush.linearGradient(listOf(PrimaryGreen, AccentGreen))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EvStation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    charger.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    charger.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${charger.powerKw} kW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (charger.isDc) WarningAmber else PrimaryGreen
                    )
                    Text(" · ", color = InkSecondary, fontSize = 12.sp)
                    Text("₹${charger.pricePerKwh}/kWh", fontSize = 12.sp, color = InkSecondary)
                    Text(" · ", color = InkSecondary, fontSize = 12.sp)
                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
                    Text(" ${charger.rating}", fontSize = 12.sp, color = InkSecondary, fontWeight = FontWeight.Medium)
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMapListTab(
    chargers: List<Charger>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onChargerClick: (Charger) -> Unit
) {
    var isMapView by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedChargerOnMap by remember { mutableStateOf<Charger?>(null) }

    val filtered = chargers.filter { charger ->
        val matchesSearch = searchQuery.isBlank() ||
                charger.name.contains(searchQuery, ignoreCase = true) ||
                charger.address.contains(searchQuery, ignoreCase = true) ||
                charger.chargerType.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "FAST" -> charger.isDc
            "AC" -> !charger.isDc
            "AVAILABLE" -> charger.status == "AVAILABLE"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        TopAppBar(
            title = {
                Column {
                    Text("Charger Explorer", fontWeight = FontWeight.Bold, color = InkPrimary)
                    Text(
                        if (isMapView) "Interactive Map (${filtered.size} nearby)" else "List View (${filtered.size} chargers)",
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                }
            },
            actions = {
                // Toggle Map vs List
                FilledTonalButton(
                    onClick = { isMapView = !isMapView },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TintGreen,
                        contentColor = DeepGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isMapView) Icons.Filled.List else Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isMapView) "List" else "Map",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteCanvas, titleContentColor = InkPrimary)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Search by area, power, type…", color = Color(0xFF6B7280)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF6B7280)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null, tint = Color(0xFF6B7280))
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InkPrimary,
                unfocusedTextColor = InkPrimary,
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderHairline,
                focusedContainerColor = WhiteCanvas,
                unfocusedContainerColor = WhiteCanvas,
                cursorColor = PrimaryGreen
            )
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("ALL" to "All", "AVAILABLE" to "Available", "FAST" to "DC Fast", "AC" to "AC Type 2")
            filterOptions.forEach { (key, label) ->
                val selected = selectedFilter == key
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) PrimaryGreen else WhiteCanvas,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.clickable { selectedFilter = key }
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else InkPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (isMapView) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {modifier = Modifier.weight(1f).fillMaxWidth()) {
                VoltMateMapView(
                    chargers = filtered,
                    selectedCharger = selectedChargerOnMap,
                    onChargerSelected = { selectedChargerOnMap = it },
                    onChargerBookClick = onChargerClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { charger ->
                    ChargerMapCard(charger = charger, onClick = { onChargerClick(charger) })
                }
                if (filtered.isEmpty()) {
                    // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching chargers found", color = InkSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTripsTab(
    bookings: List<Booking>,
    chargers: List<Charger>,
    onBookingClick: (Booking) -> Unit,
    onActiveSessionClick: (String) -> Unit,
    onFindCharger: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        TopAppBar(
            title = { Text("My Trips & Bookings", fontWeight = FontWeight.Bold, color = InkPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteCanvas, titleContentColor = InkPrimary)
        )

        if (bookings.isEmpty()) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("No bookings yet", color = InkPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Find an idle community charger near you and book a charging slot.", color = InkSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onFindCharger,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                    ) {
                        Text("Find Chargers", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookings) { booking ->
                    val charger = chargers.find { it.id == booking.chargerId }
                    BookingCard(
                        booking = booking,
                        chargerName = charger?.name ?: booking.chargerId,
                        onClick = {
                            if (booking.status == "ACTIVE") {
                                onActiveSessionClick(booking.id)
                            } else {
                                onBookingClick(booking)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking, chargerName: String, onClick: () -> Unit) {
    val statusColor = when (booking.status) {
        "BOOKED" -> PrimaryGreen
        "ARRIVED" -> WarningAmber
        "ACTIVE" -> Color(0xFF3B82F6)
        "COMPLETED" -> Color(0xFF6B7280)
        "CANCELLED" -> ErrorRed
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(chargerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InkPrimary)
                Text(
                    formatBookingTime(booking.slotStart, booking.slotEnd),
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    booking.status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun DriverProfileTab(
    userProfile: UserProfile?,
    wallet: com.company.voltmate.data.model.Wallet,
    onProfileClick: () -> Unit,
    onSwitchToOwner: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onSignOut: () -> Unit,
    sessions: List<ChargingSession>
) {
    var showVehicleSettings by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPaymentMethods by remember { mutableStateOf(false) }
    var showHelpSupport by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        // Wallet Balance Card (moved)modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(PrimaryGreen, AccentGreen))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userProfile?.name?.firstOrNull()?.uppercase() ?: "D",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            userProfile?.name ?: "Driver",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )
        Text(
            userProfile?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TintGreen
        ) {
            Text(
                "${userProfile?.carModel ?: "Tata Nexon.ev"} · ${userProfile?.preferredConnector ?: "CCS2"}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = DeepGreen,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // Big Primary Role Switch Button
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSwitchToOwner),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen, contentColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.EvStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Switch to Host Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("List your idle home charger & earn weekly", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        DriverProfileMenuItem(Icons.Filled.AccountBalanceWallet, "VoltMate Wallet (₹%.2f)".format(wallet.balanceRupees), onClick = onNavigateToWallet)
        DriverProfileMenuItem(Icons.Filled.DirectionsCar, "Vehicle & Charging Settings", onClick = { showVehicleSettings = true })
        DriverProfileMenuItem(Icons.Filled.History, "Charging History", onClick = { showHistoryDialog = true })
        DriverProfileMenuItem(Icons.Filled.Payment, "Payment Methods", onClick = { showPaymentMethods = true })
        DriverProfileMenuItem(Icons.Filled.Help, "Support & Tata Roadside Assistance", onClick = { showHelpSupport = true })

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.weight(1f))
    }

    // Vehicle Settings Dialog
    if (showVehicleSettings) {
        AlertDialog(
            onDismissRequest = { showVehicleSettings = false },
            title = { Text("Vehicle & Charging Setup", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selected EV: ${userProfile?.carModel ?: "Tata Nexon.ev 45kWh"}", fontWeight = FontWeight.SemiBold, color = InkPrimary, fontSize = 14.sp)
                    Text("Connector: ${userProfile?.preferredConnector ?: "CCS2 & Type 2"}", color = InkSecondary, fontSize = 13.sp)
                    Text("NFC Security Tag: Linked", color = PrimaryGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("Battery Reserve Alert: 20%", color = InkSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVehicleSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Save & Close")
                }
            },
            containerColor = WhiteCanvas
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Charging History", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Past 30 Days Summary", fontWeight = FontWeight.SemiBold, color = InkPrimary, fontSize = 13.sp)
                    Text("Total Energy Charged: 48.2 kWh", color = InkSecondary, fontSize = 13.sp)
                    Text("Total Amount Spent: ₹674.80", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Community Stations Visited: 4", color = InkSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("OK")
                }
            },
            containerColor = WhiteCanvas
        )
    }

    // Payment Methods Dialog
    if (showPaymentMethods) {
        AlertDialog(
            onDismissRequest = { showPaymentMethods = false },
            title = { Text("Payment Methods", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Active: Tata Pay UPI (••••@okhdfcbank)", fontWeight = FontWeight.SemiBold, color = PrimaryGreen, fontSize = 14.sp)
                    Text("Backup: HDFC Credit Card (ending 8821)", color = InkSecondary, fontSize = 13.sp)
                    Text("Auto-Pay After Connector Release: Enabled", color = InkSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPaymentMethods = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Done")
                }
            },
            containerColor = WhiteCanvas
        )
    }

    // Support Dialog
    if (showHelpSupport) {
        AlertDialog(
            onDismissRequest = { showHelpSupport = false },
            title = { Text("Driver Support & Roadside", fontWeight = FontWeight.Bold, color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tata EV Roadside Assistance:", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 13.sp)
                    Text("📞 1800 209 8282 (Toll-Free 24/7)", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("VoltMate Host Issue Helpline:", fontWeight = FontWeight.Bold, color = InkPrimary, fontSize = 13.sp)
                    Text("💬 WhatsApp: +91 98840 12345", color = InkSecondary, fontSize = 13.sp)
                    Text("⚡ Safe charging with real-time connector locking", color = InkSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpSupport = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Close")
                }
            },
            containerColor = WhiteCanvas
        )
    }
}

@Composable
fun DriverProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
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

private fun formatBookingTime(start: Long, end: Long): String {
    if (start == 0L) return "Today"
    val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
    return "${sdf.format(java.util.Date(start))} – ${sdf.format(java.util.Date(end))}"
}
