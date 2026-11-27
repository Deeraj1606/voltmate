package com.company.voltmate.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.company.voltmate.data.model.Charger
import com.company.voltmate.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun VoltMateMapView(
    chargers: List<Charger>,
    selectedCharger: Charger?,
    onChargerSelected: (Charger) -> Unit,
    onChargerBookClick: (Charger) -> Unit,
    modifier: Modifier = Modifier,
    initialLat: Double = 13.0827,
    initialLng: Double = 80.2707,
    showBookButton: Boolean = true
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var activeCharger by remember(selectedCharger) { mutableStateOf(selectedCharger) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mapViewInstance?.onPause()
                mapViewInstance?.onDetach()
            } catch (e: Exception) {
                // cleanup
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(initialLat, initialLng))
                    mapViewInstance = this
                }
            },
            update = { mapView ->
                mapViewInstance = mapView
                mapView.overlays.clear()

                // Current reference center pin (User Location)
                val centerMarker = Marker(mapView).apply {
                    position = GeoPoint(initialLat, initialLng)
                    title = "My EV Location"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    val dot = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(AndroidColor.parseColor("#10B981"))
                        setStroke(4, AndroidColor.WHITE)
                        setSize(36, 36)
                    }
                    icon = dot
                }
                mapView.overlays.add(centerMarker)

                // Charger pins
                chargers.forEach { charger ->
                    val isSelected = activeCharger?.id == charger.id
                    val markerColor = when {
                        charger.status != "AVAILABLE" -> "#9CA3AF"
                        charger.isDc -> "#F59E0B"
                        else -> "#059669"
                    }

                    val pin = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(AndroidColor.parseColor(markerColor))
                        setStroke(if (isSelected) 6 else 3, if (isSelected) AndroidColor.parseColor("#0D1F17") else AndroidColor.WHITE)
                        val sizePx = if (isSelected) 44 else 34
                        setSize(sizePx, sizePx)
                    }

                    val marker = Marker(mapView).apply {
                        position = GeoPoint(charger.lat, charger.lng)
                        title = charger.name
                        snippet = "${charger.powerKw} kW · ₹${charger.pricePerKwh}/kWh"
                        icon = pin
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            activeCharger = charger
                            onChargerSelected(charger)
                            mapView.controller.animateTo(GeoPoint(charger.lat, charger.lng))
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // Floating Map Controls (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter to Chennai EV hub
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.controller?.animateTo(GeoPoint(initialLat, initialLng))
                    mapViewInstance?.controller?.setZoom(14.5)
                },
                containerColor = WhiteCanvas,
                contentColor = DeepGreen,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Center Location")
            }

            // Zoom In
            SmallFloatingActionButton(
                onClick = { mapViewInstance?.controller?.zoomIn() },
                containerColor = WhiteCanvas,
                contentColor = InkPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom In")
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { mapViewInstance?.controller?.zoomOut() },
                containerColor = WhiteCanvas,
                contentColor = InkPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out")
            }
        }

        // Selected Charger Preview Card (Floating at Bottom)
        AnimatedVisibility(
            visible = activeCharger != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            activeCharger?.let { charger ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChargerBookClick(charger) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (charger.isDc) TintGreen else Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EvStation,
                                    contentDescription = null,
                                    tint = if (charger.isDc) WarningAmber else PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        charger.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    if (charger.isDc) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = WarningAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "DC FAST",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarningAmber,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    charger.address,
                                    fontSize = 12.sp,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { activeCharger = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = InkSecondary)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = TintGreen) {
                                    Text(
                                        "${charger.powerKw} kW",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DeepGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    "₹${charger.pricePerKwh}/kWh",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = InkPrimary
                                )

                                Text(
                                    "★ ${charger.rating}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = PrimaryGreen
                                )
                            }

                            if (showBookButton) {
                                Button(
                                    onClick = { onChargerBookClick(charger) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryGreen,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Book Slot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
