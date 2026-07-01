package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo

import com.example.ui.util.BodaLang
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(BodaLang.get(viewModel.appLanguage, "trip_completed"), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Your ride with ${trip.riderName} is finished.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Rider Card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    Text(trip.riderName.first().uppercase(), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(trip.riderName, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                    Text(trip.riderPlate, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UGX ${trip.fare.toInt()}",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                    Text("via ${trip.paymentMethod}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Receipt Details
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Official Boda Receipt", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Charged Amount", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Text("UGX ${trip.fare.toInt()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Payment Method", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                    Text(trip.paymentMethod, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Trip ID Reference", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                    Text("BODA-TRIP-${trip.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                BodaSecondaryButton(
                    text = "Download PDF Receipt",
                    onClick = { /* Simulate Receipt Download */ },
                    icon = Icons.Default.Download,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stars Rating Form
        Text(BodaLang.get(viewModel.appLanguage, "rate_rider"), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            for (i in 1..5) {
                IconButton(onClick = { starRating = i }) {
                    Icon(
                        imageVector = if (i <= starRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = feedbackComment,
            onValueChange = { feedbackComment = it },
            placeholder = { Text(BodaLang.get(viewModel.appLanguage, "comment_hint"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

        Spacer(modifier = Modifier.height(24.dp))

        if (!paymentConfirmed) {
            if (processingPayment) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Confirming MTN/Airtel Mobile Money escrow transfer...", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
