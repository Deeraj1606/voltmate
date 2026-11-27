package com.company.voltmate.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.Charger
import com.company.voltmate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    charger: Charger,
    viewModel: DriverViewModel,
    onBack: () -> Unit,
    onBookingConfirmed: (String) -> Unit
) {
    val bookingState by viewModel.bookingState.collectAsStateWithLifecycle()
    var selectedSlotIndex by remember { mutableIntStateOf(-1) }

    // Generate time slots for today and tomorrow
    val slots = remember { generateSlots(charger) }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingUiState.Success) {
            val booking = (bookingState as BookingUiState.Success).booking
            onBookingConfirmed(booking.id)
            viewModel.resetBookingState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book a Slot", fontWeight = FontWeight.Bold, color = InkPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary)
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
                .padding(16.dp)
        ) {
            // Charger summary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.EvStation,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(charger.name, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                        Text(
                            "${charger.powerKw} kW · ₹${charger.pricePerKwh}/kWh",
                            fontSize = 13.sp,
                            color = InkSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Select a Time Slot", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = InkPrimary)
            Spacer(Modifier.height(8.dp))

            // Slot list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEachIndexed { index, slot ->
                    val selected = selectedSlotIndex == index
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) TintGreen else WhiteCanvas,
                            contentColor = InkPrimary
                        ),
                        border = if (selected) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)
                        ) else null,
                        onClick = { selectedSlotIndex = index }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(slot.label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = InkPrimary)
                                Text(slot.timeRange, fontSize = 12.sp, color = InkSecondary)
                            }
                            if (selected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Error
            if (bookingState is BookingUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        (bookingState as BookingUiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = ErrorRed,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Book button
            Button(
                onClick = {
                    if (selectedSlotIndex >= 0) {
                        val slot = slots[selectedSlotIndex]
                        viewModel.bookSlot(charger, slot.startMs, slot.endMs)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = selectedSlotIndex >= 0 && bookingState !is BookingUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (bookingState is BookingUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.BookOnline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Booking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

data class TimeSlot(
    val label: String,
    val timeRange: String,
    val startMs: Long,
    val endMs: Long
)

private fun generateSlots(charger: Charger): List<TimeSlot> {
    val slots = mutableListOf<TimeSlot>()
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    val openHour = charger.openFrom.split(":")[0].toIntOrNull() ?: 8
    val closeHour = charger.openTo.split(":")[0].toIntOrNull() ?: 22
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)

    // Generate 2-hour slots for today (remaining)
    val startHour = if (currentHour < openHour) openHour else currentHour + 1
    for (h in startHour until closeHour step 2) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.HOUR_OF_DAY, 2)
        if (endCal.get(Calendar.HOUR_OF_DAY) > closeHour) continue

        slots.add(
            TimeSlot(
                label = "Today",
                timeRange = "${sdf.format(startCal.time)} – ${sdf.format(endCal.time)}",
                startMs = startCal.timeInMillis,
                endMs = endCal.timeInMillis
            )
        )
        if (slots.size >= 3) break
    }

    // Tomorrow slots
    for (h in openHour until closeHour step 2) {
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.HOUR_OF_DAY, 2)
        if (endCal.get(Calendar.HOUR_OF_DAY) > closeHour) continue

        slots.add(
            TimeSlot(
                label = "Tomorrow",
                timeRange = "${sdf.format(startCal.time)} – ${sdf.format(endCal.time)}",
                startMs = startCal.timeInMillis,
                endMs = endCal.timeInMillis
            )
        )
        if (slots.size >= 6) break
    }

    return slots
}
