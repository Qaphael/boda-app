package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.components.Color
import com.example.ui.components.Sp
import com.example.ui.components.BodaLang
import com.example.ui.components.BodaButton
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
import androidx.compose.ui.unit.sp

// --- SCREEN 6: MATCHING & WAITING ONBOARD SPINNER ---
@Composable
fun MatchingScreen(viewModel: BodaViewModel) {
    var showCancelReason by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Assigning closest Gulu Rider", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Sp.sm))
            Text("Boda Escrow secures this payment. You can cancel for free before matching.", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(Sp.xxl))

            // Animated Matching progress radar sweep
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(Color(0xFF1E293B), radius = size.width * 0.5f)
                        drawCircle(Color(0xFFFDB913).copy(alpha = viewModel.matchProgress), radius = size.width * 0.5f * viewModel.matchProgress)
                    }
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            LinearProgressIndicator(
                progress = { viewModel.matchProgress },
                color = Color(0xFFFDB913),
                trackColor = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(Sp.xxl))

            BodaButton(
                text = BodaLang.get(viewModel.appLanguage, "cancel"),
                onClick = { showCancelReason = true },
                containerColor = Color(0xFFE4002B),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        // Cancel dialog asking for reason (Mandatory)
        if (showCancelReason) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Cancel Boda Request?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text("Late cancelation may charge a 1,000 UGX fee to reimburse the rider's fuel.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        BodaTextField(
                            value = cancelReasonText,
                            onValueChange = { cancelReasonText = it },
                            label = "Reason for Cancellation",
                            placeholder = "e.g. Changed mind, long wait",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Sp.md))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BodaTextButton(
                                text = "Go Back",
                                onClick = { showCancelReason = false },
                                contentColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(Sp.sm))
                            BodaButton(
                                text = "Cancel Request",
                                onClick = {
                                    viewModel.cancelActiveTrip(cancelReasonText)
                                },
                                enabled = cancelReasonText.isNotEmpty(),
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
