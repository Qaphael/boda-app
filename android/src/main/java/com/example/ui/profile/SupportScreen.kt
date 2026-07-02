package com.example.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.BodaViewModel
import com.example.ui.components.BodaCard
import androidx.compose.material3.MaterialTheme

// --- SCREEN 15: SUPPORT CHAT & FAQ ENGINE ---
@Composable
fun SupportScreen(viewModel: BodaViewModel) {
    var inChatMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Help & Support Gulu", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (inChatMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { inChatMode = !inChatMode }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (inChatMode) "FAQ Center" else "Live Officer Chat", color = if (inChatMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (inChatMode) {
            // Live agent chat messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                    .padding(12.dp)
            ) {
                items(viewModel.activeChatMessages) { msg ->
                    val isUser = msg.sender == "user"
                    val isSystem = msg.sender == "system"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isUser) Alignment.CenterEnd else if (isSystem) Alignment.Center else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isUser) MaterialTheme.colorScheme.primary
                                    else if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(10.dp)
                                .fillMaxWidth(if (isSystem) 0.9f else 0.75f)
                        ) {
                            Text(
                                msg.message,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message text input bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.newChatMessageText,
                    onValueChange = { viewModel.newChatMessageText = it },
                    placeholder = { Text("Ask about fares or lost items...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.sendSupportChatMessage() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            // Standard FAQ Knowledge Center
            OutlinedTextField(
                value = viewModel.supportSearchQuery,
                onValueChange = { viewModel.supportSearchQuery = it },
                placeholder = { Text("Search FAQ articles (e.g. lost bag)...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

            Spacer(modifier = Modifier.height(8.dp))

            val faqs = listOf(
                "How is my fare calculated in Gulu?" to "Boda Gulu rides start with a base of 2,000 UGX, plus 1,000 UGX per kilometer. Deliveries start at 3,000 UGX.",
                "How do I use MTN Mobile Money to top up?" to "Go to Wallet, type the amount, tap deposit, and enter your MoMo pin on the phone screen.",
                "What is Boda Escrow protection?" to "Your payment is locked when you book. It is only released to the rider after you arrive and share the Security OTP.",
                "Lost item recovery in Gulu?" to "Go to Support Live Officer Chat immediately or call our Gulu hotline at 0800 112 112 to secure your item."
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(faqs.filter { it.first.contains(viewModel.supportSearchQuery, ignoreCase = true) }) { (q, a) ->
                    BodaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(q, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(a, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
