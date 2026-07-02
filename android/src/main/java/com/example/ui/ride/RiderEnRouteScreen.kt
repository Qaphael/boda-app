package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.chat.initiateCall
import com.example.ui.chat.openRiderChat

import com.example.ui.util.BodaLang
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaErrorButton
import com.example.ui.components.BodaSecondaryButton
import com.example.ui.components.BodaCard
import com.example.ui.components.GuluMapView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// --- SCREEN 7: RIDER EN ROUTE ---
@Composable
fun RiderEnRouteScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var displaySafetySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(BodaLang.get(viewModel.appLanguage, "rider_enroute"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Text(
            if (viewModel.simulationCountdown > 0) "Arriving in ${viewModel.simulationCountdown}s..." else BodaLang.get(viewModel.appLanguage, "rider_arrived"),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Gulu map tracking backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        ) {
            val progress = (8 - viewModel.simulationCountdown) / 8f
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                riderProgress = progress,
                simulationState = "enroute",
                viewModel = viewModel,
                userLocation = viewModel.currentLocation
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rider profile details & security verification code card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.riderName, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                    Text("Bike Plate: ${trip.riderPlate}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Verified community rider", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Security OTP", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text("5892", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CALL, MESSAGE, SHARE LOCATION & SOS BUTTONS
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BodaSecondaryButton(
                text = "Call",
                onClick = { viewModel.initiateCall(trip.riderName, trip.riderPhone) },
                icon = Icons.Default.Call,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BodaSecondaryButton(
                text = "Message",
                onClick = { viewModel.openRiderChat() },
                icon = Icons.Default.Message,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BodaErrorButton(
                text = "SOS",
                onClick = { displaySafetySheet = true },
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ACTIVATE ON RIDER ARRIVAL BUTTON
        if (viewModel.simulationCountdown == 0) {
            BodaButton(
                text = "Enter security OTP & Start Trip",
                onClick = { viewModel.startActiveTrip() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_ride_btn"
            )
        } else {
            BodaErrorButton(
                text = "Cancel Booking",
                onClick = { viewModel.cancelActiveTrip("Rider taking too long") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (displaySafetySheet) {
            SafetyActionsOverlay(viewModel = viewModel, onClose = { displaySafetySheet = false })
        }
    }
}
