package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.offline.dispatchSOSSMS

import com.example.ui.util.BodaLang
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaErrorButton
import com.example.ui.components.BodaSecondaryButton
import com.example.ui.components.BodaCard
import com.example.ui.components.GuluMapView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

fun getSimulatedSpeed(progress: Float): Int {
    val baseSpeed = 28
    val phase = (progress * 50).toInt() % 5
    val offset = when (phase) {
        0 -> -2
        1 -> 1
        2 -> -1
        3 -> 2
        4 -> 0
        else -> 0
    }
    return (baseSpeed + offset).coerceAtLeast(4)
}

fun getActiveNavigationStep(pickupName: String, dropoffName: String, progress: Float, realSteps: List<BodaViewModel.NavStep> = emptyList()): String {
    if (realSteps.isNotEmpty()) {
        val index = (progress * realSteps.size).toInt().coerceIn(0, realSteps.size - 1)
        return realSteps[index].instruction
    }
    val steps = listOf(
        "Depart from $pickupName onto local street grid.",
        "Turn left onto Gulu Commercial Highway.",
        "Pass through Pece Stadium Roundabout, proceed straight.",
        "Almost there! Entering neighborhood street.",
        "Approaching destination. Slowing down to arrive safely at $dropoffName."
    )
    val index = (progress * steps.size).toInt().coerceIn(0, steps.size - 1)
    return steps[index]
}

@Composable
fun ActiveTripScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var displaySafetySheet by remember { mutableStateOf(false) }
    var showAllSteps by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(BodaLang.get(viewModel.appLanguage, "active_trip"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text("Transit to ${trip.dropoffName.take(18)}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
            }
            AssistChip(
                onClick = {},
                label = { Text("${viewModel.simulationCountdown}s ETA", style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // NAVIGATION TURN-BY-TURN CARD (TOP BAR OF MAP OVERLAY)
        BodaCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Nav",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIVE BODA NAVIGATION INSTRUCTION", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = getActiveNavigationStep(trip.pickupName, trip.dropoffName, viewModel.simulationRouteProgress, viewModel.navigationSteps),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        // Expandable navigation steps list
        if (viewModel.navigationSteps.isNotEmpty()) {
            BodaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ALL NAV STEPS (${viewModel.navigationSteps.size})", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = if (showAllSteps) "Hide" else "Show",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { showAllSteps = !showAllSteps }
                        )
                    }
                    if (showAllSteps) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val currentStepIdx = (viewModel.simulationRouteProgress * viewModel.navigationSteps.size).toInt()
                            .coerceIn(0, viewModel.navigationSteps.size - 1)
                        viewModel.navigationSteps.forEachIndexed { idx, step ->
                            val isCurrent = idx == currentStepIdx
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${idx + 1}", color = if (isCurrent) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.instruction,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                        style = if (isCurrent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${step.distanceMeters / 1000.0} km · ${step.durationSeconds / 60} min",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Map drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                riderProgress = viewModel.simulationRouteProgress,
                simulationState = "active",
                viewModel = viewModel,
                userLocation = viewModel.currentLocation
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // INSTRUMENTATION PANEL: SPEEDOMETER & SAFETY SHIELD STATUS
        BodaCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("SIMULATED SPEED", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        val speed = getSimulatedSpeed(viewModel.simulationRouteProgress)
                        Text(
                            text = "$speed km/h",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BODA-WATCH STATUS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Speed Safe", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BodaSecondaryButton(
                text = "All Steps",
                onClick = { showAllSteps = true },
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { displaySafetySheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.Emergency, null)
                Spacer(Modifier.width(8.dp))
                Text("SOS", style = MaterialTheme.typography.labelLarge)
            }
        }

        // All steps list popup dialog
        if (showAllSteps) {
            AlertDialog(
                onDismissRequest = { showAllSteps = false },
                title = { Text("Trip Route Directions", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val steps = listOf(
                            "Depart from ${trip.pickupName} onto local street grid.",
                            "Turn left onto Gulu Commercial Highway.",
                            "Pass through Pece Stadium Roundabout, proceed straight.",
                            "Almost there! Entering neighborhood street.",
                            "Approaching destination. Slowing down to arrive safely at ${trip.dropoffName}."
                        )
                        steps.forEachIndexed { idx, st ->
                            val isCurrent = (viewModel.simulationRouteProgress * steps.size).toInt().coerceIn(0, steps.size - 1) == idx
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(st, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {
                    BodaButton(
                        text = "Close",
                        onClick = { showAllSteps = false },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (displaySafetySheet) {
            SafetyActionsOverlay(viewModel = viewModel, onClose = { displaySafetySheet = false })
        }
    }
}

// --- WIDGET: SAFETY SOS EMERGENCY PANEL ---
@Composable
fun SafetyActionsOverlay(viewModel: BodaViewModel, onClose: () -> Unit) {
    var activeCallContact by remember { mutableStateOf<String?>(null) }
    var activeCallNumber by remember { mutableStateOf<String?>(null) }
    var showShareSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            .clickable {
                if (activeCallContact == null && !showShareSuccess) {
                    onClose()
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // prevent dismissing when clicking card content
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize()
            ) {
                if (activeCallContact != null) {
                    // Simulating Active Emergency Call UI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Active Call",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "DIALING GULU HELPLINE...",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = activeCallContact ?: "",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = activeCallNumber ?: "",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LinearProgressIndicator(
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Simulating encrypted community watch connection...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )

                        // Render Emergency SMS dispatches dynamically
                        if (viewModel.emergencySMSDispatchLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Emergency SMS Dispatches:", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    viewModel.emergencySMSDispatchLogs.forEach { log ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(log, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        BodaSecondaryButton(
                            text = "Cancel Call",
                            onClick = {
                                activeCallContact = null
                                activeCallNumber = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (showShareSuccess) {
                    // Live Location Link Copied Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Trip Tracking Link Copied",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share this secure link with family or on WhatsApp:\nhttps://boda-gulu.ug/track/BODA-LIVE-SECURE",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    BodaButton(
                        text = "Share Live Trip Tracking Link",
                        onClick = { showShareSuccess = true },
                        icon = Icons.Default.Share,
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                } else {
                    // Primary Gulu Safety Board
                    Text("Gulu Boda Safety Center", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Active satellite tracking and immediate local community dispatch.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    // SOS Red Dial Button
                    BodaErrorButton(
                        text = "1-Tap Gulu SOS (0800 112 112)",
                        onClick = {
                            activeCallContact = "Gulu Boda Dispatch"
                            activeCallNumber = "0800 112 112"
                            viewModel.dispatchSOSSMS()
                        },
                        icon = Icons.Default.Call,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gulu Directory Title
                    Text("Local Gulu Emergency Directory", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    val safetyHotlines = listOf(
                        Triple("Gulu Central Police Station", "0471 432022", "Main division desk"),
                        Triple("Lacor Hospital Emergency Unit", "0471 432494", "24/7 Trauma ward"),
                        Triple("Pece Boda-Watch Patrol", "0772 401402", "Community riders watch")
                    )

                    safetyHotlines.forEach { (name, num, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    activeCallContact = name
                                    activeCallNumber = num
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(num, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Call, contentDescription = "Dial", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 2: Share Live Location Link
            BodaErrorButton(
                text = "Gulu Safety & SOS",
                onClick = { onClose() },
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1.5f)
            )

                    Spacer(modifier = Modifier.height(8.dp))

                    BodaSecondaryButton(
                        text = "Close Safety Panel",
                        onClick = { onClose() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
