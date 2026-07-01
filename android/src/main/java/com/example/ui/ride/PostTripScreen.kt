package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.components.Color
import com.example.ui.components.Sp
import com.example.ui.components.BodaLang
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaSecondaryButton
import com.example.ui.components.BodaCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// --- SCREEN 9: POST-TRIP RATING & RECEIPT SUMMARY ---
@Composable
fun PostTripScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var starRating by remember { mutableStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var processingPayment by remember { mutableStateOf(false) }
    var paymentConfirmed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(Sp.sm))
        Text(BodaLang.get(viewModel.appLanguage, "trip_completed"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.xs))
        Text("Your ride with ${trip.riderName} is finished.", color = Color(0xFF94A3B8), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(Sp.md))

        // Rider Card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center) {
                    Text(trip.riderName.first().uppercase(), color = Color(0xFFFDB913),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(trip.riderName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(trip.riderPlate, color = Color(0xFF64748B), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UGX ${trip.fare.toInt()}",
                        color = Color(0xFFFDB913), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("via ${trip.paymentMethod}", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Receipt Details
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Official Boda Receipt", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Charged Amount", color = Color.White, fontSize = 14.sp)
                    Text("UGX ${trip.fare.toInt()}", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Payment Method", color = Color.White, fontSize = 12.sp)
                    Text(trip.paymentMethod, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Trip ID Reference", color = Color.White, fontSize = 12.sp)
                    Text("BODA-TRIP-${trip.id}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaSecondaryButton(
                    text = "Download PDF Receipt",
                    onClick = { /* Simulate Receipt Download */ },
                    icon = Icons.Default.Download,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Stars Rating Form
        Text(BodaLang.get(viewModel.appLanguage, "rate_rider"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(Sp.sm))
        Row {
            for (i in 1..5) {
                IconButton(onClick = { starRating = i }) {
                    Icon(
                        imageVector = if (i <= starRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFDB913),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        OutlinedTextField(
            value = feedbackComment,
            onValueChange = { feedbackComment = it },
            placeholder = { Text(BodaLang.get(viewModel.appLanguage, "comment_hint"), color = Color(0xFF475569)) },
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

        Spacer(modifier = Modifier.height(Sp.lg))

        if (!paymentConfirmed) {
            if (processingPayment) {
                CircularProgressIndicator(color = Color(0xFFFDB913))
                Spacer(modifier = Modifier.height(Sp.sm))
                Text("Confirming MTN/Airtel Mobile Money escrow transfer...", color = Color(0xFFFDB913), fontSize = 12.sp)
            } else {
                BodaButton(
                    text = "Release Escrow & Pay",
                    onClick = {
                        processingPayment = true
                        // Simulate escrow check
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(4000)
                            processingPayment = false
                            paymentConfirmed = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_and_finish_btn")
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.sm)) {
                BodaSecondaryButton(
                    text = "Skip",
                    onClick = {
                        viewModel.currentSimulationTrip = null
                        viewModel.simulationState = "idle"
                        viewModel.navigateTo(Screen.Home)
                    },
                    modifier = Modifier.weight(1f)
                )
                BodaButton(
                    text = "Done",
                    onClick = { viewModel.submitPostTripRating(starRating, feedbackComment) },
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
