package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.util.BodaLang
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaErrorButton
import com.example.ui.components.BodaTextButton
import com.example.ui.components.BodaTextField
import com.example.ui.components.BodaCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// --- SCREEN 6: MATCHING & WAITING ONBOARD SPINNER ---
@Composable
fun MatchingScreen(viewModel: BodaViewModel) {
    var showCancelReason by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Assigning closest Gulu Rider", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Boda Escrow secures this payment. You can cancel for free before matching.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Matching progress radar sweep
            val radarSurface = MaterialTheme.colorScheme.surface
            val radarPrimary = MaterialTheme.colorScheme.primary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(radarSurface, radius = size.width * 0.5f)
                        drawCircle(radarPrimary.copy(alpha = viewModel.matchProgress), radius = size.width * 0.5f * viewModel.matchProgress)
                    }
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = radarPrimary, modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { viewModel.matchProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            BodaErrorButton(
                text = BodaLang.get(viewModel.appLanguage, "cancel"),
                onClick = { showCancelReason = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cancel dialog asking for reason (Mandatory)
        if (showCancelReason) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Cancel Boda Request?", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Late cancelation may charge a 1,000 UGX fee to reimburse the rider's fuel.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        BodaTextField(
                            value = cancelReasonText,
                            onValueChange = { cancelReasonText = it },
                            label = "Reason for Cancellation",
                            placeholder = "e.g. Changed mind, long wait",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BodaTextButton(
                                text = "Go Back",
                                onClick = { showCancelReason = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BodaErrorButton(
                                text = "Cancel Request",
                                onClick = {
                                    viewModel.cancelActiveTrip(cancelReasonText)
                                },
                                enabled = cancelReasonText.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}
