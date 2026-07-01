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
import androidx.compose.ui.unit.sp
import com.example.ui.BodaViewModel
import com.example.ui.components.BodaCard
import com.example.ui.components.Color
import com.example.ui.components.Sp

// --- SCREEN 15: SUPPORT CHAT & FAQ ENGINE ---
@Composable
fun SupportScreen(viewModel: BodaViewModel) {
    var inChatMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Help & Support Gulu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (inChatMode) Color(0xFFFDB913) else Color(0xFF334155))
                    .clickable { inChatMode = !inChatMode }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (inChatMode) "FAQ Center" else "Live Officer Chat", color = if (inChatMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        if (inChatMode) {
            // Live agent chat messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isUser) Color(0xFFFDB913)
                                    else if (isSystem) Color(0xFF475569).copy(alpha = 0.5f)
                                    else Color(0xFF334155)
                                )
                                .padding(10.dp)
                                .fillMaxWidth(if (isSystem) 0.9f else 0.75f)
                        ) {
                            Text(
                                msg.message,
                                color = if (isUser) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSystem) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Sp.sm))

            // Message text input bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.newChatMessageText,
                    onValueChange = { viewModel.newChatMessageText = it },
                    placeholder = { Text("Ask about fares or lost items...", color = Color(0xFF475569)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFFFDB913),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Sp.sm))
                IconButton(onClick = { viewModel.sendSupportChatMessage() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFFDB913))
                }
            }
        } else {
            // Standard FAQ Knowledge Center
            OutlinedTextField(
                value = viewModel.supportSearchQuery,
                onValueChange = { viewModel.supportSearchQuery = it },
                placeholder = { Text("Search FAQ articles (e.g. lost bag)...", color = Color(0xFF475569)) },
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

            Spacer(modifier = Modifier.height(Sp.sm))

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
                            Text(q, color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(a, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
