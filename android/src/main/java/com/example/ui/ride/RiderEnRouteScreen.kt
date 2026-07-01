package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.chat.initiateCall
import com.example.ui.chat.openRiderChat

import com.example.ui.components.Sp
import com.example.ui.util.BodaLang
import com.example.ui.components.BodaButton
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
import androidx.compose.ui.unit.sp

// --- SCREEN 7: RIDER EN ROUTE ---
@Composable
fun RiderEnRouteScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var displaySafetySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Sp.lg)
    ) {
        Text(BodaLang.get(viewModel.appLanguage, "rider_enroute"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            if (viewModel.simulationCountdown > 0) "Arriving in ${viewModel.simulationCountdown}s..." else BodaLang.get(viewModel.appLanguage, "rider_arrived"),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(Sp.sm))

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

        Spacer(modifier = Modifier.height(Sp.sm))

        // Rider profile details & security verification code card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(Sp.md),
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
                Spacer(modifier = Modifier.width(Sp.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.riderName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Bike Plate: ${trip.riderPlate}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Verified community rider", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Security OTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("5892", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // CALL, MESSAGE, SHARE LOCATION & SOS BUTTONS
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BodaSecondaryButton(
                text = "Call",
                onClick = { viewModel.initiateCall(trip.riderName, trip.riderPhone) },
                icon = Icons.Default.Call,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Sp.sm))
            BodaSecondaryButton(
                text = "Message",
                onClick = { viewModel.openRiderChat() },
                icon = Icons.Default.Message,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Sp.sm))
            BodaButton(
                text = "SOS",
                onClick = { displaySafetySheet = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // ACTIVATE ON RIDER ARRIVAL BUTTON
        if (viewModel.simulationCountdown == 0) {
            BodaButton(
                text = "Enter security OTP & Start Trip",
                onClick = { viewModel.startActiveTrip() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_ride_btn"
            )
        } else {
            BodaButton(
                text = "Cancel Booking",
                onClick = { viewModel.cancelActiveTrip("Rider taking too long") },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (displaySafetySheet) {
            SafetyActionsOverlay(viewModel = viewModel, onClose = { displaySafetySheet = false })
        }
    }
}
