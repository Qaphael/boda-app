package com.example.ui.referrals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Referral
import com.example.ui.BodaViewModel
import com.example.ui.home.navigateBack
import com.example.ui.home.shareReferralLink
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.Color
import com.example.ui.components.Sp

@Composable
fun ReferralsScreen(viewModel: BodaViewModel, referrals: List<Referral>) {
    val user by viewModel.userProfile.collectAsState()
    val myCode = user?.referralCode?.ifEmpty { "GULU-BODA-256" } ?: "GULU-BODA-256"
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val totalCount = referrals.size
    val pendingCount = referrals.count { it.status == "pending" }
    val completedCount = referrals.count { it.status == "completed" }
    val totalEarnings = referrals.filter { it.status == "completed" }.sumOf { it.rewardAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("referrals_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(Sp.md))
            Text(
                text = "Refer & Earn Gulu",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (viewModel.activePromoMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { viewModel.activePromoMessage = "" }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        text = viewModel.activePromoMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.activePromoMessage = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card explaining the system
            item {
                BodaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDB913).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Column {
                                Text("Get UGX 3,000 for every friend!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Both of you get rewarded on their 1st ride", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(Sp.sm))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(Sp.sm))

                        Text("YOUR REFERRAL CODE", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myCode,
                                color = Color(0xFFFDB913),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("referral_code_text")
                            )
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(myCode))
                                    isCopied = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCopied) Color(0xFF10B981) else Color(0xFFFDB913)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("copy_code_btn")
                            ) {
                                Text(
                                    text = if (isCopied) "Copied!" else "Copy",
                                    color = if (isCopied) ComposeColor.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.sm))
                    val shareContext = LocalContext.current
                    BodaButton(
                        text = "Share My Referral Link",
                        onClick = { viewModel.shareReferralLink(shareContext) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Stats grid card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Referred", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "$totalCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("In Progress", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "$pendingCount", color = Color(0xFFFDB913), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Earned", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "UGX ${totalEarnings.toInt()}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Referred Friends Header
            item {
                Text(
                    text = "REFERRED FRIENDS",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // List items or empty placeholder
            if (referrals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("No referrals yet in Gulu.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(referrals) { ref ->
                    BodaCard(
                        testTag = "referral_item_${ref.id}",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Column {
                                    Text(ref.referredName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(ref.referredPhone, color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }

                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (ref.status == "completed") Color(0xFF10B981).copy(alpha = 0.15f)
                                        else Color(0xFFFDB913).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (ref.status == "completed") "UGX +3,000" else "Pending 1st Ride",
                                    color = if (ref.status == "completed") Color(0xFF10B981) else Color(0xFFFDB913),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Testing / Simulation Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text("GULU REFERRAL SIMULATOR", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.xs))
                        Text(
                            "Simulate realistic refer-a-friend operations locally to test the automated payouts and statistics updates.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simulateNewReferralSignUp() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).testTag("simulate_signup_btn")
                            ) {
                                Text("1. Friend Sign-up", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.simulateReferralFirstTripCompletion() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDB913)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).testTag("simulate_trip_btn")
                            ) {
                                Text("2. Friend's 1st Ride", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
