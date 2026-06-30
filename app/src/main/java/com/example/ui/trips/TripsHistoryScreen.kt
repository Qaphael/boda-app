package com.example.ui.trips

import com.example.data.*
import com.example.ui.BodaViewModel
import com.example.ui.ride.disputeTrip
import com.example.ui.Screen
import com.example.ui.components.Color
import com.example.ui.components.Sp
import com.example.ui.components.BodaButton
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
import androidx.compose.ui.unit.sp

@Composable
fun TripsHistoryScreen(viewModel: BodaViewModel, trips: List<Trip>) {
    var displayDisputeDialog by remember { mutableStateOf<Trip?>(null) }
    var disputeReason by remember { mutableStateOf("") }
    var disputeDetails by remember { mutableStateOf("") }

    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFDB913))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Text("Your Trips & Deliveries", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(Sp.sm))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Trips",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("No trips yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("Your past ride and delivery ledger will appear here.", color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
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
                                        tint = Color(0xFFFDB913)
                                    )
                                    Spacer(modifier = Modifier.width(Sp.sm))
                                    Text(trip.type.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(
                                    text = "UGX ${trip.fare.toInt()}",
                                    color = Color(0xFFFDB913),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("From: ${trip.pickupName}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("To: ${trip.dropoffName}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(Sp.sm))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Rider: ${trip.riderName} (${trip.riderPlate})", color = Color.White, fontSize = 11.sp)
                                Text(
                                    text = trip.status.uppercase(),
                                    color = when (trip.status) {
                                        "completed" -> Color(0xFF10B981)
                                        "canceled" -> Color(0xFFEF4444)
                                        else -> Color(0xFFFDB913)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            if (trip.status == "completed") {
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Row {
                                    BodaButton(
                                        text = "Dispute Fare / Trip",
                                        onClick = { displayDisputeDialog = trip },
                                        containerColor = Color(0xFFE4002B),
                                        contentColor = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else if (trip.status == "disputed") {
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE4002B).copy(alpha = 0.2f))
                                        .padding(8.dp)
                                ) {
                                    Text("Disputed Filed: ${trip.disputeReason}. Gulu team is reviewing evidence.", color = Color(0xFFEF4444), fontSize = 11.sp)
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
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Dispute Boda Trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
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
                                    tint = Color(0xFFFDB913)
                                )
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(reason, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        OutlinedTextField(
                            value = disputeDetails,
                            onValueChange = { disputeDetails = it },
                            placeholder = { Text("Provide details / description of incident", color = Color(0xFF475569)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFFFDB913),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Sp.md))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { displayDisputeDialog = null }) {
                                Text("Cancel", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(Sp.sm))
                            BodaButton(
                                text = "Submit Dispute",
                                onClick = {
                                    viewModel.disputeTrip(disputedTrip.id, disputeReason, disputeDetails)
                                    displayDisputeDialog = null
                                },
                                enabled = disputeReason.isNotEmpty() && disputeDetails.isNotEmpty(),
                                containerColor = Color(0xFFE4002B),
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
