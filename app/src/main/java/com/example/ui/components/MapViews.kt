package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedPlace
import com.example.ui.BodaViewModel

fun getLatLngForPlace(name: String): com.google.android.gms.maps.model.LatLng {
    val s = name.lowercase()
    return when {
        s.contains("university") || s.contains("laroo") -> com.google.android.gms.maps.model.LatLng(2.7842, 32.3214)
        s.contains("hospital") || s.contains("lacor") -> com.google.android.gms.maps.model.LatLng(2.7933, 32.2571)
        s.contains("market") || s.contains("home") || s.contains("cereleno") || s.contains("roundabout") -> com.google.android.gms.maps.model.LatLng(2.7712, 32.2985)
        s.contains("stadium") || s.contains("pece") -> com.google.android.gms.maps.model.LatLng(2.7745, 32.3112)
        else -> com.google.android.gms.maps.model.LatLng(2.7750, 32.2950)
    }
}

fun generateDetailedRoute(start: com.google.android.gms.maps.model.LatLng, end: com.google.android.gms.maps.model.LatLng): List<com.google.android.gms.maps.model.LatLng> {
    val path = mutableListOf<com.google.android.gms.maps.model.LatLng>()
    path.add(start)
    val latDiff = end.latitude - start.latitude
    val lngDiff = end.longitude - start.longitude

    val p1 = com.google.android.gms.maps.model.LatLng(start.latitude + latDiff * 0.4, start.longitude)
    val p2 = com.google.android.gms.maps.model.LatLng(start.latitude + latDiff * 0.4, start.longitude + lngDiff * 0.6)
    val p3 = com.google.android.gms.maps.model.LatLng(end.latitude, start.longitude + lngDiff * 0.6)

    path.add(p1)
    path.add(p2)
    path.add(p3)
    path.add(end)
    return path
}

fun getLatLngOnPath(path: List<com.google.android.gms.maps.model.LatLng>, progress: Float): com.google.android.gms.maps.model.LatLng {
    if (path.isEmpty()) return com.google.android.gms.maps.model.LatLng(2.775, 32.295)
    if (path.size == 1) return path[0]
    if (progress <= 0f) return path.first()
    if (progress >= 1f) return path.last()

    val totalSegments = path.size - 1
    val segmentProgress = progress * totalSegments
    val currentSegmentIndex = segmentProgress.toInt().coerceAtMost(totalSegments - 1)
    val localProgress = segmentProgress - currentSegmentIndex

    val startPt = path[currentSegmentIndex]
    val endPt = path[currentSegmentIndex + 1]

    return com.google.android.gms.maps.model.LatLng(
        startPt.latitude + (endPt.latitude - startPt.latitude) * localProgress,
        startPt.longitude + (endPt.longitude - startPt.longitude) * localProgress
    )
}

@Composable
fun GoogleMapViewWrapper(
    modifier: Modifier = Modifier,
    pickupLatLng: com.google.android.gms.maps.model.LatLng,
    dropoffLatLng: com.google.android.gms.maps.model.LatLng?,
    riderProgress: Float,
    simulationState: String,
    routePoints: List<com.google.android.gms.maps.model.LatLng> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember { com.google.android.gms.maps.MapView(context) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = {
            mapView.apply {
                onCreate(android.os.Bundle())
                onResume()
                getMapAsync { googleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isMapToolbarEnabled = false
                    googleMap.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
                }
            }
        },
        modifier = modifier,
        update = { mapV ->
            mapV.getMapAsync { googleMap ->
                googleMap.clear()

                googleMap.addMarker(
                    com.google.android.gms.maps.model.MarkerOptions()
                        .position(pickupLatLng)
                        .title("Pickup Point")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN))
                )

                if (dropoffLatLng != null) {
                    googleMap.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions()
                            .position(dropoffLatLng)
                            .title("Drop-off Point")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                    )

                    val detailedRoute = if (routePoints.isNotEmpty()) {
                        routePoints
                    } else {
                        generateDetailedRoute(pickupLatLng, dropoffLatLng)
                    }

                    val polylineOpts = com.google.android.gms.maps.model.PolylineOptions()
                        .color(android.graphics.Color.parseColor("#FDB913"))
                        .width(8f)
                    detailedRoute.forEach { pt ->
                        polylineOpts.add(pt)
                    }
                    googleMap.addPolyline(polylineOpts)

                    if (simulationState in listOf("enroute", "accepted", "active")) {
                        val vehicleLatLng = if (simulationState == "active") {
                            getLatLngOnPath(detailedRoute, riderProgress)
                        } else {
                            val centerLat = 2.775
                            val centerLng = 32.295
                            val enrouteStart = com.google.android.gms.maps.model.LatLng(centerLat, centerLng)
                            val enrouteRoute = generateDetailedRoute(enrouteStart, pickupLatLng)
                            getLatLngOnPath(enrouteRoute, riderProgress)
                        }

                        googleMap.addMarker(
                            com.google.android.gms.maps.model.MarkerOptions()
                                .position(vehicleLatLng)
                                .title("Boda Motorcycle")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }

                    try {
                        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                            .include(pickupLatLng)
                            .include(dropoffLatLng)
                            .build()
                        googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    } catch (e: Exception) {
                        googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f))
                    }
                } else {
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15f))
                }
            }
        }
    )
}

@Composable
fun GuluMapView(
    modifier: Modifier = Modifier,
    pickup: SavedPlace? = null,
    dropoff: SavedPlace? = null,
    riderProgress: Float = 0f,
    simulationState: String = "idle",
    isDriverMode: Boolean = false,
    driverTripState: String = "none",
    driverPickupName: String? = null,
    driverDropoffName: String? = null,
    driverProgress: Float = 0f,
    viewModel: BodaViewModel? = null
) {
    val hasMapsApiKey = try {
        com.example.BuildConfig.MAPS_API_KEY.isNotEmpty() &&
        com.example.BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY" &&
        com.example.BuildConfig.MAPS_API_KEY != "MAPS_API_KEY_DEFAULT_VALUE"
    } catch (e: Throwable) {
        false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (hasMapsApiKey) {
            val pickupLatLng = if (isDriverMode && driverPickupName != null) {
                getLatLngForPlace(driverPickupName)
            } else if (pickup != null) {
                com.google.android.gms.maps.model.LatLng(pickup.latitude, pickup.longitude)
            } else {
                com.google.android.gms.maps.model.LatLng(2.775, 32.295)
            }

            val dropoffLatLng = if (isDriverMode && driverDropoffName != null) {
                getLatLngForPlace(driverDropoffName)
            } else if (dropoff != null) {
                com.google.android.gms.maps.model.LatLng(dropoff.latitude, dropoff.longitude)
            } else {
                null
            }

            val progress = if (isDriverMode) driverProgress else riderProgress
            val activeState = if (isDriverMode) driverTripState else simulationState

            GoogleMapViewWrapper(
                modifier = Modifier.fillMaxSize(),
                pickupLatLng = pickupLatLng,
                dropoffLatLng = dropoffLatLng,
                riderProgress = progress,
                simulationState = activeState,
                routePoints = viewModel?.osrmRoutePoints ?: emptyList()
            )
        } else {
            GuluCanvasMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = pickup,
                dropoff = dropoff,
                riderProgress = riderProgress,
                simulationState = simulationState,
                isDriverMode = isDriverMode,
                driverTripState = driverTripState,
                driverPickupName = driverPickupName,
                driverDropoffName = driverDropoffName,
                driverProgress = driverProgress
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ComposeColor(0xFFFDB913), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        "Set MAPS_API_KEY in Secrets Panel to unlock live Google Maps",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GuluCanvasMapView(
    modifier: Modifier = Modifier,
    pickup: SavedPlace? = null,
    dropoff: SavedPlace? = null,
    riderProgress: Float = 0f,
    simulationState: String = "idle",
    isDriverMode: Boolean = false,
    driverTripState: String = "none",
    driverPickupName: String? = null,
    driverDropoffName: String? = null,
    driverProgress: Float = 0f
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
    ) {
        val w = size.width
        val h = size.height

        val streetColor = Color(0xFF334155)
        val roadStroke = 4.dp.toPx()

        drawCircle(
            color = streetColor,
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = roadStroke)
        )

        drawCircle(
            color = Color(0xFF475569),
            radius = w * 0.08f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = roadStroke)
        )

        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, 0f),
            end = Offset(w * 0.5f, h),
            strokeWidth = roadStroke
        )
        drawLine(
            color = streetColor,
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = roadStroke
        )
        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(w, h * 0.1f),
            strokeWidth = roadStroke
        )
        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(0f, h * 0.3f),
            strokeWidth = roadStroke
        )

        val landmarkColor = Color(0xFF64748B)
        drawCircle(Color(0xFFFDB913), radius = 8.dp.toPx(), center = Offset(w * 0.52f, h * 0.52f))
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.12f, h * 0.34f))
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.88f, h * 0.18f))
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.68f, h * 0.68f))

        fun getCoords(label: String, name: String, isPickup: Boolean): Offset {
            val s = (label + " " + name).lowercase()
            return when {
                s.contains("university") || s.contains("laroo") || s.contains("school") -> Offset(w * 0.88f, h * 0.18f)
                s.contains("hospital") || s.contains("lacor") || s.contains("layibi") || s.contains("work") -> Offset(w * 0.12f, h * 0.34f)
                s.contains("market") || s.contains("home") || s.contains("cereleno") || s.contains("roundabout") -> Offset(w * 0.52f, h * 0.52f)
                s.contains("stadium") || s.contains("pece") -> Offset(w * 0.68f, h * 0.68f)
                else -> if (isPickup) Offset(w * 0.4f, h * 0.45f) else Offset(w * 0.65f, h * 0.58f)
            }
        }

        val activePickup: Offset?
        val activeDropoff: Offset?
        val activeProgress: Float
        val activeState: String

        if (isDriverMode) {
            activePickup = if (driverPickupName != null) getCoords("", driverPickupName, true) else null
            activeDropoff = if (driverDropoffName != null) getCoords("", driverDropoffName, false) else null
            activeProgress = driverProgress
            activeState = driverTripState
        } else {
            activePickup = if (pickup != null) getCoords(pickup.label, pickup.name, true) else null
            activeDropoff = if (dropoff != null) getCoords(dropoff.label, dropoff.name, false) else null
            activeProgress = riderProgress
            activeState = simulationState
        }

        fun generateCanvasDetailedRoute(start: Offset, end: Offset): List<Offset> {
            val path = mutableListOf<Offset>()
            path.add(start)
            val dx = end.x - start.x
            val dy = end.y - start.y

            val p1 = Offset(start.x + dx * 0.4f, start.y)
            val p2 = Offset(start.x + dx * 0.4f, start.y + dy * 0.6f)
            val p3 = Offset(end.x, start.y + dy * 0.6f)

            path.add(p1)
            path.add(p2)
            path.add(p3)
            path.add(end)
            return path
        }

        fun getOffsetOnPath(path: List<Offset>, progress: Float): Offset {
            if (path.isEmpty()) return Offset(0f, 0f)
            if (path.size == 1) return path[0]
            if (progress <= 0f) return path.first()
            if (progress >= 1f) return path.last()

            val totalSegments = path.size - 1
            val segmentProgress = progress * totalSegments
            val currentSegmentIndex = segmentProgress.toInt().coerceAtMost(totalSegments - 1)
            val localProgress = segmentProgress - currentSegmentIndex

            val startPt = path[currentSegmentIndex]
            val rPt = path[currentSegmentIndex + 1]

            return Offset(
                startPt.x + (rPt.x - startPt.x) * localProgress,
                startPt.y + (rPt.y - startPt.y) * localProgress
            )
        }

        if (activePickup != null && activeDropoff != null && activeState != "none" && activeState != "idle") {
            val canvasRoute = generateCanvasDetailedRoute(activePickup, activeDropoff)
            val routePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(canvasRoute.first().x, canvasRoute.first().y)
                for (i in 1 until canvasRoute.size) {
                    lineTo(canvasRoute[i].x, canvasRoute[i].y)
                }
            }
            drawPath(
                path = routePath,
                color = Color(0xFFFDB913),
                style = Stroke(
                    width = 5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            drawCircle(Color(0xFF10B981), radius = 7.dp.toPx(), center = activePickup)
            drawCircle(Color(0xFFE4002B), radius = 7.dp.toPx(), center = activeDropoff)

            if (activeState in listOf("enroute", "accepted", "pickup_arrived", "active")) {
                val currentX: Float
                val currentY: Float

                if (activeState == "enroute" || activeState == "accepted") {
                    val startX = w * 0.5f
                    val startY = h * 0.5f
                    val enrouteStart = Offset(startX, startY)
                    val enrouteRoute = generateCanvasDetailedRoute(enrouteStart, activePickup)
                    val vehicleOffset = getOffsetOnPath(enrouteRoute, activeProgress)
                    currentX = vehicleOffset.x
                    currentY = vehicleOffset.y
                } else if (activeState == "active") {
                    val vehicleOffset = getOffsetOnPath(canvasRoute, activeProgress)
                    currentX = vehicleOffset.x
                    currentY = vehicleOffset.y
                } else {
                    currentX = activePickup.x
                    currentY = activePickup.y
                }

                drawCircle(
                    color = Color(0xFF0061A4).copy(alpha = 0.4f),
                    radius = 16.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = Color(0xFFFDB913),
                    radius = 9.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = ComposeColor.Black,
                    radius = 4.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}
