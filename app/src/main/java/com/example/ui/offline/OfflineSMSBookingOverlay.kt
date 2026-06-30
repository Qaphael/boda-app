package com.example.ui.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BodaViewModel
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaSecondaryButton
import com.example.ui.components.Color
import com.example.ui.components.Sp

@Composable
fun OfflineSMSBookingOverlay(viewModel: BodaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.85f))
            .clickable { viewModel.showOfflineSMSDialog = false },
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            border = BorderStroke(1.5.dp, ComposeColor(0xFFFDB913)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Mode",
                    tint = ComposeColor(0xFFFDB913),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    text = "Boda-Safe SMS Fallback Booking",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    text = "You are currently offline. This booking will be dispatched via cell SMS shortcode and safely stored in your local Room SQLite database cache. Remote sync resumes automatically once you regain internet access.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Sp.md))

                // Formatted Message Window
                Text(
                    text = "SMS PAYLOAD PREVIEW:",
                    color = ComposeColor(0xFFFDB913),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(Sp.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("To Shortcode:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.offlineSMSRecipientNumber, color = ComposeColor(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        HorizontalDivider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text(
                            text = viewModel.offlineSMSMessageBody,
                            color = Color.White,
                            fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BodaSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOfflineSMSDialog = false },
                        modifier = Modifier.weight(1f)
                    )

                    BodaButton(
                        text = "Send via SMS",
                        onClick = { viewModel.confirmOfflineSMSBooking() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
