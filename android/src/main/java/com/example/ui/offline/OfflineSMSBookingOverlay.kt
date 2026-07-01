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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.BodaViewModel
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaSecondaryButton
import androidx.compose.material3.MaterialTheme

@Composable
fun OfflineSMSBookingOverlay(viewModel: BodaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            .clickable { viewModel.showOfflineSMSDialog = false },
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Boda-Safe SMS Fallback Booking",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are currently offline. This booking will be dispatched via cell SMS shortcode and safely stored in your local Room SQLite database cache. Remote sync resumes automatically once you regain internet access.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Formatted Message Window
                Text(
                    text = "SMS PAYLOAD PREVIEW:",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("To Shortcode:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(viewModel.offlineSMSRecipientNumber, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.offlineSMSMessageBody,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
