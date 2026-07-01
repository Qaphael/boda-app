package com.example.ui.components

import android.Manifest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.BodaViewModel
import com.example.ui.wallet.confirmWalletTopupWithPin
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun WelcomeBonusDialog(
    userName: String,
    usedReferralCode: Boolean,
    onDismiss: () -> Unit,
    onGoToReferrals: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDF89", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Welcome to Boda Gulu, ${userName.substringBefore(" ").ifEmpty { "Rider" }}!",
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (usedReferralCode) {
                    Text(
                        "Your referral bonus of UGX 3,000 will be added to your wallet automatically after your first completed ride.",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp,
                        textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                } else {
                    Text(
                        "Your account is ready. Share your referral code with friends and earn UGX 3,000 for every friend who completes their first ride.",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp,
                        textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (usedReferralCode) "UGX 3,000 bonus incoming \uD83C\uDFCD\uFE0F"
                        else "Earn UGX 3,000 per referral \uD83C\uDFCD\uFE0F",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                BodaButton(text = "Book My First Ride", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                if (!usedReferralCode) {
                    TextButton(onClick = onGoToReferrals) {
                        Text("Share my referral code", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionNudge() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("boda_prefs", android.content.Context.MODE_PRIVATE) }
    val alreadyAsked = remember { prefs.getBoolean("notif_permission_asked", false) }
    if (alreadyAsked) return

    val notifState: com.google.accompanist.permissions.PermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else return

    var showNudge by remember { mutableStateOf(!notifState.status.isGranted) }

    if (showNudge) {
        AlertDialog(
            onDismissRequest = {
                showNudge = false
                prefs.edit().putBoolean("notif_permission_asked", true).apply()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Know when your driver arrives", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Get notified the moment your boda is 2 minutes away — so you're ready at the pickup point.",
                    color = MaterialTheme.colorScheme.outline, fontSize = 14.sp
                )
            },
            confirmButton = {
                BodaButton(
                    text = "Allow Notifications",
                    onClick = {
                        notifState.launchPermissionRequest()
                        showNudge = false
                        prefs.edit().putBoolean("notif_permission_asked", true).apply()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showNudge = false
                    prefs.edit().putBoolean("notif_permission_asked", true).apply()
                }) {
                    Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun SystemOverlayDialog(title: String, desc: String, cta: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(Sp.md))
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(desc, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(Sp.lg))
                BodaButton(
                    text = cta,
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MoMoPinDialog(viewModel: BodaViewModel) {
    val isMtn = viewModel.momoPromptProvider.contains("MTN", ignoreCase = true)
    val brandColor = if (isMtn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val brandTextColor = if (isMtn) MaterialTheme.colorScheme.onPrimary else ComposeColor.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.85f))
            .padding(24.dp)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            border = BorderStroke(2.dp, brandColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandColor)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.momoPromptProvider.uppercase(),
                            color = brandTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = brandTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GULU SECURE ESCROW TRANSACTION",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text(
                        text = "Authorize payment of UGX ${viewModel.momoPromptAmount.toInt()} to Boda Gulu Wallet?",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text(
                        text = "Target Number: ${viewModel.momoPromptPhone}",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.momoPinInput.isEmpty()) "Enter PIN" else "• ".repeat(viewModel.momoPinInput.length),
                            color = if (viewModel.momoPinInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else brandColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    if (viewModel.momoPinError) {
                        Text(
                            text = "Invalid PIN format. Must be 4-5 digits.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Sp.md))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Clear", "0", "Delete")
                        )
                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                when (key) {
                                                    "Clear" -> viewModel.momoPinInput = ""
                                                    "Delete" -> {
                                                        if (viewModel.momoPinInput.isNotEmpty()) {
                                                            viewModel.momoPinInput = viewModel.momoPinInput.dropLast(1)
                                                        }
                                                    }
                                                    else -> {
                                                        if (viewModel.momoPinInput.length < 5) {
                                                            viewModel.momoPinInput += key
                                                            viewModel.momoPinError = false
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = if (key in listOf("Clear", "Delete")) MaterialTheme.colorScheme.outline else ComposeColor.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BodaSecondaryButton(
                            text = "Cancel",
                            onClick = { viewModel.showMoMoPinDialog = false },
                            modifier = Modifier.weight(1f)
                        )

                        BodaButton(
                            text = "Approve",
                            onClick = { viewModel.confirmWalletTopupWithPin() },
                            containerColor = brandColor,
                            contentColor = brandTextColor,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}
