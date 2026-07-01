package com.example.ui.trips

import com.example.data.*
import com.example.ui.BodaViewModel
import com.example.ui.ride.disputeTrip
import com.example.ui.home.navigateTo
import com.example.ui.Screen

import com.example.ui.components.BodaButton
import com.example.ui.components.BodaErrorButton
import com.example.ui.components.BodaCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TripsHistoryScreen(viewModel: BodaViewModel, trips: List<Trip>) {
    var displayDisputeDialog by remember { mutableStateOf<Trip?>(null) }
    var disputeReason by remember { mutableStateOf("") }
    var disputeDetails by remember { mutableStateOf("") }

    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("Your Trips & Deliveries", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Trips",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No trips yet", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your past ride and delivery ledger will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    BodaButton(
                        text = "Book your first ride",
                        onClick = { viewModel.navigateTo(Screen.Home) }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(trips) { trip ->
                    BodaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (trip.type == "ride") Icons.Default.TwoWheeler else Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(trip.type.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = "UGX ${trip.fare.toInt()}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("From: ${trip.pickupName}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                            Text("To: ${trip.dropoffName}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Rider: ${trip.riderName} (${trip.riderPlate})", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = trip.status.uppercase(),
                                    color = when (trip.status) {
                                        "completed" -> MaterialTheme.colorScheme.tertiary
                                        "canceled" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            if (trip.status == "completed") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    BodaErrorButton(
                                        text = "Dispute Fare / Trip",
                                        onClick = { displayDisputeDialog = trip },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else if (trip.status == "disputed") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                        .padding(8.dp)
                                ) {
                                    Text("Disputed Filed: ${trip.disputeReason}. Gulu team is reviewing evidence.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayDisputeDialog != null) {
            val disputedTrip = displayDisputeDialog!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Dispute Boda Trip", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Wrong Route taken", "Rider was rude", "Overcharged", "Package damaged").forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { disputeReason = reason }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (disputeReason == reason) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(reason, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = disputeDetails,
                            onValueChange = { disputeDetails = it },
                            placeholder = { Text("Provide details / description of incident", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { displayDisputeDialog = null }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            BodaErrorButton(
                                text = "Submit Dispute",
                                onClick = {
                                    viewModel.disputeTrip(disputedTrip.id, disputeReason, disputeDetails)
                                    displayDisputeDialog = null
                                },
                                enabled = disputeReason.isNotEmpty() && disputeDetails.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}
